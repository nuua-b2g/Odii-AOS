package kto.smarttour.webview.javascript;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.socks.library.KLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.URLS;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.CCLUUIDHelper;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.InnerStorageSingleton;
import kto.smarttour.common.utils.NetworkUtil;
import kto.smarttour.common.utils.PermissionUtil;
import kto.smarttour.common.utils.PreferenceUtils;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.StorageUtil;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.location.CurrentLocation;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.MainActivity;
import kto.smarttour.ui.PermissionCheckActivity;
import kto.smarttour.ui.StoryDetailWebActivity;
import kto.smarttour.ui.StoryDownloadActivity;
import kto.smarttour.ui.StoryLockerActivity;
import kto.smarttour.ui.StoryPlayerListActivity;
import kto.smarttour.ui.player.PlayListManager;
import kto.smarttour.webview.OdiiWebView;

/**
 * 웹페이지에서 자바스크트로 자바측 함수를 호출할 클래스 정의.
 *
 * @author Changhyun Jeon
 * @since 2015. 3. 17
 */
@SuppressWarnings("unused")
public class OdiiInterface {

	public static final String PREF_TOKEN = "token";
	public static final String PREF_REGISTERED_TOKEN = "tokenRegistered";

	/**
	 * The activity.
	 */
	private final MainActivity activity;
	/**
	 * 추가 - 메인 이외의 activity
	 */
	private AppCompatActivity subActivity;

	/**
	 * The web view.
	 */
	private final OdiiWebView webView;
	/**
	 * Javascript interface name.
	 */
	public static final String APP_NAME = "STG_APP";

	/**
	 * Instantiates a new smart tour interface.
	 *
	 * @param activity the activity
	 * @param webView  the web view
	 */
	public OdiiInterface(MainActivity activity, OdiiWebView webView) {
		// TODO Auto-generated constructor stub
		this.activity = activity;
		this.webView = webView;
	}

	//추가 - subActivity 전달
	public void setSubActivity(AppCompatActivity subActivity){
		this.subActivity = subActivity;
	}

	/* 앱 버전 확인 */
	@JavascriptInterface
	public void getAppVersion() {

		if(webView!=null){
			webView.post(() -> {
				try {
					String version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
					webView.resAppVersion(version);
				} catch (NameNotFoundException e) {
					webView.resAppVersion("1.0");
				}
			});
		}

	}

	/* 플랫폼 확인 */
	@JavascriptInterface
	public void getPlatform() {

		if(webView!=null){
			webView.post(() -> {
				String os = "ANDROID";
				webView.resPlatform(os);
			});
		}
	}


	/* 현재지역 GPS값을 가져온다 : return */
	@JavascriptInterface
	public void getGPS() {

		if(webView!=null){
			webView.post(() ->{

				JSONObject jsonObject = new JSONObject();
				if (SettingsUtil.isUseGps(activity)) {
					CurrentLocation currentLocation = new CurrentLocation(activity, null);
					Location location = currentLocation.getLastKnownLocation();

					if (location != null) {
						try {
							jsonObject.put("latitude", location.getLatitude());
							jsonObject.put("longitude", location.getLongitude());
							OdiiApplication.setLocation(location);
						} catch (JSONException ignored) {
						}
					} else {
						try {
							jsonObject.put("latitude", 37.566668);
							jsonObject.put("longitude", 126.978371);
						} catch (JSONException ignored) {
						}
					}
				} else {
					try {
						jsonObject.put("latitude", 37.566668);
						jsonObject.put("longitude", 126.978371);
					} catch (JSONException e) {
						e.printStackTrace();
					}

				}

                webView.resGPS(jsonObject.toString());
			});
		}
	}

