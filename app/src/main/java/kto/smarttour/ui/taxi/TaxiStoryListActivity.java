package kto.smarttour.ui.taxi;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.tabs.TabLayout;
import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.adapter.TaxiStoryListAdapter;
import kto.smarttour.binding.BindingAdapters;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.Common;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.LocationUtils;
import kto.smarttour.common.utils.NetworkUtil;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.SimpleDividerItemDecoration;
import kto.smarttour.common.utils.StorageUtil;
import kto.smarttour.common.utils.SystemUtils;
import kto.smarttour.common.utils.TourTaxiAnalytics;
import kto.smarttour.common.utils.ViewUtils;
import kto.smarttour.databinding.ActivityTaxiStoryListBinding;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.download.FileDownLoadType;
import kto.smarttour.network.response.dao.TourTaxiInfo;
import kto.smarttour.service.TaxiPlayerService;
import kto.smarttour.ui.MainActivity;
import kto.smarttour.ui.player.TaxiDriverPlayer;

public class TaxiStoryListActivity extends BaseActivity implements TaxiStoryListAdapter.OnItemClickListener, TaxiStoryListAdapter.OnControlClickListener, View.OnClickListener{
//TaxiStoryListAdapter의 추가인터페이스도 implements


    private ActivityTaxiStoryListBinding mBind;
    private TaxiStoryListAdapter adapter;
    private boolean isSelectAll = false;
    private String lang;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private float filtSpeed;
    private float localspeed;

    private static final long UPDATE_INTERVAL = 1000;
    private static final long FASTEST_UPDATE_INTERVAL = 1000;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private boolean modeTabSelection = true; //기본값
    private int colorTvTabSelected = Color.parseColor("#ff6755bb");
    private int colorTvTabNormal = Color.parseColor("#ff8e8e8e");

    //-------------------------------------------------------------------
    //-------------------------------------------------------------------
    //-------------------------------------------------------------------
    //-- 택시
    private AppCompatActivity activity;
    //putExtra 전달목적 implements Serializable 추가한 TourTaxiInfo객체 받기

    private TourTaxiInfo data;
    private List<StoryItem> updateStoryList;

    //--추가 , 현재 언어기준 데이터 정상, 업데이트 필요, 다운로드 필요, 미제공
    private final class StoryDataStatus {
        private static final int StoryData_OK_SELECTION = -2;
        private static final int StoryData_NO_SERVICE = -1;
        private static final int StoryData_UPDATE = 1;
        private static final int StoryData_OK = 2;
        private static final int StoryData_DOWNLOAD = 3;

        // If you have only static members and want to simulate a static
        // class in Java, then you can make the constructor private.
        private StoryDataStatus() {}
    }
    //초기값
    private int DataStatus = StoryDataStatus.StoryData_NO_SERVICE;
    //-------------------------------------------------------------------
    private static final int TYPE_DOWNLAOD = 0;
    private static final int TYPE_UPGRADE = 1;
    private int fileDownLoadType = TYPE_DOWNLAOD;
    //-------------------------------------------------------------------
    //-------------------------------------------------------------------


