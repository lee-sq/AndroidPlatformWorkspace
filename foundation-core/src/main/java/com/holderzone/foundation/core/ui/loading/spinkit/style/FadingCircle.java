package com.holderzone.foundation.core.ui.loading.spinkit.style;

import android.animation.ValueAnimator;

import com.holderzone.foundation.core.ui.loading.spinkit.animation.SpriteAnimatorBuilder;
import com.holderzone.foundation.core.ui.loading.spinkit.sprite.CircleLayoutContainer;
import com.holderzone.foundation.core.ui.loading.spinkit.sprite.CircleSprite;
import com.holderzone.foundation.core.ui.loading.spinkit.sprite.Sprite;

/**
 * Created by ybq.
 */
public class FadingCircle extends CircleLayoutContainer {
    private static final int ANIMATION_DURATION_MS = 750;
    private static final int CHILD_COUNT = 12;

    @Override
    public Sprite[] onCreateChild() {
        Dot[] dots = new Dot[CHILD_COUNT];
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new Dot();
            dots[i].setAnimationDelay(ANIMATION_DURATION_MS / CHILD_COUNT * i);
            dots[i].setAlpha(i == 0 ? 255 : 0);
        }
        return dots;
    }

    private static class Dot extends CircleSprite {

        @Override
        public ValueAnimator onCreateAnimation() {
            float[] fractions = new float[]{0f, 0.39f, 0.4f, 1f};
            return new SpriteAnimatorBuilder(this).
                    alpha(fractions, 255, 0, 0, 255).
                    duration(ANIMATION_DURATION_MS).
                    easeInOut(fractions).build();
        }
    }
}