	/* 언어 변경 */
	@JavascriptInterface
	public void changeLocaleLanguage(String localeLanguage) {

		new Handler(Looper.getMainLooper()).post(() -> {

			if (SettingsUtil.getLocale(activity) != null && !TextUtils.isEmpty(SettingsUtil.getLocale(activity))) {

				if (!SettingsUtil.getLocale(activity).equals(localeLanguage)) {

					//예외처리
					//언어변경시 재생종인 오디오 중지 추가
					PlayerService.startActionStop(activity);
					PreferenceUtils.setPreference(activity, "PLAY_INDEX", 0); //저장값 클리어 강제 지정 0
					PlayListManager.getInstance().setPlayIndex(0);//메모리 클리어
					//------------

					SettingsUtil.setLocale(activity, localeLanguage);
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					activity.startActivity(intent);
				}
			}
		});
	}

	/* 데이터 사용 여부 설정 */
	@JavascriptInterface
	public void setDefaultNetworkUse(boolean flagUseData) {

		new Handler(Looper.getMainLooper()).post(() -> {
			SettingsUtil.setUseDataNetwork(activity, flagUseData);
		});
	}

	/* 데이터 사용 여부 설정 확인 */
	@JavascriptInterface
	public void getDefaultNetworkUse() {

		if(webView!=null){
			webView.post(() ->{
                webView.resDefaultNetworkUse(SettingsUtil.isUseDataNetwork(activity));
			});
		}
	}

	/* 공지사항 및 소식/이벤트 정보 알림 설정 */
	@JavascriptInterface
	public void setDefaultPushUse(boolean flagAlarm) {

		new Handler(Looper.getMainLooper()).post(() -> {
			SettingsUtil.setUseFcm(activity, flagAlarm);
		});
	}

