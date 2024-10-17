package kto.smarttour.adapter;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;

import kto.smarttour.R;
import kto.smarttour.adapter.viewholder.DriverStoryListItemViewHolder;
import kto.smarttour.binding.BindingAdapters;
import kto.smarttour.common.BaseRecyclerViewAdapter;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.ui.player.DriverPlayer;

public class DriverStoryListAdapter extends BaseRecyclerViewAdapter<StoryItem, DriverStoryListItemViewHolder> {

    private OnStartDragListener onStartDragListener;
    private OnItemClickListener onItemClickListener;
    private Context context;

    private int currentPosition = 0;
    private boolean bEdit = false;
    private boolean isTaxi = true;

    public void setIndex(int index) {

        if(getItemCount() - 1 <= index) {
            index = getItemCount() - 1;
        }

        currentPosition = index;
        notifyDataSetChanged();
    }

    public int getIndex() {
        return currentPosition;
    }


    public DriverStoryListAdapter(Context context, int currentPosition) {
        super(context);
        this.context = context;
        this.currentPosition = currentPosition;
        this.isTaxi = true;
        setHasStableIds(true);
    }

    public DriverStoryListAdapter(Context context, int currentPosition, boolean isTaxi) {
        super(context);
        this.context = context;
        this.currentPosition = currentPosition;
        this.isTaxi = isTaxi;
        setHasStableIds(true);
    }

    public void setOnStartDragListener(OnStartDragListener onStartDragListener) {
        this.onStartDragListener = onStartDragListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);

        void onEndDrag(int fromPosition, int toPosition);
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemDelete(View view, int position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.driver_story_item, parent, false);
        return new DriverStoryListItemViewHolder(view);
    }

    @Override
    public void onBindView(DriverStoryListItemViewHolder holder, int position) {
        StoryItem item = getItem(position);
        holder.binding.setStory(item);

        holder.binding.btnSort.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    fromPosition = holder.getAdapterPosition();
                    onStartDragListener.onStartDrag(holder);
                }
                return false;
            }
        });
        holder.binding.btnDel.setOnClickListener(v -> {
            onItemClickListener.onItemDelete(v, holder.getAdapterPosition());
        });

        if (currentPosition == position) {
            holder.binding.ivCurrent.setVisibility(View.VISIBLE);
            holder.binding.tvIndex.setVisibility(View.INVISIBLE);
            holder.binding.itemView.setBackgroundColor(Color.parseColor("#1E1E1E"));
            holder.binding.tvTitile.setTextColor(Color.parseColor("#F9ED1C"));
            holder.binding.tvTime.setTextColor(Color.parseColor("#F9ED1C"));

            holder.binding.tvTitile.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            holder.binding.tvTitile.setSelected(true);

        } else {
            holder.binding.ivCurrent.setVisibility(View.INVISIBLE);
            holder.binding.tvIndex.setVisibility(View.VISIBLE);
            holder.binding.itemView.setBackgroundColor(Color.parseColor("#000000"));
            holder.binding.tvTitile.setTextColor(Color.parseColor("#FFFFFF"));
            holder.binding.tvTime.setTextColor(Color.parseColor("#ACACAC"));

            holder.binding.tvTitile.setEllipsize(TextUtils.TruncateAt.END);
            holder.binding.tvTitile.setSelected(false);
        }

        holder.binding.tvIndex.setText(String.format("%s", position + 1));
        holder.binding.tvIndex.setContentDescription(String.format("%s", position + 1));

        holder.itemView.setOnClickListener(view -> {
            if (onItemClickListener != null && !bEdit) {
                onItemClickListener.onItemClick(view, holder.getAdapterPosition());
            }
        });

        if (SettingsUtil.getTaxiTtid(holder.itemView.getContext()) == -1) {
            holder.binding.tvTitile.setText(item.title);
        } else {
            if (!TextUtils.isEmpty(item.titleKo)) {
                holder.binding.tvTitile.setText(item.titleKo);
            } else {
                holder.binding.tvTitile.setText(item.title);
            }
        }

        if (bEdit) {
            if(!isTaxi) {
                holder.binding.tvLang.setVisibility(View.GONE);
            } else {
                if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    if (!TextUtils.isEmpty(item.langCode)) {
                        holder.binding.tvLang.setVisibility(View.VISIBLE);
                        BindingAdapters.setDriverLangCodeBox(holder.binding.tvLang, item.langCode);
                    } else {
                        holder.binding.tvLang.setVisibility(View.GONE);
                    }
                } else {
                    holder.binding.tvLang.setVisibility(View.GONE);
                }
            }
            holder.binding.btnDel.setVisibility(View.VISIBLE);
            holder.binding.btnSort.setVisibility(View.VISIBLE);
        } else {
            if(!isTaxi) {
                holder.binding.tvLang.setVisibility(View.GONE);
            } else {
                if (!TextUtils.isEmpty(item.langCode)) {
                    holder.binding.tvLang.setVisibility(View.VISIBLE);
                    BindingAdapters.setDriverLangCodeBox(holder.binding.tvLang, item.langCode);
                } else {
                    holder.binding.tvLang.setVisibility(View.GONE);
                }
            }

            holder.binding.btnDel.setVisibility(View.GONE);
            holder.binding.btnSort.setVisibility(View.GONE);
        }


    }

    public boolean isEmpty() {
        return getItemAll().isEmpty();
    }

    public void setEdit(boolean bEdit) {
        this.bEdit = bEdit;
        notifyDataSetChanged();
    }

    int fromPosition, toPosition;

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        super.onItemMove(fromPosition, toPosition);
        this.toPosition = toPosition;
    }

    @Override
    public void OnItemMoveComplete() {
        onStartDragListener.onEndDrag(fromPosition, toPosition);
    }


    @Override
    public long getItemId(int position) {
        return getItem(position).seq;
    }
}
