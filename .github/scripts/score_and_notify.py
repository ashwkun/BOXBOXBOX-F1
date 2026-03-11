import os
import json
import hashlib
import datetime
import re
import requests
import xml.etree.ElementTree as ET
import firebase_admin
from firebase_admin import credentials, messaging
from difflib import SequenceMatcher
from dotenv import load_dotenv

load_dotenv()

# --- Configuration ---
RSS_URL = "https://www.motorsport.com/rss/f1/news/"
STATE_FILE = "data/notification_state.json"
FCM_TOPIC = "all_users"
DRY_RUN = os.environ.get('NOTIFICATION_DRY_RUN', 'false').lower() == 'true'

# --- State Schema Version ---
STATE_SCHEMA_VERSION = 3

# --- Scoring Constants ---
NUCLEAR_SCORE = 999
NUCLEAR_THRESHOLD = NUCLEAR_SCORE
MAJOR_THRESHOLD = 60   # Lowered from 95 for broader matching
DIGEST_THRESHOLD = 25  # Lowered from 40 for broader matching
DIGEST_COMBINED_THRESHOLD = 100  # Lowered from 150
MINIMUM_SCORE = 15  # Auto-ignore below this
GEMINI_MAX_DAILY_CALLS = 5  # Conservative cap for free tier (20 RPD)

GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY")
GEMINI_MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")
GEMINI_API_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{GEMINI_MODEL}:generateContent"

SPECULATIVE_MARKERS = [
    "rumored",
    "rumour",
    "rumor",
    "could",
    "might",
    "may",
    "suggests",
    "reportedly",
    "reports claim",
    "linked with",
    "set to",
]

F1_ENTITY_PATTERN = re.compile(
    r"\b("
    r"fia|formula\s?1|f1|"
    r"red\s+bull|ferrari|mercedes|mclaren|aston\s+martin|alpine|williams|rb|racing\s+bulls|sauber|haas|"
    r"verstappen|perez|hamilton|leclerc|sainz|norris|piastri|russell|alonso|stroll|gasly|ocon|albon|tsunoda|"
    r"hulkenberg|magnussen|zhou|bottas|lawson|bearman|antonelli"
    r")\b",
    re.IGNORECASE,
)

# --- Retention Periods (days) ---
SENT_RETENTION_DAYS = 30  # nuclear + major
DIGEST_RETENTION_DAYS = 14
IGNORED_ITEMS_CAP = 5000

# --- Time Windows (UTC) ---
SLOT1_START_HOUR, SLOT1_START_MIN = 7, 30
SLOT1_END_HOUR, SLOT1_END_MIN = 8, 30
SLOT2_START_HOUR, SLOT2_START_MIN = 15, 30
SLOT2_END_HOUR, SLOT2_END_MIN = 16, 30
DIGEST_START_HOUR, DIGEST_START_MIN = 2, 30
DIGEST_END_HOUR, DIGEST_END_MIN = 3, 30

# --- Universal Pre-Filters (Hard Reject Patterns) ---
UNIVERSAL_REJECT_PATTERNS = [
    # Listicles & Rankings
    r'\b(top\s+(5|10|five|ten)|best\s+of|worst\s+of)\b',
    r'\b\d+\s+(things|takeaways|moments|lessons)\s+(we\s+learned|from)\b',
    
    # Interactive/Engagement Bait
    r'\b(rate\s+the\s+race|vote\s+for|caption\s+(this|competition)|poll:)\b',
    
    # Photo/Video Galleries
    r'\b(in\s+pictures|photo\s+gallery|images\s+only|gallery:)\b',
    
    # Historical/Throwback
    r'^.*\b(201\d)\b.*$',  # Only reject 2010s decade, not 2020s
    r'\b(\d+)\s+years?\s+ago\b',
    r'\b(on\s+this\s+day|throwback|flashback)\b',
    
    # Fan/Social Content
    r'\b(fans?\s+react|social\s+media|twitter\s+explodes|x\s+reacts)\b',
    
    # Reviews
    r'\b(review:|(book|game|documentary|podcast)\s+review)\b',
]

# --- Nuclear Disqualifiers (Demote to Major) ---
NUCLEAR_DISQUALIFIERS = [
    r'\b(how|why|what)\s+(verstappen|hamilton|norris|leclerc|ferrari|red\s+bull|cadillac|audi)',
    r'\b(explained|breakdown|analysis|deep\s+dive)\b',
    r'\b(could|might|may|possible|potential|likely)\b',
    r'\b(preview|talking\s+points|what\s+to\s+watch|looking\s+ahead)\b',
    r'\b(reacts?\s+to|responds?\s+to|addresses|comments\s+on)\b',
]

# --- Digest Disqualifiers (Ignore Entirely) ---
DIGEST_DISQUALIFIERS = [
    r'\b(priceless|hilarious|bizarre|funny|amazing)\s+(reaction|moment)\b',
    r'\b(most\s+exciting\s+part|best\s+bit)\b',
]

# --- Refined Nuclear Patterns (Stricter) ---
NUCLEAR_PATTERNS = [
    r'\b(crash|accident).*\b(injured|hospitalized|medical|red\s+flag)\b',
    r'\b(red\s+flag|red-flagged)\b',
    r'\b(cancelled|postponed)\b.*\b(race|grand\s+prix|session)\b',
    r'\b(wins?|won|victory|victorious)\b.*\b(grand\s+prix|race|gp)\b',
    r'\b(grand\s+prix|race|gp)\b.*\b(wins?|won|victory|victorious)\b',
    r'\b(pole\s+position|takes\s+pole|claims\s+pole|grabs\s+pole)\b',
    r'\b(sprint)\b.*\b(win|wins?|won|victory)\b',
    r'\b(clinches?|secures?|wins?|seals?|claims?|takes?|grabs?)\b.*\b(championship|title|wdc|wcc)\b',
    r'\b(mathematically|officially)\b.*\b(eliminated|out\s+of\s+contention)\b',
    r'\b(disqualified|dsq)\b.*\b(race|grand\s+prix|gp)\b',
    r'\b(signs?|signed|confirms?|confirmed|announced?)\b.*\b(driver|contract|2027|2028)\b',
    r'\b(retires?|retirement|retiring)\b.*\b(from\s+(racing|f1|formula))\b',
    r'\b(announces?\s+retirement)\b',
]

