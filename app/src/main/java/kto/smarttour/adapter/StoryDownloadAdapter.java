package kto.smarttour.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;

import com.chauthai.swipereveallayout.ViewBinderHelper;

import java.util.ArrayList;
import java.util.List;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.adapter.viewholder.StoryDownloadViewHolder;
import kto.smarttour.common.BaseRecyclerViewAdapter;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.ImageUtil;
import kto.smarttour.common.utils.LocationDistance;
import kto.smarttour.db.item.StoryItem;

public class StoryDownloadAdapter extends BaseRecyclerViewAdapter<StoryItem, StoryDownloadViewHolder> implements Filterable {
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private final ViewBinderHelper binderHelper = new ViewBinderHelper();

    public StoryDownloadAdapter(Context context) {
        super(context);
        setHasStableIds(true);
        binderHelper.setOpenOnlyOne(true);
    }


    @NonNull
    @Override
    public StoryDownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_download_item, parent, false);
        return new StoryDownloadViewHolder(view);
    }

    @Override
    public void onBindView(StoryDownloadViewHolder holder, int position) {
        StoryItem item = getItem(position);
        holder.binding.setStory(item);
        binderHelper.bind(holder.binding.swipeLayout, String.valueOf(item.aid));

        String imageFile = FileUtils.getFileName(getContext(), item);
        //ImageUtil.localLoadImage2(holder.binding.thumbnail, imageFile, null);
        ImageUtil.localLoadImage2_CornerRadius(holder.binding.thumbnail, imageFile, null, 24);

        // frontLayout
        holder.binding.frontLayout.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, holder.getAdapterPosition());
            }
        });

        //frontLayout접근성 상태
        String strSeeDetail = getContext().getString(R.string.toast_detail);
        if(item.isSelected){
            String description = String.format("%s %s 선택됨, %s %s", position + 1, item.title, item.audioTitle, strSeeDetail);
            holder.binding.frontLayout.setContentDescription( description );
        }else{
            String description = String.format("%s %s, %s %s", position + 1, item.title, item.audioTitle, strSeeDetail);
            holder.binding.frontLayout.setContentDescription( description );
        }

        //선택 상태 이미지
        if(item.isSelected) {
            holder.binding.viewSelected.setVisibility(View.VISIBLE);
        } else {
            holder.binding.viewSelected.setVisibility(View.INVISIBLE);
        }

        holder.binding.thumbnail.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, holder.getAdapterPosition());
            }
        });
        //thumbnail 접근성
        if(item.isSelected){
            String description = String.format("%s %s 선택해제", position + 1, item.title);
            holder.binding.thumbnail.setContentDescription( description );
        }else{
            String description = String.format("%s %s 선택하기", position + 1, item.title);
            holder.binding.thumbnail.setContentDescription( description );
        }


        holder.binding.deleteLayout.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                binderHelper.closeLayout(String.valueOf(item.aid));
                onItemClickListener.onItemDeleteClick(v, holder.getAdapterPosition());

            }
        });
        holder.binding.btnDetail.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, holder.getAdapterPosition());
            }
        });

        holder.binding.btnPlay.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, holder.getAdapterPosition());
            }
        });
        //btnPlay 접근성 좀더 자세히 ( xml contentDescription 보다 조합형 "타이틀 + 재생"버튼)
        String strPlay = String.format("%s %s, %s", position + 1, item.title, getContext().getString(R.string.audio_play));
        holder.binding.btnPlay.setContentDescription(strPlay);

        //거리출럭 삭제 - 상시숨김 xml.INVISIBLE
        holder.binding.viewDistance.setVisibility(View.INVISIBLE);
        /*
        if (OdiiApplication.getLocation() != null && !TextUtils.isEmpty(item.posX) && !TextUtils.isEmpty(item.posY)) {
            holder.binding.viewDistance.setVisibility(View.VISIBLE);
            holder.binding.tvDistance.setText(String.format("%.2f Km", LocationDistance.distance(OdiiApplication.getLocation().getLatitude(), OdiiApplication.getLocation().getLongitude(), Double.parseDouble(item.posY), Double.parseDouble(item.posX), "Km")));
            holder.binding.tvDistance.setContentDescription(String.format("%.2f Km", LocationDistance.distance(OdiiApplication.getLocation().getLatitude(), OdiiApplication.getLocation().getLongitude(), Double.parseDouble(item.posY), Double.parseDouble(item.posX), "Km")));
        } else {
            holder.binding.viewDistance.setVisibility(View.INVISIBLE);
        }
        */

    }

    public List<StoryItem> getSelectList() {
        List<StoryItem> playList = new ArrayList<>();
        List<StoryItem> allItem = getItemAll();
        for (StoryItem item : allItem) {
            if (item != null && item.isSelected) {
                playList.add(item);
            }
        }

        return playList;
    }


    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    private List<StoryItem> items;

    public void setItems(List<StoryItem> items) {
        this.items = items;
    }

    private List<StoryItem> storyItemsFiltered;

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                if (storyItemsFiltered == null) {
                    storyItemsFiltered = new ArrayList<>();
                }
                storyItemsFiltered.clear();
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    storyItemsFiltered.addAll(items);
                } else {
                    List<StoryItem> filteredList = new ArrayList<>();
                    for (StoryItem row : items) {
                        if (row.title.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }
                    storyItemsFiltered.addAll(filteredList);
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = storyItemsFiltered;
                filterResults.count = storyItemsFiltered.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                ArrayList<StoryItem> filteredItems = (ArrayList<StoryItem>) filterResults.values;
                updateItems(filteredItems);
            }
        };
    }

    @Override
    public void OnItemMoveComplete() {

    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemDeleteClick(View view, int position);
    }


    public interface OnItemLongClickListener {

        void onItemLongClick(View view, int position);
    }
}
