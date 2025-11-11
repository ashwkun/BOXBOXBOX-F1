#!/usr/bin/env python3
"""
Create placeholder images locally since via.placeholder.com is not accessible
"""

from PIL import Image, ImageDraw, ImageFont
import os

def create_placeholder(width, height, text, filename):
    """Create a placeholder image with text"""
    # Create image with light gray background
    img = Image.new('RGB', (width, height), color='#CCCCCC')
    draw = ImageDraw.Draw(img)
    
    # Try to use a default font, fallback to basic if not available
    try:
        # Try to use a larger font
        font_size = min(width, height) // 3
        font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", font_size)
    except:
        try:
            font = ImageFont.load_default()
        except:
            font = None
    
    # Calculate text position (centered)
    if font:
        bbox = draw.textbbox((0, 0), text, font=font)
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]
    else:
        text_width = len(text) * 6
        text_height = 11
    
    position = ((width - text_width) // 2, (height - text_height) // 2)
    
    # Draw text
    draw.text(position, text, fill='#666666', font=font)
    
    # Save image
    img.save(filename)
    print(f"Created {filename} ({width}x{height})")

# Create placeholders directory if it doesn't exist
os.makedirs('placeholders', exist_ok=True)

# Create all placeholder images
create_placeholder(24, 18, "?", "placeholders/unknown-flag-small.png")
create_placeholder(48, 36, "?", "placeholders/unknown-flag-large.png")
create_placeholder(800, 200, "No Team Car\nAvailable", "placeholders/no-team-car.png")
create_placeholder(200, 100, "No Logo\nAvailable", "placeholders/no-logo-small.png")
create_placeholder(1320, 400, "No Logo Available", "placeholders/no-logo-large.png")
create_placeholder(1320, 400, "Logo Not Available", "placeholders/logo-not-available.png")
create_placeholder(800, 450, "No Circuit Image\nAvailable", "placeholders/no-circuit-image.png")
create_placeholder(1440, 1080, "No Circuit Background\nAvailable", "placeholders/no-circuit-background.png")
create_placeholder(80, 80, "?", "placeholders/driver-headshot-placeholder.png")

print("\n✅ All placeholder images created!")

