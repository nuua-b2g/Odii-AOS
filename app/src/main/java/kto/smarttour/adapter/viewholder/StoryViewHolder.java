package kto.smarttour.adapter.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import kto.smarttour.databinding.StoryItemBinding;

public class StoryViewHolder extends RecyclerView.ViewHolder {

	public StoryItemBinding binding;

	public StoryViewHolder(@NonNull View itemView) {
		super(itemView);
		binding = DataBindingUtil.bind(itemView);
	}
}
