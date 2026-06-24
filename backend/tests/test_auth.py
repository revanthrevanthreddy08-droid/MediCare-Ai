import os
import sys
import unittest
import json
import jwt

# Include parent directory inside Python pathway resolving imports elegantly
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from app import app, db, SecuredUser, SecuredActiveRoster, encrypt_medical_data, decrypt_medical_data
from middleware.auth import JWT_REVOKED_TOKEN_STORE, ACCOUNT_LOCKOUT_REGISTRY

class MediCareAuthSecurityTestSuite(unittest.TestCase):

    def setUp(self):
        """
        Setup ephemeral, in-memory database instance context structures for each isolated test.
        """
        app.config['TESTING'] = True
        app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///:memory:'
        app.config['JWT_SECRET_KEY'] = 'test_suite_exclusive_cryptographic_verification_secret_2026'
        
        self.app_client = app.test_client()
        
        with app.app_context():
            db.create_all()
            
        # Reset local cache memories between tests
        JWT_REVOKED_TOKEN_STORE.clear()
        ACCOUNT_LOCKOUT_REGISTRY.clear()

    def tearDown(self):
        """
        Destroy context states cleanly once verification finishes.
        """
        with app.app_context():
            db.session.remove()
            db.drop_all()

    def test_secure_user_registration_success(self):
        """
        Verifies that a valid registration payload is sanitized, securely persisted,
        and sensitive health records are encrypted at rest.
        """
        payload = {
            "email": "jane.patient@example.com",
            "password": "Password123!",
            "name": "Jane Doe",
            "role": "Patient",
            "age": 30,
            "gender": "Female",
            "bloodGroup": "B+",
            "allergies": "Gluten, Latex",
            "medicalConditions": "Type-1 Diabetes",
            "emergencyContact": "Dr. Sarah Thompson (555-0100)"
        }
        
        resp = self.app_client.post('/api/auth/register', 
                                    data=json.dumps(payload),
                                    content_type='application/json')
        
        self.assertEqual(resp.status_code, 201)
        data = json.loads(resp.data)
        self.assertTrue(data['success'])
        self.assertEqual(data['user']['email'], "jane.patient@example.com")
        self.assertEqual(data['user']['allergies'], "Gluten, Latex")
        
        # Verify direct database states: Password must only sit as hash, and health history must be encrypted
        with app.app_context():
            user = SecuredUser.query.filter_by(email="jane.patient@example.com").first()
            self.assertIsNotNone(user)
            self.assertNotEqual(user.password_hash, "Password123!")
            self.assertNotEqual(user.encrypted_allergies, "Gluten, Latex")
            # Ensure decryption resolves perfectly back to original strings
            self.assertEqual(decrypt_medical_data(user.encrypted_allergies), "Gluten, Latex")

    def test_strong_password_policy_enforcement(self):
        """
        Asserts that passwords violating length or character complexity standards are rejected.
        """
        unmet_complexity_payload = {
            "email": "weak.pw@example.com",
            "password": "simple123", # simple, short, no uppercase or specials
            "name": "Simple Pete",
            "role": "Patient"
        }
        
        resp = self.app_client.post('/api/auth/register',
                                    data=json.dumps(unmet_complexity_payload),
                                    content_type='application/json')
        
        self.assertEqual(resp.status_code, 400)
        data = json.loads(resp.data)
        self.assertIn("Validation issue", data['message'])

    def test_robust_login_and_token_distribution(self):
        """
        Verifies that valid credential validation issues a short-lived access token and refresh token.
        """
        # Register user first
        reg_payload = {
            "email": "alex.test@example.com",
            "password": "ComplexPassword99!",
            "name": "Alex Test",
            "role": "Patient"
        }
        self.app_client.post('/api/auth/register', data=json.dumps(reg_payload), content_type='application/json')
        
        # Perform Login query
        login_payload = {
            "email": "alex.test@example.com",
            "password": "ComplexPassword99!"
        }
        resp = self.app_client.post('/api/auth/login', data=json.dumps(login_payload), content_type='application/json')
        
        self.assertEqual(resp.status_code, 200)
        data = json.loads(resp.data)
        self.assertTrue(data['success'])
        self.assertIn('accessToken', data)
        self.assertIn('refreshToken', data)

    def test_account_lockout_after_multiple_failed_login_attempts(self):
        """
        Verifies brute force mitigation: accounts are locked out after 5 consecutive failed logins.
        """
        # Register a valid user
        reg_payload = {
            "email": "lockout.target@example.com",
            "password": "TargetPassword88!",
            "name": "Lockout Victim",
            "role": "Patient"
        }
        self.app_client.post('/api/auth/register', data=json.dumps(reg_payload), content_type='application/json')
        
        # Attack account with 5 consecutive incorrect passwords
        brute_payload = {
            "email": "lockout.target@example.com",
            "password": "IncorrectPassword"
        }
        
        for _ in range(5):
             resp = self.app_client.post('/api/auth/login', data=json.dumps(brute_payload), content_type='application/json')
             self.assertEqual(resp.status_code, 401)
             
        # The 6th login attempt must trigger an explicit lockout status block (HTTP 423 Locked)
        locked_resp = self.app_client.post('/api/auth/login', data=json.dumps(brute_payload), content_type='application/json')
        self.assertEqual(locked_resp.status_code, 423)
        self.assertIn("locked temporarily", json.loads(locked_resp.data)['message'])

    def test_role_based_access_controls_enforcement(self):
        """
        Verifies that access to exclusive caregiver routes is guarded securely from unauthorized patients.
        """
        # Register standard patient user
        reg_payload = {
            "email": "common.patient@example.com",
            "password": "PatientPassword1!",
            "name": "Patient Bob",
            "role": "Patient"
        }
        self.app_client.post('/api/auth/register', data=json.dumps(reg_payload), content_type='application/json')
        
        # Log in to fetch Patient JWT Access token
        login_payload = {
            "email": "common.patient@example.com",
            "password": "PatientPassword1!"
        }
        login_resp = self.app_client.post('/api/auth/login', data=json.dumps(login_payload), content_type='application/json')
        patient_token = json.loads(login_resp.data)['accessToken']
        
        # Attempt to access the secure caregiver roster route using the Patient token
        headers = {
             'Authorization': f'Bearer {patient_token}'
        }
        restricted_resp = self.app_client.get('/api/caregiver/patients', headers=headers)
        
        # Access must be forbidden with standard 403 response
        self.assertEqual(restricted_resp.status_code, 403)
        self.assertIn("Insufficient clearance level", json.loads(restricted_resp.data)['message'])

    def test_jwt_logout_and_token_revocation(self):
        """
        Tests secure logout utility which records token inside revocation registry, disabling re-use.
        """
        # Register and Login
        reg_payload = {
            "email": "logout.bob@example.com",
            "password": "BobSecurePassword22!",
            "name": "Bob Logout",
            "role": "Patient"
        }
        self.app_client.post('/api/auth/register', data=json.dumps(reg_payload), content_type='application/json')
        
        login_resp = self.app_client.post('/api/auth/login', data=json.dumps({
             "email": "logout.bob@example.com",
             "password": "BobSecurePassword22!"
        }), content_type='application/json')
        
        token = json.loads(login_resp.data)['accessToken']
        
        headers = {
             'Authorization': f'Bearer {token}'
        }
        
        # Verify first that authenticated queries pass perfectly
        profile_resp = self.app_client.get('/api/user/profile', headers=headers)
        self.assertEqual(profile_resp.status_code, 200)
        
        # Fire secure logout route
        logout_resp = self.app_client.post('/api/auth/logout', headers=headers)
        self.assertEqual(logout_resp.status_code, 200)
        
        # Re-attempting to read profile with the same token must be instantly blocked
        blocked_profile_resp = self.app_client.get('/api/user/profile', headers=headers)
        self.assertEqual(blocked_profile_resp.status_code, 401)
        self.assertIn("invalidated by logout", json.loads(blocked_profile_resp.data)['message'])


if __name__ == '__main__':
    unittest.main()
