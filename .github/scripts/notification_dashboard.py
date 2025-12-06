import streamlit as st
import firebase_admin
from firebase_admin import credentials, messaging
import json
import os
from datetime import datetime

# Page Config
st.set_page_config(
    page_title="BOXBOXBOX Notification Center",
    page_icon="🏎️",
    layout="centered"
)

# Styling
st.markdown("""
    <style>
    .stButton>button {
        width: 100%;
        background-color: #FF1801;
        color: white;
        font-weight: bold;
        border: none;
        padding: 0.5rem;
    }
    .stButton>button:hover {
        background-color: #D00000;
        color: white;
    }
    .success-box {
        padding: 1rem;
        background-color: #D4EDDA;
        color: #155724;
        border-radius: 0.5rem;
        margin-bottom: 1rem;
    }
    </style>
""", unsafe_allow_html=True)

# Header
st.title("🏎️ BOXBOXBOX Command Center")
st.markdown("Send manual notifications with **custom sound** guarantee.")

# --- Firebase Init ---
if not firebase_admin._apps:
    # Try to get credentials from env or file
    cred_json = os.environ.get("FIREBASE_CREDENTIALS")
    
    # Fallback to local file if env not set (common for local dev)
    if not cred_json and os.path.exists("firebase_credentials.json"):
        with open("firebase_credentials.json", "r") as f:
            cred_json = f.read()
            
    if cred_json:
        try:
            cred_dict = json.loads(cred_json)
            cred = credentials.Certificate(cred_dict)
            firebase_admin.initialize_app(cred)
            st.sidebar.success("Firebase Connected ✅")
        except Exception as e:
            st.error(f"Firebase Init Error: {e}")
            st.stop()
    else:
        st.warning("⚠️ No credentials found.")
        st.info("Please set `FIREBASE_CREDENTIALS` env var or place `firebase_credentials.json` in this directory.")
        
        # Allow pasting credentials
        with st.expander("Or paste credentials JSON here (unsafe for public streams)"):
            pasted_creds = st.text_area("JSON Credentials")
            if pasted_creds:
                try:
                    cred_dict = json.loads(pasted_creds)
                    cred = credentials.Certificate(cred_dict)
                    firebase_admin.initialize_app(cred)
                    st.rerun()
                except Exception as e:
                    st.error(f"Invalid JSON: {e}")
        st.stop()

# --- Form ---
with st.form("notify_form"):
    st.subheader("Compose Notification")
    
    col1, col2 = st.columns([3, 1])
    with col1:
        title = st.text_input("Title", value="F1 Update")
    with col2:
        priority = st.selectbox("Priority", ["high", "normal"], index=0)
        
    body = st.text_area("Message Body", height=100, placeholder="Enter the notification content here...")
    
    st.markdown("### Configuration")
    c1, c2 = st.columns(2)
    with c1:
        channel_id = st.selectbox(
            "Channel (Sound)", 
            [
                "f1_major",       # High Priority, Custom Sound
                "f1_nuclear",     # Max Priority, Custom Sound
                "f1_digest",      # Default Sound (usually)
                "f1_app_updates"  # Default Sound
            ],
            index=0,
            help="Select 'f1_major' or 'f1_nuclear' for the custom 'Apop' sound."
        )
    with c2:
        target_tab = st.selectbox("Target Tab", ["news", "live", "standings", "calendar"], index=0)
        
    image_url = st.text_input("Image URL (Optional)", placeholder="https://...")
    
    # Preview
    if title and body:
        st.markdown("---")
        st.markdown("**Preview:**")
        st.info(f"**{title}**\n\n{body}")
        if image_url:
            st.image(image_url, width=300)
    
    submitted = st.form_submit_button("🚀 SEND NOTIFICATION")

# --- Logic ---
if submitted:
    if not title or not body:
        st.error("Title and Body are required.")
    else:
        try:
            # Construct DATA-ONLY payload
            # This is the key: We put everything in 'data' so the Android app's
            # onMessageReceived triggers and builds the notification manually
            # with the custom sound.
            message = messaging.Message(
                topic="f1_updates",  # Sending to the main topic
                data={
                    "title": title,
                    "body": body,
                    "channel_id": channel_id,
                    "target_tab": target_tab,
                    "image_url": image_url if image_url else "",
                    "type": "manual_dispatch",
                    "timestamp": datetime.utcnow().isoformat()
                },
                android=messaging.AndroidConfig(
                    priority=priority,
                    ttl=3600  # 1 hour TTL
                )
            )
            
            response = messaging.send(message)
            st.markdown(f"""
                <div class="success-box">
                    <b>SUCCESS!</b><br>
                    Message sent to topic <code>f1_updates</code><br>
                    ID: <code>{response}</code>
                </div>
            """, unsafe_allow_html=True)
            
        except Exception as e:
            st.error(f"Failed to send: {e}")
