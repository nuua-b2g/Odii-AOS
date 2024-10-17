package kto.smarttour.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.scottyab.rootbeer.RootBeer;

import java.util.ArrayList;
import java.util.List;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.adapter.StoryLockerDetailAdapter;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.BottomSheetDialogUtil;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.InnerStorageSingleton;
import kto.smarttour.common.utils.NetworkUtil;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.SimpleDividerItemDecoration;
import kto.smarttour.common.utils.StorageUtil;
import kto.smarttour.databinding.StoryFolderDetailActivityBinding;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryFolderItem;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.player.PlayListManager;
import kto.smarttour.ui.view.Topbar;

/**
 * 보관함 폴더 상세보기 리시트
 */
public class StoryLockerDetailActivity extends BaseActivity implements StoryLockerDetailAdapter.OnItemClickListener, View.OnClickListener {

	StoryFolderDetailActivityBinding mBind;
	private boolean isSelectAll = false;
	private StoryLockerDetailAdapter adapter;
	private StoryFolderItem data;
	private BottomSheetDialogUtil bottomSheetDialogUtil;
	private int type = StoryLockerActivity.FOLDER_DEFUALT_TYPE;

	private Activity activity;
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = this;

		mBind = DataBindingUtil.setContentView(this, R.layout.story_folder_detail_activity);
		mBind.setLifecycleOwner(this);
		makeDumpView();
		((RelativeLayout.LayoutParams) mBind.topbar.getLayoutParams()).setMargins(0, statusBarHeight, 0, 0);

		bottomSheetDialogUtil = new BottomSheetDialogUtil();
		type = getIntent().getIntExtra("type", StoryLockerActivity.FOLDER_DEFUALT_TYPE);
		data = (StoryFolderItem) getIntent().getSerializableExtra("data");
		if (data == null) {
			finish();
		}

		mBind.topbar.setTopbarMode(Topbar.TOPBAR_MODE_RIGHT_CLOSE);
		mBind.topbar.setOnTopBarListener(v -> finish());
		mBind.topbar.setRightBackground(R.drawable.ic_bt_top_closew);
		setRecyclerView();
		setInit();
		mBind.topbar.setBackgroundColor(Color.parseColor("#00696CFF"));
		mBind.toastAddMenu.findViewById(R.id.btn_toast_menu_playlist_add).setOnClickListener(this);
		mBind.toastAddMenu.findViewById(R.id.btn_toast_menu_playlist_cancel).setOnClickListener(this);

		mBind.toastMenu.findViewById(R.id.btn_toast_play).setOnClickListener(this);
		mBind.toastMenu.findViewById(R.id.btn_toast_add).setOnClickListener(this);
		mBind.toastMenu.findViewById(R.id.btn_toast_locker).setOnClickListener(this);
		mBind.toastMenu.findViewById(R.id.btn_toast_download).setOnClickListener(this);
		mBind.toastMenu.findViewById(R.id.btn_toast_cancel).setOnClickListener(this);

		setViewMiniPlayer(mBind.viewMiniPlayer);

