import { Platform } from 'react-native';

const IS_SUPPORTED_PLATFORM = Platform.OS === 'android';

const module = (() => {
  if (IS_SUPPORTED_PLATFORM) {
    return require('./NativeThemeSwitchAnimationModule').default;
  }
  return undefined;
})();

export default module;
