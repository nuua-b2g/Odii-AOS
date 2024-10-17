package kto.smarttour.common.utils;

import android.os.Handler;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import kto.smarttour.R;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.download.DownloadHelper;
import kto.smarttour.ui.StoryLockerActivity;

public class DialogPerUtil {
	public interface OnDialogClick {
		void onExcute();
	}

	public interface OnDialogResultClick {
		void onResult(int position);
	}

	//반환형 수정 void -> AlertDialog
	public static AlertDialog showWarning(final AppCompatActivity activity, final String title, final String message, final String buttonLeft, final String buttonRight, final DialogUtil.OnDialogClick left, final DialogUtil.OnDialogClick right) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		View view = makeMessageView(activity);

		TextView tvTitle = view.findViewById(R.id.title);
		if (!TextUtils.isEmpty(title)) {
			tvTitle.setText(title);
			tvTitle.setContentDescription(title);
		} else {
			tvTitle.setVisibility(View.GONE);
		}

		TextView tvMessage = view.findViewById(R.id.message);
		tvMessage.setMovementMethod(new ScrollingMovementMethod());
		if (!TextUtils.isEmpty(message)) {
			tvMessage.setText(message);
			tvMessage.setContentDescription(message);
		} else {
			tvMessage.setVisibility(View.GONE);
		}

		view.findViewById(R.id.view_choice).setVisibility(View.GONE);
		builder.setView(view);

		AlertDialog dialog = builder.create();
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);

		TextView tvBtn_ok = view.findViewById(R.id.btn_ok);
		if (!TextUtils.isEmpty(buttonLeft)) {
			tvBtn_ok.setText(buttonLeft);
			tvBtn_ok.setContentDescription(buttonLeft);
			tvBtn_ok.setOnClickListener(v -> {

				//추가
				SettingsUtil.setCheckPermissionDialogAgree(activity,true);

				if (left != null) {
					left.onExcute();
				}
				dialog.dismiss();
			});
		} else {
			tvBtn_ok.setVisibility(View.GONE);
		}

		TextView tvBtn_cancel = view.findViewById(R.id.btn_cancel);
		if (!TextUtils.isEmpty(buttonRight)) {
			tvBtn_cancel.setText(buttonRight);
			tvBtn_cancel.setContentDescription(buttonRight);
			tvBtn_cancel.setOnClickListener(v -> {
				if (right != null) {
					right.onExcute();
				}
				dialog.dismiss();
			});
		} else {
			tvBtn_cancel.setVisibility(View.GONE);
		}

		dialog.show();
		dialog.getWindow().setAttributes(getLayoutParams(activity, dialog));

		//반환형 수정 void -> AlertDialog
		return dialog;
		//dialogWarning = dialog;
	}

	private static View makeMessageView(AppCompatActivity activity) {
		View view = LayoutInflater.from(activity).inflate(R.layout.custom_dialog_layout_2, null);
		return view;
	}

	private static WindowManager.LayoutParams getLayoutParams(AppCompatActivity activity, AlertDialog dialog) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int displayWidth = displayMetrics.widthPixels;
		WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
		layoutParams.copyFrom(dialog.getWindow().getAttributes());
		int dialogWindowWidth = (int) (displayWidth * 0.9f);
		layoutParams.width = dialogWindowWidth;
		return layoutParams;
	}
}
