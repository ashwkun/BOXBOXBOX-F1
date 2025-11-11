# Image URLs Quick Reference

Quick reference for all image URLs used in the F1 Dashboard application.

## Flag Images

### Small Flags (24x18)
```
https://flagcdn.com/24x18/{isoCode}.png
```

### Large Flags (48x36)
```
https://flagcdn.com/48x36/{isoCode}.png
```

**Common ISO Codes Used:**
- `gb`, `us`, `de`, `fr`, `it`, `es`, `au`, `jp`, `cn`, `br`, `mx`, `ca`, `nl`, `mc`, `ch`, `ae`, `sa`, `qa`, `bh`, `th`, `tw`, `ru`, `fi`, `ar`, `nz`, `za`, `dk`, `xx`

---

## Team Images

### Team Car Images
```
https://media.formula1.com/d_team_car_fallback_image.png/content/dam/fom-website/teams/2025/{normalizedName}.png
```

**Team Names:**
- `mclaren`, `mercedes`, `red-bull-racing`, `williams`, `aston-martin`, `kick-sauber`, `ferrari`, `alpine`, `racing-bulls`, `haas`

### Team Logos (Small)
```
https://media.formula1.com/content/dam/fom-website/teams/2025/{normalizedName}-logo.png
```

### Team Big Logos
```
McLaren: https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/mclaren
Mercedes: https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/mercedes
Red Bull: https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/red%20bull
Williams: https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/williams
Aston Martin: https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/aston%20martin
Kick Sauber: https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/kick%20sauber
Ferrari: https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/ferrari
Alpine: https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/alpine
Racing Bulls: https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/fom-website/2018-redesign-assets/team%20logos/racing%20bulls
Haas: https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/haas
```

### Circuit-to-Team Logo Mappings
```
Bahrain: https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/bahrain
Australia: https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/australia
China: https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/china
Japan: https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/japan
```

---

## Circuit Images

### Circuit Track Layouts
```
https://media.formula1.com/image/upload/f_auto,c_limit,q_auto{widthParam}/content/dam/fom-website/2018-redesign-assets/Circuit%20maps%2016x9/{circuitImageName}_Circuit
```

**Circuit Names:**
- `Great_Britain`, `Belgium`, `Italy`, `Monaco`, `China`, `Japan`, `Singapore`, `Australia`, `USA`, `Brazil`, `Baku`, `Spain`, `Hungary`, `Austria`, `Netherlands`, `Abu_Dhabi`, `Saudi_Arabia`, `Qatar`, `Bahrain`, `Las_Vegas`, `Emilia_Romagna`, `Canada`, `Mexico`

### Circuit Carbon Backgrounds
```
https://media.formula1.com/image/upload/f_auto,c_limit,w_{width},q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/{circuitCarbonName}%20carbon
```

**Circuit Names (with spaces):**
- `Great Britain`, `Belgium`, `Italy`, `Monaco`, `China`, `Japan`, `Singapore`, `Australia`, `USA`, `Brazil`, `Azerbaijan`, `Spain`, `Hungary`, `Austria`, `Netherlands`, `Abu Dhabi`, `Saudi Arabia`, `Qatar`, `Bahrain`, `Las Vegas`, `Miami`, `Emilia Romagna`, `Canada`, `Mexico`

---

## Placeholder Images

```
Unknown Flag (Small): https://via.placeholder.com/24x18?text=?
Unknown Flag (Large): https://via.placeholder.com/48x36?text=?
No Team Car: https://via.placeholder.com/800x200?text=No+Team+Car+Available
No Logo (Small): https://via.placeholder.com/200x100?text=No+Logo+Available
No Logo (Large): https://via.placeholder.com/1320x400?text=No+Logo+Available
Logo Not Available: https://via.placeholder.com/1320x400?text=Logo+Not+Available
No Circuit Image: https://via.placeholder.com/800x450?text=No+Circuit+Image+Available
No Circuit Background: https://via.placeholder.com/1440x1080?text=No+Circuit+Background+Available
Driver Headshot: https://via.placeholder.com/80?text={driver.name_acronym}
```

---

## Driver Images

Driver headshots come from the F1 API via `driver.headshot_url` field. These URLs are dynamic and provided by the API service.

---

## Image Count Summary

- **Flag Images**: ~30+ countries (2 sizes each = 60+ images)
- **Team Car Images**: 10 teams
- **Team Logos**: 10 teams (small) + 10 teams (big) = 20 images
- **Circuit Track Layouts**: 24 circuits
- **Circuit Carbon Backgrounds**: 24 circuits
- **Placeholder Images**: 9 different placeholders
- **Driver Headshots**: Dynamic (from API)

**Total External Image URLs**: ~150+ unique image patterns

