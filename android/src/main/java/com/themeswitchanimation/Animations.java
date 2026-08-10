package com.themeswitchanimation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.react.bridge.ReactContext;

public class Animations {

  public static void performInvertedCircleAnimation(ImageView overlay, View rootView, long duration, double cxRatio, double cyRatio, Runnable callback) {
    int width = rootView.getWidth();
    int height = rootView.getHeight();

    int cx = (int) (width * cxRatio);
    int cy = (int) (height * cyRatio);

    float startRadius = Helpers.getPointMaxDistanceInsideContainer(cx, cy, width, height);

    Animator anim = ViewAnimationUtils.createCircularReveal(overlay, cx, cy, startRadius, 0);
    anim.setDuration(duration);
    anim.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        overlay.setVisibility(View.GONE);
        callback.run();
      }
    });
    anim.start();
  }

  public static void performCircleAnimation(ImageView overlay, ViewGroup rootView, long duration, double cxRatio, double cyRatio, ReactContext reactContext, Runnable callback) {
    rootView.postOnAnimation(new Runnable() {
      private int frameCount = 0;

      @Override
      public void run() {
        frameCount++;
        // re-capture only after the flipped theme actually painted; the
        // react commit + fabric draw can outlast a frame or two on heavy
        // screens, and a stale capture makes the reveal look like a no-op
        if (frameCount < 6) {
          rootView.postOnAnimation(this);
        } else {
          reactContext.runOnUiQueueThread(() -> {
            // Creating another image after switching the theme
            // because we can't make the root view above the overlay
            overlay.setVisibility(View.GONE);
            final Bitmap[] capturedImageBitmap = {ThemeSwitchAnimationModule.captureScreenshot(rootView, reactContext)};
            ImageView capturedImageAfterSwitching = ThemeSwitchAnimationModule.createImageView(capturedImageBitmap[0], reactContext);
            overlay.setVisibility(View.VISIBLE);

            rootView.addView(capturedImageAfterSwitching, new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT));

            int width = rootView.getWidth();
            int height = rootView.getHeight();

            int cx = (int) (width * cxRatio);
            int cy = (int) (height * cyRatio);
            float finalRadius = Helpers.getPointMaxDistanceInsideContainer(cx, cy, width, height);

            Animator anim = ViewAnimationUtils.createCircularReveal(capturedImageAfterSwitching, cx, cy, 0, finalRadius);
            anim.setDuration(duration);
            anim.addListener(new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                capturedImageAfterSwitching.setVisibility(View.GONE);

                if (capturedImageBitmap[0] != null && !capturedImageBitmap[0].isRecycled()) {
                  capturedImageBitmap[0].recycle();
                  capturedImageBitmap[0] = null;
                }

                overlay.setVisibility(View.GONE);
                callback.run();
              }
            });

            anim.start();
          });
        }
      }
    });
  }

  public static void performFadeAnimation(final ImageView overlay, long duration, Runnable callback) {
    ObjectAnimator fadeOut = ObjectAnimator.ofFloat(overlay, "alpha", 1f, 0f);
    fadeOut.setDuration(duration);
    fadeOut.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        overlay.setVisibility(View.GONE);
        callback.run();
      }
    });
    fadeOut.start();
  }

  // draws the frozen capture with a growing hole, so the live (flipped)
  // tree shows through in the revealed ring — lets in-place motion at the
  // origin (the theme switch knob) animate visibly during the reveal
  static class HoleRevealView extends View {
    private final Bitmap bitmap;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Path full = new Path();
    private final Path hole = new Path();
    private final int cx;
    private final int cy;
    private float holeRadius;

    HoleRevealView(Context context, Bitmap bitmap, int cx, int cy) {
      super(context);
      this.bitmap = bitmap;
      this.cx = cx;
      this.cy = cy;
    }

    void setHoleRadius(float radius) {
      holeRadius = radius;
      invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
      full.reset();
      full.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
      hole.reset();
      hole.addCircle(cx, cy, holeRadius, Path.Direction.CW);
      full.op(hole, Path.Op.DIFFERENCE);
      canvas.save();
      canvas.clipPath(full);
      canvas.drawBitmap(bitmap, 0, 0, paint);
      canvas.restore();
    }
  }

  public static void performLiveCircleAnimation(final HoleRevealView overlay, ViewGroup rootView, long duration, double cxRatio, double cyRatio, Runnable callback) {
    int width = rootView.getWidth();
    int height = rootView.getHeight();

    int cx = (int) (width * cxRatio);
    int cy = (int) (height * cyRatio);
    final float finalRadius = Helpers.getPointMaxDistanceInsideContainer(cx, cy, width, height);

    ValueAnimator anim = ValueAnimator.ofFloat(0f, finalRadius);
    anim.setDuration(duration);
    anim.addUpdateListener((a) -> overlay.setHoleRadius((float) a.getAnimatedValue()));
    anim.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        callback.run();
      }
    });
    anim.start();
  }
}
