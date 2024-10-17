package kto.smarttour.ui.player;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;
import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import kto.smarttour.BuildConfig;
import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.adapter.DriverStoryListAdapter;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.Common;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.GradientTransformation;
import kto.smarttour.common.utils.ImageUtil;
import kto.smarttour.common.utils.LocationDistance;
import kto.smarttour.common.utils.LocationUtils;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.SimpleDividerItemDecoration;
import kto.smarttour.common.utils.SimpleItemTouchHelperCallback;
import kto.smarttour.common.utils.TourTaxiAnalytics;
import kto.smarttour.common.utils.ViewUtils;

//import kto.smarttour.databinding.ActivityTaxiDriverModeLayoutBinding;
import kto.smarttour.databinding.ActivityTaxiDriverModeLayoutV2Binding;

import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.service.PlayerService;
import kto.smarttour.service.TaxiPlayerService;

import androidx.appcompat.app.AlertDialog;

public class TaxiDriverPlayer extends BaseActivity implements DriverStoryListAdapter.OnItemClickListener, DriverStoryListAdapter.OnStartDragListener, View.OnClickListener {

    public static final int STORY_MSG_RECEIVE_STORY_INFO = 1;
    public static final int STORY_MSG_RECEIVE_STORY_PLAY = 2;
    public static final int STORY_MSG_RECEIVE_STORY_PAUSE = 3;
    public static final int STORY_MSG_RECEIVE_STORY_COMPLETE = 4;
    public static final int STORY_MSG_RECEIVE_STORY_LIST_COMPLETE = 5;
    public static final int STORY_MSG_RECEIVE_STORY_UIUPDATE = 6;


    private static final String TAG = TaxiDriverPlayer.class.getSimpleName();

    private AppCompatActivity activity;

    private ActivityTaxiDriverModeLayoutV2Binding mBind;

    private DriverStoryListAdapter adapter;
    private ItemTouchHelper mItemTouchHelper;
    private int storyPlayPosition;
    private boolean isDrivePlayComplete = false;

    //----------------------------------------------------------------------------
    // 데이터 인덱스 재생시 마지막 데이터 객체로 처리 lastFoundItemIdx
    //----------------------------------------------------------------------------
    private StoryItem lastFoundItem;

    private HashMap<String,String> mapFoundItemIdx;
    //동일위치 시간제한 (300초)이내 도착이벤트 방지 맵
    private HashMap<String,Long> mapArrivalTimeLimit;
    private static final long ARRIVAL_LIMIT_OFFSET = 300000;//300초 (5분)

    private static final long UPDATE_INTERVAL = 1000; // Every 1 seconds.
    private static final long FASTEST_UPDATE_INTERVAL = 1000; // Every 0.5 seconds

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    private RecyclerView.ItemDecoration divider;

    //v2 세로모드만 (manifest에서 orientation 세로로 변경함)
    boolean useBtnChangeOrientation =  false;//택시 플레이어 가로/세로변경 이벤트버튼 허용여부
    //택시 safety 사용자가 팝업을 닫은시간
    private final long TIME_SAFETY_OFFSET = 5000; //5초
    private long userSafetyCloseTime = 0;


    private AlertDialog dialogTaxiArrival;

