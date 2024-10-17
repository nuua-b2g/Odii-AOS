package kto.smarttour.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kto.smarttour.R;
import kto.smarttour.adapter.viewholder.TaxiStoryListItemViewHolder;
import kto.smarttour.common.BaseRecyclerViewAdapter;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.ImageUtil;
import kto.smarttour.db.item.StoryItem;

public class TaxiStoryListAdapter extends BaseRecyclerViewAdapter<StoryItem, TaxiStoryListItemViewHolder> {

	public ArrayList<Integer> selectArray = new ArrayList<>();

	public TaxiStoryListAdapter(Context context) {
		super(context);
		setHasStableIds(true);
	}

	private OnItemClickListener onItemClickListener;

	//추가 modeTabSelection
	private boolean modeTabSelection;
	private String lang;
	public String getLang(){
		return this.lang;
	}
	//추가
	private OnControlClickListener onControlClickListener;

	@Override
	public void onBindView(TaxiStoryListItemViewHolder holder, int position) {

		//-------------------------------------------------------------------------------------------------------------------------------------------
		StoryItem item = getItem(position);
		holder.binding.setStory(item);
		//-------------------------------------------------------------------------------------------------------------------------------------------
		holder.binding.btnPlay.setBackgroundResource( this.modeTabSelection?R.drawable.tx_img_txlist_play:R.drawable.tx_img_txlist_add);
		//-------------------------------------------------------------------------------------------------------------------------------------------

		String imageFile = FileUtils.getFileName(getContext(), item);
		//ImageUtil.localLoadImage2(holder.binding.ivThumb, imageFile, null);
		//Glide.with(getContext()).load(imageFile).dontAnimate().diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.bitmapTransform(new RoundedCorners(10))).into(holder.binding.ivThumb);
		Glide.with(getContext())
//				.asBitmap()
				.load(imageFile)
				.transform(new CenterCrop(), new RoundedCorners(24))
				.placeholder(R.drawable.ic_img_scrap_dummy2)
				.into(holder.binding.ivThumb);


		// 리스트 아이템 직접클릭이벤트는 우선 미처리
		/*
		holder.itemView.setOnClickListener(view -> {
			if (onItemClickListener != null) {
				onItemClickListener.onItemClick(view, holder.getAdapterPosition());

				if(selectArray.indexOf(holder.getAdapterPosition()) != -1) {
					selectArray.remove(selectArray.indexOf(holder.getAdapterPosition()));
				} else {
					selectArray.add(holder.getAdapterPosition());
				}

			}
		});
		*/

		//-------------------------------------------------------------------------------------------------------------
		//추가
		/*
		if(this.modeTabSelection){
			holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF")); //고정
		}
		*/

		//추가
		holder.binding.viewLocationControl.setOnClickListener(view -> {
			if (onControlClickListener != null){
				onControlClickListener.onControlClick_Action_Location(view, holder.getAdapterPosition());
			}
		});
		//추가
		holder.binding.viewActionControl.setOnClickListener(view -> {
			if (onControlClickListener != null){

				if(this.modeTabSelection){
					//재생버튼 목적기능 상태
					onControlClickListener.onControlClick_Action_Play(view, holder.getAdapterPosition());
				}else{

					//선택목록추가 목적기능 상태
					onControlClickListener.onControlClick_Action_Add(view, holder.getAdapterPosition());

					//내부 선택아이템 데이터치리 (아이템의 상태변경이 아닌 대상indexPosition의 추가/삭제)
					if( selectArray.contains(holder.getAdapterPosition()) ) {
						selectArray.remove(selectArray.indexOf(holder.getAdapterPosition()));
					} else {
						selectArray.add(holder.getAdapterPosition());
					}


				}

			}
		});
		//-------------------------------------------------------------------------------------------------------------
	}

	public boolean isEmpty() {
		return getItemAll().isEmpty();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.taxi_story_item, parent, false);
		return new TaxiStoryListItemViewHolder(view);
	}

	public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	@Override
	public void OnItemMoveComplete() {

	}

	public interface OnItemClickListener {
		void onItemClick(View view, int position);
	}

	//추가
	public void setOnControlClickListener(OnControlClickListener onControlClickListener) {
		this.onControlClickListener = onControlClickListener;
	}
	//추가
	public interface OnControlClickListener {
		void onControlClick_Action_Location(View view, int position);
		void onControlClick_Action_Play(View view, int position);
		void onControlClick_Action_Add(View view, int position);
	}
	//추가
	public void setModeTabSelection(boolean modeTabSelection, String lang){
		this.modeTabSelection = modeTabSelection;
		this.lang = lang;
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

}
