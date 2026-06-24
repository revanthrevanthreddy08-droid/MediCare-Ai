import os
import jwt
import datetime
from functools import wraps
from flask import request, jsonify
from collections import defaultdict
from utils.logger import log_security_event, app_logger

# Simple Blocklist for invalidated / revoked JWT access tokens (Handles Logouts securely)
JWT_REVOKED_TOKEN_STORE = set()

# Lockout registry to defend against dictionary brute-force operations
FAILED_ATTEMPTS_LIMIT = 5
LOCKOUT_PERIOD_MINUTES = 15
IP_FAILED_LOGINS_REGISTRY = defaultdict(list)  # tracks [timestamps] for failed attempts per IP address
ACCOUNT_LOCKOUT_REGISTRY = {} # maps username/email: lockout_till_timestamp

# Optional Two-Factor secret cache simulated dictionary
USER_2FA_KEYS = {} # maps username: secret_token

def is_ip_brute_forcing(client_ip: str) -> bool:
    """
    Sifts IP access timeline logs to block spamming attempts.
    """
    current_time = datetime.datetime.utcnow()
    # Filter only occurrences within active limits window (e.g. 15 minutes)
    valid_threshold = current_time - datetime.timedelta(minutes=LOCKOUT_PERIOD_MINUTES)
    
    attempts = IP_FAILED_LOGINS_REGISTRY[client_ip]
    active_failed_attempts = [t for t in attempts if t > valid_threshold]
    IP_FAILED_LOGINS_REGISTRY[client_ip] = active_failed_attempts
    
    return len(active_failed_attempts) >= FAILED_ATTEMPTS_LIMIT

def is_account_temporarily_locked(email: str) -> tuple[bool, int]:
    """
    Checks if specific clinical accounts have active administrative locks.
    """
    email_clean = email.strip().lower()
    if email_clean in ACCOUNT_LOCKOUT_REGISTRY:
        unlock_time = ACCOUNT_LOCKOUT_REGISTRY[email_clean]
        current_time = datetime.datetime.utcnow()
        if current_time < unlock_time:
             remaining_seconds = int((unlock_time - current_time).total_seconds())
             return True, remaining_seconds
        else:
             # Lock has expired naturally
             del ACCOUNT_LOCKOUT_REGISTRY[email_clean]
    return False, 0

def lock_account_on_consecutive_failures(email: str, client_ip: str):
    """
    Places severe block limits onto account to deflect further cracking.
    """
    email_clean = email.strip().lower()
    unlock_time = datetime.datetime.utcnow() + datetime.timedelta(minutes=LOCKOUT_PERIOD_MINUTES)
    ACCOUNT_LOCKOUT_REGISTRY[email_clean] = unlock_time
    
    log_security_event(
        event_type="ACCOUNT_LOCKED_OUT",
        ip_address=client_ip,
        details=f"Secure account lockout triggered for: {email_clean} due to {FAILED_ATTEMPTS_LIMIT} consecutive login errors.",
        severity="CRITICAL",
        status="LOCKED",
        email=email_clean
    )

def register_failed_login_attempt(email: str, client_ip: str):
    """
    Increments consecutive failure logs for account and IP, executing lockout check.
    """
    now = datetime.datetime.utcnow()
    IP_FAILED_LOGINS_REGISTRY[client_ip].append(now)
    email_clean = email.strip().lower()
    
    # Calculate current failed metrics
    valid_threshold = now - datetime.timedelta(minutes=LOCKOUT_PERIOD_MINUTES)
    failed_counts = sum(1 for t in IP_FAILED_LOGINS_REGISTRY[client_ip] if t > valid_threshold)
    
    if failed_counts >= FAILED_ATTEMPTS_LIMIT:
         lock_account_on_consecutive_failures(email_clean, client_ip)

def clear_failed_login_records(email: str, client_ip: str):
    """
    Resets track metrics once security validation hurdles successfully execute.
    """
    email_clean = email.strip().lower()
    if email_clean in ACCOUNT_LOCKOUT_REGISTRY:
        del ACCOUNT_LOCKOUT_REGISTRY[email_clean]
    if client_ip in IP_FAILED_LOGINS_REGISTRY:
        del IP_FAILED_LOGINS_REGISTRY[client_ip]


# ==========================================
# SECURE JWT CREATION SERVICES
# ==========================================

