import time
from flask import request, jsonify, current_app, abort
from collections import defaultdict
import datetime
from utils.logger import log_security_event, app_logger

# Simple memory storage to manage lightweight security rate limits (prevent brute force & DDOS)
# Tracks request timestamps per incoming remote address client IP
IP_CONNECTIONS_TRACKER = defaultdict(list)
RATE_LIMIT_MAX_ATTEMPTS = 60 # 60 requests per client
RATE_LIMIT_WINDOW_SECONDS = 60 # per 1 minute window

def limit_request_rates_on_client_ip():
    """
    Validates client request rate frequencies, dropping excessive connections.
    """
    client_ip = request.remote_addr or '127.0.0.1'
    current_time = time.time()
    
    # Prune historical tracked indexes older than rate limit bounds
    IP_CONNECTIONS_TRACKER[client_ip] = [
        t for t in IP_CONNECTIONS_TRACKER[client_ip] 
        if current_time - t < RATE_LIMIT_WINDOW_SECONDS
    ]
    
    # Check current load density
    if len(IP_CONNECTIONS_TRACKER[client_ip]) >= RATE_LIMIT_MAX_ATTEMPTS:
        log_security_event(
            event_type="RATE_LIMIT_BREACHED",
            ip_address=client_ip,
            details=f"IP triggered rate limiter threshold. Request Count: {len(IP_CONNECTIONS_TRACKER[client_ip])}",
            severity="ERROR",
            status="BLOCKED"
        )
        return jsonify({
            'message': 'Rate Limit Exceeded: Secure gateway cooldown active. Please verify parameters and repeat later.'
        }), 429
        
    IP_CONNECTIONS_TRACKER[client_ip].append(current_time)

def setup_payload_limits_and_safety_checks(app):
    """
    Registers incoming network envelope size filters and safety middleware hooks.
    """
    # Enforces maximum incoming upload/JSON parsing bounds (e.g., max 16MB) to disrupt memory bloating DoS
    app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024 

    @app.before_request
    def intercept_requests():
        # Execute active rate limiting Checks
        rate_limit_result = limit_request_rates_on_client_ip()
        if rate_limit_result:
            return rate_limit_result

    @app.after_request
    def set_secure_owasp_security_headers(response):
        """
        Enforces strict OWASP secure transmission policy headers onto outgoing packets.
        """
        # 1. Content Security Policy (CSP) - Block illicit inline scripts, stylesheets, styles framing vectors
        response.headers['Content-Security-Policy'] = (
            "default-src 'self'; "
            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
            "font-src 'self' https://fonts.gstatic.com; "
            "img-src 'self' data: "
            "frame-ancestors 'none';"
        )
        
        # 2. Strict-Transport-Security (HSTS) - Enforces browser HTTPS channel routing for 1 year
        response.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains; preload'
        
        # 3. X-Frame-Options - Immune-ize from simple UI framing clickjacking attempts
        response.headers['X-Frame-Options'] = 'DENY'
        
        # 4. X-Content-Type-Options - Disrupts browsers sniffing MIME types as secondary executables
        response.headers['X-Content-Type-Options'] = 'nosniff'
        
        # 5. Referrer-Policy - Controls downstream URL exposure in navigation headers
        response.headers['Referrer-Policy'] = 'strict-origin-when-cross-origin'
        
        # 6. Permissions-Policy - Disables client system sensor access on medical route contexts
        response.headers['Permissions-Policy'] = 'geolocation=(), camera=(), microphone=()'
        
        return response

    @app.errorhandler(400)
    def bad_request_handler(error):
        return jsonify({'message': 'Bad Request: Missing or malformed parameters.'}), 400

    @app.errorhandler(401)
    def unauthorized_handler(error):
         return jsonify({'message': 'Access Denied: Invalid credentials or session context.'}), 401

    @app.errorhandler(403)
    def forbidden_handler(error):
         return jsonify({'message': 'Access Forbidden: Insufficient role permissions.'}), 403

    @app.errorhandler(404)
    def not_found_handler(error):
         return jsonify({'message': 'Resource not found.'}), 404

    @app.errorhandler(405)
    def method_not_allowed_handler(error):
         return jsonify({'message': 'Action not permitted for request method.'}), 405

    @app.errorhandler(413)
    def payload_too_large_handler(error):
         return jsonify({'message': 'Payload Rejected: Incoming package exceeds system safe thresholds.'}), 413

    @app.errorhandler(500)
    def internal_server_error_handler(error):
        """
        Sanitizes server state outputs: Suppresses detailed traceback paths from printing directly onto user consoles.
        """
        app_logger.error(f"Internal Handled Exception: {str(error)}")
        return jsonify({
            'message': 'Error: An internal database or gateway exception occurred. Secure audit logging recorded.'
        }), 500