    private final Messenger mMessenger = new Messenger(new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case STORY_MSG_RECEIVE_STORY_INFO:
                    StoryItem item = (StoryItem) msg.obj;
                    mBind.title.setText(TextUtils.isEmpty(item.titleKo) ? item.title : item.titleKo);
                    mBind.tvStoryCount.setText(String.format("%d / %d", msg.arg1 + 1, adapter.getItemCount()));
                    storyPlayPosition = msg.arg1;
                    mBind.list.scrollToPosition(storyPlayPosition);
                    adapter.setIndex(storyPlayPosition++);
                    Glide.with(activity).load(item.thumbnailFilePath).error(R.drawable.ic_img_scrap_dummy).centerCrop().transform(new MultiTransformation<>(new CenterCrop(), new GradientTransformation())).into(mBind.ivThumb);
                    break;
                case STORY_MSG_RECEIVE_STORY_PLAY:

                    isDrivePlayComplete = false;
                    runOnUiThread(() -> {
                        mBind.btnPlayerPlay.setVisibility(View.INVISIBLE);
                        mBind.btnPlayerPause.setVisibility(View.VISIBLE);
                    });

                    if (msg.arg1 >= 0) {
                        storyPlayPosition = msg.arg1 + 1;
                    }

                    break;
                case STORY_MSG_RECEIVE_STORY_PAUSE:
                    isDrivePlayComplete = false;

                    runOnUiThread(() -> {
                        mBind.btnPlayerPlay.setVisibility(View.VISIBLE);
                        mBind.btnPlayerPause.setVisibility(View.INVISIBLE);
                    });

                    if (msg.arg1 >= 0) {
                        storyPlayPosition = msg.arg1 + 1;
                    }
                    break;
                case STORY_MSG_RECEIVE_STORY_COMPLETE:

                    isDrivePlayComplete = true;

                    // 현재 재생된 이야기 index
                    int currentIndex = msg.arg1;
                    mBind.btnPlayerPlay.setVisibility(View.VISIBLE);
                    mBind.btnPlayerPause.setVisibility(View.INVISIBLE);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        mBind.timerStart.setText("");
                        mBind.timerEnd.setText("");
                    } else {
                        mBind.timer.setText("");

                        //--v2 mBind.timer
                        if(mBind.timerStart!=null){
                            mBind.timerStart.setText("");
                        }
                        if(mBind.timerEnd!=null){
                            mBind.timerEnd.setText("");
                        }
                        //--v2 mBind.timer 숨김
                        if(mBind.timer.getVisibility()!=View.GONE) {
                            mBind.timer.setVisibility(View.GONE);
                        }

                    }
                    mBind.seekArc.setProgress(0);
                    StoryItem story = adapter.getItem(currentIndex);
                    TourTaxiAnalytics.storyPlayEnd(activity, String.valueOf(story.slid), story.langCode);
                    /**
                     *   다음 재생할 이야기로 정보 수정
                     * */

                    int changeCurrentIndex = currentIndex + 1;

