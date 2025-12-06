import requests
import websocket
import json
import urllib.parse
import time
import threading
import ssl

# Configuration
BASE_URL = "https://livetiming.formula1.com/signalr"
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

start_time = time.time()

def log_time(event):
    elapsed = (time.time() - start_time) * 1000
    print(f"[TIME] {elapsed:.0f}ms - {event}")

def get_connection_token():
    log_time("Starting Negotiation")
    url = f"{BASE_URL}/negotiate"
    params = {
        "clientProtocol": "1.5",
        "connectionData": '[{"name":"Streaming"}]'
    }
    headers = {
        "User-Agent": USER_AGENT
    }
    
    response = requests.get(url, params=params, headers=headers)
    log_time("Negotiation Response Received")
    response.raise_for_status()
    data = response.json()
    
    token = data.get("ConnectionToken")
    
    cookies = response.cookies.get_dict()
    cookie_str = "; ".join([f"{k}={v}" for k, v in cookies.items()])
    
    return token, cookie_str

def on_message(ws, message):
    log_time("Message Received")
    # Only print the first message to avoid spamming
    # ws.close() 

def on_error(ws, error):
    print(f"[ERROR] {error}")

def on_close(ws, close_status_code, close_msg):
    log_time("Connection Closed")

def on_open(ws):
    log_time("WebSocket Connected")
    
    # Subscribe to topics
    subscribe_cmd = {
        "H": "Streaming",
        "M": "Subscribe",
        "A": [["Heartbeat", "TimingData", "CarData.z", "Position.z", "SessionInfo", "TrackStatus", "ExtrapolatedClock"]],
        "I": 1
    }
    ws.send(json.dumps(subscribe_cmd))
    log_time("Subscribe Command Sent")

def main():
    try:
        token, cookie_str = get_connection_token()
        
        # Construct WebSocket URL
        encoded_data = urllib.parse.quote('[{"name":"Streaming"}]')
        ws_url = f"wss://livetiming.formula1.com/signalr/connect?transport=webSockets&clientProtocol=1.5&connectionToken={urllib.parse.quote(token)}&connectionData={encoded_data}"
        
        log_time("Connecting WebSocket...")
        ws = websocket.WebSocketApp(
            ws_url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            header={
                "User-Agent": USER_AGENT,
                "Cookie": cookie_str
            }
        )
        
        # Run for 5 seconds
        def close_timer():
            time.sleep(5)
            ws.close()
        
        threading.Thread(target=close_timer).start()
        ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE})
        
    except Exception as e:
        print(f"[ERROR] {e}")

if __name__ == "__main__":
    main()
