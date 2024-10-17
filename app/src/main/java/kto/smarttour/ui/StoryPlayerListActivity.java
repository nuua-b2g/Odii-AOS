package kto.smarttour.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.socks.library.KLog;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.adapter.StoryListAdapter;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.Common;
import kto.smarttour.common.consts.URLS;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.BottomSheetDialogUtil;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.InnerStorageSingleton;
import kto.smarttour.common.utils.NetworkUtil;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.SimpleItemTouchHelperCallback;
import kto.smarttour.common.utils.StorageUtil;
import kto.smarttour.common.utils.ViewUtils;
import kto.smarttour.databinding.ActivityPlayerListBinding;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.player.PlayListManager;


/**
 * 재생목록
 */
public class StoryPlayerListActivity extends BaseActivity implements StoryListAdapter.OnItemClickListener, View.OnClickListener, StoryListAdapter.OnStartDragListener {

	private ActivityPlayerListBinding mBind;
	private StoryListAdapter adapter;
	private MediaPlayerChangeInfoReceiver receiver;
	private ItemTouchHelper mItemTouchHelper;
	private AppCompatActivity activity;

	private StoryItem editModeLastStoryItem;

	//--
	private MapView mapView;
	private NaverMap mNaverMap;
	//--
	private ArrayList<Marker> arr_Marker = new ArrayList<>();
	private InfoWindow markerInfoWindow;
	private CustomInfoWindowAdapter infoWindowAdapter;
	//private ImageView dumpImageView;
	private Marker markerOnImageReady;
	//===========================================

	private FusedLocationSource locationSource;
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = this;
		mBind = DataBindingUtil.setContentView(this, R.layout.activity_player_list);
		mBind.setLifecycleOwner(this);

		locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

		//===============================================
		if(mapView==null){
			mapView = new MapView(this);
			//xml뷰 배치하지않음
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
			mapView.setLayoutParams(params);

			mBind.viewMapArea.addView(mapView);

			//--
			mapView.onCreate(savedInstanceState);
			mapView.getMapAsync(onMapReadyCallback);
		}
		//===============================================

		((LinearLayout.LayoutParams) mBind.topbar.getLayoutParams()).setMargins(0, statusBarHeight, 0, 0);
		setRecyclerView();

		receiver = new MediaPlayerChangeInfoReceiver();

		IntentFilter mediaChangeInfoIntentFilter = new IntentFilter();
		mediaChangeInfoIntentFilter.addAction(PlayerConstants.ACTION_PLAY_INFO);
		mediaChangeInfoIntentFilter.addAction(PlayerConstants.ACTION_SHOW_MINI_PLAYER);
		mediaChangeInfoIntentFilter.addAction(PlayerConstants.ACTION_HIDE_MINI_PLAYER);
		mediaChangeInfoIntentFilter.addAction(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE);

		mediaChangeInfoIntentFilter.addAction(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE_EDIT);

