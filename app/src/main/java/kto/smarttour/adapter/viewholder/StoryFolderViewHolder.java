package kto.smarttour.adapter.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import kto.smarttour.databinding.StoryFolderItemBinding;

/**
 *	보관함 폴더 ViewHolder
 */
public class StoryFolderViewHolder extends RecyclerView.ViewHolder {

	public StoryFolderItemBinding binding;

	public StoryFolderViewHolder(@NonNull View itemView) {
		super(itemView);
		binding = DataBindingUtil.bind(itemView);
	}
}
