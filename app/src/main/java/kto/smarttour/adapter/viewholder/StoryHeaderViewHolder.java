package kto.smarttour.adapter.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import kto.smarttour.databinding.StoryFolderDetailHeaderBinding;

/**
 * 보관함 상세리스트 헤더 ViewHolder
 */
public class StoryHeaderViewHolder extends RecyclerView.ViewHolder {

	public StoryFolderDetailHeaderBinding binding;

	public StoryHeaderViewHolder(@NonNull View itemView) {
		super(itemView);

		binding = DataBindingUtil.bind(itemView);
	}
}