# --- Major Patterns (pair-based, high confidence) ---
MAJOR_PATTERNS = [
    (70, r'\b(dominates?|dominated|dominating)\b.*\b(grand\s+prix|race|gp)\b'),
    (65, r'\b(championship|title)\b.*\b(lead|ahead|battle|fight|gap)\b'),
    (75, r'\b(signs?|signed|confirms?|confirmed)\b.*\b(2026|2027|2028|2029|contract)\b'),
    (70, r'\b(official:)\b.*\b(driver|seat|signs?|joins?)\b'),
    (80, r'\b(team\s+principal|tp)\b.*\b(leaves?|joins?|appointed)\b'),
    (65, r'\b(grid\s+(drop|penalty)|grid-place\s+penalty)\b'),
    (65, r'\b(penalty|penali[sz]ed)\b.*\b(grid|race|time|points|seconds)\b'),
    (65, r'\b(protest|appeal)\b.*\b(upheld|dismissed|successful)\b'),
    (65, r'\b(fastest|quickest|tops|leads)\b.*\b(qualifying|q[123]|shootout)\b'),
    (65, r'\b(sprint)\b.*\b(result|report|win|wins?|won)\b'),
    (65, r'\b(summoned|investigation|under\s+investigation)\b.*\b(stewards|fia)\b'),
    (65, r'\b(reveals?|launche?s?|unveils?|wraps\s+off)\b.*\b(car|livery|challenger|2026|2027|2028)\b'),
    (65, r'\b(sick|ill|surgery|hospital|medical)\b.*\b(miss|doubt|ruled\s+out|withdraws?)\b'),
    (65, r'\b(ruled\s+out|withdraws?|misses)\b.*\b(grand\s+prix|race|gp)\b'),
]

# --- Medium Patterns (pair-based) ---
MEDIUM_PATTERNS = [
    (50, r'\b(qualifying)\b.*\b(report|result|recap)\b'),
    (45, r'\b(fastest|quickest|tops|leads)\b.*\b(qualifying|q[123])\b'),
    (50, r'\b(team\s+orders)\b'),
    (45, r'\b(sprint\s+race)\b.*\b(report|result|recap)\b'),
    (45, r'\b(upgrade|update)s?\b.*\b(car|package|floor|wing|aero)\b'),
    (40, r'\b(strategy|pit\s+stop|tyre|tire)\b.*\b(briefing|problem|issue|gamble|mistake|error)\b'),
    (35, r'\b(verstappen|norris|hamilton|leclerc|piastri|russell|hadjar|alonso|bearman|antonelli|colapinto)\b.*\b(says|admits|reveals|warns|slams|fumes|blasts)\b'),
]

# --- NEW: Broad Single-Keyword Patterns ---
BROAD_PATTERNS = [
    # Race weekend activity
    (30, r'\b(qualifying|quali)\b'),
    (25, r'\b(practice|fp[123])\b'),
    (35, r'\b(race\s+report|race\s+recap|race\s+review)\b'),
    (25, r'\b(sprint)\b'),
    
    # Team/driver performance
    (30, r'\b(pace|performance|gap|deficit|advantage)\b'),
    (25, r'\b(strategy|pit\s+stop|undercut|overcut|tyre|tire)\b'),
    (30, r'\b(upgrade|update|development|floor|wing|aero|sidepod)\b'),
    
    # Significant events
    (40, r'\b(crash|accident|collision|contact|incident|damage)\b'),
    (35, r'\b(penalty|penalised|penalized|stewards)\b'),
    (35, r'\b(investigation|protest|appeal)\b'),
    (40, r'\b(banned|suspended|disqualified)\b'),
    (35, r'\b(injury|injured|hospital|medical)\b'),
    
    # Driver market / team changes
    (40, r'\b(contract|extension|deal|signs?|signed|departure|exit|sacked|fired)\b'),
    (35, r'\b(replacement|reserve|stand-in|substitute)\b'),
    (30, r'\b(rumou?r|linked|target|interest)\b'),
    
    # Official/breaking markers
    (35, r'\b(official|confirmed|breaking|exclusive|just\s+in)\b'),
    (30, r'\b(announces?|announced|announcement)\b'),
    
    # Strong tone words
    (25, r'\b(slams?|fumes|blasts?|warns?|hits\s+out|fires\s+back|rips)\b'),
    (25, r'\b(stunned|shocked|surprised|dramatic|chaos|chaotic|controversial)\b'),
    (20, r'\b(praises?|hails?|impressed|brilliant|fantastic|incredible|dominant)\b'),
    (20, r'\b(admits?|reveals?|insists?|reckons?|confident|positive|optimistic)\b'),
    (20, r'\b(concerned|worried|frustrated|disappointed|struggles?|difficult|tough)\b'),
    (20, r'\b(debut|milestone|record|historic|first\s+time|maiden)\b'),
    
    # F1-specific context boosters
    (15, r'\b(grand\s+prix|gp)\b'),
    (10, r'\b(formula\s+1|f1)\b'),
    (15, r'\b(paddock|grid|pit\s+lane|cockpit)\b'),
]

