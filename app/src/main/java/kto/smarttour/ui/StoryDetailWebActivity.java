package kto.smarttour.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.scottyab.rootbeer.RootBeer;

import java.util.ArrayList;
import java.util.List;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.URLS;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.ImageUtil;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.databinding.ActivityStoryDetailWebBinding;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.location.CurrentLocation;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.view.Topbar;
import kto.smarttour.webview.clients.OdiiWebChromeClient;
import kto.smarttour.webview.clients.OdiiWebViewClient;

/**
 * 이야기 상세보기, 웹뷰사용
 * smarttour_web/story/detail?tid=값&tlid=값&sid=값&slid=값
 * tid
 * tlid
 * sid
 * slid
 */
public class StoryDetailWebActivity extends BaseActivity implements View.OnClickListener {
	public static final int STORY_LOCKER_MODE = 5;
	public static final int STORY_DOWNLOAD_MODE = 6;
	private ActivityStoryDetailWebBinding mBind;

	private StoryItem content;
	int type;
	boolean useLocation;

	private Activity activity;

	private OdiiWebChromeClient odiiWebChromeClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = this;

		mBind = DataBindingUtil.setContentView(this, R.layout.activity_story_detail_web);
		mBind.setLifecycleOwner(this);

		((RelativeLayout.LayoutParams) mBind.detailWebView.getLayoutParams()).setMargins(0, statusBarHeight, 0, 0);

		//onNewIntent
		onNewIntent(getIntent());

		//--
		setViewMiniPlayer(mBind.viewMiniPlayer);
	}

	//onNewIntent
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if(intent!=null){

			content = (StoryItem) intent.getSerializableExtra("content");

			if (content == null) {
				finish();
			}
			type = intent.getIntExtra("type", STORY_LOCKER_MODE);
			useLocation = intent.getBooleanExtra("useLocation", false);

			mBind.setContent(content);

			//웹뷰 초기화
			mBind.detailWebView.getOdiiInterface().setSubActivity(this);//추가

			odiiWebChromeClient = new OdiiWebChromeClient(this, mBind.progress);
			mBind.detailWebView.setWebViewClient(new OdiiWebViewClient(this, mBind.progress));
			mBind.detailWebView.setWebChromeClient(odiiWebChromeClient);

			//로드
			String targetUrl = getStoryDetailWebUrl(content);
			if( targetUrl!=null && targetUrl.length()>0){
				mBind.detailWebView.loadUrl(targetUrl);
			}

		}

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
	}

	//--
	public void showBlock() {
		mBind.viewBlock.setVisibility(View.VISIBLE);
	}

	public void hideBlock() {
		if (mBind.viewBlock.getVisibility() == View.VISIBLE) {
			mBind.viewBlock.setVisibility(View.GONE);
		}
	}

	//--
	private String getStoryDetailWebUrl(StoryItem storyItem){
		/**
		 * 이야기 상세보기, 웹뷰사용
		 * smarttour_web/story/detail?tid=2573&tlid=2573&sid=3028&slid=7294
		 * tid
		 * tlid
		 * sid
		 * slid
		 */

		String url = null;
		if(storyItem!=null){
			String lang = SettingsUtil.getLocale(this);

			if(useLocation){

				Location location = new CurrentLocation(this, null).getLastKnownLocation();

				String latitude = "";
				String longitude = "";
				if (location == null) {

					latitude = String.format("%s",CurrentLocation.DEFAULT_LAT);
					longitude = String.format("%s",CurrentLocation.DEFAULT_LNG);
				} else {
					latitude = String.format("%s",location.getLatitude());
					longitude = String.format("%s",location.getLongitude());
				}
				url = URLS.DETAIL_URL + String.format("?tid=%s&tlid=%s&sid=%s&slid=%s&latitude=%s&longitude=%s&lang=%s&nativePopup=%s", storyItem.tid, storyItem.tlid, storyItem.sid, storyItem.slid, latitude, longitude, lang, "1");

			}else{
				url = URLS.DETAIL_URL + String.format("?tid=%s&tlid=%s&sid=%s&slid=%s&lang=%s&nativePopup=%s", storyItem.tid, storyItem.tlid, storyItem.sid, storyItem.slid, lang, "1");
			}


		}

		return url;

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

	}
}
