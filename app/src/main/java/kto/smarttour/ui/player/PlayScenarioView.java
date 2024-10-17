package kto.smarttour.ui.player;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import kto.smarttour.R;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.ui.StoryDetailActivity;

public class PlayScenarioView implements View.OnTouchListener {
	private final int SHOW = 1;
	private final int HIDE = 3;

	private final ViewGroup.LayoutParams xLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

	private AppCompatActivity xActivity = null;
	private View xView = null;
	private boolean isShow = false;
	private StoryItem storyItem;

	public PlayScenarioView(AppCompatActivity activity) {
		xActivity = activity;
	}

	public void setContent(StoryItem item) {
		storyItem = item;
	}

	public void show() {
		if (isShow) {
			return;
		}
		xHandler.sendEmptyMessage(SHOW);
	}

	public void hide() {
		xHandler.sendEmptyMessage(HIDE);
	}

	public boolean isShow() {
		return isShow;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

	private Handler xHandler = new Handler() {
		private void hide() {
			if (xView != null) {
				xView.setVisibility(View.GONE);
				xView.setFocusable(false);
				xView.setBackgroundDrawable(null);
				xView = null;
			}
			isShow = false;
		}

		private void show() {

			xView = View.inflate(xActivity, R.layout.play_scenario_layout, null);

			((TextView) xView.findViewById(R.id.tv_title)).setText(storyItem.title);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				((TextView) xView.findViewById(R.id.tv_content)).setText(Html.fromHtml(storyItem.audioScript, Html.FROM_HTML_MODE_LEGACY));
			} else {
				((TextView) xView.findViewById(R.id.tv_content)).setText(Html.fromHtml(storyItem.audioScript));
			}

			xView.findViewById(R.id.btn_close).setOnClickListener(v -> hide());
			xView.findViewById(R.id.btn_text_size_change).setOnClickListener(v -> {
				String[] items = {xActivity.getString(R.string.basic), "2X", "4X"};
				DialogUtil.showChoice(xActivity, xActivity.getString(R.string.text_size_change), null, items, xActivity.getString(R.string.cancel), result -> {
					switch (result) {
						case 0:
							AnalyticsInterface.getInstance().logEvent(xActivity, "library_story_font_size_1x");
							((TextView) xView.findViewById(R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
							break;
						case 1:
							AnalyticsInterface.getInstance().logEvent(xActivity, "library_story_font_size_2x");
							((TextView) xView.findViewById(R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
							break;
						case 2:
							AnalyticsInterface.getInstance().logEvent(xActivity, "library_story_font_size_4x");
							((TextView) xView.findViewById(R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
							break;

					}
				});
			});


			xView.setOnTouchListener(PlayScenarioView.this);
			xActivity.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0));
			xActivity.addContentView(xView, xLayoutParams);

			xView.setVisibility(View.VISIBLE);
			xView.findViewById(R.id.tv_title).requestFocusFromTouch();
			isShow = true;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case SHOW: {
					show();
					break;
				}
				case HIDE: {
					hide();
					break;
				}
			}
		}
	};
}
