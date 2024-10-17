package kto.smarttour.adapter.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import kto.smarttour.databinding.PlayListItemBinding;

public class StoryListItemViewHolder extends RecyclerView.ViewHolder {

	public PlayListItemBinding binding;

	public StoryListItemViewHolder(@NonNull View itemView) {
		super(itemView);
		binding = DataBindingUtil.bind(itemView);
	}
}

