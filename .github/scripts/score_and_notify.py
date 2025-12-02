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
FCM_TOPIC = "all_users" # Target topic for notifications

# --- Scoring Constants (PRODUCTION) ---
NUCLEAR_SCORE = 999
MAJOR_THRESHOLD = 85  # High threshold for major news
DIGEST_THRESHOLD = 40 # Moderate threshold for digest
DIGEST_COMBINED_THRESHOLD = 150  # Sum of top 3 digest items must exceed this

# --- Time Windows (IST / UTC) ---
# Slot 1: 13:00-14:00 IST (07:30-08:30 UTC) - 1 notification slot
# Slot 2: 21:00-22:00 IST (15:30-16:30 UTC) - 2 notification slots 
# Digest: 08:00-09:00 IST (02:30-03:30 UTC)

SLOT1_START_HOUR = 7   # UTC
SLOT1_START_MIN = 30
SLOT1_END_HOUR = 8
SLOT1_END_MIN = 30

SLOT2_START_HOUR = 15  # UTC
SLOT2_START_MIN = 30
SLOT2_END_HOUR = 16
SLOT2_END_MIN = 30

DIGEST_START_HOUR = 2  # UTC
DIGEST_START_MIN = 30
DIGEST_END_HOUR = 3
DIGEST_END_MIN = 30

# --- Patterns (Regex) ---
# LENIENT NUCLEAR PATTERNS - Can be tuned down based on user feedback
NUCLEAR_PATTERNS = [
    # Safety/Critical Events
    r"\b(crash|accident|injured|hospitalized|fatal|death|died)\b",
    r"\b(red flag|red-flagged)\b",
    r"\b(cancelled|postponed)\b.*\b(race|grand prix|gp|session)\b",
    
    # Race Results (Lenient - includes wins, poles, sprint)
    r"\b(wins|won|victory|victorious)\b.*\b(grand prix|race|gp)\b",
    r"\b(pole position|takes pole|claims pole|grabs pole|snatches pole)\b",
    r"\b(sprint)\b.*\b(win|wins|won|victory)\b",
    
    # Championships
    r"\b(clinches|secures|wins|seals)\b.*\b(championship|title|wdc|wcc)\b",
    r"\b(mathematically|officially)\b.*\b(eliminated|out of contention)\b",
    
    # Disqualifications/Bans
    r"\b(disqualified)\b.*\b(race|grand prix|gp)\b",
    r"\b(banned|suspended)\b.*\b(driver|team|races)\b",
    
    # Major Team/Driver Changes
    r"\b(leaves|exits|departs|replaced)\b.*\b(red bull|ferrari|mercedes|mclaren)\b",
    r"\b(horner|wolff|vasseur|brown|stella)\b.*\b(leaves|exits|departs|replaced)\b",
    r"\b(retires|retirement|retiring)\b.*\b(from racing|from f1|from formula)\b",
    r"\b(announces retirement)\b",
    
    # Records
    r"\b(breaks? record|all-time|historic|history-making)\b.*\b(win|pole|podium|fastest)\b",
    r"\b(most wins|most poles|most podiums)\b",
    
    # Team Changes
    r"\b(team.*withdraw|leaving f1|exits formula 1)\b",
    r"\b(new team|team entry|joins f1|entering formula 1)\b",
    r"\b(sold|bought|ownership|takeover)\b.*\b(red bull|ferrari|mercedes|mclaren)\b",
    
    # All Driver Moves (Inter-team and Intra-team)
    r"\b(moves to|joins|signs for|promoted to|in at|switches to|signed by)\b.*\b(red bull|ferrari|mercedes|mclaren|aston martin|alpine|williams|haas|sauber|audi|racing bulls|rb|alphatauri)\b",
    r"\b(replaces|replacing)\b.*\b(driver|seat)\b",
    r"\b(contract)\b.*\b(extension|extended|renewed|signed)\b",

    # Regulatory
    r"\b(cost cap|budget cap)\b.*\b(breach|violation|exceeded)\b",
    r"\b(regulation change|rule change|technical directive)\b.*\b(2025|2026|immediate)\b",
    r"\b(calendar)\b.*\b(added|removed|cancelled|replaced)\b",
    r"\b(illegal|non-compliant|technical infringement)\b.*\b(car|component)\b",
    r"\b(protest|appeal)\b.*\b(upheld|successful|overturned)\b"
]