                    if (adapter.getItemCount() - 1 >= changeCurrentIndex && currentIndex != 0) {
                        setMediaControllerChange(changeCurrentIndex);
                        try {
                            mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_POS, changeCurrentIndex, 0));
                        } catch (RemoteException e) {

                        }
                        adapter.setIndex(changeCurrentIndex);
                    }
                    break;
                case STORY_MSG_RECEIVE_STORY_LIST_COMPLETE:
                    removeLocationUpdates();
                    DialogUtil.showWarning(activity, "안내", "관광택시 안내가이드가 모두 완료되었습니다.", "확인", "", () -> {
                        finish();
                    }, () -> {

                    });
                    break;
                case STORY_MSG_RECEIVE_STORY_UIUPDATE:
                    int t1 = msg.arg1;
                    int t2 = msg.arg2;
                    float seekArcTime = (float) t1 / (float) t2 * 100.0f;

                    if (mBind != null) {
                        mBind.seekArc.setProgress((int) seekArcTime);


                        String start = String.format("%02d:%02d", t1 / (60 * 1000) % 60, t1 / 1000 % 60);
                        String end = String.format("%02d : %02d", t2 / (60 * 1000) % 60, t2 / 1000 % 60);
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            mBind.timerStart.setText(start);
                            mBind.timerEnd.setText(end);
                        } else {
                            String time = getColoredSpanned(start + " / ", "#FFFFFF") + getColoredSpanned(end, "#ACACAC");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                mBind.timer.setText(Html.fromHtml(time, Html.FROM_HTML_MODE_LEGACY));
                            } else {
                                mBind.timer.setText(Html.fromHtml(time));
                            }

                            String startV2 = String.format("%d:%02d", t1 / (60 * 1000) % 60, t1 / 1000 % 60);
                            String endV2 = String.format("%d:%02d", t2 / (60 * 1000) % 60, t2 / 1000 % 60);
                            //--v2 mBind.timer
                            if(mBind.timerStart!=null){
                                mBind.timerStart.setText(startV2);
                            }
                            if(mBind.timerEnd!=null){
                                mBind.timerEnd.setText(endV2);
                            }
                            //--v2 mBind.timer 숨김
                            if(mBind.timer.getVisibility()!=View.GONE) {
                                mBind.timer.setVisibility(View.GONE);
                            }

                        }
                    }
                    break;
            }
            return false;
        }
    }));

    private String getColoredSpanned(String text, String color) {
        String input = "<font color=" + color + ">" + text + "</font>";
        return input;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        bindService(new Intent(this, TaxiPlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity = this;
        divider = new SimpleDividerItemDecoration(this, SimpleDividerItemDecoration.DIVIDER_TOP_MARGIN, ViewUtils.dp2px(60), R.drawable.line_driver_divider);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        storyPlayPosition = getIntent().getIntExtra("position", 0);

        createLocationRequest();
        initView();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        initView();
        try {
            mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_STATE_INFO));
        } catch (RemoteException e) {
        }

        if (TaxiPlayerService.isPlay) {
            if (storyPlayPosition >= 0) {
                storyPlayPosition++;
            }
        }

    }

    /**
     * Show quit dialog.
     */
    private void showQuitTaxiAppFinishDialog() {
        DialogUtil.showWarning(this, getString(R.string.finish), "Odii관광택시를\n종료하겠습니까?", getString(R.string.message_yes), getString(R.string.message_no), () -> {
            //--
            stopService(new Intent(activity, TaxiPlayerService.class));

            //  OdiiApplication.finishApplication();

            this.moveTaskToBack(true);
            this.finishAndRemoveTask();
            android.os.Process.killProcess(android.os.Process.myPid());

            //--
        }, () -> {

        });
    }

    private void initView() {
        mBind = DataBindingUtil.setContentView(activity, R.layout.activity_taxi_driver_mode_layout_v2);
        mBind.seekArc.setOnTouchListener((v, event) -> {
            return true;
        });
        mBind.setLifecycleOwner(this);
        makeDumpView();
        setRecyclerView();

        //타이틀
        setupLayout();

        //v2 safety
        //-------------------------------------------------------------------------------------------------------------
        //safety 메시지 색상번경 : 정차 후 사용해 주세요.\n관광지에 도착시 자동 실행됩니다.
        TextView tv_safety_msg = findViewById(R.id.tv_safety_msg);
        String msg_safety = tv_safety_msg.getText().toString();
        SpannableString sapnnable_msg_safety = new SpannableString(msg_safety);

        String focus_msg_safety = "정차 후 사용해 주세요.\n관광지에 도착시 자동 실행됩니다.";
        int start_msg_safety = msg_safety.indexOf(focus_msg_safety);
        int end_msg_safety = start_msg_safety + focus_msg_safety.length();

        sapnnable_msg_safety.setSpan(new ForegroundColorSpan(Color.parseColor("#f03508")), start_msg_safety, end_msg_safety, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv_safety_msg.setText(sapnnable_msg_safety);

        //---------------------
        mBind.btnSafetyAction.setOnClickListener(v ->{
            userSafetyCloseTime = System.currentTimeMillis();

            if(mBind.layoutSafety.getVisibility()!=View.GONE){
                mBind.layoutSafety.setVisibility(View.GONE);
            }
        });

        //-------------------------------------------------------------------------------------------------------------



        mBind.btnClose.setOnClickListener(view -> finish());
        //(v2 보조) 우측 추가종료버튼
        mBind.btnCloseRight.setOnClickListener(view ->{
            showQuitTaxiAppFinishDialog();
        });

        //v2 세로모드만 (manifest에서 orientation 세로로 변경함)
        //------------------------------------------------------------------------------------------------------------
        useBtnChangeOrientation =  false;//택시 플레이어 가로/세로변경 이벤트버튼 허용여부
        if(!useBtnChangeOrientation){
            //orientation 변경금지
            mBind.btnRotation.setVisibility(View.INVISIBLE);
        }
        //------------------------------------------------------------------------------------------------------------

        mBind.btnRotation.setOnClickListener(view -> {

            //v2 세로모드만 (manifest에서 orientation 세로로 변경함)
            if(!useBtnChangeOrientation){
                return;
            }

            if (adapter.getItemCount() == 1) {
                storyPlayPosition = 0;
            } else {
                int itemCount = adapter.getItemCount() - 1;
                int index = adapter.getIndex();
                if (itemCount <= index) {
                    storyPlayPosition = itemCount;
                } else {
                    storyPlayPosition = index;
                }
            }

            int rotation = getResources().getConfiguration().orientation;
            if (rotation == Configuration.ORIENTATION_PORTRAIT) {
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else if (rotation == Configuration.ORIENTATION_LANDSCAPE) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });

        //v2 목록편집기능 숨김
        mBind.btnEdit.setVisibility(View.GONE);
        mBind.btnEdit.setOnClickListener(view -> {

            DialogUtil.showWarning(activity, "안내", "안내 순서를 편집 하실 경우 처음부터 안내가 시작됩니다.\n안내가 완료된 관광지는 삭제해주세요.", "확인", "취소", () -> {
                try {
                    if (mService != null) {
                        mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_STOP));
                    }
                } catch (RemoteException e) {
                }
                storyPlayPosition = 0;
                adapter.setIndex(0);
                adapter.setEdit(true);
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mBind.timerStart.setText("");
                    mBind.timerEnd.setText("");
                } else {
                    mBind.seekArc.setVisibility(View.GONE);
                    mBind.timer.setText("");

                    //--v2 mBind.timer
                    if(mBind.timerStart!=null){
                        mBind.timerStart.setText("");
                    }
                    if(mBind.timerEnd!=null){
                        mBind.timerEnd.setText("");
                    }
                    //--v2 mBind.timer 숨김
                    if(mBind.timer.getVisibility()!=View.GONE) {
                        mBind.timer.setVisibility(View.GONE);
                    }
                }
                mBind.layoutInfo.setVisibility(View.GONE);
                mBind.btnEdit.setVisibility(View.GONE);
                mBind.btnEditComplete.setVisibility(View.VISIBLE);
                removeLocationUpdates();
            }, () -> {

            });


        });

        mBind.btnEditComplete.setOnClickListener(view -> {
            storyPlayPosition = 0;
            try {
                mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_POS, 0, 0));
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            adapter.setIndex(storyPlayPosition);
            adapter.setEdit(false);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            } else {
                mBind.seekArc.setVisibility(View.VISIBLE);
            }

            mBind.layoutInfo.setVisibility(View.VISIBLE);
            mBind.btnEdit.setVisibility(View.VISIBLE);
            mBind.btnEditComplete.setVisibility(View.GONE);
            requestLocationUpdates();
            checkPlayer();

            ArrayList<StoryItem> items = adapter.getItemAll();

            //-- 기록 초기화
            lastFoundItem = null;
            if( items!=null && items.size()>0 ){
                mapFoundItemIdx = new HashMap<>(items.size());
                //--
                mapArrivalTimeLimit = new HashMap<>(items.size());

            }else{
                mapFoundItemIdx = new HashMap<>();
                //--
                mapArrivalTimeLimit = new HashMap<>();
            }

            TaxiPlayerService.isStoryListComplete = false;

            StoryDbManager.getInstance(activity).clearPlayList();
            StoryDbManager.getInstance(activity).addPlayList("D", items);
        });

        mBind.btnPlayerPlay.setOnClickListener(this);
        mBind.btnPlayerPause.setOnClickListener(this);
        mBind.btnPlayerNext.setOnClickListener(this);
        mBind.btnPlayerPrev.setOnClickListener(this);

        setMediaControllerChange(storyPlayPosition);

        mBind.btnPlayerPlay.setVisibility(View.VISIBLE);
        mBind.btnPlayerPause.setVisibility(View.INVISIBLE);

        mBind.tvStoryTmp.setText("이야기");
        mBind.btnEdit.setText("목록편집");
        mBind.btnEditComplete.setText("편집완료");
    }

    private void setupLayout() {
        String strTitle = "Odii 관광택시";

        Intent intent = getIntent();
        if(intent!=null){
            String title = intent.getStringExtra("title");
            /*
            if( title!=null && !title.isEmpty() ){
                strTitle = strTitle + " - "+title;
            }
            */
            strTitle = title;

            mBind.tvTaxiTitle.setText(strTitle);

        }else{
            mBind.tvTaxiTitle.setText(strTitle);
        }
    }


    private void setMediaControllerChange(int position) {
        if (adapter != null && !adapter.isEmpty()) {
            if (position == -1) {
                position = 0;
            }

            if (adapter.getItemCount() == 1) {
                position = 0;
                storyPlayPosition = 0;
            } else if (adapter.getItemCount() - 1 <= position) {
                position = adapter.getItemCount() - 1;
                storyPlayPosition = adapter.getItemCount() - 1;
            }

            StoryItem item = adapter.getItem(position);
            Glide.with(this).load(item.thumbnailFilePath).placeholder(R.drawable.ic_img_scrap_dummy).error(R.drawable.ic_img_scrap_dummy).centerCrop().transform(new MultiTransformation<>(new CenterCrop(), new GradientTransformation())).into(mBind.ivThumb);
            mBind.title.setText(TextUtils.isEmpty(item.titleKo) ? item.title : item.titleKo);
            //MARQUEE
            mBind.title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            mBind.title.setSelected(true);

            mBind.tvStoryCount.setText(String.format("%d / %d", position + 1, adapter.getItemCount()));
        }
    }

    private void requestPermissions() {

        boolean permissionAccessFineLocationApproved = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (permissionAccessFineLocationApproved) {
            requestLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(TaxiDriverPlayer.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private boolean checkPermissions() {
        int fineLocationPermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        //지오팬스 ACCESS_BACKGROUND_LOCATION
        int backgroundLocationPermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        return (fineLocationPermissionState == PackageManager.PERMISSION_GRANTED) && (backgroundLocationPermissionState == PackageManager.PERMISSION_GRANTED);

        //return (fineLocationPermissionState == PackageManager.PERMISSION_GRANTED);
    }


    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void setRecyclerView() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mBind.list.addItemDecoration(divider);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mBind.list.getItemDecorationCount() != 0) {
                mBind.list.removeItemDecoration(divider);
            }
        }

        RecyclerView.ItemAnimator animator = mBind.list.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        adapter = new DriverStoryListAdapter(this, storyPlayPosition);

        //--
        ArrayList<StoryItem> items = StoryDbManager.getInstance(this).getPlayList().story;

        //기록 초기화
        lastFoundItem = null;

        if( items!=null && items.size()>0 ){
            mapFoundItemIdx = new HashMap<>(items.size());

            mapArrivalTimeLimit = new HashMap<>(items.size());
        }else{
            mapFoundItemIdx = new HashMap<>();

            mapArrivalTimeLimit = new HashMap<>();
        }
        TaxiPlayerService.isStoryListComplete = false;

        //--
        adapter.updateItems(items);
        adapter.setOnItemClickListener(this);
        adapter.setOnStartDragListener(this);
        mBind.list.setAdapter(adapter);

        //--
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mBind.list);

        mBind.list.scrollToPosition(storyPlayPosition);

    }

    private void makeDumpView() {
        RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams) mBind.dumpView.getLayoutParams();
        rl.height = statusBarHeight;
        mBind.dumpView.setLayoutParams(rl);
        mBind.dumpView.setBackgroundColor(changeAlpha(Color.parseColor("#353B49"), 0.9f));

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            ((RelativeLayout.LayoutParams) mBind.topbar.getLayoutParams()).setMargins(0, statusBarHeight, 0, 0);
            mBind.dumpView.setVisibility(View.VISIBLE);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ((RelativeLayout.LayoutParams) mBind.topbar.getLayoutParams()).setMargins(0, 0, 0, 0);
            mBind.dumpView.setVisibility(View.GONE);
        }
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
        super.onResume();

        //	액티비티 확인 - 택시 가이드
        Common.gCurrentTaxtActivity = Common.ACTIVITY_TAXI_PLAYER;

        if (SettingsUtil.getTaxiTtid(activity) != -1) {
            if (!checkPermissions()) {
                requestPermissions();
            } else {
                requestLocationUpdates();
            }
        }

        try {
            stopService(new Intent(activity, PlayerService.class));
        } catch (Exception e) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //KLog.i("LifeCycle", "onPause");
        removeLocationUpdates();

        SettingsUtil.setTexiLastIndex(activity, adapter.getIndex() == -1 ? 0 : adapter.getIndex());

        try {
            mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_PAUSE));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 이야기 선택시 재생 요청 콜백
     *
     * @param view
     * @param position
     */
    @Override
    public void onItemClick(View view, int position) {
        //setMediaControllerChange(position);
        try {
            mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_PLAY_INDEX, position, 0));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBind.list.scrollToPosition(position);
                }
            }, 200);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 이야기 스와이프 삭제 시 콜백
     *
     * @param view
     * @param position
     */
    @Override
    public void onItemDelete(View view, int position) {
        StoryItem deleteItem = adapter.getItem(position);
        adapter.onItemDismiss(position);
        StoryDbManager.getInstance(activity).removePlayList_Slid(deleteItem.slid);
        adapter.notifyDataSetChanged();
    }

    /**
     * 권한 요청
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                //Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Snackbar.make(findViewById(R.id.main_activity), R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Build intent that displays the App settings screen.
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }).show();
            }
        }
    }

    /**
     * 화면 종료시 처리
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //KLog.i("LifeCycle", "onDestroy");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (bound) {
            unbindService(mConnection);
            bound = false;
        }
    }

    @Override
    public void onClick(View view) {
        try {
            int id = view.getId();
            switch (id) {
                case R.id.btn_player_play:
                    mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_PLAY));
                    break;
                case R.id.btn_player_pause:
                    mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_PAUSE));
                    break;
                case R.id.btn_player_next:
                    mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_NEXT));
                    break;
                case R.id.btn_player_prev:
                    mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_PREV));
                    break;
            }
        } catch (Exception e) {
        }
    }

    /**
     * 이야기 정보 썸네일, 제목 업데이트
     */
    private void checkPlayer() {
        StoryItem item = adapter.getItem(storyPlayPosition);
        String imageFile = FileUtils.getFileName(this, item);
        ImageUtil.localLoadImage3(mBind.ivThumb, imageFile, null);


        mBind.title.setText(TextUtils.isEmpty(item.titleKo) ? item.title : item.titleKo);
        mBind.tvStoryCount.setText(String.format("%d / %d", storyPlayPosition + 1, adapter.getItemCount()));
    }


    /**
     * 리스트 편집 순서변경 시작 콜백
     *
     * @param viewHolder
     */
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    /**
     * 리스트 변집 콜백
     *
     * @param fromPosition
     * @param toPosition
     */
    @Override
    public void onEndDrag(int fromPosition, int toPosition) {
        adapter.notifyDataSetChanged();
    }


    /**
     * 위치정보 콜백 등록
     */
    public void requestLocationUpdates() {
        try {
            LocationUtils.setRequestingLocationUpdates(this, true);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            LocationUtils.setRequestingLocationUpdates(this, false);
            e.printStackTrace();
        }
    }

    /**
     * 위치정보 콜백 제거
     */
    public void removeLocationUpdates() {
        //Log.i(TAG, "Removing location updates");
        LocationUtils.setRequestingLocationUpdates(this, false);
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * 위정보정 콜백
     */
    float filtSpeed;
    float localspeed;
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locations = locationResult.getLocations();
            Location location = locations.get(0);

            //인덱스 아이템
            StoryItem item = null;

            if (adapter != null && !adapter.isEmpty()) {

                boolean checkAll = true;
                if(checkAll){
                    //찾기
                    ArrayList arrayItems = adapter.getItemAll();
                    if(arrayItems!=null){

                        int size = arrayItems.size();

                        //범위안 데이터에서 단일 데이터 추출 : true 설정
                        //범위안 데이터에서 배열 추출 : false 설정
                        boolean checkDistanceSimpleShort = false;

                        double foundDistance = -1;
                        ArrayList<StoryItem> arrayItemsInRange = new ArrayList<>();

                        for(int i=0; i<size; i++){
                            StoryItem itemCandidate = (StoryItem)arrayItems.get(i);
                            itemCandidate.taxiplay_index = i;

                            double distance = LocationDistance.distance(Double.valueOf(itemCandidate.posY), Double.valueOf(itemCandidate.posX), location.getLatitude(), location.getLongitude(), "m");

                            //--
                            if (distance < itemCandidate.radius) {
                                if(checkDistanceSimpleShort){
                                    if(foundDistance >= 0){
                                        if(foundDistance > distance){
                                            foundDistance = distance;
                                            arrayItemsInRange.set(0,itemCandidate);
                                        }
                                    }else{
                                        foundDistance = distance;
                                        arrayItemsInRange.add(itemCandidate);
                                    }
                                }else{
                                    arrayItemsInRange.add(itemCandidate);
                                }


                            }

                        }

                        if (arrayItemsInRange != null && arrayItemsInRange.size()>0) {

                            if(checkDistanceSimpleShort){
                                try {

                                    boolean allowArrivalEvent = false;

                                    if(dialogTaxiArrival!=null && dialogTaxiArrival.isShowing() ){
                                        allowArrivalEvent = false;
                                    }else{
                                        item = arrayItemsInRange.get(0);
                                        if(lastFoundItem == null){
                                            lastFoundItem = item;
                                            allowArrivalEvent = true;
                                        }else{
                                            //아이템이 다를경우
                                            if( !lastFoundItem.isEquals(item) ){

                                                String keyTimeLimit = String.valueOf(item.taxiplay_index);
                                                if( mapArrivalTimeLimit.containsKey(keyTimeLimit) ){
                                                    long currentMils = System.currentTimeMillis();
                                                    long lastArrived = mapArrivalTimeLimit.get(keyTimeLimit);

                                                    long diff = currentMils - lastArrived;
                                                    if( diff > ARRIVAL_LIMIT_OFFSET ){
                                                        lastFoundItem = item;
                                                        allowArrivalEvent = true;
                                                    }

                                                }else{
                                                    lastFoundItem = item;
                                                    allowArrivalEvent = true;
                                                }


                                            }

                                        }

                                    }

                                    if(allowArrivalEvent){
                                        dialogTaxiArrival = DialogUtil.showTaxiArrival(activity, "", "", arrayItemsInRange, null, "", (resultItem,confirm) -> {

                                            if(confirm){
                                                storyPlayPosition = resultItem.taxiplay_index;
                                                try {
                                                    mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_PLAY_INDEX, storyPlayPosition, 0));//전달
                                                }catch (Exception e){

                                                }

                                                //기록
                                                String keyFound = String.valueOf(storyPlayPosition);
                                                if( !mapFoundItemIdx.containsKey(keyFound) ){
                                                    mapFoundItemIdx.put(keyFound,keyFound);
                                                }

                                                if(size == mapFoundItemIdx.keySet().size()){
                                                    TaxiPlayerService.isStoryListComplete = true;
                                                }
                                                //밀리세컨드 기록
                                                String keyTimeLimit = String.valueOf(storyPlayPosition);
                                                mapArrivalTimeLimit.put(keyTimeLimit,System.currentTimeMillis());

                                            }

                                        });

                                        //--
                                        if (mBind.layoutSafety.getVisibility() != View.GONE) {
                                            mBind.layoutSafety.setVisibility(View.GONE);
                                        }
                                    }

                                } catch (Exception e) {
                                }

                            }else{
                                //여러위치
                                try {
                                    boolean allowArrivalEvent = false;
                                    if(dialogTaxiArrival!=null && dialogTaxiArrival.isShowing() ){
                                        allowArrivalEvent = false;
                                    }else{
                                        if (lastFoundItem == null) {
                                            allowArrivalEvent = true;
                                        }else{

                                            boolean notFoundObject = true;
                                            for(int idx=0; idx<arrayItemsInRange.size(); idx++){
                                                StoryItem itemCandidate = arrayItemsInRange.get(idx);
                                                if(lastFoundItem.isEquals(itemCandidate)){
                                                    notFoundObject = false;
                                                    break;
                                                }
                                            }
                                            if(notFoundObject){
                                                if(arrayItemsInRange.size()>0){

                                                    if(arrayItemsInRange.size()>1){
                                                        allowArrivalEvent = true;
                                                    }else{
                                                        allowArrivalEvent = false;
                                                        StoryItem itemCandidate = arrayItemsInRange.get(0);
                                                        String keyTimeLimit = String.valueOf(itemCandidate.taxiplay_index);
                                                        if( mapArrivalTimeLimit.containsKey(keyTimeLimit) ){
                                                            long currentMils = System.currentTimeMillis();
                                                            long lastArrived = mapArrivalTimeLimit.get(keyTimeLimit);

                                                            long diff = currentMils - lastArrived;
                                                            if( diff > ARRIVAL_LIMIT_OFFSET ){
                                                                allowArrivalEvent = true;
                                                            }
                                                        }else{
                                                            allowArrivalEvent = true;
                                                        }
                                                    }

                                                }

                                            }
                                            //---------------------------------------
                                        }

                                    }



                                    //도착
                                    if(allowArrivalEvent){

                                        dialogTaxiArrival = DialogUtil.showTaxiArrival(activity, "", "", arrayItemsInRange, null, "", (resultItem,confirm) -> {
                                            //OnDialogStoryItemClick
                                            lastFoundItem = resultItem;
                                            storyPlayPosition = resultItem.taxiplay_index;

                                            if(confirm){
                                                //오디오
                                                try {
                                                    mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_PLAY_INDEX, storyPlayPosition, 0));//전달
                                                }catch (Exception e){

                                                }

                                                //기록
                                                String keyFound = String.valueOf(storyPlayPosition);
                                                if( !mapFoundItemIdx.containsKey(keyFound) ){
                                                    mapFoundItemIdx.put(keyFound,keyFound);
                                                }

                                                if(size == mapFoundItemIdx.keySet().size()){
                                                    TaxiPlayerService.isStoryListComplete = true;
                                                }
                                            }

                                            //밀리세컨드 기록
                                            String keyTimeLimit = String.valueOf(resultItem.taxiplay_index);
                                            mapArrivalTimeLimit.put(keyTimeLimit,System.currentTimeMillis());

                                        });

                                        //--
                                        if (mBind.layoutSafety.getVisibility() != View.GONE) {
                                            mBind.layoutSafety.setVisibility(View.GONE);
                                        }

                                    }

                                } catch (Exception e) {
                                }

                            }

                        }

                    }

                }else{
                    //인덱스기반
                    if (adapter.getItemCount() - 1 >= storyPlayPosition) {
                        //----------------------------------------------
                        item = adapter.getItem(storyPlayPosition);
                        //----------------------------------------------
                        double distance = LocationDistance.distance(Double.valueOf(item.posY), Double.valueOf(item.posX), location.getLatitude(), location.getLongitude(), "m");

                        if (distance < item.radius) {
                            try {

                                if (isDrivePlayComplete || storyPlayPosition == 0) {
                                    isDrivePlayComplete = false;

                                    mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_PLAY_INDEX, storyPlayPosition++, 0));//전달 후 증가

                                    if (adapter.getItemCount() - 1 <= storyPlayPosition) {
                                        storyPlayPosition = 0;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                        //----------------------------------------------
                    }
                }
            }

            //----------------------------------------------

            boolean useSafeLayoutIgnoreSpeed = false; //개발용 true

            boolean hasSpeed = false;
            if (location.hasSpeed()) {
                hasSpeed = true;

                localspeed = location.getSpeed() * 3.6f;
                filtSpeed = speedFilter(filtSpeed, localspeed);

                if(useSafeLayoutIgnoreSpeed){
                    filtSpeed = 20;
                }

                runOnUiThread(() -> {
                    if (filtSpeed >= 10) {
                        Common.isTaxiDriving = true;

                        long now = System.currentTimeMillis();
                        long diff = now - userSafetyCloseTime;
                        if(diff > TIME_SAFETY_OFFSET){

                            //diff 무한증가 방지
                            userSafetyCloseTime = (now - TIME_SAFETY_OFFSET) - 2000;
                            //----------------------------------------------------------------
                            if(dialogTaxiArrival!=null && dialogTaxiArrival.isShowing() ){

                                if (mBind.layoutSafety.getVisibility() != View.GONE) {
                                    mBind.layoutSafety.setVisibility(View.GONE);
                                }

                            }else{

                                if (mBind.layoutSafety.getVisibility() != View.VISIBLE) {
                                    mBind.layoutSafety.setVisibility(View.VISIBLE);
                                }

                            }
                        }//else

                    } else {
                        Common.isTaxiDriving = false;
                        if (mBind.layoutSafety.getVisibility() != View.GONE) {
                            mBind.layoutSafety.setVisibility(View.GONE);
                        }
                    }
                });
                //KLog.i("LocationCheckValue", String.format("speed : %s", filtSpeed));
            }

            if (Common.isPushAvailable()) {
                Common.showNotification(getApplicationContext(), location);
            }

        }
    };


    /**
     * 서비스 연결
     */
    private Messenger mService = null;
    private boolean bound;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = new Messenger(iBinder);
            bound = true;

            try {
                Message msg = Message.obtain(null, TaxiPlayerService.STORY_MSG_HANDLER);
                msg.replyTo = mMessenger;
                mService.send(msg);

                mService.send(Message.obtain(null, TaxiPlayerService.STORY_MSG_RECEIVE_POS, storyPlayPosition, 0));
            } catch (RemoteException e) {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            bound = false;
        }
    };


    /**
     * 속도 계산기
     *
     * @param prev 이전 속도
     * @param curr 현재 속도
     * @return
     */
    private float speedFilter(final float prev, final float curr) {
        // If first time through, initialise digital filter with current values
        if (Float.isNaN(prev)) return curr;
        // If current value is invalid, return previous filtered value
        if (Float.isNaN(curr)) return prev;
        // Calculate new filtered value
        return (float) (curr / 2 + prev * (1.0 - 1.0 / 2));
    }
}
