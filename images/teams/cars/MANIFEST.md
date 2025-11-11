# Team Car Images

This directory should contain car images for all F1 teams.

## Image Files Required

- `mclaren.png` - McLaren Formula 1 Team
- `mercedes.png` - Mercedes-AMG PETRONAS F1 Team
- `red-bull-racing.png` - Oracle Red Bull Racing
- `williams.png` - Williams Racing
- `aston-martin.png` - Aston Martin Aramco F1 Team
- `kick-sauber.png` - Stake F1 Team Kick Sauber
- `ferrari.png` - Scuderia Ferrari
- `alpine.png` - BWT Alpine F1 Team
- `racing-bulls.png` - Visa Cash App RB Formula One Team
- `haas.png` - MoneyGram Haas F1 Team

## Source URLs

All images can be downloaded from:
```
https://media.formula1.com/d_team_car_fallback_image.png/content/dam/fom-website/teams/2025/{teamName}.png
```

Replace `{teamName}` with the team name from the list above.

## Example Download Commands

```bash
# Download all team car images
teams=("mclaren" "mercedes" "red-bull-racing" "williams" "aston-martin" "kick-sauber" "ferrari" "alpine" "racing-bulls" "haas")
for team in "${teams[@]}"; do
  curl -o "${team}.png" "https://media.formula1.com/d_team_car_fallback_image.png/content/dam/fom-website/teams/2025/${team}.png"
done
```