MAJOR_PATTERNS = [
    (110, r"\b(dominates|dominated|dominating)\b.*\b(grand prix|race|gp)\b"),
    (85, r"\b(podium)\b"),
    (105, r"\b(can clinch)\b.*\b(championship)\b"),
    (100, r"\b(championship|title)\b.*\b(lead|ahead|battle|fight|deficit|gap)\b"),
    (90, r"\b(points lead|points gap|points deficit|points ahead)\b"),
    (115, r"\b(signs|signed|confirms|confirmed|joins|joined)\b.*\b(2025|2026|2027|contract|deal)\b"),
    (110, r"\b(official:)\b.*\b(driver|seat|signs|joins)\b"),
    (105, r"\b(replaces|replacing|replacement)\b.*\b(driver|seat)\b"),
    (100, r"\b(confirms|confirmed)\b.*\b(driver|lineup|seat)\b"),
    (120, r"\b(team principal|tp|ceo)\b.*\b(leaves|joins|appointed|names|confirms)\b"),
    (115, r"\b(horner|wolff|vasseur|brown|stella|newey)\b.*\b(leaves|exits|joins|appointed)\b"),
    (100, r"\b(disqualified|dsq)\b"),
    (95, r"\b(grid drop|grid penalty|grid-place penalty)\b"),
    (90, r"\b(penalty|penalised|penalized)\b.*\b(grid|race|time|points|seconds)\b"),
    (85, r"\b(stewards)\b.*\b(decision|ruling|penalty|explain)\b"),
    (90, r"\b(protest|appeal)\b.*\b(accepted|upheld|dismissed|successful)\b")
]

MEDIUM_PATTERNS = [
    (75, r"\b(qualifying)\b.*\b(report|result)\b"),
    (70, r"\b(fastest|quickest|tops|leads)\b.*\b(qualifying|q1|q2|q3)\b"),
    (65, r"\b(grid|starting grid|grid positions)\b"),
    (75, r"\b(team orders)\b"),
    (70, r"\b(sprint race)\b.*\b(report|result)\b"),
    (65, r"\b(sprint)\b.*\b(pole position|pole)\b"),
    (60, r"\b(fastest|tops|leads)\b.*\b(practice|fp1|fp2|fp3)\b"),
    (70, r"\b(upgrade|upgrades|update|updates)\b.*\b(car|package|floor|wing|aero)\b"),
    (65, r"\b(strategy|pit stop|tyre|tire)\b.*\b(briefing|problem|issue|concern)\b"),
    (60, r"\b(pace|performance|balance)\b.*\b(issue|problem|struggle|concern)\b"),
    (60, r"\b(verstappen|norris|hamilton|leclerc|piastri|sainz|russell)\b.*\b(says|admits|reveals|claims|insists)\b"),
    (69, r"\b(collision|incident|contact|clash)\b.*\b(penalty|investigation|cleared|reprimand)\b"),
    (65, r"\b(red bull|ferrari|mercedes|mclaren)\b.*\b(upgrade|issue|problem|pace)\b"),
    (55, r"\b(driver ratings|interactive data|analysis)\b")
]

LOW_PATTERNS = [
    (35, r"\b(interview|speaks|says|comments|admits|reveals|insists|denies|explains|clarifies)\b"),
    (30, r"\b(believes|expects|hopes|predicts|thinks)\b"),
    (40, r"\b(practice|fp1|fp2|fp3)\b"),
    (25, r"\b(as it happened|live updates|live blog)\b"),
    (30, r"\b(preview|talking points|what to watch)\b"),
    (30, r"\b(how to watch|tv times|viewing guide)\b"),
    (40, r"\b(strategy briefing|race preview)\b"),
    (40, r"\b(qualifying data|race data|lap time data)\b"),
    (35, r"\b(data|stats|statistics|analysis)\b"),
    (40, r"\b(alonso|ocon|gasly|hulkenberg|tsunoda|stroll|albon|bearman|lawson)\b"),
    (35, r"\b(antonelli|hadjar|bortoleto|colapinto|doohan)\b"),
    (30, r"\b(history|historic|past|previous|2024|last year)\b"),
    (25, r"\b(top five|top ten|top 10|top 5)\b"),
    (25, r"\b(fans|vote|poll|caption competition|round-up)\b"),
    (25, r"\b(pictures|photos|images|gallery|in pictures)\b")
]

