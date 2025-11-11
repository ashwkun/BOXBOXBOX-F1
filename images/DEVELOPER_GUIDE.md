# F1 Dashboard Image Repository - Developer Guide

Complete guide for developers working with images in the F1 Dashboard application.

## 📖 Documentation Index

| Document | Purpose | When to Use |
|----------|---------|-------------|
| **[QUICK_START.md](./QUICK_START.md)** | Quick reference | Getting started quickly |
| **[NAMING_CONVENTIONS.md](./NAMING_CONVENTIONS.md)** | Complete naming guide | Understanding file naming |
| **[IMAGE_CATALOG.md](./IMAGE_CATALOG.md)** | Full image catalog | Finding specific images |
| **[IMAGE_INDEX.md](./IMAGE_INDEX.md)** | Complete file list | Listing all images |
| **[IMAGE_URLS.md](./IMAGE_URLS.md)** | URL patterns | Understanding source URLs |
| **[STRUCTURE.md](./STRUCTURE.md)** | Directory structure | Understanding organization |
| **[DOWNLOAD_SUMMARY.md](./DOWNLOAD_SUMMARY.md)** | Download status | Checking what's downloaded |

## 🎯 Quick Decision Tree

**"How do I use images in my code?"**

```
Do you need a flag?
├─ Yes → Use getFlagUrl() or getLargeFlagUrl()
│         Example: getFlagUrl('GBR') → flags/small/gb.png
│
Do you need a team image?
├─ Car → Use getTeamCarImageUrl()
│        Example: getTeamCarImageUrl('McLaren') → teams/cars/mclaren.png
├─ Small logo → Use getTeamLogoUrl()
│               Example: getTeamLogoUrl('Ferrari') → teams/logos/ferrari-logo.png
└─ Big logo → Use getTeamBigLogoUrl()
              Example: getTeamBigLogoUrl('Red Bull') → teams/big-logos/red-bull.png

Do you need a circuit image?
├─ Track layout → Use getCircuitTrackImageUrl()
│                 Example: getCircuitTrackImageUrl('silverstone') 
│                 → circuits/track-layouts/Great_Britain_Circuit.png
└─ Carbon background → Use getCircuitCarbonImageUrl()
                        Example: getCircuitCarbonImageUrl('baku')
                        → circuits/carbon-backgrounds/Azerbaijan carbon.png
```

## 📁 Directory Structure Overview

```
images/
│
├── flags/
│   ├── small/          [27 files]  Country flags 24x18
│   └── large/          [27 files]  Country flags 48x36
│
├── teams/
│   ├── cars/           [10 files]  Team car images
│   ├── logos/           [10 files]  Small team logos
│   └── big-logos/       [14 files]  Large logos (10 teams + 4 circuits)
│
├── circuits/
│   ├── track-layouts/   [23 files]  Circuit track maps
│   └── carbon-backgrounds/ [24 files]  Circuit backgrounds
│
├── placeholders/        [9 files]   Fallback images
│
└── driver-headshots/    [Dynamic]   From F1 API
```

## 🔤 Naming Convention Cheat Sheet

### Flags
- **Pattern**: `{isoCode}.png`
- **Case**: lowercase
- **Example**: `gb.png`, `us.png`, `fr.png`
- **Source**: ISO 3166-1 alpha-2 codes

### Teams
- **Cars**: `{team}.png` → `mclaren.png`
- **Logos**: `{team}-logo.png` → `mclaren-logo.png`
- **Big Logos**: `{name}.png` → `red-bull.png` (note: not `red-bull-racing.png`)
- **Case**: lowercase with hyphens
- **Normalization**: "McLaren Formula 1 Team" → `mclaren`

### Circuits
- **Track Layouts**: `{Name}_Circuit.png` → `Great_Britain_Circuit.png`
  - Uses underscores
  - Title Case
  - Suffix: `_Circuit`
  
