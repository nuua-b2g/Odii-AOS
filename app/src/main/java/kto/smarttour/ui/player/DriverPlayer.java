package kto.smarttour.ui.player;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import kto.smarttour.BuildConfig;
import kto.smarttour.R;
import kto.smarttour.adapter.DriverStoryListAdapter;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.GradientTransformation;
import kto.smarttour.common.utils.LocationDistance;
import kto.smarttour.common.utils.LocationUtils;
import kto.smarttour.common.utils.PermissionUtil;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.SimpleDividerItemDecoration;
import kto.smarttour.common.utils.SimpleItemTouchHelperCallback;
import kto.smarttour.common.utils.SystemUtils;
import kto.smarttour.common.utils.ViewUtils;
import kto.smarttour.databinding.ActivityDriverModeLayoutBinding;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.PermissionCheckActivity;

public class DriverPlayer extends BaseActivity implements DriverStoryListAdapter.OnItemClickListener, DriverStoryListAdapter.OnStartDragListener, View.OnClickListener {

	private static final String TAG = DriverPlayer.class.getSimpleName();

	private AppCompatActivity activity;
	private ActivityDriverModeLayoutBinding mBind;
	private DriverStoryListAdapter adapter;
	private ItemTouchHelper mItemTouchHelper;

	private PlayerControllReceiver receiver;
	private IntentFilter playerIntentFilter;


	private static final long UPDATE_INTERVAL = 3000; // Every 1 seconds.
	private static final long FASTEST_UPDATE_INTERVAL = 3000; // Every 0.5 seconds
	private int CONTENT_GPS_RADIUS = 1; // km단위

	private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

	private LocationRequest mLocationRequest;
	private FusedLocationProviderClient mFusedLocationClient;

	private RecyclerView.ItemDecoration divider;

	private ArrayList<StoryItem> story = new ArrayList();

	private boolean isGpsPlayPopup = false;
	private boolean isClickOrientationEvent = false;
	private boolean isGpsPermissionEvent = false;

	private ArrayList<Integer> played = new ArrayList<>(); // 드라이브 모드 이야기 재생한 목록 리스트
	private boolean isStoryEdit = false;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AnalyticsInterface.getInstance().screenView(this, "player_driver");

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		story = StoryDbManager.getInstance(this).getPlayList().story;

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		activity = this;
		divider = new SimpleDividerItemDecoration(this, SimpleDividerItemDecoration.DIVIDER_TOP_MARGIN, ViewUtils.dp2px(60), R.drawable.line_driver_divider);
		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

		receiver = new PlayerControllReceiver();

		mBind = DataBindingUtil.setContentView(activity, R.layout.activity_driver_mode_layout);
		mBind.seekArc.setOnTouchListener((v, event) -> {
			return true;
		});
		mBind.setLifecycleOwner(this);

		makeDumpView();

		playerIntentFilter = new IntentFilter();
		playerIntentFilter.addAction(PlayerConstants.GUI_UPDATE_ACTION);
		playerIntentFilter.addAction(PlayerConstants.ACTION_PLAY_INFO);
		playerIntentFilter.addAction(PlayerConstants.ACTION_STOP);
		playerIntentFilter.addAction(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE);
		playerIntentFilter.addAction(PlayerConstants.ACTION_PLAYER_STATUS_PLAY);
		playerIntentFilter.addAction(PlayerConstants.ACTION_PLAY_COMPLETE);
		playerIntentFilter.addAction(PlayerConstants.ACTION_NOTIFICATION_MUSIC_CONTORLL_CHANGE);
		setRecyclerView();

		createLocationRequest();

		setMediaControllerChange(PlayListManager.getInstance().getPlayIndex());

