package kto.smarttour.network.response.dao;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable; //putExtra 전달목적 implements Serializable 추가
import java.util.ArrayList;

import kto.smarttour.db.item.StoryItem;

//putExtra 전달목적 implements Serializable 추가
public class TourTaxiInfo implements Serializable {

	@SerializedName("delete_yn")
	@Expose
	public String deleteYn;
	@SerializedName("update_date_sec")
	@Expose
	public long updateDateSec;
	@SerializedName("title")
	@Expose
	public String title;
	@SerializedName("create_date")
	@Expose
	public String createDate;
	@SerializedName("thumbnail_file_path")
	@Expose
	public String thumbnailFilePath;

	@SerializedName("storyList")
	@Expose(serialize = true, deserialize = false)
	public ArrayList<StoryItem> storyList = new ArrayList<>();

	@SerializedName("code_addr1")
	@Expose
	public String codeAddr1;

	@Expose
	private ArrayList<StoryItem> ko;

	@Expose
	private ArrayList<StoryItem> en;

	@Expose
	private ArrayList<StoryItem> cn;

	@Expose
	private ArrayList<StoryItem> jp;

	public ArrayList<StoryItem> getKo() {
		if (ko == null) {
			ko = new ArrayList<>();
		}
		return ko;
	}

	public void setKo(ArrayList<StoryItem> ko) {
		this.ko = ko;
	}

	public ArrayList<StoryItem> getEn() {
		if (en == null) {
			en = new ArrayList<>();
		}
		return en;
	}

	public void setEn(ArrayList<StoryItem> en) {
		this.en = en;
	}

	public ArrayList<StoryItem> getCn() {
		if (cn == null) {
			cn = new ArrayList<>();
		}
		return cn;
	}

	public void setCn(ArrayList<StoryItem> cn) {
		this.cn = cn;
	}

	public ArrayList<StoryItem> getJp() {
		if (jp == null) {
			jp = new ArrayList<>();
		}
		return jp;
	}

	public void setJp(ArrayList<StoryItem> jp) {
		this.jp = jp;
	}
}