- **Carbon Backgrounds**: `{Name} carbon.png` → `Great Britain carbon.png`
  - Uses spaces
  - Title Case
  - Suffix: ` carbon`
  - Special: Baku → `Azerbaijan carbon.png`

### Placeholders
- **Pattern**: `{description}.png`
- **Case**: kebab-case
- **Examples**: `no-team-car.png`, `unknown-flag-small.png`

## 💡 Common Patterns

### Pattern 1: Displaying a Flag

```typescript
import { getFlagUrl } from '../utils/flagUtils';

function CountryFlag({ countryCode }: { countryCode: string }) {
  return (
    <img 
      src={getFlagUrl(countryCode)} 
      alt={`${countryCode} flag`}
      className="h-4"
      onError={(e) => {
        e.target.src = '/images/placeholders/unknown-flag-small.png';
      }}
    />
  );
}
```

### Pattern 2: Displaying Team Images

```typescript
import { getTeamCarImageUrl, getTeamBigLogoUrl } from '../utils/teamUtils';

function TeamCard({ teamName }: { teamName: string }) {
  return (
    <div>
      <img 
        src={getTeamBigLogoUrl(teamName)} 
        alt={`${teamName} logo`}
        className="h-8"
      />
      <img 
        src={getTeamCarImageUrl(teamName)} 
        alt={`${teamName} car`}
        className="h-12"
        onError={(e) => {
          e.target.src = '/images/placeholders/no-team-car.png';
        }}
      />
    </div>
  );
}
```

### Pattern 3: Displaying Circuit Images

```typescript
import { 
  getCircuitTrackImageUrl, 
  getCircuitCarbonImageUrl 
} from '../utils/circuitUtils';

function CircuitView({ circuitId }: { circuitId: string }) {
  return (
    <div>
      <img 
        src={getCircuitTrackImageUrl(circuitId)} 
        alt={`${circuitId} track layout`}
        className="w-full"
      />
      <div 
        className="bg-cover"
        style={{
          backgroundImage: `url(${getCircuitCarbonImageUrl(circuitId)})`
        }}
      />
    </div>
  );
}
```

## ⚠️ Common Mistakes to Avoid

### ❌ Mistake 1: Hardcoding Paths

```typescript
// ❌ BAD
<img src="/images/flags/small/gb.png" />

// ✅ GOOD
<img src={getFlagUrl('GBR')} />
```

**Why?** The utility function handles:
- Country code normalization (GBR → gb)
- Error handling
- Future path changes

### ❌ Mistake 2: Not Normalizing Team Names

```typescript
// ❌ BAD
const team = 'McLaren Formula 1 Team';
const path = `/images/teams/cars/${team}.png`; // Won't work!

// ✅ GOOD
const path = getTeamCarImageUrl('McLaren Formula 1 Team');
```

**Why?** Team names need normalization to match file names.

### ❌ Mistake 3: Wrong Case for Circuit Images

```typescript
// ❌ BAD
const track = 'great_britain_circuit.png'; // Wrong case

// ✅ GOOD
const track = getCircuitTrackImageUrl('silverstone');
// Returns: Great_Britain_Circuit.png (correct case)
```

### ❌ Mistake 4: Mixing Track Layout and Carbon Naming

```typescript
// ❌ BAD - Using underscore for carbon background
const carbon = 'Great_Britain carbon.png';

// ✅ GOOD - Carbon backgrounds use spaces
const carbon = getCircuitCarbonImageUrl('silverstone');
// Returns: Great Britain carbon.png
```

## 🔍 Finding Images

### "I need to find an image for..."

**A country flag:**
1. Convert F1 country code to ISO code (see flagUtils.ts)
2. File location: `flags/small/{isoCode}.png` or `flags/large/{isoCode}.png`
3. Use: `getFlagUrl(countryCode)`