NEGATIVE_PATTERNS = [
    (-40, r"\b(caption competition)\b"),
    (-35, r"\b(round-up|roundup)\b"),
    (-30, r"\b(review)\b.*\b(book|game|documentary)\b"),
    (-25, r"\b(top 5|top 10|ranked|ranking|list)\b"),
    (-40, r"\b(pictures only|photos only|images only|gallery)\b"),
    (-30, r"\b(in pictures|qualifying day in pictures)\b"),
    (-25, r"\b(as it happened)\b"),
    (-30, r"\b(throwback|years ago|historic|history|past)\b"),
    (-35, r"\b(2023|2022|2021|2020|2019)\b"),
    (-20, r"\b(rumour|rumor|speculation|could|might|may|possible|potential)\b"),
    (-15, r"\b(report:|report says|reports suggest)\b"),
    (-30, r"\b(rate the race)\b"),
    (-30, r"\b(vote for|poll:|voting)\b"),
    (-20, r"\b(fans|fan reaction|social media|twitter|instagram)\b"),
    (-25, r"\b(indycar|f2|f3|f4|formula 2|formula 3|formula 4|formula e|wec|fe)\b"),
    (-30, r"\b(moto gp|motogp)\b")
]

# --- Helper Functions ---

def load_state():
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, 'r') as f:
            state = json.load(f)
            # Migrate old state format if needed
            if 'major_slots_remaining' in state:
                # Old format - convert to new slot system
                state['slot1_remaining'] = state.get('slot1_remaining', 1)
                state['slot2_remaining'] = state.get('slot2_remaining', 2)
                if 'major_slots_remaining' in state:
                    del state['major_slots_remaining']
                if 'major_slots_used' in state:
                    del state['major_slots_used']
            # Ensure nuclear_queue exists
            if 'nuclear_queue' not in state:
                state['nuclear_queue'] = []
            return state
    return {
        "date": "1970-01-01",
        "nuclear_sent": [],
        "nuclear_queue": [],  # Queue for nuclear items during quiet hours
        "major_sent": [],
        "slot1_remaining": 1,
        "slot2_remaining": 2,
        "digest_items": [],
        "digest_sent": False,
        "ignored_items": []
    }

def save_state(state):
    with open(STATE_FILE, 'w') as f:
        json.dump(state, f, indent=2)

def generate_id(title, pub_date):
    return hashlib.md5((title + pub_date).encode('utf-8')).hexdigest()

def score_headline(title):
    print(f"  [DEBUG] Scoring: '{title}'")
    # Check Nuclear
    for pattern in NUCLEAR_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            print(f"    [MATCH] Nuclear pattern: '{pattern}'")
            return NUCLEAR_SCORE, "nuclear"

    score = 0
    
    # Add Points
    for points, pattern in MAJOR_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            print(f"    [MATCH] Major pattern ({points} pts): '{pattern}'")
            score += points
    for points, pattern in MEDIUM_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            print(f"    [MATCH] Medium pattern ({points} pts): '{pattern}'")
            score += points
    for points, pattern in LOW_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            print(f"    [MATCH] Low pattern ({points} pts): '{pattern}'")
            score += points
            
    # Subtract Points
    for points, pattern in NEGATIVE_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            print(f"    [MATCH] Negative pattern ({points} pts): '{pattern}'")
            score += points
            
    category = "ignore"
    if score >= MAJOR_THRESHOLD:
        category = "major"
    elif score >= DIGEST_THRESHOLD:
        category = "digest"
        
    print(f"    [RESULT] Final Score: {score} | Category: {category}")
    return score, category