		mBind.btnClose.setOnClickListener(view -> finish());
		mBind.btnRotation.setOnClickListener(view -> {
			isClickOrientationEvent = true;
			int rotation = getResources().getConfiguration().orientation;
			if (rotation == Configuration.ORIENTATION_PORTRAIT) {
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			} else if (rotation == Configuration.ORIENTATION_LANDSCAPE) {
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
		});

		mBind.btnEdit.setOnClickListener(view -> {
			isStoryEdit = true;
			DialogUtil.showWarning(activity, R.string.ok, R.string.drive_mode_msg_4, R.string.ok, R.string.cancel, () -> {
				played.clear();
				PlayerService.startActionStop2(activity);
				adapter.setEdit(true);
				mBind.layoutInfo.setVisibility(View.GONE);
				mBind.btnEdit.setVisibility(View.GONE);
				mBind.btnEditComplete.setVisibility(View.VISIBLE);
				setMediaControllerChange(0);
				PlayListManager.getInstance().setPlayIndex(0);
			}, () -> {
				isStoryEdit = false;
			});
		});

		mBind.btnEditComplete.setOnClickListener(view -> {
			isStoryEdit = false;
			adapter.setEdit(false);
			mBind.layoutInfo.setVisibility(View.VISIBLE);
			mBind.btnEdit.setVisibility(View.VISIBLE);
			mBind.btnEditComplete.setVisibility(View.GONE);
		});

		mBind.btnPlayerPlay.setOnClickListener(this);
		mBind.btnPlayerPause.setOnClickListener(this);
		mBind.btnPlayerNext.setOnClickListener(this);
		mBind.btnPlayerPrev.setOnClickListener(this);
		mBind.btnGps.setOnClickListener(this);

		mBind.tvDriveMsg.setText(R.string.drive_mode_msg);


		if(SystemUtils.checkGps(activity)) {
			if(PermissionUtil.hasPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
				if (SettingsUtil.getDriveMode(activity)) {
					mBind.btnGps.setBackgroundResource(R.drawable.ic_bt_drive_tgon);
					PlayerService.setDriveMode(true);
				} else {
					mBind.btnGps.setBackgroundResource(R.drawable.ic_bt_drive_tgoff);
					PlayerService.setDriveMode(false);
				}
			} else {
				mBind.btnGps.setBackgroundResource(R.drawable.ic_bt_drive_tgoff);
				PlayerService.setDriveMode(false);
			}
		} else {
			SettingsUtil.setDriveMode(activity, false);
			mBind.btnGps.setBackgroundResource(R.drawable.ic_bt_drive_tgoff);
			PlayerService.setDriveMode(false);
		}

		if(PlayerService.isPlay()) {
			played.add(PlayListManager.getInstance().getCurrentStorySeq());
		}

	}

