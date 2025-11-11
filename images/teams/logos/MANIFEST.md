# Team Logo Images (Small)

This directory should contain small logo images for all F1 teams.

## Image Files Required

- `mclaren-logo.png` - McLaren Formula 1 Team
- `mercedes-logo.png` - Mercedes-AMG PETRONAS F1 Team
- `red-bull-racing-logo.png` - Oracle Red Bull Racing
- `williams-logo.png` - Williams Racing
- `aston-martin-logo.png` - Aston Martin Aramco F1 Team
- `kick-sauber-logo.png` - Stake F1 Team Kick Sauber
- `ferrari-logo.png` - Scuderia Ferrari
- `alpine-logo.png` - BWT Alpine F1 Team
- `racing-bulls-logo.png` - Visa Cash App RB Formula One Team
- `haas-logo.png` - MoneyGram Haas F1 Team

## Source URLs

All images can be downloaded from:
```
https://media.formula1.com/content/dam/fom-website/teams/2025/{teamName}-logo.png
```

Replace `{teamName}` with the team name from the list above (without `-logo` suffix).

## Example Download Commands

```bash
# Download all team logos
teams=("mclaren" "mercedes" "red-bull-racing" "williams" "aston-martin" "kick-sauber" "ferrari" "alpine" "racing-bulls" "haas")
for team in "${teams[@]}"; do
  curl -o "${team}-logo.png" "https://media.formula1.com/content/dam/fom-website/teams/2025/${team}-logo.png"
done
```

