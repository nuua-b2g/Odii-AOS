package kto.smarttour.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.scottyab.rootbeer.RootBeer;

import java.util.ArrayList;
import java.util.List;

import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.ImageUtil;
import kto.smarttour.databinding.ActivityStoryDetailBinding;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.view.Topbar;

/**
 * 이야기 상세보기
 */
public class StoryDetailActivity extends BaseActivity implements View.OnClickListener {
	public static final int STORY_LOCKER_MODE = 5;
	public static final int STORY_DOWNLOAD_MODE = 6;
	private ActivityStoryDetailBinding mBind;

	private StoryItem content;
	int type;
	private Activity activity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = this;
		mBind = DataBindingUtil.setContentView(this, R.layout.activity_story_detail);
		mBind.setLifecycleOwner(this);
		makeDumpView();
		((RelativeLayout.LayoutParams) mBind.topbar.getLayoutParams()).setMargins(0, statusBarHeight, 0, 0);

		content = (StoryItem) getIntent().getSerializableExtra("content");

		if (content == null) {
			finish();
		}
		type = getIntent().getIntExtra("type", STORY_LOCKER_MODE);
		mBind.setContent(content);
		mBind.topbar.setTopbarMode(Topbar.TOPBAR_MODE_CLOSE);
		mBind.topbar.setOnTopBarListener(v -> finish());
		mBind.topbar.setLeftBackGround(R.drawable.ic_bt_top_backw);

		if (type == STORY_LOCKER_MODE) {
			ImageUtil.loadImage(mBind.ivThumb, content.thumbnailFilePath, null);
		} else {
			String imageFile = FileUtils.getFileName(this, content.tid, content.thumbnailFilePath);
			ImageUtil.localLoadImage2(mBind.ivThumb, imageFile, null);
		}

		mBind.btnTextSizeChange.setOnClickListener(this);

		mBind.viewScroll.setScrollViewListener((l, t, oldl, oldt) -> {
			if (t >= 3) {
				mBind.dumpView.setBackgroundColor(changeAlpha(Color.parseColor("#696CFF"), 0.8f));
				mBind.topbar.setBackgroundColor(changeAlpha(Color.parseColor("#696CFF"), 0.8f));
			} else {
				mBind.dumpView.setBackgroundColor(changeAlpha(Color.parseColor("#696CFF"), 0.0f));
				mBind.topbar.setBackgroundColor(changeAlpha(Color.parseColor("#696CFF"), 0.0f));
			}
		});

		mBind.btnPlay.setOnClickListener(v -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_story_play");
			List<StoryItem> item = new ArrayList<>();
			item.add(content);
			List<StoryItem> list = StoryDbManager.getInstance(this).getPlayList().story;
			StoryDbManager.getInstance(this).clearPlayList();

			if (type == STORY_LOCKER_MODE) {
				StoryDbManager.getInstance(this).addPlayList("L", item);
			} else {
				StoryDbManager.getInstance(this).addPlayList("D", item);
			}


			StoryDbManager.getInstance(this).addPlayList(list);
			sendPlayInsertCast(item.size());
			Intent playIntent = new Intent(this, PlayerService.class);
			playIntent.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
			PlayerService.isFloating = false;
			startService(playIntent);
		});

		setViewMiniPlayer(mBind.viewMiniPlayer);
	}

	private void makeDumpView() {
		RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams) mBind.dumpView.getLayoutParams();
		rl.height = statusBarHeight;
		mBind.dumpView.setLayoutParams(rl);
	}

	public int changeAlpha(int color, float fraction) {
		int red = Color.red(color);
		int green = Color.green(color);
		int blue = Color.blue(color);
		int alpha = (int) (Color.alpha(color) * fraction);
		return Color.argb(alpha, red, green, blue);
	}

	@Override
	protected void onResume() {
		//RootBeer rootBeer = new RootBeer(this);
		if (CommonUtils.isRooted(this)) {
			DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.rooted_message), getString(R.string.finish), "", () -> {
				ActivityCompat.finishAffinity(this);
				System.exit(0);
			}, () -> {

			});
		} else if (CommonUtils.isEmulator()) {
			DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.emulator_message), getString(R.string.finish), "", () -> {
				ActivityCompat.finishAffinity(this);
				System.exit(0);
			}, () -> {

			});
		} else if (!CommonUtils.isKeyChecker(this, "MD5")) {
			DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.Integrity_message), getString(R.string.finish), "", () -> {
				ActivityCompat.finishAffinity(this);
				System.exit(0);
			}, () -> {

			});
		}

		super.onResume();
		mBind.viewMiniPlayer.checkPlayer();
		AnalyticsInterface.getInstance().screenView(this, "local_story_detail");
	}


	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btn_text_size_change) {
			DialogUtil.showChoiceTextSize(StoryDetailActivity.this, getString(R.string.text_size_change), getString(R.string.cancel), result -> {
				switch (result) {
					case 0:
						AnalyticsInterface.getInstance().logEvent(activity, "library_story_font_size_1x");
						mBind.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
						break;
					case 1:
						AnalyticsInterface.getInstance().logEvent(activity, "library_story_font_size_2x");
						mBind.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
						break;
					case 2:
						AnalyticsInterface.getInstance().logEvent(activity, "library_story_font_size_4x");
						mBind.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
						break;

				}
			});
		}
	}
}