	private void setMediaControllerChange(int position) {
		KLog.i("DriverPlayer", "setMediaControllerChange position : " + position);
		if (adapter != null && !adapter.isEmpty()) {
			StoryItem item = adapter.getItem(position);

			mBind.title.setText(item.title);
			mBind.tvStoryCount.setText(String.format("%d / %d", position + 1, adapter.getItemCount()));

			new Handler(Looper.getMainLooper()) {
				@Override
				public void handleMessage(@NonNull Message msg) {
					super.handleMessage(msg);
					Glide.with(activity).load(item.thumbnailFilePath).placeholder(R.drawable.ic_img_scrap_dummy).error(R.drawable.ic_img_scrap_dummy).centerCrop().transform(new MultiTransformation<>(new CenterCrop(), new GradientTransformation())).into(mBind.ivThumb);
				}
			}.sendEmptyMessage(0);
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntegerArrayList("played", played);
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if(savedInstanceState.getIntegerArrayList("played") != null) {
			played = savedInstanceState.getIntegerArrayList("played");
		}
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

		adapter = new DriverStoryListAdapter(this, PlayListManager.getInstance().getPlayIndex(), false);
		adapter.updateItems(StoryDbManager.getInstance(this).getPlayList().story);
		adapter.setOnItemClickListener(this);
		adapter.setOnStartDragListener(this);
		mBind.list.setAdapter(adapter);

		ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
		mItemTouchHelper = new ItemTouchHelper(callback);
		mItemTouchHelper.attachToRecyclerView(mBind.list);
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
		requestLocationUpdates();

		if (receiver != null && playerIntentFilter != null) {
			LocalBroadcastManager.getInstance(this).registerReceiver(receiver, playerIntentFilter);
		}

		adapter.setIndex(PlayListManager.getInstance().getPlayIndex());


		checkPlayerButton();
		checkPlayer();
	}

	@Override
	protected void onPause() {
		super.onPause();
		KLog.i("TEST_LOG", "DriverPlayer onPause()");
		removeLocationUpdates();

		if (receiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		}

		if(!isClickOrientationEvent && !isGpsPermissionEvent) {
			PlayerService.setDriveMode(false);
			PlayerService.setGpsPlayComplete(false);
			finish();
		}
		isClickOrientationEvent = false;
		isGpsPermissionEvent = false;
	}

	@Override
	public void onItemClick(View view, int position) {
		//		setMediaControllerChange(position);
		if (position == PlayListManager.getInstance().getPlayIndex()) {
			return;
		}
		setDriveModeDisable();
		PlayListManager.getInstance().setPlayIndex(position);
		PlayerService.startActionPlayIndex(this);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onItemDelete(View view, int position) {

		int playIndex = PlayListManager.getInstance().getPlayIndex();
		StoryItem deleteItem = adapter.getItem(position);

		adapter.onItemDismiss(position);
		PlayListManager.getInstance().remove(position);

		StoryDbManager.getInstance(activity).removePlayList(deleteItem.tlid, deleteItem.slid);

		if (PlayListManager.getInstance().getListSize() == 0) {
			PlayerService.startActionStop(this);
		} else {
			if (playIndex > position) {
				playIndex -= 1;
				PlayListManager.getInstance().setPlayIndex(playIndex);
				adapter.setIndex(playIndex);
			} else if (playIndex == position) {

				if (PlayListManager.getInstance().getListSize() <= playIndex) {
					PlayListManager.getInstance().setPlayIndex(0);
				} else {
					PlayListManager.getInstance().setPlayIndex(playIndex);
				}
			}
		}

		adapter.notifyDataSetChanged();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		Log.i(TAG, "onRequestPermissionResult");
		if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
			if (grantResults.length <= 0) {
				Log.i(TAG, "User interaction was cancelled.");
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


	@Override
	protected void onDestroy() {
		super.onDestroy();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		switch (id) {
			case R.id.btn_player_play:
				PlayerService.startActionPlay(this);
				break;
			case R.id.btn_player_pause:
				PlayerService.startActionPause(this);
				break;
			case R.id.btn_player_next:
				setDriveModeDisable();
				PlayerService.startActionNextSong(this);
				break;
			case R.id.btn_player_prev:
				setDriveModeDisable();
				PlayerService.startActionPreviousSong(this);
				break;
			case R.id.btn_gps:

				if(!SystemUtils.checkGps(activity)) {
					// GPS 체크
					DialogUtil.showWarning(activity, "", activity.getString(R.string.setting_non_check_location), activity.getString(R.string.settings), activity.getString(R.string.cancel), () -> {
						moveConfigGPS();
					}, () -> {

					});
				} else if(!PermissionUtil.hasPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
					movePermissionGps();
				} else {
					if (!SettingsUtil.getDriveMode(activity)) {
						DialogUtil.showWarning(activity, R.string.ok, R.string.drive_mode_msg_1, R.string.ok, R.string.cancel, () -> {
							mBind.btnGps.setBackgroundResource(R.drawable.ic_bt_drive_tgon);
							PlayerService.setDriveMode(true);
							SettingsUtil.setDriveMode(activity, true);
						}, () -> {
							mBind.btnGps.setBackgroundResource(R.drawable.ic_bt_drive_tgoff);
							PlayerService.setDriveMode(false);
							SettingsUtil.setDriveMode(activity, false);
							played.clear();
						});
					} else {
						SettingsUtil.setDriveMode(activity, false);
						mBind.btnGps.setBackgroundResource(R.drawable.ic_bt_drive_tgoff);
						PlayerService.setDriveMode(false);

						played.clear();
					}
				}
				break;
		}
	}

	private void checkPlayer() {
		int position = PlayListManager.getInstance().getPlayIndex();
		if(position == -1) {
			return;
		}

		StoryItem item = adapter.getItem(position);

		Glide.with(activity).load(item.thumbnailFilePath).error(R.drawable.ic_img_scrap_dummy).centerCrop().transform(new MultiTransformation<>(new CenterCrop(), new GradientTransformation())).into(mBind.ivThumb);

		mBind.title.setText(item.title);
		mBind.tvStoryCount.setText(String.format("%d / %d", position + 1, adapter.getItemCount()));
	}

	private void checkPlayerButton() {
		if (PlayerService.isPlay) {
			mBind.btnPlayerPlay.setVisibility(View.INVISIBLE);
			mBind.btnPlayerPause.setVisibility(View.VISIBLE);
		} else {
			mBind.btnPlayerPlay.setVisibility(View.VISIBLE);
			mBind.btnPlayerPause.setVisibility(View.INVISIBLE);
		}
	}


	@Override
	public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
		mItemTouchHelper.startDrag(viewHolder);
	}

	@Override
	public void onEndDrag(int fromPosition, int toPosition) {
		KLog.i("onEndDrag", String.format("onEndDrag fromPosition : %s / toPosition : %s", fromPosition, toPosition));
		int index = PlayListManager.getInstance().getPlayIndex();
		if (fromPosition == adapter.getIndex()) {
			index = toPosition;
			PlayListManager.getInstance().setPlayIndex(index);
			adapter.setIndex(index);
		} else if (fromPosition > PlayListManager.getInstance().getPlayIndex() && toPosition <= PlayListManager.getInstance().getPlayIndex()) {
			index++;
			PlayListManager.getInstance().setPlayIndex(index);
			adapter.setIndex(index);
		}

		adapter.notifyDataSetChanged();
		StoryDbManager.getInstance(activity).movePlayList(adapter.getItem(fromPosition), adapter.getItem(toPosition));
	}

	private class PlayerControllReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			KLog.i("DriverPlayer", "PlayerControllReceiver onReceive : " + intent.getAction());

			switch (intent.getAction()) {
				case PlayerConstants.GUI_UPDATE_ACTION:
					int t1 = intent.getIntExtra(PlayerConstants.ACTUAL_TIME_VALUE_EXTRA, 0);
					int t2 = intent.getIntExtra(PlayerConstants.TOTAL_TIME_VALUE_EXTRA, 0);
					float seekArcTime = (float) t1 / (float) t2 * 100.0f;
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
					}
					break;
				case PlayerConstants.ACTION_STOP:
					finish();
					break;
				case PlayerConstants.ACTION_PLAYER_STATUS_PAUSE:
				case PlayerConstants.ACTION_PLAYER_STATUS_PLAY:
					checkPlayerButton();
					break;
				case PlayerConstants.ACTION_PLAY_INFO:
					checkPlayerButton();
					checkPlayer();
					adapter.setIndex(PlayListManager.getInstance().getPlayIndex());
					mBind.list.scrollToPosition(PlayListManager.getInstance().getPlayIndex());
					break;
				case PlayerConstants.ACTION_PLAY_COMPLETE:
					mBind.seekArc.setProgress(0);
					if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						mBind.timerStart.setText("");
						mBind.timerEnd.setText("");
					} else {
						mBind.timer.setText("");
					}
					checkPlayerButton();
					break;
				case PlayerConstants.ACTION_NOTIFICATION_MUSIC_CONTORLL_CHANGE:
					setDriveModeDisable();
					break;

			}
		}

