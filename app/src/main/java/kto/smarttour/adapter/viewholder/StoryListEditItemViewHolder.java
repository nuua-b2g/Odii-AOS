package kto.smarttour.adapter.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import kto.smarttour.databinding.StoryEditItemBinding;

public class StoryListEditItemViewHolder extends RecyclerView.ViewHolder {

	public StoryEditItemBinding binding;

	public StoryListEditItemViewHolder(@NonNull View itemView) {
		super(itemView);
		binding = DataBindingUtil.bind(itemView);
	}
}

