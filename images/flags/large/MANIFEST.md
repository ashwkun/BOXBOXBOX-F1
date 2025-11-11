# Large Flag Images (48x36)

This directory should contain large flag images for all countries used in the F1 Dashboard.

## Image Files Required

Based on the country codes used in the application, the following flag images should be present:

- `gb.png` - Great Britain
- `us.png` - United States
- `de.png` - Germany
- `fr.png` - France
- `it.png` - Italy
- `es.png` - Spain
- `au.png` - Australia
- `jp.png` - Japan
- `cn.png` - China
- `br.png` - Brazil
- `mx.png` - Mexico
- `ca.png` - Canada
- `nl.png` - Netherlands
- `mc.png` - Monaco
- `ch.png` - Switzerland
- `ae.png` - United Arab Emirates
- `sa.png` - Saudi Arabia
- `qa.png` - Qatar
- `bh.png` - Bahrain
- `th.png` - Thailand
- `tw.png` - Chinese Taipei
- `ru.png` - Russia
- `fi.png` - Finland
- `ar.png` - Argentina
- `nz.png` - New Zealand
- `za.png` - South Africa
- `dk.png` - Denmark
- `xx.png` - Unknown/Placeholder

## Source URLs

All images can be downloaded from:
```
https://flagcdn.com/48x36/{isoCode}.png
```

Replace `{isoCode}` with the ISO code from the list above.

## Example Download Commands

```bash
# Download all flags
for code in gb us de fr it es au jp cn br mx ca nl mc ch ae sa qa bh th tw ru fi ar nz za dk xx; do
  curl -o "${code}.png" "https://flagcdn.com/48x36/${code}.png"
done
```

