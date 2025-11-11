# Placeholder Images

This directory should contain placeholder images used as fallbacks when external images fail to load.

## Image Files Required

- `unknown-flag-small.png` (24x18) - Unknown flag placeholder
- `unknown-flag-large.png` (48x36) - Unknown flag placeholder (large)
- `no-team-car.png` (800x200) - No team car available
- `no-logo-small.png` (200x100) - No logo available (small)
- `no-logo-large.png` (1320x400) - No logo available (large)
- `logo-not-available.png` (1320x400) - Logo not available
- `no-circuit-image.png` (800x450) - No circuit image available
- `no-circuit-background.png` (1440x1080) - No circuit background available
- `driver-headshot-placeholder.png` (80x80) - Driver headshot placeholder (generic)

## Source URLs

All placeholder images can be generated from:
```
https://via.placeholder.com/{width}x{height}?text={text}
```

### Specific Placeholders

1. **Unknown Flag (Small)**
   - URL: `https://via.placeholder.com/24x18?text=?`
   - File: `unknown-flag-small.png`

2. **Unknown Flag (Large)**
   - URL: `https://via.placeholder.com/48x36?text=?`
   - File: `unknown-flag-large.png`

3. **No Team Car Available**
   - URL: `https://via.placeholder.com/800x200?text=No+Team+Car+Available`
   - File: `no-team-car.png`

4. **No Logo Available (Small)**
   - URL: `https://via.placeholder.com/200x100?text=No+Logo+Available`
   - File: `no-logo-small.png`

5. **No Logo Available (Large)**
   - URL: `https://via.placeholder.com/1320x400?text=No+Logo+Available`
   - File: `no-logo-large.png`

6. **Logo Not Available**
   - URL: `https://via.placeholder.com/1320x400?text=Logo+Not+Available`
   - File: `logo-not-available.png`

7. **No Circuit Image Available**
   - URL: `https://via.placeholder.com/800x450?text=No+Circuit+Image+Available`
   - File: `no-circuit-image.png`

8. **No Circuit Background Available**
   - URL: `https://via.placeholder.com/1440x1080?text=No+Circuit+Background+Available`
   - File: `no-circuit-background.png`

9. **Driver Headshot Placeholder (Generic)**
   - URL: `https://via.placeholder.com/80?text=?`
   - File: `driver-headshot-placeholder.png`
   - Note: Dynamic placeholders use `{driver.name_acronym}` in the URL

## Example Download Commands

```bash
# Download all placeholder images
curl -o "unknown-flag-small.png" "https://via.placeholder.com/24x18?text=?"
curl -o "unknown-flag-large.png" "https://via.placeholder.com/48x36?text=?"
curl -o "no-team-car.png" "https://via.placeholder.com/800x200?text=No+Team+Car+Available"
curl -o "no-logo-small.png" "https://via.placeholder.com/200x100?text=No+Logo+Available"
curl -o "no-logo-large.png" "https://via.placeholder.com/1320x400?text=No+Logo+Available"
curl -o "logo-not-available.png" "https://via.placeholder.com/1320x400?text=Logo+Not+Available"
curl -o "no-circuit-image.png" "https://via.placeholder.com/800x450?text=No+Circuit+Image+Available"
curl -o "no-circuit-background.png" "https://via.placeholder.com/1440x1080?text=No+Circuit+Background+Available"
curl -o "driver-headshot-placeholder.png" "https://via.placeholder.com/80?text=?"
```

