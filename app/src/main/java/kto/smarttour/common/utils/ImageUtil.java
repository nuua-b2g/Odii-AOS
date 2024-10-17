package kto.smarttour.common.utils;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.io.File;

import kto.smarttour.R;

public class ImageUtil {
	public static void loadImage(ImageView imageView, String url, Drawable errorDrawable) {
		try {
			Glide.with(imageView.getContext()).load(url).error(errorDrawable).placeholder(R.drawable.ic_img_scrap_dummy).centerCrop().into(imageView);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	//추가 loadImage_CornerRadius
	public static void loadImage_CornerRadius(ImageView imageView, String url, Drawable errorDrawable, int cornerRadius) {
		try {
			//Glide.with(imageView.getContext()).load(url).error(errorDrawable).placeholder(R.drawable.ic_img_scrap_dummy).centerCrop().into(imageView);
			//cornerRadius=24
			Glide.with(imageView.getContext())
//				.asBitmap()
					.load(url)
					.transform(new CenterCrop(), new RoundedCorners(cornerRadius))
					.error(errorDrawable)
					.placeholder(R.drawable.ic_img_scrap_dummy)
					.into(imageView);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void loadImage(ImageView imageView, Uri url, Drawable errorDrawable) {
		try {
			Glide.with(imageView.getContext()).load(url).error(errorDrawable).placeholder(R.drawable.ic_img_scrap_dummy).centerCrop().into(imageView);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void localLoadImage(ImageView imageView, String imageFileName, Drawable errorDrawable) {
		try {
			Glide.with(imageView.getContext()).load(new File(FileUtils.getImageUrl(imageView.getContext(), imageFileName))).error(errorDrawable).placeholder(R.drawable.ic_img_scrap_dummy).centerCrop().into(imageView);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void localLoadImage2(ImageView imageView, String imageFileName, Drawable errorDrawable) {
		try {
			Glide.with(imageView.getContext()).load(new File(imageFileName)).error(errorDrawable).placeholder(R.drawable.ic_img_scrap_dummy).centerCrop().into(imageView);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	//추가 localLoadImage2_CornerRadius
	public static void localLoadImage2_CornerRadius(ImageView imageView, String imageFileName, Drawable errorDrawable, int cornerRadius) {
		try {
			//Glide.with(imageView.getContext()).load(new File(imageFileName)).error(errorDrawable).placeholder(R.drawable.ic_img_scrap_dummy).centerCrop().into(imageView);
			//cornerRadius=24
			Glide.with(imageView.getContext())
//				.asBitmap()
					.load(new File(imageFileName))
					.transform(new CenterCrop(), new RoundedCorners(cornerRadius))
					.error(errorDrawable)
					.placeholder(R.drawable.ic_img_scrap_dummy)
					.into(imageView);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void localLoadImage3(ImageView imageView, String imageFileName, Object o) {
		try {
			Glide.with(imageView.getContext()).load(new File(imageFileName)).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().centerCrop().transform(new MultiTransformation<>(new CenterCrop(), new GradientTransformation())).into(imageView);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
