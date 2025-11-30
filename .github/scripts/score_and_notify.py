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

# --- Scoring Constants ---
NUCLEAR_SCORE = 999
MAJOR_THRESHOLD = 15
DIGEST_THRESHOLD = 5

# --- Patterns (Regex) ---
# Note: Using simple lists of patterns for brevity, but implementing the full logic.
NUCLEAR_PATTERNS = [
    r"\b(crash|accident|injured|hospitalized|fatal|death|died)\b",
    r"\b(red flag|red-flagged)\b",
    r"\b(cancelled|postponed)\b.*\b(race|grand prix|gp|session)\b",
    r"\b(disqualified)\b.*\b(race|grand prix|gp)\b",
    r"\b(banned|suspended)\b.*\b(driver|team|races)\b",
    r"\b(leaves|exits|departs|replaced)\b.*\b(red bull|ferrari|mercedes|mclaren)\b",
    r"\b(horner|wolff|vasseur|brown|stella)\b.*\b(leaves|exits|departs|replaced)\b",
    r"\b(retires|retirement|retiring)\b.*\b(from racing|from f1|from formula)\b",
    r"\b(announces retirement)\b"
]

MAJOR_PATTERNS = [
    (110, r"\b(wins|won|victory|victorious)\b.*\b(grand prix|race|gp)\b"),
    (105, r"\b(dominates|dominated|dominating)\b.*\b(grand prix|race|gp)\b"),
    (95, r"\b(pole position|takes pole|claims pole|grabs pole|snatches pole)\b"),
    (85, r"\b(podium)\b"),
    (130, r"\b(clinches|secures|wins|seals)\b.*\b(championship|title|wdc|wcc)\b"),
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
    (70, r"\b(sprint race)\b.*\b(report|result|win|victory)\b"),
    (65, r"\b(sprint)\b.*\b(pole position|pole)\b"),
    (60, r"\b(fastest|tops|leads)\b.*\b(practice|fp1|fp2|fp3)\b"),
    (70, r"\b(upgrade|upgrades|update|updates)\b.*\b(car|package|floor|wing|aero)\b"),
    (65, r"\b(strategy|pit stop|tyre|tire)\b.*\b(briefing|problem|issue|concern)\b"),
    (60, r"\b(pace|performance|balance)\b.*\b(issue|problem|struggle|concern)\b"),
    (60, r"\b(verstappen|norris|hamilton|leclerc|piastri|sainz|russell)\b.*\b(says|admits|reveals|claims|insists)\b"),
    (70, r"\b(collision|incident|contact|clash)\b.*\b(penalty|investigation|cleared|reprimand)\b"),
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
            return json.load(f)
    return {
        "date": "1970-01-01",
        "nuclear_sent": [],
        "major_sent": [],
        "major_slots_used": 0,
        "major_slots_remaining": 2,
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

def send_fcm_notification(title, body, data, priority="high", channel_id="f1_major"):
    print(f"[INFO] Sending FCM Notification: {title}")
    try:
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            data=data,
            topic=FCM_TOPIC,
            android=messaging.AndroidConfig(
                priority=priority,
                notification=messaging.AndroidNotification(
                    channel_id=channel_id,
                    color="#FF0000" if channel_id == "f1_nuclear" else None
                )
            )
        )
        response = messaging.send(message)
        print(f"  [SUCCESS] Message sent: {response}")
        return True
    except Exception as e:
        print(f"  [ERROR] Error sending message: {e}")
        return False

def generate_digest_title(items, current_date):
    count = len(items)
    day_of_week = current_date.strftime('%A')
    
    # Simple logic for now, can be expanded
    if day_of_week == 'Friday':
        return f"🏁 F1 Practice Roundup • {count} Updates"
    elif day_of_week == 'Saturday':
        return f"⚡ F1 Qualifying Digest • {count} Updates"
    elif day_of_week == 'Sunday':
        return f"🏆 F1 Race Day Wrap • {count} Updates"
        
    if count <= 2:
        return f"📰 F1 Quick Brief • {count} Updates"
    elif count <= 5:
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

# --- Main Logic ---

def main():
    print(f"[INFO] Starting run at {datetime.datetime.utcnow()}")
    
    # 1. Initialize
    state = load_state()
    current_date_str = datetime.datetime.utcnow().strftime('%Y-%m-%d')
    print(f"[INFO] Loaded state. Date: {state['date']}, Major Slots Remaining: {state['major_slots_remaining']}")
    
    # Reset if new day
    if state['date'] != current_date_str:
        print(f"[INFO] New day detected ({current_date_str}). Resetting state.")
        state['date'] = current_date_str
        state['major_slots_used'] = 0
        state['major_slots_remaining'] = 2
        state['major_sent'] = []
        state['nuclear_sent'] = []
        state['digest_items'] = []
        state['digest_sent'] = False
        # Keep ignored items for now to avoid re-processing immediately
    
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

    # 3. Process Headlines
    channel = root.find('channel')
    items = channel.findall('item')
    print(f"[INFO] Found {len(items)} items in RSS feed.")
    
    current_time = datetime.datetime.utcnow()
    
    for item in items:
        title = item.find('title').text
        link = item.find('link').text
        pub_date_str = item.find('pubDate').text
        
        print(f"\n[ITEM] Processing: {title}")
        print(f"       Link: {link}")
        print(f"       PubDate: {pub_date_str}")

        # Basic parsing
        try:
            pub_date = datetime.datetime.strptime(pub_date_str, "%a, %d %b %Y %H:%M:%S %z").replace(tzinfo=None)
        except Exception as e:
            print(f"       [WARN] Failed to parse date: {e}. Skipping.")
            continue
            
        headline_id = generate_id(title, pub_date_str)
        print(f"       ID: {headline_id}")
        
        # Check if processed
        if headline_id in [x['id'] for x in state['nuclear_sent']]:
            print("       [SKIP] Already sent (Nuclear).")
            continue
        if headline_id in [x['id'] for x in state['major_sent']]:
            print("       [SKIP] Already sent (Major).")
            continue
        if headline_id in [x['id'] for x in state['digest_items']]:
            print("       [SKIP] Already in digest queue.")
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
            "timestamp": datetime.datetime.utcnow().isoformat()
        }
        
        if category == "nuclear":
            print("       [ACTION] Sending NUCLEAR notification.")
            # Send Immediately
            send_fcm_notification(
                title="🚨 F1 BREAKING NEWS",
                body=title,
                data={"type": "nuclear", "url": link, "score": str(score)},
                priority="high",
                channel_id="f1_nuclear"
            )
            state['nuclear_sent'].append(item_data)
            
        elif category == "major":
            # Check slots and time window
            hour = current_time.hour
            in_window = (12 <= hour < 15) or (18 <= hour < 21)
            print(f"       [ACTION] Category MAJOR. Slots: {state['major_slots_remaining']}, Window: {in_window} (Hour: {hour})")
            
            if state['major_slots_remaining'] > 0 and in_window:
                print("       [ACTION] Sending MAJOR notification.")
                send_fcm_notification(
                    title="🏁 F1 Major News",
                    body=title,
                    data={"type": "major", "url": link, "score": str(score)},
                    priority="high",
                    channel_id="f1_major"
                )
                state['major_slots_used'] += 1
                state['major_slots_remaining'] -= 1
                state['major_sent'].append(item_data)
            else:
                print("       [ACTION] Overflowing to digest.")
                state['digest_items'].append(item_data)
                
        elif category == "digest":
            print("       [ACTION] Adding to digest queue.")
            state['digest_items'].append(item_data)
            
        else: # ignore
            print("       [ACTION] Ignoring.")
            state['ignored_items'].append(headline_id)

    # 4. Digest Check
    print(f"\n[INFO] Checking digest status. Time: {current_time.hour}:{current_time.minute}, Sent: {state['digest_sent']}, Items: {len(state['digest_items'])}")
    if 16 <= current_time.hour < 17 and current_time.minute >= 30 and not state['digest_sent']:
        if len(state['digest_items']) > 0:
            print("[INFO] Generating digest...")
            # Sort by score
            sorted_items = sorted(state['digest_items'], key=lambda x: x['score'], reverse=True)
            top_items = sorted_items[:6] # Max 6 items
            
            digest_title = generate_digest_title(top_items, current_time)
            
            body_lines = []
            for item in top_items:
                emoji = get_emoji_for_item(item)
                body_lines.append(f"{emoji} {item['title']}")
            
            body = "\n".join(body_lines) + "\n\nTap to read more"
            
            send_fcm_notification(
                title=digest_title,
                body=body,
                data={"type": "digest", "count": str(len(top_items))},
                priority="normal",
                channel_id="f1_digest"
            )
            state['digest_sent'] = True
            state['digest_items'] = [] # Clear after sending
        else:
            print("[INFO] No items for digest.")

    # 5. Cleanup Old State (Keep last 48 hours)
    print("[INFO] Cleaning up old state...")
    cutoff_time = current_time - datetime.timedelta(hours=48)
    
    state['nuclear_sent'] = [x for x in state['nuclear_sent'] if datetime.datetime.fromisoformat(x['timestamp']) > cutoff_time]
    state['major_sent'] = [x for x in state['major_sent'] if datetime.datetime.fromisoformat(x['timestamp']) > cutoff_time]
    # Note: digest_items are cleared when sent, or we can keep them for 24h if not sent
    # Ignored items are just IDs, so we can't check timestamp easily unless we store it.
    # For now, let's limit ignored_items size to last 200
    if len(state['ignored_items']) > 200:
        state['ignored_items'] = state['ignored_items'][-200:]

    # 6. Save State
    save_state(state)
    print("[INFO] Run completed.")

if __name__ == "__main__":
    main()
