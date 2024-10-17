package kto.smarttour.ui.player;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryItem;

public class PlayListManager {

	private Context context;

	private PlayListManager() {
	}

	private static class LazyHolder {
		public static final PlayListManager INSTANCE = new PlayListManager();
	}

	public static PlayListManager getInstance() {
		return LazyHolder.INSTANCE;
	}

	public void init(Context context) {
		this.context = context;
	}

	public void load() {
		source.clear();
		source.addAll(StoryDbManager.getInstance(context).getPlayList().story);
	}

	private int playIndex = 0;

	public List<StoryItem> source = new ArrayList<>();

	public void addAll(List<StoryItem> storyItems) {
		source.addAll(storyItems);
	}

	public void clearAdd(List<StoryItem> storyItems) {
		source.clear();
		addAll(storyItems);
	}

	public void add(StoryItem item) {
		source.add(item);
	}

	public StoryItem getStoryItem() {
		if (!source.isEmpty() && source.size() <= playIndex) {
			return null;
		}

		if (playIndex == -1) {
			return null;
		}

		if (playIndex < source.size()) {
			return source.get(playIndex);
		}

		return null;
	}

	public StoryItem getStoryItem(int position) {

		if (!source.isEmpty() && source.size() <= position) {
			return null;
		}
		return source.get(position);
	}

	public int getStoryIndex(int seq) {
		if(source != null && !source.isEmpty()) {
			for(int i = 0; i <= source.size(); i++) {
				if(source.get(i).seq == seq) {
					return i;
				}
			}
		}
		return -1;
	}

	public int getCurrentStorySeq() {
		return source.get(playIndex).seq;
	}

	public void remove(int position) {
		source.remove(position);
	}

	public void remove(StoryItem item) {
		source.remove(item);
	}

	public int getPlayIndex() {
		return playIndex;
	}

	public int setPlayIndex(int playIndexSet) {
		this.playIndex = playIndexSet;
		return this.playIndex;
	}

	public int getListSize() {
		if (source == null) {
			return 0;
		}
		return source.size();
	}

	public StoryItem next() {
		playIndex += 1;
		if (playIndex >= source.size()) {
			return null;
		}
		return source.get(playIndex);
	}

	public StoryItem prev() {
		playIndex -= 1;
		if (playIndex <= 0) {
			return null;
		}
		return source.get(playIndex);
	}

	public void clear() {
		if(source != null) {
			source.clear();
		}
	}
}