# --- Negative Patterns (Softened) ---
NEGATIVE_PATTERNS = [
    (-30, r'\b(caption\s+competition|round-?up)\b'),
    (-25, r'\b(pictures?\s+only|photos?\s+only|gallery)\b'),
    (-20, r'\b(top\s+\d+|ranked|ranking)\b'),
    (-15, r'\b(as\s+it\s+happened|years?\s+ago)\b'),
    (-10, r'\b(could|might|may)\b'),
    (-15, r'\b(rumou?rs?|speculation)\b'),
    (-20, r'\b(rate\s+the|poll:?|vote\s+for)\b'),
    (-15, r'\b(indycar|formula\s+[234e]|motogp)\b'),
]

# --- Age Decay Curves ---
AGE_DECAY_CURVES = {
    'race_result': [
        (6, 1.0), (12, 1.0), (24, 0.9), (48, 0.7), (72, 0.4), (96, 0.1)
    ],
    'breaking': [
        (6, 1.0), (12, 0.6), (24, 0.2), (48, 0.05)
    ],
    'analysis': [
        (24, 1.0), (48, 0.8), (72, 0.5), (96, 0.2)
    ],
}

# ============================================================================
# STATE MANAGEMENT
# ============================================================================

def load_state():
    """Load and migrate state to current schema version"""
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, 'r') as f:
            state = json.load(f)
    else:
        state = create_default_state()
    
    # Migrate to v2 if needed
    if state.get('schema_version', 1) == 1:
        print("[INFO] Migrating state from v1 to v2...")
        state = migrate_v1_to_v2(state)
    
    # Migrate to v3 if needed (scoring overhaul)
    if state.get('schema_version', 1) == 2:
        print("[INFO] Migrating state from v2 to v3 (scoring overhaul)...")
        state = migrate_v2_to_v3(state)
    
    return state

def create_default_state():
    """Create default state structure"""
    return {
        "schema_version": STATE_SCHEMA_VERSION,
        "date": "1970-01-01",
        "nuclear_sent": [],
        "nuclear_queue": [],
        "major_sent": [],
        "slot1_remaining": 1,
        "slot2_remaining": 2,
        "digest_items": [],
        "digest_sent": False,
        "ignored_items": [],
        "sent_urls": [],
        "title_fingerprints": [],
    }

def migrate_v1_to_v2(state):
    """Migrate v1 state to v2 (add new fields)"""
    # Extract URLs from existing sent items
    sent_urls = set()
    for item in state.get('nuclear_sent', []) + state.get('major_sent', []):
        if 'url' in item:
            sent_urls.add(item['url'])
    
    # Create title fingerprints from recent sent items
    recent_sent = (state.get('nuclear_sent', []) + state.get('major_sent', []))[-200:]
    title_fingerprints = [
        create_title_fingerprint(item['title'], item.get('timestamp', ''))
        for item in recent_sent
    ]
    
    state['schema_version'] = 2
    state['sent_urls'] = list(sent_urls)
    state['title_fingerprints'] = title_fingerprints
    
    # Ensure nuclear_queue exists
    if 'nuclear_queue' not in state:
        state['nuclear_queue'] = []
    
    return state

def migrate_v2_to_v3(state):
    """Migrate v2 state to v3 (scoring overhaul with broad patterns)"""
    # Clear ignored_items — they were scored with the old broken patterns
    # and need to be re-evaluated with the new broad keyword matching
    old_ignored_count = len(state.get('ignored_items', []))
    state['ignored_items'] = []
    
    # Clear digest_items — old scores are no longer valid
    old_digest_count = len(state.get('digest_items', []))
    state['digest_items'] = []
    state['digest_sent'] = False
    
    # Reset daily slots so we can test immediately
    state['slot1_remaining'] = 1
    state['slot2_remaining'] = 2
    
    state['schema_version'] = 3
    print(f"[INFO] v2→v3 migration: cleared {old_ignored_count} ignored items, {old_digest_count} digest items")
    
    return state

def save_state(state):
    """Save state to file"""
    with open(STATE_FILE, 'w') as f:
        json.dump(state, f, indent=2)

# ============================================================================
# DEDUPLICATION HELPERS
# ============================================================================

def generate_id(title, pub_date):
    """Generate unique ID from title + date"""
    return hashlib.md5((title + pub_date).encode('utf-8')).hexdigest()

def normalize_title(title):
    """Normalize title for fuzzy matching"""
    title = title.lower()
    title = re.sub(r'\b(breaking|official|exclusive):?\s*', '', title)
    title = re.sub(r'[^\w\s]', ' ', title)  # Remove punctuation
    title = re.sub(r'\s+', ' ', title).strip()  # Collapse whitespace
    return title

def create_title_fingerprint(title, timestamp):
    """Create fingerprint for fuzzy matching"""
    norm = normalize_title(title)
    tokens = set(norm.split())
    return {
        'tokens': list(tokens),  # JSON can't serialize sets
        'title': title,
        'timestamp': timestamp or datetime.datetime.utcnow().isoformat()
    }

def is_fuzzy_duplicate(title, fingerprints, threshold=0.80):
    """Check if title is fuzzy duplicate of recent items"""
    new_norm = normalize_title(title)
    new_tokens = set(new_norm.split())
    
    if len(new_tokens) == 0:
        return False, None
    
    for fp in fingerprints[-200:]:  # Check last 200
        fp_tokens = set(fp['tokens'])
        if len(fp_tokens) == 0:
            continue
        
        # Calculate Jaccard similarity (intersection / union)
        intersection = len(new_tokens & fp_tokens)
        union = len(new_tokens | fp_tokens)
        similarity = intersection / union if union > 0 else 0
        
        if similarity >= threshold:
            return True, fp['title']
    
    return False, None

