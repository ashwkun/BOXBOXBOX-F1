#!/bin/bash

# F1 Dashboard Image Download Script
# This script downloads all images used in the F1 Dashboard application

# Don't exit on error - continue downloading other images

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Base directory
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Function to download with error handling
download_image() {
    local url=$1
    local output=$2
    local description=$3
    
    if [ -f "$output" ]; then
        echo -e "${YELLOW}⏭  Skipping $description (already exists)${NC}"
        return 0
    fi
    
    echo -e "${GREEN}⬇  Downloading $description...${NC}"
    if curl -s -f -L -o "$output" "$url"; then
        echo -e "${GREEN}✓  Downloaded $description${NC}"
        return 0
    else
        echo -e "${RED}✗  Failed to download $description${NC}"
        rm -f "$output"  # Remove partial file
        return 1
    fi
}

# Create directories
echo -e "${GREEN}📁 Creating directories...${NC}"
mkdir -p "$BASE_DIR/flags/small"
mkdir -p "$BASE_DIR/flags/large"
mkdir -p "$BASE_DIR/teams/cars"
mkdir -p "$BASE_DIR/teams/logos"
mkdir -p "$BASE_DIR/teams/big-logos"
mkdir -p "$BASE_DIR/circuits/track-layouts"
mkdir -p "$BASE_DIR/circuits/carbon-backgrounds"
mkdir -p "$BASE_DIR/placeholders"

# Download small flags (24x18)
echo -e "\n${GREEN}🏴 Downloading small flags (24x18)...${NC}"
cd "$BASE_DIR/flags/small"
for code in gb us de fr it es au jp cn br mx ca nl mc ch ae sa qa bh th tw ru fi ar nz za dk; do
    download_image "https://flagcdn.com/24x18/${code}.png" "${code}.png" "Flag ${code} (small)"
done

# Download large flags (48x36)
echo -e "\n${GREEN}🏴 Downloading large flags (48x36)...${NC}"
cd "$BASE_DIR/flags/large"
for code in gb us de fr it es au jp cn br mx ca nl mc ch ae sa qa bh th tw ru fi ar nz za dk; do
    download_image "https://flagcdn.com/48x36/${code}.png" "${code}.png" "Flag ${code} (large)"
done

# Download team car images
echo -e "\n${GREEN}🏎️  Downloading team car images...${NC}"
cd "$BASE_DIR/teams/cars"
teams=("mclaren" "mercedes" "red-bull-racing" "williams" "aston-martin" "kick-sauber" "ferrari" "alpine" "racing-bulls" "haas")
for team in "${teams[@]}"; do
    download_image "https://media.formula1.com/d_team_car_fallback_image.png/content/dam/fom-website/teams/2025/${team}.png" "${team}.png" "Team car ${team}"
done

# Download small team logos
echo -e "\n${GREEN}🏁 Downloading small team logos...${NC}"
cd "$BASE_DIR/teams/logos"
teams=("mclaren" "mercedes" "red-bull-racing" "williams" "aston-martin" "kick-sauber" "ferrari" "alpine" "racing-bulls" "haas")
for team in "${teams[@]}"; do
    download_image "https://media.formula1.com/content/dam/fom-website/teams/2025/${team}-logo.png" "${team}-logo.png" "Team logo ${team}"
done

# Download big team logos
echo -e "\n${GREEN}🏁 Downloading big team logos...${NC}"
cd "$BASE_DIR/teams/big-logos"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/mclaren" "mclaren.png" "Big logo McLaren"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/mercedes" "mercedes.png" "Big logo Mercedes"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/red%20bull" "red-bull.png" "Big logo Red Bull"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/williams" "williams.png" "Big logo Williams"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/aston%20martin" "aston-martin.png" "Big logo Aston Martin"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/kick%20sauber" "kick-sauber.png" "Big logo Kick Sauber"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/ferrari" "ferrari.png" "Big logo Ferrari"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/alpine" "alpine.png" "Big logo Alpine"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/fom-website/2018-redesign-assets/team%20logos/racing%20bulls" "racing-bulls.png" "Big logo Racing Bulls"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/haas" "haas.png" "Big logo Haas"

# Download circuit logos
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_75,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/bahrain" "bahrain.png" "Circuit logo Bahrain"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/australia" "australia.png" "Circuit logo Australia"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/china" "china.png" "Circuit logo China"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_auto,w_1320/content/dam/fom-website/2018-redesign-assets/team%20logos/japan" "japan.png" "Circuit logo Japan"

# Download circuit track layouts
echo -e "\n${GREEN}🏁 Downloading circuit track layouts...${NC}"
cd "$BASE_DIR/circuits/track-layouts"
circuits=("Great_Britain" "Belgium" "Italy" "Monaco" "China" "Japan" "Singapore" "Australia" "USA" "Brazil" "Baku" "Spain" "Hungary" "Austria" "Netherlands" "Abu_Dhabi" "Saudi_Arabia" "Qatar" "Bahrain" "Las_Vegas" "Emilia_Romagna" "Canada" "Mexico")
for circuit in "${circuits[@]}"; do
    download_image "https://media.formula1.com/image/upload/f_auto,c_limit,q_auto/content/dam/fom-website/2018-redesign-assets/Circuit%20maps%2016x9/${circuit}_Circuit" "${circuit}_Circuit.png" "Circuit track ${circuit}"