def issue_access_and_refresh_tokens(user_email: str, user_name: str, user_role: str, secret_key: str):
    """
    Issues short-lived access parameters with isolated refresh credentials tracking (OWASP recommendation).
    """
    now = datetime.datetime.utcnow()
    
    # Access Token: Expire in 15 minutes (Minimize exposure window)
    access_payload = {
        'sub': user_email,
        'name': user_name,
        'role': user_role,
        'token_type': 'access',
        'iat': now,
        'exp': now + datetime.timedelta(minutes=15)
    }
    
    # Refresh Token: Expire in 7 days (Enables safe background sessions continuity)
    refresh_payload = {
        'sub': user_email,
        'token_type': 'refresh',
        'iat': now,
        'exp': now + datetime.timedelta(days=7)
    }
    
    access_token = jwt.encode(access_payload, secret_key, algorithm='HS256')
    refresh_token = jwt.encode(refresh_payload, secret_key, algorithm='HS256')
    
    return access_token, refresh_token


# ==========================================
# AUTH PROTECTIVE GATES DECORATORS
# ==========================================

def jwt_access_token_required(f):
    """
    Secures route endpoint verifying authentic header JWT signatures and revokation states.
    """
    @wraps(f)
    def decorated_method(*args, **kwargs):
        token = None
        client_ip = request.remote_addr or '127.0.0.1'
        
        # Pull down Authorization Header (Bearer <token>)
        if 'Authorization' in request.headers:
            auth_header = request.headers['Authorization']
            try:
                parts = auth_header.split(" ")
                if parts[0].lower() == 'bearer' and len(parts) == 2:
                    token = parts[1]
            except IndexError:
                return jsonify({'message': 'Malformed Authorization structure. Must resolve "Bearer <token>"'}), 401

        if not token:
             return jsonify({'message': 'Security block: Access token required to query endpoint.'}), 401

        # Check Active Logout status in Revoked Tokens blocklist store
        if token in JWT_REVOKED_TOKEN_STORE:
             log_security_event(
                 event_type="REVOKED_TOKEN_REUSE_ATTEMPT",
                 ip_address=client_ip,
                 details="Request presented a logged-out or invalidated token signature.",
                 severity="HIGH",
                 status="BLOCKED"
             )
             return jsonify({'message': 'Authentication Denied: Token signature has been invalidated by logout.'}), 401

        try:
             # Retrieve cryptographic verification secret key
             sec_key = os.environ.get('JWT_SECRET_KEY', 'medicare_ai_ultra_secure_jwt_secret_token_key_2026')
             decoded_claims = jwt.decode(token, sec_key, algorithms=["HS256"])
             
             # Prevent using Refresh Tokens inside standard Access Token Routes
             if decoded_claims.get('token_type') != 'access':
                  return jsonify({'message': 'Usage Error: Expected Access Token payload structure.'}), 401
                  
             # Inject token details onto request contexts for routing services consumption
             request.jwt_claims = decoded_claims
             
        except jwt.ExpiredSignatureError:
             return jsonify({'message': 'Access session has expired. Token refresh actions required.'}), 401
        except jwt.InvalidTokenError as e:
             log_security_event(
                 event_type="INVALID_JWT_SIGNATURE",
                 ip_address=client_ip,
                 details=f"Rejected token parsing anomaly: {str(e)}",
                 severity="CRITICAL",
                 status="BLOCKED"
             )
             return jsonify({'message': 'Access Denied: Malformed token credentials.'}), 401

        # Forward execution to actual business handlers
        return f(*args, **kwargs)
        
    return decorated_method


def role_check_required(*allowed_roles):
    """
    RBAC verification validating token scope matches route privileges.
    """
    def endpoint_gate_decorator(f):
        @wraps(f)
        def wrapper_method(*args, **kwargs):
            # Assert target claims are set on the request envelope object
            claims = getattr(request, 'jwt_claims', None)
            if not claims:
                return jsonify({'message': 'Authorization failure: Claims mapping undefined.'}), 401
                
            user_role = claims.get('role', 'Patient')
            if user_role not in allowed_roles:
                 log_security_event(
                     event_type="UNAUTHORIZED_RBAC_ESCALATION",
                     ip_address=request.remote_addr or '127.0.0.1',
                     details=f"Unauthorized access. Required: {allowed_roles}, Actual: {user_role}",
                     severity="HIGH",
                     status="FORBIDDEN",
                     email=claims.get('sub')
                 )
                 return jsonify({
                     'message': f'Access Forbidden: Insufficient clearance level. Required: {allowed_roles}'
                 }), 403
                 
            return f(*args, **kwargs)
        return wrapper_method
    return endpoint_gate_decorator
