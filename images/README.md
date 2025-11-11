# F1 Dashboard Image Repository

This directory contains documentation and structure for all images used in the F1 Dashboard application.

## Directory Structure

```
images/
├── flags/              # Country flag images
│   ├── small/         # 24x18 flag images
│   └── large/         # 48x36 flag images
├── teams/             # F1 team images
│   ├── cars/          # Team car images
│   ├── logos/         # Team logo images
│   └── big-logos/     # Large team logo images
├── circuits/          # Circuit/track images
│   ├── track-layouts/ # Circuit track layout images
│   └── carbon-backgrounds/ # Circuit carbon background images
├── placeholders/      # Placeholder images for fallbacks
├── driver-headshots/  # Driver headshot images (from API)
└── IMAGE_CATALOG.md   # Complete catalog of all image URLs
```

## Image Sources

### External URLs Currently Used

1. **Flag Images**: `flagcdn.com`
   - Small flags: `https://flagcdn.com/24x18/{isoCode}.png`
   - Large flags: `https://flagcdn.com/48x36/{isoCode}.png`

2. **Team Images**: `media.formula1.com`
   - Team cars: `https://media.formula1.com/d_team_car_fallback_image.png/content/dam/fom-website/teams/2025/{teamName}.png`
   - Team logos: `https://media.formula1.com/content/dam/fom-website/teams/2025/{teamName}-logo.png`
   - Big logos: Various URLs (see IMAGE_CATALOG.md)

3. **Circuit Images**: `media.formula1.com`
   - Track layouts: `https://media.formula1.com/image/upload/f_auto,c_limit,q_auto/content/dam/fom-website/2018-redesign-assets/Circuit%20maps%2016x9/{circuitName}_Circuit`
   - Carbon backgrounds: `https://media.formula1.com/image/upload/f_auto,c_limit,w_{width},q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/{circuitName}%20carbon`

4. **Placeholder Images**: `via.placeholder.com`
   - Various placeholder images for fallback scenarios

5. **Driver Headshots**: From F1 API
   - `driver.headshot_url` - Provided by the F1 API service

## Usage

All images are currently loaded via URLs from external sources. This repository structure is provided for:
- Documentation purposes
- Future migration to local assets
- Reference for image naming conventions
- Tracking all images used in the application

## Image Lists

Each directory contains a `MANIFEST.md` file that lists:
- All image files that should exist in that directory
- Source URLs for downloading each image
- Example download commands

### Quick Access to Image Lists

- **[IMAGE_INDEX.md](./IMAGE_INDEX.md)** - Complete list of all images in one place
- **[IMAGE_CATALOG.md](./IMAGE_CATALOG.md)** - Detailed catalog with usage information
- **[IMAGE_URLS.md](./IMAGE_URLS.md)** - Quick reference for URL patterns

### Directory Manifests

- `flags/small/MANIFEST.md` - Small flag images (24x18)
- `flags/large/MANIFEST.md` - Large flag images (48x36)
- `teams/cars/MANIFEST.md` - Team car images
- `teams/logos/MANIFEST.md` - Small team logos
- `teams/big-logos/MANIFEST.md` - Large team logos
- `circuits/track-layouts/MANIFEST.md` - Circuit track layouts
- `circuits/carbon-backgrounds/MANIFEST.md` - Circuit carbon backgrounds
- `placeholders/MANIFEST.md` - Placeholder images
- `driver-headshots/MANIFEST.md` - Driver headshot information

For complete details on all image URLs and their usage, see [IMAGE_CATALOG.md](./IMAGE_CATALOG.md).

## Developer Documentation

### Getting Started
- **[QUICK_START.md](./QUICK_START.md)** - Quick reference guide for developers (start here!)
- **[DEVELOPER_GUIDE.md](./DEVELOPER_GUIDE.md)** - Complete developer guide with examples and troubleshooting

### Reference Documentation
- **[NAMING_CONVENTIONS.md](./NAMING_CONVENTIONS.md)** - Complete naming conventions and usage guide
- **[IMAGE_INDEX.md](./IMAGE_INDEX.md)** - Complete list of all images
- **[IMAGE_CATALOG.md](./IMAGE_CATALOG.md)** - Detailed catalog with usage information
- **[IMAGE_URLS.md](./IMAGE_URLS.md)** - Quick reference for URL patterns

### Structure & Status
- **[STRUCTURE.md](./STRUCTURE.md)** - Directory structure explanation
- **[DOWNLOAD_SUMMARY.md](./DOWNLOAD_SUMMARY.md)** - Summary of downloaded images

