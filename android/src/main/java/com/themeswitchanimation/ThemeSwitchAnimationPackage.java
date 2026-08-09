package com.themeswitchanimation;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.TurboReactPackage;

import java.util.HashMap;
import java.util.Map;

public class ThemeSwitchAnimationPackage extends TurboReactPackage {

  @Nullable
  @Override
  public NativeModule getModule(String name, ReactApplicationContext reactContext) {
    if (name.equals(ThemeSwitchAnimationModule.NAME)) {
      return new ThemeSwitchAnimationModule(reactContext);
    } else {
      return null;
    }
  }

  @Override
  public ReactModuleInfoProvider getReactModuleInfoProvider() {
    return () -> {
      final Map<String, ReactModuleInfo> moduleInfos = new HashMap<>();
      moduleInfos.put(
              ThemeSwitchAnimationModule.NAME,
              new ReactModuleInfo(
                      ThemeSwitchAnimationModule.NAME,
                      ThemeSwitchAnimationModule.NAME,
                      false, // canOverrideExistingModule
                      false, // needsEagerInit
                      false, // isCxxModule
                      true // isTurboModule
      ));
      return moduleInfos;
    };
  }
}
