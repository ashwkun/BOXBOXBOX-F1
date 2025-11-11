# 🎨 Adding Your BOX BOX BOX Logo

## Quick Steps:

### 1. **Export Logo from Image**

Save your "BOX BOX BOX" tire logo as a PNG file:
- Size: **512x512px** (or larger, square)
- Format: **PNG with transparency**
- Name it: `boxboxbox_logo.png`

### 2. **Add to Project**

Copy the logo to:
```
/Users/aswinc/BOXBOXBOX APP/app/src/main/res/drawable/splash_logo.png
```

This will replace the temporary placeholder logo.

### 3. **Update Launcher Icons**

For the app icon, create different sizes:

**Tool Option 1: Android Asset Studio (Easiest)**
1. Go to: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
2. Upload your logo
3. Download the generated icons
4. Replace files in `app/src/main/res/mipmap-*/`

**Tool Option 2: Manual**
Create these sizes and place in respective folders:
- `mipmap-mdpi/ic_launcher.png` - 48x48px
- `mipmap-hdpi/ic_launcher.png` - 72x72px
- `mipmap-xhdpi/ic_launcher.png` - 96x96px
- `mipmap-xxhdpi/ic_launcher.png` - 144x144px
- `mipmap-xxxhdpi/ic_launcher.png` - 192x192px

### 4. **For the Figma Animation**

Your animation repo: https://github.com/ashwkun/F1applaunchanimation

To integrate the Figma animation:

**Option A: Convert to Lottie (Recommended)**
1. Export animation from Figma as Lottie JSON
2. Add Lottie dependency to `app/build.gradle.kts`:
   ```kotlin
   implementation("com.airbnb.android:lottie-compose:6.1.0")
   ```
3. Place JSON file in `app/src/main/res/raw/launch_animation.json`
4. Use in SplashScreen.kt:
   ```kotlin
   val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.launch_animation))
   LottieAnimation(composition = composition)
   ```

**Option B: Use Vector Drawables**
1. Export animation frames as SVG
2. Convert to Android Vector Drawables
3. Animate using AnimatedVectorDrawable

## Current Status:
✅ Splash screen framework ready
✅ Animation structure in place
⏳ Waiting for logo image
⏳ Waiting for Lottie animation JSON

## After Adding Files:
```bash
cd "/Users/aswinc/BOXBOXBOX APP"
export ANDROID_HOME=~/Library/Android/sdk
./gradlew assembleDebug
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

