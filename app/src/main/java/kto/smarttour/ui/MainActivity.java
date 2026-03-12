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
import android.webkit.WebResourceRequest;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
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
import com.bumptech.glide.request.target.Target;
import com.google.firebase.messaging.FirebaseMessaging;
import com.socks.library.KLog;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
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
import kto.smarttour.webview.OdiiWebViewCallback;
import kto.smarttour.webview.clients.OdiiWebChromeClient;
import kto.smarttour.webview.clients.OdiiWebViewClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity implements CurrentLocation.OnLocationListener, OdiiWebViewCallback {

    private ActivityMainBinding mBind;
    private Activity activity;
    private CurrentLocation currentLocation;
    private boolean isFirstResume = true;

    private SystemReceiver systemReceiver;

    private OdiiWebChromeClient odiiWebChromeClient;

    private boolean introSkip = false;
    private NoticeUtils noticeUtils;

    private int colorIntroTint;

    private String webViewHomeUrl;
    private String onLoadMainWebViewUrl = URLS.URL;

    // 필드로 등록
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> startIntro()
    );

    // 공지 관련
    private final ActivityResultLauncher<Intent> noticeNextWorkLauncher =
            registerForActivityResult(
                    new StartActivityForResult(),
                    result -> noticeUtils.nextWork()
            );

    // 이벤트 URL 관련
    private final ActivityResultLauncher<Intent> eventUrlLauncher =
            registerForActivityResult(
                    new StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            String url = result.getData().getStringExtra("Event_URL");
                            if (!TextUtils.isEmpty(url)) {
                                if (url.toLowerCase().startsWith(URLS.BASE_URL)) {
                                    mBind.mainWebView.loadUrl(url);
                                } else {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                                }
                            }
                            noticeUtils.nextWork();
                        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                            noticeUtils.nextWork();
                        }
                    }
            );

    // GPS 권한 관련
    private final ActivityResultLauncher<Intent> gpsLauncher =
            registerForActivityResult(
                    new StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            mBind.mainWebView.getOdiiInterface().getGPS();
                        }
                    }
            );

    // 크롬 클라이언트 관련
    private final ActivityResultLauncher<Intent> chromeClientLauncher =
            registerForActivityResult(
                    new StartActivityForResult(),
                    result -> odiiWebChromeClient.onActivityResult(
                            result.getResultCode(), result.getData()
                    )
            );

    //--------------------------------------------------------------------------
    private final AudioFinishedReceiver mAudioFinishedReceiver = new AudioFinishedReceiver();

    @Override
    public void onProgressChanged(int newProgress) {
        mBind.progress.setProgress(newProgress);
    }

    @Override
    public void onPageFinished() {
        mBind.progress.setVisibility(View.GONE);
    }

    @Override
    public void onPageStart() {
        mBind.progress.setVisibility(View.GONE);
    }

    @Nullable
    @Override
    public Boolean shouldOverrideUrlLoading(WebResourceRequest request) {
        Uri uri = request.getUrl();
        if (uri != null && uri.toString().contains("/story/detail")) {
            String lang = uri.getQueryParameter("lang");
            if (lang != null) {
                String currentLang = SettingsUtil.getLocale(this);
                if (!lang.equals(currentLang)) {
                    Toast.makeText(this, R.string.unsupported_language, Toast.LENGTH_SHORT)
                            .show();
                    mBind.mainWebView.loadUrl(webViewHomeUrl);
                    return true;
                }
            }
        }
        return null;
    }

    private class AudioFinishedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && Objects.equals(intent.getAction(), "AUDIO_FINISHED")) {
                //--
                int tlid = intent.getIntExtra("tlid", -1);
                int slid = intent.getIntExtra("slid", -1);
                if (tlid >= 0 && slid >= 0) {
                    mBind.mainWebView.updateAudioEndStatus(tlid, slid);
                }
            }
        }

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        String locale = SettingsUtil.getLocale(newBase);
        if (locale == null) {
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

        }
        newBase = CommonUtils.changeLocaleLanguage2(newBase, locale);
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webViewHomeUrl = String.format(URLS.URL, SettingsUtil.getLocale(this));
        onLoadMainWebViewUrl = webViewHomeUrl;
        String deepLinkUrl = getDeepLinkInflowUrl(getIntent());
        if (deepLinkUrl != null) {
            introSkip = true;
            onLoadMainWebViewUrl = deepLinkUrl;
        }

        activity = this;
        OdiiApplication.setWebActivity(this);
        try {
            startService(new Intent(this, UnCatchTaskService.class));
        } catch (Exception ignored) {
        }
        systemReceiver = new SystemReceiver();
        mBind = DataBindingUtil.setContentView(this, R.layout.activity_main);
        currentLocation = new CurrentLocation(this, this);

        //tint설정 대상 뷰 초기값 숨김
        mBind.ivTypologo.setVisibility(View.INVISIBLE);
        mBind.btnSkipintro.setVisibility(View.INVISIBLE);
        mBind.ivBottomlogo.setVisibility(View.INVISIBLE);
        initializer();
    }

    @Nullable
    private String getDeepLinkInflowUrl(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            String path = data.getPath();
            if (path != null && path.contains("inflow")) {
                String ifwId = data.getQueryParameter("ifwId");
                return String.format(URLS.INFLOW_URL, ifwId);
            }
        }
        return null;
    }


    /***
     * 디버깅용 택시모드 진입
     *
     * <p>
     * //(양양)
     * Integer.valueOf(7)
     * <p>
     * //(곡성)
     * Integer.valueOf(9)
     * <p>
     * //(순천)
     * Integer.valueOf(8)
     */
    private void initializer() {
        Intent intent = getIntent();

        //택시모드 종료에 의한 MainActivity호출시 introSkip
        if (!introSkip && intent != null) {
            introSkip = intent.getBooleanExtra("introSkip", false);
        }

        //(디버그용) 저장되어있는 택시id값이 있는것으로 택시모드 진입 시점
        //SettingsUtil.setTaxiTtid(activity, Integer.valueOf(3));

        if (intent != null && intent.getBooleanExtra("notification", false)) {
            introSkip = true;
            Intent playerIntent = new Intent(this, Player.class);
            playerIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(playerIntent);
        }

        if (!NetworkUtil.isNetworkConnected(this)) {
            DialogUtil.showWarning(
                    this,
                    getString(R.string.confirm),
                    getString(R.string.network_error),
                    "",
                    getString(R.string.finish),
                    () -> {
                    },
                    this::finish
            );
            return;
        }

        odiiWebChromeClient = new OdiiWebChromeClient(this, this);
        mBind.mainWebView.setWebViewClient(new OdiiWebViewClient(this, this));
        mBind.mainWebView.setWebChromeClient(odiiWebChromeClient);

        setViewMiniPlayer(mBind.viewMiniPlayer, true); //initHide =true

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(systemReceiver, intentFilter);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                        mAudioFinishedReceiver, new IntentFilter("AUDIO_FINISHED")
                );

        if (CommonUtils.isRooted(this)) {
            showWarningWithFinish(getString(R.string.rooted_message));
        } else if (CommonUtils.isEmulator()) {
            showWarningWithFinish(getString(R.string.emulator_message));
        } else if (!CommonUtils.isKeyChecker(this, "MD5")) {
            showWarningWithFinish(getString(R.string.Integrity_message));
        } else {
            if (CommonUtils.isRooted2(this)) {
                showWarningWithFinish(getString(R.string.rooted_message));
            } else {
                if (Common.ignoreRooting) {
                    //디버그시 checkUtil_start 주석처리 및 진행함수 직접호출 (추가 앱체크 생략)
                    showInAppDisclosureUseLocation();
                } else {
                    //릴리즈시 사용 (추가 앱체크)
                    checkUtil_start();
                }
            }
        }
        clearGeofencingNotification();
        //---------------------------------------------------------------------------------------
    }

    private void showWarningWithFinish(String message) {
        DialogUtil.showWarning(
                this,
                getString(R.string.finish),
                message,
                getString(R.string.finish),
                "",
                this::finish,
                () -> {

                }
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        KLog.i("MainActivity.onNewIntent", intent.getData());
        String deepLinkUrl = getDeepLinkInflowUrl(intent);
        if (deepLinkUrl != null) {
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
                runOnUiThread(() -> StampDBManager.getInstance(this).updateStampComplete(tlid, slid));
                mBind.mainWebView.loadUrl(url);
            }
        } else if (!TextUtils.isEmpty(intent.getStringExtra("url"))) {
            loadUrl(intent.getStringExtra("url"));
        }

        //추가
        checkFcmData(intent);
    }

    /**
     * FCM 수신데이터 처리
     * intent.putExtra("fcmData",fcmData); //Serializable
     */
    private void checkFcmData(Intent intent) {
        if (intent != null) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> data = (HashMap<String, String>) intent.getSerializableExtra("fcmData");
            if (data != null) {
                //--
                String click_log_url = data.get("click_log_url"); //post방식 피드백
                if (click_log_url != null) {
                    if (click_log_url.startsWith("http")) {
                        sendFeedbackFcm(click_log_url);
                    }
                }

                //--
                String link_url = data.get("link_url");
                if (link_url != null) {
                    if (link_url.startsWith("http")) {
                        mBind.mainWebView.loadUrl(link_url);
                    }
                }
            }
        }
    }

    private void sendFeedbackFcm(String click_log_url) {

        if (click_log_url == null) {
            return;
        }
        if (!click_log_url.startsWith("http")) {
            return;
        }

        //click_log_url
        ApiService.get().sendFeedbackFcm_click_log_url(click_log_url).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(
                    @NonNull Call<ResponseBody> call,
                    @NonNull Response<ResponseBody> response
            ) {
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            }
        });
    }

    /**
     * 권한체크전 위치사용에 대한 명시적 인앱공개
     */

    private void showInAppDisclosureUseLocation() {
        //	수정 - 권한 사용 알림 제거
        requestPermission();

        FirebaseMessaging.getInstance().setAutoInitEnabled(false);
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(this, task -> {
        });
        //ALL_ANDROID_DEV , ALL_ANDROID_PRD , ALL_IOS_DEV, ALL_IOS_PRD
        FirebaseMessaging.getInstance().subscribeToTopic("ALL_ANDROID_PRD");
    }

    /**
     * 권한 체크
     */
    private void requestPermission() {
        //--관한요청
        if (!MainActivity.this.isFinishing()) {
            String[] resPermission = SystemUtils.getPermissionRequestList();
            ArrayList<String> remainPermission = new ArrayList<>();
            for (String permission : resPermission) {

                if (ActivityCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                    remainPermission.add(permission);
                }
            }
            boolean notificationEnabled = checkNotificationsEnabled();
            if (!notificationEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    remainPermission.add(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
            if (remainPermission.isEmpty()) {
                startIntro();
            } else {
                //요청할 권한있음.
                String[] permissionRemained = remainPermission.toArray(new String[0]);
                permissionLauncher.launch(permissionRemained);
            }
        }
    }

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

    private void startIntro() {
        if (introSkip) {
            //mBind.layoutVideo.setVisibility(View.GONE);

            mBind.contIntro.setVisibility(View.GONE);
            mBind.mainWebView.loadUrl(onLoadMainWebViewUrl);
            return;
        }

        try {

            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            //IntroImageData
            ApiService.get().introImages(map).enqueue(new Callback<IntroImageData>() {
                @Override
                public void onResponse(
                        @NonNull Call<IntroImageData> call,
                        @NonNull Response<IntroImageData> response
                ) {
                    if (response.isSuccessful()) {
                        //인트로이미지 체크
                        IntroImageData body = response.body();
                        if (body != null) {
                            String imageUrlString = response.body().getImage();
                            colorIntroTint = Color.TRANSPARENT;
                            initIntro(imageUrlString);
                        } else {
                            colorIntroTint = Color.TRANSPARENT;
                        }
                    } else {
                        colorIntroTint = Color.TRANSPARENT;
                        initIntro(null);
                    }
                }

                @Override
                public void onFailure(
                        @NonNull Call<IntroImageData> call,
                        @NonNull Throwable t
                ) {
                    KLog.i(call.toString());

                    colorIntroTint = Color.TRANSPARENT;
                    initIntro(null);
                }
            });

        } catch (Exception e) {
            //예외시
            mBind.contIntro.setVisibility(View.GONE);
            mBind.mainWebView.loadUrl(onLoadMainWebViewUrl);
        }
    }

    //안드로이드 13관련 노티권한
    private boolean checkNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //안드로이드 13이상
            return NotificationManagerCompat.from(MainActivity.this).areNotificationsEnabled();
        } else {
            //안드로이드 13미만, 권한이 있는것으로 간주한다.
            return true;
        }
    }

    private void initIntro(String imageUrlStr) {
        try {
            mBind.btnSkipintro.setOnClickListener(view -> {
                mBind.ivIntro.clearAnimation();
                playIntroAnimationResult();
            });

            //리모트 이미지
            if (imageUrlStr != null && !imageUrlStr.trim().isEmpty()) {
                Glide.with(this).load(imageUrlStr).fitCenter().addListener(listenerIntroLoad).into(mBind.ivIntro);
            } else {
                //로컬 이미지 리소스
                int[] localDrawableArray = getLocalDrawableArray();
                Random random = new Random();
                int randomIdx = random.nextInt(localDrawableArray.length);
                int selectedResId = localDrawableArray[randomIdx];
                Uri imageUri = Uri.parse("android.resource://" + getPackageName() + "/" + selectedResId);
                Glide.with(this).load(imageUri).fitCenter().addListener(listenerIntroLoad).into(mBind.ivIntro);
            }
        } catch (Exception ignored) {
        }
    }

    @NonNull
    private static int[] getLocalDrawableArray() {
        return new int[]{
                R.drawable.imgintro03_1,
                R.drawable.imgintro03,
                R.drawable.imgintro04,
                R.drawable.imgintro08,
                R.drawable.imgintro12_1,
                R.drawable.imgintro13,
                R.drawable.imgintro15,
                R.drawable.imgintro17,
                R.drawable.imgintro18,
                R.drawable.imgintro19_1,
                R.drawable.imgintro19_2,
                R.drawable.imgintro19_3,
        };
    }

    private void applyIntroTintColor() {
        //tint 적용
        mBind.ivTypologo.setColorFilter(colorIntroTint);
        mBind.btnSkipintro.setColorFilter(colorIntroTint);
        mBind.ivBottomlogo.setColorFilter(colorIntroTint);

        //tint설정 대상 뷰 보임
        mBind.ivTypologo.setVisibility(View.VISIBLE);
        mBind.btnSkipintro.setVisibility(View.VISIBLE);
        mBind.ivBottomlogo.setVisibility(View.VISIBLE);
    }

    private final RequestListener<Drawable> listenerIntroLoad = new RequestListener<Drawable>() {

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
            //tint 적용
            applyIntroTintColor();

            playIvIntroAnimation(false);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            //tint 적용
            applyIntroTintColor();
            playIvIntroFadeInAnimation();
            return false;
        }

    };

    private void playIvIntroFadeInAnimation() {
        AlphaAnimation a_animation = new AlphaAnimation(0.0f, 1.0f);
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
    }

    private void playIvIntroAnimation(boolean imageSuccess) {

        //--
        if (imageSuccess) {
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

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    playIntroAnimationResult();

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            t_animation.setDuration(3000); //3초
            t_animation.setFillAfter(true);

            Runnable delayAnimationRunnable = () -> mBind.ivIntro.startAnimation(t_animation);
            new Handler(getMainLooper()).postDelayed(delayAnimationRunnable, 300);

        } else {
            //--
            playIntroAnimationResult();

        }
    }

    // 애니메이션 결과 중복호출 방지
    private boolean onceFlag_playIntroAnimationResult = false;

    private void playIntroAnimationResult() {
        if (onceFlag_playIntroAnimationResult) {
            return;
        }
        onceFlag_playIntroAnimationResult = true;

        //애니메이션이 완료되었거나, 애니매이션이 없을때 다음동작 호출
        setNoticeNetwork();
        CheckPlay();

        mBind.contIntro.setVisibility(View.GONE);
        mBind.mainWebView.loadUrl(onLoadMainWebViewUrl);

        //추가
        new Handler(Looper.getMainLooper())
                .postDelayed(() -> checkFcmData(getIntent()), 2500);

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

        if (!TextUtils.isEmpty(mBind.mainWebView.getUrl()) && mBind.mainWebView.getUrl().contains("/story/main")) {
            mBind.mainWebView.requestFootStampList();
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
        } catch (Exception ignored) {
        }

        OdiiApplication.isGeofenceNotifcationClick = false;
        OdiiApplication.stamp_url = null;
    }

    @Override
    public void onBackPressed() {
        String currentUrl = mBind.mainWebView.getUrl();
        boolean currentIsHomeUrl = currentUrl != null && webViewHomeUrl.contains(currentUrl);
        if (!mBind.mainWebView.canGoBack() || currentIsHomeUrl) {
            if (onLoadMainWebViewUrl.equals(webViewHomeUrl)) {
                showQuitDialog();
                return;
            } else {
                onLoadMainWebViewUrl = webViewHomeUrl;
            }
        }

        if (mBind.mainWebView.getUrl().contains(URLS.BASE_URL)) {
            mBind.mainWebView.getOdiiInterface().backPress();
        } else {
            mBind.mainWebView.goBack();
        }
    }

    @Override
    public void onRecievedLocation(Location location) {
    }

    /**
     * 다운로드 카운터 갱신
     */
    public void refreshDownloadCount() {
        int count = StoryDbManager.getInstance(this).getDownloadCount();
        new Handler(Looper.getMainLooper())
                .post(() -> mBind.mainWebView.resStoryDownloadCount(count));
    }

    /**
     * 보관함 카운터 갱신
     */
    public void refreshLockerCount() {
        int count = StoryDbManager.getInstance(this).getStoryCount();
        new Handler(Looper.getMainLooper())
                .post(() -> mBind.mainWebView.resStoryLockerCount(count));
    }

    /**
     * 앱 종료
     */
    public void webHistoryBack() {
        showQuitDialog();
    }

    /**
     * 웹 URL 호출
     */
    public void loadUrl(String url) {
        new Handler(Looper.getMainLooper()).post(() -> mBind.mainWebView.loadUrl(url));
    }

    public void loadStampPageChange() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mBind != null && !TextUtils.isEmpty(mBind.mainWebView.getUrl()) && mBind.mainWebView.getUrl().contains("/story/main")) {
                mBind.mainWebView.requestFootStampList();
            }
        });
    }

    public ActivityResultLauncher<Intent> getNoticeNextWorkLauncher() {
        return noticeNextWorkLauncher;
    }

    public ActivityResultLauncher<Intent> getEventUrlLauncher() {
        return eventUrlLauncher;
    }

    public ActivityResultLauncher<Intent> getGpsLauncher() {
        return gpsLauncher;
    }

    public ActivityResultLauncher<Intent> getChromeClientLauncher() {
        return chromeClientLauncher;
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
            if (Objects.equals(intent.getAction(), Intent.ACTION_HEADSET_PLUG)) {
                boolean isEarphoneOn = intent.getIntExtra("state", 0) > 0;

                if (!isEarphoneOn && PlayerService.isPlay) {
                    PlayerService.startActionPause(MainActivity.this);
                }
            }
        }
    }

    public void isMiniPlayer() {
        boolean visible = mBind.viewMiniPlayer.getVisibility() == View.VISIBLE;
        new Handler(Looper.getMainLooper())
                .post(() -> mBind.mainWebView.resMiniPlayer(visible));
    }

    public void hideSlidMenu() {
        new Handler(Looper.getMainLooper())
                .post(() -> mBind.mainWebView.closeRnbMenu());
    }

    public void showBlock() {
        mBind.viewBlock.setVisibility(View.VISIBLE);
    }

    public void hideBlock() {
        if (mBind.viewBlock.getVisibility() == View.VISIBLE) {
            mBind.viewBlock.setVisibility(View.GONE);
        }
    }

    private final NoticeUtils.OnCheckListener checkSeListener = new NoticeUtils.OnCheckListener() {
        @Override
        public void onCheckComplete() {
        }

        @Override
        public void onCheckCompleteOnCreate() {
            showInAppDisclosureUseLocation();//권한체크전 위치사용에 대한 명시적 인앱공개
        }

        @Override
        public void onCheckTerminated() {
            if (noticeUtils != null) {
                noticeUtils.clearWork();
            }
        }
    };

    //앱체크
    private void checkUtil_start() {
        NoticeUtils checkUtils = new NoticeUtils(MainActivity.this, checkSeListener);
        checkUtils.start(true);
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
            public void onResponse(
                    @NonNull Call<NoticeData> call,
                    @NonNull Response<NoticeData> response
            ) {
                if (response.isSuccessful()) {
                    //작업공지 > 업데이트 > 튜토리얼 > 재난공지 > 일반공지 > 이벤트 > QR
                    noticeUtils = new NoticeUtils(MainActivity.this, response.body(), noticeListener);
                    noticeUtils.start();
                } else {
                    mBind.mainWebView.loadUrl(String.format(URLS.URL, SettingsUtil.getLocale(MainActivity.this)));
                }

            }

            @Override
            public void onFailure(
                    @NonNull Call<NoticeData> call,
                    @NonNull Throwable t
            ) {
                KLog.i(call.toString());
                mBind.mainWebView.loadUrl(String.format(URLS.URL, SettingsUtil.getLocale(MainActivity.this)));
            }
        });
    }

    private final NoticeUtils.OnNoticeListener noticeListener = new NoticeUtils.OnNoticeListener() {
        @Override
        public void onNoticeComplete() {
            mBind.mainWebView.loadUrl(onLoadMainWebViewUrl);

            if (getIntent().getBooleanExtra("stamp", false)) {
                try {
                    OdiiApplication.isGeofenceNotifcationClick = true;
                    OdiiApplication.stamp_url = getIntent().getStringExtra("url");
                    Uri uri = Uri.parse(OdiiApplication.stamp_url);
                    String tlid = uri.getQueryParameter("tlid");
                    String slid = uri.getQueryParameter("slid");

                    if (tlid != null && slid != null) {
                        runOnUiThread(() -> StampDBManager.getInstance(activity).updateStampComplete(Integer.parseInt(tlid), Integer.parseInt(slid)));
                    }
                } catch (Exception e) {
                    KLog.e(e);
                }
            }
        }

        @Override
        public void onQRCodeListener(String url) {
            try {
                Uri uri = Uri.parse(url);
                String ttid = uri.getQueryParameter("ttid");

                if (!TextUtils.isEmpty(ttid)) {

                    int newTtid = Integer.parseInt(ttid);
                    int oldTtid = SettingsUtil.getTaxiTtid(activity);

                    //oldTtid
                    //-1 기존 다운로드 데이터 초기화
                    //-2 기존데이터 초기화하지 않음

                    if (oldTtid == -1 || newTtid != oldTtid) {
                        if (oldTtid != -2) {
                            StoryDbManager.getInstance(activity).clearAll();
                            FileUtils.clearCacheStoryDelete(activity);
                        }
                        SettingsUtil.setTaxiTtid(activity, Integer.parseInt(ttid));
                    }

                    stopService(new Intent(activity, PlayerService.class));
                    SystemUtils.setTaxiPlayerServiceEnabled(activity);

                    FileUtils.setGlideCacheClear(activity);

                    startActivity(new Intent(MainActivity.this, TaxiMainActivity.class));
                    finish();
                }
            } catch (Exception ignored) {
            }

            mBind.mainWebView.loadUrl(url);
        }

    };

    private void clearGeofencingNotification() {
        try {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(LocationService.GEOFENCING_NOTIFICATION_ID);
        } catch (Exception ignored) {
        }
    }

}
