package kto.smarttour.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.webkit.ValueCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.scottyab.rootbeer.RootBeer;
import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.Common;
import kto.smarttour.common.consts.URLS;
import kto.smarttour.common.utils.CCLUUIDHelper;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.NetworkUtil;
import kto.smarttour.common.utils.PreferenceUtils;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.SystemUtils;
import kto.smarttour.databinding.ActivityMainBinding;
import kto.smarttour.db.StampDBManager;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.geo.LocationService;
import kto.smarttour.location.CurrentLocation;
import kto.smarttour.network.ApiService;
import kto.smarttour.network.response.IntroImageData;
import kto.smarttour.network.response.NoticeData;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.service.UnCatchTaskService;
import kto.smarttour.ui.player.PlayListManager;
import kto.smarttour.ui.player.Player;
import kto.smarttour.ui.taxi.TaxiMainActivity;
import kto.smarttour.webview.clients.OdiiWebChromeClient;
import kto.smarttour.webview.clients.OdiiWebViewClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static kto.smarttour.webview.javascript.OdiiInterface.JAVASCRIPT_PREFIX;

public class MainActivity extends BaseActivity implements CurrentLocation.OnLocationListener {

	private ActivityMainBinding mBind;
	private Activity activity;
	private CurrentLocation currentLocation;
	private boolean isFirstResume = true;

	private SystemReceiver systemReceiver;

	private OdiiWebChromeClient odiiWebChromeClient;

	private boolean introSkip = false;
	private NoticeUtils checkUtils;
	private NoticeUtils noticeUtils;
	private RootBeer rootBeer;

	private int colorIntroTint;
    private String onPlayMainWebViewUrl = URLS.URL;

	//안드로이드 13권한 다이얼로그
	//private Dialog dialogRequest_POST_NOTIFICATIONS;

