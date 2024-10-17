package kto.smarttour.common;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kto.smarttour.common.utils.ItemTouchHelperAdapter;

public abstract class BaseRecyclerViewAdapter<T, H extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperAdapter {

	protected ArrayList<T> arrayList = new ArrayList<>();

	private Context context;


	public BaseRecyclerViewAdapter(Context context) {
		this.context = context;
	}

	public Context getContext() {
		return context;
	}

	@Override
	public int getItemCount() {
		if (arrayList == null){
			return 0;
		}
		return arrayList.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public T getItem(int position) {

		if (arrayList == null){
			return null;
		}

		//return arrayList.get(position);

		//추가수정 (인덱스) 범위 이내만 동작처리
		if( (position > -1) && (arrayList.size() > position) ){
			return arrayList.get(position);
		}else{
			return null;
		}

	}

	public void removeItem(int position) {
		if(arrayList != null && !arrayList.isEmpty()) {

			//arrayList.remove(position);

			//추가수정 (인덱스) 범위 이내만 동작처리
			if((position > -1) && (arrayList.size() > position)){
				arrayList.remove(position);
			}


		}
	}

	public void removeItem(Object obj) {
		if(arrayList != null && !arrayList.isEmpty()) {
			arrayList.remove(obj);
		}
	}

	public ArrayList<T> getItemAll() {
		if (arrayList == null) {
			return null;
		}
		return arrayList;
	}

	public void updateItems(ArrayList<T> items) {
		if (items == null || items.isEmpty()) {
			this.arrayList = new ArrayList<>();
		}
		else {
			this.arrayList.clear();
			this.arrayList.addAll(items);
		}
		notifyDataSetChanged();
	}

	public void updateItemHeader() {
		this.arrayList.add(0, null);
		notifyDataSetChanged();
	}

	@Deprecated
	public void addItems(ArrayList<T> items) {
		if (this.arrayList == null) {
			this.arrayList = items;
		} else {
			this.arrayList.addAll(items);
		}
		notifyDataSetChanged();
	}

	public void clearItems() {
		if (arrayList != null) {
			arrayList.clear();
			notifyDataSetChanged();
		}
	}

	@Override
	public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
		onBindView((H) holder, position);
	}

	abstract public void onBindView(H holder, int position);

	@Override
	public void onItemMove(int fromPosition, int toPosition) {
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				Collections.swap(arrayList, i, i + 1);
			}
		} else {
			for (int i = fromPosition; i > toPosition; i--) {
				Collections.swap(arrayList, i, i - 1);
			}
		}
//		Collections.swap(arrayList, fromPosition, toPosition);
		notifyItemMoved(fromPosition, toPosition);
	}

	@Override
	public void onItemDismiss(int position) {
		arrayList.remove(position);
		notifyItemRemoved(position);
	}
}