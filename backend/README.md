# MediCare AI - Secure HIPAA JWT Authentication Gateways
---

This folder contains a complete, industry-standard, role-guarded **Python Flask Backend System** that handles JWT-based (JSON Web Token) authentication for **patients**, **caregivers**, and **admins**, ensuring robust credentials security and HIPAA-compliant data routing.

## 🔐 Key Architectural Features

1. **Robust Password Security**: Integrates secure pbkdf2-sha256/scrypt password hashing natively via `werkzeug.security` routines inside Flask, protecting patients' access endpoints.
2. **Cryptographically Signed JSON Web Tokens**: Signs JSON objects securely using `HS256` HMAC with a high-entropy `JWT_SECRET_KEY` signature.
3. **Role-Guard Gates**: Decorators check decoded user session payload identities to enforce permission parameters (e.g., patient-only profile telemetry vs. caregiver-only rosters).
4. **Active Care Circle Roster**: SQLite storage linking caregivers to patients they have explicit permission to monitor.

---

## 🚀 Step-by-Step Installation

### 1. Prerequisites
Ensure you have `python3` and `pip` installed:
```bash
python3 --version
```

### 2. Setup Virtual Environment & Install Dependencies
Run the following in your terminal inside this backend folder:
```bash
# Initialize Python Virtual Environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install essential dependencies
pip install -r requirements.txt
```

### 3. Launching the Backend Server
Run the Flask server:
```bash
python app.py
```
*The server will boot up locally at http://127.0.0.1:5000/ with debug logging enabled.*

---

## 📡 API Specification & curl Commands Example Walkthrough

### 1. Pre-seed Database (Database Bootstrapping Option)
Convenient setup utility to quickly compile standard SQLite tables and populate mock patient & caregiver profiles.
* **Route**: `POST /api/init-database`
```bash
curl -X POST http://127.0.0.1:5000/api/init-database
```

---

### 2. Register New Patient or Caregiver
Adds user securely into SQLite database, hashing the raw password container.
* **Route**: `POST /api/auth/register`
* **JSON Body Parameters**:
  * `email` (string, required)
  * `password` (string, required)
  * `name` (string, required)
  * `role` (string, required: "Patient" | "Caregiver")
  * `age`, `bloodGroup`, `medicalConditions` (optional fields)

**Execute Patient Registration:**
```bash
curl -X POST http://127.0.0.1:5000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "sarah.patient@example.com",
    "password": "securePassword123",
    "name": "Sarah Connor",
    "role": "Patient",
    "age": 32,
    "bloodGroup": "A-",
    "medicalConditions": "Chronic Stress, Hypertension"
  }'
```

**Execute Caregiver Registration:**
```bash
curl -X POST http://127.0.0.1:5000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.care@example.com",
    "password": "caregiverPass123",
    "name": "Dr. John Connor",
    "role": "Caregiver"
  }'
```

---

### 3. Log In & Retrieve JWT Access Token
Validates email and hashed passwords. Returns a JSON dictionary including the signed JWT bearer token credentials.
* **Route**: `POST /api/auth/login`
```bash
# Log in as Caregiver to obtain security token
curl -X POST http://127.0.0.1:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.care@example.com",
    "password": "caregiverPass123"
  }'
```
*Response will output:*
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ...",
  "expiresIn": 86400,
  "user": {
    "email": "john.care@example.com",
    "name": "Dr. John Connor",
    "role": "Caregiver"
  }
}
```

---

### 4. Fetch Secure Profile Details (Protected Patient/Caregiver Route)
Uses `@token_required` decorator to validate the incoming JWT token signature inside the HTTP headers.
* **Route**: `GET /api/user/profile`
* **Header Required**: `Authorization: Bearer <TOKEN>`
```bash
curl -X GET http://127.0.0.1:5000/api/user/profile \
  -H "Authorization: Bearer <PASTE_YOUR_JWT_TOKEN_HERE>"
```

---

### 5. Assign Patient to Caregiver Circle (Protected Caregiver Route)
Allows an authorized caregiver user to add a patient account email to their monitoring radar list.
* **Route**: `POST /api/caregiver/assign`
* **Header Required**: `Authorization: Bearer <CAREGIVER_JWT_TOKEN_HERE>`
* **JSON Body Parameters**:
  * `patientEmail` (string, required)

```bash
curl -X POST http://127.0.0.1:5000/api/caregiver/assign \
  -H "Authorization: Bearer <CAREGIVER_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "patientEmail": "sarah.patient@example.com"
  }'
```

---

### 6. View My Monitored Patients (Protected Caregiver Route)
Lists all patient health records assigned to the signed-in caregiver user.
* **Route**: `GET /api/caregiver/patients`
* **Header Required**: `Authorization: Bearer <CAREGIVER_JWT_TOKEN_HERE>`
```bash
curl -X GET http://127.0.0.1:5000/api/caregiver/patients \
  -H "Authorization: Bearer <CAREGIVER_JWT_TOKEN>"
```
