package kto.smarttour.adapter.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import kto.smarttour.databinding.DriverStoryItemBinding;
import kto.smarttour.databinding.PlayListItemBinding;

public class DriverStoryListItemViewHolder extends RecyclerView.ViewHolder {

	public DriverStoryItemBinding binding;

	public DriverStoryListItemViewHolder(@NonNull View itemView) {
		super(itemView);
		binding = DataBindingUtil.bind(itemView);
	}
}