	@JavascriptInterface
	public void getDefaultPushUse() {

		new Handler(Looper.getMainLooper()).post(() -> {

			String token = PreferenceUtils.getPreferenceString(activity, PREF_TOKEN);

			if (TextUtils.isEmpty(token)) {
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        String pushToken = task.getResult();
                        PreferenceUtils.setPreference(activity, PREF_TOKEN, pushToken);
                        PreferenceUtils.setPreference(activity, PREF_REGISTERED_TOKEN, false);

                        if(webView!=null){
                            webView.post(() -> webView.resDefaultPushUse(SettingsUtil.isUseFcm(activity), pushToken));
                        }
                    }
				});
			} else {

				if(webView!=null){
					webView.post(() ->{
                        webView.resDefaultPushUse(SettingsUtil.isUseFcm(activity), token);
					});
				}
			}

		});
	}


	/* 위치정보 사용 여부 설정 */
	@JavascriptInterface
	public void setDefaultGPSUse(boolean flagUseLocation) {

		new Handler(Looper.getMainLooper()).post(() -> {
			SettingsUtil.setUseGps(activity, flagUseLocation);
		});
	}

	/* 위치정보 사용 여부 설정 확인 */
	@JavascriptInterface
	public void getDefaultGPSUse() {

		if(webView!=null){
			webView.post(() ->{
                webView.resDefaultGPSUse(SettingsUtil.isUseGps(activity));
			});
		}

	}

	/* 미니플레이어 표시 유무 */
	@JavascriptInterface
	public void isMiniPlayer() {

		new Handler(Looper.getMainLooper()).post(() -> {
			((MainActivity) activity).isMiniPlayer();
		});

	}

	/* 미니플레이어 숨기기 */
	@JavascriptInterface
	public void hideMiniPlayer() {
		new Handler(Looper.getMainLooper()).post(() -> {
			LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(PlayerConstants.ACTION_HIDE_MINI_PLAYER));
		});
	}

	/* 미니플레이어 표시 */
	@JavascriptInterface
	public void showMiniPlayer() {
		new Handler(Looper.getMainLooper()).post(() -> {
			LocalBroadcastManager.getInstance(activity).sendBroadcast(new Intent(PlayerConstants.ACTION_SHOW_MINI_PLAYER));
		});
	}

	/* 미니플레이어 숨기기 */
	@JavascriptInterface
	public void hideMiniPlayerCloseBtn() {
		//KLog.i("TEST_LOG", "JavascriptInterface hideMiniPlayerCloseBtn");
	}

	/* 미니플레이어 표시 */
	@JavascriptInterface
	public void showMiniPlayerCloseBtn() {
		//KLog.i("TEST_LOG", "JavascriptInterface showMiniPlayerCloseBtn");
	}

	/* 보관함 목록 화면 호출 */
	@JavascriptInterface
	public void goStoryLocker() {
		new Handler(Looper.getMainLooper()).post(() -> {
			((MainActivity) activity).hideSlidMenu();
			Intent intent = new Intent(activity, StoryLockerActivity.class);
			activity.startActivity(intent);
		});
	}

	/* 보관함 목록 카운트 */
	@JavascriptInterface
	public void getStoryLockerCount() {

		if(webView!=null){
			webView.post(() ->{
				int count = StoryDbManager.getInstance(activity).getStoryCount();
                webView.resStoryLockerCount(count);
			});
		}
	}

	/* 인터페이스 추가 2021.09.01 - 재생목록 이동( 웹 to 앱 ) */
	@JavascriptInterface
	public void goPlaylist() {

		new Handler(Looper.getMainLooper()).post(() -> {
			int count = StoryDbManager.getInstance(activity).getPlayListCount();
			if(count > 0){
				((MainActivity) activity).hideSlidMenu();
				Intent intent = new Intent(activity, StoryPlayerListActivity.class);
				//intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(intent);
			}else{
				//메시지 출력
				Intent intent = new Intent(PlayerConstants.ACTION_SIMPLE_TOAST);
				intent.putExtra("message", activity.getString(R.string.msg_play_list_empty) );
				LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
			}
		});

	}

	/* 인터페이스 추가 2021.09.01 - 재생목록 카운트 요청( 웹 to 앱  -> 최종 웹으로 결과 전달) */
	@JavascriptInterface
	public void getPlaylistCount() {

		if(webView!=null){
			webView.post(() -> {
				int count = StoryDbManager.getInstance(activity).getPlayListCount();
                webView.resPlaylistCount(count);
			});
		}

		/* evaluateJavascript방식
		String javascript = "STG_SVR.resPlaylistCount('" + 3 + "')";
		webView.evaluateJavascript(javascript, new ValueCallback<String>() {
			@Override
			public void onReceiveValue(String s) {
			}
		});
		*/

	}

	/* 다운로드 목록 화면 호출 */
	@JavascriptInterface
	public void goStoryDownload() {

		new Handler(Looper.getMainLooper()).post(() -> {
			((MainActivity) activity).hideSlidMenu();
			Intent intent = new Intent(activity, StoryDownloadActivity.class);
			activity.startActivity(intent);
		});
	}

	/* 다운로드 목록 카운터 */
	@JavascriptInterface
	public void getStoryDownloadCount() {

		if(webView!=null){
			webView.post(() -> {
				int count = StoryDbManager.getInstance(activity).getDownloadCount();
                webView.resStoryDownloadCount(count);
			});
		}
	}

	public static final String ADDPLAY = "ADDPLAY";
	public static final String CHANGEPLAY = "CHANGEPLAY";
	public static final String ADD = "ADD";

	@JavascriptInterface
	public void setAudioList(String audioList, String type) {

		KLog.json("TEST_LOG1", audioList);
		new Handler(Looper.getMainLooper()).post(() -> {

			StoryData data = new StoryData();
			StoryItem[] arrarys = new Gson().fromJson(audioList, StoryItem[].class);
			for (StoryItem item : arrarys) {
				data.story.add(item);
			}

			if (data != null && !data.story.isEmpty()) {
				switch (type) {
					case ADDPLAY:

						List<StoryItem> list = StoryDbManager.getInstance(activity).getPlayList().story;
						StoryDbManager.getInstance(activity).clearPlayList();

						/*
						StoryDbManager.getInstance(activity).addPlayList("L", data.story);
						StoryDbManager.getInstance(activity).addPlayList(list);
						*/
						StoryDbManager.getInstance(activity).addPlayList("L", data.story);
						StoryDbManager.getInstance(activity).addPlayList(list);



						Intent intent = new Intent(activity, PlayerService.class);
						PlayerService.isFloating = false;

						intent.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
						activity.startService(intent);
						((MainActivity) activity).sendPlayInsertCast(data.story.size());
						break;
					case CHANGEPLAY:
						StoryDbManager.getInstance(activity).clearPlayList();

						StoryDbManager.getInstance(activity).addPlayList("L", data.story);
						Intent playIntent = new Intent(activity, PlayerService.class);

						playIntent.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
						PlayerService.isFloating = false;

						activity.startService(playIntent);
						((MainActivity) activity).sendPlayInsertCast(data.story.size());
						break;
					case ADD:
						List<StoryItem> list1 = StoryDbManager.getInstance(activity).getPlayList().story;
						StoryDbManager.getInstance(activity).clearPlayList();

						/*
						StoryDbManager.getInstance(activity).addPlayList("L", data.story);
						StoryDbManager.getInstance(activity).addPlayList(list1);
						*/
						StoryDbManager.getInstance(activity).addPlayList("L", data.story);
						StoryDbManager.getInstance(activity).addPlayList(list1);


						((MainActivity) activity).sendPlayInsertCast(data.story.size());
						break;
				}
			}

		});

	}

	/* 보관함 목록에 스토리 담기 */
	@JavascriptInterface
	public void setStoryListInLocker(String storyList) {

		new Handler(Looper.getMainLooper()).post(() -> {

			StoryData data = new StoryData();
			StoryItem[] arrarys = new Gson().fromJson(storyList, StoryItem[].class);
			for (StoryItem item : arrarys) {
				data.story.add(item);
			}
			Intent intent = new Intent(activity, StoryLockerActivity.class);
			intent.putExtra("type", StoryLockerActivity.FOLDER_SELECT_TYPE);
			InnerStorageSingleton.getSingleton().setData(data);
			activity.startActivity(intent);

		});

	}

	/* 오디오 다운로드 */
	@JavascriptInterface
	public void setStoryDownload(String storyList) {

		new Handler(Looper.getMainLooper()).post(() -> {

			//KLog.json("TEST_LOG", storyList);
			if (!NetworkUtil.isNetworkConnected(activity)) { // 인터넷 미접속
				DialogUtil.showWarning(activity, R.string.ok, R.string.network_error, R.string.ok);
			} else if (NetworkUtil.isWifiConnected(activity) && NetworkUtil.isNetworkConnected(activity)) { // 와이파이 접속
				getDownlaod(storyList);
			} else if (SettingsUtil.isUseDataNetwork(activity) && !NetworkUtil.isWifiConnected(activity)) { //데이터 사용O 와이파이 접속X
				DialogUtil.showWarning(activity, R.string.ok, R.string.network_use_message, R.string.ok, R.string.cancel, () -> {
					//				SettingsUtil.setUseDataNetwork(activity, true);
					getDownlaod(storyList);
				}, () -> {

				});
			} else if (!SettingsUtil.isUseDataNetwork(activity) && !NetworkUtil.isWifiConnected(activity)) { //데이터 사용X 와이파이 접속X
				DialogUtil.showWarning(activity, R.string.ok, R.string.network_use_message2, R.string.ok, R.string.cancel, () -> {
					//				SettingsUtil.setUseDataNetwork(activity, true);
					getDownlaod(storyList);
				}, () -> {

				});
			}

		});

	}

	private void getDownlaod(String storyList) {

		new Handler(Looper.getMainLooper()).post(() -> {

			long fileDownloadSize = 0L;
			StoryData data = new StoryData();
			StoryItem[] arrarys = new Gson().fromJson(storyList, StoryItem[].class);
			for (StoryItem item : arrarys) {
				data.story.add(item);
				fileDownloadSize += item.audioFileSize;
			}

			//500000000 > 대략 500M
			if ((StorageUtil.GetAvailableInternalMemorySize() - fileDownloadSize) >= 500000000) {
				DialogUtil.showDownLoad(activity, R.string.download, R.string.download_message, data, R.string.cancel, () -> {
					((MainActivity) activity).refreshDownloadCount();
				});
			} else {
				DialogUtil.showWarning(activity, R.string.ok, R.string.storage_limit_error, R.string.ok);
			}

		});

	}

	/**
	 * 추가 2021.09.02 웹에서 웹뷰를 사용하는 엑티비티를 종료하고싶을 떄
	 * goNativeBack
	 */
	@JavascriptInterface
	public void goNativeBack() {

		new Handler(Looper.getMainLooper()).post(() -> {

			if (subActivity != null && subActivity instanceof StoryDetailWebActivity) {
				subActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						subActivity.finish();
					}
				});

			}

		});

	}

	/**
	 * History back.
	 */
	@JavascriptInterface
	public void historyBack() {

		new Handler(Looper.getMainLooper()).post(() -> {

			if (activity instanceof MainActivity) {
				((MainActivity) activity).runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						((MainActivity) activity).webHistoryBack();
					}
				});
			}

		});

	}

	@JavascriptInterface
	public void goBrowser(String url) {

		new Handler(Looper.getMainLooper()).post(() -> {

			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);

		});

	}

	public void backPress() {
		if(webView!=null){
			webView.post(webView::scriptBackPress);
		}
	}

	@JavascriptInterface
	public void getCountry() {

		if(webView!=null){
			webView.post(() -> {
                webView.setCountry(CommonUtils.getCountry(activity));
			});
		}

	}

	@JavascriptInterface
	public void reqUUIDAndAppVersion() {

		if(webView!=null){
			webView.post(() -> {
				String version = "1.0.0";
				try {
					version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
				} catch (NameNotFoundException e) {
					version = "1.0.0";
				}
				String finalVersion = version;
                webView.resUUIDAndAppVersion(CCLUUIDHelper.id(activity), finalVersion);
			});
		}

	}

	@JavascriptInterface
	public void checkGPSPermission() {
		//오디 GPS
		new Handler(Looper.getMainLooper()).post(() -> {

			if (CurrentLocation.checkGps(activity) && SettingsUtil.isUseGps(activity)) {

				if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

				} else {
					Intent gpsPermissionCheckIntent = new Intent(activity, PermissionCheckActivity.class);
					gpsPermissionCheckIntent.putExtra("onlyGps", true);
                    activity.getGpsLauncher()
                            .launch(gpsPermissionCheckIntent);
					activity.overridePendingTransition(0,0);
				}

			} else {
				//GPS설정을 왁인해주세요.
				DialogUtil.showWarning(activity, "", activity.getString(R.string.setting_non_check_location), activity.getString(R.string.cancel), activity.getString(R.string.settings), () -> {
				}, () -> {
					//오디 gps사용설정, 시스템GPS 하나라도 문제가 있는경우.
					//수정 독립적으로 시스템 GPS가 Off인경우는 시스템 설정으로 이동
					if( !CurrentLocation.checkGps(activity) ){
						new Handler().postDelayed((Runnable) () -> {
							activity.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
						},100);
						return;
					}

					// 수정 독립적으로 오디고유설정 해제되어있는경우 웹페이지 이동
					if( !SettingsUtil.isUseGps(activity) ){

						if(webView!=null){
							webView.post(() -> {
								webView.loadUrl(URLS.SETTING_URL);
							});
						}
					}


				});
			}

		});

	}
}