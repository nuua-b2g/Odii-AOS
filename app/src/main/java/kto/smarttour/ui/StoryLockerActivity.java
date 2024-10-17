package kto.smarttour.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.scottyab.rootbeer.RootBeer;

import java.util.List;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.adapter.StoryLockerAdapter;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.BottomSheetDialogUtil;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.InnerStorageSingleton;
import kto.smarttour.common.utils.SimpleDividerItemDecoration;
import kto.smarttour.databinding.StoryFolderLayoutBinding;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryFolderItem;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.player.PlayListManager;
import kto.smarttour.ui.view.Topbar;

/**
 * 보관함 폴더 리스트
 */
public class StoryLockerActivity extends BaseActivity implements StoryLockerAdapter.OnItemClickListener {

	public static final int FOLDER_DEFUALT_TYPE = 0;
	public static final int FOLDER_SELECT_TYPE = 1;
	public static final int FOLDER_PLAY_TYPE = 2;

	private StoryFolderLayoutBinding mBind;
	private StoryLockerAdapter adapter;
	private BottomSheetDialogUtil bottomSheetDialogUtil;

	private int type = 0;
	private int selectPosition = -1;
	private StoryData data;

	private Activity activity;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = this;
		bottomSheetDialogUtil = new BottomSheetDialogUtil();
		mBind = DataBindingUtil.setContentView(this, R.layout.story_folder_layout);
		mBind.setLifecycleOwner(this);

		((LinearLayout.LayoutParams) mBind.topbar.getLayoutParams()).setMargins(0, statusBarHeight, 0, 0);

		type = getIntent().getIntExtra("type", FOLDER_DEFUALT_TYPE);
		if (type == FOLDER_SELECT_TYPE) {
			data = new StoryData();
			data = (StoryData)InnerStorageSingleton.getSingleton().getData();

			mBind.topbar.setTitle(getString(R.string.toast_locker));
			mBind.layoutCountView.setVisibility(View.INVISIBLE);
			mBind.layoutSelectView.setVisibility(View.VISIBLE);
		} else {
			mBind.layoutCountView.setVisibility(View.VISIBLE);
			mBind.layoutSelectView.setVisibility(View.INVISIBLE);
			if (type == FOLDER_PLAY_TYPE) {
				mBind.viewMake.setVisibility(View.GONE);
			}

			mBind.topbar.setTitle(getString(R.string.fill_msg));
		}

		mBind.topbar.setTopbarMode(Topbar.TOPBAR_MODE_LEFT);
		mBind.topbar.setOnTopBarListener(v -> finish());


