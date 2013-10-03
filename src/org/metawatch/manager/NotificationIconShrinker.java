package org.metawatch.manager;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

public class NotificationIconShrinker {
    static final int ICON_SIZE = 11;
    static final int NOTIFICATION_ICON_SIZE = 16;

    static double luminance(int color) {
	return Color.alpha(color) * (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color));
    }

    /** Used for the initial colour-to-monochrome threshold */
    public static double chooseThreshold(String packageName) {
	return (packageName.equals("com.google.android.music") || packageName.equals("com.android.music") || packageName.equals("com.google.android.apps.maps")) ? 0.1 : packageName.startsWith("com.meecel.") ? 0.9 : 0.65;
    }

    /** Used to resolve dithered pixels after shrinking */
    public static int chooseThreshold2(String packageName) {
	return packageName.equals("com.thedeck.android.app") ? 0xfe : 0x7f;
    }

    public static boolean shouldInvert(String packageName) {
	return packageName.equals("com.fsck.k9");
    }

    public static Bitmap shrink(Resources r, int iconId, String packageName, int maxSize) {
	Drawable d = null;
	try {
	    d = r.getDrawable(iconId);
	} catch (Exception e) {
	}
	if (d == null)
	    return null;
	return shrink(d, packageName, maxSize);
    }

    public static Bitmap shrink(Drawable d, String packageName, int maxSize) {
	if (d == null)
	    return null;

	// Coerce to Bitmap - might already be a BitmapDrawable, but make
	// sure it's mutable and doesn't have unhelpful density info attached
	int iw = d.getIntrinsicWidth();
	int ih = d.getIntrinsicHeight();
	if (iw <= 0) {
	    iw = maxSize;
	    ih = maxSize;
	}
	Bitmap icon = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_8888);
	d.setBounds(0, 0, iw, ih);
	d.draw(new Canvas(icon));

	// Find the brightest colour in use
	double maxLum = 0;
	iw = icon.getWidth();
	ih = icon.getHeight();
	for (int y = 0; y < ih; y++) {
	    for (int x = 0; x < iw; x++) {
		maxLum = Math.max(maxLum, luminance(icon.getPixel(x, y)));
	    }
	}

	// Threshold to monochrome (for LCD), and create a bounding box
	double thresholdLum = maxLum * chooseThreshold(packageName);
	int minX = iw, maxX = 0, minY = ih, maxY = 0;
	boolean inv = shouldInvert(packageName);
	for (int y = 0; y < ih; y++) {
	    for (int x = 0; x < iw; x++) {
		if (luminance(icon.getPixel(x, y)) >= thresholdLum) {
		    minX = Math.min(minX, x);
		    maxX = Math.max(maxX, x);
		    minY = Math.min(minY, y);
		    maxY = Math.max(maxY, y);
		    icon.setPixel(x, y, inv ? Color.WHITE : Color.BLACK);
		} else {
		    icon.setPixel(x, y, inv ? Color.BLACK : Color.WHITE);
		}
	    }
	}

	// Crop to remove all blank space around the thresholded icon
	if (maxX - minX >= 5 && maxY - minY >= 5) {
	    icon = Bitmap.createBitmap(icon, minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
	    iw = icon.getWidth();
	    ih = icon.getHeight();
	}

	// Remove border: if we can see a smaller bounding box ignoring the
	// outermost 1 pixel, then crop again to that
	minX = iw;
	maxX = 0;
	minY = ih;
	maxY = 0;
	for (int y = 1; y < ih - 1; y++) {
	    for (int x = 1; x < iw - 1; x++) {
		if (icon.getPixel(x, y) == Color.BLACK) {
		    minX = Math.min(minX, x);
		    maxX = Math.max(maxX, x);
		    minY = Math.min(minY, y);
		    maxY = Math.max(maxY, y);
		}
	    }
	}
	if (maxX - minX >= 5 && maxY - minY >= 5 && maxX - minX < ih - 3 && maxY - minY < ih - 3) {
	    icon = Bitmap.createBitmap(icon, minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
	    iw = icon.getWidth();
	    ih = icon.getHeight();
	}

	// Scale it to maxSize pixels high
	int w, h;
	if (iw > ih) {
	    w = maxSize;
	    h = (int) Math.round((((double) ih) / iw) * w);
	} else {
	    h = maxSize;
	    w = (int) Math.round((((double) iw) / ih) * h);
	}
	icon = Bitmap.createScaledBitmap(icon, w, h, true);

	// There may now be grey pixels; threshold them (again)
	int t = chooseThreshold2(packageName);
	for (int y = 0; y < h; y++) {
	    for (int x = 0; x < w; x++) {
		if (Color.green(icon.getPixel(x, y)) > t) {
		    icon.setPixel(x, y, Color.WHITE);
		} else {
		    icon.setPixel(x, y, Color.BLACK);
		}
	    }
	}
	return icon;
    }
}
