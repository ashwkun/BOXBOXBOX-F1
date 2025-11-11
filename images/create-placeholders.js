#!/usr/bin/env node

/**
 * Create simple placeholder images using canvas (if available) or skip
 * This is a fallback script for creating placeholder images
 */

const fs = require('fs');
const path = require('path');

// Create placeholders directory
const placeholdersDir = path.join(__dirname, 'placeholders');
if (!fs.existsSync(placeholdersDir)) {
  fs.mkdirSync(placeholdersDir, { recursive: true });
}

// Try to use canvas if available, otherwise create empty files as placeholders
let canvas;
try {
  canvas = require('canvas');
} catch (e) {
  console.log('Canvas module not available. Creating empty placeholder files.');
  console.log('You can manually create these images or install canvas: npm install canvas');
  
  // Create empty files as placeholders
  const placeholders = [
    { name: 'unknown-flag-small.png', size: '24x18' },
    { name: 'unknown-flag-large.png', size: '48x36' },
    { name: 'no-team-car.png', size: '800x200' },
    { name: 'no-logo-small.png', size: '200x100' },
    { name: 'no-logo-large.png', size: '1320x400' },
    { name: 'logo-not-available.png', size: '1320x400' },
    { name: 'no-circuit-image.png', size: '800x450' },
    { name: 'no-circuit-background.png', size: '1440x1080' },
    { name: 'driver-headshot-placeholder.png', size: '80x80' },
  ];
  
  placeholders.forEach(p => {
    const filePath = path.join(placeholdersDir, p.name);
    if (!fs.existsSync(filePath)) {
      fs.writeFileSync(filePath, '');
      console.log(`Created placeholder file: ${p.name} (${p.size})`);
    }
  });
  
  console.log('\nNote: These are empty files. You can download placeholder images from:');
  console.log('https://via.placeholder.com/ or create them manually.');
  process.exit(0);
}

// If canvas is available, create actual images
function createPlaceholder(width, height, text, filename) {
  const img = canvas.createCanvas(width, height);
  const ctx = img.getContext('2d');
  
  // Background
  ctx.fillStyle = '#CCCCCC';
  ctx.fillRect(0, 0, width, height);
  
  // Text
  ctx.fillStyle = '#666666';
  ctx.font = `${Math.min(width, height) / 3}px Arial`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(text, width / 2, height / 2);
  
  // Save
  const buffer = img.toBuffer('image/png');
  fs.writeFileSync(path.join(placeholdersDir, filename), buffer);
  console.log(`Created ${filename} (${width}x${height})`);
}

createPlaceholder(24, 18, '?', 'unknown-flag-small.png');
createPlaceholder(48, 36, '?', 'unknown-flag-large.png');
createPlaceholder(800, 200, 'No Team Car\nAvailable', 'no-team-car.png');
createPlaceholder(200, 100, 'No Logo\nAvailable', 'no-logo-small.png');
createPlaceholder(1320, 400, 'No Logo Available', 'no-logo-large.png');
createPlaceholder(1320, 400, 'Logo Not Available', 'logo-not-available.png');
createPlaceholder(800, 450, 'No Circuit Image\nAvailable', 'no-circuit-image.png');
createPlaceholder(1440, 1080, 'No Circuit Background\nAvailable', 'no-circuit-background.png');
createPlaceholder(80, 80, '?', 'driver-headshot-placeholder.png');

console.log('\n✅ All placeholder images created!');

