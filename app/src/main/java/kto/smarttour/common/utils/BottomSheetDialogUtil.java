package kto.smarttour.common.utils;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.nguyenhoanglam.imagepicker.model.Config;
import com.nguyenhoanglam.imagepicker.model.Image;
import com.nguyenhoanglam.imagepicker.ui.imagepicker.ImagePicker;

import java.io.File;
import java.util.ArrayList;

import kto.smarttour.R;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.ui.StoryLockerActivity;

import static androidx.appcompat.app.AppCompatActivity.RESULT_OK;

public class BottomSheetDialogUtil {

	public static final int SUCCESS = 0;
	public static final int ERROR = -1;

	public static final int TOAST_MENU_PLAYER_DETAIL = 0;
	public static final int TOAST_MENU_PLAYER_LOCKER = 1;
	public static final int TOAST_MENU_PLAYER_DOWNLOAD = 2;
	public static final int TOAST_MENU_PLAYER_CANCEL = -1;


	private BottomSheetDialog bottomSheetDialog;
	private ImageView ivThumb;
	private String imageFilePath = "";
	private String imageFileName = "";
	private OnBottomSheetListener callback;

	public void showFolderMakeDialog(AppCompatActivity activity, OnBottomSheetListener callback) {
		this.callback = callback;
		View bottomSheetView = activity.getLayoutInflater().inflate(R.layout.story_folder_make_layout, null);
		bottomSheetDialog = new BottomSheetDialog(activity);
		bottomSheetDialog.setContentView(bottomSheetView);
		BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
		bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == BottomSheetBehavior.STATE_HIDDEN) {
					bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
					bottomSheetDialog.dismiss();
				}
			}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {

			}
		});

		EditText folderName = bottomSheetView.findViewById(R.id.edit_folder_name);
		folderName.setOnEditorActionListener((v, actionId, event) -> {
			CommonUtils.hideKeyboard(v);
			if(!TextUtils.isEmpty(v.getText())) {
				if(ViewUtils.isAccessibility(activity)) {
					folderName.setHint(v.getText() + " 으로 보관함 제목이 설정됨");
				}
			}
			return false;
		});


		ivThumb = bottomSheetView.findViewById(R.id.iv_thumb);
		ivThumb.setOnClickListener(view -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_create_image");
			ImagePicker.with(activity)                         //  Initialize ImagePicker with activity or fragment context
					.setToolbarColor("#212121")         //  Toolbar color
					.setStatusBarColor("#000000")       //  StatusBar color (works with SDK >= 21  )
					.setToolbarTextColor("#FFFFFF")     //  Toolbar text color (Title and Done button)
					.setToolbarIconColor("#FFFFFF")     //  Toolbar icon color (Back and Camera button)
					.setProgressBarColor("#4CAF50")     //  ProgressBar color
					.setBackgroundColor("#212121")      //  Background color
					.setCameraOnly(false)               //  Camera mode
					.setMultipleMode(false)              //  Select multiple images or single image
					.setFolderMode(true)                //  Folder mode
					.setShowCamera(true)                //  Show camera button
					.setFolderTitle("Albums")           //  Folder title (works with FolderMode = true)
					.setImageTitle("Galleries")         //  Image title (works with FolderMode = false)
					.setDoneTitle("Done")               //  Done button title
					.setLimitMessage("You have reached selection limit")    // Selection limit message
					.setDirectoryName("ImagePicker")         //  Image capture folder name
					.setAlwaysShowDoneButton(true)      //  Set always show done button in multiple mode
					.setRequestCode(100)                //  Set request code, default Config.RC_PICK_IMAGES
					.start();                           //  Start ImagePicker

		});

		bottomSheetView.findViewById(R.id.btn_cancel).setOnClickListener(view -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_create_cancel");
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			bottomSheetDialog.dismiss();
		});

		bottomSheetView.findViewById(R.id.btn_close).setOnClickListener(view -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_create_close");
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			bottomSheetDialog.dismiss();
		});

		bottomSheetView.findViewById(R.id.btn_make).setOnClickListener(view -> {
			if (TextUtils.isEmpty(folderName.getText())) {
				callback.onResult(ERROR);
			} else {
				if (!TextUtils.isEmpty(imageFileName) && !TextUtils.isEmpty(imageFileName)) {
					FileUtils.copyFile(activity, new File(imageFilePath), imageFileName);
				}
				AnalyticsInterface.getInstance().logEvent(activity, "library_create_ok");
				StoryDbManager.getInstance(activity).insertFolder(folderName.getText().toString(), imageFileName, "");
				callback.onResult(SUCCESS);
				bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
				bottomSheetDialog.dismiss();
			}
		});

		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
		bottomSheetDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		bottomSheetDialog.show();
	}

	public void showFolderEditorDialog(AppCompatActivity activity, String fid, String name, String imgUrl, OnBottomSheetListener callback) {
		this.callback = callback;
		View bottomSheetView = activity.getLayoutInflater().inflate(R.layout.story_folder_edit_layout, null);
		bottomSheetDialog = new BottomSheetDialog(activity);
		bottomSheetDialog.setContentView(bottomSheetView);
		BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
		bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == BottomSheetBehavior.STATE_HIDDEN) {
					bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
					bottomSheetDialog.dismiss();
				}
			}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {

			}
		});

		EditText folderName = bottomSheetView.findViewById(R.id.edit_folder_name);
		folderName.setOnEditorActionListener((v, actionId, event) -> {
			if(!TextUtils.isEmpty(folderName.getText())) {
				if(ViewUtils.isAccessibility(activity)) {
					String contentDescription = folderName.getText() + " 으로 보관함 제목이 설정됨";
					folderName.setHint(contentDescription);
				}
			}
			CommonUtils.hideKeyboard(v);
			return false;
		});
		folderName.setText(name);

		ivThumb = bottomSheetView.findViewById(R.id.iv_thumb);
		ivThumb.setOnClickListener(view -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_edit_image");
			ImagePicker.with(activity)                         //  Initialize ImagePicker with activity or fragment context
					.setToolbarColor("#212121")         //  Toolbar color
					.setStatusBarColor("#000000")       //  StatusBar color (works with SDK >= 21  )
					.setToolbarTextColor("#FFFFFF")     //  Toolbar text color (Title and Done button)
					.setToolbarIconColor("#FFFFFF")     //  Toolbar icon color (Back and Camera button)
					.setProgressBarColor("#4CAF50")     //  ProgressBar color
					.setBackgroundColor("#212121")      //  Background color
					.setCameraOnly(false)               //  Camera mode
					.setMultipleMode(false)              //  Select multiple images or single image
					.setFolderMode(true)                //  Folder mode
					.setShowCamera(true)                //  Show camera button
					.setFolderTitle("Albums")           //  Folder title (works with FolderMode = true)
					.setImageTitle("Galleries")         //  Image title (works with FolderMode = false)
					.setDoneTitle("Done")               //  Done button title
					.setLimitMessage("You have reached selection limit")    // Selection limit message
					.setDirectoryName("ImagePicker")         //  Image capture folder name
					.setAlwaysShowDoneButton(true)      //  Set always show done button in multiple mode
					.setRequestCode(100)                //  Set request code, default Config.RC_PICK_IMAGES
					.start();                           //  Start ImagePicker

		});

		bottomSheetView.findViewById(R.id.btn_cancel).setOnClickListener(view -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_edit_cancel");
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			bottomSheetDialog.dismiss();
		});

		bottomSheetView.findViewById(R.id.btn_close).setOnClickListener(view -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_edit_close");
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			bottomSheetDialog.dismiss();
		});

		bottomSheetView.findViewById(R.id.btn_make).setOnClickListener(view -> {
			if (TextUtils.isEmpty(folderName.getText())) {
				callback.onResult(ERROR);
			} else {
				if (!TextUtils.isEmpty(imageFilePath) && !TextUtils.isEmpty(imageFileName)) {
					FileUtils.copyFile(activity, new File(imageFilePath), imageFileName);
				}
				StoryDbManager.getInstance(activity).updateFolder(fid, folderName.getText().toString(), imageFileName);
				new File(imgUrl).delete();
				AnalyticsInterface.getInstance().logEvent(activity, "library_edit_ok");
				callback.onResult(SUCCESS);
				bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
				bottomSheetDialog.dismiss();
			}
		});

		if (!TextUtils.isEmpty(imgUrl)) {
			ImageUtil.localLoadImage(ivThumb, imgUrl, null);
		}

		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
		bottomSheetDialog.show();
	}

	public static void showPlayerMenuDialog(AppCompatActivity activity, OnBottomSheetListener callback) {
		showPlayerMenuDialog(activity, -1, callback);
	}

	public static void showPlayerMenuDialog(AppCompatActivity activity, @StyleRes int styleId, OnBottomSheetListener callback) {
		View bottomSheetView = activity.getLayoutInflater().inflate(R.layout.bottom_toast_menu_player, null);
		BottomSheetDialog bottomSheetDialog;
		if (styleId != -1) {
			bottomSheetDialog = new BottomSheetDialog(activity, styleId);
		} else {
			bottomSheetDialog = new BottomSheetDialog(activity);
		}
		bottomSheetDialog.setContentView(bottomSheetView);
		BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
		bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == BottomSheetBehavior.STATE_HIDDEN) {
					bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
					bottomSheetDialog.dismiss();
				}
			}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {

			}
		});

		bottomSheetView.findViewById(R.id.btn_toast_detail).setOnClickListener(view -> {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			bottomSheetDialog.dismiss();
			callback.onResult(TOAST_MENU_PLAYER_DETAIL);
			AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_detail");
		});

		bottomSheetView.findViewById(R.id.btn_toast_locker).setOnClickListener(view -> {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			bottomSheetDialog.dismiss();
			callback.onResult(TOAST_MENU_PLAYER_LOCKER);
			AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_fill");
		});

		bottomSheetView.findViewById(R.id.btn_toast_download).setOnClickListener(view -> {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			bottomSheetDialog.dismiss();
			callback.onResult(TOAST_MENU_PLAYER_DOWNLOAD);
			AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_download");
		});

		bottomSheetView.findViewById(R.id.btn_toast_cancel).setOnClickListener(view -> {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			bottomSheetDialog.dismiss();
			callback.onResult(TOAST_MENU_PLAYER_CANCEL);
			AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_cancel");
		});

		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
		WindowManager.LayoutParams lp = bottomSheetDialog.getWindow().getAttributes();
		lp.dimAmount = 0.6f;
		bottomSheetDialog.getWindow().setAttributes(lp);
		bottomSheetDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		bottomSheetDialog.show();
	}

	public void setThumb(String url) {
		if (ivThumb != null) {
			ImageUtil.loadImage(ivThumb, url, getDrawableFromResource(ivThumb, R.drawable.ic_img_scrap_dummy));
		}
	}

	public void setThumb(Uri url) {
		if (ivThumb != null) {
			ImageUtil.loadImage(ivThumb, url, getDrawableFromResource(ivThumb, R.drawable.ic_img_scrap_dummy));
		}
	}

	protected static Drawable getDrawableFromResource(View view, int resourceId) {
        return view.getContext().getDrawable(resourceId);
    }

	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == Config.RC_PICK_IMAGES && resultCode == RESULT_OK && data != null) {
			ArrayList<Image> images = data.getParcelableArrayListExtra(Config.EXTRA_IMAGES);
			imageFilePath = images.get(0).getPath();
			imageFileName = images.get(0).getName();
			setThumb(images.get(0).getPath());
		}
	}

	public interface OnBottomSheetListener {
		void onResult(int result);
	}
}
