package kto.smarttour.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chauthai.swipereveallayout.ViewBinderHelper;

import java.util.List;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.adapter.viewholder.StoryHeaderViewHolder;
import kto.smarttour.adapter.viewholder.StoryViewHolder;
import kto.smarttour.common.BaseRecyclerViewAdapter;
import kto.smarttour.common.utils.ImageUtil;
import kto.smarttour.common.utils.LocationDistance;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryFolderItem;
import kto.smarttour.db.item.StoryItem;

public class StoryLockerDetailAdapter extends BaseRecyclerViewAdapter<StoryItem, RecyclerView.ViewHolder> {

	private OnItemClickListener onItemClickListener;
	private OnItemLongClickListener onItemLongClickListener;
	private final ViewBinderHelper binderHelper = new ViewBinderHelper();
	private StoryFolderItem headerItem;
	private Context context;
	public StoryLockerDetailAdapter(Context context) {
		super(context);
		this.context = context;
		setHasStableIds(true);
		binderHelper.setOpenOnlyOne(true);
	}

	@Override
	public int getItemViewType(int position) {
		if (position == 0) {
			return 0;
		} else {
			return position;
		}
	}

	public void setHeaderItem(StoryFolderItem headerItem) {
		this.headerItem = headerItem;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;

		if (viewType == 0) {
			view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_folder_detail_header, parent, false);
			return new StoryHeaderViewHolder(view);
		} else {
			view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_item, parent, false);
			return new StoryViewHolder(view);
		}
	}

	@Override
	public void onBindView(RecyclerView.ViewHolder holder, int position) {

		if (position == 0) {
			((StoryHeaderViewHolder) holder).binding.setFolder(headerItem);

			if (!TextUtils.isEmpty(headerItem.imgUrl)) {
				ImageUtil.localLoadImage(((StoryHeaderViewHolder) holder).binding.image, headerItem.imgUrl, null);
			} else {
				((StoryHeaderViewHolder) holder).binding.image.setBackgroundColor(Color.parseColor("#B3000000"));
			}
			((StoryHeaderViewHolder) holder).binding.tvCount.setText(String.valueOf(StoryDbManager.getInstance(getContext()).getStoryCount(headerItem.seq)));
			((StoryHeaderViewHolder) holder).binding.tvCount.setContentDescription(String.valueOf(StoryDbManager.getInstance(getContext()).getStoryCount(headerItem.seq)));
			((StoryHeaderViewHolder) holder).binding.btnSelectAll.setOnClickListener(view -> {
				if (onItemClickListener != null) {
					onItemClickListener.onClickSelectAll();
				}
			});

			((StoryHeaderViewHolder) holder).binding.btnPlayAll.setOnClickListener(view -> {
				if (onItemClickListener != null) {
					onItemClickListener.onClickSelectAllPlay();
				}
			});

			((StoryHeaderViewHolder) holder).binding.btnDel.setOnClickListener(view -> {
				if (onItemClickListener != null) {
					onItemClickListener.onClickDelete();
				}
			});

			((StoryHeaderViewHolder) holder).binding.btnStoryInfoView.setOnClickListener(view -> {
				if (onItemClickListener != null) {
					onItemClickListener.onClickEdit();
				}
			});

			List<StoryItem> items = getItemAll();
			int count = 0;
			for (StoryItem item : items) {
				if (item != null && item.isSelected) {
					count++;
				}
			}

			if (getItemCount() == 1) {
				((StoryHeaderViewHolder) holder).binding.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_icon_selectall_dis);
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setText(context.getString(R.string.all_select));
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setContentDescription(context.getString(R.string.all_select));
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setTextColor(Color.parseColor("#000000"));
				//				isSelectAll = false;
			} else if (count == getItemCount() - 1) {
				((StoryHeaderViewHolder) holder).binding.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_icon_selectall_enb);
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setText(context.getString(R.string.all_select_cancel));
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setContentDescription(context.getString(R.string.all_select_cancel));
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setTextColor(Color.parseColor("#696CFF"));
				//				isSelectAll = true;
			} else if (count == 0) {
				((StoryHeaderViewHolder) holder).binding.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_icon_selectall_dis);
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setText(context.getString(R.string.all_select));
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setContentDescription(context.getString(R.string.all_select));
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setTextColor(Color.parseColor("#000000"));
				//				isSelectAll = false;
			} else {
				((StoryHeaderViewHolder) holder).binding.btnSelectAllIcon.setBackgroundResource(R.drawable.ic_icon_selectall_enb);
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setText(context.getString(R.string.select_cancel));
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setContentDescription(context.getString(R.string.select_cancel));
				((StoryHeaderViewHolder) holder).binding.btnSelectAllText.setTextColor(Color.parseColor("#696CFF"));
				//				isSelectAll = true;
			}

		} else {
			StoryItem item = getItem(position);
			((StoryViewHolder) holder).binding.setStory(item);
			binderHelper.bind(((StoryViewHolder) holder).binding.swipeLayout, String.valueOf(item.aid));

			if(item.isSelected) {
				((StoryViewHolder) holder).binding.viewSelected.setVisibility(View.VISIBLE);
			} else {
				((StoryViewHolder) holder).binding.viewSelected.setVisibility(View.GONE);
			}

			((StoryViewHolder) holder).binding.frontLayout.setOnClickListener(v -> {
				if (onItemClickListener != null) {
					onItemClickListener.onItemClick(v, holder.getAdapterPosition());
				}
			});
			((StoryViewHolder) holder).binding.deleteLayout.setOnClickListener(v -> {
				if (onItemClickListener != null) {
					binderHelper.closeLayout(String.valueOf(item.aid));
					onItemClickListener.onItemDeleteClick(v, holder.getAdapterPosition());

				}
			});
			((StoryViewHolder) holder).binding.btnDetail.setOnClickListener(v -> {
				if (onItemClickListener != null) {
					onItemClickListener.onItemClick(v, holder.getAdapterPosition());
				}
			});

			((StoryViewHolder) holder).binding.btnPlay.setOnClickListener( v -> {
				if (onItemClickListener != null) {
					onItemClickListener.onItemClick(v, holder.getAdapterPosition());
				}
			});

			if (OdiiApplication.getLocation() != null && !TextUtils.isEmpty(item.posX) && !TextUtils.isEmpty(item.posY)) {
				((StoryViewHolder) holder).binding.viewDistance.setVisibility(View.VISIBLE);
				((StoryViewHolder) holder).binding.tvDistance.setText(String.format("%.2f Km", LocationDistance.distance(OdiiApplication.getLocation().getLatitude(), OdiiApplication.getLocation().getLongitude(), Double.parseDouble(item.posY), Double.parseDouble(item.posX), "Km")));
				((StoryViewHolder) holder).binding.tvDistance.setContentDescription(String.format("%.2f Km", LocationDistance.distance(OdiiApplication.getLocation().getLatitude(), OdiiApplication.getLocation().getLongitude(), Double.parseDouble(item.posY), Double.parseDouble(item.posX), "Km")));
			} else {
				((StoryViewHolder) holder).binding.viewDistance.setVisibility(View.INVISIBLE);
			}
		}
	}

	@Override
	public int getItemCount() {
		return super.getItemCount();
	}

	public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
		this.onItemLongClickListener = onItemLongClickListener;
	}

	@Override
	public void OnItemMoveComplete() {

	}

	public interface OnItemClickListener {
		void onItemClick(View view, int position);

		void onItemDeleteClick(View view, int position);

		void onClickSelectAll();

		void onClickSelectAllPlay();

		void onClickDelete();

		void onClickEdit();
	}

	public interface OnItemLongClickListener {
		void onItemLongClick(View view, int position);
	}


}
