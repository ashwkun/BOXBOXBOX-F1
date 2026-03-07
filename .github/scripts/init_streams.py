import os
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import json

def initialize_firebase():
    """Initialize Firebase Admin SDK using the credentials JSON stored in environment"""
    try:
        cred_json = os.environ.get('FIREBASE_CREDENTIALS')
        if not cred_json:
            raise ValueError("FIREBASE_CREDENTIALS environment variable not set")
            
        cred_dict = json.loads(cred_json)
        cred = credentials.Certificate(cred_dict)
        
        # Check if already initialized
        if not firebase_admin._apps:
            # We must specify the database map URL for RTDB
            firebase_admin.initialize_app(cred, {
                'databaseURL': 'https://boxboxboxapp-default-rtdb.firebaseio.com/'
            })
            
        print("[SUCCESS] Firebase initialized")
    except Exception as e:
        print(f"[ERROR] Failed to initialize Firebase: {e}")
        exit(1)

def upload_stream_configs():
    print("Uploading stream configs to live_config/streams...")
    
    # The default URLs with NO custom JS right now (handled locally as fallback for now if empty)
    streams = [
        {
            "name": "sky",
            "label": "SKY F1",
            "url": "https://php.adffdafdsafds.sbs/channel/SkySportsF1%5BUK%5D",
            "customCss": "",
            "customJs": ""
        },
        {
            "name": "f1tv",
            "label": "F1 TV",
            "url": "https://hakunamatata5.org/hakunamatata5.html",
            "customCss": "",
            "customJs": ""
        },
        {
            "name": "other",
            "label": "OTHER",
            "url": "https://embedsports.top/embed/admin/ppv-australian-grand-prix-practice-3/1",
            "customCss": "",
            "customJs": ""
        }
    ]
    
    ref = db.reference("live_config/streams")
    ref.set(streams)
    
    print("[SUCCESS] initial streaming configuration uploaded to Realtime Database!")

if __name__ == "__main__":
    initialize_firebase()
    upload_stream_configs()