	//Jingle SoundPool
	private SoundPool soundPool;
	private int soundJingle_short;
	private boolean b_ready_soundJingle_short; //로드완료 상태
	private int soundJingle_long;
	private boolean b_ready_soundJingle_long; //로드완료 상태
	private SoundPool createSoundPool(){
		SoundPool sp;
		//반환용 사운드풀 준비
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
// USAGE_MEDIA
// USAGE_GAME
                .setUsage(AudioAttributes.USAGE_GAME)
// CONTENT_TYPE_MUSIC
// CONTENT_TYPE_SPEECH, etc.
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        sp = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                // Stream설정
                .setMaxStreams(2)
                .build();

        return sp;
	}
	private void playJingleSound(int soundJingle_id){

		//도이도 재생기능 중단 (임시기능 호출안함)
		boolean allowPlaySound = false;
		if(!allowPlaySound){
			return;
		}

		if(soundPool!=null){
			soundPool.play(soundJingle_id, 1.0f, 1.0f, 1, 0, 1);
		}
	}
	//floating actions menu , to config about soundpool useage
	private FloatingActionsMenu floatingMenu;
	private HashMap<String,Integer> map_floatingButtons;
	private String createTargetKey_map_floatingButton(String at, String type){
		if(at!=null && type!=null){
			return String.format("%s_%s",at,type);
		}
		return null;
	}
	private void initFloatingMenu(boolean useShow){
		if(useShow){
			if(floatingMenu==null) {
				floatingMenu = (FloatingActionsMenu) findViewById(R.id.floating_action_soundpool);

				map_floatingButtons = new HashMap<>();
				int sizeBtn = floatingMenu.getChildCount();
				for(int i=0; i<sizeBtn; i++){

					View v = floatingMenu.getChildAt(i);
					int id = v.getId();
					String value_sound_at = null;
					String value_sound_type = null;
					switch (id){
						case R.id.floating_action_soundpool_intro_start_short:
							value_sound_at = "start";
							value_sound_type = "short";
							break;
						case R.id.floating_action_soundpool_intro_start_long:
							value_sound_at = "start";
							value_sound_type = "long";
							break;
						case R.id.floating_action_soundpool_intro_end_short:
							value_sound_at = "end";
							value_sound_type = "short";
							break;
						case R.id.floating_action_soundpool_intro_end_long:
							value_sound_at = "end";
							value_sound_type = "long";
							break;
					}
					String key = createTargetKey_map_floatingButton(value_sound_at,value_sound_type);
					if(key!=null){
						map_floatingButtons.put(key,id);
						v.setOnClickListener(listenerFloatingMenuButtonAction);
					}
				}

				floatingMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
					@Override
					public void onMenuExpanded() {

						//활성화 상태 반영 ( 사운드플 재생 설정값 기준 맵핑 )
						String value_sound_at = PreferenceUtils.getPreferenceString(activity,"JINGLE_SOUND_AT"); // "start" , "end"
						String value_sound_type = PreferenceUtils.getPreferenceString(activity,"JINGLE_SOUND_TYPE"); //"short" , "long"
						String targetKey = createTargetKey_map_floatingButton(value_sound_at,value_sound_type);

						if(targetKey!=null){
							for ( String key : map_floatingButtons.keySet() ) {
								int targetViewId = map_floatingButtons.get(key);
								FloatingActionButton floatingActionButton = (FloatingActionButton)findViewById(targetViewId);
								if(targetKey.equals(key)){
									floatingActionButton.setPressed(true);
								}else{
									floatingActionButton.setPressed(false);
								}
							}
						}
					}

					@Override
					public void onMenuCollapsed() {

					}
				});
			}
		}
	}
	private View.OnClickListener listenerFloatingMenuButtonAction = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			int id = v.getId();
			//String key_sound_at = "JINGLE_SOUND_AT";
			String value_sound_at = null;
			//String key_sound_type = "JINGLE_SOUND_TYPE";
			String value_sound_type = null;

			switch (id){
				case R.id.floating_action_soundpool_intro_start_short:
					value_sound_at = "start";
					value_sound_type = "short";
					break;
				case R.id.floating_action_soundpool_intro_start_long:
					value_sound_at = "start";
					value_sound_type = "long";
					break;
				case R.id.floating_action_soundpool_intro_end_short:
					value_sound_at = "end";
					value_sound_type = "short";
					break;
				case R.id.floating_action_soundpool_intro_end_long:
					value_sound_at = "end";
					value_sound_type = "long";
					break;
			}
			if(value_sound_at!=null){
				PreferenceUtils.setPreference(activity, "JINGLE_SOUND_AT", value_sound_at);
			}
			if(value_sound_type!=null){
				PreferenceUtils.setPreference(activity, "JINGLE_SOUND_TYPE", value_sound_type);
			}


			//---------------------------
			if(floatingMenu!=null){
				floatingMenu.collapse();
			}
		}
	};

	//--------------------------------------------------------------------------
	private AudioFinishedReceiver mAudioFinishedReceiver = new AudioFinishedReceiver();
	private class AudioFinishedReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && intent.getAction().equals("AUDIO_FINISHED")) {

				//--
				int tlid = intent.getIntExtra("tlid",-1);
				int slid = intent.getIntExtra("slid",-1);
				if( tlid>=0 && slid>=0 ){
					sendAudioFinished(tlid, slid);
				}

			}
		}

	}

	private void sendAudioFinished(int tlid, int slid) {
		String javascript = "STG_SVR.updateAudioEndStatus('" + tlid + "','" + slid + "');";
		mBind.mainWebView.evaluateJavascript(javascript, new ValueCallback<String>() {
			@Override
			public void onReceiveValue(String value) {

			}
		});
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		String locale = SettingsUtil.getLocale(newBase);
		if (locale != null) {
			newBase = CommonUtils.changeLocaleLanguage2(newBase, locale);
		} else {
			locale = newBase.getResources().getConfiguration().locale.getLanguage();
			switch (locale) {
				case "ko":
					locale = "ko";
					break;
				case "zh":
				case "zh_CN":
				case "zh_TW":
					locale = "zh";
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
			String finalLocale = locale;
			new Handler(Looper.getMainLooper()).post(() -> SettingsUtil.setLocale(this, finalLocale));
			newBase = CommonUtils.changeLocaleLanguage2(newBase, locale);

		}
		super.attachBaseContext(newBase);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        onPlayMainWebViewUrl = String.format(URLS.URL, SettingsUtil.getLocale(this));
        String deepLinkUrl = getDeepLinkInflowUrl(getIntent());
        if(deepLinkUrl != null) {
            introSkip = true;
            onPlayMainWebViewUrl = deepLinkUrl;
        }

		onCreate_SoundPool(false);
		//------------------------------------------------------------------
		//------------------------------------------------------------------
		//jingle soundPool
		if(soundPool==null){

			//사운드풀 재생설정 기본값
			String value_sound_at = PreferenceUtils.getPreferenceString(activity,"JINGLE_SOUND_AT");
			if(value_sound_at==null){
				PreferenceUtils.setPreference(activity, "JINGLE_SOUND_AT", "start"); //"start" , "end"
			}

			String value_sound_type = PreferenceUtils.getPreferenceString(activity,"JINGLE_SOUND_TYPE");
			if(value_sound_type==null){
				PreferenceUtils.setPreference(activity, "JINGLE_SOUND_TYPE", "short"); // "short", "long"
			}


			soundPool = createSoundPool();
			//--
			//sound리소스 로드
			soundJingle_short = soundPool.load(this,R.raw.jingle_short,1);
			soundJingle_long = soundPool.load(this,R.raw.jingle_long,1);
			//SoundPool.OnLoadCompleteListener가 수신 되기전 soundPool.load의 사운드id는 이미 할당됨(soundId != 0)
			//setOnLoadCompleteListener
			soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
				@Override
				public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

					boolean success = false;
					if(status == 0){//success
						success = true;
					}
					if(sampleId == soundJingle_short && success){
						b_ready_soundJingle_short = true;
					}
					if(sampleId == soundJingle_long && success){
						b_ready_soundJingle_long = true;
					}

					//사전할당된 사운드id로 할당여부로 판단하지않고 로드완료로 판단.
					if( b_ready_soundJingle_short && b_ready_soundJingle_long ){
						//------------------------------------------------------------------
						//-- init floating actions menu
						//ui, floating actions menu - soundpool config useage
						initFloatingMenu(false); //설정용 UI출력 여부.
						//------------------------------------------------------------------
						//------------------------------------------------------------------

						onCreate_SoundPool(true); //남은 onCreate동작 수행
					}
					//--
				}
			});
		}

	}

    @Nullable
    private String getDeepLinkInflowUrl(Intent intent) {
        Uri data = intent.getData();
        if(data != null) {
            String path = data.getPath();
            if(path != null && path.contains("inflow")) {
                String ifwId = data.getQueryParameter("ifwId");
                return String.format(URLS.INFLOW_URL, ifwId);
            }
        }
        return null;
    }

    private void onCreate_SoundPool(boolean soundPoolInitialized){
		if(!soundPoolInitialized){
			activity = this;

			OdiiApplication.setWebActivity(this);
			try {
				startService(new Intent(this, UnCatchTaskService.class));
			} catch (Exception e) {
			}

			systemReceiver = new SystemReceiver();

			mBind = DataBindingUtil.setContentView(this, R.layout.activity_main);
			currentLocation = new CurrentLocation(this, this);
		}else{
			Intent intent = getIntent();
			//tint설정 대상 뷰 초기값 숨김
			mBind.ivTypologo.setVisibility(View.INVISIBLE);
			mBind.btnSkipintro.setVisibility(View.INVISIBLE);
			mBind.ivBottomlogo.setVisibility(View.INVISIBLE);

			//택시모드 종료에 의한 MainActivity호출시 introSkip
			if (!introSkip && intent != null) {
				introSkip = intent.getBooleanExtra("introSkip", false);
			}

			//(디버그용) 저장되어있는 택시id값이 있는것으로 택시모드 진입
			if(!introSkip){
				//필요시 주석해제
				//SettingsUtil.setTaxiTtid(activity, Integer.valueOf(3));
				//(양양)
				//SettingsUtil.setTaxiTtid(activity, Integer.valueOf(7));
				//(곡성)
				//SettingsUtil.setTaxiTtid(activity, Integer.valueOf(9));
				//(순천)
				//SettingsUtil.setTaxiTtid(activity, Integer.valueOf(8));
			}

			if (intent != null && intent.getBooleanExtra("notification", false)) {
				introSkip = true;
				Intent playerIntent = new Intent(this, Player.class);
				playerIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(playerIntent);
			}

			if (!NetworkUtil.isNetworkConnected(this)) {
				DialogUtil.showWarning(this, getString(R.string.confirm), getString(R.string.network_error), "", getString(R.string.finish), () -> {

				}, () -> {
					finish();
				});
				return;
			}
			odiiWebChromeClient = new OdiiWebChromeClient(this, mBind.progress);
			mBind.mainWebView.setWebViewClient(new OdiiWebViewClient(this, mBind.progress));
			mBind.mainWebView.setWebChromeClient(odiiWebChromeClient);

			setViewMiniPlayer(mBind.viewMiniPlayer,true); //initHide =true

			IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
			registerReceiver(systemReceiver, intentFilter);

			LocalBroadcastManager.getInstance(this).registerReceiver(mAudioFinishedReceiver, new IntentFilter("AUDIO_FINISHED"));

			rootBeer = new RootBeer(this);

			if (CommonUtils.isRooted(this)) {
				DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.rooted_message), getString(R.string.finish), "", () -> {
					finish();
				}, () -> {

				});
			}
			else if (CommonUtils.isEmulator()) {
				DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.emulator_message), getString(R.string.finish), "", () -> {
					finish();
				}, () -> {

				});
			} else if (!CommonUtils.isKeyChecker(this, "MD5")) {
				DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.Integrity_message), getString(R.string.finish), "", () -> {
					finish();
				}, () -> {

				});
			} else {

				if (CommonUtils.isRooted2(this)) {
					DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.rooted_message), getString(R.string.finish), "", () -> {
						finish();
					}, () -> {

					});
				}else{
					if(Common.ignoreRooting){
						//디버그시 checkUtil_start 주석처리 및 진행함수 직접호출 (추가 앱체크 생략)
						showInAppDisclosureUseLocation();
					}else{
						//릴리즈시 사용 (추가 앱체크)
						checkUtil_start(true);
					}

				}

			}
			clearGeofencingNotification();
		}

		//---------------------------------------------------------------------------------------
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
        KLog.i("MainActivity.onNewIntent", intent.getData());
        String deepLinkUrl = getDeepLinkInflowUrl(intent);
        if(deepLinkUrl != null) {
            mBind.mainWebView.loadUrl(deepLinkUrl);
            return;
        }
		if (intent.getBooleanExtra("notification", false)) {
			Intent playerIntent = new Intent(this, Player.class);
			playerIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(playerIntent);
		} else if (intent.getBooleanExtra("stamp", false)) {
			String url = intent.getStringExtra("url");
			if (!TextUtils.isEmpty(url)) {
				int tlid = intent.getIntExtra("tlid", 0);
				int slid = intent.getIntExtra("slid", 0);
				runOnUiThread(() -> {
					StampDBManager.getInstance(this).updateStampComplete(tlid, slid);
				});

				mBind.mainWebView.loadUrl(url);
			}
		} else if (!TextUtils.isEmpty(intent.getStringExtra("url"))) {
			loadUrl(intent.getStringExtra("url"));
		} else {
			getDynamicLink(intent);
		}

		//추가
		checkFcmData(intent,"onNewIntent");
	}

	/**
	 * 동적 링크 파싱
	 * 관광택시 구분 호출시 사용
	 * @param intent
	 */
	private void getDynamicLink(Intent intent) {
//		FirebaseDynamicLinks.getInstance().getDynamicLink(intent).addOnSuccessListener(this, pendingDynamicLinkData -> {
//			Uri deepLink = null;
//			if (pendingDynamicLinkData != null) {
//				deepLink = pendingDynamicLinkData.getLink();
//			}
//
//			if (deepLink != null) {
//				Uri finalDeepLink = deepLink;
//
//
//				try {
//					String ttid = finalDeepLink.getQueryParameter("ttid");
//					if (!TextUtils.isEmpty(ttid)) {
//
//						int newTtid = Integer.valueOf(ttid);
//						int oldTtid = SettingsUtil.getTaxiTtid(activity);
//
//						//-1 택시모드 qr진입, 기존 다운로드 데이터 초기화됨.
//						//-2 초기화 하지않음
//						if (oldTtid == -1 || newTtid != oldTtid) {
//							if (oldTtid == -2){
//								//nothing to do
//							}else{
//								StoryDbManager.getInstance(activity).clearAll();
//								FileUtils.clearCacheStoryDelete(activity);
//							}
//							SettingsUtil.setTaxiTtid(activity, Integer.valueOf(ttid));
//						}
//
//						stopService(new Intent(activity, PlayerService.class));
//						SystemUtils.setTaxiPlayerServiceEnabled(activity);
//
//						FileUtils.setGlideCacheClear(activity);
//
//						startActivity(new Intent(MainActivity.this, TaxiMainActivity.class));
//						finish();
//					} else {
//						new Handler() {
//							@Override
//							public void handleMessage(Message msg) {
//								mBind.mainWebView.loadUrl(finalDeepLink.toString());
//								super.handleMessage(msg);
//							}
//						}.sendEmptyMessageDelayed(0, 1000);
//					}
//				} catch (Exception e) {
//
//				}
//			}
//		}).addOnFailureListener(this, e -> {
//
//		});
	}

	/**
	 *  FCM 수신데이터 처리
	 *  intent.putExtra("fcmData",fcmData); //Serializable
	 */
	private void checkFcmData(Intent intent, String from){

		if(intent!=null){
			HashMap<String,String> data = (HashMap<String, String>)intent.getSerializableExtra("fcmData");
			if(data!=null){
				//--
				String click_log_url = data.get("click_log_url"); //post방식 피드백
				if(click_log_url!=null){
					if(click_log_url.startsWith("http")){
						sendFeedbackFcm(click_log_url);
					}
				}

				//--
				String link_url = data.get("link_url");
				if(link_url!=null){
					if(link_url.startsWith("http")){
						mBind.mainWebView.loadUrl(link_url);
					}
				}
			}
		}
	}

	private void sendFeedbackFcm(String click_log_url){

		if(click_log_url==null){
			return;
		}
		if(!click_log_url.startsWith("http")){
			return;
		}

		//click_log_url
		ApiService.get().sendFeedbackFcm_click_log_url(click_log_url).enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
			}

			@Override
			public void onFailure(Call<ResponseBody> call, Throwable t) {
			}
		});
	}

	/**
	 * 권한체크전 위치사용에 대한 명시적 인앱공개
	 */

	private void showInAppDisclosureUseLocation(){
		//String appName = getString(R.string.app_name);

		//	기존 - 권한 사용 알림 항상 표시
//		String title = getString(R.string.disclosure_use_location_title);
//		String messageFormat = getString(R.string.disclosure_use_location_message);
//
//		DialogPerUtil.showWarning(this, title, messageFormat, getString(R.string.ok), "", () -> {
//			//권한체크 진행
//			checkPermission();
//
//		}, () -> {
//
//		});

		//	수정 - 권한 사용 알림 제거
		checkPermission();

		FirebaseMessaging.getInstance().setAutoInitEnabled(false);
		FirebaseMessaging.getInstance().getToken().addOnCompleteListener(this, new OnCompleteListener<String>() {
			@Override
			public void onComplete(@NonNull Task<String> task) {
			}
		});
		FirebaseMessaging.getInstance().subscribeToTopic("ALL_ANDROID_PRD"); //ALL_ANDROID_DEV , ALL_ANDROID_PRD , ALL_IOS_DEV, ALL_IOS_PRD
	}
	/**
	 * 권한 체크
	 */
	private void checkPermission() {


		boolean flagCheckPermission = true;
		if(flagCheckPermission){

			//--관한요청
			if(!MainActivity.this.isFinishing()){
				//--관한요청
				//ArrayList<String> resPermission = SystemUtils.checkSelfPermission(this, SystemUtils.getPermissionRequestList());

				String[] resPermission = SystemUtils.getPermissionRequestList();

				ArrayList<String> remainPermission = new ArrayList<>();
				for(String permission:resPermission) {

					if(ActivityCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED){
						remainPermission.add(permission);
					}
				}

				if(remainPermission.size()>0){
					//요청할 권한있음.
					String[] permissionRemained = remainPermission.toArray(new String[remainPermission.size()]);
					ActivityCompat.requestPermissions(MainActivity.this, permissionRemained,4989);
				}else{
					startWeb();
				}

			}

			return;
		}


		//기존 권한요청구조
		if (!SettingsUtil.isCheckPermission(this)) {
			ArrayList<String> resPermission;

			/*
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				//resPermission = SystemUtils.checkSelfPermission(this, SystemUtils.PERMISSION_REQUEST_LIST_Q);
				resPermission = SystemUtils.checkSelfPermission(this, SystemUtils.getPermissionRequestList());
			} else {
				resPermission = SystemUtils.checkSelfPermission(this, SystemUtils.getPermissionRequestList());
			}



			SettingsUtil.setCheckPermission(this, true); //전반적인 권한안내 팝업 이력기록
			if (resPermission.size() == 0) {
				startWeb();
			} else {
				startActivityForResult(new Intent(this, PermissionCheckActivity.class), 4589);
			}
			*/



		} else {
			//최초 전반적인 권한안내 팝업에서 사용자가 권한설정관련 동작일 진행했을때
			if (SettingsUtil.isCheckPermissionDialogAgree(this)) {
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					//ACCESS_BACKGROUND_LOCATION 활성화 여부 체크
//					String permissions[] = {Manifest.permission.ACCESS_BACKGROUND_LOCATION};
//					ArrayList<String> resPermission = SystemUtils.checkSelfPermission(this, permissions);
//					if (resPermission.size() == 0) {
//						startWeb();
//						return;
//					} else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
//
//						//위치권한 설정에서 돌아온 후 결과체크 ACCESS_BACKGROUND_LOCATION 허용여부
//						DialogPerUtil.showWarning(this, getString(R.string.permission_content_location_alltime_title), getString(R.string.permission_content_location_alltime) , getString(R.string.settings), getString(R.string.cancel), () -> {
//							//안드로이드 10이상 에서 한번더 사용자에게 권한 안내 및 설정요청 ( 그에대한 결과는 사용자가 무엇을 선택하든 진행 시켜주자 requestCode 4589)
//							String[] permissionList = {Manifest.permission.ACCESS_BACKGROUND_LOCATION};
//							ActivityCompat.requestPermissions(this, permissionList, 4589);
//
//						}, () -> {
//							//추가적인 설정요청도 거절하면 그냥 진행
//							startWeb();
//						});
//					} else {
//
//						//안드로이드 10이상 에서 한번더 사용자에게 권한 안내 및 설정요청 ( 그에대한 결과는 사용자가 무엇을 선택하든 진행 시켜주자 requestCode 4589)
//						String[] permissionList = {Manifest.permission.ACCESS_BACKGROUND_LOCATION};
//						ActivityCompat.requestPermissions(this, permissionList, 4589);
//
//					}
					startWeb();
				}else{
					startWeb();
				}

			}else{
				//최초 전반적인 권한안내 팝업에서 사용자가 권한설정관련을 처음부터 거절 했을때
				startWeb();
			}

		}

	}

	/**
	 * 권한 요청 결과
	 *
	 * 권한 승인 여부 상관없이 진행
	 * @param requestCode
	 * @param permissions
	 * @param grantResults
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if(requestCode != -1) {      // && (requestCode&0xffff0000) != 0  //java.lang.IllegalArgumentException: Can only use lower 8 bits for requestCode 오류

			switch (requestCode) {
				case 4989:
				{
					//--
					//boolean isDenied_ACCESS_BACKGROUND_LOCATION = false;

//			for (int i = 0; i < permissions.length; i++) {
//				String permission = permissions[i];
//				if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
//					//검출대상 android.permission.ACCESS_BACKGROUND_LOCATION
//					String deniedPermissionName = permissions[i];
//					if(deniedPermissionName.equalsIgnoreCase( Manifest.permission.ACCESS_BACKGROUND_LOCATION )){
//						isDenied_ACCESS_BACKGROUND_LOCATION = true;
//					}
//				}
//			}

					//펜스 백그라운드 권한요청 여부
//			if(isDenied_ACCESS_BACKGROUND_LOCATION){
//				//권한문있음
//				DialogPerUtil.showWarning(this, getString(R.string.permission_content_location_alltime_denied_title), getString(R.string.permission_content_location_alltime_denied), getString(R.string.ok), "", () -> {
//					startWeb();
//				}, () -> {
//					//nothing
//				});
//			}else{
					//권한문제 없음
					startWeb();
//			}

					break;
				}

				case 61540:
					//안드로이드 13 노티권한 요청결과
					if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
						//성공
						//Toast.makeText(MainActivity.this,"가가가가가가가가가가가가가가가가가가",Toast.LENGTH_SHORT).show();
						startWeb_Next();
					} else {
						//Toast.makeText(MainActivity.this,"나나나나나나나나나나나나나나나나나나나나",Toast.LENGTH_SHORT).show();
						//미승인
						startWeb_Next();

					}
					break;

				default:
					super.onRequestPermissionsResult(requestCode, permissions, grantResults);
			}
		}
	}

	// FCM알림 권한 추가체크 (Android 13이상 관련)
	// Declare the launcher at the top of your Activity/Fragment:
	/*
	private final ActivityResultLauncher<String> requestPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
				if (isGranted) {
					// FCM SDK (and your app) can post notifications.
				} else {
					// TODO: Inform user that that your app will not show notifications.
				}
			});

	private void askNotificationPermission() {
		// This is only necessary for API level >= 33 (TIRAMISU)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
					PackageManager.PERMISSION_GRANTED) {
				// FCM SDK (and your app) can post notifications.
			} else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
				// TODO: display an educational UI explaining to the user the features that will be enabled
				//       by them granting the POST_NOTIFICATION permission. This UI should provide the user
				//       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
				//       If the user selects "No thanks," allow the user to continue without notifications.
			} else {
				// Directly ask for the permission
				requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
			}
		}
	}
	*/

	/**
	 * 인트로 비디오 영상 초기화
	 */
	/*
	private void initVideo() {
		try {
			Uri video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.intro_ani);
			mBind.videoIntro.setDataSource(this, video);
			mBind.videoIntro.setLooping(false);
			mBind.videoIntro.setOnCompletionListener((mp) -> {
				mBind.layoutVideo.setVisibility(View.GONE);

				setNoticeNetwork();

				CheckPlay();

			});
			mBind.videoIntro.setOnErrorListener((MediaPlayer mp, int what, int extra) -> {
				mBind.layoutVideo.setVisibility(View.GONE);
				mBind.mainWebView.loadUrl(onPlayMainWebViewUrl);
				return false;
			});
		} catch (Exception e) {
		}
	}
	*/

	private void CheckPlay() {
		try {
			int playIndex = PreferenceUtils.getPreferenceInt(MainActivity.this, "PLAY_INDEX", -1);
			if (playIndex != -1) {
				if (!PlayerService.isPlay()) {
					PlayListManager.getInstance().setPlayIndex(playIndex);
					PlayerService.startActionInit(MainActivity.this);
				}
			}
		} catch (Exception e) {
			LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(PlayerConstants.ACTION_HIDE_MINI_PLAYER));
		}
	}

	/**
	 * 인트로 영상 이후 웹페이지 호출
	 */
	private void startWeb() {

		//-- 안드로이드 13 노티피게이션 권한 추가체크
		boolean notificationEnabled = checkNotificationsEnabled();
		if(notificationEnabled){
			//Toast.makeText(MainActivity.this,"checkNotificationsEnabled",Toast.LENGTH_SHORT).show();
			startWeb_Next();
		}else{
			//권한요청
			request_POST_NOTIFICATIONS();
		}
	}

	private void startWeb_Next(){
		if (introSkip) {
			//mBind.layoutVideo.setVisibility(View.GONE);

			mBind.contIntro.setVisibility(View.GONE);
			mBind.mainWebView.loadUrl(onPlayMainWebViewUrl);
			return;
		}

		try {
			//mBind.videoIntro.prepare((mp) -> mp.start());

			LinkedHashMap<String, String> map = new LinkedHashMap<>();
			//IntroImageData
			ApiService.get().introImages(map).enqueue(new Callback<IntroImageData>() {
				@Override
				public void onResponse(Call<IntroImageData> call, Response<IntroImageData> response) {
					if (response.isSuccessful()) {
						/**
						 * 인트로이미지 체크
						 */
						String imageUrlString = response.body().getImage();
						colorIntroTint = Color.TRANSPARENT;

						initIntro(imageUrlString);
					} else {
						colorIntroTint = Color.TRANSPARENT;
						initIntro(null);
					}
				}

				@Override
				public void onFailure(Call<IntroImageData> call, Throwable t) {
					KLog.i(call.toString());

					colorIntroTint = Color.TRANSPARENT;
					initIntro(null);
				}
			});

		} catch (Exception e) {
			//mBind.layoutVideo.setVisibility(View.GONE);

			//예외시
			mBind.contIntro.setVisibility(View.GONE);
			mBind.mainWebView.loadUrl(onPlayMainWebViewUrl);
		}
	}
	//안드로이드 13관련 노티권한
	private boolean checkNotificationsEnabled(){
		boolean isNoficationEnable = false;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			//안드로이드 13이상
			return NotificationManagerCompat.from(MainActivity.this).areNotificationsEnabled();
		}else {
			//안드로이드 13미만, 권한이 있는것으로 간주한다.
			return true;
		}
	}
	//안드로이드 13관련 노티권한 요청
	private void request_POST_NOTIFICATIONS(){
		/*
		if(!MainActivity.this.isFinishing() && dialogRequest_POST_NOTIFICATIONS!=null && dialogRequest_POST_NOTIFICATIONS.isShowing()){
			dialogRequest_POST_NOTIFICATIONS.dismiss();
			//--관한요청
			if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
				ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS},61540);
			}

		}
		*/

		//--관한요청
		if(!MainActivity.this.isFinishing()){
			//--관한요청
			if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
				ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS},61540);
			}

		}

		/*
		dialogRequest_POST_NOTIFICATIONS = new Dialog(MainActivity.this);
		dialogRequest_POST_NOTIFICATIONS.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialogRequest_POST_NOTIFICATIONS.setContentView(R.layout.easy_dialog_noti_guide);
		if(dialogRequest_POST_NOTIFICATIONS.getWindow()!=null){
			dialogRequest_POST_NOTIFICATIONS.getWindow().setBackgroundDrawable(new ColorDrawable(0));
			dialogRequest_POST_NOTIFICATIONS.setCancelable(false);
			Button btnConfirm = (Button) dialogRequest_POST_NOTIFICATIONS.findViewById(R.id.btn_confirm_post_notification);
			btnConfirm.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(!MainActivity.this.isFinishing() && dialogRequest_POST_NOTIFICATIONS!=null && dialogRequest_POST_NOTIFICATIONS.isShowing()){
						dialogRequest_POST_NOTIFICATIONS.dismiss();
						//--관한요청
						if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
							ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS},61540);
						}

					}
				}
			});
			//--show
			if(!MainActivity.this.isFinishing() && dialogRequest_POST_NOTIFICATIONS!=null && !dialogRequest_POST_NOTIFICATIONS.isShowing() ){
				dialogRequest_POST_NOTIFICATIONS.show();
			}
		}
		*/
	}

	/**
	 * 인트로 이미지 초기화
	 */
	private Drawable GetImage(Context c, String ImageName) {
		return c.getResources().getDrawable(c.getResources().getIdentifier(ImageName, "drawable", c.getPackageName()));
	}
	private int GetImageResId(Context c, String ImageName) {
		return c.getResources().getIdentifier(ImageName, "drawable", c.getPackageName());
	}
	private void initIntro(String imageUrlStr) {
		try {
			mBind.btnSkipintro.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					mBind.ivIntro.clearAnimation();

					playIntroAnimationResult(false);
				}
			});

			//리모트 이미지
			if(imageUrlStr!=null && imageUrlStr.trim().length()>0){
				Glide.with(this).load(imageUrlStr).fitCenter().addListener(listenerIntroLoad).into(mBind.ivIntro);
			}else{
				//로컬 이미지 리소스
				ArrayList<String> arr_drawable_name = new ArrayList<>();
				arr_drawable_name.add("imgintro03_1");
				arr_drawable_name.add("imgintro03");
				arr_drawable_name.add("imgintro04");
				arr_drawable_name.add("imgintro08");
				arr_drawable_name.add("imgintro12_1");
				arr_drawable_name.add("imgintro13");
				arr_drawable_name.add("imgintro15");
				arr_drawable_name.add("imgintro17");
				arr_drawable_name.add("imgintro18");
				arr_drawable_name.add("imgintro19_1");
				arr_drawable_name.add("imgintro19_2");
				arr_drawable_name.add("imgintro19_3");

				Random random = new Random();
				int randomIdx = random.nextInt(arr_drawable_name.size());
				int selectedResId = GetImageResId(this,arr_drawable_name.get(randomIdx));

				Uri imageUri = Uri.parse("android.resource://" + getPackageName() + "/" + selectedResId);
				Glide.with(this).load(imageUri).fitCenter().addListener(listenerIntroLoad).into(mBind.ivIntro);
			}

			/*
			mBind.videoIntro.setDataSource(this, video);
			mBind.videoIntro.setLooping(false);
			mBind.videoIntro.setOnCompletionListener((mp) -> {
				KLog.i("ODII_CHECK", "Intro video complete");
				mBind.layoutVideo.setVisibility(View.GONE);

				setNoticeNetwork();

				CheckPlay();

			});
			mBind.videoIntro.setOnErrorListener((MediaPlayer mp, int what, int extra) -> {
				mBind.layoutVideo.setVisibility(View.GONE);
				mBind.mainWebView.loadUrl(onPlayMainWebViewUrl);
				return false;
			});

			*/
		} catch (Exception e) {
		}
	}
	private void applyIntroTintColor(){
		//tint 적용
		mBind.ivTypologo.setColorFilter(colorIntroTint);
		mBind.btnSkipintro.setColorFilter(colorIntroTint);
		mBind.ivBottomlogo.setColorFilter(colorIntroTint);

		//tint설정 대상 뷰 보임
		mBind.ivTypologo.setVisibility(View.VISIBLE);
		mBind.btnSkipintro.setVisibility(View.VISIBLE);
		mBind.ivBottomlogo.setVisibility(View.VISIBLE);
	}
	private RequestListener listenerIntroLoad = new RequestListener() {
		@Override
		public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
			//tint 적용
			applyIntroTintColor();

			playIvIntroAnimation(false);
			return false;
		}

		@Override
		public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
			//tint 적용
			applyIntroTintColor();
			playIvIntroFadeInAnimation();
			return false;
		}
	};

	private SizeReadyCallback mSizeReadyCallback = new SizeReadyCallback() {
		@Override
		public void onSizeReady(int width, int height) {
		}
	};

	private void playIvIntroFadeInAnimation(){
		AlphaAnimation a_animation = new AlphaAnimation(0.0f,1.0f);
		a_animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				playIvIntroAnimation(true);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
		//a_animation.setFillBefore(false);
		a_animation.setDuration(1000); //1초
		a_animation.setFillAfter(true);

		mBind.ivIntro.startAnimation(a_animation);
		/*
		Runnable delayAnimationRunnable = new Runnable() {
			@Override
			public void run() {
				mBind.ivIntro.startAnimation(a_animation);
			}
		};
		new Handler(getMainLooper()).postDelayed(delayAnimationRunnable,0);
		*/
	}
	private void playIvIntroAnimation(boolean imageSuccess) {

		//--
		if(imageSuccess){
			//이미지 로드 성공,애니메이션 호출
			/*
			TranslateAnimation animation = new TranslateAnimation(
					Animation.ABSOLUTE, -100,
					Animation.ABSOLUTE, -100,
					Animation.RELATIVE_TO_SELF, 1.2f,
					Animation.RELATIVE_TO_PARENT, 0.8f);
			*/

			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int width = size.x;

			TranslateAnimation t_animation = new TranslateAnimation(
					Animation.RELATIVE_TO_SELF, 0,
					Animation.ABSOLUTE, -(mBind.ivIntro.getMeasuredWidth() - width),
					Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_PARENT, 0.0f);

			t_animation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

					//-------------------------------------------------------------
					//-------------------------------------------------------------
					//애니메이션 시작시, 징글 사운드 재생 하는 경우
					//사운드풀 재생 기준값 로드
					String value_sound_at = PreferenceUtils.getPreferenceString(activity,"JINGLE_SOUND_AT");
					if(value_sound_at!=null){
						//"start" , "end"
						if(value_sound_at.equals("start")){

							String value_sound_type = PreferenceUtils.getPreferenceString(activity,"JINGLE_SOUND_TYPE");
							if(value_sound_type!=null){
								// "short", "long"
								if(value_sound_type.equals("short")){
									playJingleSound(soundJingle_short);
								}else if(value_sound_type.equals("long")){
									playJingleSound(soundJingle_long);
								}

							}

						}
					}
					//-------------------------------------------------------------
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					playIntroAnimationResult(true);

				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
			t_animation.setDuration(3000); //3초
			t_animation.setFillAfter(true);

			Runnable delayAnimationRunnable = new Runnable() {
				@Override
				public void run() {
					mBind.ivIntro.startAnimation(t_animation);
				}
			};
			new Handler(getMainLooper()).postDelayed(delayAnimationRunnable,300);

		}else{
			//--
			playIntroAnimationResult(false);

		}
	}

	// 애니메이션 결과 중복호출 방지
	private boolean onceFlag_playIntroAnimationResult = false;
	private void playIntroAnimationResult(boolean imageAnimationSuccess) {
		if(onceFlag_playIntroAnimationResult){
			return;
		}
		onceFlag_playIntroAnimationResult = true;

		//-------------------------------------------------------------
		//-------------------------------------------------------------
		//애니메이션 종료시, 징글 사운드 재생 하는 경우
		//사운드풀 재생 기준값 로드
		String value_sound_at = PreferenceUtils.getPreferenceString(activity,"JINGLE_SOUND_AT");
		if(value_sound_at!=null){
			//"start" , "end"
			if(value_sound_at.equals("end")){

				String value_sound_type = PreferenceUtils.getPreferenceString(activity,"JINGLE_SOUND_TYPE");
				if(value_sound_type!=null){
					// "short", "long"
					if(value_sound_type.equals("short")){
						playJingleSound(soundJingle_short);
					}else if(value_sound_type.equals("long")){
						playJingleSound(soundJingle_long);
					}

				}

			}
		}

		if(floatingMenu!=null){;
			if(floatingMenu.getVisibility()!=View.VISIBLE){
				floatingMenu.setVisibility(View.VISIBLE);
			}
		}
		//-------------------------------------------------------------
		//-------------------------------------------------------------


		//애니메이션이 완료되었거나, 애니매이션이 없을때 다음동작 호출
		setNoticeNetwork();
		CheckPlay();

		mBind.contIntro.setVisibility(View.GONE);
		mBind.mainWebView.loadUrl(onPlayMainWebViewUrl);

		//추가
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			checkFcmData(getIntent(),"MainWebLoad");
		}, 2500);

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (CommonUtils.isRooted(this)) {
			DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.rooted_message), getString(R.string.finish), "", () -> {
				ActivityCompat.finishAffinity(this);
				System.exit(0);
			}, () -> {

			});
			return;
		} else if (CommonUtils.isEmulator()) {
			DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.emulator_message), getString(R.string.finish), "", () -> {
				ActivityCompat.finishAffinity(this);
				System.exit(0);
			}, () -> {

			});
			return;
		} else if (!CommonUtils.isKeyChecker(this, "MD5")) {
			DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.Integrity_message), getString(R.string.finish), "", () -> {
				ActivityCompat.finishAffinity(this);
				System.exit(0);
			}, () -> {

			});
			return;
		}
		//--------------------
		if (!isFirstResume && currentLocation != null) {
			currentLocation.startLocationUpdate();
		} else {
			isFirstResume = false;
		}

		if (SystemUtils.isMyServiceRunning(this, PlayerService.class)) {

			//기존코드
			LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(PlayerConstants.ACTION_SHOW_MINI_PLAYER));

		} else {
			if (StoryDbManager.getInstance(this).getPlayList().story.isEmpty()) {
				LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(PlayerConstants.ACTION_HIDE_MINI_PLAYER));
			}
		}

		if (mBind.mainWebView != null && !TextUtils.isEmpty(mBind.mainWebView.getUrl()) && mBind.mainWebView.getUrl().contains("/story/main")) {
			mBind.mainWebView.loadUrl("javascript:requestFootStampList()");
		}

	}

	@Override
	protected void onPause() {
		super.onPause();

		if (currentLocation != null) {
			currentLocation.stopLocationUpdate();
		}

		if (odiiWebChromeClient != null) odiiWebChromeClient.closeCreateWindowDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(mAudioFinishedReceiver);
			unregisterReceiver(systemReceiver);
			if (!PlayerService.isPlay) {
				PlayerService.startActionStop(this);
			}

			mBind.mainWebView.clearCache(true);
			stopService(new Intent(this, UnCatchTaskService.class));
		} catch (Exception e) {
		}

		OdiiApplication.isGeofenceNotifcationClick = false;
		OdiiApplication.stamp_url = null;
	}

	@Override
	public void onBackPressed() {
		if (mBind.mainWebView != null) {
			if (!mBind.mainWebView.canGoBack()) {
				showQuitDialog();
			} else {
				if (!mBind.mainWebView.getUrl().contains("odii.kr")) {
					mBind.mainWebView.goBack();
				}
				else {
					mBind.mainWebView.getOdiiInterface().backPress();
				}
			}
			return;
		}

		super.onBackPressed();

	}

	@Override
	public void onRecievedLocation(Location location) {
		if (location == null) {

		}
	}

	/**
	 * 재생목록 카운터 갱신 (2021.09.02) 추가
	 */
	public void refreshPlaylistCount() {
		new Handler(Looper.getMainLooper()).post(() -> {
			mBind.mainWebView.loadUrl(JAVASCRIPT_PREFIX + "resPlaylistCount('" + StoryDbManager.getInstance(this).getPlayListCount() + "')");
		});
	}

	/**
	 * 다운로드 카운터 갱신
	 */
	public void refreshDownloadCount() {
		new Handler(Looper.getMainLooper()).post(() -> {
			mBind.mainWebView.loadUrl(JAVASCRIPT_PREFIX + "resStoryDownloadCount('" + StoryDbManager.getInstance(this).getDownloadCount() + "')");
		});
	}

	/**
	 * 보관함 카운터 갱신
	 */
	public void refreshLockerCount() {
		new Handler(Looper.getMainLooper()).post(() -> {
			mBind.mainWebView.loadUrl(JAVASCRIPT_PREFIX + "resStoryLockerCount('" + StoryDbManager.getInstance(this).getStoryCount() + "')");
		});
	}

	/**
	 * 앱 종료
	 */
	public void webHistoryBack() {
		showQuitDialog();
	}

	/**
	 * 웹 URL 호출
	 *
	 * @param url
	 */
	public void loadUrl(String url) {
		new Handler(Looper.getMainLooper()).post(() -> {
			mBind.mainWebView.loadUrl(url);
		});
	}

	/**
	 * 웹 새로고침 호출
	 */
	public void reloadMainWeb() {
		new Handler(Looper.getMainLooper()).post(() -> {
			mBind.mainWebView.reload();
		});
	}

	public void loadStampPageChange() {
		new Handler(Looper.getMainLooper()).post(() -> {
			if (mBind != null && mBind.mainWebView != null && !TextUtils.isEmpty(mBind.mainWebView.getUrl()) && mBind.mainWebView.getUrl().contains("/story/main")) {
				mBind.mainWebView.loadUrl("javascript:requestFootStampList()");
			}
		});
	}

	/**
	 * Show quit dialog.
	 */
	private void showQuitDialog() {
		hideSlidMenu();
		DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.quit), getString(R.string.message_yes), getString(R.string.message_no), () -> {
//			finish();
			ActivityCompat.finishAffinity(this);
			System.exit(0);
		}, () -> {
		});
	}


	/**
	 * 헤드셋 Receiver
	 */
	private class SystemReceiver extends BroadcastReceiver {

		@SuppressLint("StringFormatMatches")
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
				boolean isEarphoneOn = (intent.getIntExtra("state", 0) > 0) ? true : false;

				if (!isEarphoneOn && PlayerService.isPlay) {
					PlayerService.startActionPause(MainActivity.this);
				}
			}
		}
	}

	public void isMiniPlayer() {
		new Handler(Looper.getMainLooper()).post(() -> {
			mBind.mainWebView.loadUrl(JAVASCRIPT_PREFIX + "resMiniPlayer('" + (mBind.viewMiniPlayer.getVisibility() == View.VISIBLE ? true : false) + "')");
		});
	}

	public void hideSlidMenu() {
		new Handler(Looper.getMainLooper()).post(() -> {
			mBind.mainWebView.loadUrl(JAVASCRIPT_PREFIX + "closeRnbMenu()");
		});
	}

	public void showBlock() {
		mBind.viewBlock.setVisibility(View.VISIBLE);
	}

	public void hideBlock() {
		if (mBind.viewBlock.getVisibility() == View.VISIBLE) {
			mBind.viewBlock.setVisibility(View.GONE);
		}
	}

	private NoticeUtils.OnCheckListener checkSeListener = new NoticeUtils.OnCheckListener() {
		@Override
		public void onCheckComplete() {
		}

		@Override
		public void onCheckCompleteOnCreate() {
			showInAppDisclosureUseLocation();//권한체크전 위치사용에 대한 명시적 인앱공개
		}

		@Override
		public void onCheckTerminated() {
			if(noticeUtils!=null){
				noticeUtils.clearWork();
			}
		}
	};
	//앱체크
	private void checkUtil_start(boolean isOnCreate){
		checkUtils = new NoticeUtils(MainActivity.this, checkSeListener);
		checkUtils.start(isOnCreate);
	}

	private void setNoticeNetwork() {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		map.put("os_type", "android");
		map.put("uuid", CCLUUIDHelper.id(getApplicationContext()));
		String language = getString(R.string.language);
		switch (language) {
			case "zh":
				language = "cn1";
				break;
			case "tw":
				language = "cn2";
				break;
			case "ja":
				language = "jp";
				break;
		}
		map.put("lang_code", language);


		ApiService.get().notice(map).enqueue(new Callback<NoticeData>() {
			@Override
			public void onResponse(Call<NoticeData> call, Response<NoticeData> response) {
				if (response.isSuccessful()) {
					/**
					 * 작업공지 > 업데이트 > 튜토리얼 > 재난공지 > 일반공지 > 이벤트 > QR
					 */
					//	테스트
//					ArrayList<StampEventList> arrayList = new ArrayList<>();
//					for (int i = 0 ; i < 4 ; i++) {
//						StampEventList stampEventList = new StampEventList();
//						stampEventList._id = 1 + i;
//						stampEventList.eid = 1 + i;
//						stampEventList.elid = 1 + i;
//						stampEventList.tid = 1 + i;
//						stampEventList.slid = 1 + i;
//						stampEventList.tlid = 1 + i;
//						stampEventList.radius = 25;
//						stampEventList.startDate = 1678028400000L;
//						stampEventList.startDateMillis = 1678028400000L;
//						stampEventList.end_date = 1683298800000L;
//						stampEventList.endDateMillis = 1683298800000L;
//						stampEventList.stamp_v_yn = "N";
//						stampEventList.stamp_p_yn = "N";
//
//						if (i == 0) {
//							//	좌측 하단
//							stampEventList.posY = "35.11206962840675";
//							stampEventList.posX = "126.87690530326879";
//							stampEventList.linkUrl = "https://www.naver.com";
//						}
//						else if (i == 1) {
//							//	좌측 상단
//							stampEventList.posY = "35.11259762649068";
//							stampEventList.posX = "126.87759557898507";
//							stampEventList.linkUrl = "https://www.daum.net";
//						}
//						else if (i == 2) {
//							//	우측 상단
//							stampEventList.posY = "35.112314000833194";
//							stampEventList.posX = "126.87789765950511";
//							stampEventList.linkUrl = "https://www.google.com";
//						}
//						else {
//							//	우측 하단
//							stampEventList.posY = "35.111934940556935";
//							stampEventList.posX = "126.87741557697053";
//							stampEventList.linkUrl = "https://www.nate.com";
//						}
//						arrayList.add(stampEventList);
//					}
//					response.body().setStampEventList(arrayList);
					noticeUtils = new NoticeUtils(MainActivity.this, response.body(), noticeListener);
					noticeUtils.start();
				} else {
					mBind.mainWebView.loadUrl(String.format(URLS.URL, SettingsUtil.getLocale(MainActivity.this)));
				}

			}

			@Override
			public void onFailure(Call<NoticeData> call, Throwable t) {
				KLog.i(call.toString());
				mBind.mainWebView.loadUrl(String.format(URLS.URL, SettingsUtil.getLocale(MainActivity.this)));
			}
		});
	}

	private NoticeUtils.OnNoticeListener noticeListener = new NoticeUtils.OnNoticeListener() {
		@Override
		public void onNoticeComplete() {
			mBind.mainWebView.loadUrl(onPlayMainWebViewUrl);

			if (getIntent().getBooleanExtra("stamp", false)) {
				try {
					OdiiApplication.isGeofenceNotifcationClick = true;
					OdiiApplication.stamp_url = getIntent().getStringExtra("url");
					Uri uri = Uri.parse(OdiiApplication.stamp_url);
					String tlid = uri.getQueryParameter("tlid");
					String slid = uri.getQueryParameter("slid");

					runOnUiThread(() -> StampDBManager.getInstance(activity).updateStampComplete(Integer.parseInt(tlid), Integer.parseInt(slid)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onQRCodeListener(String url) {
			try {
				Uri uri = Uri.parse(url);
				String ttid = uri.getQueryParameter("ttid");

				if (!TextUtils.isEmpty(ttid)) {

					int newTtid = Integer.valueOf(ttid);
					int oldTtid = SettingsUtil.getTaxiTtid(activity);

					//oldTtid
					//-1 기존 다운로드 데이터 초기화
					//-2 기존데이터 초기화하지 않음

					if (oldTtid == -1 || newTtid != oldTtid) {

						if (oldTtid == -2){
							//--
							//nothing to do
						}else{
							StoryDbManager.getInstance(activity).clearAll();
							FileUtils.clearCacheStoryDelete(activity);
						}
						SettingsUtil.setTaxiTtid(activity, Integer.valueOf(ttid));
					}

					stopService(new Intent(activity, PlayerService.class));
					SystemUtils.setTaxiPlayerServiceEnabled(activity);

					FileUtils.setGlideCacheClear(activity);

					startActivity(new Intent(MainActivity.this, TaxiMainActivity.class));
					finish();
				}
			} catch (Exception e) {
			}

			mBind.mainWebView.loadUrl(url);
		}

	};

	private void clearGeofencingNotification() {
		try {
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(LocationService.GEOFENCING_NOTIFICATION_ID);
		} catch (Exception e) {

		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 8775) {
			noticeUtils.nextWork();
		} else if (requestCode == 8776 && resultCode == Activity.RESULT_OK) {
			String url = data.getStringExtra("Event_URL");
			if (!TextUtils.isEmpty(url)) {
				if (url.toLowerCase().startsWith(URLS.BASE_URL)) {
					mBind.mainWebView.loadUrl(url);
				} else {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				}

			}
			noticeUtils.nextWork();
		} else if (requestCode == 8776 && resultCode != Activity.RESULT_CANCELED) {
			noticeUtils.nextWork();
		} else if (requestCode == 1599 && Activity.RESULT_OK == resultCode) {
			mBind.mainWebView.getOdiiInterface().getGPS();
		} else if (requestCode == 4589) {
			startWeb();
		} else if( requestCode == 6154) {
			//추가 - 전반적인 권한안내 팝업에 동의하여 진행하였지만 권한이 부족한경우, 시스템 권한설정에서 돌아올 경우
			startWeb();
		}
		else {
			odiiWebChromeClient.onActivityResult(requestCode, resultCode, data);
		}
	}
}
