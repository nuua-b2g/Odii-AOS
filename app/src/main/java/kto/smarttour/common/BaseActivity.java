package kto.smarttour.common;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.db.StampDBManager;
import kto.smarttour.geo.LocationService;
import kto.smarttour.network.response.dao.StampEventList;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.MainActivity;
import kto.smarttour.ui.view.MiniPlayerView;

public class BaseActivity extends AppCompatActivity {
	public int statusBarHeight = 0;
	private MediaPlayerReceiver receiver;
	private EventStampReceiver eventStampReceiver;
	private IntentFilter mediaFilter, eventStampFilter;

	protected int visiblityBaseMiniPlayer = View.GONE;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
        //Android 15 edge-to-edge 비활성화
		WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        //상단바 icon 사라짐 문제 해결
        setStatusBarIconToDark(getWindow());
		String locale = SettingsUtil.getLocale(this);
		if (locale != null) {
			//iOS 긴체 ( zh-Hans ) , 번체 ( zh-Hant )
			//Android 간체 ( zh-rCN ), 번체 ( zh-tTW )
			if(locale.contains("zh")){
				//Log.d("TAG", ">>> onCreate Lang zh to zh-rCN " + "변경함");
				//CommonUtils.changeLocaleLanguage(this, "zh-rCN");

				CommonUtils.changeLocaleLanguage(this, locale); //테스트중 일단 일반동작 처럼 전달.
			}else{

				CommonUtils.changeLocaleLanguage(this, locale);
			}

		} else {
			locale = getResources().getConfiguration().locale.getLanguage();
			switch (locale) {
				case "ko":
					locale = "ko";
					break;
				case "zh":
				case "zh_CN":
				case "zh_TW":
					locale = "zh";;
					break;
				case "tw":
					locale = "tw";
					break;
				case "ja":
					locale = "ja";
					break;
				default:
					locale = "en";
					break;
			}

			SettingsUtil.setLocale(this, locale);
			CommonUtils.changeLocaleLanguage(this, locale);
		}
		getStatusBarHeight();
		receiver = new MediaPlayerReceiver();
		mediaFilter = new IntentFilter();
		mediaFilter.addAction(PlayerConstants.ACTION_SHOW_MINI_PLAYER);
		mediaFilter.addAction(PlayerConstants.ACTION_HIDE_MINI_PLAYER);
		mediaFilter.addAction(PlayerConstants.ACTION_PLAY_INFO);
		mediaFilter.addAction(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE);

		mediaFilter.addAction(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE_EDIT);

		mediaFilter.addAction(PlayerConstants.ACTION_PLAYER_STATUS_PLAY);
		mediaFilter.addAction(PlayerConstants.ACTION_PLAY_INSERT_TOAST);
		mediaFilter.addAction(PlayerConstants.ACTION_PLAYER_FLOAT_MODE);
		mediaFilter.addAction(PlayerConstants.ACTION_STOP);

		//추가
		mediaFilter.addAction(PlayerConstants.ACTION_SIMPLE_TOAST);
		//시크바용 추가
		mediaFilter.addAction(PlayerConstants.GUI_UPDATE_ACTION);