# ============================================================================
# AGE-BASED SCORING
# ============================================================================

def calculate_age_hours(pub_date):
    """Calculate age in hours from publication date"""
    current_time = datetime.datetime.utcnow()
    age_delta = current_time - pub_date
    return age_delta.total_seconds() / 3600

def classify_content_type(title):
    """Classify content for age decay curve selection"""
    # Race results
    if re.search(r'\b(wins?|won|victory|pole\s+position|podium)\b.*\b(grand\s+prix|race|gp|qualifying)\b', title, re.IGNORECASE):
        return 'race_result'
    
    # Breaking news
    if re.search(r'\b(crash|accident|red\s+flag|cancelled|disqualified|signs?|confirms?)\b', title, re.IGNORECASE):
        return 'breaking'
    
    # Default to analysis
    return 'analysis'

def get_age_decay(age_hours, content_type):
    """Get age decay multiplier based on content type"""
    curve = AGE_DECAY_CURVES.get(content_type, AGE_DECAY_CURVES['breaking'])
    
    for hour_threshold, multiplier in curve:
        if age_hours < hour_threshold:
            return multiplier
    
    return 0.05  # Very old: 5% retention

# ============================================================================
# PATTERN MATCHING & SCORING
# ============================================================================

def check_universal_reject(title):
    """Check if title should be universally rejected"""
    for pattern in UNIVERSAL_REJECT_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            return True, pattern
    return False, None

def score_headline(title):
    """Get base score from pattern matching (no age applied yet)"""
    print(f"  [DEBUG] Scoring: '{title}'")
    
    # Check nuclear patterns first
    for pattern in NUCLEAR_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            print(f"    [MATCH] Nuclear pattern: '{pattern}'")
            
            # Check nuclear disqualifiers
            for disq_pattern in NUCLEAR_DISQUALIFIERS:
                if re.search(disq_pattern, title, re.IGNORECASE):
                    print(f"    [DEMOTE] Nuclear disqualified by: '{disq_pattern}'")
                    # Fall through to major scoring
                    break
            else:
                # No disqualifier matched, keep as nuclear
                return NUCLEAR_SCORE, "nuclear"
    
    # Major/Medium/Broad scoring
    score = 0
    
    for points, pattern in MAJOR_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            print(f"    [MATCH] Major pattern ({points} pts): '{pattern}'")
            score += points
    
    for points, pattern in MEDIUM_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            print(f"    [MATCH] Medium pattern ({points} pts): '{pattern}'")
            score += points
    
    for points, pattern in BROAD_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            print(f"    [MATCH] Broad pattern ({points} pts): '{pattern}'")
            score += points
    
    # Negative patterns
    for points, pattern in NEGATIVE_PATTERNS:
        if re.search(pattern, title, re.IGNORECASE):
            print(f"    [MATCH] Negative pattern ({points} pts): '{pattern}'")
            score += points
    
    # Determine category from base score
    if score >= MAJOR_THRESHOLD:
        category = "major"
    elif score >= DIGEST_THRESHOLD:
        category = "digest"
    else:
        category = "ignore"
    
    print(f"    [RESULT] Base Score: {score} | Category: {category}")
    return score, category

def score_with_age(title, pub_date):
    """Score headline with age decay applied"""
    # Get base score
    base_score, base_category = score_headline(title)
    
    # Calculate age
    age_hours = calculate_age_hours(pub_date)
    
    # Classify content type
    content_type = classify_content_type(title)
    
    # Apply age decay
    decay_multiplier = get_age_decay(age_hours, content_type)
    final_score = base_score * decay_multiplier
    
    print(f"  [AGE] {age_hours:.1f}h old | Type: {content_type} | Decay: {decay_multiplier:.2f}")
    print(f"  [AGE] Base: {base_score} → Final: {final_score:.0f}")
    
    # Re-categorize based on final score
    if final_score >= NUCLEAR_SCORE:
        final_category = "nuclear"
    elif final_score >= MAJOR_THRESHOLD:
        final_category = "major"
    elif final_score >= DIGEST_THRESHOLD:
        final_category = "digest"
    elif final_score >= MINIMUM_SCORE:
        final_category = "ignore"
    else:
        final_category = "hard_ignore"  # Below minimum, don't even track
    
    # Apply digest disqualifiers
    if final_category == "digest":
        for disq_pattern in DIGEST_DISQUALIFIERS:
            if re.search(disq_pattern, title, re.IGNORECASE):
                print(f"    [IGNORE] Digest disqualified by: '{disq_pattern}'")
                final_category = "ignore"
                break
    
    return final_score, final_category



def contains_specific_f1_entity(text):
    """Check whether text references a specific F1 entity."""
    return bool(F1_ENTITY_PATTERN.search(text or ""))


def has_speculative_language(text):
    """Detect speculative language in text."""
    lower_text = (text or "").lower()
    return any(marker in lower_text for marker in SPECULATIVE_MARKERS)


def is_state_change_event(text):
    """Detect high-impact state-change events from title/summary."""
    state_change_pattern = re.compile(
        r'\b('
        r'signs?|signed|contract|extends?|renews?|joins?|appointed|sacked|leaves?|exit|departs?|retires?|retirement|'
        r'disqualified|dsq|penalty|banned|suspended|withdraws?|ruled\s+out|wins?\s+appeal|appeal\s+upheld|'
        r'fia\s+decision|officially\s+confirmed|announces?' 
        r')\b',
        re.IGNORECASE
    )
    return bool(state_change_pattern.search(text or ""))


