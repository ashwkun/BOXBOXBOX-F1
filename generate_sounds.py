#!/usr/bin/env python3
"""Generate retro 8-bit sound effects for the Hotlap game."""
import wave
import struct
import math
import os

OUTPUT_DIR = "/Users/aswinc/BOXBOXBOX Antigravity/BOXBOXBOX-F1/app/src/main/res/raw"

def generate_tone(filename, frequency, duration_ms, volume=0.5, fade_out=True, wave_type="square"):
    """Generate a retro 8-bit style tone."""
    sample_rate = 22050
    num_samples = int(sample_rate * duration_ms / 1000)
    
    samples = []
    for i in range(num_samples):
        t = i / sample_rate
        
        if wave_type == "square":
            # Square wave for that classic 8-bit sound
            value = 1.0 if math.sin(2.0 * math.pi * frequency * t) > 0 else -1.0
        elif wave_type == "triangle":
            # Triangle wave
            period = 1.0 / frequency
            value = 4.0 * abs((t / period) - math.floor(t / period + 0.5)) - 1.0
        else:
            # Sine wave
            value = math.sin(2.0 * math.pi * frequency * t)
        
        # Apply fade out
        if fade_out:
            fade = 1.0 - (i / num_samples) ** 0.5
        else:
            fade = 1.0
        
        value = int(value * volume * fade * 32767)
        samples.append(struct.pack('<h', max(-32767, min(32767, value))))
    
    filepath = os.path.join(OUTPUT_DIR, filename)
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(b''.join(samples))
    print(f"Created: {filename}")

def generate_arpeggio(filename, frequencies, note_duration_ms, volume=0.5):
    """Generate an arpeggio (multiple notes in sequence)."""
    sample_rate = 22050
    samples = []
    
    for freq in frequencies:
        num_samples = int(sample_rate * note_duration_ms / 1000)
        for i in range(num_samples):
            t = i / sample_rate
            # Square wave
            value = 1.0 if math.sin(2.0 * math.pi * freq * t) > 0 else -1.0
            fade = 1.0 - (i / num_samples) ** 0.3
            value = int(value * volume * fade * 32767)
            samples.append(struct.pack('<h', max(-32767, min(32767, value))))
    
    filepath = os.path.join(OUTPUT_DIR, filename)
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(b''.join(samples))
    print(f"Created: {filename}")

# Countdown beeps
generate_tone("beep_countdown.wav", 440, 150, volume=0.4)  # A4 beep
generate_tone("beep_go.wav", 880, 300, volume=0.5)  # Higher pitch for GO

# Sector crossing sounds
generate_tone("sector_wr.wav", 1047, 100, volume=0.4)  # High C - WR (best)
generate_arpeggio("sector_pb.wav", [523, 659, 784], 60, volume=0.35)  # C-E-G arpeggio - PB
generate_tone("sector_slow.wav", 220, 100, volume=0.3)  # Low pitch - slower

# DNF sound (descending tones)
generate_arpeggio("sound_dnf.wav", [440, 330, 220, 147], 100, volume=0.4)  # Descending arpeggio

# Finish sounds
generate_arpeggio("finish_normal.wav", [523, 659, 784], 100, volume=0.4)  # C major arpeggio
generate_arpeggio("finish_pb.wav", [523, 659, 784, 1047], 80, volume=0.45)  # C major + high C
generate_arpeggio("finish_wr.wav", [523, 659, 784, 1047, 1319, 1568], 70, volume=0.5)  # Triumphant arpeggio

print("All sound files generated!")