		eventStampReceiver = new EventStampReceiver();
		eventStampFilter = new IntentFilter();
		eventStampFilter.addAction(LocationService.EVENT_STAMP_ACTION);

		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, mediaFilter);

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		LocalBroadcastManager.getInstance(this).registerReceiver(eventStampReceiver, eventStampFilter);

		new Handler(getMainLooper()).post(() -> {
			if(viewMiniPlayer!=null){
				visiblityBaseMiniPlayer = View.VISIBLE;
				visiblityBaseMiniPlayer = viewMiniPlayer.getLastVisibility_bySetPlayerMode();

				viewMiniPlayer.checkPlayer();
			}
		});
		super.onResume();
	}

	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(eventStampReceiver);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}

	public void sendPlayInsertCast(int count) {
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			Intent intent = new Intent(PlayerConstants.ACTION_PLAY_INSERT_TOAST);
			intent.putExtra("count", count);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}, 1000);
	}

    public static void setStatusBarIconToDark(Window window) {
        // icon 어둡게
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else {
            int flags = window.getDecorView().getSystemUiVisibility();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // dark icons
                window.getDecorView().setSystemUiVisibility(flags);
            } else {
                // M 미만: light status bar 지원 안 됨
                window.setStatusBarColor(Color.BLACK); // 검은색 배경
            }
        }
    }

	private void getStatusBarHeight() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");

			if (resourceId > 0) {
				statusBarHeight = getResources().getDimensionPixelSize(resourceId);
			}
		} else {
			statusBarHeight = 0;
		}
	}

	private MiniPlayerView viewMiniPlayer;

	//--
	public void setViewMiniPlayer(MiniPlayerView viewMiniPlayer, boolean initHide) {
		//--
		this.viewMiniPlayer = viewMiniPlayer;

		//doNotShowFirstInit
		if( initHide ){
			viewMiniPlayer.setIsOnceHideEvenIfPlayListExist(true);
			//--
			return;
		}
		if (PlayerService.isPlay()) {

			if (PlayerService.isFloating) {
				viewMiniPlayer.setPlayerMode(2);
			} else {
				viewMiniPlayer.setPlayerMode(1);
			}
		}

	}
	//--
	public void setViewMiniPlayer(MiniPlayerView viewMiniPlayer) {
		setViewMiniPlayer(viewMiniPlayer, false);
	}

	/**
	 * 미니 플레이어 및 화면 UI 갱신용 Receiver
	 */
	private class MediaPlayerReceiver extends BroadcastReceiver {

		@SuppressLint("StringFormatMatches")
		@Override
		public void onReceive(Context context, Intent intent) {

			if (viewMiniPlayer == null) {
				return;
			}

			if (intent.getAction().equals(PlayerConstants.ACTION_SHOW_MINI_PLAYER)) {

				if (PlayerService.isPlay()) {

					//--
					if(PlayerService.checkPlay()){
						if( viewMiniPlayer.getIsOnceHideEvenIfPlayListExist() ){
							viewMiniPlayer.setIsOnceHideEvenIfPlayListExist(false);
						}
					}

					if (PlayerService.isFloating) {
						viewMiniPlayer.setPlayerMode(2);
					} else {
						viewMiniPlayer.setPlayerMode(1);
					}
				}

				viewMiniPlayer.checkPlayer();
			} else if (intent.getAction().equals(PlayerConstants.ACTION_HIDE_MINI_PLAYER)) {

				if(!viewMiniPlayer.isStoryPlayerListActivity()){
					viewMiniPlayer.setVisibility(View.GONE);
				}

			} else if (intent.getAction().equals(PlayerConstants.ACTION_PLAY_INFO)) {
				viewMiniPlayer.checkPlayer();
			} else if (intent.getAction().equals(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE)) {

				viewMiniPlayer.setPlayerMode(1);
				viewMiniPlayer.checkPlayer();

			} else if (intent.getAction().equals(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE_EDIT)) {
				//--
				viewMiniPlayer.setPlayerMode(4);
				viewMiniPlayer.checkPlayer();

			}else if (intent.getAction().equals(PlayerConstants.ACTION_STOP)) {
				viewMiniPlayer.setPlayerMode(4);
			} else if (intent.getAction().equals(PlayerConstants.ACTION_PLAYER_STATUS_PLAY)) {
				viewMiniPlayer.checkPlayer();
			} else if (intent.getAction().equals(PlayerConstants.ACTION_PLAYER_FLOAT_MODE)) {
				runOnUiThread(() -> viewMiniPlayer.setPlayerMode(2));
			} else if (intent.getAction().equals(PlayerConstants.ACTION_PLAY_INSERT_TOAST)) {
				int count = intent.getIntExtra("count", 0);
				if (count == 0) return;

				if (PlayerService.isFloating) {
					PlayerService.isFloating = false;
					viewMiniPlayer.setPlayerMode(1);
					viewMiniPlayer.checkPlayer();

					if(count>1){
						viewMiniPlayer.showToast(String.format(getString(R.string.toast_story_multi_insert_msg, count)));
					}else{
						//단일
						viewMiniPlayer.showToast(String.format(getString(R.string.toast_story_insert_msg, count)));
					}

					viewMiniPlayer.setCloseVisibility(View.VISIBLE);
				} else {
					viewMiniPlayer.setPlayerMode(3);

					if(count>1){
						viewMiniPlayer.showToast(String.format(getString(R.string.toast_story_multi_insert_msg, count)));
					}else{
						//단일
						viewMiniPlayer.showToast(String.format(getString(R.string.toast_story_insert_msg, count)));
					}
				}

			} else if (intent.getAction().equals(PlayerConstants.ACTION_SIMPLE_TOAST)) {

				String msg = intent.getStringExtra("message");

				if (msg == null) return;

				/*
				viewMiniPlayer.setPlayerMode(3);
				viewMiniPlayer.showToast(msg);
				*/
				Toast.makeText(BaseActivity.this, msg, Toast.LENGTH_SHORT).show();

			}
			//--
			else if (intent.getAction().equals(PlayerConstants.GUI_UPDATE_ACTION)) {

				if(viewMiniPlayer!=null){

					if(viewMiniPlayer.getIsSeekTouch()){
						return;
					}

					viewMiniPlayer.apply_MiniPlayer_GUI_UPDATE_ACTION(intent);
				}
				//===================================================================

			}
		}
	}

	private class EventStampReceiver extends BroadcastReceiver {
		//포어그라운드
		@Override
		public void onReceive(Context context, Intent intent) {
			String stampId = intent.getStringExtra("stamp_id");
			String url = null;
			int tlid = 0;
			int slid = 0;
			if(TextUtils.isEmpty(stampId)) {
				return;
			}

			ArrayList<StampEventList> list = StampDBManager.getInstance(context).getList();
			for(StampEventList stamp : list) {
				if(stampId.equals(String.format("%s%s", stamp.tlid, stamp.slid))) {
					url = stamp.linkUrl;
					tlid = stamp.tlid;
					slid = stamp.slid;
				}
			}

			if(TextUtils.isEmpty(url)) {
				return;
			}

			if(!OdiiApplication.isGeofenceNotifcationClick) {
				String finalUrl = url;
				int finalTlid = tlid;
				int finalSlid = slid;
				DialogUtil.showWarning(BaseActivity.this, getString(R.string.stamp_push_title), getString(R.string.stamp_push_content), getString(R.string.stamp_push_go), getString(R.string.cancel), () -> {
					//	바로보기
					runOnUiThread(() -> {
						StampDBManager.getInstance(context).updateStampComplete(finalTlid, finalSlid);

						Intent stampIntent = new Intent(context, MainActivity.class);
						stampIntent.putExtra("stamp", true);
						stampIntent.putExtra("url", finalUrl);
						stampIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
						startActivity(stampIntent);
					});
				}, () -> {
					//	취소
					runOnUiThread(() -> StampDBManager.getInstance(context).updateStampComplete(finalTlid, finalSlid));
				});
			}
		}
	}
}
