# Image Naming Conventions Documentation

This document explains the naming conventions and file structure for all images in the F1 Dashboard image repository. This guide helps developers understand how images are organized and named.

## Table of Contents

1. [Directory Structure](#directory-structure)
2. [Flag Images](#flag-images)
3. [Team Images](#team-images)
4. [Circuit Images](#circuit-images)
5. [Placeholder Images](#placeholder-images)
6. [File Naming Patterns](#file-naming-patterns)
7. [Usage Examples](#usage-examples)
8. [Best Practices](#best-practices)

---

## Directory Structure

```
images/
├── flags/
│   ├── small/          # 24x18 flag images
│   └── large/          # 48x36 flag images
├── teams/
│   ├── cars/           # Team car images
│   ├── logos/          # Small team logos
│   └── big-logos/      # Large team logos
├── circuits/
│   ├── track-layouts/  # Circuit track layout images
│   └── carbon-backgrounds/  # Circuit carbon background images
├── placeholders/       # Placeholder/fallback images
└── driver-headshots/   # Driver headshot images (from API)
```

---

## Flag Images

### Location
- Small flags: `flags/small/`
- Large flags: `flags/large/`

### Naming Convention
**Pattern**: `{isoCode}.png`

**Format**: 
- Lowercase ISO 3166-1 alpha-2 country codes
- File extension: `.png`
- No spaces, hyphens, or special characters

### Examples
```
gb.png      → Great Britain
us.png      → United States
de.png      → Germany
fr.png      → France
it.png      → Italy
es.png      → Spain
au.png      → Australia
jp.png      → Japan
cn.png      → China
br.png      → Brazil
mx.png      → Mexico
ca.png      → Canada
nl.png      → Netherlands
mc.png      → Monaco
ch.png      → Switzerland
ae.png      → United Arab Emirates
sa.png      → Saudi Arabia
qa.png      → Qatar
bh.png      → Bahrain
```

### Country Code Mapping

The application uses F1 country codes that may differ from ISO codes. The mapping is handled in `src/utils/flagUtils.ts`:

| F1 Code | ISO Code | Country |
|---------|----------|---------|
| UK, GBR | gb | Great Britain |
| USA | us | United States |
| GER | de | Germany |
| FRA | fr | France |
| ITA | it | Italy |
| ESP | es | Spain |
| AUS | au | Australia |
| JPN | jp | Japan |
| CHN | cn | China |
| BRA | br | Brazil |
| MEX | mx | Mexico |
| CAN | ca | Canada |
| NED, NET | nl | Netherlands |
| MON | mc | Monaco |
| SUI | ch | Switzerland |
| UAE | ae | United Arab Emirates |
| RSA | za | South Africa |
| DEN | dk | Denmark |

### Usage in Code
```typescript
// Get flag URL
import { getFlagUrl, getLargeFlagUrl } from '../utils/flagUtils';

const smallFlag = getFlagUrl('GBR');      // Returns path to flags/small/gb.png
const largeFlag = getLargeFlagUrl('USA'); // Returns path to flags/large/us.png
```

---

## Team Images

### Team Car Images

**Location**: `teams/cars/`

**Naming Convention**: `{normalizedTeamName}.png`

**Format**:
- Lowercase team names
- Hyphens replace spaces
- No special characters
- File extension: `.png`

**Examples**:
```
mclaren.png
mercedes.png
red-bull-racing.png
williams.png
aston-martin.png
kick-sauber.png
ferrari.png
alpine.png
racing-bulls.png
haas.png
```

**Team Name Normalization**:
The application normalizes team names using `getNormalizedTeamName()` in `src/utils/teamUtils.ts`:

| Official Name | Normalized Name | Filename |
|--------------|-----------------|----------|
| McLaren Formula 1 Team | mclaren | `mclaren.png` |
| Mercedes-AMG PETRONAS F1 Team | mercedes | `mercedes.png` |
| Oracle Red Bull Racing | red-bull-racing | `red-bull-racing.png` |
| Williams Racing | williams | `williams.png` |
| Aston Martin Aramco F1 Team | aston-martin | `aston-martin.png` |
| Stake F1 Team Kick Sauber | kick-sauber | `kick-sauber.png` |
| Scuderia Ferrari | ferrari | `ferrari.png` |
| BWT Alpine F1 Team | alpine | `alpine.png` |
| Visa Cash App RB Formula One Team | racing-bulls | `racing-bulls.png` |
| MoneyGram Haas F1 Team | haas | `haas.png` |

### Team Logos (Small)

**Location**: `teams/logos/`

**Naming Convention**: `{normalizedTeamName}-logo.png`

**Format**:
- Same normalized team name as car images
- Suffix: `-logo`
- File extension: `.png`

**Examples**:
```
mclaren-logo.png
mercedes-logo.png
red-bull-racing-logo.png
williams-logo.png
aston-martin-logo.png
kick-sauber-logo.png
ferrari-logo.png
alpine-logo.png
racing-bulls-logo.png
haas-logo.png
```

### Team Big Logos

**Location**: `teams/big-logos/`

**Naming Convention**: `{normalizedName}.png`

**Format**:
- Team logos: Same normalized names as car images
- Circuit logos: Country/circuit names (lowercase, hyphens)
- File extension: `.png`

**Team Logo Examples**:
```
mclaren.png
mercedes.png
red-bull.png          # Note: "red-bull" not "red-bull-racing"
williams.png
aston-martin.png
kick-sauber.png
ferrari.png
alpine.png
racing-bulls.png
haas.png
```

**Circuit Logo Examples** (used for certain circuits):
```
bahrain.png
australia.png
china.png
japan.png
```

**Important Notes**:
- Red Bull big logo uses `red-bull.png` (not `red-bull-racing.png`)
- Circuit logos are used when displaying certain Grand Prix locations
- See `circuitToTeamMap` in `src/utils/teamUtils.ts` for mappings

### Usage in Code
```typescript
import { 
  getTeamCarImageUrl, 
  getTeamLogoUrl, 
  getTeamBigLogoUrl 
} from '../utils/teamUtils';

const carImage = getTeamCarImageUrl('McLaren Formula 1 Team');
// Returns: teams/cars/mclaren.png

const logo = getTeamLogoUrl('Mercedes-AMG PETRONAS F1 Team');
// Returns: teams/logos/mercedes-logo.png

const bigLogo = getTeamBigLogoUrl('Oracle Red Bull Racing');
// Returns: teams/big-logos/red-bull.png
```

---

## Circuit Images

### Circuit Track Layouts

**Location**: `circuits/track-layouts/`

**Naming Convention**: `{CircuitName}_Circuit.png`

**Format**:
- Circuit name with underscores replacing spaces
- Suffix: `_Circuit`
- File extension: `.png`
- Capitalize first letter of each word

**Examples**:
```
Great_Britain_Circuit.png
Belgium_Circuit.png
Italy_Circuit.png
Monaco_Circuit.png
China_Circuit.png
Japan_Circuit.png
Singapore_Circuit.png
Australia_Circuit.png
USA_Circuit.png
Brazil_Circuit.png
Baku_Circuit.png
Spain_Circuit.png
Hungary_Circuit.png
Austria_Circuit.png
Netherlands_Circuit.png
Abu_Dhabi_Circuit.png
Saudi_Arabia_Circuit.png
Qatar_Circuit.png
Bahrain_Circuit.png
Las_Vegas_Circuit.png
Emilia_Romagna_Circuit.png
Canada_Circuit.png
Mexico_Circuit.png
```

**Circuit Name Mapping**:
The application maps circuit IDs to image names using `CIRCUIT_IMAGE_MAP` in `src/utils/circuitUtils.ts`:

| Circuit ID | Image Name | Filename |
|------------|------------|----------|
| silverstone | Great_Britain | `Great_Britain_Circuit.png` |
| spa | Belgium | `Belgium_Circuit.png` |
| monza | Italy | `Italy_Circuit.png` |
| monaco | Monaco | `Monaco_Circuit.png` |
| shanghai | China | `China_Circuit.png` |
| suzuka | Japan | `Japan_Circuit.png` |
| marina_bay | Singapore | `Singapore_Circuit.png` |
| albert_park | Australia | `Australia_Circuit.png` |
| americas | USA | `USA_Circuit.png` |
| interlagos | Brazil | `Brazil_Circuit.png` |
| baku | Baku | `Baku_Circuit.png` |
| catalunya | Spain | `Spain_Circuit.png` |
| hungaroring | Hungary | `Hungary_Circuit.png` |
| red_bull_ring | Austria | `Austria_Circuit.png` |
| zandvoort | Netherlands | `Netherlands_Circuit.png` |
| yas_marina | Abu_Dhabi | `Abu_Dhabi_Circuit.png` |
| jeddah | Saudi_Arabia | `Saudi_Arabia_Circuit.png` |
| losail | Qatar | `Qatar_Circuit.png` |
| bahrain | Bahrain | `Bahrain_Circuit.png` |
| las_vegas | Las_Vegas | `Las_Vegas_Circuit.png` |
| imola | Emilia_Romagna | `Emilia_Romagna_Circuit.png` |
| villeneuve | Canada | `Canada_Circuit.png` |
| rodriguez | Mexico | `Mexico_Circuit.png` |

### Circuit Carbon Backgrounds

**Location**: `circuits/carbon-backgrounds/`

**Naming Convention**: `{CircuitName} carbon.png`

**Format**:
- Circuit name with spaces (not underscores)
- Suffix: ` carbon`
- File extension: `.png`
- Capitalize first letter of each word

**Examples**:
```
Great Britain carbon.png
Belgium carbon.png
Italy carbon.png
Monaco carbon.png
China carbon.png
Japan carbon.png
Singapore carbon.png
Australia carbon.png
USA carbon.png
Brazil carbon.png
Azerbaijan carbon.png      # Note: Baku circuit uses "Azerbaijan"
Spain carbon.png
Hungary carbon.png
Austria carbon.png
Netherlands carbon.png
Abu Dhabi carbon.png
Saudi Arabia carbon.png
Qatar carbon.png
Bahrain carbon.png
Las Vegas carbon.png
Miami carbon.png           # Note: Miami has separate carbon background
Emilia Romagna carbon.png
Canada carbon.png
Mexico carbon.png
```

**Important Differences from Track Layouts**:
- Uses **spaces** instead of underscores
- Baku circuit uses `Azerbaijan` instead of `Baku`
- Miami has a separate carbon background (not included in track layouts)

**Circuit Name Mapping**:
Uses `CIRCUIT_CARBON_MAP` in `src/utils/circuitUtils.ts`:

| Circuit ID | Carbon Image Name | Filename |
|------------|-------------------|----------|
| baku | Azerbaijan | `Azerbaijan carbon.png` |
| miami | Miami | `Miami carbon.png` |
| (others) | Same as track layout but with spaces | `{Name} carbon.png` |

### Usage in Code
```typescript
import { 
  getCircuitTrackImageUrl, 
  getCircuitCarbonImageUrl 
} from '../utils/circuitUtils';

const trackImage = getCircuitTrackImageUrl('silverstone');
// Returns: circuits/track-layouts/Great_Britain_Circuit.png

const carbonImage = getCircuitCarbonImageUrl('baku');
// Returns: circuits/carbon-backgrounds/Azerbaijan carbon.png
```

---

## Placeholder Images

**Location**: `placeholders/`

**Naming Convention**: `{description}.png`

**Format**:
- Descriptive names using kebab-case (hyphens)
- Clear indication of purpose
- File extension: `.png`

**Examples**:
```
unknown-flag-small.png          # 24x18 unknown flag placeholder
unknown-flag-large.png          # 48x36 unknown flag placeholder
no-team-car.png                 # 800x200 team car placeholder
no-logo-small.png               # 200x100 logo placeholder
no-logo-large.png               # 1320x400 logo placeholder
logo-not-available.png          # 1320x400 logo not available
no-circuit-image.png            # 800x450 circuit image placeholder
no-circuit-background.png       # 1440x1080 circuit background placeholder
driver-headshot-placeholder.png # 80x80 driver headshot placeholder
```

**Naming Pattern**:
- `unknown-{type}-{size}.png` - For unknown/missing flags
- `no-{item}.png` - For missing items
- `{item}-not-available.png` - Alternative for unavailable items
- `{item}-placeholder.png` - Generic placeholder

---

## File Naming Patterns

### General Rules

1. **Case**: Use lowercase for most files, except:
   - Circuit images use Title Case with underscores/spaces
   - Country codes are always lowercase

2. **Separators**:
   - **Hyphens (`-`)**: Used in team names, placeholder descriptions
   - **Underscores (`_`)**: Used in circuit track layout names
   - **Spaces**: Used in circuit carbon background names

3. **Suffixes**:
   - `-logo` for team logos
   - `_Circuit` for circuit track layouts
   - ` carbon` for circuit carbon backgrounds
   - `-placeholder` for generic placeholders

4. **File Extensions**: Always `.png`

### Quick Reference Table

| Image Type | Location | Pattern | Example |
|------------|----------|---------|---------|
| Flag (small) | `flags/small/` | `{isoCode}.png` | `gb.png` |
| Flag (large) | `flags/large/` | `{isoCode}.png` | `us.png` |
| Team car | `teams/cars/` | `{team}.png` | `mclaren.png` |
| Team logo | `teams/logos/` | `{team}-logo.png` | `ferrari-logo.png` |
| Big logo | `teams/big-logos/` | `{name}.png` | `red-bull.png` |
| Track layout | `circuits/track-layouts/` | `{Name}_Circuit.png` | `Great_Britain_Circuit.png` |
| Carbon bg | `circuits/carbon-backgrounds/` | `{Name} carbon.png` | `Great Britain carbon.png` |
| Placeholder | `placeholders/` | `{description}.png` | `no-team-car.png` |

---

## Usage Examples

### Example 1: Getting a Flag Image

```typescript
// In your component
import { getFlagUrl } from '../utils/flagUtils';

// The function handles country code conversion
const flagUrl = getFlagUrl('GBR'); // Returns: flags/small/gb.png
const largeFlagUrl = getLargeFlagUrl('USA'); // Returns: flags/large/us.png

// In JSX
<img src={flagUrl} alt="Country flag" />
```

### Example 2: Getting Team Images

```typescript
import { 
  getTeamCarImageUrl, 
  getTeamLogoUrl, 
  getTeamBigLogoUrl 
} from '../utils/teamUtils';

// All functions normalize team names automatically
const car = getTeamCarImageUrl('McLaren Formula 1 Team');
// Returns: teams/cars/mclaren.png

const logo = getTeamLogoUrl('Mercedes-AMG PETRONAS F1 Team');
// Returns: teams/logos/mercedes-logo.png

const bigLogo = getTeamBigLogoUrl('Oracle Red Bull Racing');
// Returns: teams/big-logos/red-bull.png
```

### Example 3: Getting Circuit Images

```typescript
import { 
  getCircuitTrackImageUrl, 
  getCircuitCarbonImageUrl 
} from '../utils/circuitUtils';

const track = getCircuitTrackImageUrl('silverstone');
// Returns: circuits/track-layouts/Great_Britain_Circuit.png

const carbon = getCircuitCarbonImageUrl('baku');
// Returns: circuits/carbon-backgrounds/Azerbaijan carbon.png
```

### Example 4: Direct File Access

```typescript
// If you need direct file paths (not recommended, use utility functions)
const flagPath = '/images/flags/small/gb.png';
const teamCarPath = '/images/teams/cars/mclaren.png';
const circuitTrackPath = '/images/circuits/track-layouts/Great_Britain_Circuit.png';
```

---

## Best Practices

### ✅ DO

1. **Use Utility Functions**: Always use the utility functions from `src/utils/` instead of hardcoding paths
   ```typescript
   // ✅ Good
   const flag = getFlagUrl(countryCode);
   
   // ❌ Bad
   const flag = `/images/flags/small/${countryCode}.png`;
   ```

2. **Handle Errors**: Always provide fallback images for missing files
   ```typescript
   <img 
     src={getTeamCarImageUrl(teamName)} 
     alt={teamName}
     onError={(e) => {
       e.target.src = '/images/placeholders/no-team-car.png';
     }}
   />
   ```

3. **Use Descriptive Alt Text**: Always include meaningful alt text
   ```typescript
   <img src={flagUrl} alt={`${countryName} flag`} />
   ```

4. **Follow Naming Conventions**: When adding new images, follow the established patterns

### ❌ DON'T

1. **Don't Hardcode Paths**: Avoid hardcoding image paths
   ```typescript
   // ❌ Bad
   <img src="/images/flags/small/gb.png" />
   
   // ✅ Good
   <img src={getFlagUrl('GBR')} />
   ```

2. **Don't Mix Naming Styles**: Stick to the conventions for each image type
   ```typescript
   // ❌ Bad - mixing styles
   const filename = 'great_britain_circuit.png'; // Wrong case
   
   // ✅ Good
   const filename = 'Great_Britain_Circuit.png';
   ```

3. **Don't Skip Normalization**: Always normalize team/circuit names before using them
   ```typescript
   // ❌ Bad
   const team = 'McLaren Formula 1 Team';
   const path = `/images/teams/cars/${team}.png`; // Won't work!
   
   // ✅ Good
   const path = getTeamCarImageUrl('McLaren Formula 1 Team'); // Handles normalization
   ```

---

## Adding New Images

### Checklist for Adding New Images

1. ✅ Determine the correct directory based on image type
2. ✅ Follow the naming convention for that image type
3. ✅ Update the appropriate utility function if needed
4. ✅ Update the MANIFEST.md file in that directory
5. ✅ Update IMAGE_INDEX.md with the new image
6. ✅ Test that the image loads correctly
7. ✅ Add error handling for missing images

### Example: Adding a New Team

```typescript
// 1. Add team name mapping in src/utils/teamUtils.ts
export const teamNameMap: Record<string, string> = {
  // ... existing teams
  'New Team Name': 'new-team', // Add normalization
};

// 2. Download images:
// - teams/cars/new-team.png
// - teams/logos/new-team-logo.png
// - teams/big-logos/new-team.png

// 3. Update MANIFEST.md files
// 4. Test the images load correctly
```

---

## Troubleshooting

### Common Issues

1. **Image Not Found**
   - Check the file exists in the correct directory
   - Verify the naming convention matches exactly
   - Check for typos in team/circuit names
   - Ensure the utility function is normalizing correctly

2. **Wrong Image Displayed**
   - Verify the team/circuit name mapping is correct
   - Check if multiple teams map to the same normalized name
   - Review the normalization logic in utility functions

3. **Case Sensitivity Issues**
   - Remember: circuit images use Title Case
   - Team images use lowercase
   - Flag images use lowercase ISO codes

---

## Quick Reference

### File Path Patterns

```
flags/small/{isoCode}.png
flags/large/{isoCode}.png
teams/cars/{normalizedTeamName}.png
teams/logos/{normalizedTeamName}-logo.png
teams/big-logos/{name}.png
circuits/track-layouts/{CircuitName}_Circuit.png
circuits/carbon-backgrounds/{CircuitName} carbon.png
placeholders/{description}.png
```

### Utility Functions

```typescript
// Flags
getFlagUrl(countryCode: string): string
getLargeFlagUrl(countryCode: string): string

// Teams
getTeamCarImageUrl(teamName: string): string
getTeamLogoUrl(teamName: string): string
getTeamBigLogoUrl(teamName: string): string

// Circuits
getCircuitTrackImageUrl(circuitId: string, width?: number): string
getCircuitCarbonImageUrl(circuitId: string, width?: number): string
```

---

## Summary

- **Flags**: Use ISO country codes (lowercase) → `{isoCode}.png`
- **Teams**: Normalize to lowercase with hyphens → `{team}.png` or `{team}-logo.png`
- **Circuits**: Use Title Case with underscores (track) or spaces (carbon) → `{Name}_Circuit.png` or `{Name} carbon.png`
- **Placeholders**: Use descriptive kebab-case → `{description}.png`

**Always use utility functions** - they handle normalization, mapping, and error handling automatically!

