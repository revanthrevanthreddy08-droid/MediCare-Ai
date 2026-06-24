import os
import logging
from logging.handlers import RotatingFileHandler
import datetime
import json

# Setup standard logging folders and logs path
LOGS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'logs'))
if not os.path.exists(LOGS_DIR):
    os.makedirs(LOGS_DIR)

# File names for decoupled security audit metrics and general runtime logs
GENERAL_LOG_FILE = os.path.join(LOGS_DIR, 'medicare_app.log')
SECURITY_AUDIT_FILE = os.path.join(LOGS_DIR, 'security_audit.log')

# Define custom log formatting style
class SecurityJSONFormatter(logging.Formatter):
    """
    Format security audit records into industry-standard JSON structures for SIEM ingestion.
    """
    def format(self, record):
        log_payload = {
            "timestamp": datetime.datetime.utcnow().isoformat() + "Z",
            "level": record.levelname,
            "module": record.module,
            "message": record.getMessage(),
        }
        # Safely extract extra fields if present
        if hasattr(record, "audit_context"):
            log_payload["auditContext"] = record.audit_context
        
        return json.dumps(log_payload)

# Formatter definitions
standard_formatter = logging.Formatter(
    '[%(asctime)s] %(levelname)s in %(module)s (Line %(lineno)d): %(message)s'
)
json_formatter = SecurityJSONFormatter()

# Setup General Application Logger
app_logger = logging.getLogger('medicare_app')
app_logger.setLevel(logging.INFO)

general_handler = RotatingFileHandler(
    GENERAL_LOG_FILE, 
    maxBytes=10 * 1024 * 1024,  # 10 MB rotating threshold bounds
    backupCount=5
)
general_handler.setFormatter(standard_formatter)
app_logger.addHandler(general_handler)

# Setup Highly Protected Security Audit Logger (HIPAA-compliant, tamper-resistant stream)
security_logger = logging.getLogger('medicare_security')
security_logger.setLevel(logging.INFO)

security_handler = RotatingFileHandler(
    SECURITY_AUDIT_FILE,
    maxBytes=20 * 1024 * 1024,  # 20 MB rotating threshold bounds
    backupCount=10
)
security_handler.setFormatter(json_formatter)
security_logger.addHandler(security_handler)

def log_security_event(event_type, ip_address, details, severity="WARN", status="FAILED", email=None):
    """
    Shorthand helper to audit security occurrences including brute-force alerts or permission violations.
    """
    context = {
        "eventType": event_type,
        "ipAddress": ip_address,
        "status": status,
        "identity": email,
        "timestamp": datetime.datetime.utcnow().isoformat() + "Z",
    }
    
    msg_str = f"[{event_type}] [Severity: {severity}] Status: {status} for IP: {ip_address}. Context: {details}"
    
    # Target log level based on severity level
    if severity == "CRITICAL":
        security_logger.critical(msg_str, extra={"audit_context": context})
    elif severity == "ERROR":
        security_logger.error(msg_str, extra={"audit_context": context})
    elif severity == "WARN":
        security_logger.warning(msg_str, extra={"audit_context": context})
    else:
        security_logger.info(msg_str, extra={"audit_context": context})
