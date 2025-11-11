# Quick Start Guide for Developers

This guide helps developers quickly understand and use the image repository.

## 🚀 Quick Reference

### Image Locations

```
images/
├── flags/small/          → Country flags (24x18)
├── flags/large/          → Country flags (48x36)
├── teams/cars/           → F1 team car images
├── teams/logos/           → Small team logos
├── teams/big-logos/       → Large team logos
├── circuits/track-layouts/ → Circuit track maps
├── circuits/carbon-backgrounds/ → Circuit backgrounds
└── placeholders/          → Fallback images
```

## 📝 Naming Conventions Summary

| Image Type | Pattern | Example |
|------------|---------|---------|
| **Flag** | `{isoCode}.png` | `gb.png`, `us.png` |
| **Team Car** | `{team}.png` | `mclaren.png`, `ferrari.png` |
| **Team Logo** | `{team}-logo.png` | `mclaren-logo.png` |
| **Big Logo** | `{name}.png` | `red-bull.png` |
| **Track Layout** | `{Name}_Circuit.png` | `Great_Britain_Circuit.png` |
| **Carbon BG** | `{Name} carbon.png` | `Great Britain carbon.png` |
| **Placeholder** | `{desc}.png` | `no-team-car.png` |

## 💻 Code Examples

### Using Images in Components

```typescript
import { getFlagUrl } from '../utils/flagUtils';
import { getTeamCarImageUrl } from '../utils/teamUtils';
import { getCircuitTrackImageUrl } from '../utils/circuitUtils';

// Flag image
const flag = getFlagUrl('GBR'); // Returns: flags/small/gb.png

// Team car image
const car = getTeamCarImageUrl('McLaren Formula 1 Team'); 
// Returns: teams/cars/mclaren.png

// Circuit track image
const track = getCircuitTrackImageUrl('silverstone');
// Returns: circuits/track-layouts/Great_Britain_Circuit.png
```

### In JSX

```tsx
<img 
  src={getFlagUrl(driver.country_code)} 
  alt={`${driver.country_code} flag`}
  onError={(e) => {
    e.target.src = '/images/placeholders/unknown-flag-small.png';
  }}
/>
```

## 🔑 Key Points

1. **Always use utility functions** - Don't hardcode paths
2. **Team names are normalized** - "McLaren Formula 1 Team" → `mclaren.png`
3. **Circuit names vary** - Track layouts use underscores, carbon uses spaces
4. **Country codes are ISO** - F1 codes (GBR) map to ISO codes (gb)
5. **Handle errors** - Always provide fallback images

## 📚 Full Documentation

- **[NAMING_CONVENTIONS.md](./NAMING_CONVENTIONS.md)** - Complete naming conventions guide
- **[IMAGE_CATALOG.md](./IMAGE_CATALOG.md)** - Full catalog of all images
- **[IMAGE_INDEX.md](./IMAGE_INDEX.md)** - Complete list of all image files
- **[README.md](./README.md)** - Repository overview

## 🆘 Need Help?

1. Check the utility functions in `src/utils/` - they handle all normalization
2. Review the MANIFEST.md files in each directory
3. See NAMING_CONVENTIONS.md for detailed examples
4. Check existing components for usage patterns

