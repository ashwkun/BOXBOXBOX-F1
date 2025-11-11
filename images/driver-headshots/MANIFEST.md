# Driver Headshot Images

This directory is for driver headshot images that come from the F1 API.

## Note

Driver headshots are **not hardcoded** in the application. They come dynamically from the F1 API via the `driver.headshot_url` field. These URLs are provided by the API service and may change over time.

## Usage

Driver headshots are displayed in:
- `src/components/DriversList.tsx` - Driver cards

## Fallback Behavior

When a driver headshot fails to load, the application falls back to:
- A placeholder image: `https://via.placeholder.com/80?text={driver.name_acronym}`
- Or a styled div with the driver's acronym

## Storage Considerations

If you want to cache driver headshots locally:
1. Monitor the F1 API for driver headshot URLs
2. Download images as they appear
3. Map driver IDs/names to local file paths
4. Update the component to check for local files first, then fall back to API URLs

## Example Structure (if caching locally)

```
driver-headshots/
├── {driver_id}.png
├── {driver_name_acronym}.png
└── ...
```

## Current Implementation

The application currently uses `driver.headshot_url` directly from the API response, with error handling that falls back to placeholder images.

