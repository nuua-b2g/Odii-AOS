package kto.smarttour.network.response.dao;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EventList implements Parcelable {

	@SerializedName("end_date")
	@Expose
	private String endDate;
	@SerializedName("lang_code")
	@Expose
	private String langCode;
	@SerializedName("img_url")
	@Expose
	private String imgUrl;
	@SerializedName("nid")
	@Expose
	private Integer nid;
	@SerializedName("link_url")
	@Expose
	private String linkUrl;
	@SerializedName("nlid")
	@Expose
	private Integer nlid;
	@SerializedName("title")
	@Expose
	private String title;
	@SerializedName("start_date")
	@Expose
	private String startDate;
	@SerializedName("img_alt")
	@Expose
	private String imgAlt = "";

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getLangCode() {
		return langCode;
	}

	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}

	public String getImgUrl() {
		return imgUrl;
	}

	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	public Integer getNid() {
		return nid;
	}

	public void setNid(Integer nid) {
		this.nid = nid;
	}

	public String getLinkUrl() {
		return linkUrl;
	}

	public void setLinkUrl(String linkUrl) {
		this.linkUrl = linkUrl;
	}

	public Integer getNlid() {
		return nlid;
	}

	public void setNlid(Integer nlid) {
		this.nlid = nlid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getImgAlt() {
		return imgAlt;
	}

	public void setImgAlt(String imgAlt) {
		this.imgAlt = imgAlt;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.endDate);
		dest.writeString(this.langCode);
		dest.writeString(this.imgUrl);
		dest.writeValue(this.nid);
		dest.writeString(this.linkUrl);
		dest.writeValue(this.nlid);
		dest.writeString(this.title);
		dest.writeString(this.startDate);
		dest.writeString(this.imgAlt);
	}

	public EventList() {
	}

	protected EventList(Parcel in) {
		this.endDate = in.readString();
		this.langCode = in.readString();
		this.imgUrl = in.readString();
		this.nid = (Integer) in.readValue(Integer.class.getClassLoader());
		this.linkUrl = in.readString();
		this.nlid = (Integer) in.readValue(Integer.class.getClassLoader());
		this.title = in.readString();
		this.startDate = in.readString();
		this.imgAlt = in.readString();
	}

	public static final Parcelable.Creator<EventList> CREATOR = new Parcelable.Creator<EventList>() {
		@Override
		public EventList createFromParcel(Parcel source) {
			return new EventList(source);
		}

		@Override
		public EventList[] newArray(int size) {
			return new EventList[size];
		}
	};
}