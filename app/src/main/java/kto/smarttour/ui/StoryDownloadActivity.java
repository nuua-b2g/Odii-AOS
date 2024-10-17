package kto.smarttour.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.DataBindingUtil;

import com.scottyab.rootbeer.RootBeer;

import java.util.ArrayList;
import java.util.List;

import kto.smarttour.BuildConfig;
import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.adapter.StoryDownloadAdapter;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.InnerStorageSingleton;
import kto.smarttour.common.utils.SimpleDividerItemDecoration;
import kto.smarttour.common.utils.ViewUtils;
import kto.smarttour.databinding.ActivityDownloadBinding;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.player.PlayListManager;
import kto.smarttour.ui.view.Topbar;

public class StoryDownloadActivity extends BaseActivity implements StoryDownloadAdapter.OnItemClickListener, View.OnClickListener {

	private ActivityDownloadBinding mBind;
	private StoryDownloadAdapter adapter;
	private boolean isSelectAll = false;

	private Activity activity;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = this;
		mBind = DataBindingUtil.setContentView(this, R.layout.activity_download);
		mBind.setLifecycleOwner(this);

		((LinearLayout.LayoutParams) mBind.topbar.getLayoutParams()).setMargins(0, statusBarHeight, 0, 0);

		mBind.topbar.setTitle(getString(R.string.download));
		mBind.topbar.setTopbarMode(Topbar.TOPBAR_MODE_LEFT);
		mBind.topbar.setOnTopBarListener(v -> finish());
		setRecyclerView();
		check();
		mBind.btnSelectAll.setOnClickListener(this);
		mBind.btnPlayAll.setOnClickListener(this);

		//--
		mBind.toastMenu.findViewById(R.id.btn_toast_del).setOnClickListener(this);
		mBind.toastMenu.findViewById(R.id.btn_toast_add).setOnClickListener(this);//xml gone
		mBind.toastMenu.findViewById(R.id.btn_toast_play).setOnClickListener(this);//xml gone
		mBind.toastMenu.findViewById(R.id.btn_toast_locker).setOnClickListener(this);//xml gone
		mBind.toastMenu.findViewById(R.id.btn_toast_cancel).setOnClickListener(this);

