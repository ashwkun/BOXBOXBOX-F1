import argparse
import firebase_admin
from firebase_admin import credentials, messaging
import json
import os
import sys
from datetime import datetime

def init_firebase():
    """Initialize Firebase from Env Var"""
    cred_json = os.environ.get("FIREBASE_CREDENTIALS")
    if not cred_json:
        print("[ERROR] FIREBASE_CREDENTIALS environment variable not set.")
        sys.exit(1)
    
    try:
        cred_dict = json.loads(cred_json)
        cred = credentials.Certificate(cred_dict)
        firebase_admin.initialize_app(cred)
        print("[INFO] Firebase initialized successfully.")
    except Exception as e:
        print(f"[ERROR] Firebase init failed: {e}")
        sys.exit(1)

def send_notification(args):
    """Send the notification using Data payload for custom sound support"""
    print(f"[INFO] Preparing to send: '{args.title}'")
    
    # Construct Message
    # We use DATA payload to ensure onMessageReceived is triggered in the app,
    # which is where the custom sound logic resides.
    message = messaging.Message(
        topic="all_users",
        data={
            "title": args.title,
            "body": args.body,
            "channel_id": args.channel,
            "target_tab": args.target_tab,
            "url": args.url if args.url else "",
            "image_url": args.image_url if args.image_url else "",
            "type": "manual_dispatch",
            "timestamp": datetime.utcnow().isoformat()
        },
        android=messaging.AndroidConfig(
            priority=args.priority,
            ttl=3600
        )
    )
    
    try:
        response = messaging.send(message)
        print(f"[SUCCESS] Notification sent! ID: {response}")
    except Exception as e:
        print(f"[ERROR] Failed to send notification: {e}")
        sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Send Manual F1 Notification")
    
    parser.add_argument("--title", required=True, help="Notification Title")
    parser.add_argument("--body", required=True, help="Notification Body")
    parser.add_argument("--channel", default="f1_major", choices=["f1_major", "f1_nuclear", "f1_digest", "f1_app_updates"], help="Notification Channel (Sound)")
    parser.add_argument("--priority", default="high", choices=["high", "normal"], help="FCM Priority")
    parser.add_argument("--target_tab", default="news", help="App tab to open")
    parser.add_argument("--url", default="", help="Deep link URL (e.g. YouTube link)")
    parser.add_argument("--image_url", default="", help="Image URL (optional)")
    
    args = parser.parse_args()
    
    init_firebase()
    send_notification(args)