def validate_nuclear_event(title, summary):
    """Validate if a potential nuclear headline is confirmed and high-impact.

    Returns:
        dict: {
            "confirmed": bool,
            "impact": "high"|"medium"|"low",
            "reason": str,
            "demote_to": "major"|"digest"|None
        }
    """
    combined = f"{title or ''}\n{summary or ''}".strip()

    # Fast deterministic guardrails before API call
    if not contains_specific_f1_entity(combined):
        return {
            "confirmed": False,
            "impact": "low",
            "reason": "No specific F1 entity detected",
            "demote_to": "digest",
        }

    if has_speculative_language(combined):
        return {
            "confirmed": False,
            "impact": "low",
            "reason": "Speculative language detected",
            "demote_to": "major",
        }

    if not is_state_change_event(combined):
        return {
            "confirmed": False,
            "impact": "medium",
            "reason": "No clear state-change event",
            "demote_to": "major",
        }

    # Option B: Gemini API validation
    if not GEMINI_API_KEY:
        return {
            "confirmed": True,
            "impact": "high",
            "reason": "Gemini key missing; deterministic checks passed",
            "demote_to": None,
        }

    prompt = f"""
You are validating Formula 1 breaking-news headlines for urgent alerts.
Decide if this item is confirmed and high-impact enough for an immediate top-priority alert.

Headline: {title}
Summary: {summary}

Rules:
1) Must include a specific F1 entity (driver/team/FIA).
2) Reject speculative or uncertain phrasing (e.g., could, might, rumored, suggests, reports claim).
3) Must be a state-change event (signing, retirement, penalty, disqualification, official appointment/removal, confirmed withdrawal).

Return STRICT JSON only with keys:
- confirmed (boolean)
- impact ("high"|"medium"|"low")
- reason (short string)
""".strip()

    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": 0,
            "maxOutputTokens": 180,
            "responseMimeType": "application/json",
        },
    }

    response = requests.post(
        GEMINI_API_URL,
        params={"key": GEMINI_API_KEY},
        json=payload,
        timeout=8,
    )
    response.raise_for_status()
    data = response.json()

    text_response = (
        data.get("candidates", [{}])[0]
        .get("content", {})
        .get("parts", [{}])[0]
        .get("text", "")
        .strip()
    )
    parsed = json.loads(text_response)

    confirmed = bool(parsed.get("confirmed", False))
    impact = parsed.get("impact", "low")
    reason = parsed.get("reason", "Gemini validation")

    demote_to = None
    if not confirmed:
        demote_to = "major" if impact in {"high", "medium"} else "digest"

    return {
        "confirmed": confirmed,
        "impact": impact,
        "reason": reason,
        "demote_to": demote_to,
    }

# ============================================================================
# FIREBASE & NOTIFICATIONS
# ============================================================================

def init_firebase():
    """Initialize Firebase"""
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
    """Send FCM notification (or log if dry-run)"""
    if DRY_RUN:
        print(f"[DRY RUN] Would send notification:")
        print(f"  Title: {title}")
        print(f"  Body: {body}")
        print(f"  Data: {data}")
        print(f"  Channel: {channel_id}")
        return True
    
    print(f"[INFO] Sending FCM Notification: {title}")
    
    # Add to data payload
    if data is None:
        data = {}
    data["title"] = title
    data["body"] = body
    data["channel_id"] = channel_id
    
    # Validate image
    if image_url and image_url.startswith('https://'):
        data["image_url"] = image_url
    
    try:
        android_config = messaging.AndroidConfig(priority=priority)
        fcm_options = messaging.FCMOptions(analytics_label="f1_news_auto")
        message = messaging.Message(
            data=data,
            topic=FCM_TOPIC,
            android=android_config,
            fcm_options=fcm_options
        )
        response = messaging.send(message)
        print(f"  [SUCCESS] Message sent: {response}")
        return True
    except Exception as e:
        print(f"  [ERROR] Error sending message: {e}")
        return False

# ============================================================================
# TIME WINDOW HELPERS
# ============================================================================

def is_in_nuclear_quiet_hours(current_time):
    """Check if in nuclear quiet hours (7:30PM - 2:30AM IST = 19:30-02:30 UTC)"""
    hour, minute = current_time.hour, current_time.minute
    
    # 19:30 UTC to 23:59 UTC
    if hour > 19 or (hour == 19 and minute >= 30):
        return True
    
    # 00:00 UTC to 02:30 UTC
    if hour < 2 or (hour == 2 and minute < 30):
        return True
    
    return False

def is_in_slot1_window(current_time):
    """Check if in Slot 1 window"""
    hour, minute = current_time.hour, current_time.minute
    if hour == SLOT1_START_HOUR and minute >= SLOT1_START_MIN:
        return True
    elif SLOT1_START_HOUR < hour < SLOT1_END_HOUR:
        return True
    elif hour == SLOT1_END_HOUR and minute < SLOT1_END_MIN:
        return True
    return False

def is_in_slot2_window(current_time):
    """Check if in Slot 2 window"""
    hour, minute = current_time.hour, current_time.minute
    if hour == SLOT2_START_HOUR and minute >= SLOT2_START_MIN:
        return True
    elif SLOT2_START_HOUR < hour < SLOT2_END_HOUR:
        return True
    elif hour == SLOT2_END_HOUR and minute < SLOT2_END_MIN:
        return True
    return False

def is_in_digest_window(current_time):
    """Check if in Digest window"""
    hour, minute = current_time.hour, current_time.minute
    if hour == DIGEST_START_HOUR and minute >= DIGEST_START_MIN:
        return True
    elif DIGEST_START_HOUR < hour < DIGEST_END_HOUR:
        return True
    elif hour == DIGEST_END_HOUR and minute < DIGEST_END_MIN:
        return True
    return False

