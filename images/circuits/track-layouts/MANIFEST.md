# Circuit Track Layout Images

This directory should contain track layout images for all F1 circuits.

## Image Files Required

- `Great_Britain_Circuit.png` - Silverstone
- `Belgium_Circuit.png` - Spa-Francorchamps
- `Italy_Circuit.png` - Monza
- `Monaco_Circuit.png` - Monaco
- `China_Circuit.png` - Shanghai
- `Japan_Circuit.png` - Suzuka
- `Singapore_Circuit.png` - Marina Bay
- `Australia_Circuit.png` - Albert Park
- `USA_Circuit.png` - Circuit of the Americas / Miami
- `Brazil_Circuit.png` - Interlagos
- `Baku_Circuit.png` - Baku City Circuit
- `Spain_Circuit.png` - Catalunya
- `Hungary_Circuit.png` - Hungaroring
- `Austria_Circuit.png` - Red Bull Ring
- `Netherlands_Circuit.png` - Zandvoort
- `Abu_Dhabi_Circuit.png` - Yas Marina
- `Saudi_Arabia_Circuit.png` - Jeddah
- `Qatar_Circuit.png` - Losail
- `Bahrain_Circuit.png` - Bahrain
- `Las_Vegas_Circuit.png` - Las Vegas
- `Emilia_Romagna_Circuit.png` - Imola
- `Canada_Circuit.png` - Gilles Villeneuve
- `Mexico_Circuit.png` - Rodriguez

## Source URLs

All images can be downloaded from:
```
https://media.formula1.com/image/upload/f_auto,c_limit,q_auto/content/dam/fom-website/2018-redesign-assets/Circuit%20maps%2016x9/{circuitName}_Circuit
```

Replace `{circuitName}` with the circuit name from the list above.

## Example Download Commands

```bash
# Download all circuit track layouts
circuits=("Great_Britain" "Belgium" "Italy" "Monaco" "China" "Japan" "Singapore" "Australia" "USA" "Brazil" "Baku" "Spain" "Hungary" "Austria" "Netherlands" "Abu_Dhabi" "Saudi_Arabia" "Qatar" "Bahrain" "Las_Vegas" "Emilia_Romagna" "Canada" "Mexico")
for circuit in "${circuits[@]}"; do
  curl -o "${circuit}_Circuit.png" "https://media.formula1.com/image/upload/f_auto,c_limit,q_auto/content/dam/fom-website/2018-redesign-assets/Circuit%20maps%2016x9/${circuit}_Circuit"
done
```

