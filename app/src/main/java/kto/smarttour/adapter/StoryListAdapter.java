package kto.smarttour.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import kto.smarttour.R;
import kto.smarttour.adapter.viewholder.StoryListItemViewHolder;
import kto.smarttour.common.BaseRecyclerViewAdapter;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.ImageUtil;
import kto.smarttour.common.utils.ViewUtils;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.ui.player.PlayListManager;

public class StoryListAdapter extends BaseRecyclerViewAdapter<StoryItem, StoryListItemViewHolder> {

	//============================================
	public enum LIST_MODE {
		NORMAL,
		REORDER,
		EDIT
	}
	private LIST_MODE currentMode = LIST_MODE.NORMAL;
	public void setMode(LIST_MODE mode){
		this.currentMode = mode;
		notifyDataSetChanged();
	}
	public LIST_MODE getMode(){
		return this.currentMode;
	}
	//============================================
	//============================================
	//============================================

	private OnItemClickListener onItemClickListener;
	private final ViewBinderHelper binderHelper = new ViewBinderHelper();

	private OnStartDragListener onStartDragListener;
	private Context context;

	private int currentPosition = 0;

	public StoryListAdapter(Context context) {
		super(context);
		this.context = context;
		setHasStableIds(false);
		binderHelper.setOpenOnlyOne(true);
	}

	/*
	private boolean bEdit = false;
	public void setEdit(boolean bEdit) {
		this.bEdit = bEdit;
		notifyDataSetChanged();
	}

	public boolean isEdit() {
		return this.bEdit;
	}
	*/

	public void setOnStartDragListener(OnStartDragListener onStartDragListener) {
		this.onStartDragListener = onStartDragListener;
	}

	public int getCurrentPosition() {
		return currentPosition;
	}

	public void setCurrentPosition(int currentPosition) {
		int oldPosition = this.currentPosition;
		this.currentPosition = currentPosition;
		notifyItemChanged(currentPosition);
		notifyItemChanged(oldPosition);
//		notifyDataSetChanged();
	}

	public interface OnStartDragListener {
		void onStartDrag(RecyclerView.ViewHolder viewHolder);
		void onEndDrag(int fromPosition, int toPosition);
	}

