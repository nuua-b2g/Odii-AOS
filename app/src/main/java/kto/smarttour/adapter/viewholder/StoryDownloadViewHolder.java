package kto.smarttour.adapter.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import kto.smarttour.databinding.StoryDownloadItemBinding;

/**
 * 다운로드 이야기 ViewHolder
 */
public class StoryDownloadViewHolder extends RecyclerView.ViewHolder {

	public StoryDownloadItemBinding binding;

	public StoryDownloadViewHolder(@NonNull View itemView) {
		super(itemView);
		binding = DataBindingUtil.bind(itemView);
	}
}
