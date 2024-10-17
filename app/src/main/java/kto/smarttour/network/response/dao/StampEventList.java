package kto.smarttour.network.response.dao;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class StampEventList implements Parcelable {

	public int _id;
	@SerializedName("end_date")
	@Expose
	public long end_date;
	@SerializedName("eid")
	@Expose
	public int eid;
	@SerializedName("slid")
	@Expose
	public int slid;
	@SerializedName("tlid")
	@Expose
	public int tlid;
	@SerializedName("stamp_p_yn")
	@Expose
	public String stamp_p_yn;
	@SerializedName("elid")
	@Expose
	public int elid;
	@SerializedName("tid")
	@Expose
	public int tid;
	@SerializedName("pos_y")
	@Expose
	public String posY;
	@SerializedName("pos_x")
	@Expose
	public String posX;
	@SerializedName("stamp_v_yn")
	@Expose
	public String stamp_v_yn;
	@SerializedName("end_date_millis")
	@Expose
	public long endDateMillis;
	@SerializedName("link_url")
	@Expose
	public String linkUrl;
	@SerializedName("radius")
	@Expose
	public int radius;
	@SerializedName("start_date_millis")
	@Expose
	public long startDateMillis;
	@SerializedName("start_date")
	@Expose
	public long startDate;


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(this.end_date);
		dest.writeInt(this.eid);
		dest.writeInt(this.slid);
		dest.writeInt(this.tlid);
		dest.writeString(this.stamp_p_yn);
		dest.writeInt(this.elid);
		dest.writeInt(this.tid);
		dest.writeString(this.posY);
		dest.writeString(this.posX);
		dest.writeLong(this.endDateMillis);
		dest.writeString(this.linkUrl);
		dest.writeInt(this.radius);
		dest.writeLong(this.startDateMillis);
		dest.writeLong(this.startDate);
	}

	public StampEventList() {
	}

	protected StampEventList(Parcel in) {
		this.end_date = in.readLong();
		this.eid = in.readInt();
		this.slid = in.readInt();
		this.tlid = in.readInt();
		this.stamp_p_yn = in.readString();
		this.elid = in.readInt();
		this.tid = in.readInt();
		this.posY = in.readString();
		this.posX = in.readString();
		this.endDateMillis = in.readLong();
		this.linkUrl = in.readString();
		this.radius = in.readInt();
		this.startDateMillis = in.readLong();
		this.startDate = in.readLong();
	}

	public static final Parcelable.Creator<StampEventList> CREATOR = new Parcelable.Creator<StampEventList>() {
		@Override
		public StampEventList createFromParcel(Parcel source) {
			return new StampEventList(source);
		}

		@Override
		public StampEventList[] newArray(int size) {
			return new StampEventList[size];
		}
	};

	@Override
	public String toString() {
		return "StampEventList{" + "_id=" + _id + ", end_date=" + end_date + ", eid=" + eid + ", slid=" + slid + ", tlid=" + tlid + ", stamp_p_yn='" + stamp_p_yn + '\'' + ", elid=" + elid + ", tid=" + tid + ", posY='" + posY + '\'' + ", posX='" + posX + '\'' + ", stamp_v_yn='" + stamp_v_yn + '\'' + ", endDateMillis=" + endDateMillis + ", linkUrl='" + linkUrl + '\'' + ", radius=" + radius + ", startDateMillis=" + startDateMillis + ", startDate=" + startDate + '}';
	}
}