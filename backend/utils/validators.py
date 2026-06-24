import re
import html
from flask import abort

# Regular Expression patterns matching strict data requirements
EMAIL_REGEX = re.compile(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$')
PASSWORD_MIN_LENGTH = 10

# Suspicious SQLi or Command Injection structural indicator blocks
XSS_MALICIOUS_SUBSTRINGS = [
    '<script', 'javascript:', 'onload=', 'onerror=', '<iframe', 'eval(', 'document.cookie'
]

def sanitize_input_string(val: str) -> str:
    """
    Escapes HTML tags and trims strings to suppress XSS vectors.
    """
    if not isinstance(val, str):
        return ""
    val_trimmed = val.strip()
    # Simple recursive entity escaping for special HTML anchors
    escaped = html.escape(val_trimmed)
    
    # Check for prominent XSS signature arrays
    for trigger in XSS_MALICIOUS_SUBSTRINGS:
        if trigger in escaped.lower():
            # Strips tag entirely to immunize context payloads
            escaped = escaped.lower().replace(trigger, "[XSS-STRIPPED]")
            
    return escaped

def validate_email_address(email: str) -> bool:
    """
    Enforces standard compliant structure checks.
    """
    if not email or not isinstance(email, str):
        return False
    return bool(EMAIL_REGEX.match(email.strip()))

def check_password_strength(password: str) -> tuple[bool, str]:
    """
    Validates password contains sufficient complexity to block brute-force actions.
    Enforces OWASP standard password strength configurations.
    """
    if not password or len(password) < PASSWORD_MIN_LENGTH:
        return False, f"Password must be at least {PASSWORD_MIN_LENGTH} characters long."
    
    if not re.search(r"[A-Z]", password):
        return False, "Password context must contain at least one uppercase letter."
        
    if not re.search(r"[a-z]", password):
        return False, "Password context must contain at least one lowercase letter."
        
    if not re.search(r"\d", password):
        return False, "Password context must contain at least one numeric digit."
        
    if not re.search(r"[!@#$%^&*(),.?\":{}|<>]", password):
        return False, "Password context must contain at least one special character symbol."
        
    return True, "Excellent. Password complexity satisfies security norms."

def sanitize_and_validate_registration_payload(data: dict) -> dict:
    """
    Validates and standardizes registration user payloads, raising clear exceptions on issues.
    """
    if not data:
        raise ValueError("Request body must be a valid JSON dictionary.")

    email = sanitize_input_string(data.get('email', '')).lower()
    password = data.get('password', '')  # Do not HTML-escape passwords before hashing (messes up structure)
    name = sanitize_input_string(data.get('name', ''))
    role = sanitize_input_string(data.get('role', 'Patient'))

    if not validate_email_address(email):
        raise ValueError("Malformed or incorrect Email address format.")

    pw_valid, msg = check_password_strength(password)
    if not pw_valid:
        raise ValueError(msg)

    if not name or len(name) < 2:
        raise ValueError("Name field is mandatory and must contain at least 2 characters.")

    if role not in ['Patient', 'Caregiver', 'Admin']:
        raise ValueError("Specified role must belong to approved types: Patient, Caregiver, Admin.")

    try:
        age_str = str(data.get('age', '28')).strip()
        age = int(age_str)
        if age < 0 or age > 120:
            raise ValueError()
    except (TypeError, ValueError):
         raise ValueError("Age field must reflect an integer between bounds [0-120].")

    # Construct validated structured payload
    return {
        'email': email,
        'password': password,
        'name': name,
        'role': role,
        'age': age,
        'gender': sanitize_input_string(data.get('gender', 'Male')),
        'bloodGroup': sanitize_input_string(data.get('bloodGroup', 'O+')),
        'allergies': sanitize_input_string(data.get('allergies', 'None')),
        'medicalConditions': sanitize_input_string(data.get('medicalConditions', 'None')),
        'emergencyContact': sanitize_input_string(data.get('emergencyContact', 'Dr. Thompson'))
    }
