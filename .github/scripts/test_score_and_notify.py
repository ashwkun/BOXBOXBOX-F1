import os
import json
import hashlib
import datetime
import re
import requests
import xml.etree.ElementTree as ET
import firebase_admin
from firebase_admin import credentials, messaging

# --- Configuration ---
RSS_URL = "https://www.motorsport.com/rss/f1/news/"
STATE_FILE = "notification_state.json"
FCM_TOPIC = "all_users"

# =============================================================================
# TEST MODE - RELAXED THRESHOLDS
# =============================================================================

# --- Scoring Constants ---
NUCLEAR_SCORE = 999
MAJOR_THRESHOLD = 250   
DIGEST_THRESHOLD = 500  
DIGEST_COMBINED_THRESHOLD = 30

# --- Time Windows (24/7 for Testing) ---
SLOT1_START_HOUR = 0; SLOT1_START_MIN = 0; SLOT1_END_HOUR = 23; SLOT1_END_MIN = 59
SLOT2_START_HOUR = 0; SLOT2_START_MIN = 0; SLOT2_END_HOUR = 23; SLOT2_END_MIN = 59
DIGEST_START_HOUR = 0; DIGEST_START_MIN = 0; DIGEST_END_HOUR = 23; DIGEST_END_MIN = 59

# --- Patterns (Abbreviated for length - ensure your full list is here) ---
NUCLEAR_PATTERNS = [
    r"\b(crash|accident|injured|hospitalized|fatal|death|died)\b",
    r"\b(red flag|red-flagged)\b",
    r"\b(cancelled|postponed)\b.*\b(race|grand prix|gp|session)\b",
    r"\b(leaves|exits|departs|replaced)\b.*\b(red bull|ferrari|mercedes|mclaren)\b"
]

MAJOR_PATTERNS = [
    (110, r"\b(dominates|dominated|dominating)\b.*\b(grand prix|race|gp)\b"),
    (115, r"\b(signs|signed|confirms|confirmed|joins|joined)\b.*\b(2025|2026|contract)\b"),
    (120, r"\b(team principal|tp|ceo)\b.*\b(leaves|joins|appointed)\b"),
    (100, r"\b(disqualified|dsq)\b"),
    (95, r"\b(grid drop|grid penalty)\b")
]

MEDIUM_PATTERNS = [
    (75, r"\b(qualifying)\b.*\b(report|result)\b"),
    (70, r"\b(fastest|quickest)\b.*\b(qualifying|q1|q2|q3)\b"),
    (65, r"\b(grid|starting grid)\b")
]

LOW_PATTERNS = [
    (35, r"\b(interview|speaks|says|comments)\b"),
    (30, r"\b(believes|expects|hopes|predicts)\b")
]

NEGATIVE_PATTERNS = [
    (-40, r"\b(caption competition)\b"),
    (-30, r"\b(rate the race)\b")
]

# --- Helper Functions ---

def load_state():
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, 'r') as f:
            state = json.load(f)
            if 'nuclear_queue' not in state: state['nuclear_queue'] = []
            return state
    return {
        "date": "1970-01-01",
        "nuclear_sent": [], "nuclear_queue": [],
        "major_sent": [], "slot1_remaining": 1, "slot2_remaining": 2,
        "digest_items": [], "digest_sent": False, "ignored_items": []
    }

def save_state(state):
    with open(STATE_FILE, 'w') as f:
        json.dump(state, f, indent=2)

def generate_id(title, pub_date):
    return hashlib.md5((title + pub_date).encode('utf-8')).hexdigest()

def score_headline(title):
    # Check Nuclear
    for pattern in NUCLEAR_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            return NUCLEAR_SCORE, "nuclear"

    score = 0
    for points, pattern in MAJOR_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE): score += points
    for points, pattern in MEDIUM_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE): score += points
    for points, pattern in LOW_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE): score += points
    for points, pattern in NEGATIVE_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE): score += points
            
    category = "ignore"
    if score >= MAJOR_THRESHOLD: category = "major"
    elif score >= DIGEST_THRESHOLD: category = "digest"
        
    return score, category

def init_firebase():
    cred_json = os.environ.get("FIREBASE_CREDENTIALS")
    if cred_json:
        try:
            cred_dict = json.loads(cred_json)
            cred = credentials.Certificate(cred_dict)
            firebase_admin.initialize_app(cred)
            return True
        except Exception as e:
            print(f"[ERROR] Error initializing Firebase: {e}")
            return False
    return False

# --- CRITICAL FIX: Updated Notification Function ---
def send_fcm_notification(title, body, data, priority="high", channel_id="f1_updates_channel_v3", image_url=None):
    print(f"[INFO] Sending FCM Notification (data-only): {title}")
    
    # Ensure data is a dict
    if data is None:
        data = {}
    
    # Add title and body to data payload for onMessageReceived to handle
    data["title"] = title
    data["body"] = body
    data["channel_id"] = channel_id
    
    # Validate Image URL
    final_image = image_url
    if image_url and not image_url.startswith('https://'):
        final_image = None
    if final_image:
        data["image_url"] = final_image
    
    try:
        android_config = messaging.AndroidConfig(
            priority=priority
        )
        
        # DATA-ONLY message - no notification payload!
        # This ensures onMessageReceived() is ALWAYS called
        message = messaging.Message(
            data=data,
            topic=FCM_TOPIC,
            android=android_config
        )
        response = messaging.send(message)
        print(f"  [SUCCESS] Message sent: {response}")
        return True
    except Exception as e:
        print(f"  [ERROR] Error sending message: {e}")
        return False

