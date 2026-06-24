import os
import uuid
from werkzeug.utils import secure_filename
from flask import abort

# Constants matching environmental security setup requirements
ALLOWED_EXTENSIONS = {'pdf', 'png', 'jpg', 'jpeg'}
MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024  # Limit files to max 5MB (Prevent DoS)

# Base upload folders outside of web root templates directories
BASE_SECURE_STORAGE = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'secure_uploads'))
if not os.path.exists(BASE_SECURE_STORAGE):
    os.makedirs(BASE_SECURE_STORAGE)

def is_allowed_file(filename: str) -> bool:
    """
    Checks if extension falls into permissible safe white-list files types.
    """
    if '.' not in filename:
        return False
    ext = filename.rsplit('.', 1)[1].lower()
    return ext in ALLOWED_EXTENSIONS

def scan_file_for_malware_simulated(file_bytes: bytes) -> bool:
    """
    Simulated security malware signature engine.
    Prunes files for general exploit payload segments or fake file extension structures.
    In real life, this integrates ClamAV or malware scanning APIs.
    """
    # Look for malicious shell payload heuristics
    malicious_indicators = [
        b'<?php', b'eval($_POST', b'cmd.exe', b'/bin/bash', b'system(', b'exec('
    ]
    for indicator in malicious_indicators:
        if indicator in file_bytes:
            return False # Infected or compromised payload header found!
    return True # Clean

def save_uploaded_file_securely(file_storage_obj) -> str:
    """
    Saves user-uploaded file on server with severe visual security structures:
    1. Size validations
    2. Secure filename sanitization + unique ID generation (blocks path traversals)
    3. Content malware scanning checks
    4. Safe directory separation
    """
    if not file_storage_obj or file_storage_obj.filename == '':
        raise ValueError("Invalid file container: Empty or missing data stream.")

    # Rule 1: Securely fetch extension and enforce type approvals
    orig_filename = secure_filename(file_storage_obj.filename)
    if not is_allowed_file(orig_filename):
        raise ValueError("Access Denied: Unsupported file extension type.")

    # Rule 2: Enforce strict file size checks before stream persistence
    file_storage_obj.seek(0, os.SEEK_END)
    file_size = file_storage_obj.tell()
    file_storage_obj.seek(0)  # Reset stream position index pointer
    
    if file_size > MAX_FILE_SIZE_BYTES:
        raise ValueError(f"Payload Denied: Upload file size exceeds {MAX_FILE_SIZE_BYTES / (1024*1024)}MB threshold bounds.")

    # Rule 3: Scan content with security engine
    file_data = file_storage_obj.read()
    file_storage_obj.seek(0) # Reset stream pointer
    
    if not scan_file_for_malware_simulated(file_data):
        raise ValueError("Threat Alert: Upload aborted. Malware scanner detected suspicious heuristics.")

    # Rule 4: Renames uploaded file to random UUID (Prevents Insecure Direct Object References & Path Traversals)
    ext = orig_filename.rsplit('.', 1)[1].lower()
    secure_uuid_name = f"{uuid.uuid4().hex}.{ext}"
    
    # Store strictly outside public context directory paths
    target_path = os.path.join(BASE_SECURE_STORAGE, secure_uuid_name)
    
    # Save stream safely onto the absolute physical volume
    file_storage_obj.save(target_path)
    
    return secure_uuid_name
