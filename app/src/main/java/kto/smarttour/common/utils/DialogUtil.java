package kto.smarttour.common.utils;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import kto.smarttour.R;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.download.DownloadHelper;
import kto.smarttour.ui.StoryLockerActivity;

public class DialogUtil {
	public interface OnDialogClick {
		void onExcute();
	}

	public interface OnDialogResultClick {
		void onResult(int position);
	}

	//--
	public static void showWarning(final AppCompatActivity activity, final int title, final int message, final int buttonLeft) {
		showWarning(activity, activity.getString(title), activity.getString(message), activity.getString(buttonLeft), null, null, null);
	}

	//--
	public static void showWarning(final AppCompatActivity activity, final int title, final int message, final int buttonLeft, final int buttonRight, final OnDialogClick left, final OnDialogClick right) {
		showWarning(activity, activity.getString(title), activity.getString(message), activity.getString(buttonLeft), activity.getString(buttonRight), left, right);
	}

	//--
	public static void showWarning(final AppCompatActivity activity, final String title, final String message, final int buttonLeft, final int buttonRight, final OnDialogClick left, final OnDialogClick right) {
		showWarning(activity, title, message, activity.getString(buttonLeft), activity.getString(buttonRight), left, right);
	}

	public static void showWarning(final AppCompatActivity activity, final String title, final String message, final String buttonLeft, final String buttonRight, final OnDialogClick left, final OnDialogClick right) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		View view = makeMessageView(activity);

		if (!TextUtils.isEmpty(title)) {
			((TextView) view.findViewById(R.id.title)).setText(title);
		} else {
			view.findViewById(R.id.view_title).setVisibility(View.GONE);
		}

		((TextView) view.findViewById(R.id.message)).setMovementMethod(new ScrollingMovementMethod());
		if (!TextUtils.isEmpty(message)) {
			((TextView) view.findViewById(R.id.message)).setText(message);
		} else {
			view.findViewById(R.id.view_message).setVisibility(View.GONE);
		}

