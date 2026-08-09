# react-native-theme-switch-animation

A Plug & Play animation for switching (Dark/Light) themes, **Android only**, revived for the **New Architecture** (Expo SDK 55+ / React Native 0.82+).

This is a revival of [WadhahEssam/react-native-theme-switch-animation](https://github.com/WadhahEssam/react-native-theme-switch-animation) (v0.8.0, abandoned at RN 0.74). The original broke on modern React Native because its Android Gradle build still gated the New Architecture behind the removed `newArchEnabled` property. This fork:

- targets the New Architecture only (the only architecture since RN 0.82)
- always applies the `com.facebook.react` Gradle plugin and codegen
- drops iOS support, the old-architecture sources and the legacy bridge fallback
- uses a source-only TypeScript package (no builder-bob build step)

## Installation

```sh
npm install react-native-theme-switch-animation
```

> Requires a development build (`npx expo prebuild && npx expo run:android`). It does **not** work in Expo Go, since it ships native code.

## Usage

```tsx
import switchTheme from 'react-native-theme-switch-animation';

const toggleTheme = () => {
  switchTheme({
    switchThemeFunction: () => {
      // flip your theme state here — it runs right after the screen is frozen
      setDarkMode(!darkMode);
    },
    animationConfig: {
      type: 'circular', // 'circular' | 'inverted-circular' | 'fade'
      duration: 800,
      startingPoint: { cx: 120, cy: 400 }, // or cxRatio / cyRatio (0..1)
      captureType: 'layer', // 'layer' | 'hierarchy'
    },
  });
};
```

## Compatibility

| Requirement | Value |
|---|---|
| Expo SDK | 55+ (56, 57 confirmed targets) |
| React Native | 0.82+ (New Architecture only) |
| React | 19.x |
| Platforms | Android (minSdk 24) |

## How it works

1. `freezeScreen` snapshots the current screen into an overlay.
2. A `FINISHED_FREEZING_SCREEN` event fires, your `switchThemeFunction` flips the theme underneath.
3. `unfreezeScreen` reveals the new theme with a circular / inverted-circular / fade animation.

## License

MIT