def validate_image_url(image_url):
    """
    Validate image URL for FCM compatibility.
    FCM requirements:
    - Must be HTTPS
    - Must be publicly accessible
    - Must be under 1MB
    - Should respond within reasonable time
    """
    if not image_url:
        return None
    
    # Check HTTPS
    if not image_url.startswith('https://'):
        print(f"  [WARN] Image URL not HTTPS, skipping: {image_url}")
        return None
    
    try:
        # HEAD request to check size and accessibility
        response = requests.head(image_url, timeout=5, allow_redirects=True)
        
        if response.status_code != 200:
            print(f"  [WARN] Image URL returned {response.status_code}, skipping: {image_url}")
            return None
        
        # Check size (FCM limit is ~1MB, be conservative)
        content_length = response.headers.get('content-length')
        if content_length:
            size_mb = int(content_length) / (1024 * 1024)
            if size_mb > 0.9:  # 900KB limit for safety
                print(f"  [WARN] Image too large ({size_mb:.2f}MB), skipping: {image_url}")
                return None
        
        print(f"  [INFO] Image validated: {image_url}")
        return image_url
        
    except requests.exceptions.Timeout:
        print(f"  [WARN] Image URL timeout, skipping: {image_url}")
        return None
    except Exception as e:
        print(f"  [WARN] Image validation failed ({e}), skipping: {image_url}")
        return None

def init_firebase():
    cred_json = os.environ.get("FIREBASE_CREDENTIALS")
    if cred_json:
        try:
            cred_dict = json.loads(cred_json)
            cred = credentials.Certificate(cred_dict)
            firebase_admin.initialize_app(cred)
            print("[INFO] Firebase initialized successfully.")
            return True
        except Exception as e:
            print(f"[ERROR] Error initializing Firebase: {e}")
            return False
    else:
        print("[ERROR] FIREBASE_CREDENTIALS env var not found.")
        return False

def send_fcm_notification(title, body, data, priority="high", channel_id="f1_major", image_url=None):
    print(f"[INFO] Sending FCM Notification (data-only): {title}")
    
    # Add title and body to data payload for onMessageReceived to handle
    if data is None:
        data = {}
    data["title"] = title
    data["body"] = body
    data["channel_id"] = channel_id
    
    # Validate image URL
    final_image = image_url
    if image_url and not image_url.startswith('https://'):
        print(f"  [WARN] Image URL is not HTTPS, skipping image: {image_url}")
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

# ... (generate_digest_title and get_emoji_for_item remain unchanged) ...

# ... (is_in_nuclear_quiet_hours remains unchanged) ...

# ... (is_in_slot1_window etc remain unchanged) ...

# ...

    # 4. Process Nuclear Items (Respect Quiet Hours)
    in_quiet_hours = is_in_nuclear_quiet_hours(current_time)
    print(f"\n[INFO] Nuclear quiet hours active: {in_quiet_hours} (12 AM - 8 AM IST)")
    
    # Send any queued nuclear items if we're outside quiet hours
    if not in_quiet_hours and state['nuclear_queue']:
        print(f"\n[INFO] Sending {len(state['nuclear_queue'])} queued nuclear notifications from quiet hours...")
        # Use a copy of the list to iterate safely
        queue_copy = list(state['nuclear_queue'])
        for item in queue_copy:
            print(f"\n[NUCLEAR QUEUED] Sending: {item['title']}")
            if send_fcm_notification(
                title="F1 News",
                body=f"🚨 {item['title']}",
                data={"type": "nuclear", "url": item['url'], "score": str(item['score']), "image": item.get('image', ''), "channel_id": "f1_nuclear"},
                priority="high",
                channel_id="f1_nuclear",
                image_url=item.get('image')
            ):
                state['nuclear_sent'].append(item)
                state['nuclear_queue'].remove(item) # Remove only if sent successfully
            else:
                print(f"  [WARN] Failed to send queued nuclear item. Keeping in queue.")
    
    # Process new nuclear items
    for item in nuclear_candidates:
        if in_quiet_hours:
            # Queue for later
            print(f"\n[NUCLEAR] Queuing for later (quiet hours): {item['title']}")
            state['nuclear_queue'].append(item)
        else:
            # Send immediately
            print(f"\n[NUCLEAR] Sending: {item['title']}")
            if send_fcm_notification(
                title="F1 News",
                body=f"🚨 {item['title']}",
                data={"type": "nuclear", "url": item['url'], "score": str(item['score']), "image": item.get('image', '')},
                priority="high",
                channel_id="f1_nuclear",
                image_url=item.get('image')
            ):
                state['nuclear_sent'].append(item)
            else:
                 print(f"  [WARN] Failed to send nuclear item. Will NOT mark as sent.")

