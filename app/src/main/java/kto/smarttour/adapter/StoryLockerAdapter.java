package kto.smarttour.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chauthai.swipereveallayout.ViewBinderHelper;

import kto.smarttour.R;
import kto.smarttour.adapter.viewholder.StoryFolderViewHolder;
import kto.smarttour.common.BaseRecyclerViewAdapter;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryFolderItem;
import kto.smarttour.ui.StoryLockerActivity;

public class StoryLockerAdapter extends BaseRecyclerViewAdapter<StoryFolderItem, StoryFolderViewHolder> {
	private OnItemClickListener onItemClickListener;
	private OnItemLongClickListener onItemLongClickListener;
	private final ViewBinderHelper binderHelper = new ViewBinderHelper();

	public StoryLockerAdapter(Context context) {
		super(context);
		setHasStableIds(true);
		binderHelper.setOpenOnlyOne(true);
	}

	@Override
	public void onBindView(StoryFolderViewHolder holder, int position) {
		StoryFolderItem item = getItem(position);
		holder.binding.setFolder(item);
		binderHelper.bind(holder.binding.swipeLayout, String.valueOf(item.seq));
		holder.binding.frontLayout.setOnClickListener(v -> {
			if (onItemClickListener != null) {
				onItemClickListener.onItemClick(v, holder.getAdapterPosition());
			}
		});
		holder.binding.deleteLayout.setOnClickListener(v -> {
			if (onItemClickListener != null) {
				binderHelper.closeLayout(String.valueOf(item.seq));
				onItemClickListener.onItemDeleteClick(v, holder.getAdapterPosition());

			}
		});

		holder.binding.btnPlay.setOnClickListener(v -> {
			if (onItemClickListener != null && !TextUtils.isEmpty(item.fileData) && Integer.parseInt(item.fileData) > 0) {
				onItemClickListener.onItemPlayBoxClick(v, holder.getAdapterPosition());
			}
		});

		if (item.isSelected) {
			holder.binding.frontLayout.setBackgroundColor(Color.parseColor("#F7F8FF"));
		} else {
			holder.binding.frontLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
		}
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_folder_item, parent, false);
		return new StoryFolderViewHolder(view);
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

		void onItemPlayBoxClick(View view, int position);
	}


	public interface OnItemLongClickListener {

		void onItemLongClick(View view, int position);
	}
}