**A team:**
1. Normalize team name (see teamUtils.ts)
2. File locations:
   - Car: `teams/cars/{team}.png`
   - Logo: `teams/logos/{team}-logo.png`
   - Big logo: `teams/big-logos/{name}.png`
3. Use: `getTeamCarImageUrl()`, `getTeamLogoUrl()`, or `getTeamBigLogoUrl()`

**A circuit:**
1. Get circuit ID (see circuitUtils.ts)
2. File locations:
   - Track: `circuits/track-layouts/{Name}_Circuit.png`
   - Carbon: `circuits/carbon-backgrounds/{Name} carbon.png`
3. Use: `getCircuitTrackImageUrl()` or `getCircuitCarbonImageUrl()`

## 📊 Image Statistics

- **Total Images**: 140 files
- **Flags**: 54 images (27 small + 27 large)
- **Teams**: 34 images (10 cars + 10 logos + 14 big logos)
- **Circuits**: 47 images (23 tracks + 24 carbon)
- **Placeholders**: 9 files
- **Total Size**: ~2.8 MB

## 🛠️ Utility Functions Reference

All utility functions are in `src/utils/`:

### flagUtils.ts
```typescript
getISOCountryCode(countryCode: string): string
getFlagUrl(countryCode: string): string
getLargeFlagUrl(countryCode: string): string
```

### teamUtils.ts
```typescript
getNormalizedTeamName(teamName: string): string
getTeamCarImageUrl(teamName: string): string
getTeamLogoUrl(teamName: string): string
getTeamBigLogoUrl(teamName: string): string
```

### circuitUtils.ts
```typescript
getCircuitId(circuitIdentifier: string): string
getCircuitTrackImageUrl(circuitId: string, width?: number): string
getCircuitCarbonImageUrl(circuitId: string, width?: number): string
```

## 📝 Adding New Images

### Step-by-Step Process

1. **Determine Image Type**
   - Flag? Team? Circuit? Placeholder?

2. **Follow Naming Convention**
   - Check NAMING_CONVENTIONS.md for the pattern
   - Ensure correct case and separators

3. **Add to Appropriate Directory**
   - Place file in correct subdirectory

4. **Update Documentation**
   - Update MANIFEST.md in that directory
   - Update IMAGE_INDEX.md
   - Update IMAGE_CATALOG.md if needed

5. **Update Utility Functions** (if needed)
   - Add mappings in appropriate utility file
   - Test normalization works correctly

6. **Test**
   - Verify image loads correctly
   - Test error handling
   - Check fallback behavior

## 🆘 Troubleshooting

### Image Not Loading?

1. Check file exists: `ls images/{directory}/{filename}`
2. Verify naming matches convention exactly
3. Check utility function normalization
4. Verify path in browser DevTools Network tab
5. Check for typos in team/circuit names

### Wrong Image Displayed?

1. Check team/circuit name mapping
2. Verify normalization logic
3. Check for duplicate mappings
4. Review utility function code

### Case Sensitivity Issues?

- Flags: lowercase ISO codes
- Teams: lowercase with hyphens
- Circuit tracks: Title_Case with underscores
- Circuit carbon: Title Case with spaces

## 📚 Additional Resources

- **Source Code**: `src/utils/flagUtils.ts`, `teamUtils.ts`, `circuitUtils.ts`
- **Components**: Check `src/components/` for usage examples
- **Manifests**: Each directory has a MANIFEST.md with file lists

## 🎓 Learning Path

1. **Start Here**: [QUICK_START.md](./QUICK_START.md)
2. **Understand Naming**: [NAMING_CONVENTIONS.md](./NAMING_CONVENTIONS.md)
3. **Find Images**: [IMAGE_INDEX.md](./IMAGE_INDEX.md)
4. **Deep Dive**: [IMAGE_CATALOG.md](./IMAGE_CATALOG.md)

---

**Remember**: Always use utility functions - they handle all the complexity for you! 🚀