def generate_digest_title(count, day_of_week):
    """Generate digest title based on previous day's context"""
    # Monday digest = Sunday's (race day) wrap
    # Sunday digest = Saturday's (qualifying) digest
    # Saturday digest = Friday's (practice) roundup
    # Friday digest = Pre-race week digest
    # Tue/Wed/Thu = Generic based on count
    
    if day_of_week == 'Monday':
        return f"🏆 F1 Race Day Wrap • {count} Updates"
    elif day_of_week == 'Sunday':
        return f"⚡ F1 Qualifying Digest • {count} Updates"
    elif day_of_week == 'Saturday':
        return f"🏁 F1 Practice Roundup • {count} Updates"
    elif day_of_week == 'Friday':
        return f"📋 Pre Raceweek Digest • {count} Updates"
    else:
        # Tuesday, Wednesday, Thursday - generic
        if count <= 2:
            return f"📰 F1 Quick Brief • {count} Updates"
        elif count <= 4:
            return f"📋 F1 Daily Digest • {count} Updates"
        else:
            return f"🔥 F1 Busy Day • {count} Updates"

def get_emoji_for_item(item):
    # Simple mapping based on score/content (could be refined)
    score = item.get('score', 0)
    title = item.get('title', '').lower()
    
    if 'win' in title or 'victory' in title: return "🏆"
    if 'pole' in title: return "⚡"
    if 'crash' in title or 'accident' in title: return "💥"
    if 'penalty' in title: return "⚖️"
    if 'quote' in title or 'says' in title: return "💬"
    if 'upgrade' in title: return "🔧"
    
    if score >= 75: return "⚡"
    return "📰"

def is_in_nuclear_quiet_hours(current_time):
    """
    Check if we're in nuclear notification quiet hours.
    Quiet hours: 01:00 AM - 08:00 AM IST = 19:30 - 02:30 UTC
    Crosses midnight UTC, so need special handling.
    """
    hour = current_time.hour
    minute = current_time.minute
    
    # 19:30 UTC to 23:59 UTC (evening, previous IST day)
    if hour > 19 or (hour == 19 and minute >= 30):
        return True
    
    # 00:00 UTC to 02:30 UTC (early morning, same IST day)
    if hour < 2 or (hour == 2 and minute < 30):
        return True
    
    return False

def is_in_slot1_window(current_time):
    """Check if current time is in Slot 1 window (1-2 PM IST / 07:30-08:30 UTC)"""
    hour = current_time.hour
    minute = current_time.minute
    
    if hour == SLOT1_START_HOUR and minute >= SLOT1_START_MIN:
        return True
    elif SLOT1_START_HOUR < hour < SLOT1_END_HOUR:
        return True
    elif hour == SLOT1_END_HOUR and minute < SLOT1_END_MIN:
        return True
    return False

def is_in_slot2_window(current_time):
    """Check if current time is in Slot 2 window (9-10 PM IST / 15:30-16:30 UTC)"""
    hour = current_time.hour
    minute = current_time.minute
    
    if hour == SLOT2_START_HOUR and minute >= SLOT2_START_MIN:
        return True
    elif SLOT2_START_HOUR < hour < SLOT2_END_HOUR:
        return True
    elif hour == SLOT2_END_HOUR and minute < SLOT2_END_MIN:
        return True
    return False

def is_in_digest_window(current_time):
    """Check if current time is in Digest window (8-9 AM IST / 02:30-03:30 UTC)"""
    hour = current_time.hour
    minute = current_time.minute
    
    if hour == DIGEST_START_HOUR and minute >= DIGEST_START_MIN:
        return True
    elif DIGEST_START_HOUR < hour < DIGEST_END_HOUR:
        return True
    elif hour == DIGEST_END_HOUR and minute < DIGEST_END_MIN:
        return True
    return False

# --- Main Logic ---