		mediaChangeInfoIntentFilter.addAction(PlayerConstants.ACTION_PLAYER_STATUS_PLAY);

		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, mediaChangeInfoIntentFilter);

		String thumbnailFilePath = "";
		StoryItem item = PlayListManager.getInstance().getStoryItem();
		if (item!=null) {
			thumbnailFilePath = item.thumbnailFilePath;
			if(thumbnailFilePath == null){
				thumbnailFilePath = "";
			}
		}

		//------------- v2 ( v1관련 gone처리 )-----------------------------------------
		//v2, 전체선택/전체해제 (all_select/all_select_cancel)
		mBind.btnSelectAllV2.setOnClickListener(v -> {

			if(adapter==null){
				return;
			}
			StoryListAdapter.LIST_MODE mode = adapter.getMode();

			//--
			if( mode != StoryListAdapter.LIST_MODE.EDIT )
			{
				if(adapter!=null){
					//--
					AnalyticsInterface.getInstance().logEvent(activity, "play_list_edit_select_all");
					adapter.setMode(StoryListAdapter.LIST_MODE.EDIT);

					PlayerService.startActionPause_Edit(activity); //일시정시
					//사용자가 의도적으로 일시정시한게 아님, 이벤트로그 전송x AnalyticsInterface.getInstance().logEvent(this, "mini_player_play");

					setSelectedCancel(true);
				}

			}
			else
			{
				if(adapter!=null){
					//==
					AnalyticsInterface.getInstance().logEvent(activity, "play_list_edit_deselect_all");
					adapter.setMode(StoryListAdapter.LIST_MODE.NORMAL);

					setSelectedCancel(false);
				}

			}
			//*******************************************************************************************************************************************************************************

		});
		//v2, 선택삭제 (delete_selected)
		mBind.btnDeleteSelectedV2.setOnClickListener(v -> {

			List<StoryItem> checkDeleteList = getCheckList();
			if (!checkDeleteList.isEmpty()) {

				//삭제대상 데이터처리
				StoryItem currentSongItem = adapter.getItem(adapter.getCurrentPosition());
				for(StoryItem deleteItem : checkDeleteList) {
					StoryDbManager.getInstance(activity).removePlayList(deleteItem.seq);
					adapter.removeItem(deleteItem);
				}
				//=====
				PlayListManager.getInstance().load();
				adapter.updateItems(StoryDbManager.getInstance(activity).getPlayList().story);
				ArrayList<StoryItem> items2 = adapter.getItemAll();
				boolean isMatch = false;
				for(int i = 0; i < items2.size();i++) {
					if(currentSongItem.seq == items2.get(i).seq) {
						adapter.setCurrentPosition(i);
						PlayListManager.getInstance().setPlayIndex(i);
						isMatch = true;
						break;
					}
				}
				//=====
				if(!isMatch) {
					PlayerService.startActionStop(activity);

					adapter.setCurrentPosition(0);
					PlayListManager.getInstance().setPlayIndex(0);

					//if(PlayListManager.getInstance().getListSize() > 0) {
						//대체주석 ImageUtil.loadImage(mBind.ivThumb, PlayListManager.getInstance().getStoryItem(0).thumbnailFilePath, null);
					//}
				}
				//=====
			}

			mBind.count.setText(String.valueOf(adapter.getItemCount()));
			//v2편집모드 해제
			editModeLastStoryItem = null;
			//선택아이템 해제
			if(adapter!=null){
				//==
				//AnalyticsInterface.getInstance().logEvent(activity, "play_list_edit_complete");
				adapter.setMode(StoryListAdapter.LIST_MODE.NORMAL);
				setSelectedCancel(false);
			}


		});
		//v2, 순서변경/변경저장 (playlist_change_order/playlist_save_order)
		mBind.btnReorderDragV2.setOnClickListener(v -> {

			StoryListAdapter.LIST_MODE mode = adapter.getMode();

			//boolean isEmptyCheckList = (getCheckList().size()>0)?false:true;
			//순서변경모드
			if( mode != StoryListAdapter.LIST_MODE.REORDER)
			{
				if(adapter!=null){
					//--
					AnalyticsInterface.getInstance().logEvent(activity, "play_list_edit_select_all");
					adapter.setMode(StoryListAdapter.LIST_MODE.REORDER);

					PlayerService.startActionPause_Edit(activity); //일시정시 호출
					//사용자가 의도적으로 일시정시한게 아님 이벤트로그 전송x AnalyticsInterface.getInstance().logEvent(this, "mini_player_play");

					setSelectedCancel(false);
				}
			}
			else
			{
				//선택아이템 해제
				if(adapter!=null){
					//==
					AnalyticsInterface.getInstance().logEvent(activity, "play_list_edit_deselect_all");
					adapter.setMode(StoryListAdapter.LIST_MODE.NORMAL);

					setSelectedCancel(false);
				}
			}

			editModeLastStoryItem = null;
		});


		//--
		mBind.btnShowMap.setText(getString(R.string.playlist_show_map)); //지도보기 이동 기능으로 표기 (아이콘도 필요시 추가변경)
		mBind.ivShowMap.setImageResource(R.drawable.ic_player_list_map_v2);
		//--
		mBind.btnShowMap.setOnClickListener(v -> {

			if (adapter==null){
				return;
			}
			if ( (adapter.getItemAll() == null) || adapter.getItemAll().isEmpty() ) {
				return;
			}
			//--
			int visibility = mBind.layoutMapShowing.getVisibility();
			if(visibility!=View.VISIBLE){
				mBind.layoutMapShowing.setVisibility(View.VISIBLE);
				mBind.btnShowMap.setText(getString(R.string.playlist_show_list));
				mBind.ivShowMap.setImageResource(R.drawable.ic_player_map_list_v2);

				mBind.layoutPlay.setVisibility(View.GONE);
				if(mNaverMap!=null){
					action_AddPoi_onNaverMap(mNaverMap);
				}

			}else{
				mBind.layoutMapShowing.setVisibility(View.GONE);
				mBind.btnShowMap.setText(getString(R.string.playlist_show_map));
				mBind.ivShowMap.setImageResource(R.drawable.ic_player_list_map_v2);


				mBind.layoutPlay.setVisibility(View.VISIBLE);
			}
		});

		mBind.btnClose.setOnClickListener(this);
		//======

		mBind.toastMenu.findViewById(R.id.btn_toast_locker).setOnClickListener(this);
		mBind.toastMenu.findViewById(R.id.btn_toast_del).setOnClickListener(this);
		mBind.toastMenu.findViewById(R.id.btn_toast_cancel).setOnClickListener(this);


		if (adapter.isEmpty()) {
			mBind.emptyView.setVisibility(View.VISIBLE);
			mBind.viewMiniPlayer.setVisibility(View.GONE);

			PlayerService.startActionStop(this);
		} else {
			mBind.emptyView.setVisibility(View.GONE);

			PlayListManager playListManager = PlayListManager.getInstance();
			adapter.setCurrentPosition(PlayListManager.getInstance().getPlayIndex());



		}
		mBind.count.setText(String.valueOf(adapter.getItemCount()));
		AnalyticsInterface.getInstance().screenView(this, "player_list");

		//--
		setViewMiniPlayer(mBind.viewMiniPlayer);

		//---
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				int playIndex = PlayListManager.getInstance().getPlayIndex();
				int listSize = PlayListManager.getInstance().getListSize();
				if (listSize == 0) {
					mBind.emptyView.setVisibility(View.VISIBLE);
					mBind.viewMiniPlayer.setVisibility(View.GONE);
				} else {

					//데이터는 존재하고, 목표인덱스가 범위내 이면
					if(!PlayerService.checkPlay()) {
						if ( (listSize > playIndex) && (playIndex >= 0) ) {

							adapter.notifyDataSetChanged();

							PlayerService.startActionPause(activity); //일시정시 호출

						} else {

							PlayListManager.getInstance().setPlayIndex(0);
							adapter.setCurrentPosition(0);

							adapter.notifyDataSetChanged();

							PlayerService.startActionPause(activity); //일시정시 호출

						}
					}

				}

			}
		});

		ViewCompat.setAccessibilityDelegate(mBind.list, new RecyclerViewAccessibilityDelegate(mBind.list) {
			@Override
			public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
				super.onInitializeAccessibilityNodeInfo(host, info);

				//편집모드 진입버튼과, 하단팝업 편집동작 뷰를 사용하지않으므로 주석처리.ㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇ

				//구 isEdit 일때
				/* 대체전
					if(mBind.viewPlayer.getVisibility() == View.VISIBLE) {
						info.setTraversalBefore(mBind.toastMenu);
					} else {
						info.setTraversalBefore(mBind.btnPlayListPrev);
					}
				*/

				/*
				if(adapter.isEdit()) {
					if(mBind.viewMiniPlayer.getVisibility() == View.VISIBLE) {
						info.setTraversalBefore(mBind.toastMenu);
					} else {
						//info.setTraversalBefore(mBind.btnPlayListPrev);
					}


					info.setTraversalAfter(mBind.btnEditComplete);
				} else {
					//대체주석 info.setTraversalBefore(mBind.btnPlayListPrev);

					info.setTraversalAfter(mBind.btnEdit);
				}
				*/
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   @NonNull String[] permissions,  @NonNull int[] grantResults) {
		if (locationSource.onRequestPermissionsResult(
				requestCode, permissions, grantResults)) {
			if (!locationSource.isActivated()) { // 권한 거부됨
				mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
			}
			return;
		}
		super.onRequestPermissionsResult(
				requestCode, permissions, grantResults);
	}

	//onNewIntent
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (intent != null) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (adapter != null) {
						adapter.updateItems(StoryDbManager.getInstance(StoryPlayerListActivity.this).getPlayList().story);
					}
				}
			});

		}
	}

	private void makeDumpView() {
		RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams) mBind.dumpView.getLayoutParams();
		rl.height = statusBarHeight;
		mBind.dumpView.setLayoutParams(rl);
		mBind.dumpView.setBackgroundColor(changeAlpha(Color.parseColor("#353B49"), 0.9f));
	}

	public int changeAlpha(int color, float fraction) {
		int red = Color.red(color);
		int green = Color.green(color);
		int blue = Color.blue(color);
		int alpha = (int) (Color.alpha(color) * fraction);
		return Color.argb(alpha, red, green, blue);
	}

	private void setRecyclerView() {
		//mBind.list.addItemDecoration(new SimpleDividerItemDecoration(this, SimpleDividerItemDecoration.DIVIDER_TOP2_END_MARGIN));
		//mBind.list.addItemDecoration(new SimpleDividerItemDecoration(this, SimpleDividerItemDecoration.DIVIDER_END_MARGIN));

		adapter = new StoryListAdapter(this);
		mBind.list.setAdapter(adapter);
		adapter.setOnItemClickListener(this);
		mItemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(adapter));
		mItemTouchHelper.attachToRecyclerView(mBind.list);

		adapter.updateItems(StoryDbManager.getInstance(this).getPlayList().story);
		adapter.setOnStartDragListener(this);
	}

	private static class CustomInfoWindowAdapter extends InfoWindow.ViewAdapter {

		private final Context mContext;
		private Object imageObject;

		public CustomInfoWindowAdapter(@NonNull Context context) {
			mContext = context;
		}

		public void setImageObject(Object imageObject){
			this.imageObject = imageObject;
		}

		@NonNull
		@Override
		public View getView(@NonNull InfoWindow infoWindow) {
			View view = View.inflate(mContext, R.layout.naver_custom_info_window, null);

			TextView txtTitle = (TextView) view.findViewById(R.id.tv_title);
			ImageView imagePoint = (ImageView) view.findViewById(R.id.iv_thumbnail);

			//--
			Object obj_markerOverlay = infoWindow.getTag();
			if(obj_markerOverlay !=null ){
				if(obj_markerOverlay instanceof Marker){
					Marker markerOverlay = (Marker)obj_markerOverlay;

					Object obj_storyItem = markerOverlay.getTag();
					if(obj_storyItem !=null ){

						if(obj_storyItem instanceof StoryItem){
							StoryItem item = (StoryItem)obj_storyItem;

							txtTitle.setText(item.title);
							if(imageObject!=null){

								if(imageObject instanceof Drawable){
									//BitmapDrawable
									imagePoint.setImageDrawable( (BitmapDrawable)imageObject );

								}

							}


						}

					}

				}
			}

			return view;
		}
	}

	//--
	private void action_AddPoi_onNaverMap(NaverMap naverMap){

		if(naverMap==null){
			return;
		}

		double currentZoom = naverMap.getCameraPosition().zoom;

		CameraPosition init_position = new CameraPosition( new LatLng(37.324276, 127.990001) , currentZoom);
		CameraUpdate.toCameraPosition(init_position);
		//--
		if(arr_Marker==null){
			arr_Marker = new ArrayList<>();
		}
		//제거 (네이버 마커 com.naver.maps.map.overlay.Marker )
		for(int i=0; i<arr_Marker.size(); i++){
			arr_Marker.get(i).setMap(null);
		}
		arr_Marker.clear();

		//-------------------
		if(adapter==null){
			return;
		}
		ArrayList<StoryItem> items = adapter.getItemAll();
		if(items == null){
			return;
		}
		//-------------------
		//=============================================================

		int boundaryCount = 0;
		//--
		double north = -1.0f;
		double south = -1.0f;
		double west = -1.0f;
		double east = -1.0f;
		//--
		LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

		PointF anchor = new PointF(0.5f,1.0f);
		OverlayImage icon = OverlayImage.fromResource(R.drawable.ic_player_map_ic_marker); // 44x63px
		int px_icon_w = ViewUtils.dp2px(44/2);
		int px_icon_h = ViewUtils.dp2px(63/2);

		for(int i=0; i<items.size(); i++){

			StoryItem item = items.get(i);
			try {
				double d_lat = Double.parseDouble(item.posY);
				double d_lon = Double.parseDouble(item.posX);

				if( d_lat>10.0 && d_lon>10.0f){

					LatLng position = new LatLng(d_lat, d_lon);

					//========================================================================================
					if(i==0){
						//--
						north = d_lat;
						south = d_lat;
						east = d_lon;
						west = d_lon;
					}else{
						//--
						if (north < d_lat) {
							north = d_lat;
						}
						if (south > d_lat) {
							south = d_lat;
						}
						if (east < d_lon) {
							east = d_lon;
						}
						if (west > d_lon) {
							west = d_lon;
						}
					}
					boundaryCount++;
					boundsBuilder.include(position);
					//========================================================================================
					//--
					Marker marker = new Marker();
					marker.setPosition(position);
					marker.setAnchor( anchor );
					//--
					marker.setIcon( icon );
					//--
					//marker.setCaptionText("" + i);
					marker.setWidth(px_icon_w);
					marker.setHeight(px_icon_h);
					//--
					marker.setTag(item); //StoryItem
					//--
					marker.setOnClickListener(new Overlay.OnClickListener() {
						@Override
						public boolean onClick(@NonNull Overlay overlay) {
							//---------------------------
							Marker markerOverlay = (Marker)overlay;
							openInfoWindowOnImageReady(markerOverlay);
							//---------------------------
							return true;
						}
					});
					//--
					marker.setMap(naverMap);
					//--
					arr_Marker.add(marker);
					//========================================================================================

				}

			} catch (NumberFormatException e) {
				//e.printStackTrace();
			}


		}

		if(boundaryCount>0){
			if(boundaryCount>1){
				//--
				int px_padding = ViewUtils.dp2px(40);

				CameraUpdate cameraUpdate = CameraUpdate.fitBounds( boundsBuilder.build(), px_padding).animate(CameraAnimation.None);
				mNaverMap.moveCamera(cameraUpdate);
			}else{
				//--
				CameraUpdate cameraUpdate = CameraUpdate.scrollTo( new LatLng(north,west) ).animate(CameraAnimation.None);
				mNaverMap.moveCamera(cameraUpdate);
			}
		}

		//--
		StoryItem currentSongItem = adapter.getItem(adapter.getCurrentPosition());
		Marker marker_Matched = null;

		for(int i=0; i<arr_Marker.size(); i++){
			Marker marker = arr_Marker.get(i);

			Object obj_StoryItem = marker.getTag();
			if(obj_StoryItem!=null){
				if(obj_StoryItem instanceof StoryItem){
					StoryItem item = (StoryItem)obj_StoryItem;
					if(currentSongItem.seq == item.seq) {

						//idx_isMatched = i;
						marker_Matched = marker;

						break;
					}

				}
			}

		}
		if(marker_Matched!=null)
		{
			openInfoWindowOnImageReady(marker_Matched);
		}

	}

	private void openInfoWindowOnImageReady(Marker markerOverlay){
		if (markerOverlay==null){
			return;
		}
		markerOnImageReady = markerOverlay;
		if (markerOverlay.getInfoWindow() == null) {

			if(markerInfoWindow!=null){
				//ß닫기
				markerInfoWindow.close();
			}

			StoryItem item = (StoryItem)markerOverlay.getTag(); //StoryItem

			//iv_dumpimage_for_infowindow
			Glide.with(StoryPlayerListActivity.this).load(item.thumbnailFilePath).fitCenter().addListener(listenerIntroLoad).into(mBind.ivDumpimageForInfowindow);

		} else {
			// 이미 현재 마커에 정보 창이 열려있을 경우 닫음
								/*
								if(markerInfoWindow!=null){
									//기존 인포윈도우가 있으면 닫기
									markerInfoWindow.close();

									markerInfoWindow = null;
								}
								*/
		}

	}

	private void openInfoWindowOnImageResult(Object imageObject){

		Marker markerOverlay = markerOnImageReady;
		if(markerOverlay==null){
			return;
		}

		//--
		markerInfoWindow.setTag( markerOverlay );
		infoWindowAdapter.setImageObject(imageObject);
		//--
		markerInfoWindow.open(markerOverlay, Align.Top);
		//--
		markerInfoWindow.setOnClickListener(new Overlay.OnClickListener() {
			@Override
			public boolean onClick(@NonNull Overlay overlay) {

				//StoryItem
				Object obj_markerOverlay = overlay.getTag();
				if(obj_markerOverlay !=null ){
					if(obj_markerOverlay instanceof Marker){
						Marker markerOverlay = (Marker)obj_markerOverlay;
						Object obj_storyItem = markerOverlay.getTag();
						if(obj_storyItem!=null && (obj_storyItem instanceof StoryItem) ){
							StoryItem item = (StoryItem)obj_storyItem;
							OnClickDetail(null,item);
						}

					}
				}
				return true;
			}
		});

		//--
		if(mNaverMap!=null){
			CameraUpdate cameraUpdate = CameraUpdate.scrollTo( markerOverlay.getPosition() ).animate(CameraAnimation.Easing);
			mNaverMap.moveCamera(cameraUpdate);
		}
	}

	private RequestListener listenerIntroLoad = new RequestListener() {
		@Override
		public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
			openInfoWindowOnImageResult(null);
			return false;
		}

		@Override
		public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
			openInfoWindowOnImageResult(resource);
			return false;
		}
	};

	//--
	NaverMap.OnMapClickListener onMapClickListener = new NaverMap.OnMapClickListener() {
		@Override
		public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
			if(markerInfoWindow!=null){
				markerInfoWindow.close();
			}
		}
	};
	//--
	OnMapReadyCallback onMapReadyCallback = new OnMapReadyCallback() {
		@Override
		public void onMapReady(@NonNull @NotNull NaverMap naverMap) {
			mNaverMap = naverMap;
			mNaverMap.setOnMapClickListener(onMapClickListener);

			mNaverMap.setLocationSource(locationSource);
			mNaverMap.getUiSettings().setLogoGravity(Gravity.START|Gravity.BOTTOM);
			mNaverMap.getUiSettings().setLocationButtonEnabled(true);

			//인포윈도우 생성
			markerInfoWindow = new InfoWindow();
			infoWindowAdapter = new CustomInfoWindowAdapter(StoryPlayerListActivity.this);
			markerInfoWindow.setAdapter(infoWindowAdapter);

			//@NonNull NaverMap 객체
			action_AddPoi_onNaverMap(naverMap);

		}
	};

	@Override
	protected  void onStart() {
		super.onStart();

		if(mapView!=null){
			mapView.onStart();
		}
	}
	@Override
	protected void onResume() {
		//
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

		if(mapView!=null){
			mapView.onResume();
		}

		if (Common.isDetailActivityShowing) {
			if(mBind.layoutMapShowing.getVisibility()==View.VISIBLE){
				mBind.layoutMapShowing.setVisibility(View.GONE);
				mBind.btnShowMap.setText(getString(R.string.playlist_show_map));
				mBind.ivShowMap.setImageResource(R.drawable.ic_player_list_map_v2);
				mBind.layoutPlay.setVisibility(View.VISIBLE);
			}

			Common.isDetailActivityShowing = false;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(mapView!=null){
			mapView.onPause();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(mapView!=null){
			mapView.onStop();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

		if(mapView!=null){
			mapView.onDestroy();
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mapView!=null){
			mapView.onSaveInstanceState(outState);
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		if(mapView!=null){
			mapView.onLowMemory();
		}
	}

	@Override
	public void OnClickMore(View view, StoryItem storyItem) {
		AnalyticsInterface.getInstance().logEvent(activity, "play_list_menu");
		BottomSheetDialogUtil.showPlayerMenuDialog(StoryPlayerListActivity.this, result -> {
			switch (result) {
				case BottomSheetDialogUtil.TOAST_MENU_PLAYER_DETAIL:

					//--
					StoryItem content = storyItem;
					Intent contentIntent = new Intent(this, StoryDetailWebActivity.class);
					contentIntent.putExtra("content", content);
					contentIntent.putExtra("useLocation", true);
					//추가
					//contentIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(contentIntent);

					break;
				case BottomSheetDialogUtil.TOAST_MENU_PLAYER_LOCKER:
					StoryData data = new StoryData();
					data.story.add(storyItem);
					Intent lockerIntent = new Intent(StoryPlayerListActivity.this, StoryLockerActivity.class);
					lockerIntent.putExtra("type", StoryLockerActivity.FOLDER_SELECT_TYPE);
					InnerStorageSingleton.getSingleton().setData(data);
					startActivity(lockerIntent);
					break;
				case BottomSheetDialogUtil.TOAST_MENU_PLAYER_DOWNLOAD:
					StoryData downloadData = new StoryData();
					downloadData.story.add(storyItem);
					if (!NetworkUtil.isNetworkConnected(this)) { // 인터넷 미접속
						DialogUtil.showWarning(this, R.string.ok, R.string.network_error, R.string.ok);
					} else if (NetworkUtil.isWifiConnected(this) && NetworkUtil.isNetworkConnected(this)) { // 와이파이 접속
						getDownload(downloadData);
					} else if (SettingsUtil.isUseDataNetwork(this) && !NetworkUtil.isWifiConnected(this)) { //데이터 사용O 와이파이 접속X
						DialogUtil.showWarning(this, R.string.ok, R.string.network_use_message, R.string.ok, R.string.cancel, () -> {
							getDownload(downloadData);
						}, () -> {

						});
					} else if (!SettingsUtil.isUseDataNetwork(this) && !NetworkUtil.isWifiConnected(this)) { //데이터 사용X 와이파이 접속X
						DialogUtil.showWarning(this, R.string.ok, R.string.network_use_message2, R.string.ok, R.string.cancel, () -> {
							getDownload(downloadData);
						}, () -> {

						});
					}
					break;
			}
		});
	}

	//--
	@Override
	public void OnClickDetail(View view, StoryItem storyItem) {

		if(storyItem!=null){
			//--
			StoryItem content = storyItem;
			Intent contentIntent = new Intent(this, StoryDetailWebActivity.class);
			contentIntent.putExtra("content", content);
			contentIntent.putExtra("useLocation", true);
			//추가
			//contentIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(contentIntent);

			Common.isDetailActivityShowing = true;
		}

	}

	private void getDownload(StoryData downloadData) {
		if ((StorageUtil.GetAvailableInternalMemorySize() - downloadData.story.get(0).audioFileSize) >= 500000000) {
			DialogUtil.showDownLoad(StoryPlayerListActivity.this, R.string.download, R.string.download_message, downloadData, R.string.cancel, () -> {
				((MainActivity) OdiiApplication.getWebActivity()).refreshDownloadCount();
			});
		} else {
			DialogUtil.showWarning(OdiiApplication.getWebActivity(), R.string.ok, R.string.storage_limit_error, R.string.ok);
		}
	}

	@Override
	public void onItemClick(View view, int position) {
		if (position == PlayListManager.getInstance().getPlayIndex()) {
			//--
			if (!PlayerService.isPlay) {
				//AnalyticsInterface.getInstance().logEvent(activity, "player_play");
				PlayerService.startActionPlay(this);
				return;
			}
			return;
		}
		//--
		AnalyticsInterface.getInstance().logEvent(activity, "play_list_play");
//		adapter.notifyItemChanged(PlayListManager.getInstance().getPlayIndex());
		PlayListManager.getInstance().setPlayIndex(position);

		adapter.notifyItemChanged(PlayListManager.getInstance().getPlayIndex());
		PlayerService.startActionPlayIndex(this);
	}

	@Override
	public void onItemDeleteClick(View view, int position) {
		AnalyticsInterface.getInstance().logEvent(activity, "play_list_delete");

		int playIndex = PlayListManager.getInstance().getPlayIndex();
		StoryItem deleteItem = adapter.getItem(position);

		adapter.onItemDismiss(position);
		PlayListManager.getInstance().remove(position);

		StoryDbManager.getInstance(this).removePlayList(deleteItem.seq);

		if (PlayListManager.getInstance().getListSize() == 0) {
			PlayerService.startActionStop(this);
			mBind.emptyView.setVisibility(View.VISIBLE);
			//대체 mBind.viewPlayer.setVisibility(View.GONE);
			mBind.viewMiniPlayer.setVisibility(View.GONE);

		} else {
			if (playIndex > position) {
				playIndex -= 1;
				PlayListManager.getInstance().setPlayIndex(playIndex);
				adapter.setCurrentPosition(playIndex);
			} else if (playIndex == position) {

				if (PlayListManager.getInstance().getListSize() <= playIndex) {
					PlayListManager.getInstance().setPlayIndex(0);
					PlayerService.startActionStop(this);
				} else {
					PlayListManager.getInstance().setPlayIndex(playIndex);
					PlayerService.startActionPlayIndex(this);
				}
			}
		}

		adapter.notifyDataSetChanged();
		mBind.count.setText(String.valueOf(adapter.getItemCount()));
	}

	@Override
	public void onItemSelect(View view, int position) {
		check();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_add:
				String[] value = {getString(R.string.fill_msg), getString(R.string.story_search_msg)};
				AnalyticsInterface.getInstance().logEvent(activity, "play_list_add");
				DialogUtil.showChoice(this, getString(R.string.toast_add), getString(R.string.story_list_add_message), value, getString(R.string.cancel), result -> {
					if (result == 0) {
						Intent intent = new Intent(StoryPlayerListActivity.this, StoryLockerActivity.class);
						intent.putExtra("type", StoryLockerActivity.FOLDER_PLAY_TYPE);
						startActivity(intent);
					} else if (result == 1) {
						Intent storySearchWebIntent = new Intent(activity, MainActivity.class);
						storySearchWebIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
						storySearchWebIntent.putExtra("url", URLS.SEARCH_URL);
						startActivity(storySearchWebIntent);
						finish();
					}
				});
				break;
			case R.id.btn_locker:
				if (StoryDbManager.getInstance(StoryPlayerListActivity.this).getPlayList().story.isEmpty()) {
					return;
				}
				AnalyticsInterface.getInstance().logEvent(activity, "play_list_fill");
				Intent intent = new Intent(StoryPlayerListActivity.this, StoryLockerActivity.class);
				intent.putExtra("type", StoryLockerActivity.FOLDER_SELECT_TYPE);
				InnerStorageSingleton.getSingleton().setData(StoryDbManager.getInstance(StoryPlayerListActivity.this).getPlayList());
				startActivity(intent);
				break;
			case R.id.btn_close:
				finish();
				break;
			case R.id.btn_sort:
				if (adapter.getItemAll().isEmpty()) {
					return;
				}
				String[] items = {getString(R.string.msg_sort_1), getString(R.string.msg_sort_2), getString(R.string.msg_sort_3), getString(R.string.msg_sort_4)};
				DialogUtil.showChoice(this, getString(R.string.msg_sort), getString(R.string.msg_sort_change_complete), items, getString(R.string.cancel), result -> {
					adapter.setSort(result);
				});
				AnalyticsInterface.getInstance().logEvent(activity, "play_list_edit_sort");
				break;

			case R.id.btn_toast_locker:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_fill");
				List<StoryItem> checkList = getCheckList();
				StoryData dataLocker = new StoryData();
				for (StoryItem item : checkList) {
					if (item != null) {
						dataLocker.story.add(item);
					}
				}
				Intent lockerIntent = new Intent(this, StoryLockerActivity.class);
				lockerIntent.putExtra("type", StoryLockerActivity.FOLDER_SELECT_TYPE);
				InnerStorageSingleton.getSingleton().setData(dataLocker);
				startActivity(lockerIntent);
				setSelectedCancel(false);
				break;
			case R.id.btn_toast_del:
				List<StoryItem> checkDeleteList = getCheckList();

				if (!checkDeleteList.isEmpty()) {
					DialogUtil.showWarning(activity, getString(R.string.confirm), getString(R.string.msg_select_story_delete), getString(R.string.confirm), getString(R.string.cancel), () -> {

						StoryItem currentSongItem = adapter.getItem(adapter.getCurrentPosition());
						for(StoryItem deleteItem : checkDeleteList) {
							StoryDbManager.getInstance(activity).removePlayList(deleteItem.seq);
							adapter.removeItem(deleteItem);
						}

						PlayListManager.getInstance().load();
						adapter.updateItems(StoryDbManager.getInstance(activity).getPlayList().story);
						ArrayList<StoryItem> items2 = adapter.getItemAll();
						boolean isMatch = false;
						for(int i = 0; i < items2.size();i++) {
							if(currentSongItem.seq == items2.get(i).seq) {
								adapter.setCurrentPosition(i);
								PlayListManager.getInstance().setPlayIndex(i);
								isMatch = true;
								break;
							}
						}
						if(!isMatch) {
							PlayerService.startActionStop(activity);
							adapter.setCurrentPosition(0);
							PlayListManager.getInstance().setPlayIndex(0);

						}
						check();
						mBind.count.setText(String.valueOf(adapter.getItemCount()));
					}, () -> {
						setSelectedCancel(false);
					});


				}
				break;

		}
	}

	@Override
	public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
		editModeLastStoryItem = adapter.getItem(adapter.getCurrentPosition());
		mItemTouchHelper.startDrag(viewHolder);
	}

	@Override
	public void onEndDrag(int fromPosition, int toPosition) {
		if (editModeLastStoryItem != null) {
			int i = 0;
			for (StoryItem item : adapter.getItemAll()) {
				if (editModeLastStoryItem.seq == item.seq) {
					KLog.i("onEnddrag", String.format("fromPosition : %s   toPosition : %s", fromPosition, toPosition));
					adapter.setCurrentPosition(i);
					PlayListManager.getInstance().setPlayIndex(i);
					break;
				}
				i++;
			}
		}

		ArrayList<StoryItem> saveData = adapter.getItemAll();
		StoryDbManager.getInstance(activity).clearPlayList();
		StoryDbManager.getInstance(activity).addPlayList(saveData);

		PlayListManager.getInstance().load();
		adapter.updateItems(StoryDbManager.getInstance(activity).getPlayList().story);
		editModeLastStoryItem = adapter.getItem(PlayListManager.getInstance().getPlayIndex());
	}

	private class MediaPlayerChangeInfoReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (PlayerConstants.ACTION_PLAY_INFO.equals(intent.getAction())) {
				try {
					int index = PlayListManager.getInstance().getPlayIndex();
					adapter.setCurrentPosition(index);

				} catch (Exception e) {
					finish();
				}

			} else if (intent.getAction().equals(PlayerConstants.ACTION_SHOW_MINI_PLAYER)) {
				mBind.viewMiniPlayer.setVisibility(View.VISIBLE);

			} else if (intent.getAction().equals(PlayerConstants.ACTION_HIDE_MINI_PLAYER)) {
				mBind.viewMiniPlayer.setVisibility(View.GONE);

			} else if (intent.getAction().equals(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE)) {
			} else if (intent.getAction().equals(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE_EDIT)) {
				//편집모드 진입용 일시정지 호출시 수신
				mBind.viewMiniPlayer.setVisibility(View.GONE);

			} else if (intent.getAction().equals(PlayerConstants.ACTION_PLAYER_STATUS_PLAY)) {
			}
		}
	}

	private void applyCheckResult(StoryListAdapter.LIST_MODE mode){

		runOnUiThread(() -> {

			//===========================
			mBind.toastMenu.setVisibility(View.GONE);
			mBind.btnDeleteSelectedV2.setVisibility(View.GONE);
			mBind.tvSelectAllV2.setText(getString(R.string.all_select));//전체선택
			mBind.tvReorderDragV2.setText(getString(R.string.playlist_change_order));//순서변경

			mBind.viewMiniPlayer.setVisibility(View.GONE);
			//===========================

			if(mode == StoryListAdapter.LIST_MODE.NORMAL)
			{
				//공통(선택편집, 순서변경) 미디어플레이어 숨김
				mBind.viewMiniPlayer.setVisibility(View.VISIBLE);
				//현재 모드와 관계없이 일반모드로 진입 (텍스트, description 디폴트 )
				//---------------------------------------------------
				mBind.btnSelectAllV2.setContentDescription("전체 해제됨");
				mBind.btnSelectAllV2.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);

				//리스트모드 변경 -> 노멀
				adapter.setMode(StoryListAdapter.LIST_MODE.NORMAL);

			}
			else
			{
				//-------------------------------------------------------
				if(mode == StoryListAdapter.LIST_MODE.EDIT)
				{
					//부분 적용
					mBind.btnDeleteSelectedV2.setVisibility(View.VISIBLE);
					mBind.tvSelectAllV2.setText(getString(R.string.all_select_cancel));//전체해재
					mBind.btnSelectAllV2.setContentDescription("전체 선택됨");
					mBind.btnSelectAllV2.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);

				}
				else if(mode == StoryListAdapter.LIST_MODE.REORDER)
				{
					//부분 적용
					mBind.tvReorderDragV2.setText(getString(R.string.playlist_save_order));
				}

			}

		});


	}

	private void check() {
		if(adapter==null){
			return;
		}
		List<StoryItem> items = adapter.getItemAll();
		int allCount = 0;
		int selectedCount = 0;
		if(items!=null){
			allCount = items.size();

			for (StoryItem item : items) {
				if (item != null && item.isSelected) {
					selectedCount++;
				}
			}

		}


		boolean isV2 = true;
		if(isV2){
			//v2
			StoryListAdapter.LIST_MODE mode = adapter.getMode();
			//boolean isEmptyCheckList = (getCheckList().size()>0)?false:true;
			boolean isEmpty = (allCount>0)?false:true;
			boolean isEmptyCheckList = (selectedCount>0)?false:true;

			/*if( isEmpty || isEmptyCheckList)*/
			if(mode == StoryListAdapter.LIST_MODE.NORMAL)
			{
				applyCheckResult(mode); //노멀모드
			}
			else
			{
				//-------------------------------------------------------
				if(mode == StoryListAdapter.LIST_MODE.EDIT)
				{
					if(isEmptyCheckList){
						applyCheckResult(StoryListAdapter.LIST_MODE.NORMAL);//노멀모드 전환
					}else{
						applyCheckResult(mode);//편집모드
					}


				}
				else if(mode == StoryListAdapter.LIST_MODE.REORDER)
				{
					applyCheckResult(mode);//순서변경모드
				}

			}

			//======== 재생목록에 데이터가 존재하지않음,종료
			if(isEmpty){
				//mBind.viewMiniPlayer.setVisibility(View.VISIBLE);
				finish();
			}
			//=========
			return;
		}

	}

	private void setSelectedCancel(boolean isCheck) {
		List<StoryItem> items = adapter.getItemAll();

		for (StoryItem item : items) {
			if (item != null) {
				item.isSelected = isCheck;
			}
		}

		adapter.notifyDataSetChanged();
		check();
	}

	private ArrayList<StoryItem> getCheckList() {
		ArrayList<StoryItem> items = adapter.getItemAll();
		ArrayList<StoryItem> result = new ArrayList<>();

		for (StoryItem item : items) {
			if (item != null && item.isSelected) {
				result.add(item);
			}
		}
		return result;
	}

}
