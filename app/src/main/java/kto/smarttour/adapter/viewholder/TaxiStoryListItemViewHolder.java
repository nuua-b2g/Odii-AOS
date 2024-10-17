package kto.smarttour.adapter.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import kto.smarttour.databinding.TaxiStoryItemBinding;

public class TaxiStoryListItemViewHolder extends RecyclerView.ViewHolder {

	public TaxiStoryItemBinding binding;

	public TaxiStoryListItemViewHolder(@NonNull View itemView) {
		super(itemView);
		binding = DataBindingUtil.bind(itemView);
	}
}

