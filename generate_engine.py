import wave
import struct
import math
import random

def generate_engine_sound(filename, duration_sec=1.0, sample_rate=44100):
    num_samples = int(duration_sec * sample_rate)
    
    # Engine parameters
    base_freq = 40.0  # Extremely low rumble
    
    with wave.open(filename, 'w') as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(1)  # 8-bit
        wav_file.setframerate(sample_rate)
        
        for i in range(num_samples):
            t = float(i) / sample_rate
            
            # Sine wave
            val = math.sin(2 * math.pi * base_freq * t)
            
            # Soft clipping to add dull harmonics (like a muffler)
            if val > 0.5: val = 0.5 + (val - 0.5) * 0.5
            if val < -0.5: val = -0.5 + (val + 0.5) * 0.5
            
            # Very low volume
            value = val * 0.2
            
            # Convert to 8-bit unsigned (0-255)
            sample = int((value + 1.0) * 127.5)
            wav_file.writeframes(struct.pack('B', sample))

if __name__ == "__main__":
    generate_engine_sound("app/src/main/res/raw/engine_loop.wav", duration_sec=2.0)
    print("Generated engine_loop.wav")