		setRecyclerView();
		mBind.btnFolder.setOnClickListener(view -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_create");
			bottomSheetDialogUtil.showFolderMakeDialog(this, result -> {
				if (result == BottomSheetDialogUtil.SUCCESS) {
					adapter.updateItems(StoryDbManager.getInstance(this).getStoryFolderList());
					checkEmpty();
				} else {
					// TODO 팝업
				}
			});
		});
		mBind.btnOk.setEnabled(false);



		if (type != FOLDER_SELECT_TYPE) {
			setViewMiniPlayer(mBind.viewMiniPlayer);
		}

		adapter.updateItems(StoryDbManager.getInstance(this).getStoryFolderList());
	}

	private void setRecyclerView() {
		mBind.list.addItemDecoration(new SimpleDividerItemDecoration(this, SimpleDividerItemDecoration.DIVIDER_END_MARGIN));
		adapter = new StoryLockerAdapter(this);
		mBind.list.setAdapter(adapter);
		adapter.setOnItemClickListener(this);
	}

	@Override
	protected void onResume() {
			RootBeer rootBeer = new RootBeer(this);
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

		adapter.updateItems(StoryDbManager.getInstance(this).getStoryFolderList());
		checkEmpty();
		mBind.viewMiniPlayer.checkPlayer();

		if (type == FOLDER_SELECT_TYPE) {
			AnalyticsInterface.getInstance().screenView(this, "player_list_library_add");
		} else {
			AnalyticsInterface.getInstance().screenView(this, "library_list");
		}
	}

	@Override
	public void onItemClick(View view, int position) {

		if ((TextUtils.isEmpty(adapter.getItem(position).fileData) && Integer.parseInt(adapter.getItem(position).fileData) > 0) && type != FOLDER_SELECT_TYPE) {
			DialogUtil.showWarning(StoryLockerActivity.this, R.string.ok, R.string.empty_story, R.string.ok);
			return;
		}

		if (FOLDER_DEFUALT_TYPE == type) {
			StoryFolderItem item = adapter.getItem(position);

			if (!TextUtils.isEmpty(item.fileData) && Integer.valueOf(item.fileData) <= 0) {
				return;
			}
			AnalyticsInterface.getInstance().logEvent(activity, "library");
			Intent detailActivity = new Intent(StoryLockerActivity.this, StoryLockerDetailActivity.class);
			detailActivity.putExtra("data", item);
			startActivity(detailActivity);
		} else if (FOLDER_SELECT_TYPE == type) {
			List<StoryFolderItem> items = adapter.getItemAll();
			mBind.btnOk.setEnabled(false);
			for (int i = 0; i < items.size(); i++) {
				if (items.get(i).isSelected) {
					items.get(i).isSelected = false;
					adapter.notifyItemChanged(i);
				}

				if (i == position) {
					items.get(i).isSelected = true;
					selectPosition = i;
					mBind.btnOk.setEnabled(true);
					adapter.notifyItemChanged(i);
				}
			}
		} else if (FOLDER_PLAY_TYPE == type) {
			StoryFolderItem item = adapter.getItem(position);

			if (!TextUtils.isEmpty(item.fileData) && Integer.valueOf(item.fileData) <= 0) {
				return;
			}
			Intent detailActivity = new Intent(StoryLockerActivity.this, StoryLockerDetailActivity.class);
			detailActivity.putExtra("type", FOLDER_PLAY_TYPE);
			detailActivity.putExtra("data", item);
			startActivityForResult(detailActivity, 3985);
		}
	}

	@Override
	public void onItemDeleteClick(View view, int position) {
		AnalyticsInterface.getInstance().logEvent(activity, "library_delete");
		DialogUtil.showWarning(this, R.string.ok, R.string.locker_list_remove_message, R.string.ok, R.string.cancel, () -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_delete_ok");
			StoryFolderItem item = adapter.getItem(position);
			StoryDbManager.getInstance(this).removeFolder(item);
			adapter.removeItem(position);

			if (position == 0) {
				adapter.notifyDataSetChanged();
			} else {
				adapter.notifyItemRemoved(position);
			}
			((MainActivity) OdiiApplication.getWebActivity()).refreshLockerCount();
			checkEmpty();
		}, () -> {
			AnalyticsInterface.getInstance().logEvent(activity, "library_delete_cancel");
		});
	}

	@Override
	public void onItemPlayBoxClick(View view, int position) {
		if (type != FOLDER_SELECT_TYPE) {

			AnalyticsInterface.getInstance().logEvent(activity, "library_play");

			String[] items = {getString(R.string.playlist_insert_play_msg), getString(R.string.playlist_replace_play_meg)};
			int[] items_resource = {R.drawable.ic_icon_all_add, R.drawable.ic_icon_all_replace};

			DialogUtil.showChoice(this, getString(R.string.all_play_msg), getString(R.string.playlist_insert_play_in_play), items, items_resource, getString(R.string.cancel), result -> {
				List<StoryItem> storyItems = StoryDbManager.getInstance(this).getStoryList(adapter.getItem(position)).story;
				if (result == 0) {
					AnalyticsInterface.getInstance().logEvent(activity, "popup_add_play");
					List<StoryItem> list = StoryDbManager.getInstance(this).getPlayList().story;
					StoryDbManager.getInstance(this).clearPlayList();
					StoryDbManager.getInstance(this).addPlayList("L", storyItems);
					StoryDbManager.getInstance(this).addPlayList(list);
					Intent intent = new Intent(this, PlayerService.class);
					intent.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
					startService(intent);
					sendPlayInsertCast(storyItems.size());
				} else {
					AnalyticsInterface.getInstance().logEvent(activity, "popup_replace");
					PlayListManager.getInstance().setPlayIndex(0);
					StoryDbManager.getInstance(this).clearPlayList();
					StoryDbManager.getInstance(this).addPlayList("L", storyItems);
					Intent playIntent = new Intent(this, PlayerService.class);
					playIntent.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
					startService(playIntent);
					sendPlayInsertCast(storyItems.size());
				}
				if (type == FOLDER_PLAY_TYPE) {
					finish();
				}

			});

		}
	}

	private void checkEmpty() {
		if (adapter.getItemCount() == 0) {
			mBind.viewList.setVisibility(View.GONE);
			mBind.emptyView.setVisibility(View.VISIBLE);
		} else {
			mBind.viewList.setVisibility(View.VISIBLE);
			mBind.emptyView.setVisibility(View.GONE);
		}

		if (type == FOLDER_SELECT_TYPE) {
			mBind.btnOk.setVisibility(View.VISIBLE);
			mBind.btnOk.setOnClickListener(view -> {
				StoryFolderItem folderItem = adapter.getItem(selectPosition);
				StoryDbManager.getInstance(this).insertStory(folderItem.seq, data);
				((MainActivity) OdiiApplication.getWebActivity()).refreshLockerCount();
				finish();
			});
		} else {
			mBind.btnOk.setVisibility(View.GONE);
		}

		mBind.tvFolderCount.setText(String.valueOf(StoryDbManager.getInstance(this).getStoryFolderCount()));
		mBind.tvFolderCount.setContentDescription(StoryDbManager.getInstance(this).getStoryFolderCount() + getString(R.string.libraries));

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		bottomSheetDialogUtil.onActivityResult(requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 3985 && resultCode == Activity.RESULT_OK) {
			finish();
		}
	}
}
