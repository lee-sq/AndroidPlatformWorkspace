package com.holderzone.foundation.core.ui.feedback.toast;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.holderzone.foundation.core.R;

/**
 * StyledToast helper methods.
 *
 * <p>This file is based on Toasty, licensed under LGPL-3.0-or-later.</p>
 */
final class StyledToastUtils {
    private StyledToastUtils() {
    }

    static Drawable tintIcon(@NonNull Drawable drawable, @ColorInt int tintColor) {
        drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    static Drawable tint9PatchDrawableFrame(@NonNull Context context, @ColorInt int tintColor) {
        final NinePatchDrawable toastDrawable = (NinePatchDrawable) getDrawable(
                context,
                R.drawable.foundation_toast_frame
        );
        return tintIcon(toastDrawable, tintColor);
    }

    static void setBackground(@NonNull View view, Drawable drawable) {
        view.setBackground(drawable);
    }

    static Drawable getDrawable(@NonNull Context context, @DrawableRes int id) {
        return AppCompatResources.getDrawable(context, id);
    }

    static int getColor(@NonNull Context context, @ColorRes int color) {
        return ContextCompat.getColor(context, color);
    }
}
