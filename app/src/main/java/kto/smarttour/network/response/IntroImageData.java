package kto.smarttour.network.response;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class IntroImageData implements Serializable, Parcelable {

	@SerializedName("image")
	//@Exposeßß
	private String image;

	public IntroImageData() {}

	public IntroImageData(String image) {
		this.image = image;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	@Override
	public String toString() {
		return "IntroImageData{" +
				"image='" + image + '\'' +
				'}';
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.image);
	}

	protected IntroImageData(Parcel source) {
		this.image = source.readString();
	}

	public static final Parcelable.Creator<IntroImageData> CREATOR = new Parcelable.Creator<IntroImageData>() {
		@Override
		public IntroImageData createFromParcel(Parcel source) {
			return new IntroImageData(source);
		}

		@Override
		public IntroImageData[] newArray(int size) {
			return new IntroImageData[size];
		}
	};

}
