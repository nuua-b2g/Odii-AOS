package kto.smarttour.network.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LogData {
	@SerializedName("slid")
	@Expose
	private String slid;
	@SerializedName("tlid")
	@Expose
	private String tlid;
	@SerializedName("iid")
	@Expose
	private String iid;
	@SerializedName("title")
	@Expose
	private String title;
	@SerializedName("audioTitle")
	@Expose
	private String audioTitle;
	@SerializedName("iflid")
	@Expose
	private String iflid;
	@SerializedName("aid")
	@Expose
	private String aid;

	public String getSlid() {
		return slid;
	}

	public void setSlid(String slid) {
		this.slid = slid;
	}

	public String getTlid() {
		return tlid;
	}

	public void setTlid(String tlid) {
		this.tlid = tlid;
	}

	public String getIid() {
		return iid;
	}

	public void setIid(String iid) {
		this.iid = iid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAudioTitle() {
		return audioTitle;
	}

	public void setAudioTitle(String audioTitle) {
		this.audioTitle = audioTitle;
	}

	public String getIflid() {
		return iflid;
	}

	public void setIflid(String iflid) {
		this.iflid = iflid;
	}

	public String getAid() {
		return aid;
	}

	public void setAid(String aid) {
		this.aid = aid;
	}

}