    @Override
    protected void onResume() {
        super.onResume();

        //	액티비티 확인 - 택시 이야기
        Common.gCurrentTaxtActivity = Common.ACTIVITY_TAXI_STORY;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;

        mBind = DataBindingUtil.setContentView(this, R.layout.activity_taxi_story_list);
        mBind.setLifecycleOwner(this);
        mBind.btnClose.setOnClickListener(view -> finish());
        //(v2 보조) 우측 추가종료버튼
        mBind.btnCloseRight.setOnClickListener(view ->{
            showQuitTaxiAppFinishDialog();
        });

        lang = getIntent().getStringExtra("lang");

        Object obj = getIntent().getSerializableExtra("TourTaxiInfo");
        if(obj!=null){
            if(obj instanceof TourTaxiInfo){
                data = (TourTaxiInfo)obj;
            }
        }

        //타이틀
        setupLayout();

        BindingAdapters.setDriverLangCodeBox(mBind.tvLang, lang);
        if (statusBarHeight > 0) {
            RelativeLayout.LayoutParams ll = (RelativeLayout.LayoutParams) mBind.dumpView.getLayoutParams();
            ll.height = statusBarHeight;
            mBind.dumpView.setLayoutParams(ll);
        }

        //selectAll과 playAll위치에 새로운 기능으로 리뉴얼
        mBind.tvTabSelection.setOnClickListener(this);
        mBind.tvTabAll.setOnClickListener(this);
        mBind.tvTabSelectAll.setOnClickListener(this);


        mBind.btnBottomMenuPlay.setOnClickListener(this);
        mBind.btnBottomMenuCancel.setOnClickListener(this);

        mBind.btnListAction.setOnClickListener(this);

        //선택 ,전체목록 모도 초기화 토글
        setModeTabSelection(true,false);

        //탭 초기화 (탭 추가 : 한국어 , 영어 , 중국어 , 일본어)
        setTabLayout();

        //setRecyclerView();
        //데이터는 있으나, 업데이트 상황 삭제된 데이터상황 등 체크해야함..
        checkStory(); //택시 메인에 있던 기능 1 -> 완료 후 최종  setRecyclerView()이 호출될 수 있도록 수정

        createLocationRequest();

        if (!checkPermissions()) {
            requestPermissions();
        }
        else {
            requestLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        removeLocationUpdates();
    }

    private void setModeTabSelection(boolean tabSelection, boolean updateRecyclerView){
        this.modeTabSelection = tabSelection;

        mBind.tvTabSelectAll.setTextColor(this.colorTvTabNormal);

        if(this.modeTabSelection){
            mBind.tvTabSelection.setTextColor(this.colorTvTabSelected);
            mBind.tvTabSelection.setPaintFlags(mBind.tvTabSelection.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            //--------------------------------------------
            mBind.tvTabAll.setTextColor(this.colorTvTabNormal);
            //지정되어있는 페인트 플래그 체크 후, 목적 속성 제거
            if ((mBind.tvTabAll.getPaintFlags() & Paint.UNDERLINE_TEXT_FLAG) > 0){
                mBind.tvTabAll.setPaintFlags( mBind.tvTabAll.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
            }
            //--------------------------------------------
            if( mBind.tvTabSelectAll.getVisibility() == View.VISIBLE ){
                mBind.tvTabSelectAll.setVisibility(View.INVISIBLE);
            }

        }else{

            mBind.tvTabSelection.setTextColor(this.colorTvTabNormal);
            //지정되어있는 페인트 플래그 체크 후, 목적 속성 제거
            if ((mBind.tvTabSelection.getPaintFlags() & Paint.UNDERLINE_TEXT_FLAG) > 0){
                mBind.tvTabSelection.setPaintFlags( mBind.tvTabSelection.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
            }

            //--------------------------------------------
            mBind.tvTabAll.setTextColor(this.colorTvTabSelected);
            mBind.tvTabAll.setPaintFlags(mBind.tvTabAll.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            //--------------------------------------------
            if( mBind.tvTabSelectAll.getVisibility() != View.VISIBLE ){
                mBind.tvTabSelectAll.setVisibility(View.VISIBLE);
            }

        }

        if(updateRecyclerView){
            //기존 setRecyclerView();
            checkStory();
        }

    }

    private void setupLayout() {
        String strTitle = "Odii 관광택시";
        if(data!=null){
            String title = data.title;
            if( title!=null && !title.isEmpty() ){
                strTitle = strTitle + " - "+title;
            }
            mBind.tvTaxiTitle.setText(strTitle);
        }else{
            mBind.tvTaxiTitle.setText(strTitle);
        }
    }

    private void setTabLayout(){

        //lang 기준 코드
        // "ko"
        // "en"
        // "cn1"
        // "jp"

        //탭 초기화 (탭 추가 : 한국어 , 영어 , 중국어 , 일본어)
        TabLayout.Tab tab = mBind.tabLang.newTab();
        tab.setText("한국어");
        mBind.tabLang.addTab(tab);
        //--
        tab = mBind.tabLang.newTab();
        tab.setText("영어");
        mBind.tabLang.addTab(tab);
        //--
        tab = mBind.tabLang.newTab();
        tab.setText("중국어");
        mBind.tabLang.addTab(tab);
        //-- 일본어
        tab = mBind.tabLang.newTab();
        tab.setText("일본어");
        mBind.tabLang.addTab(tab);

        //이벤트 연결
        mBind.tabLang.addOnTabSelectedListener(tabSelectedListener);
    }

    private TabLayout.OnTabSelectedListener tabSelectedListener = new TabLayout.OnTabSelectedListener() {

        //lang 기준 코드
        // "ko"
        // "en"
        // "cn1"
        // "jp"

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            int position = tab.getPosition();
            switch (position) {
                case 0:
                    lang = "ko";
                    break;
                case 1:
                    lang = "en";
                    break;
                case 2:
                    lang = "cn1";
                    break;
                case 3:
                    lang = "jp";
                    break;
                default:
                    lang = "ko";
                    break;
            }

            checkStory();
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }
    };

    private void setRecyclerView() {

        //초기상태
        mBind.list.setVisibility(View.INVISIBLE);

        if(adapter==null){
            mBind.list.addItemDecoration(new SimpleDividerItemDecoration(this, SimpleDividerItemDecoration.DIVIDER_END_MARGIN, ViewUtils.dp2px(80), R.drawable.line_divider));
            adapter = new TaxiStoryListAdapter(this);
            adapter.setOnItemClickListener(this);
            //추가정의 클릭이벤트 인터페이스 할당
            adapter.setOnControlClickListener(this);

            mBind.list.setAdapter(adapter);
        }

        //어댑터 내부에 언어를 자정하기 전 설정된언어
        String adapterLang = adapter.getLang();

        adapter.setModeTabSelection(this.modeTabSelection,this.lang);

        if(this.modeTabSelection){
            //빈 선택목록을 지정
            if(adapterLang!=null && adapterLang.equalsIgnoreCase(this.lang)) {
                //adapter에서 선택아이템을 가져와 사용
                List<StoryItem> selectItems = adapter.getSelectList();
                //선택아이템을 가져와서 선택항목모드의 리스트에 세팅
                adapter.updateItems( (ArrayList<StoryItem>)selectItems );
            }else{
                adapter.updateItems(null);
            }
            check_V2();

        }else{

            //전체 목록
            List<StoryItem> items = StoryDbManager.getInstance(this).getDownloadList(lang);

            if(adapterLang!=null && adapterLang.equalsIgnoreCase(this.lang)) {
                //동일언어의 기존 선택상태 아이템 준비
                List<StoryItem> selectItems = adapter.getSelectList();

                if(selectItems.size()>0){
                    for(StoryItem selectedItem : selectItems){
                        //동일언어일떄 선택아이템에 대해 조건부 선택상태 복구
                        for (StoryItem item : items) {

                            if (item.isEquals(selectedItem) ){
                                item.isSelected = true;
                            }

                        }
                    }
                }
            }

            adapter.updateItems((ArrayList<StoryItem>) items);
            check_V2();

        }

        if (DataStatus == StoryDataStatus.StoryData_UPDATE || DataStatus == StoryDataStatus.StoryData_NO_SERVICE){
            //리스트 숨기고 안내페이지 보여주자
            mBind.list.setVisibility(View.INVISIBLE);
            mBind.listEmpty.setVisibility(View.VISIBLE);
        }else{
            //리스트 아이템 없을때 출력하는 뷰
            int itemCount = adapter.getItemCount();

            int visibility = mBind.listEmpty.getVisibility();
            if(itemCount>0){

                //초기상태 복귀
                int listVisibility = mBind.list.getVisibility();
                if(listVisibility!=View.VISIBLE){
                    mBind.list.setVisibility(View.VISIBLE);
                }

                if(visibility!=View.GONE){
                    //데이터가 있는 경우 숨김
                    mBind.listEmpty.setVisibility(View.GONE);
                }
            }else{
                //저장되어있는 데이터가 없을때 해당.
                //데이터가 없을 떄 보임
                if(visibility!=View.VISIBLE){
                    mBind.listEmpty.setVisibility(View.VISIBLE);
                }

            }
            mBind.tvCount.setText(String.valueOf(itemCount));
        }
    }

    private void checkStory() {

        if(this.modeTabSelection){
            initDownLoadResource(lang, StoryDataStatus.StoryData_OK_SELECTION);
            return;
        }

        List<StoryItem> oldStories = StoryDbManager.getInstance(activity).getDownloadList(lang);
        List<StoryItem> newStories = new ArrayList<>();

        //현재 언어에 맞는 객체 맵핑 (서버데이터 객체로부터 스토리목록 데이터 준비)
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

        //현재언어의 서버데이터 스토리목록이 있으면
        if (!newStories.isEmpty()) {
            //로컬 저장데이터가 없으면
            if (oldStories.isEmpty()) {
                initDownLoadResource(lang, StoryDataStatus.StoryData_DOWNLOAD);

                // 용량 체크
                long totalFileSize = 0L;
                for (StoryItem item : newStories) {
                    totalFileSize += Long.valueOf(item.audioFileSize);
                }

            } else {
                // 현재언어의 로컬 저장데이터가 있으면
                // --
                List<StoryItem> deleteStoryList = compareStoryDeleteList(oldStories, newStories);

                if (!deleteStoryList.isEmpty()) {
                    StoryDbManager.getInstance(activity).removeDownLoad2(deleteStoryList);
                    FileUtils.removeFile(this, deleteStoryList);
                }
                oldStories = StoryDbManager.getInstance(activity).getDownloadList(lang);

                // --
                updateStoryList = compareStoryInsertList(oldStories, newStories);
                if(updateStoryList==null){
                    updateStoryList = new ArrayList<>();
                }

                List<StoryItem> updateStoryTempList = compareStoryUpdateList(oldStories, newStories);
                FileUtils.removeFile(this, updateStoryTempList);
                updateStoryList.addAll(updateStoryTempList);

                if (!updateStoryList.isEmpty()) {
                    initDownLoadResource(lang, StoryDataStatus.StoryData_UPDATE);
                } else {
                    initDownLoadResource(lang, StoryDataStatus.StoryData_OK);
                }

            }
        } else {
            initDownLoadResource(lang,StoryDataStatus.StoryData_NO_SERVICE);
        }

    }

    private void initDownLoadResource(String lang, int type) {

        this.DataStatus = type;

        //-----------------------------------------------------------------------------------------------
        String strListEmpty = null;
        String strListEmptyContentDescription = null;
        //-----------------------------------------------------------------------------------------------
        String strListAction = null;
        String strListActionContentDescription = null;

        if(this.modeTabSelection){
            //--
            strListEmpty = "재생목록이 비었습니다\n오디오 이야기를 추가해 주세요";
            strListEmptyContentDescription = "재생목록이 비었습니다 오디오 이야기를 추가해 주세요";
            //--
            strListAction = "선택하기";
            strListActionContentDescription = strListAction;

        }else{
            //전체
            switch (type) {
                case StoryDataStatus.StoryData_NO_SERVICE:
                    //--
                    strListEmpty = "현재 제공되지 않는 언어 입니다";
                    strListEmptyContentDescription = "현재 제공되지 않는 언어 입니다";
                    //--
                    strListAction = "";
                    strListActionContentDescription = strListAction;
                    break;
                case StoryDataStatus.StoryData_UPDATE:
                    //--
                    strListEmpty = "업데이트 후 사용 가능합니다\n오디오 이야기를 추가해 주세요";
                    strListEmptyContentDescription = "업데이트 후 사용 가능합니다 오디오 이야기를 추가해 주세요";
                    //--
                    strListAction = "업데이트하기";
                    strListActionContentDescription = strListAction;
                    break;
                case StoryDataStatus.StoryData_OK:
                    //현재언어기준 정상상태 (최신상태) mBind.btnDownKoComplete.setVisibility(View.VISIBLE);
                    break;
                case StoryDataStatus.StoryData_DOWNLOAD:
                    //--
                    strListEmpty = "다운로드 후 사용 가능합니다\n오디오 이야기를 추가해 주세요";
                    strListEmptyContentDescription = "다운로드 후 사용 가능합니다 오디오 이야기를 추가해 주세요";
                    //--
                    strListAction = "다운로드하기";
                    strListActionContentDescription = strListAction;
                    break;
                default:
                    break;
            }
        }

        //지정되어있는 페인트 플래그 체크 후, 목적 속성 제거
            /*
            if ((mBind.btnListAction.getPaintFlags() & Paint.UNDERLINE_TEXT_FLAG) > 0){
                mBind.btnListAction.setPaintFlags( mBind.btnListAction.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
            }
            */
        //페인트 속성이 없을때만 추가
        if ((mBind.btnListAction.getPaintFlags() & Paint.UNDERLINE_TEXT_FLAG) > 0){
        }else{
            mBind.btnListAction.setPaintFlags(mBind.btnListAction.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

        //--
        mBind.tvListEmpty.setText(strListEmpty);
        mBind.tvListEmpty.setContentDescription(strListEmptyContentDescription);
        //--
        mBind.btnListAction.setText(strListAction);
        mBind.btnListAction.setContentDescription(strListActionContentDescription);

        //--더미이벤트 연결 ( 가려진 영역 이벤트 방지 )
        mBind.layoutBottomMenu.setOnClickListener(null);

        setRecyclerView();
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

    private void getDownLoadStory(View view, List<StoryItem> data) {

        List<StoryItem> storyList;
        switch (DataStatus) {
            case StoryDataStatus.StoryData_UPDATE:

                fileDownLoadType = TYPE_UPGRADE;
                storyList = data;
                break;
            case StoryDataStatus.StoryData_DOWNLOAD:

                fileDownLoadType = TYPE_DOWNLAOD;
                storyList = data;
                break;
            default:
                DialogUtil.showWarning(activity, R.string.taxi_info, R.string.taxi_network_error, R.string.taxi_ok);
                return;
        }

        //--
        if (NetworkUtil.isWifiConnected(activity) || NetworkUtil.isNetworkConnected(activity)) {
            getDownlaod(storyList);
        } else {
            DialogUtil.showWarning(activity, R.string.taxi_info, R.string.taxi_network_error, R.string.taxi_ok);
        }
    }

    //택시 메인에 있던 기능 7 (현재 엑티비티에 맞도록 조건 처리 변경)
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

            DialogUtil.showDownLoad_V2(activity, mHandler, fileDownLoadType, R.string.taxi_download, R.string.taxi_download_message, stories, R.string.taxi_cancel, () -> {

            });

        } else {
            DialogUtil.showWarning(activity, R.string.taxi_info, R.string.taxi_storage_limit_error, R.string.taxi_ok);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {

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

            super.handleMessage(msg);
        }
    };

    @Override
    public void onItemClick(View view, int position) {
        //adapter 내부에서 이 인터페이스 호출하지않도록 처리
        /*
        adapter.getItem(position).isSelected = !adapter.getItem(position).isSelected;
        if (adapter.getItem(position).isSelected) {
            view.setBackgroundColor(Color.parseColor("#F7F8FF"));
        } else {
            view.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
        adapter.notifyItemChanged(position);
        check();
        */
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public void onControlClick_Action_Location(View view, int position) {
        StoryItem item = adapter.getItem(position);
        if(item!=null){

            //---------------------------------
            Intent intent = new Intent(this, TaxiStoryLocationPopupActivity.class);
            intent.putExtra("lang", lang);//(옵션)
            //StoryItem객체(Serializable) 천달
            intent.putExtra("StoryItem",item);
            startActivity(intent);
            //전환효과
            overridePendingTransition(0,0);


        }
    }

    @Override
    public void onControlClick_Action_Play(View view, int position) {
        StoryItem item = adapter.getItem(position);
        if(item!=null){

            //----------------------------------------------------------
            //List<StoryItem> items2 = adapter.getSelectList();
            List<StoryItem> items2 = new ArrayList<>();
            items2.add(item);

            StoryDbManager.getInstance(this).clearPlayList();
            StoryDbManager.getInstance(this).addPlayList("D", items2);

            Intent intent = new Intent(this, TaxiDriverPlayer.class);
            intent.putExtra("position", 0);
            intent.putExtra("title",mBind.tvTaxiTitle.getText().toString());//추가
            startActivity(intent);
            //----------------------------------------------------------

        }
    }

    @Override
    public void onControlClick_Action_Add(View view, int position) {
        StoryItem item = adapter.getItem(position);
        if(item!=null){

            adapter.getItem(position).isSelected = !adapter.getItem(position).isSelected;
            if (adapter.getItem(position).isSelected) {
                //view.setBackgroundColor(Color.parseColor("#F7F8FF"));
            } else {
                //view.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
            adapter.notifyItemChanged(position);
            check_V2();

        }
    }
    //---------------------------------------------------------------------------------------------------------------------------------------------------

    private void check() {
        List<StoryItem> items = adapter.getItemAll();
        int count = 0;

        for (StoryItem item : items) {
            if (item != null && item.isSelected) {
                count++;
            }
        }
        if (items.size() == 0) {
            mBind.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_tx_selectall_dis);
            mBind.btnSelectAllText.setText("전체선택");
            mBind.btnSelectAllText.setTextColor(Color.parseColor("#000000"));
            isSelectAll = false;
            mBind.layoutBottomMenu.setVisibility(View.GONE);
        } else if (count == items.size()) {
            mBind.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_tx_selectall_enb);
            mBind.btnSelectAllText.setText("전체해제");
            mBind.btnSelectAllText.setTextColor(Color.parseColor("#696CFF"));
            isSelectAll = true;
            mBind.tvSelectCount.setText(String.valueOf(count));
            mBind.layoutBottomMenu.setVisibility(View.VISIBLE);
        } else if (count == 0) {
            mBind.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_tx_selectall_dis);
            mBind.btnSelectAllText.setText("전체선택");
            mBind.btnSelectAllText.setTextColor(Color.parseColor("#000000"));
            isSelectAll = false;
            mBind.layoutBottomMenu.setVisibility(View.GONE);
        } else {
            mBind.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_tx_selectall_enb);
            mBind.btnSelectAllText.setText("선택해제");
            mBind.btnSelectAllText.setTextColor(Color.parseColor("#696CFF"));
            isSelectAll = true;
            mBind.tvSelectCount.setText(String.valueOf(count));
            mBind.layoutBottomMenu.setVisibility(View.VISIBLE);
        }
        mBind.tvCount.setText(String.valueOf(adapter.getItemCount()));
    }

    //v2대응 필요동작용
    private void check_V2() {
        List<StoryItem> items = adapter.getItemAll();
        int count = 0;

        for (StoryItem item : items) {
            if (item != null && item.isSelected) {
                count++;
            }
        }
        if (items.size() == 0) {

            isSelectAll = false;
            mBind.layoutBottomMenu.setVisibility(View.GONE);
            //v2용 제어추가
            mBind.tvTabSelectAll.setText("전체선택");
            mBind.tvTabSelectAll.setTextColor(colorTvTabNormal);
        } else if (count == items.size()) {

            isSelectAll = true;
            mBind.tvSelectCount.setText(String.valueOf(count));
            mBind.layoutBottomMenu.setVisibility(View.VISIBLE);
            //v2용 제어추가
            mBind.tvTabSelectAll.setText("전체해제");
            mBind.tvTabSelectAll.setTextColor(colorTvTabSelected);
        } else if (count == 0) {

            isSelectAll = false;
            mBind.layoutBottomMenu.setVisibility(View.GONE);
            //v2용 제어추가
            mBind.tvTabSelectAll.setText("전체선택");
            mBind.tvTabSelectAll.setTextColor(colorTvTabNormal);
        } else {

            isSelectAll = true;
            mBind.tvSelectCount.setText(String.valueOf(count));
            mBind.layoutBottomMenu.setVisibility(View.VISIBLE);
            //v2용 제어추가
            mBind.tvTabSelectAll.setText("선택해제");
            mBind.tvTabSelectAll.setTextColor(colorTvTabSelected);
        }

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_select_all) {
            List<StoryItem> items = adapter.getItemAll();

            if (items.size() == 0) {
                return;
            }

            if (isSelectAll) {
                for (StoryItem item : items) {
                    item.isSelected = false;
                }
                isSelectAll = false;
            } else {
                for (StoryItem item : items) {
                    item.isSelected = true;
                }
                isSelectAll = true;
            }

            ArrayList<StoryItem> deepCopyItem = new ArrayList<>();
            for (StoryItem item : items) {
                try {
                    deepCopyItem.add(item.deepCopy());
                } catch (Exception e) {
                }
            }

            adapter.updateItems(deepCopyItem);
            check();
        } else if (id == R.id.btn_bottom_menu_cancel) {
            List<StoryItem> items = adapter.getItemAll();
            isSelectAll = false;

            ArrayList<StoryItem> deepCopyItem = new ArrayList<>();
            for (StoryItem item : items) {
                try {
                    item.isSelected = false;
                    deepCopyItem.add(item.deepCopy());
                } catch (Exception e) {
                }
            }

            adapter.updateItems(deepCopyItem);
            check();
        } else if (id == R.id.btn_bottom_menu_play) {
            //----------------------------------------------------------
            List<StoryItem> items2 = adapter.getSelectList();

            StoryDbManager.getInstance(this).clearPlayList();
            StoryDbManager.getInstance(this).addPlayList("D", items2);

            Intent intent = new Intent(this, TaxiDriverPlayer.class);
            intent.putExtra("position", 0);
            intent.putExtra("title",mBind.tvTaxiTitle.getText().toString());//추가
            startActivity(intent);

            //----------------------------------------------------------
        } else if (id == R.id.btn_play_all) {
            //기존기능 호출안함
            /*
            List<StoryItem> items2 = adapter.getItemAll();

            StoryDbManager.getInstance(this).clearPlayList();
            StoryDbManager.getInstance(this).addPlayList("D", items2);
            startActivity(new Intent(this, TaxiDriverPlayer.class).putExtra("position", 0));

            List<StoryItem> items = adapter.getItemAll();
            isSelectAll = false;

            ArrayList<StoryItem> deepCopyItem = new ArrayList<>();
            for (StoryItem item : items) {
                try {
                    item.isSelected = false;
                    deepCopyItem.add(item.deepCopy());
                } catch (Exception e) {
                }
            }

            adapter.updateItems(deepCopyItem);
            check();
            */
        } else if(id == R.id.tv_tab_selection){
            setModeTabSelection(true,true);
        } else if(id == R.id.tv_tab_all){
            setModeTabSelection(false,true);
        } else if(id == R.id.tv_tab_select_all){
            //추가
            List<StoryItem> items = adapter.getItemAll();
            if (items.size() > 0) {
                //토글기능
                if (isSelectAll) {
                    for (StoryItem item : items) {
                        item.isSelected = false;
                    }
                    isSelectAll = false;
                } else {
                    for (StoryItem item : items) {
                        item.isSelected = true;
                    }
                    isSelectAll = true;
                }
                adapter.notifyDataSetChanged();
            }
            check_V2();

        } else if(id == R.id.btn_list_action){
            //추가
            //전체
            switch (DataStatus) {
                case StoryDataStatus.StoryData_NO_SERVICE:
                    break;
                case StoryDataStatus.StoryData_UPDATE:

                    //---------------------------
                    if(updateStoryList==null){
                        updateStoryList = new ArrayList<>();
                    }
                    //----------------------------
                    fileDownLoadType = TYPE_UPGRADE;
                    if (updateStoryList.isEmpty()) {
                        if( "ko".equalsIgnoreCase(lang) ){
                            updateStoryList.addAll(data.getKo());
                        }else if( "en".equalsIgnoreCase(lang) ){
                            updateStoryList.addAll(data.getEn());
                        }else if( "cn1".equalsIgnoreCase(lang) ){
                            updateStoryList.addAll(data.getCn());
                        }else if( "jp".equalsIgnoreCase(lang) ){
                            updateStoryList.addAll(data.getJp());
                        }
                    }
                    getDownLoadStory(view, updateStoryList);
                    //---------------------------
                    break;
                case StoryDataStatus.StoryData_OK:
                    break;
                case StoryDataStatus.StoryData_DOWNLOAD:

                    //---------------------------
                    //다운로드 버튼 동작 필요 - 현재 언어에 맞는 객체 맵핑 (서버데이터 객체로부터 스토리목록 데이터 준비)
                    fileDownLoadType = TYPE_DOWNLAOD;
                    if( "ko".equalsIgnoreCase(lang) ){
                        getDownLoadStory(view, data.getKo());
                    }else if( "en".equalsIgnoreCase(lang) ){
                        getDownLoadStory(view, data.getEn());
                    }else if( "cn1".equalsIgnoreCase(lang) ){
                        getDownLoadStory(view, data.getCn());
                    }else if( "jp".equalsIgnoreCase(lang) ){
                        getDownLoadStory(view, data.getJp());
                    }
                    //---------------------------
                    break;
                case StoryDataStatus.StoryData_OK_SELECTION:
                    setModeTabSelection(false,true);
                    break;
                default:
                    break;
            }

        }

    }

    private void requestPermissions() {
        boolean permissionAccessFineLocationApproved = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (permissionAccessFineLocationApproved) {
            requestLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(TaxiStoryListActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
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

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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
                KLog.i("LocationCheckValue", String.format("speed : %s", filtSpeed));
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
