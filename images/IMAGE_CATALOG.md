# F1 Dashboard Image Catalog

Complete catalog of all images used in the F1 Dashboard application, organized by category.

## Table of Contents

1. [Flag Images](#flag-images)
2. [Team Images](#team-images)
3. [Circuit Images](#circuit-images)
4. [Placeholder Images](#placeholder-images)
5. [Driver Images](#driver-images)

---

## Flag Images

### Source
- **Provider**: flagcdn.com
- **Base URL**: `https://flagcdn.com/`

### Small Flags (24x18)
- **URL Pattern**: `https://flagcdn.com/24x18/{isoCode}.png`
- **Usage**: Used in driver lists and small UI elements
- **Function**: `getFlagUrl()` in `src/utils/flagUtils.ts`
- **Example Countries**:
  - `gb` - Great Britain
  - `us` - United States
  - `de` - Germany
  - `fr` - France
  - `it` - Italy
  - `es` - Spain
  - `au` - Australia
  - `jp` - Japan
  - `cn` - China
  - `br` - Brazil
  - `mx` - Mexico
  - `ca` - Canada
  - `nl` - Netherlands
  - `mc` - Monaco
  - `ch` - Switzerland
  - `ae` - United Arab Emirates
  - `sa` - Saudi Arabia
  - `qa` - Qatar
  - `bh` - Bahrain
  - `th` - Thailand
  - `tw` - Chinese Taipei
  - `ru` - Russia
  - `fi` - Finland
  - `ar` - Argentina
  - `nz` - New Zealand
  - `za` - South Africa
  - `dk` - Denmark
  - `xx` - Unknown/Placeholder

### Large Flags (48x36)
- **URL Pattern**: `https://flagcdn.com/48x36/{isoCode}.png`
- **Usage**: Used in session headers and larger UI elements
- **Function**: `getLargeFlagUrl()` in `src/utils/flagUtils.ts`
- **Same countries as small flags**

---

## Team Images

### Source
- **Provider**: media.formula1.com
- **Base URL**: `https://media.formula1.com/`

### Team Car Images
- **URL Pattern**: `https://media.formula1.com/d_team_car_fallback_image.png/content/dam/fom-website/teams/2025/{normalizedName}.png`
- **Usage**: Displayed in driver cards and team sections
- **Function**: `getTeamCarImageUrl()` in `src/utils/teamUtils.ts`
- **Teams**:
  - `mclaren` - McLaren Formula 1 Team
  - `mercedes` - Mercedes-AMG PETRONAS F1 Team
  - `red-bull-racing` - Oracle Red Bull Racing
  - `williams` - Williams Racing
  - `aston-martin` - Aston Martin Aramco F1 Team
  - `kick-sauber` - Stake F1 Team Kick Sauber (formerly Alfa Romeo)
  - `ferrari` - Scuderia Ferrari
  - `alpine` - BWT Alpine F1 Team
  - `racing-bulls` - Visa Cash App RB Formula One Team (formerly AlphaTauri)
  - `haas` - MoneyGram Haas F1 Team

### Team Logos (Small)
- **URL Pattern**: `https://media.formula1.com/content/dam/fom-website/teams/2025/{normalizedName}-logo.png`
- **Usage**: Fallback for team big logos
- **Function**: `getTeamLogoUrl()` in `src/utils/teamUtils.ts`
- **Same teams as car images**

### Team Big Logos
- **URL Pattern**: Various (see specific URLs below)
- **Usage**: Displayed in driver cards and team sections
- **Function**: `getTeamBigLogoUrl()` in `src/utils/teamUtils.ts`
- **Specific URLs**:
  - **McLaren**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/mclaren`
  - **Mercedes**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/mercedes`
  - **Red Bull Racing**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/red%20bull`
  - **Williams**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/williams`
  - **Aston Martin**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/aston%20martin`
  - **Kick Sauber**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/kick%20sauber`
  - **Ferrari**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/ferrari`
  - **Alpine**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/alpine`
  - **Racing Bulls**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/fom-website/2018-redesign-assets/team%20logos/racing%20bulls`
  - **Haas**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/haas`

### Circuit-to-Team Logo Mapping
Some circuits use country/race logos instead of team logos:
- **Sakhir (Bahrain)**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/bahrain`
- **Melbourne (Australia)**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/australia`
- **Shanghai (China)**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/china`
- **Suzuka (Japan)**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/japan`

---

## Circuit Images

### Source
- **Provider**: media.formula1.com
- **Base URL**: `https://media.formula1.com/image/upload/`

### Circuit Track Layout Images
- **URL Pattern**: `https://media.formula1.com/image/upload/f_auto,c_limit,q_auto{widthParam}/content/dam/fom-website/2018-redesign-assets/Circuit%20maps%2016x9/{circuitImageName}_Circuit`
- **Usage**: Display circuit track layouts
- **Function**: `getCircuitTrackImageUrl()` in `src/utils/circuitUtils.ts`
- **Width Parameter**: Optional `,w_{width}` (e.g., `,w_800`)
- **Circuits**:
  - `Great_Britain` - Silverstone
  - `Belgium` - Spa-Francorchamps
  - `Italy` - Monza
  - `Monaco` - Monaco
  - `China` - Shanghai
  - `Japan` - Suzuka
  - `Singapore` - Marina Bay
  - `Australia` - Albert Park
  - `USA` - Circuit of the Americas / Miami
  - `Brazil` - Interlagos
  - `Baku` - Baku City Circuit
  - `Spain` - Catalunya
  - `Hungary` - Hungaroring
  - `Austria` - Red Bull Ring
  - `Netherlands` - Zandvoort
  - `Abu_Dhabi` - Yas Marina
  - `Saudi_Arabia` - Jeddah
  - `Qatar` - Losail
  - `Bahrain` - Bahrain
  - `Las_Vegas` - Las Vegas
  - `Emilia_Romagna` - Imola
  - `Canada` - Gilles Villeneuve
  - `Mexico` - Rodriguez

### Circuit Carbon Background Images
- **URL Pattern**: `https://media.formula1.com/image/upload/f_auto,c_limit,w_{width},q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/{circuitCarbonName}%20carbon`
- **Usage**: Background images for circuit views
- **Function**: `getCircuitCarbonImageUrl()` in `src/utils/circuitUtils.ts`
- **Default Width**: 1440px
- **Circuits** (with spaces in names):
  - `Great Britain` - Silverstone
  - `Belgium` - Spa-Francorchamps
  - `Italy` - Monza
  - `Monaco` - Monaco
  - `China` - Shanghai
  - `Japan` - Suzuka
  - `Singapore` - Marina Bay
  - `Australia` - Albert Park
  - `USA` - Circuit of the Americas
  - `Brazil` - Interlagos
  - `Azerbaijan` - Baku
  - `Spain` - Catalunya
  - `Hungary` - Hungaroring
  - `Austria` - Red Bull Ring
  - `Netherlands` - Zandvoort
  - `Abu Dhabi` - Yas Marina
  - `Saudi Arabia` - Jeddah
  - `Qatar` - Losail
  - `Bahrain` - Bahrain
  - `Las Vegas` - Las Vegas
  - `Miami` - Miami
  - `Emilia Romagna` - Imola
  - `Canada` - Gilles Villeneuve
  - `Mexico` - Rodriguez

---

## Placeholder Images

### Source
- **Provider**: via.placeholder.com
- **Base URL**: `https://via.placeholder.com/`

### Placeholder URLs Used

1. **Unknown Flag (Small)**
   - URL: `https://via.placeholder.com/24x18?text=?`
   - Usage: Fallback for unknown country codes
   - Location: `src/utils/flagUtils.ts` - `getFlagUrl()`

2. **Unknown Flag (Large)**
   - URL: `https://via.placeholder.com/48x36?text=?`
   - Usage: Fallback for unknown country codes
   - Location: `src/utils/flagUtils.ts` - `getLargeFlagUrl()`

3. **No Team Car Available**
   - URL: `https://via.placeholder.com/800x200?text=No+Team+Car+Available`
   - Usage: Fallback when team car image fails to load
   - Location: `src/utils/teamUtils.ts` - `getTeamCarImageUrl()`

4. **No Logo Available**
   - URL: `https://via.placeholder.com/200x100?text=No+Logo+Available`
   - Usage: Fallback when team logo fails to load
   - Location: `src/utils/teamUtils.ts` - `getTeamLogoUrl()`

5. **No Logo Available (Large)**
   - URL: `https://via.placeholder.com/1320x400?text=No+Logo+Available`
   - Usage: Fallback when team big logo fails to load
   - Location: `src/utils/teamUtils.ts` - `getTeamBigLogoUrl()`

6. **Logo Not Available**
   - URL: `https://via.placeholder.com/1320x400?text=Logo+Not+Available`
   - Usage: Fallback when team big logo is not found
   - Location: `src/utils/teamUtils.ts` - `getTeamBigLogoUrl()`

7. **No Circuit Image Available**
   - URL: `https://via.placeholder.com/800x450?text=No+Circuit+Image+Available`
   - Usage: Fallback when circuit track image fails to load
   - Location: `src/utils/circuitUtils.ts` - `getCircuitTrackImageUrl()`

8. **No Circuit Background Available**
   - URL: `https://via.placeholder.com/1440x1080?text=No+Circuit+Background+Available`
   - Usage: Fallback when circuit carbon background fails to load
   - Location: `src/utils/circuitUtils.ts` - `getCircuitCarbonImageUrl()`

9. **Driver Headshot Placeholder**
   - URL Pattern: `https://via.placeholder.com/80?text={driver.name_acronym}`
   - Usage: Fallback when driver headshot fails to load
   - Location: `src/components/DriversList.tsx` - `onError` handler

---

## Driver Images

### Driver Headshots
- **Source**: F1 API (`driver.headshot_url`)
- **Usage**: Displayed in driver cards
- **Location**: `src/components/DriversList.tsx`
- **Fallback**: Placeholder with driver acronym (see above)
- **Note**: These URLs come directly from the F1 API and are not hardcoded

---

## Image Usage Summary

### By Component

1. **DriversList.tsx**
   - Driver headshots (from API)
   - Small flags (`getFlagUrl()`)
   - Team big logos (`getTeamBigLogoUrl()`)
   - Team logos (`getTeamLogoUrl()` - fallback)
   - Team car images (`getTeamCarImageUrl()`)
   - Placeholder images for fallbacks

2. **LiveSessionTable.tsx**
   - Team car images (`getTeamCarImageUrl()`)

3. **LiveSession.tsx**
   - Large flags (`getLargeFlagUrl()`)

4. **ApiLogger.tsx**
   - Circuit carbon backgrounds (`getCircuitCarbonImageUrl()`)

### By Utility File

1. **flagUtils.ts**
   - Small flags (24x18)
   - Large flags (48x36)
   - Placeholder flags

2. **teamUtils.ts**
   - Team car images
   - Team logos
   - Team big logos
   - Circuit-to-team logo mappings
   - Placeholder images

3. **circuitUtils.ts**
   - Circuit track layout images
   - Circuit carbon background images
   - Placeholder images

---

## Notes

- All images are currently loaded from external URLs
- Images use Cloudinary transformations (f_auto, c_limit, q_auto, w_{width})
- Placeholder images are used as fallbacks when images fail to load
- Driver headshots come from the F1 API and are not stored locally
- Team and circuit images are from Formula 1's official media CDN
- Flag images are from flagcdn.com, a free flag icon service

---

## Future Considerations

If migrating to local assets:
1. Download all flag images for countries used in F1
2. Download team car images for all 10 teams
3. Download team logos (small and big) for all teams
4. Download circuit track layouts for all circuits
5. Download circuit carbon backgrounds for all circuits
6. Create placeholder images matching the current ones
7. Update utility functions to use local paths instead of URLs