		mBind.btnClear.setOnClickListener(v -> mBind.editSearch.setText(""));
		mBind.btnFind.setOnClickListener(v -> {
			CommonUtils.hideKeyboard(v);
			if(ViewUtils.isAccessibility(this)) {
				mBind.editSearch.setHint(mBind.editSearch.getText() + " 검색 중 입니다. 수정하시려면 탭하세요.");
			}
		});
		mBind.editSearch.setOnFocusChangeListener((v, hasFocus) -> {
			if (hasFocus) {
				mBind.viewSearch.setBackgroundColor(Color.parseColor("#000000"));
			} else {
				mBind.viewSearch.setBackgroundColor(Color.parseColor("#EBEBEB"));
			}
		});
		mBind.editSearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (TextUtils.isEmpty(s)) {
					mBind.btnClear.setVisibility(View.INVISIBLE);
					mBind.viewSearch.setBackgroundColor(Color.parseColor("#EBEBEB"));
					ImageViewCompat.setImageTintList(mBind.btnFind, ColorStateList.valueOf(ContextCompat.getColor(StoryDownloadActivity.this, R.color.tint_color_disable)));
				} else {
					mBind.btnClear.setVisibility(View.VISIBLE);
					mBind.viewSearch.setBackgroundColor(Color.parseColor("#000000"));
					ImageViewCompat.setImageTintList(mBind.btnFind, ColorStateList.valueOf(ContextCompat.getColor(StoryDownloadActivity.this, R.color.tint_color_enable)));
				}
				adapter.getFilter().filter(s);
			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});

		mBind.editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {
				CommonUtils.hideKeyboard(v);
				if(ViewUtils.isAccessibility(activity)) {
					mBind.editSearch.setHint(v.getText() + " 검색 중 입니다. 수정하시려면 탭하세요.");
				}
				return false;
			}
		});

		setViewMiniPlayer(mBind.viewMiniPlayer);
		if(PlayerService.isPlay()) {
			mBind.viewMiniPlayer.setVisibility(View.VISIBLE);
		}
	}

	private void check() {
		List<StoryItem> items = adapter.getItemAll();
		int count = 0;

		for (StoryItem item : items) {
			if (item != null && item.isSelected) {
				count++;
			}
		}
		if (items.size() == 0) {
			mBind.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_icon_selectall_dis);
			mBind.btnSelectAllText.setText(getString(R.string.all_select));
			mBind.btnSelectAllText.setTextColor(Color.parseColor("#000000"));
			isSelectAll = false;
			mBind.toastMenu.setVisibility(View.GONE);
			if(PlayerService.isPlay()) {
				mBind.viewMiniPlayer.setVisibility(View.VISIBLE);
			}
		} else if (count == items.size()) {
			mBind.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_icon_selectall_enb);
			mBind.btnSelectAllText.setText(getString(R.string.all_select_cancel));
			mBind.btnSelectAllText.setTextColor(Color.parseColor("#696CFF"));
			isSelectAll = true;
			((TextView)mBind.toastMenu.findViewById(R.id.tv_select_count)).setText(String.valueOf(count));
			((TextView)mBind.toastMenu.findViewById(R.id.tv_select_count)).setContentDescription(String.format("%s개의 이야기가 선택됨", count));
			mBind.toastMenu.setVisibility(View.VISIBLE);
			mBind.viewMiniPlayer.setVisibility(View.GONE);
		} else if (count == 0) {
			mBind.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_icon_selectall_dis);
			mBind.btnSelectAllText.setText(getString(R.string.all_select));
			mBind.btnSelectAllText.setTextColor(Color.parseColor("#000000"));
			isSelectAll = false;
			mBind.toastMenu.setVisibility(View.GONE);
			if(PlayerService.isPlay()) {
				mBind.viewMiniPlayer.setVisibility(View.VISIBLE);
			}
		} else {
			mBind.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_icon_selectall_enb);
			mBind.btnSelectAllText.setText(getString(R.string.select_cancel));
			mBind.btnSelectAllText.setTextColor(Color.parseColor("#696CFF"));
			isSelectAll = true;
			((TextView)mBind.toastMenu.findViewById(R.id.tv_select_count)).setText(String.valueOf(count));
			((TextView)mBind.toastMenu.findViewById(R.id.tv_select_count)).setContentDescription(String.format("%s개의 이야기가 선택됨", count));
			mBind.toastMenu.setVisibility(View.VISIBLE);
			mBind.viewMiniPlayer.setVisibility(View.GONE);
		}

		int countDownloaded = StoryDbManager.getInstance(this).getDownloadCount();
		String strCountUnit = getString(R.string.multi_story);
		if(countDownloaded<2){
			strCountUnit = getString(R.string.story);
		}
		mBind.tvCountUnit.setText(strCountUnit);

		mBind.tvCount.setText(String.valueOf(countDownloaded));
		mBind.tvStoryCount.setContentDescription(String.valueOf(countDownloaded) + strCountUnit);

		if (adapter.getItemCount() == 0) {
			mBind.list.setVisibility(View.GONE);
			mBind.emptyView.setVisibility(View.VISIBLE);
		} else {
			mBind.list.setVisibility(View.VISIBLE);
			mBind.emptyView.setVisibility(View.GONE);
		}

	}

	private void setRecyclerView() {
		//mBind.list.addItemDecoration(new SimpleDividerItemDecoration(this, SimpleDividerItemDecoration.DIVIDER_END_MARGIN));

		adapter = new StoryDownloadAdapter(this);
		mBind.list.setAdapter(adapter);
		adapter.updateItems(StoryDbManager.getInstance(this).getDownloadList().story);
		adapter.setItems(StoryDbManager.getInstance(this).getDownloadList().story);
		adapter.setOnItemClickListener(this);
	}

	@Override
	protected void onResume() {

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
//		mBind.viewMiniPlayer.checkPlayer();
		AnalyticsInterface.getInstance().screenView(this, "download_story_list");
	}

	@Override
	public void onItemClick(View view, int position) {

		if (view.getId() == R.id.front_layout) {

			//--
			AnalyticsInterface.getInstance().logEvent(activity, "download_story_detail");
			StoryItem content = adapter.getItem(position);
			Intent contentIntent = new Intent(this, StoryDetailWebActivity.class);
			contentIntent.putExtra("content", content);
			contentIntent.putExtra("type", StoryDetailActivity.STORY_DOWNLOAD_MODE);
			//추가
			//contentIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(contentIntent);

		} else if(view.getId() == R.id.thumbnail) {
			//--
			View rv = (View)view.getParent();
			if( rv!=null && rv.getId() == R.id.front_layout){
				adapter.getItem(position).isSelected = !adapter.getItem(position).isSelected;

				if (adapter.getItem(position).isSelected) {

					String description = String.format("%s %s 선택됨", position + 1, adapter.getItem(position).title);
					rv.setContentDescription( description );
					//===============================================================
					rv.setBackgroundColor(Color.parseColor("#F7F8FF"));
					rv.findViewById(R.id.view_selected).setVisibility(View.VISIBLE);
				} else {

					String description = String.format("%s %s 선택해제됨", position + 1, adapter.getItem(position).title);
					rv.setContentDescription( description );
					//===============================================================
					rv.setBackgroundColor(Color.parseColor("#FFFFFF"));
					rv.findViewById(R.id.view_selected).setVisibility(View.INVISIBLE);
				}

				String strSeeDetail = getString(R.string.toast_detail);
				if(adapter.getItem(position).isSelected) {
					rv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);

					String description = String.format("%s %s 선택됨, %s %s", position + 1, adapter.getItem(position).title, adapter.getItem(position).audioTitle, strSeeDetail);
					rv.setContentDescription(description);

				} else {
					rv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);

					String description = String.format("%s %s, %s %s", position + 1, adapter.getItem(position).title, adapter.getItem(position).audioTitle, strSeeDetail);
					rv.setContentDescription(description);
				}

				//thumbnail 접근성 부분클릭시
				if(adapter.getItem(position).isSelected){
					String description = String.format("%s %s 선택해제", position + 1, adapter.getItem(position).title);
					view.setContentDescription( description );
				}else{
					String description = String.format("%s %s 선택하기", position + 1, adapter.getItem(position).title);
					view.setContentDescription( description );
				}

				if(adapter.getItem(position).isSelected) {
					AnalyticsInterface.getInstance().logEvent(activity, "download_story_select");
				} else {
					AnalyticsInterface.getInstance().logEvent(activity, "download_story_deselect");
				}
			}

			check();
			//---------------------------

		} else if (view.getId() == R.id.btn_detail) {

			//--
			AnalyticsInterface.getInstance().logEvent(activity, "download_story_detail");
			StoryItem content = adapter.getItem(position);
			Intent contentIntent = new Intent(StoryDownloadActivity.this, StoryDetailWebActivity.class);
			contentIntent.putExtra("content", content);
			contentIntent.putExtra("type", StoryDetailActivity.STORY_DOWNLOAD_MODE);
			//추가
			//contentIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(contentIntent);

		} else if (view.getId() == R.id.btn_play) {

			AnalyticsInterface.getInstance().logEvent(activity, "downlaod_story_play");
			List<StoryItem> item = new ArrayList<>();
			item.add(adapter.getItem(position));
			List<StoryItem> list = StoryDbManager.getInstance(this).getPlayList().story;
			StoryDbManager.getInstance(this).clearPlayList();
			StoryDbManager.getInstance(this).addPlayList("D", item);
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
		AnalyticsInterface.getInstance().logEvent(activity, "download_story_delete");
		StoryItem item = adapter.getItem(position);
		StoryDbManager.getInstance(this).removeDownLoad(item);
		FileUtils.removeFile(this, item);
		adapter.removeItem(position);
		((MainActivity) OdiiApplication.getWebActivity()).refreshDownloadCount();
		if (position == 0) {
			adapter.notifyDataSetChanged();
		} else {
			adapter.notifyItemRemoved(position);
		}
		check();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_select_all:
				List<StoryItem> items = adapter.getItemAll();

				if (items.size() == 0) {
					return;
				}

				if (isSelectAll) {
					for (StoryItem item : items) {
						item.isSelected = false;
					}
					AnalyticsInterface.getInstance().logEvent(activity, "downlaod_story_deselect_all");
					isSelectAll = false;
					mBind.btnSelectAll.setContentDescription("전체 해체됨");
					mBind.btnSelectAll.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
				} else {
					for (StoryItem item : items) {
						item.isSelected = true;
					}
					AnalyticsInterface.getInstance().logEvent(activity, "downlaod_story_select_all");

					isSelectAll = true;
					mBind.btnSelectAll.setContentDescription("전체 선택됨");
					mBind.btnSelectAll.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
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

				break;
			case R.id.btn_toast_del:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_del");
				new FileDeleteAsyncTask().execute();
				break;
			case R.id.btn_toast_add:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_add");
				List<StoryItem> addlist = getCheckList();
				StoryDbManager.getInstance(this).addPlayList("D", addlist);
				setSelectedCancel();
				check();
				sendPlayInsertCast(addlist.size());
				break;
			case R.id.btn_toast_play:
				{
					if(adapter.getItemAll().isEmpty()) {
						return;
					}
					AnalyticsInterface.getInstance().logEvent(activity, "downlaod_story_play_all");
					String[] value1 = {getString(R.string.playlist_insert_play_msg), getString(R.string.playlist_replace_play_meg)};
					int[] items_resource1 = {R.drawable.ic_icon_all_add, R.drawable.ic_icon_all_replace};

					DialogUtil.showChoice(this, getString(R.string.all_play_msg), getString(R.string.playlist_insert_play_in_play), value1, items_resource1, getString(R.string.cancel), result -> {
						if (result ==  0) {
							AnalyticsInterface.getInstance().logEvent(activity, "popup_add_play");
							List<StoryItem> list = StoryDbManager.getInstance(this).getPlayList().story;
							List<StoryItem> checkItems = adapter.getItemAll();
							StoryDbManager.getInstance(this).clearPlayList();
							StoryDbManager.getInstance(this).addPlayList("D", checkItems);
							StoryDbManager.getInstance(this).addPlayList(list);
							Intent intent1 = new Intent(this, PlayerService.class);
							intent1.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
							startService(intent1);
							sendPlayInsertCast(checkItems.size());
						} else {
							AnalyticsInterface.getInstance().logEvent(activity, "popup_replace");
							PlayListManager.getInstance().setPlayIndex(0);
							List<StoryItem> checkItems = adapter.getItemAll();
							StoryDbManager.getInstance(this).clearPlayList();
							StoryDbManager.getInstance(this).addPlayList("D", checkItems);
							Intent playIntent = new Intent(this, PlayerService.class);
							playIntent.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
							startService(playIntent);
							sendPlayInsertCast(checkItems.size());
						}
						setSelectedCancel();
						check();
					});

					//-------------------------------------------------------------------------------
					break;
				}
			case R.id.btn_toast_locker:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_fill");
				List<StoryItem> checkList = getCheckList();
				StoryData data = new StoryData();
				for (StoryItem item : checkList) {
					if (item != null) {
						data.story.add(item);
					}
				}
				Intent intent = new Intent(this, StoryLockerActivity.class);
				intent.putExtra("type", StoryLockerActivity.FOLDER_SELECT_TYPE);
				InnerStorageSingleton.getSingleton().setData(data);
				startActivity(intent);
				setSelectedCancel();
				check();
				break;
			case R.id.btn_toast_cancel:
				AnalyticsInterface.getInstance().logEvent(activity, "bottom_menu_cancel");
				setSelectedCancel();
				check();
				break;
			case R.id.btn_play_all:
				if(adapter.getItemAll().isEmpty()) {
					return;
				}

				//--
				AnalyticsInterface.getInstance().logEvent(activity, "popup_add_play");
				List<StoryItem> list = StoryDbManager.getInstance(this).getPlayList().story;
				List<StoryItem> checkItems = adapter.getItemAll();
				StoryDbManager.getInstance(this).clearPlayList();
				StoryDbManager.getInstance(this).addPlayList("D", checkItems);
				StoryDbManager.getInstance(this).addPlayList(list);
				Intent intent1 = new Intent(this, PlayerService.class);
				intent1.setAction(PlayerConstants.ACTION_CLEAR_PLAY);
				startService(intent1);
				sendPlayInsertCast(checkItems.size());
				setSelectedCancel();
				check();
				//=================================================================================+

				break;
		}
	}

	private class FileDeleteAsyncTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			PlayerService.startActionPause_Edit(activity); //일시정시 호출
		}

		@Override
		protected Void doInBackground(Void... voids) {
			List<StoryItem> removeItem = getCheckList();

			for (StoryItem item : removeItem) {
				adapter.removeItem(item);
				FileUtils.removeFile(StoryDownloadActivity.this, item);
			}
			StoryDbManager.getInstance(StoryDownloadActivity.this).removeDownLoad(removeItem);
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			adapter.notifyDataSetChanged();
			check();
			((MainActivity) OdiiApplication.getWebActivity()).refreshDownloadCount();
			super.onPostExecute(aVoid);
		}
	}

	/**
	 * 전체 선택
	 *
	 * @return 선택된 StoryItem List
	 */
	private List<StoryItem> getCheckList() {
		List<StoryItem> items = adapter.getItemAll();
		List<StoryItem> result = new ArrayList<>();

		for (StoryItem item : items) {
			if (item != null && item.isSelected) {
				result.add(item);
			}
		}

		return result;
	}

	/**
	 * 전체 해체
	 */
	private void setSelectedCancel() {
		List<StoryItem> items = adapter.getItemAll();

		for (StoryItem item : items) {
			if (item != null) {
				item.isSelected = false;
			}
		}
		adapter.notifyDataSetChanged();
	}
}