		private String getColoredSpanned(String text, String color) {
			String input = "<font color=" + color + ">" + text + "</font>";
			return input;
		}
	}

	public void requestLocationUpdates() {
		try {
			Log.i(TAG, "Starting location updates");
			LocationUtils.setRequestingLocationUpdates(this, true);
			mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
		} catch (SecurityException e) {
			LocationUtils.setRequestingLocationUpdates(this, false);
			e.printStackTrace();
		}
	}

	public void removeLocationUpdates() {
		Log.i(TAG, "Removing location updates");
		LocationUtils.setRequestingLocationUpdates(this, false);
		mFusedLocationClient.removeLocationUpdates(mLocationCallback);
	}


	float filtSpeed;
	float localspeed;
	@SuppressLint("StringFormatInvalid")
	private LocationCallback mLocationCallback = new LocationCallback() {
		@Override
		public void onLocationResult(LocationResult locationResult) {
			super.onLocationResult(locationResult);

			List<Location> locations = locationResult.getLocations();
			Location location = locations.get(0);


			KLog.i("GPS_TEST_VALUE", "start : " + System.currentTimeMillis());

			if(story != null && !story.isEmpty()) {
				for (StoryItem item : story) {
					item.currentDistance = LocationDistance.distance(Double.valueOf(item.posY), Double.valueOf(item.posX), location.getLatitude(), location.getLongitude(), "Km");
				}


				Collections.sort(story, (s1, s2) ->{
					if (s1.currentDistance < s2.currentDistance) {
						return -1;
					} else if (s1.currentDistance > s2.currentDistance) {
						return 1;
					}
					return 0;
				});

				if(PlayerService.getDriveMode() && !isGpsPlayPopup && PlayerService.getGpsPlayComplete() && !isStoryEdit) {
					for(int i = 0; i < story.size(); i++) {
						if(story.get(i).currentDistance <= CONTENT_GPS_RADIUS && !played.contains(story.get(i).seq)) {
							int finalI = i;
							isGpsPlayPopup = true;
							String title = "";
							if("ko".equals(getString(R.string.language))) {
								title = String.format(getString(R.string.drive_mode_msg_6), story.get(i).title);
							} else {
								title = getString(R.string.drive_mode_msg_5);
							}

							DialogUtil.showWarning(activity, getString(R.string.ok), title, activity.getString(R.string.drive_mode_msg_2), activity.getString(R.string.drive_mode_msg_3), () -> {
								played.add(story.get(finalI).seq);
								int index = PlayListManager.getInstance().getStoryIndex(story.get(finalI).seq);
								PlayListManager.getInstance().setPlayIndex(index);
								PlayerService.startActionPlayIndex(activity);
								isGpsPlayPopup = false;
								PlayerService.setGpsPlayComplete(false);
							}, () -> {
								mBind.btnGps.setBackgroundResource(R.drawable.ic_bt_drive_tgoff);
								PlayerService.setDriveMode(false);
								isGpsPlayPopup = false;
								PlayerService.setGpsPlayComplete(false);
								SettingsUtil.setDriveMode(activity, false);
								played.clear();
							});
							break;
						}
					}
				}
			}

			if (location.hasSpeed()) {
				localspeed = location.getSpeed() * 3.6f;
				filtSpeed = speedFilter(filtSpeed, localspeed);

				runOnUiThread(() -> {
					if (filtSpeed >= 10) {
						if (mBind.layoutSafety.getVisibility() == View.GONE) {
							mBind.layoutSafety.setVisibility(View.VISIBLE);
						}
					} else {
						if (mBind.layoutSafety.getVisibility() == View.VISIBLE) {
							mBind.layoutSafety.setVisibility(View.GONE);
						}
					}
				});
			}
		}
	};

	private float speedFilter(final float prev, final float curr) {
		// If first time through, initialise digital filter with current values
		if (Float.isNaN(prev)) return curr;
		// If current value is invalid, return previous filtered value
		if (Float.isNaN(curr)) return prev;
		// Calculate new filtered value
		return (float) (curr / 2 + prev * (1.0 - 1.0 / 2));
	}

	public class DistanceSorter implements Comparator<StoryItem> {
		public int compare(StoryItem s1, StoryItem s2) {
			if (s1.currentDistance < s2.currentDistance) {
				return -1;
			} else if (s1.currentDistance > s2.currentDistance) {
				return 1;
			} else if (s1.seq < s2.seq) {
				return -1;
			} else if (s1.seq > s2.seq) {
				return 1;
			}
			return 0;
		}
	}

	private void setDriveModeDisable() {
		PlayerService.setDriveMode(false);
		PlayerService.setGpsPlayComplete(false);
		mBind.btnGps.setBackgroundResource(R.drawable.ic_bt_drive_tgoff);
	}

	private void moveConfigGPS() {
		isGpsPermissionEvent = true;
		Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivityForResult(gpsOptionsIntent, 4444);
	}

	private void movePermissionGps() {
		ArrayList<String> resPermission = SystemUtils.checkSelfPermission(this, SystemUtils.PERMISSION_REQUEST_GPS);
		SettingsUtil.setCheckPermission(this, true);
		if (resPermission.size() == 0) {
			mBind.btnGps.performClick();
		} else {
			isGpsPermissionEvent = true;
			Intent requestPermissionIntent = new Intent(this, PermissionCheckActivity.class);
			requestPermissionIntent.putExtra("drive", true);
			startActivityForResult(requestPermissionIntent, 4600);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 4444 && SystemUtils.checkGps(activity)) {
			mBind.btnGps.performClick();
		} else if(requestCode == 4600 && resultCode == Activity.RESULT_OK) {
			mBind.btnGps.performClick();
		}
	}
}
