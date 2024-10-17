package kto.smarttour.common.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

public class GradientTransformation extends BitmapTransformation {
	@Override
	protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
		Bitmap bitmap = toTransform.copy(Bitmap.Config.ARGB_8888, true);

		int w = outWidth;
		int h = outHeight;
		Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(result);

		canvas.drawBitmap(bitmap, 0, 0, null);

		Paint paint = new Paint();

		LinearGradient shader = new LinearGradient(0, h/3, 0, h, Color.parseColor("#00000000"), Color.parseColor("#FF000000"), Shader.TileMode.CLAMP);
		paint.setShader(shader);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
		canvas.drawRect(0, 0, w, h, paint);

		return result;
	}

	@Override
	public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
		messageDigest.update("Gradient Transformation".getBytes());
	}
}
