# MediCare AI – Production Deployment & Hardening Security Guide
---

This document outlines the architecture, configuration steps, and security procedures required to deploy the **MediCare AI Python Flask Secure Backend** to an enterprise, HIPAA-compliant production environment.

## 🐳 Docker Containerization Architecture (Non-Root Hardening)

To prevent container breakout exploits, the application is packaged in a minimal base image, running as a non-privileged system user without `root` shell capabilities.

### Production `Dockerfile` Template
Create this file in the backend root directory (`/backend/Dockerfile`):

```dockerfile
# ==============================================================================
# STAGE 1: Build Dependencies
# ==============================================================================
FROM python:3.11-slim AS builder

WORKDIR /app

# Install system compilation packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libmariadb-dev \
    && rm -rf /var/lib/apt/lists/*

# Compile wheel dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# ==============================================================================
# STAGE 2: Secure Runtime Environment
# ==============================================================================
FROM python:3.11-slim AS runner

WORKDIR /app

# Create a restricted user and group (UID/GID 10001)
RUN groupadd -g 10001 medicaregrp && \
    useradd -u 10001 -g medicaregrp -s /sbin/nologin -m medicareusr

# Inherit built wheels package binaries
COPY --from=builder /root/.local /home/medicareusr/.local
COPY . .

# Set appropriate directory ownerships
RUN mkdir -p logs secure_uploads && \
    chown -R medicareusr:medicaregrp /app && \
    chmod -R 750 /app

# Update system PATH variable
ENV PATH=/home/medicareusr/.local/bin:$PATH
ENV FLASK_ENV=production

# Drop privileges explicitly - container runs as non-root user
USER medicareusr

EXPOSE 5000

# Fire production-grade Gunicorn WSGI runtime server
CMD ["gunicorn", "--workers", "4", "--bind", "0.0.0.0:5000", "app:app", "--log-level=info"]
```

---

## 🛡️ Production Nginx Reverse Proxy SSL Configurations

The backend should never be directly exposed to the internet. Always deploy behind an **Nginx** reverse proxy or high-performance load balancer terminating SSL/TLS 1.3 channels.

### Secure Nginx Server Block Template (`/etc/nginx/conf.d/medicare.conf`)

```nginx
upstream flask_backend {
    server 127.0.0.1:5000;
    keepalive 32;
}

server {
    listen 80;
    server_name api.medicare.ai;
    # Force HTTP Strict Transport redirecting routes safely
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.medicare.ai;

    # High-Strength Let's Encrypt certificates configurations
    ssl_certificate /etc/letsencrypt/live/api.medicare.ai/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.medicare.ai/privkey.pem;

    # Enforced Cryptography Policy (TLS 1.2 and TLS 1.3 only)
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    ssl_ciphers "ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384";

    # SSL Session Optimization parameters
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;

    # Enforce secure cookies headers matching HSTS mechanisms
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Content-Security-Policy "default-src 'self'; frame-ancestors 'none';" always;

    # Safe log monitoring outputs
    access_log /var/log/nginx/medicare_access.log;
    error_log /var/log/nginx/medicare_error.log warn;

    # Location rule forwarding parameters onto Gunicorn container gateway
    location / {
        proxy_pass http://flask_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;

        # Constrain maximum incoming package upload sizes
        client_max_body_size 16M;
    }
}
```

---

## 🔒 10-Point Pre-Deployment Security Hardening Checklist

Prior to launching MediCare AI onto live production environments, engineers MUST confirm the following audit statuses:

1. [ ] **Environment Isolation Checks**: Verify `FLASK_ENV=production` is set and `FLASK_DEBUG=False`. 
2. [ ] **Secrets Rotations audit**: Verify all keys (`JWT_SECRET_KEY`, `REFRESH_TOKEN_SECRET`, `FLASK_COOKIE_SECRET`) are rotated under high-entropy unique sequences inside the active server environment, not standard template fallbacks.
3. [ ] **Database Connection Safeguard**: Confirm SQL user `medicare_secure_app` only holds permissions for DML operations (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) on database, totally disabling DDL executions in public directories.
4. [ ] **At-Rest Cipher checks**: Confirm the unique system `AES_ENCRYPTION_KEY` hash key is configured securely to safeguard and decrypt columns correctly.
5. [ ] **Disable External Networking**: Confirm the database port (e.g., MySQL default `3306`) is localized via system firewall constraints, blocking public internet socket attachments.
6. [ ] **Cookie flags check**: Assure cookies hold `Secure`, `HttpOnly` and `SameSite=Strict` elements to neutralize brute force credential harvesting.
7. [ ] **SSL/TLS Cert Validity**: Periodically run check-ups utilizing SSL Labs API tools to assert TLS compliance scores sit firmly within `A+` standards.
8. [ ] **Automatic Logs Forwarders**: Ensure security audit logging files (under `/backend/logs/security_audit.log`) are bound via cron scripts to forward alerts onto Security Information and Event Management (SIEM) consoles.
9. [ ] **Upload isolation boundaries**: Confirm the directory `/var/medicare/secure_uploads` holds custom write permissions restricted entirely to our runner container context owner.
10. [ ] **Failsafe backup schema**: Assert automatic continuous snapshot backups of active MySQL volumes are operational and stored separately in isolated cold-vault backups.