done

# Download circuit carbon backgrounds
echo -e "\n${GREEN}🏁 Downloading circuit carbon backgrounds...${NC}"
cd "$BASE_DIR/circuits/carbon-backgrounds"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Great%20Britain%20carbon" "Great Britain carbon.png" "Carbon background Great Britain"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Belgium%20carbon" "Belgium carbon.png" "Carbon background Belgium"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Italy%20carbon" "Italy carbon.png" "Carbon background Italy"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Monaco%20carbon" "Monaco carbon.png" "Carbon background Monaco"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/China%20carbon" "China carbon.png" "Carbon background China"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Japan%20carbon" "Japan carbon.png" "Carbon background Japan"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Singapore%20carbon" "Singapore carbon.png" "Carbon background Singapore"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Australia%20carbon" "Australia carbon.png" "Carbon background Australia"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/USA%20carbon" "USA carbon.png" "Carbon background USA"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Brazil%20carbon" "Brazil carbon.png" "Carbon background Brazil"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Azerbaijan%20carbon" "Azerbaijan carbon.png" "Carbon background Azerbaijan"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Spain%20carbon" "Spain carbon.png" "Carbon background Spain"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Hungary%20carbon" "Hungary carbon.png" "Carbon background Hungary"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Austria%20carbon" "Austria carbon.png" "Carbon background Austria"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Netherlands%20carbon" "Netherlands carbon.png" "Carbon background Netherlands"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Abu%20Dhabi%20carbon" "Abu Dhabi carbon.png" "Carbon background Abu Dhabi"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Saudi%20Arabia%20carbon" "Saudi Arabia carbon.png" "Carbon background Saudi Arabia"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Qatar%20carbon" "Qatar carbon.png" "Carbon background Qatar"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Bahrain%20carbon" "Bahrain carbon.png" "Carbon background Bahrain"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Las%20Vegas%20carbon" "Las Vegas carbon.png" "Carbon background Las Vegas"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Miami%20carbon" "Miami carbon.png" "Carbon background Miami"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Emilia%20Romagna%20carbon" "Emilia Romagna carbon.png" "Carbon background Emilia Romagna"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Canada%20carbon" "Canada carbon.png" "Carbon background Canada"
download_image "https://media.formula1.com/image/upload/f_auto,c_limit,w_1440,q_auto/f_auto/q_auto/content/dam/fom-website/2018-redesign-assets/Track%20icons%204x3/Mexico%20carbon" "Mexico carbon.png" "Carbon background Mexico"

# Download placeholder images
echo -e "\n${GREEN}📦 Downloading placeholder images...${NC}"
cd "$BASE_DIR/placeholders"
download_image "https://via.placeholder.com/24x18?text=?" "unknown-flag-small.png" "Placeholder unknown flag small"
download_image "https://via.placeholder.com/48x36?text=?" "unknown-flag-large.png" "Placeholder unknown flag large"
download_image "https://via.placeholder.com/800x200?text=No+Team+Car+Available" "no-team-car.png" "Placeholder no team car"
download_image "https://via.placeholder.com/200x100?text=No+Logo+Available" "no-logo-small.png" "Placeholder no logo small"
download_image "https://via.placeholder.com/1320x400?text=No+Logo+Available" "no-logo-large.png" "Placeholder no logo large"
download_image "https://via.placeholder.com/1320x400?text=Logo+Not+Available" "logo-not-available.png" "Placeholder logo not available"
download_image "https://via.placeholder.com/800x450?text=No+Circuit+Image+Available" "no-circuit-image.png" "Placeholder no circuit image"
download_image "https://via.placeholder.com/1440x1080?text=No+Circuit+Background+Available" "no-circuit-background.png" "Placeholder no circuit background"
download_image "https://via.placeholder.com/80?text=?" "driver-headshot-placeholder.png" "Placeholder driver headshot"

echo -e "\n${GREEN}✅ Download complete!${NC}"
echo -e "${GREEN}📊 Summary:${NC}"
echo "  - Flags (small): $(ls -1 "$BASE_DIR/flags/small"/*.png 2>/dev/null | wc -l | tr -d ' ') files"
echo "  - Flags (large): $(ls -1 "$BASE_DIR/flags/large"/*.png 2>/dev/null | wc -l | tr -d ' ') files"
echo "  - Team cars: $(ls -1 "$BASE_DIR/teams/cars"/*.png 2>/dev/null | wc -l | tr -d ' ') files"
echo "  - Team logos: $(ls -1 "$BASE_DIR/teams/logos"/*.png 2>/dev/null | wc -l | tr -d ' ') files"
echo "  - Big logos: $(ls -1 "$BASE_DIR/teams/big-logos"/*.png 2>/dev/null | wc -l | tr -d ' ') files"
echo "  - Circuit tracks: $(ls -1 "$BASE_DIR/circuits/track-layouts"/*.png 2>/dev/null | wc -l | tr -d ' ') files"
echo "  - Carbon backgrounds: $(ls -1 "$BASE_DIR/circuits/carbon-backgrounds"/*.png 2>/dev/null | wc -l | tr -d ' ') files"
echo "  - Placeholders: $(ls -1 "$BASE_DIR/placeholders"/*.png 2>/dev/null | wc -l | tr -d ' ') files"