def is_in_nuclear_quiet_hours(current_time):
    # Test Mode: Assuming quiet hours logic still applies or is disabled.
    # Logic: 01:00 AM - 08:00 AM IST
    hour = current_time.hour
    minute = current_time.minute
    if hour > 19 or (hour == 19 and minute >= 30): return True
    if hour < 2 or (hour == 2 and minute < 30): return True
    return False

# --- Main Logic ---

def main():
    print("="*80)
    print("TEST MODE - Using f1_updates_channel_v3 for all notifications")
    print("="*80)
    
    state = load_state()
    current_date_str = datetime.datetime.utcnow().strftime('%Y-%m-%d')
    
    if state['date'] != current_date_str:
        print(f"[INFO] New day detected. Resetting state.")
        state['date'] = current_date_str
        state['slot1_remaining'] = 1; state['slot2_remaining'] = 2
        state['major_sent'] = []; state['nuclear_sent'] = []; state['digest_sent'] = False

    if not init_firebase(): return

    # Fetch RSS
    try:
        response = requests.get(RSS_URL)
        response.raise_for_status()
        root = ET.fromstring(response.content)
    except Exception as e:
        print(f"[ERROR] RSS Fetch failed: {e}")
        return

    channel = root.find('channel')
    items = channel.findall('item')
    print(f"[DEBUG] Found {len(items)} items in RSS feed.")
    
    current_time = datetime.datetime.utcnow()
    
    nuclear_candidates = []
    major_candidates = []
    
    # 1. Process Items
    for item in items:
        title = item.find('title').text
        link = item.find('link').text
        pub_date_str = item.find('pubDate').text
        
        # Image extraction
        image_url = None
        enclosure = item.find('enclosure')
        if enclosure is not None and enclosure.get('type', '').startswith('image/'):
            image_url = enclosure.get('url')
        
        item_id = generate_id(title, pub_date_str)
        
        # Check duplicates
        is_sent = False
        for sent in state['nuclear_sent'] + state['major_sent'] + state['digest_items']:
            if sent['id'] == item_id:
                is_sent = True
                break
        if is_sent:
            print(f"[DEBUG] Skipping duplicate (already sent): {title}")
            continue
        if item_id in state['ignored_items']:
            print(f"[DEBUG] Skipping ignored item: {title}")
            continue

        score, category = score_headline(title)
        print(f"[DEBUG] Item: {title} | Score: {score} | Category: {category}")
        
        item_data = {
            "id": item_id, "title": title, "url": link, 
            "score": score, "image": image_url, "pub_date": pub_date_str
        }

        if category == "nuclear":
            nuclear_candidates.append(item_data)
        elif category == "major":
            major_candidates.append(item_data)

    # --- FORCE TEST NOTIFICATION ---
    # Inject a fake item to ensure we get a notification for testing
    test_id = f"test_notification_{int(current_time.timestamp())}"
    test_item = {
        "id": test_id,
        "title": f"Test Notification {current_time.strftime('%H:%M:%S')}",
        "url": "https://www.motorsport.com/f1/news/",
        "score": "100",
        "image": None,
        "pub_date": current_time.strftime('%Y-%m-%d %H:%M:%S')
    }
    print(f"[DEBUG] Injecting forced test item: {test_item['title']}")
    nuclear_candidates.append(test_item)
    # -------------------------------
        # (Digest logic omitted for brevity, but follows same pattern)

    # 2. Process Nuclear (Fixed Channel ID)
    in_quiet_hours = is_in_nuclear_quiet_hours(current_time)
    
    # Process Queue
    if not in_quiet_hours and state['nuclear_queue']:
        for item in list(state['nuclear_queue']):
            if send_fcm_notification(
                title="F1 News",
                body=f"🚨 {item['title']}",
                data={"type": "nuclear", "url": item['url'], "score": str(item['score'])},
                channel_id="f1_updates_channel_v3", # FIXED ID
                image_url=item.get('image')
            ):
                state['nuclear_sent'].append(item)
                state['nuclear_queue'].remove(item)

    # Process New Nuclear
    for item in nuclear_candidates:
        if in_quiet_hours:
            state['nuclear_queue'].append(item)
        else:
            if send_fcm_notification(
                title="F1 News",
                body=f"🚨 {item['title']}",
                data={"type": "nuclear", "url": item['url'], "score": str(item['score'])},
                channel_id="f1_updates_channel_v3", # FIXED ID
                image_url=item.get('image')
            ):
                state['nuclear_sent'].append(item)

    # 3. Process Major (Fixed Channel ID)
    # Simple send logic for test mode (ignoring slots for now as per "Test Mode" intent)
    for item in major_candidates:
        if send_fcm_notification(
            title="F1 Major Update",
            body=f"📢 {item['title']}",
            data={"type": "major", "url": item['url'], "score": str(item['score'])},
            channel_id="f1_updates_channel_v3", # FIXED ID
            image_url=item.get('image')
        ):
            state['major_sent'].append(item)

    save_state(state)
    print("[INFO] Run complete.")

if __name__ == "__main__":
    main()
