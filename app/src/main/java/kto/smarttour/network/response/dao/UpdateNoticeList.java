package kto.smarttour.network.response.dao;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UpdateNoticeList {

	@SerializedName("end_date")
	@Expose
	private String endDate;
	@SerializedName("googleplay_ver")
	@Expose
	private String googleplayVer;
	@SerializedName("lang_code")
	@Expose
	private String langCode;
	@SerializedName("nid")
	@Expose
	private Integer nid;
	@SerializedName("appstore_ver")
	@Expose
	private String appstoreVer;
	@SerializedName("title")
	@Expose
	private String title;
	@SerializedName("appstore_url")
	@Expose
	private String appstoreUrl;
	@SerializedName("content")
	@Expose
	private String content;
	@SerializedName("start_date")
	@Expose
	private String startDate;
	@SerializedName("force_yn")
	@Expose
	private String forceYn;
	@SerializedName("googleplay_url")
	@Expose
	private String googleplayUrl;
	@SerializedName("img_alt")
	@Expose
	private String imgAlt = "";

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getGoogleplayVer() {
		return googleplayVer;
	}

	public void setGoogleplayVer(String googleplayVer) {
		this.googleplayVer = googleplayVer;
	}

	public String getLangCode() {
		return langCode;
	}

	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}

	public Integer getNid() {
		return nid;
	}

	public void setNid(Integer nid) {
		this.nid = nid;
	}

	public String getAppstoreVer() {
		return appstoreVer;
	}

	public void setAppstoreVer(String appstoreVer) {
		this.appstoreVer = appstoreVer;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAppstoreUrl() {
		return appstoreUrl;
	}

	public void setAppstoreUrl(String appstoreUrl) {
		this.appstoreUrl = appstoreUrl;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getForceYn() {
		return forceYn;
	}

	public void setForceYn(String forceYn) {
		this.forceYn = forceYn;
	}

	public String getGoogleplayUrl() {
		return googleplayUrl;
	}

	public void setGoogleplayUrl(String googleplayUrl) {
		this.googleplayUrl = googleplayUrl;
	}

	public String getImgAlt() {
		return imgAlt;
	}

	public void setImgAlt(String imgAlt) {
		this.imgAlt = imgAlt;
	}
}