def main():
    print(f"[INFO] Starting run at {datetime.datetime.utcnow()}")
    
    # 1. Initialize
    state = load_state()
    current_date_str = datetime.datetime.utcnow().strftime('%Y-%m-%d')
    print(f"[INFO] Loaded state. Date: {state['date']}, Slot1 Remaining: {state['slot1_remaining']}, Slot2 Remaining: {state['slot2_remaining']}")
    
    # Reset if new day
    if state['date'] != current_date_str:
        print(f"[INFO] New day detected ({current_date_str}). Resetting state.")
        state['date'] = current_date_str
        state['slot1_remaining'] = 1
        state['slot2_remaining'] = 2
        state['major_sent'] = []
        state['nuclear_sent'] = []
        state['digest_sent'] = False
        # Keep digest_items and ignored_items for continuity
    
    if not init_firebase():
        print("[CRITICAL] Firebase init failed. Exiting.")
        return

    # 2. Fetch RSS
    print(f"[INFO] Fetching RSS feed from {RSS_URL}...")
    try:
        response = requests.get(RSS_URL)
        response.raise_for_status()
        print(f"[INFO] RSS fetch successful. Content length: {len(response.content)} bytes.")
        root = ET.fromstring(response.content)
    except Exception as e:
        print(f"[ERROR] Error fetching RSS: {e}")
        return

    # 3. Score ALL items first (don't send yet)
    channel = root.find('channel')
    items = channel.findall('item')
    print(f"[INFO] Found {len(items)} items in RSS feed.")
    
    current_time = datetime.datetime.utcnow()
    
    nuclear_candidates = []
    major_candidates = []
    digest_candidates = []
    ignored_candidates = []
    
    for item in items:
        title = item.find('title').text
        link = item.find('link').text
        pub_date_str = item.find('pubDate').text
        
        # Extract Image
        image_url = None
        enclosure = item.find('enclosure')
        if enclosure is not None:
            image_url = enclosure.get('url')
        
        print(f"\n[ITEM] Processing: {title}")
        print(f"       Link: {link}")
        print(f"       Image: {image_url}")
        print(f"       PubDate: {pub_date_str}")

        # Basic parsing
        try:
            pub_date = datetime.datetime.strptime(pub_date_str, "%a, %d %b %Y %H:%M:%S %z").replace(tzinfo=None)
        except Exception as e:
            print(f"       [WARN] Failed to parse date: {e}. Skipping.")
            continue
            
        headline_id = generate_id(title, pub_date_str)
        print(f"       ID: {headline_id}")
        
        # Check if already processed
        if headline_id in [x['id'] for x in state['nuclear_sent']]:
            print("       [SKIP] Already sent (Nuclear).")
            continue
        if headline_id in [x['id'] for x in state['nuclear_queue']]:
            print("       [SKIP] Already queued (Nuclear).")
            continue
        if headline_id in [x['id'] for x in state['major_sent']]:
            print("       [SKIP] Already sent (Major).")
            continue
        if headline_id in state['ignored_items']:
            print("       [SKIP] Already ignored.")
            continue
            
        score, category = score_headline(title)
        
        item_data = {
            "id": headline_id,
            "title": title,
            "url": link,
            "score": score,
            "timestamp": pub_date.isoformat(),
            "image": image_url
        }
        
        # Categorize
        if category == "nuclear":
            nuclear_candidates.append(item_data)
        elif category == "major":
            major_candidates.append(item_data)
        elif category == "digest":
            digest_candidates.append(item_data)
        else:  # ignore
            ignored_candidates.append(headline_id)

    print(f"\n[INFO] Categorization complete:")
    print(f"       Nuclear: {len(nuclear_candidates)}")
    print(f"       Major: {len(major_candidates)}")
    print(f"       Digest: {len(digest_candidates)}")
    print(f"       Ignored: {len(ignored_candidates)}")

    # 4. Process Nuclear Items (Respect Quiet Hours)
    in_quiet_hours = is_in_nuclear_quiet_hours(current_time)
    print(f"\n[INFO] Nuclear quiet hours active: {in_quiet_hours} (12 AM - 8 AM IST)")
    
    # Send any queued nuclear items if we're outside quiet hours
    if not in_quiet_hours and state['nuclear_queue']:
        print(f"\n[INFO] Sending {len(state['nuclear_queue'])} queued nuclear notifications from quiet hours...")
        for item in state['nuclear_queue']:
            print(f"\n[NUCLEAR QUEUED] Sending: {item['title']}")
            send_fcm_notification(
                title="F1 News",
                body=f"🚨 {item['title']}",
                data={"type": "nuclear", "url": item['url'], "score": str(item['score']), "image": item.get('image', ''), "channel_id": "f1_nuclear"},
                priority="high",
                channel_id="f1_nuclear",
                image_url=item.get('image')
            )
            state['nuclear_sent'].append(item)
        state['nuclear_queue'] = []  # Clear the queue
    
    # Process new nuclear items
    for item in nuclear_candidates:
        if in_quiet_hours:
            # Queue for later
            print(f"\n[NUCLEAR] Queuing for later (quiet hours): {item['title']}")
            state['nuclear_queue'].append(item)
        else:
            # Send immediately
            print(f"\n[NUCLEAR] Sending: {item['title']}")
            send_fcm_notification(
                title="F1 News",
                body=f"🚨 {item['title']}",
                data={"type": "nuclear", "url": item['url'], "score": str(item['score']), "image": item.get('image', ''), "channel_id": "f1_nuclear"},
                priority="high",
                channel_id="f1_nuclear",
                image_url=item.get('image')
            )
            state['nuclear_sent'].append(item)

    # 5. Build Major Candidates Pool (Hot Pool System)
    # Combine: digest_queue items with score >= 85 + new major candidates
    print(f"\n[INFO] Building major candidates pool...")
    
    # Get items from digest queue that qualify as major
    major_from_queue = [item for item in state['digest_items'] if item.get('score', 0) >= MAJOR_THRESHOLD]
    print(f"       Major-level items from digest queue: {len(major_from_queue)}")
    
    # Combine with new major candidates
    all_major_candidates = major_from_queue + major_candidates
    
    # Deduplicate by ID (prevent items from appearing in both queue and RSS)
    unique_candidates = {}
    for item in all_major_candidates:
        unique_candidates[item['id']] = item
    all_major_candidates = list(unique_candidates.values())

    print(f"       Total major candidates (deduplicated): {len(all_major_candidates)}")
    
    # Filter out already sent (double check)
    sent_ids = set([x['id'] for x in state['nuclear_sent']] + [x['id'] for x in state['major_sent']])
    all_major_candidates = [item for item in all_major_candidates if item['id'] not in sent_ids]
    print(f"       After filtering sent items: {len(all_major_candidates)}")
    
    # Sort by score (desc), then by timestamp (desc = newer first)
    all_major_candidates.sort(key=lambda x: (x['score'], x['timestamp']), reverse=True)
    
    # 6. Send Major Notifications Based on Slot Availability
    in_slot1 = is_in_slot1_window(current_time)
    in_slot2 = is_in_slot2_window(current_time)
    
    print(f"\n[INFO] Time Windows: Slot1={in_slot1}, Slot2={in_slot2}")
    print(f"       Available slots: Slot1={state['slot1_remaining']}, Slot2={state['slot2_remaining']}")
    
    unsent_majors = []
    
    if in_slot1 and state['slot1_remaining'] > 0:
        # Send top 1 item for slot1
        to_send = all_major_candidates[:state['slot1_remaining']]
        for item in to_send:
            print(f"\n[MAJOR SLOT1] Sending: {item['title']} (score: {item['score']})")
            if send_fcm_notification(
                title="F1 News",
                body=item['title'],
                data={"type": "major", "url": item['url'], "score": str(item['score']), "image": item.get('image', ''), "channel_id": "f1_major"},
                priority="high",
                channel_id="f1_major",
                image_url=item.get('image')
            ):
                state['slot1_remaining'] -= 1
                state['major_sent'].append(item)
                all_major_candidates.remove(item)
    
    if in_slot2 and state['slot2_remaining'] > 0:
        # Send top 2 items for slot2
        to_send = all_major_candidates[:state['slot2_remaining']]
        for item in to_send:
            print(f"\n[MAJOR SLOT2] Sending: {item['title']} (score: {item['score']})")
            if send_fcm_notification(
                title="F1 News",
                body=item['title'],
                data={"type": "major", "url": item['url'], "score": str(item['score']), "image": item.get('image', ''), "channel_id": "f1_major"},
                priority="high",
                channel_id="f1_major",
                image_url=item.get('image')
            ):
                state['slot2_remaining'] -= 1
                state['major_sent'].append(item)
                all_major_candidates.remove(item)
    
    # Remaining major candidates become unsent majors
    unsent_majors = all_major_candidates
    print(f"\n[INFO] Unsent major items: {len(unsent_majors)}")

    # 7. Rebuild Digest Queue (Hot Pool System)
    print(f"\n[INFO] Rebuilding digest queue...")
    
    # Collect all candidates
    all_digest_candidates = []
    
    # Add existing digest items (exclude those sent as major)
    sent_major_ids = set([x['id'] for x in state['major_sent']])
    existing_digest = [item for item in state['digest_items'] if item['id'] not in sent_major_ids]
    all_digest_candidates.extend(existing_digest)
    print(f"       Existing digest items: {len(existing_digest)}")
    
    # Add new digest items from RSS
    all_digest_candidates.extend(digest_candidates)
    print(f"       New digest items from RSS: {len(digest_candidates)}")
    
    # Add unsent major items
    all_digest_candidates.extend(unsent_majors)
    print(f"       Unsent major items: {len(unsent_majors)}")
    
    # Deduplicate by ID
    unique_digest = {}
    for item in all_digest_candidates:
        unique_digest[item['id']] = item
    all_digest_candidates = list(unique_digest.values())
    
    # Filter out already sent items (nuclear + major)
    all_sent_ids = set([x['id'] for x in state['nuclear_sent']] + [x['id'] for x in state['major_sent']])
    all_digest_candidates = [item for item in all_digest_candidates if item['id'] not in all_sent_ids]
    
    # Sort by score (desc), then timestamp (desc = newer first)
    all_digest_candidates.sort(key=lambda x: (x['score'], x['timestamp']), reverse=True)
    
    # Keep top 6
    state['digest_items'] = all_digest_candidates[:6]
    print(f"       Final digest queue size: {len(state['digest_items'])}")
    if state['digest_items']:
        print(f"       Top scores: {[item['score'] for item in state['digest_items']]}")

    # 8. Check Digest Send Time
    if is_in_digest_window(current_time) and not state['digest_sent']:
        print(f"\n[INFO] In digest window. Checking threshold...")
        
        if len(state['digest_items']) >= 3:
            # Check threshold: sum of top 3
            top3_sum = sum([item['score'] for item in state['digest_items'][:3]])
            print(f"       Top 3 sum: {top3_sum} (threshold: {DIGEST_COMBINED_THRESHOLD})")
            
            if top3_sum >= DIGEST_COMBINED_THRESHOLD:
                # Determine count based on day
                day_of_week = current_time.strftime('%A')
                is_race_weekend = day_of_week in ['Monday', 'Friday', 'Saturday', 'Sunday']
                max_items = 6 if is_race_weekend else 4
                
                items_to_send = state['digest_items'][:max_items]
                digest_title = generate_digest_title(len(items_to_send), day_of_week)
                
                print(f"[INFO] Sending digest: {digest_title}")
                print(f"       Day: {day_of_week}, Max items: {max_items}, Sending: {len(items_to_send)}")
                
                # Build digest body
                body_lines = []
                for item in items_to_send:
                    emoji = get_emoji_for_item(item)
                    body_lines.append(f"{emoji} {item['title']}")
                
                body = "\n".join(body_lines) + "\n\nTap to read more"
                
                # Send digest
                send_fcm_notification(
                    title=digest_title,
                    body=body,
                    data={"type": "digest", "count": str(len(items_to_send)), "channel_id": "f1_digest", "target_tab": "news"},
                    priority="normal",
                    channel_id="f1_digest"
                )
                
                state['digest_sent'] = True
                state['digest_items'] = []  # Clear after sending
            else:
                print(f"       Threshold not met. Not sending digest.")
        else:
            print(f"       Not enough items for threshold check ({len(state['digest_items'])} < 3)")
    
    # 9. Update ignored items
    state['ignored_items'].extend(ignored_candidates)

    # 10. Cleanup Old State (Keep last 48 hours)
    print(f"\n[INFO] Cleaning up old state...")
    cutoff_time = current_time - datetime.timedelta(hours=96)
    
    state['nuclear_sent'] = [x for x in state['nuclear_sent'] if datetime.datetime.fromisoformat(x['timestamp']) > cutoff_time]
    state['major_sent'] = [x for x in state['major_sent'] if datetime.datetime.fromisoformat(x['timestamp']) > cutoff_time]
    
    # Limit ignored_items size to last 200
    if len(state['ignored_items']) > 500:
        state['ignored_items'] = state['ignored_items'][-500:]

    # 11. Save State
    save_state(state)
    print("[INFO] Run completed.")

if __name__ == "__main__":
    main()