		if(PlayerService.isPlay()) {
			mBind.viewMiniPlayer.setVisibility(View.VISIBLE);
		}
	}

	private void makeDumpView() {
		RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams) mBind.dumpView.getLayoutParams();
		rl.height = statusBarHeight;
		mBind.dumpView.setLayoutParams(rl);
	}

	private void setInit() {
		List<StoryItem> items = adapter.getItemAll();
		int count = 0;
		for (StoryItem item : items) {
			if (item != null && item.isSelected) {
				count++;
			}
		}

		if (items.size() == 0) {
			isSelectAll = false;
			if (type == StoryLockerActivity.FOLDER_PLAY_TYPE) {
				mBind.toastAddMenu.setVisibility(View.GONE);
				if(PlayerService.isPlay()) {
					mBind.viewMiniPlayer.setVisibility(View.VISIBLE);
				}
			} else {
				mBind.toastMenu.setVisibility(View.GONE);
				if(PlayerService.isPlay()) {
					mBind.viewMiniPlayer.setVisibility(View.VISIBLE);
				}
			}

		} else if (count == items.size()) {
			isSelectAll = true;
			if (type == StoryLockerActivity.FOLDER_PLAY_TYPE) {
				((TextView) mBind.toastAddMenu.findViewById(R.id.tv_select_count)).setText(String.valueOf(count));
				((TextView) mBind.toastAddMenu.findViewById(R.id.tv_select_count)).setContentDescription(String.format("%s개의 이야기가 선택됨", count));
				mBind.toastAddMenu.setVisibility(View.VISIBLE);
				mBind.viewMiniPlayer.setVisibility(View.GONE);
			} else {
				((TextView) mBind.toastMenu.findViewById(R.id.tv_select_count)).setText(String.valueOf(count));
				((TextView) mBind.toastMenu.findViewById(R.id.tv_select_count)).setContentDescription(String.format("%s개의 이야기가 선택됨", count));
				mBind.toastMenu.setVisibility(View.VISIBLE);
				mBind.viewMiniPlayer.setVisibility(View.GONE);
			}
		} else if (count == 0) {
			isSelectAll = false;
			if (type == StoryLockerActivity.FOLDER_PLAY_TYPE) {
				mBind.toastAddMenu.setVisibility(View.GONE);
				if(PlayerService.isPlay()) {
					mBind.viewMiniPlayer.setVisibility(View.VISIBLE);
				}
			} else {
				mBind.toastMenu.setVisibility(View.GONE);
				if(PlayerService.isPlay()) {
					mBind.viewMiniPlayer.setVisibility(View.VISIBLE);
				}
			}

		} else {
			isSelectAll = true;
			if (type == StoryLockerActivity.FOLDER_PLAY_TYPE) {
				((TextView) mBind.toastAddMenu.findViewById(R.id.tv_select_count)).setText(String.valueOf(count));
				((TextView) mBind.toastAddMenu.findViewById(R.id.tv_select_count)).setContentDescription(String.format("%s개의 이야기가 선택됨", count));
				mBind.toastAddMenu.setVisibility(View.VISIBLE);
				mBind.viewMiniPlayer.setVisibility(View.GONE);
			} else {
				((TextView) mBind.toastMenu.findViewById(R.id.tv_select_count)).setText(String.valueOf(count));
				((TextView) mBind.toastMenu.findViewById(R.id.tv_select_count)).setContentDescription(String.format("%s개의 이야기가 선택됨", count));
				mBind.toastMenu.setVisibility(View.VISIBLE);
				mBind.viewMiniPlayer.setVisibility(View.GONE);
			}
		}
		mBind.list.getAdapter().notifyItemChanged(0);

	}

	private void setRecyclerView() {
		mBind.list.setItemAnimator(null);
		mBind.list.addItemDecoration(new SimpleDividerItemDecoration(this));
		adapter = new StoryLockerDetailAdapter(this);
		adapter.setHasStableIds(true);
		mBind.list.setAdapter(adapter);
		adapter.updateItems(StoryDbManager.getInstance(this).getStoryList(data).story);
		adapter.updateItemHeader(); // 0 덱스 임의값 저장
		adapter.setHeaderItem(data); // header 정보 전달
		adapter.setOnItemClickListener(this);

	}

	@Override
	public void onItemClick(View view, int position) {

		if (view.getId() == R.id.front_layout) {
			if (adapter.getItem(position).isSelected) {
				view.setBackgroundColor(Color.parseColor("#F7F8FF"));
				view.findViewById(R.id.view_selected).setVisibility(View.VISIBLE);
			} else {
				view.setBackgroundColor(Color.parseColor("#FFFFFF"));
				view.findViewById(R.id.view_selected).setVisibility(View.GONE);
			}
			adapter.getItem(position).isSelected = !adapter.getItem(position).isSelected;

			if(adapter.getItem(position).isSelected) {

				view.setContentDescription("선택됨");
				view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);

				String description = String.format("%s 선택됨 %s %s", position + 1, adapter.getItem(position).title, adapter.getItem(position).audioTitle);
				view.setContentDescription(description);
			} else {
				view.setContentDescription("선택해제됨");
				view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);

				String description = String.format("%s %s %s", position + 1, adapter.getItem(position).title, adapter.getItem(position).audioTitle);
				view.setContentDescription(description);
			}

			if(adapter.getItem(position).isSelected) {
				AnalyticsInterface.getInstance().logEvent(activity, "library_story_select");
			} else {
				AnalyticsInterface.getInstance().logEvent(activity, "library_story_deselect");
			}

			adapter.notifyItemChanged(position);
			setInit();
		} else if (view.getId() == R.id.btn_detail) {
			AnalyticsInterface.getInstance().logEvent(activity, "library_story_detail");
			StoryItem content = adapter.getItem(position);
			Intent contentIntent = new Intent(StoryLockerDetailActivity.this, StoryDetailActivity.class);
			contentIntent.putExtra("content", content);
			contentIntent.putExtra("type", StoryDetailActivity.STORY_LOCKER_MODE);
			startActivity(contentIntent);
		} else if (view.getId() == R.id.btn_play) {
			AnalyticsInterface.getInstance().logEvent(activity, "library_story_play");
			List<StoryItem> item = new ArrayList<>();
			item.add(adapter.getItem(position));
			List<StoryItem> list = StoryDbManager.getInstance(this).getPlayList().story;
			StoryDbManager.getInstance(this).clearPlayList();
			StoryDbManager.getInstance(this).addPlayList("L", item);
			StoryDbManager.getInstance(this).addPlayList(list);
			sendPlayInsertCast(item.size());
			Intent playIntent = new Intent(this, PlayerService.class);
			playIntent.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
			PlayerService.isFloating = false;
			startService(playIntent);
		}
	}

	@Override
	public void onItemDeleteClick(View view, int position) {
		AnalyticsInterface.getInstance().logEvent(activity, "library_story_delete");
		DialogUtil.showWarning(this, getString(R.string.confirm), getString(R.string.msg_select_story_delete), getString(R.string.confirm), getString(R.string.cancel), () -> {
			StoryItem item = adapter.getItem(position);
			StoryDbManager.getInstance(this).removeStory(data.seq, item);
			adapter.removeItem(position);

			if (position == 0) {
				adapter.notifyDataSetChanged();
			} else {
				adapter.notifyItemRemoved(position);
			}
			AnalyticsInterface.getInstance().logEvent(activity, "library_story_delete_ok");
			((MainActivity) OdiiApplication.getWebActivity()).refreshLockerCount();
			setInit();
		}, () -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_story_delete_cancel");
		});
	}

	@Override
	public void onClickSelectAll() {
		List<StoryItem> items = adapter.getItemAll();

		if (isSelectAll) {
			for (StoryItem item : items) {
				if (item != null) {
					item.isSelected = false;
				}
			}
			AnalyticsInterface.getInstance().logEvent(activity, "library_story_deselect_all");
			isSelectAll = false;
		} else {
			for (StoryItem item : items) {
				if (item != null) {
					item.isSelected = true;
				}
			}
			AnalyticsInterface.getInstance().logEvent(activity, "library_story_select_all");
			isSelectAll = true;
		}
		adapter.notifyDataSetChanged();
		setInit();
	}

	@Override
	public void onClickSelectAllPlay() {
		String[] value1 = {getString(R.string.playlist_insert_play_msg), getString(R.string.playlist_replace_play_meg)};
		int[] items_resource1 = {R.drawable.ic_icon_all_add, R.drawable.ic_icon_all_replace};
		AnalyticsInterface.getInstance().logEvent(activity, "library_story_play_all");
		DialogUtil.showChoice(this, getString(R.string.all_play_msg), getString(R.string.playlist_insert_play_in_play), value1, items_resource1, getString(R.string.cancel), result -> {
			if (result == 0) {
				List<StoryItem> checkItems = adapter.getItemAll();
				List<StoryItem> items = new ArrayList<>();

				for (StoryItem item : checkItems) {
					if (item != null) {
						items.add(item);
					}
				}
				AnalyticsInterface.getInstance().logEvent(activity, "popup_add_play");
				List<StoryItem> list = StoryDbManager.getInstance(this).getPlayList().story;
				StoryDbManager.getInstance(this).clearPlayList();
				StoryDbManager.getInstance(this).addPlayList("L", items);
				StoryDbManager.getInstance(this).addPlayList(list);
				Intent intent1 = new Intent(this, PlayerService.class);
				intent1.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
				PlayerService.isFloating = false;
				startService(intent1);
				sendPlayInsertCast(items.size());
			} else {
				AnalyticsInterface.getInstance().logEvent(activity, "popup_replace");
				PlayListManager.getInstance().setPlayIndex(0);
				List<StoryItem> checkItems = adapter.getItemAll();
				List<StoryItem> items = new ArrayList<>();

				for (StoryItem item : checkItems) {
					if (item != null) {
						items.add(item);
					}
				}
				StoryDbManager.getInstance(this).clearPlayList();
				StoryDbManager.getInstance(this).addPlayList("L", items);
				Intent playIntent = new Intent(this, PlayerService.class);
				playIntent.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
				PlayerService.isFloating = false;
				startService(playIntent);
				sendPlayInsertCast(items.size());
			}
			setSelectCancel();
			mBind.toastMenu.setVisibility(View.GONE);
		});
	}

	@Override
	public void onClickDelete() {
		AnalyticsInterface.getInstance().logEvent(activity, "library_delete");
		DialogUtil.showWarning(this, R.string.ok, R.string.locker_list_remove_message, R.string.ok, R.string.cancel, () -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_delete_ok");
			StoryDbManager.getInstance(this).removeFolder(data);
			((MainActivity)OdiiApplication.getWebActivity()).refreshLockerCount();
			finish();
		}, () -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_delete_cancel");
		});
	}

	@Override
	public void onClickEdit() {
		AnalyticsInterface.getInstance().logEvent(activity, "library_edit");
		bottomSheetDialogUtil.showFolderEditorDialog(this, data.seq, data.title, data.imgUrl, (result) -> {
			if (result == BottomSheetDialogUtil.SUCCESS) {
				StoryFolderItem folderItem = StoryDbManager.getInstance(this).getStoryFolder(data.seq);
				adapter.setHeaderItem(folderItem);
				adapter.notifyItemChanged(0);
			}

		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		bottomSheetDialogUtil.onActivityResult(requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_toast_menu_playlist_add:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_add");
				List<StoryItem> checkItems = getSelectItems();
				StoryDbManager.getInstance(this).addPlayList("L", checkItems);
				setResult(Activity.RESULT_OK);
				finish();
				break;
			case R.id.btn_toast_menu_playlist_cancel:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_cancel");
				setSelectCancel();
				setInit();
				break;
			case R.id.btn_toast_play:
				List<StoryItem> playItems = getSelectItems();
				AnalyticsInterface.getInstance().logEvent(activity, "popup_add_play");
				List<StoryItem> list = StoryDbManager.getInstance(this).getPlayList().story;
				StoryDbManager.getInstance(this).clearPlayList();
				StoryDbManager.getInstance(this).addPlayList("L", playItems);
				StoryDbManager.getInstance(this).addPlayList(list);
				Intent intent1 = new Intent(this, PlayerService.class);
				intent1.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
				PlayerService.isFloating = false;
				startService(intent1);
				sendPlayInsertCast(playItems.size());
				setSelectCancel();
				mBind.toastMenu.setVisibility(View.GONE);
				/*
				List<StoryItem> playItems = getSelectItems();
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_play");
				String[] value1 = {getString(R.string.playlist_insert_play_msg), getString(R.string.playlist_replace_play_meg)};
				int[] items_resource1 = {R.drawable.ic_icon_all_add, R.drawable.ic_icon_all_replace};

				DialogUtil.showChoice(this, getString(R.string.all_play_msg), getString(R.string.playlist_insert_play_in_play), value1, items_resource1, getString(R.string.cancel), result -> {
					if (result == 0) {
						AnalyticsInterface.getInstance().logEvent(activity, "popup_add_play");
						List<StoryItem> list = StoryDbManager.getInstance(this).getPlayList().story;
						StoryDbManager.getInstance(this).clearPlayList();
						StoryDbManager.getInstance(this).addPlayList("L", playItems);
						StoryDbManager.getInstance(this).addPlayList(list);
						Intent intent1 = new Intent(this, PlayerService.class);
						intent1.setAction(PlayerService.ACTION_CLEAR_PLAY);
						PlayerService.isFloating = false;
						startService(intent1);
						sendPlayInsertCast(playItems.size());
					} else {
						AnalyticsInterface.getInstance().logEvent(activity, "popup_replace");
						PlayListManager.getInstance().setPlayIndex(0);
						StoryDbManager.getInstance(this).clearPlayList();
						StoryDbManager.getInstance(this).addPlayList("L", playItems);
						Intent playIntent = new Intent(this, PlayerService.class);
						playIntent.setAction(PlayerService.ACTION_CLEAR_PLAY);
						PlayerService.isFloating = false;
						startService(playIntent);
						sendPlayInsertCast(playItems.size());
					}
					setSelectCancel();
					mBind.toastMenu.setVisibility(View.GONE);
				});
				 */
				break;
			case R.id.btn_toast_add:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_add");
				List<StoryItem> addItems = getSelectItems();
				StoryDbManager.getInstance(this).addPlayList("L", addItems);
				setSelectCancel();
				mBind.toastMenu.setVisibility(View.GONE);
				sendPlayInsertCast(addItems.size());
				break;
			case R.id.btn_toast_locker:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_fill");
				List<StoryItem> lockerItems = getSelectItems();
				StoryData data = new StoryData();
				data.story.addAll(lockerItems);
				Intent intent = new Intent(StoryLockerDetailActivity.this, StoryLockerActivity.class);
				intent.putExtra("type", StoryLockerActivity.FOLDER_SELECT_TYPE);
				InnerStorageSingleton.getSingleton().setData(data);
				startActivity(intent);
				setSelectCancel();
				mBind.toastMenu.setVisibility(View.GONE);
				break;
			case R.id.btn_toast_download:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_download");
				List<StoryItem> downloadItems = getSelectItems();

				if (!NetworkUtil.isNetworkConnected(this)) { // 인터넷 미접속
					DialogUtil.showWarning(this, R.string.ok, R.string.network_error, R.string.ok);
				} else if (NetworkUtil.isWifiConnected(this) && NetworkUtil.isNetworkConnected(this)) { // 와이파이 접속
					getDownlaod(downloadItems);
				} else if (SettingsUtil.isUseDataNetwork(this) && !NetworkUtil.isWifiConnected(this)) { //데이터 사용O 와이파이 접속X
					DialogUtil.showWarning(this, R.string.ok, R.string.network_use_message, R.string.ok, R.string.cancel, () -> {
//						SettingsUtil.setUseDataNetwork(this, true);
						getDownlaod(downloadItems);
					}, () -> {

					});
				} else if (!SettingsUtil.isUseDataNetwork(this) && !NetworkUtil.isWifiConnected(this)) { //데이터 사용X 와이파이 접속X
					DialogUtil.showWarning(this, R.string.ok, R.string.network_use_message2, R.string.ok, R.string.cancel, () -> {
//						SettingsUtil.setUseDataNetwork(this, true);
						getDownlaod(downloadItems);
					}, () -> {

					});
				}

				break;
			case R.id.btn_toast_cancel:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_cancel");
				setSelectCancel();
//				mBind.toastMenu.setVisibility(View.GONE);
				setInit();
				break;
		}
	}

	private void getDownlaod(List<StoryItem> downloadItems) {
		setSelectCancel();
		mBind.toastMenu.setVisibility(View.GONE);
		long fileDownloadSize = 0L;
		StoryData data = new StoryData();
		for (StoryItem item : downloadItems) {
			data.story.add(item);
			fileDownloadSize += item.audioFileSize;
		}

		//500000000 > 대략 500M
		if ((StorageUtil.GetAvailableInternalMemorySize() - fileDownloadSize) >= 500000000) {
			DialogUtil.showDownLoad(StoryLockerDetailActivity.this, R.string.download, R.string.download_message, data, R.string.cancel, () -> {
				((MainActivity) OdiiApplication.getWebActivity()).refreshDownloadCount();
			});
		} else {
			DialogUtil.showWarning(StoryLockerDetailActivity.this, R.string.ok, R.string.storage_limit_error, R.string.ok);
		}
	}

	private List<StoryItem> getSelectItems() {
		List<StoryItem> checkItems = adapter.getItemAll();
		List<StoryItem> items = new ArrayList<>();

		for (StoryItem item : checkItems) {
			if (item != null && item.isSelected) {
				items.add(item);
			}
		}
		return items;
	}

	private void setSelectCancel() {
		List<StoryItem> cancleItems = adapter.getItemAll();

		for (StoryItem item : cancleItems) {
			if (item != null) {
				item.isSelected = false;
			}
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	protected void onResume() {
			//RootBeer rootBeer = new RootBeer(this);
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
		mBind.list.addOnScrollListener(onScrollListener);

		if (type == StoryLockerActivity.FOLDER_PLAY_TYPE) {
			AnalyticsInterface.getInstance().screenView(this, "player_list_add_story");
		} else {
			AnalyticsInterface.getInstance().screenView(this, "library_story_list");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mBind.list.removeOnScrollListener(onScrollListener);
	}


	private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
		@Override
		public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
			super.onScrollStateChanged(recyclerView, newState);
		}

		@Override
		public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
			super.onScrolled(recyclerView, dx, dy);

			if (!mBind.list.canScrollVertically(-1)) {
				mBind.dumpView.setBackgroundColor(changeAlpha(Color.parseColor("#696CFF"), 0.0f));
				mBind.topbar.setBackgroundColor(changeAlpha(Color.parseColor("#696CFF"), 0.0f));
			} else {
				mBind.dumpView.setBackgroundColor(changeAlpha(Color.parseColor("#696CFF"), 0.8f));
				mBind.topbar.setBackgroundColor(changeAlpha(Color.parseColor("#696CFF"), 0.8f));
			}


		}
	};

	public int changeAlpha(int color, float fraction) {
		int red = Color.red(color);
		int green = Color.green(color);
		int blue = Color.blue(color);
		int alpha = (int) (Color.alpha(color) * fraction);
		return Color.argb(alpha, red, green, blue);
	}

}
