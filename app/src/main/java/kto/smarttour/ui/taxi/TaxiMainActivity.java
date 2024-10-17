package kto.smarttour.ui.taxi;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.socks.library.KLog;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import kto.smarttour.R;
import kto.smarttour.binding.BindingAdapters;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.Common;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.LocationUtils;
import kto.smarttour.common.utils.NetworkUtil;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.StorageUtil;
import kto.smarttour.common.utils.SystemUtils;
import kto.smarttour.common.utils.TourTaxiAnalytics;
import kto.smarttour.databinding.TaxiMainActivityBinding;
import kto.smarttour.databinding.TaxiMainActivityV2Binding;
import kto.smarttour.db.StampDBManager;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.download.FileDownLoadType;
import kto.smarttour.geo.GeofenceController;
import kto.smarttour.network.ApiService;
import kto.smarttour.network.response.TaxiData;
import kto.smarttour.network.response.dao.StampEventList;
import kto.smarttour.network.response.dao.TourTaxiInfo;
import kto.smarttour.service.TaxiPlayerService;
import kto.smarttour.ui.MainActivity;
import kto.smarttour.ui.player.TaxiDriverPlayer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaxiMainActivity extends BaseActivity implements View.OnClickListener {
    public static final int TYPE_DOWNLAOD = 0;
    public static final int TYPE_UPGRADE = 1;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private AppCompatActivity activity;
    //private TaxiMainActivityBinding mBind;
    private TaxiMainActivityV2Binding mBind;
    private TourTaxiInfo data;

    private List<StoryItem> updateStoryList;


    private int fileDownLoadType = TYPE_DOWNLAOD;

    //	 위치정보
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private float filtSpeed;
    private float localspeed;

    private static final long UPDATE_INTERVAL = 1000;
    private static final long FASTEST_UPDATE_INTERVAL = 1000;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            //-----------------------------------------------------------------
            //주석처리 -v2레이아웃 로드안함
            /*
            if (!isFinishing()) {
                if (msg.what != 0) {
                    Toast.makeText(activity, "다운로드가 취소되었습니다.", Toast.LENGTH_SHORT).show();
                }

                FileDownLoadType fileType = (FileDownLoadType) msg.obj;
                if (fileType != null && fileType.getCount() > 0) {
                    if (fileType.getFileType() == TYPE_DOWNLAOD) {
                        TourTaxiAnalytics.download(activity, fileType.getLangCode(), fileType.getCount());
                    } else if (fileType.getFileType() == TYPE_UPGRADE) {
                        TourTaxiAnalytics.update(activity, fileType.getLangCode(), fileType.getCount());
                    }
                }

                checkStory();

            }
            */
            //-----------------------------------------------------------------
            super.handleMessage(msg);
        }
    };


    /**
     * Show quit dialog.
     */
    private void showQuitTaxiModeDialog() {
        DialogUtil.showWarning(this, getString(R.string.finish), "Odii서비스로\n이동하겠습니까?", getString(R.string.message_yes), getString(R.string.message_no), () -> {
            //--
            stopService(new Intent(activity, TaxiPlayerService.class));

            //저장되어있는 택시 컨텐트 인덱스 클리어.
            //-1 택시모드 qr재진입시 기존 다운로드 데이터 초기화됨.
            //-2 택시모드 qr재진입시 기존데이터 초기화하지 않도록 추가처리. (반영))
            SettingsUtil.setTaxiTtid(activity, Integer.valueOf(-2));

            SystemUtils.setPlayerServiceEnable(activity);

            FileUtils.setGlideCacheClear(activity);
            Intent intent = new Intent(TaxiMainActivity.this, MainActivity.class);
            intent.putExtra("introSkip", true); //인트로 생략 introSkip 세팅
            startActivity(intent);

            finish();
            //--
        }, () -> {
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;

        //mBind = DataBindingUtil.setContentView(this, R.layout.taxi_main_activity);
        mBind = DataBindingUtil.setContentView(this, R.layout.taxi_main_activity_v2);

        mBind.setLifecycleOwner(this);
        ((RelativeLayout.LayoutParams) mBind.topbar.getLayoutParams()).setMargins(0, statusBarHeight, 0, 0);
        mBind.btnHelp.setOnClickListener(view -> startActivity(new Intent(this, TaxiGuideActivity.class)));

        //택시모드 종료 수행
        mBind.btnQuitTaxiMode.setOnClickListener(view -> {
            showQuitTaxiModeDialog();
        });
        //택시 오디오듣기 버튼
        mBind.btnTaxiStart.setOnClickListener(view -> {

            //디폴트 한국어 모드 진입
            Intent intent = new Intent(this, TaxiStoryListActivity.class);
            intent.putExtra("lang", "ko");
            //TourTaxiInfo객체(Serializable) 천달
            intent.putExtra("TourTaxiInfo",data);
            startActivity(intent);

            //startActivity(new Intent(this, TaxiStoryListActivity.class).putExtra("lang", "ko"));
            //startActivity(new Intent(this, TaxiStoryListActivity.class).putExtra("lang", "en"));
            //startActivity(new Intent(this, TaxiStoryListActivity.class).putExtra("lang", "cn1"));
            //startActivity(new Intent(this, TaxiStoryListActivity.class).putExtra("lang", "jp"));

        });

        mBind.btnPlay.setOnClickListener(this);

        mBind.btnKo.setOnClickListener(this);
        mBind.btnEn.setOnClickListener(this);
        mBind.btnCn.setOnClickListener(this);
        mBind.btnJp.setOnClickListener(this);

        mBind.btnDownKo.setOnClickListener(this);
        mBind.btnDownEn.setOnClickListener(this);
        mBind.btnDownCn.setOnClickListener(this);
        mBind.btnDownJp.setOnClickListener(this);

        mBind.btnUpgradeKo.setOnClickListener(this);
        mBind.btnUpgradeEn.setOnClickListener(this);
        mBind.btnUpgradeCn.setOnClickListener(this);
        mBind.btnUpgradeJp.setOnClickListener(this);

        //-----------------------------------------------------------------
        //주석처리 -v2레이아웃 로드안함
        /*
        if (SettingsUtil.getTaxiFirstRun(this)) {
            SettingsUtil.setTaxiFirstRun(this, false);
            //layout_story_info
            mBind.layoutStoryInfo.setVisibility(View.GONE);
            mBind.layoutInfo.setVisibility(View.VISIBLE);
        } else {
            mBind.layoutStoryInfo.setVisibility(View.VISIBLE);
            if (StoryDbManager.getInstance(this).getPlayList().story.isEmpty()) {
                mBind.tvEmpty.setVisibility(View.VISIBLE);
            } else {
                mBind.tvEmpty.setVisibility(View.GONE);
                mBind.layoutInfo.setVisibility(View.GONE);
            }
        }
        */
        //-----------------------------------------------------------------


        checkRegion();

        setNetwork();

        TourTaxiAnalytics.init(activity);

//        if (SystemUtils.checkGps(activity)) {
//            if (!checkPermissions()) {
//                requestPermissions();
//            }
//        } else {
//            DialogUtil.showWarning(activity, "", "GPS설정을 활성화해주세요.", activity.getString(R.string.finish), activity.getString(R.string.settings), () -> {
//                finish();
//            }, () -> {
//                moveConfigGPS();
//            });
//        }

        createLocationRequest();

        if (!checkPermissions()) {
            requestPermissions();
        }
        else {
            requestLocationUpdates();
        }

        clearStamp();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void requestPermissions() {
        boolean permissionAccessFineLocationApproved = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean permissionAccessFineLocationApproved1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!(permissionAccessFineLocationApproved && permissionAccessFineLocationApproved1)) {
            ActivityCompat.requestPermissions(activity, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private boolean checkPermissions() {
        int fineLocationPermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int fineLocationPermissionState1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fineLocationPermissionState == PackageManager.PERMISSION_GRANTED && fineLocationPermissionState1 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean neverAskAgin = false;
                boolean isRerequest = false;
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                            // 권한요청 다시보지 않기 체크
                            neverAskAgin = true;
                        }
                        isRerequest = true;
                    }

                }

                if (neverAskAgin) {
                    DialogUtil.showWarning(this, "", getString(R.string.permission_rquest_message), getString(R.string.settings), getString(R.string.cancel), () -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                        finish();
                    }, () -> {
                        finish();
                    });
                } else if (isRerequest) {
                    DialogUtil.showWarning(this, "", "GPS권한은 필수 권한입니다.", getString(R.string.ok), getString(R.string.finish), () -> {
                        requestPermissions();
                    }, () -> {
                        finish();
                    });
                }
            }
        }
    }

    private void checkRegion() {
        String newTtid = getIntent().getStringExtra("ttid");
        String oldTtid = String.valueOf(SettingsUtil.getTaxiTtid(activity));

        //-1 택시모드 qr재진입시 기존 다운로드 데이터 초기화됨.
        //-2 택시모드 qr재진입시 기존데이터 초기화하지 않도록 추가처리. (반영)

        if (!TextUtils.isEmpty(newTtid) && !"-1".equals(newTtid) && !"-2".equals(newTtid)) {
            if (!newTtid.equals(oldTtid)) {
                StoryDbManager.getInstance(activity).clearAll();
                FileUtils.clearCacheStoryDelete(activity);
                SettingsUtil.setTaxiTtid(activity, Integer.valueOf(newTtid));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //	액티비티 확인 - 택시 메인
        Common.gCurrentTaxtActivity = Common.ACTIVITY_TAXI_MAIN;

        //-----------------------------------------------------------------
        //주석처리 -v2레이아웃 로드안함
        /*
        if (mBind.layoutStoryInfo.getVisibility() == View.VISIBLE) {
            if (StoryDbManager.getInstance(this).getPlayList().story.isEmpty()) {
                mBind.tvEmpty.setVisibility(View.VISIBLE);
            } else {
                mBind.tvEmpty.setVisibility(View.GONE);
                mBind.layoutInfo.setVisibility(View.GONE);
                runOnUiThread(() -> {
                    int index = SettingsUtil.getTexiLastIndex(activity);
                    KLog.i("TEST_LOG", "LAST STORY PLAY next index: " + index);
                    setRecentViewChange(index);
                });
            }
        }
        */
        //-----------------------------------------------------------------
    }

    @Override
    protected void onPause() {
        super.onPause();

        removeLocationUpdates();
    }

    //<!-- v2 숨김 -->
    private void setRecentViewChange(int index) {

        if (index == -1) {
            index = 0;
        }

        StoryItem story = StoryDbManager.getInstance(this).getPlayList().story.get(index);

        String imageFile = FileUtils.getFileName(this, story);
        Glide.with(this).load(new File(imageFile)).error(R.drawable.ic_img_scrap_dummy).placeholder(R.drawable.ic_img_scrap_dummy).centerCrop().into(mBind.ivThumb);
        mBind.tvStoryTitle.setText(!TextUtils.isEmpty(story.titleKo) ? story.titleKo : story.title);
        BindingAdapters.setDriverLangCodeBox(mBind.tvLang, story.langCode);
        mBind.tvStoryPlayTime.setText(SystemUtils.sec2minsec(story.audioPlayTime));

        int total = StoryDbManager.getInstance(activity).getPlayListCount();
        mBind.progress.setMax(total);

        if (index == 0) {
            mBind.progress.setProgress(index);
        } else {
            mBind.progress.setProgress(index + 1);
        }


    }


    private void setNetwork() {
        mBind.indicator.setVisibility(View.VISIBLE);
        Map<String, String> map = new HashMap<>();
        map.put("ttid", String.valueOf(SettingsUtil.getTaxiTtid(activity)));
        ApiService.get().tourTaxi(map).enqueue(new Callback<TaxiData>() {
            @Override
            public void onResponse(Call<TaxiData> call, Response<TaxiData> response) {
                mBind.indicator.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    mBind.layoutMain.setVisibility(View.VISIBLE);

                    //텍시 API TourTaxiInfo, 내부 추가분류
                    try {
                        data = setLangCodeFilter(response.body().tourTaxiInfo);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                    if (data != null && data.getKo() != null && !data.getKo().isEmpty()) {
                        //-----------------------------------------------------------------
                        //주석처리 -v2레이아웃 로드안함
                        //mBind.tvCount.setText(String.valueOf(data.getKo().size()));
                        //-----------------------------------------------------------------
                    }

                    setupLayout();

                    //-----------------------------------------------------------------
                    //주석처리 -v2레이아웃 로드안함
                    //checkStory();
                    //-----------------------------------------------------------------

                } else {
                    DialogUtil.showWarning(activity, "", getString(R.string.taxi_network_error), getString(R.string.taxi_finish), getString(R.string.taxi_retry), () -> {
                        finish();
                    }, () -> {
                        mBind.indicator.setVisibility(View.VISIBLE);
                        call.clone().enqueue(this);
                    });
                }
            }

            @Override
            public void onFailure(Call<TaxiData> call, Throwable t) {
                mBind.indicator.setVisibility(View.GONE);

                if (StoryDbManager.getInstance(activity).getDownloadList().story.isEmpty()) {
                    DialogUtil.showWarning(activity, "", getString(R.string.taxi_network_error), getString(R.string.taxi_finish), getString(R.string.taxi_retry), () -> {
                        finish();
                    }, () -> {
                        mBind.indicator.setVisibility(View.VISIBLE);
                        call.clone().enqueue(this);
                    });
                } else {

                }
            }
        });
    }

    private void setupLayout() {
        if(data!=null){

            mBind.title.setText(data.title);
        }else{

            mBind.title.setText("관광택시");
        }
    }

    @Override
    public void onClick(View view) {

        if(updateStoryList==null){
            updateStoryList = new ArrayList<>();
        }

        int id = view.getId();
        switch (id) {
            case R.id.btn_ko:
                if (mBind.btnDownKoComplete.getVisibility() == View.VISIBLE) {
                    startActivity(new Intent(this, TaxiStoryListActivity.class).putExtra("lang", "ko"));
                } else {
                    Toast.makeText(activity, "다운로드 후 사용 가능합니다.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_en:
                if (mBind.btnDownEnComplete.getVisibility() == View.VISIBLE) {
                    startActivity(new Intent(this, TaxiStoryListActivity.class).putExtra("lang", "en"));
                } else {
                    Toast.makeText(activity, "다운로드 후 사용 가능합니다.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_cn:
                if (mBind.btnDownCnComplete.getVisibility() == View.VISIBLE) {
                    startActivity(new Intent(this, TaxiStoryListActivity.class).putExtra("lang", "cn1"));
                } else {
                    Toast.makeText(activity, "다운로드 후 사용 가능합니다.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_jp:
                if (mBind.btnDownJpComplete.getVisibility() == View.VISIBLE) {
                    startActivity(new Intent(this, TaxiStoryListActivity.class).putExtra("lang", "jp"));
                } else {
                    Toast.makeText(activity, "다운로드 후 사용 가능합니다.", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btn_down_ko:
                fileDownLoadType = TYPE_DOWNLAOD;
                getDownLoadStory(view, data.getKo());
                break;
            case R.id.btn_down_en:
                fileDownLoadType = TYPE_DOWNLAOD;
                getDownLoadStory(view, data.getEn());
                break;
            case R.id.btn_down_cn:
                fileDownLoadType = TYPE_DOWNLAOD;
                getDownLoadStory(view, data.getCn());
                break;
            case R.id.btn_down_jp:
                fileDownLoadType = TYPE_DOWNLAOD;
                getDownLoadStory(view, data.getJp());
                break;

            case R.id.btn_upgrade_ko:
            case R.id.btn_upgrade_en:
            case R.id.btn_upgrade_cn:
            case R.id.btn_upgrade_jp:
                if (updateStoryList.isEmpty()) {
                    switch (id) {
                        case R.id.btn_upgrade_ko:
                            fileDownLoadType = TYPE_UPGRADE;
                            updateStoryList.addAll(data.getKo());
                            break;
                        case R.id.btn_upgrade_en:
                            fileDownLoadType = TYPE_UPGRADE;
                            updateStoryList.addAll(data.getEn());
                            break;
                        case R.id.btn_upgrade_cn:
                            fileDownLoadType = TYPE_UPGRADE;
                            updateStoryList.addAll(data.getCn());
                            break;
                        case R.id.btn_upgrade_jp:
                            fileDownLoadType = TYPE_UPGRADE;
                            updateStoryList.addAll(data.getJp());
                            break;
                    }
                }

                getDownLoadStory(view, updateStoryList);
                break;
            case R.id.btn_play:
                List<StoryItem> storyItems = StoryDbManager.getInstance(activity).getPlayList().story;
                if (storyItems == null || storyItems.isEmpty()) {
                    return;
                } else {
                    StoryDbManager.getInstance(this).clearPlayList();
                    StoryDbManager.getInstance(this).addPlayList("D", storyItems);
                    startActivity(new Intent(this, TaxiDriverPlayer.class).putExtra("position", SettingsUtil.getTexiLastIndex(activity)));
                }

                break;
        }
    }

    private void getDownLoadStory(View view, List<StoryItem> data) {
        List<StoryItem> storyList;
        switch (view.getId()) {
            case R.id.btn_upgrade_ko:
            case R.id.btn_upgrade_en:
            case R.id.btn_upgrade_cn:
            case R.id.btn_upgrade_jp:
                fileDownLoadType = TYPE_UPGRADE;
                storyList = data;
                break;
            case R.id.btn_down_ko:
            case R.id.btn_down_en:
            case R.id.btn_down_cn:
            case R.id.btn_down_jp:
                fileDownLoadType = TYPE_DOWNLAOD;
                storyList = data;
                break;
            default:
                DialogUtil.showWarning(activity, R.string.taxi_info, R.string.taxi_network_error, R.string.taxi_ok);
                return;
        }

        if (NetworkUtil.isWifiConnected(activity) || NetworkUtil.isNetworkConnected(activity)) {
            DialogUtil.showWarning(activity, R.string.taxi_info, R.string.taxi_network_use_message, R.string.taxi_ok, R.string.taxi_cancel, () -> {
                getDownlaod(storyList);
            }, () -> {

            });
        } else {
            DialogUtil.showWarning(activity, R.string.taxi_info, R.string.taxi_network_error, R.string.taxi_ok);
        }
    }

    private void getDownlaod(List<StoryItem> storyList) {
        long fileDownloadSize = 0L;
        StoryData stories = new StoryData();
        for (StoryItem item : storyList) {

            if(stories.story==null){
                stories.story = new ArrayList<>();
            }

            stories.story.add(item);
            fileDownloadSize += item.audioFileSize;
        }

        //500000000 > 대략 500M
        if ((StorageUtil.GetAvailableInternalMemorySize() - fileDownloadSize) >= 500000000) {
            DialogUtil.showDownLoad(activity, mHandler, fileDownLoadType, R.string.taxi_download, R.string.taxi_download_message, stories, R.string.taxi_cancel, () -> {

            });
        } else {
            DialogUtil.showWarning(activity, R.string.taxi_info, R.string.taxi_storage_limit_error, R.string.taxi_ok);
        }
    }
    //<!-- v2 숨김 -->
    private void checkStory() {

        List<String> langs = Arrays.asList(new String[]{"ko", "en", "cn1", "jp"});

        for (String lang : langs) {
            List<StoryItem> oldStories = StoryDbManager.getInstance(activity).getDownloadList(lang);
            List<StoryItem> newStories = new ArrayList<>();
            switch (lang) {
                case "ko":
                    //예외처리 추가
                    if (data != null && data.getKo() != null) {
                        newStories.addAll(data.getKo());
                    }
                    break;
                case "en":
                    if (data != null && data.getEn() != null) {
                        newStories.addAll(data.getEn());
                    }
                    break;
                case "cn1":
                    if (data != null && data.getCn() != null) {
                        newStories.addAll(data.getCn());
                    }
                    break;
                case "jp":
                    if (data != null && data.getJp() != null) {
                        newStories.addAll(data.getJp());
                    }
                    break;
            }

            if (!newStories.isEmpty()) {
                if (oldStories.isEmpty()) {
                    initDownLoadResource(lang, 3);

                    // 용량 체크
                    long totalFileSize = 0L;
                    for (StoryItem item : newStories) {
                        totalFileSize += Long.valueOf(item.audioFileSize);
                    }
                    switch (lang) {
                        case "ko":
                            mBind.tvKoFileSize.setVisibility(View.VISIBLE);
                            mBind.tvKoFileSize.setText(StorageUtil.getFileSize(totalFileSize));
                            break;
                        case "en":
                            mBind.tvEnFileSize.setVisibility(View.VISIBLE);
                            mBind.tvEnFileSize.setText(StorageUtil.getFileSize(totalFileSize));
                            break;
                        case "cn1":
                            mBind.tvCnFileSize.setVisibility(View.VISIBLE);
                            mBind.tvCnFileSize.setText(StorageUtil.getFileSize(totalFileSize));
                            break;
                        case "jp":
                            mBind.tvJpFileSize.setVisibility(View.VISIBLE);
                            mBind.tvJpFileSize.setText(StorageUtil.getFileSize(totalFileSize));
                            break;
                    }

                } else {
                    //--
                    List<StoryItem> deleteStoryList = compareStoryDeleteList(oldStories, newStories);
                    if (!deleteStoryList.isEmpty()) {
                        StoryDbManager.getInstance(activity).removeDownLoad2(deleteStoryList);
                        FileUtils.removeFile(this, deleteStoryList);
                    }
                    oldStories = StoryDbManager.getInstance(activity).getDownloadList(lang);

                    //--
                    updateStoryList = compareStoryInsertList(oldStories, newStories);

                    if(updateStoryList==null){
                        updateStoryList = new ArrayList<>();
                    }

                    List<StoryItem> updateStoryTempList = compareStoryUpdateList(oldStories, newStories);
                    FileUtils.removeFile(this, updateStoryTempList);
                    updateStoryList.addAll(updateStoryTempList);
                    if (!updateStoryList.isEmpty()) {
                        initDownLoadResource(lang, 1);
                    } else {
                        initDownLoadResource(lang, 2);
                    }

                }
            } else {
                switch (lang) {
                    case "ko":
                        mBind.btnKo.setVisibility(View.GONE);
                        break;
                    case "en":
                        mBind.btnEn.setVisibility(View.GONE);
                        break;
                    case "cn1":
                        mBind.btnCn.setVisibility(View.GONE);
                        break;
                    case "jp":
                        mBind.btnJp.setVisibility(View.GONE);
                        break;
                }
            }

        }

    }

    private void initDownLoadResource(String lang, int type) {
        if (type == 1) {
            switch (lang) {
                case "ko":
                    mBind.btnDownKo.setVisibility(View.GONE);
                    mBind.btnUpgradeKo.setVisibility(View.VISIBLE);
                    mBind.btnDownKoComplete.setVisibility(View.GONE);
                    mBind.tvKoFileSize.setVisibility(View.GONE);
                    break;
                case "en":
                    mBind.btnDownEn.setVisibility(View.GONE);
                    mBind.btnUpgradeEn.setVisibility(View.VISIBLE);
                    mBind.btnDownEnComplete.setVisibility(View.GONE);
                    mBind.tvEnFileSize.setVisibility(View.GONE);
                    break;
                case "cn1":
                    mBind.btnDownCn.setVisibility(View.GONE);
                    mBind.btnUpgradeCn.setVisibility(View.VISIBLE);
                    mBind.btnDownCnComplete.setVisibility(View.GONE);
                    mBind.tvCnFileSize.setVisibility(View.GONE);
                    break;
                case "jp":
                    mBind.btnDownJp.setVisibility(View.GONE);
                    mBind.btnUpgradeJp.setVisibility(View.VISIBLE);
                    mBind.btnDownJpComplete.setVisibility(View.GONE);
                    mBind.tvJpFileSize.setVisibility(View.GONE);
                    break;
            }
        } else if (type == 2) {
            switch (lang) {
                case "ko":
                    mBind.btnDownKo.setVisibility(View.GONE);
                    mBind.btnUpgradeKo.setVisibility(View.GONE);
                    mBind.btnDownKoComplete.setVisibility(View.VISIBLE);
                    mBind.tvKoFileSize.setVisibility(View.GONE);
                    break;
                case "en":
                    mBind.btnDownEn.setVisibility(View.GONE);
                    mBind.btnUpgradeEn.setVisibility(View.GONE);
                    mBind.btnDownEnComplete.setVisibility(View.VISIBLE);
                    mBind.tvEnFileSize.setVisibility(View.GONE);
                    break;
                case "cn1":
                    mBind.btnDownCn.setVisibility(View.GONE);
                    mBind.btnUpgradeCn.setVisibility(View.GONE);
                    mBind.btnDownCnComplete.setVisibility(View.VISIBLE);
                    mBind.tvCnFileSize.setVisibility(View.GONE);
                    break;
                case "jp":
                    mBind.btnDownJp.setVisibility(View.GONE);
                    mBind.btnUpgradeJp.setVisibility(View.GONE);
                    mBind.btnDownJpComplete.setVisibility(View.VISIBLE);
                    mBind.tvJpFileSize.setVisibility(View.GONE);
                    break;
            }
        } else if (type == 3) {
            switch (lang) {
                case "ko":
                    mBind.btnDownKo.setVisibility(View.VISIBLE);
                    mBind.btnUpgradeKo.setVisibility(View.GONE);
                    mBind.btnDownKoComplete.setVisibility(View.GONE);
                    mBind.tvKoFileSize.setVisibility(View.VISIBLE);
                    break;
                case "en":
                    mBind.btnDownEn.setVisibility(View.VISIBLE);
                    mBind.btnUpgradeEn.setVisibility(View.GONE);
                    mBind.btnDownEnComplete.setVisibility(View.GONE);
                    mBind.tvEnFileSize.setVisibility(View.VISIBLE);
                    break;
                case "cn1":
                    mBind.btnDownCn.setVisibility(View.VISIBLE);
                    mBind.btnUpgradeCn.setVisibility(View.GONE);
                    mBind.btnDownCnComplete.setVisibility(View.GONE);
                    mBind.tvCnFileSize.setVisibility(View.VISIBLE);
                    break;
                case "jp":
                    mBind.btnDownJp.setVisibility(View.VISIBLE);
                    mBind.btnUpgradeJp.setVisibility(View.GONE);
                    mBind.btnDownJpComplete.setVisibility(View.GONE);
                    mBind.tvJpFileSize.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    /**
     * 로컬 스토리, 서버 스토리 비교하여 로컬Story 삭제할 목록
     *
     * @param target 로컬 스토로
     * @param source 서버 스토리
     * @return 삭제된 Story
     */
    private List<StoryItem> compareStoryDeleteList(List<StoryItem> target, List<StoryItem> source) {
        ArrayList<StoryItem> tmpArr = new ArrayList<>();
        tmpArr.addAll(target);

        for (StoryItem sourceItem : source) {
            for (StoryItem targetItem : target) {
                if (sourceItem.slid == targetItem.slid) {
                    tmpArr.remove(targetItem);
                }
            }
        }
        return tmpArr;
    }

    /**
     * 로컬 스토리, 서버 스토리 비교하여 로컬Story 업데이트 목록
     *
     * @param target 로컬 스토로
     * @param source 서버 스토리
     * @return 업데이트된 Story
     */
    private List<StoryItem> compareStoryUpdateList(List<StoryItem> target, List<StoryItem> source) {
        ArrayList<StoryItem> retrunArr = new ArrayList<>();

        for (StoryItem sourceItem : source) {
            for (StoryItem targetItem : target) {
                if (sourceItem.slid == targetItem.slid) {
                    if (sourceItem.audioVersion != targetItem.audioVersion) {
                        retrunArr.add(sourceItem);
                    }

                }
            }
        }

        return retrunArr;
    }

    /**
     * 로컬 스토리, 서버 스토리 비교하여 로컬Story 추가해야할 목록
     *
     * @param target 로컬 스토로
     * @param source 서버 스토리
     * @return 업데이트된 Story
     */
    private List<StoryItem> compareStoryInsertList(List<StoryItem> target, List<StoryItem> source) {
        ArrayList<StoryItem> tmpArr = new ArrayList<>();
        tmpArr.addAll(source);
        // targetItem server story
        for (StoryItem targetItem : source) {
            // sourceItem local db story
            for (StoryItem sourceItem : target) {
                if (sourceItem.slid == targetItem.slid) {
                    tmpArr.remove(targetItem);

                    if (sourceItem.thumbnailUpdateDateSec < targetItem.thumbnailUpdateDateSec) {
                        FileUtils.updateThumbnail(activity, sourceItem, targetItem);
                    }
                    StoryDbManager.getInstance(activity).updateStory(sourceItem.slid, targetItem);


                }
            }
        }
        return tmpArr;
    }

    private TourTaxiInfo setLangCodeFilter(TourTaxiInfo tour) throws NullPointerException {
        for (StoryItem story : tour.storyList) {
            if (story.langCode != null && !story.langCode.isEmpty()) {
                String lc = story.langCode.toLowerCase();
                if (lc.equals("ko")) {
                    tour.getKo().add(story);
                } else if (lc.equals("en")) {
                    tour.getEn().add(story);
                } else if (lc.equals("cn1")) {
                    tour.getCn().add(story);
                } else if (lc.equals("jp")) {
                    tour.getJp().add(story);
                }
            }
        }
        return tour;
    }

    private void check() {
        PackageManager pm = getPackageManager();
        ComponentName compName = new ComponentName(this, getPackageName() + ".service.PlayerService");
        //		pm.setComponentEnabledSetting(compName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.);
    }

    // 관광택시 진입시 발도장 기능 제거
    private void clearStamp() {
        activity.runOnUiThread(() -> {
            ArrayList<StampEventList> oldList = StampDBManager.getInstance(activity).getList();
            if (!oldList.isEmpty()) {
                StampDBManager.getInstance(activity).clear();
                //지오팬스
                GeofenceController.getInstance().unregister(oldList);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 4444) {
            if (SystemUtils.checkGps(activity)) {
                if (!checkPermissions()) {
                    requestPermissions();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void moveConfigGPS() {
        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(gpsOptionsIntent, 4444);
    }

    public void requestLocationUpdates() {
        try {
            LocationUtils.setRequestingLocationUpdates(this, true);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            LocationUtils.setRequestingLocationUpdates(this, false);
            e.printStackTrace();
        }
    }

    public void removeLocationUpdates() {
        LocationUtils.setRequestingLocationUpdates(this, false);
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locations = locationResult.getLocations();
            Location location = locations.get(0);

            boolean hasSpeed = false;
            if (location.hasSpeed()) {
                hasSpeed = true;

                localspeed = location.getSpeed() * 3.6f;
                filtSpeed = speedFilter(filtSpeed, localspeed);

                runOnUiThread(() -> {
                    if (filtSpeed >= 10) {
                        //	이동중
                        Common.isTaxiDriving = true;
                    } else {
                        Common.isTaxiDriving = false;
                    }
                });
                //KLog.i("LocationCheckValue", String.format("speed : %s", filtSpeed));
            }

            if (Common.isPushAvailable()) {
                Common.showNotification(getApplicationContext(), location);
            }
        }

    };

    private float speedFilter(final float prev, final float curr) {
        if (Float.isNaN(prev)) return curr;
        if (Float.isNaN(curr)) return prev;
        return (float) (curr / 2 + prev * (1.0 - 1.0 / 2));
    }

}