def generate_digest_title(count, day_of_week):
    """Generate context-aware digest title"""
    if day_of_week == 'Monday':
        return f"🏆 F1 Race Day Wrap • {count} Updates"
    elif day_of_week == 'Sunday':
        return f"⚡ F1 Qualifying Digest • {count} Updates"
    elif day_of_week == 'Saturday':
        return f"🏁 F1 Practice Roundup • {count} Updates"
    elif day_of_week == 'Friday':
        return f"📋 Pre-Race Week Digest • {count} Updates"
    else:
        if count <= 2:
            return f"📰 F1 Quick Brief • {count} Updates"
        elif count <= 4:
            return f"📋 F1 Daily Digest • {count} Updates"
        else:
            return f"🔥 F1 Busy Day • {count} Updates"

def get_emoji_for_item(item):
    """Get emoji for digest item"""
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

# ============================================================================
# MAIN LOGIC
# ============================================================================

def main():
    print(f"[INFO] Starting run at {datetime.datetime.utcnow()}")
    if DRY_RUN:
        print("[INFO] *** DRY RUN MODE *** No notifications will be sent")
    
    # 1. Load state
    state = load_state()
    current_date_str = datetime.datetime.utcnow().strftime('%Y-%m-%d')
    print(f"[INFO] State schema v{state.get('schema_version', 1)}")
    print(f"[INFO] Date: {state['date']}, Slot1: {state['slot1_remaining']}, Slot2: {state['slot2_remaining']}")
    
    # 2. Reset daily limits if new day
    if state['date'] != current_date_str:
        print(f"[INFO] New day detected ({current_date_str}). Resetting daily limits.")
        state['date'] = current_date_str
        state['slot1_remaining'] = 1
        state['slot2_remaining'] = 2
        state['digest_sent'] = False
        # DO NOT clear nuclear_sent or major_sent (30-day retention)
    
    # 3. Initialize Firebase
    if not DRY_RUN and not init_firebase():
        print("[CRITICAL] Firebase init failed. Exiting.")
        return
    
    # 4. Fetch RSS
    print(f"[INFO] Fetching RSS from {RSS_URL}...")
    try:
        response = requests.get(RSS_URL, timeout=10)
        response.raise_for_status()
        root = ET.fromstring(response.content)
    except Exception as e:
        print(f"[ERROR] RSS fetch failed: {e}")
        return
    
    # 5. Process items
    channel = root.find('channel')
    items = channel.findall('item')
    print(f"[INFO] Found {len(items)} items in RSS feed")
    
    current_time = datetime.datetime.utcnow()
    
    nuclear_candidates = []
    major_candidates = []
    digest_candidates = []
    ignored_candidates = []
    
    # Build lookup sets
    sent_nuclear_ids = set(x['id'] for x in state['nuclear_sent'])
    sent_major_ids = set(x['id'] for x in state['major_sent'])
    sent_urls = set(state.get('sent_urls', []))
    ignored_ids = set(state.get('ignored_items', []))
    queued_nuclear_ids = set(x['id'] for x in state['nuclear_queue'])
    
    for item in items:
        title = item.find('title').text
        link = item.find('link').text
        pub_date_str = item.find('pubDate').text
        summary_node = item.find('description')
        summary = summary_node.text if summary_node is not None else ""
        
        # Extract image
        image_url = None
        enclosure = item.find('enclosure')
        if enclosure is not None:
            image_url = enclosure.get('url')
        
        print(f"\n[ITEM] {title}")
        
        # Parse date
        try:
            pub_date = datetime.datetime.strptime(pub_date_str, "%a, %d %b %Y %H:%M:%S %z").replace(tzinfo=None)
        except Exception as e:
            print(f"  [SKIP] Date parse failed: {e}")
            continue
        
        headline_id = generate_id(title, pub_date_str)
        
        # === DEDUPLICATION LAYER ===
        
        # Check 1: ID in sent lists
        if headline_id in sent_nuclear_ids:
            print("  [SKIP] Already sent (Nuclear)")
            continue
        if headline_id in sent_major_ids:
            print("  [SKIP] Already sent (Major)")
            continue
        if headline_id in queued_nuclear_ids:
            print("  [SKIP] Already queued (Nuclear)")
            continue
        if headline_id in ignored_ids:
            print("  [SKIP] Already ignored")
            continue
        
        # Check 2: URL dedup
        if link in sent_urls:
            print(f"  [SKIP] URL already sent (title may have changed)")
            ignored_candidates.append(headline_id)
            continue
        
        # Check 3: Fuzzy title match
        is_dup, similar_title = is_fuzzy_duplicate(title, state.get('title_fingerprints', []))
        if is_dup:
            print(f"  [SKIP] Fuzzy duplicate of: {similar_title}")
            ignored_candidates.append(headline_id)
            continue
        
        # Check 4: Universal reject patterns
        is_reject, reject_pattern = check_universal_reject(title)
        if is_reject:
            print(f"  [SKIP] Universal reject: {reject_pattern}")
            ignored_candidates.append(headline_id)
            continue
        
        # === SCORING ===
        
        score, category = score_with_age(title, pub_date)

        if score >= NUCLEAR_THRESHOLD and category == "nuclear":
            try:
                validation = validate_nuclear_event(title, summary)
                if not validation.get("confirmed", False):
                    demote_to = validation.get("demote_to") or "major"
                    category = "major" if demote_to == "major" else "digest"
                    if category == "major":
                        score = max(MAJOR_THRESHOLD, min(score, NUCLEAR_THRESHOLD - 1))
                    else:
                        score = max(DIGEST_THRESHOLD, min(score, MAJOR_THRESHOLD - 1))
                    print(f"  [AI DEMOTE] Nuclear candidate demoted to {category}: {validation.get('reason', 'validation failed')}")
                else:
                    print(f"  [AI PASS] Nuclear validation passed: {validation.get('reason', 'confirmed state-change')}" )
            except Exception as e:
                print(f"  [AI FALLBACK] Nuclear validation unavailable, keeping regex score: {e}")
        
        item_data = {
            "id": headline_id,
            "title": title,
            "url": link,
            "score": int(score),
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
        else:  # ignore or hard_ignore
            ignored_candidates.append(headline_id)
    
    print(f"\n[INFO] Categorization: Nuclear={len(nuclear_candidates)}, Major={len(major_candidates)}, Digest={len(digest_candidates)}, Ignored={len(ignored_candidates)}")
    
    # === NUCLEAR PROCESSING ===
    
    in_quiet_hours = is_in_nuclear_quiet_hours(current_time)
    print(f"\n[INFO] Nuclear quiet hours: {in_quiet_hours}")
    
    # Send queued nuclear items (if outside quiet hours)
    if not in_quiet_hours and state['nuclear_queue']:
        print(f"\n[INFO] Processing {len(state['nuclear_queue'])} queued nuclear items...")
        sent_nuclear_ids_set = set(x['id'] for x in state['nuclear_sent'])
        queue_copy = list(state['nuclear_queue'])
        successfully_sent = []
        
        for item in queue_copy:
            # Double-check not already sent
            if item['id'] in sent_nuclear_ids_set:
                print(f"\n[NUCLEAR QUEUED] SKIP (already sent): {item['title']}")
                successfully_sent.append(item)
                continue
            
            print(f"\n[NUCLEAR QUEUED] Sending: {item['title']}")
            if send_fcm_notification(
                title="F1 News",
                body=f"🚨 {item['title']}",
                data={"type": "nuclear", "url": item['url'], "score": str(item['score']), "channel_id": "f1_nuclear"},
                priority="high",
                channel_id="f1_nuclear",
                image_url=item.get('image')
            ):
                state['nuclear_sent'].append(item)
                state['sent_urls'].append(item['url'])
                state['title_fingerprints'].append(create_title_fingerprint(item['title'], item['timestamp']))
                successfully_sent.append(item)
                sent_nuclear_ids_set.add(item['id'])
            else:
                print(f"  [WARN] Send failed, keeping in queue")
        
        state['nuclear_queue'] = [item for item in state['nuclear_queue'] if item not in successfully_sent]
        print(f"[INFO] Queue processed. Remaining: {len(state['nuclear_queue'])}")
    
    # Process new nuclear items
    for item in nuclear_candidates:
        if in_quiet_hours:
            print(f"\n[NUCLEAR] Queuing (quiet hours): {item['title']}")
            state['nuclear_queue'].append(item)
        else:
            print(f"\n[NUCLEAR] Sending: {item['title']}")
            if send_fcm_notification(
                title="F1 News",
                body=f"🚨 {item['title']}",
                data={"type": "nuclear", "url": item['url'], "score": str(item['score']), "channel_id": "f1_nuclear"},
                priority="high",
                channel_id="f1_nuclear",
                image_url=item.get('image')
            ):
                state['nuclear_sent'].append(item)
                state['sent_urls'].append(item['url'])
                state['title_fingerprints'].append(create_title_fingerprint(item['title'], item['timestamp']))
    
    # === MAJOR PROCESSING ===
    
    # Build major pool (queue + new)
    major_from_queue = [item for item in state['digest_items'] if item.get('score', 0) >= MAJOR_THRESHOLD]
    all_major_candidates = major_from_queue + major_candidates
    
    # Dedup
    unique_major = {}
    for item in all_major_candidates:
        unique_major[item['id']] = item
    all_major_candidates = list(unique_major.values())
    
    # Filter already sent
    all_sent_ids = sent_nuclear_ids | sent_major_ids | queued_nuclear_ids
    all_major_candidates = [item for item in all_major_candidates if item['id'] not in all_sent_ids]
    
    # Sort by score, then timestamp
    all_major_candidates.sort(key=lambda x: (x['score'], x['timestamp']), reverse=True)
    
    print(f"\n[INFO] Major candidates: {len(all_major_candidates)}")
    
    # Send based on slots
    in_slot1 = is_in_slot1_window(current_time)
    in_slot2 = is_in_slot2_window(current_time)
    
    print(f"[INFO] Time windows: Slot1={in_slot1}, Slot2={in_slot2}")
    print(f"[INFO] Available slots: Slot1={state['slot1_remaining']}, Slot2={state['slot2_remaining']}")
    
    if in_slot1 and state['slot1_remaining'] > 0:
        to_send = all_major_candidates[:state['slot1_remaining']]
        for item in to_send:
            print(f"\n[MAJOR SLOT1] Sending: {item['title']} (score: {item['score']})")
            if send_fcm_notification(
                title="F1 News",
                body=item['title'],
                data={"type": "major", "url": item['url'], "score": str(item['score']), "channel_id": "f1_major"},
                priority="high",
                channel_id="f1_major",
                image_url=item.get('image')
            ):
                state['slot1_remaining'] -= 1
                state['major_sent'].append(item)
                state['sent_urls'].append(item['url'])
                state['title_fingerprints'].append(create_title_fingerprint(item['title'], item['timestamp']))
                all_major_candidates.remove(item)
    
    if in_slot2 and state['slot2_remaining'] > 0:
        to_send = all_major_candidates[:state['slot2_remaining']]
        for item in to_send:
            print(f"\n[MAJOR SLOT2] Sending: {item['title']} (score: {item['score']})")
            if send_fcm_notification(
                title="F1 News",
                body=item['title'],
                data={"type": "major", "url": item['url'], "score": str(item['score']), "channel_id": "f1_major"},
                priority="high",
                channel_id="f1_major",
                image_url=item.get('image')
            ):
                state['slot2_remaining'] -= 1
                state['major_sent'].append(item)
                state['sent_urls'].append(item['url'])
                state['title_fingerprints'].append(create_title_fingerprint(item['title'], item['timestamp']))
                all_major_candidates.remove(item)
    
    unsent_majors = all_major_candidates
    
    # === DIGEST QUEUE REBUILD ===
    
    # Collect all digest candidates
    all_digest_candidates = []
    
    # Existing digest items (exclude those sent as major)
    sent_major_ids_set = set(x['id'] for x in state['major_sent'])
    existing_digest = [item for item in state['digest_items'] if item['id'] not in sent_major_ids_set]
    all_digest_candidates.extend(existing_digest)
    
    # New digest items
    all_digest_candidates.extend(digest_candidates)
    
    # Unsent majors
    all_digest_candidates.extend(unsent_majors)
    
    # Dedup
    unique_digest = {}
    for item in all_digest_candidates:
        unique_digest[item['id']] = item
    all_digest_candidates = list(unique_digest.values())
    
    # Filter sent
    all_sent_ids = set(x['id'] for x in state['nuclear_sent']) | set(x['id'] for x in state['major_sent'])
    all_digest_candidates = [item for item in all_digest_candidates if item['id'] not in all_sent_ids]
    
    # Sort and keep (Protect Majors)
    pending_majors = [x for x in all_digest_candidates if x['score'] >= MAJOR_THRESHOLD]
    pending_digests = [x for x in all_digest_candidates if x['score'] < MAJOR_THRESHOLD]

    # Cap only digest items (keep top 12)
    pending_digests.sort(key=lambda x: (x['score'], x['timestamp']), reverse=True)
    pending_digests = pending_digests[:12]

    # Recombine and sort
    state['digest_items'] = pending_majors + pending_digests
    state['digest_items'].sort(key=lambda x: (x['score'], x['timestamp']), reverse=True)
    
    print(f"\n[INFO] Digest queue: {len(state['digest_items'])} items")
    if state['digest_items']:
        print(f"[INFO] Top scores: {[item['score'] for item in state['digest_items']]}")
    
    # === DIGEST SEND ===
    
    if is_in_digest_window(current_time) and not state['digest_sent']:
        print(f"\n[INFO] In digest window...")
        
        if len(state['digest_items']) >= 3:
            top3_sum = sum(item['score'] for item in state['digest_items'][:3])
            print(f"[INFO] Top 3 sum: {top3_sum} (threshold: {DIGEST_COMBINED_THRESHOLD})")
            
            if top3_sum >= DIGEST_COMBINED_THRESHOLD:
                day_of_week = current_time.strftime('%A')
                is_race_weekend = day_of_week in ['Monday', 'Friday', 'Saturday', 'Sunday']
                max_items = 6 if is_race_weekend else 4
                
                items_to_send = state['digest_items'][:max_items]
                digest_title = generate_digest_title(len(items_to_send), day_of_week)
                
                print(f"[INFO] Sending digest: {digest_title}")
                
                body_lines = []
                for item in items_to_send:
                    emoji = get_emoji_for_item(item)
                    body_lines.append(f"{emoji} {item['title']}")
                
                body = "\n".join(body_lines) + "\n\nTap to read more"
                
                send_fcm_notification(
                    title=digest_title,
                    body=body,
                    data={"type": "digest", "count": str(len(items_to_send)), "channel_id": "f1_digest", "target_tab": "news"},
                    priority="normal",
                    channel_id="f1_digest"
                )
                
                state['digest_sent'] = True
                state['digest_items'] = []
            else:
                print(f"[INFO] Threshold not met")
        else:
            print(f"[INFO] Not enough items ({len(state['digest_items'])} < 3)")
    
    # === UPDATE IGNORED ITEMS ===
    
    state['ignored_items'].extend(ignored_candidates)
    
    # === CLEANUP ===
    
    print(f"\n[INFO] Cleaning up state...")
    
    # 30-day retention for sent items
    sent_cutoff = current_time - datetime.timedelta(days=SENT_RETENTION_DAYS)
    state['nuclear_sent'] = [x for x in state['nuclear_sent'] if datetime.datetime.fromisoformat(x['timestamp']) > sent_cutoff]
    state['major_sent'] = [x for x in state['major_sent'] if datetime.datetime.fromisoformat(x['timestamp']) > sent_cutoff]
    
    # 14-day retention for digest
    digest_cutoff = current_time - datetime.timedelta(days=DIGEST_RETENTION_DAYS)
    state['digest_items'] = [x for x in state['digest_items'] if datetime.datetime.fromisoformat(x['timestamp']) > digest_cutoff]
    
    # Cap ignored items at 5000
    if len(state['ignored_items']) > IGNORED_ITEMS_CAP:
        state['ignored_items'] = state['ignored_items'][-IGNORED_ITEMS_CAP:]
    
    # Update sent_urls (30-day retention)
    # For simplicity, rebuild from nuclear_sent + major_sent
    state['sent_urls'] = list(set(
        [x['url'] for x in state['nuclear_sent']] +
        [x['url'] for x in state['major_sent']]
    ))
    
    # Update title_fingerprints (keep last 200)
    all_sent = state['nuclear_sent'] + state['major_sent']
    all_sent.sort(key=lambda x: x['timestamp'], reverse=True)
    state['title_fingerprints'] = [
        create_title_fingerprint(x['title'], x['timestamp'])
        for x in all_sent[:200]
    ]
    
    print(f"[INFO] Cleanup: nuclear_sent={len(state['nuclear_sent'])}, major_sent={len(state['major_sent'])}, ignored={len(state['ignored_items'])}")
    
    # === SAVE STATE ===
    
    save_state(state)
    print("[INFO] Run completed.")

if __name__ == "__main__":
    main()
