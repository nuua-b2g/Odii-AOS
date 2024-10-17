package kto.smarttour.db.item;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class StoryItem implements Serializable {
	@SerializedName("audio_file_path")
	@Expose
	public String audioFilePath;

	@SerializedName("slid")
	@Expose
	public int slid;

	@SerializedName("audio_play_time")
	@Expose
	public String audioPlayTime;

	@SerializedName("addr_list")
	@Expose
	public String addrList;

	@SerializedName("distance")
	@Expose
	public String distance;

	@SerializedName("tlid")
	@Expose
	public int tlid;
	@SerializedName("iid")
	@Expose
	public int iid;
	@SerializedName("title")
	@Expose
	public String title;
	@SerializedName("title_ko")
	@Expose
	public String titleKo;
	@SerializedName("tid")
	@Expose
	public int tid;
	@SerializedName("sid")
	@Expose
	public int sid;
	@SerializedName("play_time")
	@Expose
	public String playTime;
	@SerializedName("pos_y")
	@Expose
	public String posY;
	@SerializedName("pos_x")
	@Expose
	public String posX;
	@SerializedName("audio_version")
	@Expose
	public int audioVersion;
	@SerializedName("version")
	@Expose
	public int version;
	@SerializedName("tag")
	@Expose
	public String tag;
	@SerializedName("audio_file_size")
	@Expose
	public int audioFileSize;
	@SerializedName("audio_script")
	@Expose
	public String audioScript;
	@SerializedName("audio_title")
	@Expose
	public String audioTitle;
	@SerializedName("thumbnail_file_path")
	@Expose
	public String thumbnailFilePath;
	@SerializedName("thumbnail_update_date_sec")
	@Expose
	public int thumbnailUpdateDateSec;
	@SerializedName("iflid")
	@Expose
	public int iflid;
	@SerializedName("aid")
	@Expose
	public int aid;
	@SerializedName("lang_code")
	@Expose
	public String langCode;
	@SerializedName("radius")
	@Expose
	public int radius;

	public String play_type;
	public int seq;
	public int pindex;
	public boolean isSelected = false;
	public String date;
	public int _id;
	public double currentDistance = 0;

	//추가
	public int taxiplay_index;//텍시 플레이어 인덱스 참조용

	@Override
	public String toString() {
		return "StoryItem{" + "audioFilePath='" + audioFilePath + '\'' + ", slid=" + slid + ", audioPlayTime='" + audioPlayTime + '\'' + ", addrList='" + addrList + '\'' + ", distance='" + distance + '\'' + ", tlid=" + tlid + ", iid=" + iid + ", title='" + title + '\'' + ", tid=" + tid + ", sid=" + sid + ", playTime='" + playTime + '\'' + ", posY='" + posY + '\'' + ", posX='" + posX + '\'' + ", audioVersion=" + audioVersion + ", tag='" + tag + '\'' + ", audioFileSize=" + audioFileSize + ", audioScript='" + audioScript + '\'' + ", audioTitle='" + audioTitle + '\'' + ", thumbnailFilePath='" + thumbnailFilePath + '\'' + ", iflid=" + iflid + ", aid=" + aid + '}';
	}


	public String toString2() {
		return "StoryItem{" + "slid=" + slid + ", tlid=" + tlid + ", title='" + title + '\'' + ", titleKo='" + titleKo + '\'' + ", seq=" + seq + ", pindex=" + pindex + ", _id=" + _id + '}';
	}

	public String toString3() {
		return "StoryItem{" + ", seq=" + seq + ", currentDistance=" + currentDistance + ", title="+ title +'}';
	}

	public StoryItem deepCopy() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);
		out.writeObject(this);
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bis);
		StoryItem copied = (StoryItem) in.readObject();
		return copied;
	}

	public String getParams() {
		return "?slid=" + slid + "&tlid=" + tlid + "&iid=" + iid + "&title=" + title + "&audioTitle=" + audioTitle + "&iflid=" + iflid + "&aid=" + aid;
	}

	public boolean isEquals(StoryItem obj) {
		return this.slid == obj.slid && this.tlid == obj.tlid && this.sid == obj.sid && this.tid == obj.tid;
	}
}