	public boolean isEmpty() {
		return getItemAll().isEmpty();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.play_list_item, parent, false);
		return new StoryListItemViewHolder(view);
	}

	@Override
	public void onBindView(StoryListItemViewHolder holder, int position) {
		StoryItem item = getItem(position);
		holder.binding.setStory(item);
		binderHelper.bind(holder.binding.swipeLayout, String.valueOf(item.seq));
		if (currentPosition == position) {

			holder.binding.ivCurrentPosition.setVisibility(View.INVISIBLE); //인덱스 표기 상시숨김으로 변경  VISIBLE -> INVISIBLE

			holder.binding.tvCurrentPosition.setVisibility(View.VISIBLE);

			holder.binding.tvIndex.setVisibility(View.VISIBLE); //인덱스 표기 상시출력으로 변경
			holder.binding.frontLayout.setBackgroundColor(Color.parseColor("#F7F8FF"));

			//String description = String.format("%s 재생 선택됨 %s %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
			String description = String.format("%s %s 재생 선택됨, %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
			holder.binding.frontLayout.setContentDescription(description);
		} else {
			holder.binding.ivCurrentPosition.setVisibility(View.INVISIBLE); //기존 출력조건

			holder.binding.tvCurrentPosition.setVisibility(View.INVISIBLE);

			holder.binding.tvIndex.setVisibility(View.VISIBLE); //기존 출력조건

			//편집모드
			if(this.currentMode == LIST_MODE.EDIT)
			{

				if(item.isSelected) {
					//String description = String.format("%s 선택됨 %s %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
					String description = String.format("%s %s 선택됨, %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
					holder.binding.frontLayout.setContentDescription(description);
				} else {
					//String description = String.format("%s %s %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
					String description = String.format("%s %s, %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
					holder.binding.frontLayout.setContentDescription(description);
				}


			}
			//순서변경모드
			else if(this.currentMode == LIST_MODE.REORDER)
			{

				//순서변경모드 선택상태 고려안함
				//String description = String.format("%s %s %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
				String description = String.format("%s %s, %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
				holder.binding.frontLayout.setContentDescription(description);

			}
			else
			{
				//String description = String.format("%s %s %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
				String description = String.format("%s %s, %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
				holder.binding.frontLayout.setContentDescription(description);
			}

			holder.binding.frontLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
		}

		holder.binding.tvIndex.setText(String.format("%s", position + 1));
		holder.binding.tvIndex.setContentDescription(String.format("%s", position + 1));
		//ImageUtil.loadImage(holder.binding.ivThumb, item.thumbnailFilePath, null); //라운드 지정하는형식으로 커스텀 추가필요
		ImageUtil.loadImage_CornerRadius(holder.binding.ivThumb, item.thumbnailFilePath, null, 24); //라운드 지정하는형식으로 커스텀 추가필요

		//기존 btnMore 숨기기
		holder.binding.btnMore.setVisibility(View.GONE);

		//편집모드
		if(this.currentMode == LIST_MODE.EDIT)
		{

			//편집모드이지만 순서변경모드 x
			holder.binding.btnMove.setVisibility(View.GONE);
			/*숨김상태
			holder.binding.btnMore.setOnClickListener(v -> {
				onItemClickListener.OnClickMore(v, item);
			});
			*/


			//선택 편집모드에서만 선택아이템 활성화 표시
			if(item.isSelected) {
				holder.binding.frontLayout.setBackgroundColor(Color.parseColor("#F7F8FF"));
				holder.binding.viewSelected.setVisibility(View.VISIBLE);
			} else {
				holder.binding.frontLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
				holder.binding.viewSelected.setVisibility(View.GONE);
			}
		}
		//순서변경모드
		else if(this.currentMode == LIST_MODE.REORDER)
		{
			//편집모드의 다른형태 (순서변경모드)
			//holder.binding.btnMore.setVisibility(View.INVISIBLE);
			//holder.binding.btnMore.setVisibility(View.GONE); //수정 -> 상시숨김 (move버튼 위치)


			//순서변경모드 선택아이템 활성화 표시 x
			holder.binding.viewSelected.setVisibility(View.GONE);
			//-------------------------------------------------------

			holder.binding.btnMove.setVisibility(View.VISIBLE);
			holder.binding.btnMove.setOnTouchListener((v, event) -> {
				if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
					fromPosition = holder.getAdapterPosition();
					onStartDragListener.onStartDrag(holder);
				}
				return false;
			});

		}
		else
		{

			//일반모드 선택아이템 활성화 표시 x
			holder.binding.viewSelected.setVisibility(View.GONE);
			//-------------------------------------------------------

			//holder.binding.btnMore.setVisibility(View.VISIBLE);
			//holder.binding.btnMore.setVisibility(View.GONE); //수정 -> 상시숨김 (move버튼 위치)

			holder.binding.btnMove.setVisibility(View.GONE);
			holder.binding.btnMore.setOnClickListener(v -> {
				onItemClickListener.OnClickMore(v, item);
			});

		}

		//추가 이벤트 리스너 연결, holder.binding.btnMore 버튼 상시숨김 대신 -> 새로운 뷰이 에빈트 연결
		holder.binding.btnMoreDetail.setOnClickListener(v -> {
			//onItemClickListener.OnClickMore(v, item);
			if(this.currentMode == LIST_MODE.NORMAL){
				onItemClickListener.OnClickDetail(v, item); //신규추가 인터페이스로 바로연결
			}

		});

		holder.binding.frontLayout.setOnClickListener(v -> {

			//편집모드
			if(this.currentMode == LIST_MODE.EDIT)
			{
				//편집모드이지만 순서변경모드 x
				if(item.isSelected) {
					item.isSelected = false;
					AnalyticsInterface.getInstance().logEvent(context, "play_list_deselect");
					v.setBackgroundColor(Color.parseColor("#FFFFFF"));
					v.setContentDescription("선택해제됨");
					v.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
					v.findViewById(R.id.view_selected).setVisibility(View.GONE);

					//String description = String.format("%s %s %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
					String description = String.format("%s %s, %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
					v.setContentDescription(description);
				} else {
					item.isSelected = true;
					AnalyticsInterface.getInstance().logEvent(context, "play_list_select");
					v.setBackgroundColor(Color.parseColor("#F7F8FF"));

					//String description = String.format("%s 선택됨 %s %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
					String description = String.format("%s %s 선택됨, %s", holder.getAdapterPosition() + 1, item.title, item.audioTitle);
					holder.binding.frontLayout.setContentDescription(description);
					v.findViewById(R.id.view_selected).setVisibility(View.VISIBLE);
					v.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
				}

				onItemClickListener.onItemSelect(v, holder.getAdapterPosition());

			}
			//순서변경모드
			else if(this.currentMode == LIST_MODE.REORDER)
			{
				//편집모드의 다른형태 (순서변경모드) 선택상태 전달하지말자.
				//-- nothing to do

			}
			else
			{
				//일반 아이템 클릭 이벤트 전달
				onItemClickListener.onItemClick(v, holder.getAdapterPosition());
			}

		});

		holder.binding.deleteLayout.setOnClickListener(v -> {
			onItemClickListener.onItemDeleteClick(v, holder.getAdapterPosition());
		});

		//미디어 길이(재생시간) 출력
		if(item.audioPlayTime!=null){
			int timeUnit = 1; //밀리세컨드 단위일경우 1000, 지금은 초단위로 받고있으므로 1
			int time = 0;
			try {
				time = Integer.parseInt(item.audioPlayTime);
			} catch (NumberFormatException e) {
				//e.printStackTrace();
			}
			//---- 시간변환
			holder.binding.tvAudioPlayTime.setText( String.format("%02d:%02d", time / (60 * timeUnit) % 60, time / timeUnit % 60) );
		}else{
			//----
			holder.binding.tvAudioPlayTime.setText("--:--");
		}

	}

	@Override
	public int getItemCount() {
		return super.getItemCount();
	}

	public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	public interface OnItemClickListener {
		void OnClickMore(View view, StoryItem item);
		void OnClickDetail(View view, StoryItem item);//인터페이스 추가  OnClickMore->상세보기 바로호출
		void onItemClick(View view, int position);
		void onItemDeleteClick(View view, int position);
		void onItemSelect(View view, int position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).seq;
	}

	int fromPosition, toPosition;



	@Override
	public void onItemMove(int fromPosition, int toPosition) {
		super.onItemMove(fromPosition, toPosition);
		this.toPosition = toPosition;
		if(fromPosition < toPosition) {
			ViewUtils.announceForAccessibility(context, String.format("%s %s %s 아래로 내려감", toPosition + 1, getItem(fromPosition).title, getItem(fromPosition).audioTitle));
		} else {
			ViewUtils.announceForAccessibility(context, String.format("%s %s %s 위로 올라감", toPosition + 1, getItem(fromPosition).title, getItem(fromPosition).audioTitle));
		}

	}

	@Override
	public void OnItemMoveComplete() {
		onStartDragListener.onEndDrag(fromPosition, toPosition);
	}

	public void setSort(int sortType) {
		ArrayList<StoryItem> checkItems = getItemAll();
		int pindex = checkItems.get(currentPosition).pindex;
		Comparator<StoryItem> comp;
		if (sortType == 0) {
			//이야기 제목 (가~하 순)
			comp = new ComparatorByTitleAsc();
			Collections.sort(checkItems, comp);
		} else if (sortType == 1) {
			//이야기 제목 (하~가 순)
			comp = new ComparatorByTitleDesc();
			Collections.sort(checkItems, comp);
		} else if (sortType == 2) {
			//지역명 (가~하 순)
			comp = new ComparatorByAreaAsc();
			Collections.sort(checkItems, comp);
		} else if (sortType == 3) {
			//지역명 (하~가 순)
			comp = new ComparatorByAreaDesc();
			Collections.sort(checkItems, comp);
		}

		for (int i = 0; i < checkItems.size(); i++) {
			if (checkItems.get(i).pindex == pindex) {
				currentPosition = i;
				break;
			}
		}
		StoryDbManager.getInstance(context).clearPlayList();
		StoryDbManager.getInstance(context).addPlayList(checkItems);
		PlayListManager.getInstance().setPlayIndex(currentPosition);
		ArrayList<StoryItem> reLoadItem = StoryDbManager.getInstance(context).getPlayList().story;
		updateItems(reLoadItem);
	}

	private class ComparatorByTitleAsc implements Comparator<StoryItem> {
		@Override
		public int compare(StoryItem lhs, StoryItem rhs) {
			return lhs.title.compareTo(rhs.title);
		}
	}

	private class ComparatorByTitleDesc implements Comparator<StoryItem> {
		@Override
		public int compare(StoryItem lhs, StoryItem rhs) {
			return rhs.title.compareTo(lhs.title);
		}
	}

	private class ComparatorByAreaAsc implements Comparator<StoryItem> {
		@Override
		public int compare(StoryItem lhs, StoryItem rhs) {
			return lhs.addrList.compareTo(rhs.addrList);
		}
	}

	private class ComparatorByAreaDesc implements Comparator<StoryItem> {
		@Override
		public int compare(StoryItem lhs, StoryItem rhs) {
			return rhs.addrList.compareTo(lhs.addrList);
		}
	}
}
