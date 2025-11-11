# Image Repository Structure

This document describes the complete structure of the image repository.

## Directory Tree

```
images/
├── README.md                    # Main documentation
├── IMAGE_CATALOG.md            # Complete catalog of all images
├── IMAGE_URLS.md               # Quick reference for URLs
├── STRUCTURE.md                # This file
├── .gitkeep                    # Git tracking file
│
├── flags/                      # Country flag images
│   ├── small/                  # 24x18 flag images
│   └── large/                  # 48x36 flag images
│
├── teams/                      # F1 team images
│   ├── cars/                   # Team car images (10 teams)
│   ├── logos/                  # Small team logos (10 teams)
│   └── big-logos/              # Large team logos (10 teams + 4 circuit mappings)
│
├── circuits/                   # Circuit/track images
│   ├── track-layouts/          # Circuit track layout images (24 circuits)
│   └── carbon-backgrounds/     # Circuit carbon background images (24 circuits)
│
├── placeholders/               # Placeholder images for fallbacks (9 types)
│
└── driver-headshots/           # Driver headshot images (from API, dynamic)
```

## File Descriptions

### Documentation Files

1. **README.md**
   - Overview of the image repository
   - Directory structure explanation
   - Image sources and usage
   - Links to detailed documentation

2. **IMAGE_CATALOG.md**
   - Complete catalog of all images
   - Detailed URL patterns
   - Usage by component
   - Usage by utility file
   - Future migration considerations

3. **IMAGE_URLS.md**
   - Quick reference for all image URLs
   - URL patterns and examples
   - Image count summary

4. **STRUCTURE.md** (this file)
   - Directory structure visualization
   - File descriptions
   - Organization rationale

### Directory Purposes

#### flags/
- **Purpose**: Store country flag images
- **Subdirectories**:
  - `small/`: 24x18 pixel flags for compact UI elements
  - `large/`: 48x36 pixel flags for headers and larger displays
- **Current Source**: flagcdn.com
- **Count**: ~30+ countries × 2 sizes = 60+ images

#### teams/
- **Purpose**: Store F1 team-related images
- **Subdirectories**:
  - `cars/`: Team car images (10 teams)
  - `logos/`: Small team logos (10 teams)
  - `big-logos/`: Large team logos (10 teams + 4 circuit mappings)
- **Current Source**: media.formula1.com
- **Count**: ~34 images total

#### circuits/
- **Purpose**: Store circuit/track images
- **Subdirectories**:
  - `track-layouts/`: Circuit track layout images (16:9 aspect ratio)
  - `carbon-backgrounds/`: Circuit carbon background images (4:3 aspect ratio)
- **Current Source**: media.formula1.com
- **Count**: 24 circuits × 2 types = 48 images

#### placeholders/
- **Purpose**: Store placeholder images for fallback scenarios
- **Types**: 9 different placeholder images
- **Current Source**: via.placeholder.com
- **Usage**: Fallback when external images fail to load

#### driver-headshots/
- **Purpose**: Store driver headshot images
- **Current Source**: F1 API (dynamic URLs)
- **Note**: These are provided by the API and not hardcoded

## Organization Rationale

### Why This Structure?

1. **Categorization by Type**: Images are grouped by their purpose (flags, teams, circuits, etc.)
2. **Size Separation**: Flags are separated by size for easy management
3. **Team Organization**: Team images are grouped together for easy updates
4. **Circuit Organization**: Circuit images are separated by type (layout vs background)
5. **Future-Proof**: Structure allows for easy migration from external URLs to local assets

### Benefits

- **Clear Organization**: Easy to find specific image types
- **Scalability**: Can add new images without restructuring
- **Documentation**: Each directory has a clear purpose
- **Migration Ready**: Structure supports future local asset migration
- **Version Control**: Can track image changes over time

## Usage Guidelines

### Adding New Images

1. Place images in the appropriate directory based on type
2. Use consistent naming conventions:
   - Flags: `{isoCode}.png` (e.g., `gb.png`, `us.png`)
   - Teams: `{normalizedName}.png` (e.g., `mclaren.png`)
   - Circuits: `{circuitName}.png` (e.g., `silverstone.png`)
3. Update documentation files if adding new image types
4. Update utility functions to reference new local paths

### Naming Conventions

- **Flags**: Use ISO country codes (lowercase)
- **Teams**: Use normalized team names (lowercase, hyphens)
- **Circuits**: Use circuit IDs from `circuitUtils.ts`
- **Placeholders**: Use descriptive names (e.g., `no-team-car.png`)

## Migration Path

When ready to migrate from external URLs to local assets:

1. Download images from external sources
2. Place them in appropriate directories
3. Update utility functions (`flagUtils.ts`, `teamUtils.ts`, `circuitUtils.ts`)
4. Update image paths from URLs to local imports
5. Test all image displays
6. Remove external URL dependencies

## Statistics

- **Total Image Categories**: 5 (flags, teams, circuits, placeholders, driver-headshots)
- **Total Subdirectories**: 8
- **Estimated Total Images**: ~150+ unique image patterns
- **Documentation Files**: 4
- **External Sources**: 3 (flagcdn.com, media.formula1.com, via.placeholder.com)