		view.findViewById(R.id.view_choice).setVisibility(View.GONE);
		builder.setView(view);
		AlertDialog dialog = builder.create();
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);

		if (!TextUtils.isEmpty(buttonLeft)) {
			((TextView) view.findViewById(R.id.btn_ok)).setText(buttonLeft);
			view.findViewById(R.id.btn_ok).setOnClickListener(v -> {
				if (!activity.isFinishing()) {
					//dialog.dismiss();

					if (left != null) {
						left.onExcute();
					}

					dialog.dismiss();
				}
			});
		} else {
			view.findViewById(R.id.btn_ok).setVisibility(View.GONE);
		}

		if (!TextUtils.isEmpty(buttonRight)) {
			((TextView) view.findViewById(R.id.btn_cancel)).setText(buttonRight);
			view.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
				if (!activity.isFinishing()) {
					//dialog.dismiss();

					if (right != null) {
						right.onExcute();
					}

					dialog.dismiss();
				}
			});
		} else {
			view.findViewById(R.id.btn_cancel).setVisibility(View.GONE);
		}

		dialog.show();
		dialog.getWindow().setAttributes(getLayoutParams(activity, dialog));
	}


	public static void showChoice(final AppCompatActivity activity, final String title, final String message, final String[] items, final String buttonRight, final OnDialogResultClick right) {
		showChoice(activity, title, message, items, null, buttonRight, right);
	}

	public static void showChoice(final AppCompatActivity activity, final String title, final String message, final String[] items, final int[] item_resource, final String buttonRight, final OnDialogResultClick right) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		View view = makeMessageView(activity);

		if (!TextUtils.isEmpty(title)) {
			((TextView) view.findViewById(R.id.title)).setText(title);
			((TextView) view.findViewById(R.id.title)).setContentDescription(title);
		} else {
			view.findViewById(R.id.view_title).setVisibility(View.GONE);
		}

		if (!TextUtils.isEmpty(message)) {
			((TextView) view.findViewById(R.id.message)).setText(message);
			((TextView) view.findViewById(R.id.message)).setContentDescription(message);
		} else {
			view.findViewById(R.id.view_message).setVisibility(View.GONE);
		}

		builder.setView(view);
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);

		view.findViewById(R.id.btn_ok).setVisibility(View.GONE);
		LinearLayout ll = (LinearLayout) view.findViewById(R.id.view_choice);
		for (int i = 0; i < items.length; i++) {

			View v;
			if (item_resource != null) {
				v = LayoutInflater.from(activity).inflate(R.layout.choice_icon_text_item, null);
				((ImageView) v.findViewById(R.id.icon)).setBackgroundResource(item_resource[i]);
				((TextView) v.findViewById(R.id.title)).setText(items[i]);
			} else {
				v = LayoutInflater.from(activity).inflate(R.layout.choice_text_item, null);
				((TextView) v.findViewById(R.id.title)).setText(items[i]);
				((TextView) v.findViewById(R.id.title)).setContentDescription(items[i]);
			}


			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (right != null) {
						right.onResult((int) v.getTag());
					}
					dialog.dismiss();
				}
			});

			v.setTag(i);
			ll.addView(v);
		}
		if (!TextUtils.isEmpty(buttonRight) && right != null) {
			((TextView) view.findViewById(R.id.btn_cancel)).setText(buttonRight);
			view.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
				AnalyticsInterface.getInstance().logEvent(activity, "popup_cancel");
				dialog.dismiss();
			});
		} else {
			view.findViewById(R.id.btn_cancel).setVisibility(View.GONE);
		}

		dialog.show();
		dialog.getWindow().setAttributes(getLayoutParams(activity, dialog));
	}

	public static void showChoiceTextSize(final AppCompatActivity activity, final String title, final String buttonRight, final OnDialogResultClick right) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		String[] items = {activity.getString(R.string.basic), "2X", "4X"};
		String[] itemsDescriptions = {"기본 크기", "2배 크기", "4배 크기"};
		View view = makeMessageView(activity);

		if (!TextUtils.isEmpty(title)) {
			((TextView) view.findViewById(R.id.title)).setText(title);
			((TextView) view.findViewById(R.id.title)).setContentDescription(title);
		} else {
			view.findViewById(R.id.view_title).setVisibility(View.GONE);
		}

		view.findViewById(R.id.view_message).setVisibility(View.GONE);

		builder.setView(view);
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);

		view.findViewById(R.id.btn_ok).setVisibility(View.GONE);
		LinearLayout ll = (LinearLayout) view.findViewById(R.id.view_choice);

		for (int i = 0; i < items.length; i++) {
			View v = LayoutInflater.from(activity).inflate(R.layout.choice_text_item, null);
			((TextView) v.findViewById(R.id.title)).setText(items[i]);
			((TextView) v.findViewById(R.id.title)).setContentDescription(itemsDescriptions[i]);


			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (right != null) {
						right.onResult((int) v.getTag());
					}
					dialog.dismiss();
				}
			});

			v.setTag(i);
			ll.addView(v);
		} if (!TextUtils.isEmpty(buttonRight) && right != null) {
			((TextView) view.findViewById(R.id.btn_cancel)).setText(buttonRight);
			view.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
				AnalyticsInterface.getInstance().logEvent(activity, "popup_cancel");
				dialog.dismiss();
			});
		} else {
			view.findViewById(R.id.btn_cancel).setVisibility(View.GONE);
		}

		dialog.show();
		dialog.getWindow().setAttributes(getLayoutParams(activity, dialog));
	}

	public static void showDownLoad(final AppCompatActivity activity, final int title, final int message, final StoryData downloadList, final int buttonRight, final OnDialogClick right) {
		showDownLoad(activity, null, -1, title, message, downloadList, buttonRight, right);
	}

	public static void showDownLoad(final AppCompatActivity activity, final Handler handler, final int file_download_type, final int title, final int message, final StoryData downloadList, final int buttonRight, final OnDialogClick right) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		View view = makeProgressView(activity);

		if (!TextUtils.isEmpty(activity.getString(title))) {
			((TextView) view.findViewById(R.id.title)).setText(title);
		} else {
			view.findViewById(R.id.view_title).setVisibility(View.GONE);
		}

		if (!TextUtils.isEmpty(activity.getString(message))) {
			((TextView) view.findViewById(R.id.message)).setText(String.format(activity.getString(message), downloadList.story.size()));
		} else {
			view.findViewById(R.id.view_message).setVisibility(View.GONE);
		}

		builder.setView(view);
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);

		view.findViewById(R.id.btn_ok).setVisibility(View.GONE);
		ProgressBar progressBar = view.findViewById(R.id.progress);
		DownloadHelper helper = new DownloadHelper(activity, progressBar, dialog, handler, file_download_type);

		if (!TextUtils.isEmpty(activity.getString(buttonRight)) && right != null) {
			((TextView) view.findViewById(R.id.btn_cancel)).setText(buttonRight);
			view.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
				if (right != null) {
					dialog.dismiss();
				}
				helper.cancel();
			});
		} else {
			view.findViewById(R.id.btn_cancel).setVisibility(View.GONE);
		}

		dialog.setOnKeyListener((adialog, keyCode, event) -> {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				return true;
			}
			return false;
		});

		dialog.show();
		helper.start(downloadList);
		dialog.getWindow().setAttributes(getLayoutParams(activity, dialog));

	}

	public static void showDownLoad_V2(final AppCompatActivity activity, final Handler handler, final int file_download_type, final int title, final int message, final StoryData downloadList, final int buttonRight, final OnDialogClick right) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		View view = makeProgressView_V2(activity);

		if (!TextUtils.isEmpty(activity.getString(title))) {
			((TextView) view.findViewById(R.id.title)).setText(title);
		} else {
			view.findViewById(R.id.view_title).setVisibility(View.GONE);
		}

		if (!TextUtils.isEmpty(activity.getString(message))) {
			((TextView) view.findViewById(R.id.message)).setText(String.format(activity.getString(message), downloadList.story.size()));
		} else {
			view.findViewById(R.id.view_message).setVisibility(View.GONE);
		}
		
		//----
		//초기
		View view_step_one = view.findViewById(R.id.view_step_one);
		if(view_step_one.getVisibility()!=View.VISIBLE){
			view_step_one.setVisibility(View.VISIBLE);
		}
		View view_step_two = view.findViewById(R.id.view_step_two);
		if(view_step_two.getVisibility()!=View.GONE){
			view_step_two.setVisibility(View.GONE);
		}

		//스탬 1 메시지 색상번경 : 정차 후 사용해 주세요.
		TextView tv_steo_one_msg = view.findViewById(R.id.tv_step_one_msg);
		String msg_step_one = tv_steo_one_msg.getText().toString();
		SpannableString sapnnable_msg_step_one = new SpannableString(msg_step_one);

		String focus_msg_step_one = "정차 후 사용해 주세요.";
		int start_msg_step_one = msg_step_one.indexOf(focus_msg_step_one);
		int end_msg_step_one = start_msg_step_one + focus_msg_step_one.length();

		sapnnable_msg_step_one.setSpan(new ForegroundColorSpan(Color.parseColor("#f03508")), start_msg_step_one, end_msg_step_one, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		tv_steo_one_msg.setText(sapnnable_msg_step_one);

		//스탬 2 메시지 색상번경 : WI-FI를 이용해 주세요.
		TextView tv_steo_two_msg = view.findViewById(R.id.tv_step_two_msg);
		String msg_step_two = tv_steo_two_msg.getText().toString();
		SpannableString sapnnable_msg_step_two = new SpannableString(msg_step_two);

		String focus_msg_step_two = "WI-FI를 이용해 주세요.";
		int start_msg_step_two = msg_step_two.indexOf(focus_msg_step_two);
		int end_msg_step_two = start_msg_step_two + focus_msg_step_two.length();

		sapnnable_msg_step_two.setSpan(new ForegroundColorSpan(Color.parseColor("#f03508")), start_msg_step_two, end_msg_step_two, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		tv_steo_two_msg.setText(sapnnable_msg_step_two);

		//--
		TextView tv_step_two_percentage = view.findViewById(R.id.tv_step_two_percentage);
		tv_step_two_percentage.setText("0%");

		//총 다운로드 용량 안내
		long fileDownloadSize = 0L;
		StoryData stories = new StoryData();
		for (StoryItem item : downloadList.story) {

			//--
			if(stories.story==null){
				stories.story = new ArrayList<>();
			}

			stories.story.add(item);
			fileDownloadSize += item.audioFileSize;
		}
		String strSizeInfo = "N/A";
		if( fileDownloadSize > 0L ){
			strSizeInfo = StorageUtil.getFileSize(fileDownloadSize);
		}
		String strFileDownloadSize = String.format("[ 파일 총 용량 %s ]",strSizeInfo);
		TextView tv_step_two_size = view.findViewById(R.id.tv_step_two_size);
		tv_step_two_size.setText( strFileDownloadSize );

		//----
		builder.setView(view);
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);

		ProgressBar progressBar = view.findViewById(R.id.progress);
		//--
		DownloadHelper helper = new DownloadHelper(activity, progressBar, dialog, handler, file_download_type, tv_step_two_percentage);

		//스탭1 확인버튼 처리
		view.findViewById(R.id.btn_step_one_action).setOnClickListener(v ->{
			view_step_one.setVisibility(View.GONE);
			view_step_two.setVisibility(View.VISIBLE);
		});

		view.findViewById(R.id.btn_step_two_action).setOnClickListener(v ->{


			TextView tv = (TextView)v;
			String action = tv.getText().toString();
			//--
			if( activity.getString(R.string.download).equalsIgnoreCase(action) ) {
				//다운로드 시작 (취소문구로 변경)
				tv.setText(activity.getString(R.string.cancel));
				//--
				helper.start(downloadList);

			}else{
				//취소기능
				if(dialog!=null){

					dialog.dismiss();
				}
				if(helper!=null){
					helper.cancel();
				}
			}

		});

		dialog.setOnKeyListener((adialog, keyCode, event) -> {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				return true;
			}
			return false;
		});

		dialog.show();
		//이전 다운로드 helper.start(downloadList)호출위치
		dialog.getWindow().setAttributes(getLayoutParams_V2(activity, dialog));

	}

	private static View makeMessageView(AppCompatActivity activity) {
		View view = LayoutInflater.from(activity).inflate(R.layout.custom_dialog_layout, null);
		return view;
	}

	private static View makeProgressView(AppCompatActivity activity) {
		View view = LayoutInflater.from(activity).inflate(R.layout.custom_dialog_progress_layout, null);
		return view;
	}

	private static WindowManager.LayoutParams getLayoutParams(AppCompatActivity activity, AlertDialog dialog) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int displayWidth = displayMetrics.widthPixels;
		WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
		layoutParams.copyFrom(dialog.getWindow().getAttributes());
		int dialogWindowWidth = (int) (displayWidth * 0.8f);
		layoutParams.width = dialogWindowWidth;
		return layoutParams;
	}

	//------------------------
	private static View makeProgressView_V2(AppCompatActivity activity) {
		View view = LayoutInflater.from(activity).inflate(R.layout.custom_dialog_progress_layout_v2, null);
		return view;
	}
	private static WindowManager.LayoutParams getLayoutParams_V2(AppCompatActivity activity, AlertDialog dialog) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int displayWidth = displayMetrics.widthPixels;
		WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
		layoutParams.copyFrom(dialog.getWindow().getAttributes());
		return layoutParams;
	}

	//==========================================================================
	//@FunctionalInterface
	public interface OnDialogStoryItemClick {
		//void onStoryItemResult(StoryItem item);
		void onStoryItemResult(StoryItem item, boolean confirm);
	}
	//==========================================================================
	public static AlertDialog showTaxiArrival(final AppCompatActivity activity, final String title, final String message, final ArrayList<StoryItem> items, final int[] item_resource, final String buttonRight, final OnDialogStoryItemClick right) {

		//단일인지 복수인지 체크
		if(items!=null && !items.isEmpty() ){

			//AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			//DialogThemeTaxiArrival
			AlertDialog.Builder builder = new AlertDialog.Builder(activity,R.style.DialogThemeTaxiArrival);
			View view = makeTaxiArrival(activity);

			builder.setView(view);
			AlertDialog dialog = builder.create();
			dialog.setCanceledOnTouchOutside(false);
			//추가
			dialog.setCancelable(false);


			View viewMulti = view.findViewById(R.id.view_multi);
			View viewSingle = view.findViewById(R.id.view_single);
			//--
			if( items.size()>1){

				if(viewMulti.getVisibility()!=View.VISIBLE){
					viewMulti.setVisibility(View.VISIBLE);
				}
				if(viewSingle.getVisibility()!=View.GONE){
					viewSingle.setVisibility(View.GONE);
				}
				//----------

				LinearLayout ll = (LinearLayout) view.findViewById(R.id.view_choice);
				for (int i = 0; i < items.size(); i++) {

					StoryItem item = items.get(i);
					String titleStory = (TextUtils.isEmpty(item.titleKo) ? item.title : item.titleKo);
					View v = LayoutInflater.from(activity).inflate(R.layout.choice_taxi_text_item, null);
					((TextView) v.findViewById(R.id.title)).setText(titleStory);
					((TextView) v.findViewById(R.id.title)).setContentDescription(titleStory);

					v.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (right != null) {
								//right.onStoryItemResult(item);
								right.onStoryItemResult(item,true);
							}
							dialog.dismiss();
						}
					});

					v.setTag(i);
					ll.addView(v);
				}

				//--
				view.findViewById(R.id.btn_close).setOnClickListener(v ->{
					//--
					dialog.dismiss();
				});

			}else{
				if(viewMulti.getVisibility()!=View.GONE){
					viewMulti.setVisibility(View.GONE);
				}
				if(viewSingle.getVisibility()!=View.VISIBLE){
					viewSingle.setVisibility(View.VISIBLE);
				}
				//----------
				//--
				StoryItem item = items.get(0);

				TextView tv_msg_upper = view.findViewById(R.id.tv_msg_upper);
				String titleStory = (TextUtils.isEmpty(item.titleKo) ? item.title : item.titleKo);
				String msg_upper = titleStory + "\n"+"에 도착했습니다";
				tv_msg_upper.setText(msg_upper);

				//-------------------------------------------------------------------------------------------

				TextView tv_msg_lower = view.findViewById(R.id.tv_msg_lower);
				int nSecondColor = Color.parseColor("#ffffd543");
				CountDownTimer timer = new CountDownTimer(5000,1000) {
					@Override
					public void onTick(long millisUntilFinished) {
						//--
						int nSecond = (int)(millisUntilFinished / 1000) +1;
						String strFocus = nSecond+"초 후";
						String msg = strFocus+" 이야기를 재생합니다.";
						SpannableString sapnnable_msg = new SpannableString(msg);

						int start_focus = msg.indexOf(strFocus);
						int end_focus = start_focus + strFocus.length();

						//--
						sapnnable_msg.setSpan(new ForegroundColorSpan(nSecondColor), start_focus, end_focus, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						tv_msg_lower.setText(sapnnable_msg);
					}

					@Override
					public void onFinish() {
						String msg = "Done";
						tv_msg_lower.setText("");

						if (right != null) {
							//right.onStoryItemResult(item);
							right.onStoryItemResult(item,true);
						}
						dialog.dismiss();
					}

				};
				// 타이며개시
				timer.start();

				//....
				view.findViewById(R.id.layout_left).setOnClickListener(v->{
					if(timer!=null){
						timer.cancel();
					}


					if (right != null) {
						//right.onStoryItemResult(item);
						right.onStoryItemResult(item,true);
					}


					dialog.dismiss();
				});

				view.findViewById(R.id.layout_right).setOnClickListener(v->{
					if(timer!=null){
						timer.cancel();
					}

					//단일목록 취소 콜백

					if (right != null) {
						//right.onStoryItemResult(item);
						right.onStoryItemResult(item,false);
					}


					dialog.dismiss();
				});

			}

			dialog.show();
			dialog.getWindow().setAttributes(getLayoutParamsTaxiArrival_V2(activity, dialog));
			return dialog;

		}else{
			return null;
		}

	}

	private static View makeTaxiArrival(AppCompatActivity activity) {
		View view = LayoutInflater.from(activity).inflate(R.layout.custom_dialog_taxi_arrival_layout, null);
		return view;
	}

	private static WindowManager.LayoutParams getLayoutParamsTaxiArrival_V2(AppCompatActivity activity, AlertDialog dialog) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);



		WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
		layoutParams.copyFrom(dialog.getWindow().getAttributes());


		//가득채우기
		/*
		int displayWidth = displayMetrics.widthPixels;
		int displayHeight = displayMetrics.heightPixels;
		layoutParams.width = displayWidth;
		layoutParams.height = displayHeight;
		*/

		/*
		int dialogWindowWidth = (int) (displayWidth * 1.0f);
		layoutParams.width = dialogWindowWidth;
		*/
		/*
		int dialogWindowHeight = (int) (displayHeight * 0.8f); //높이는 80퍼센트만 사용하자
		layoutParams.height = dialogWindowHeight;
		*/
		return layoutParams;
	}
}
