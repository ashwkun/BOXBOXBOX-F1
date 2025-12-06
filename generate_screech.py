import wave
import struct
import math
import random

def generate_screech_sound(filename, duration_sec=0.5, sample_rate=44100):
    num_samples = int(duration_sec * sample_rate)
    
    # Pink noise state
    b0 = b1 = b2 = b3 = b4 = b5 = b6 = 0.0
    
    with wave.open(filename, 'w') as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(1)  # 8-bit
        wav_file.setframerate(sample_rate)
        
        for i in range(num_samples):
            white = random.uniform(-1.0, 1.0)
            
            # Pink noise filter (Paul Kellet's refined method)
            b0 = 0.99886 * b0 + white * 0.0555179
            b1 = 0.99332 * b1 + white * 0.0750759
            b2 = 0.96900 * b2 + white * 0.1538520
            b3 = 0.86650 * b3 + white * 0.3104856
            b4 = 0.55000 * b4 + white * 0.5329522
            b5 = -0.7616 * b5 - white * 0.0168980
            pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
            b6 = white * 0.115926
            
            # Normalize roughly
            value = pink * 0.1
            
            # Apply envelope to fade in/out slightly to avoid clicks
            if i < 1000:
                value *= (i / 1000.0)
            elif i > num_samples - 1000:
                value *= ((num_samples - i) / 1000.0)
            
            # Very low volume
            value = value * 0.15
            
            # Clip
            value = max(-1.0, min(1.0, value))
            
            # Convert to 8-bit unsigned (0-255)
            sample = int((value + 1.0) * 127.5)
            wav_file.writeframes(struct.pack('B', sample))

if __name__ == "__main__":
    generate_screech_sound("app/src/main/res/raw/screech_loop.wav", duration_sec=1.0)
    print("Generated screech_loop.wav")
