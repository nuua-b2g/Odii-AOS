package kto.smarttour.binding;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;

import kto.smarttour.R;
import kto.smarttour.common.utils.ImageUtil;

public class BindingAdapters {
	@BindingAdapter({"bind:imageUrl", "bind:error"})
	public static void loadImage(ImageView imageView, String url, Drawable errorDrawable) {
		ImageUtil.loadImage(imageView, url, errorDrawable);
	}

	@BindingAdapter({"bind:localImageName", "bind:localError"})
	public static void localImage(ImageView imageView, String url, Drawable errorDrawable) {
		ImageUtil.localLoadImage(imageView, url, errorDrawable);
	}

	@BindingAdapter("bind:selected")
	public static void setSelected(View view, boolean isSelected) {
		if (isSelected) {
			view.setBackgroundColor(Color.parseColor("#F7F8FF"));
		} else {
			view.setBackgroundColor(Color.parseColor("#FFFFFF"));
		}
	}

	@BindingAdapter("bind:drvierLangCodeBox")
	public static void setDriverLangCodeBox(TextView tv, String langCode) {
		if(TextUtils.isEmpty(langCode)) {
			tv.setVisibility(View.INVISIBLE);
			return;
		}
		switch (langCode) {
			case "ko":
				tv.setBackgroundResource(R.drawable.box_driver_ko);
				tv.setTextColor(Color.parseColor("#696CFF"));
				tv.setText("한국어");
				break;
			case "cn":
			case "cn1":
				tv.setBackgroundResource(R.drawable.box_driver_cn);
				tv.setTextColor(Color.parseColor("#25CE60"));
				tv.setText("중국어");
				break;
			case "jp":
				tv.setBackgroundResource(R.drawable.box_driver_jp);
				tv.setTextColor(Color.parseColor("#FE6F61"));
				tv.setText("일본어");
				break;
			default:
			case "en":
				tv.setBackgroundResource(R.drawable.box_driver_en);
				tv.setTextColor(Color.parseColor("#2DCCD2"));
				tv.setText("영어");
				break;
		}
	}
}
