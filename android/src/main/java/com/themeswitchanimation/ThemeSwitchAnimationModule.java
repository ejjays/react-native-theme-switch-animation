package com.themeswitchanimation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactMethod;

public class ThemeSwitchAnimationModule extends ThemeSwitchAnimationModuleSpec {
  public static final String NAME = "ThemeSwitchAnimationModule";

  private final ReactApplicationContext reactContext;
  private ViewGroup rootView;
  private ImageView capturedImageView;
  private Bitmap capturedImageBitmap;
  private boolean isAnimating = false;

  public ThemeSwitchAnimationModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void freezeScreen(String captureType) {
    if (this.isAnimating) {
      return;
    }
    this.isAnimating = true;
    View activityView = reactContext.getCurrentActivity() != null
        ? reactContext.getCurrentActivity().getWindow().getDecorView()
        : null;
    if (activityView == null || !(activityView instanceof ViewGroup)) {
      // No window to freeze (e.g. app in background) - degrade to a plain switch.
      this.isAnimating = false;
      reactContext.emitDeviceEvent("FINISHED_FREEZING_SCREEN");
      return;
    }
    this.rootView = (ViewGroup) activityView;

    // capture must draw on the ui thread: drawing the tree from the native
    // modules thread races the frame render, and compose aborts on the
    // cross-thread snapshot observation
    reactContext.runOnUiQueueThread(() -> {
      this.capturedImageBitmap = captureScreenshot(this.rootView, this.reactContext);
      if (capturedImageBitmap == null) {
        this.isAnimating = false;
        reactContext.emitDeviceEvent("FINISHED_FREEZING_SCREEN");
        return;
      }
      this.capturedImageView = createImageView(this.capturedImageBitmap, this.reactContext);
      if (capturedImageView == null || rootView == null) {
        return;
      }
      rootView.addView(capturedImageView, new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT));
      reactContext.emitDeviceEvent("FINISHED_FREEZING_SCREEN");
    });
  }

  @ReactMethod
  public void unfreezeScreen(String animationType, double duration, double cxRatio, double cyRatio) {
    reactContext.runOnUiQueueThread(new Runnable() {
      @Override
      public void run() {
        if (isAnimating) {
          switch (animationType) {
            case "circular-live":
              // live hole reveal: swap the frozen capture for the hole view,
              // which draws the capture minus a growing circle
              rootView.removeView(capturedImageView);
              int width = rootView.getWidth();
              int height = rootView.getHeight();
              int cx = (int) (width * cxRatio);
              int cy = (int) (height * cyRatio);
              final Animations.HoleRevealView holeView = new Animations.HoleRevealView(reactContext, capturedImageBitmap, cx, cy);
              rootView.addView(holeView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
              Animations.performLiveCircleAnimation(holeView, rootView, (int) duration, cxRatio, cyRatio, new Runnable() {
                @Override
                public void run() {
                  rootView.removeView(holeView);
                  cleanUp();
                }
              });
              break;
            case "circular":
              Animations.performCircleAnimation(capturedImageView, rootView, (int) duration, cxRatio, cyRatio, reactContext, new Runnable() {
                @Override
                public void run() {
                  cleanUp();
                }
              });
              break;
            case "inverted-circular":
              Animations.performInvertedCircleAnimation(capturedImageView, rootView, (int) duration, cxRatio, cyRatio, new Runnable() {
                @Override
                public void run() {
                  cleanUp();
                }
              });
              break;
            case "fade":
            default:
              Animations.performFadeAnimation(capturedImageView, (int) duration, new Runnable() {
                @Override
                public void run() {
                  cleanUp();
                }
              });
              break;
          }
        }
      }
    });
  }

  public void cleanUp() {
    if (capturedImageBitmap != null && !capturedImageBitmap.isRecycled()) {
      capturedImageBitmap.recycle();
      capturedImageBitmap = null;
    }
    if (rootView != null && capturedImageView != null) {
      rootView.removeView(capturedImageView);
    }
    capturedImageView = null;
    isAnimating = false;
  }

  public static Bitmap captureScreenshot(View rootView, ReactContext reactContext) {
    Bitmap capturedImageBitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(capturedImageBitmap);
    rootView.draw(canvas);

    return capturedImageBitmap;
  }

  public static ImageView createImageView(Bitmap capturedImageBitmap, ReactContext reactContext) {
    ImageView capturedImageView = new ImageView(reactContext);
    LinearLayout.LayoutParams fullScreenImageOverlayLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    capturedImageView.setLayoutParams(fullScreenImageOverlayLP);
    capturedImageView.setImageBitmap(capturedImageBitmap);
    capturedImageView.setVisibility(View.VISIBLE);

    return capturedImageView;
  }

  @ReactMethod
  public void addListener(String eventName) {}

  @ReactMethod
  public void removeListeners(double count) {}
}
