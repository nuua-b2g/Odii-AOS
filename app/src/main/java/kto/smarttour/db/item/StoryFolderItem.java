package kto.smarttour.db.item;

import java.io.Serializable;

public class StoryFolderItem implements Serializable {
	public String seq;
	public String title;
	public String imgUrl;
	public String fileData;
	public int totalCount = 0;
	public boolean isSelected = false;
}
