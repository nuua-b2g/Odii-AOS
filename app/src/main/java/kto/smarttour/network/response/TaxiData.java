package kto.smarttour.network.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import kto.smarttour.network.response.dao.TourTaxiInfo;

public class TaxiData {

	/*
	@SerializedName("result")
	@Expose
	public String result;
	@SerializedName("message")
	@Expose
	public String message;
	@SerializedName("title")
	@Expose
	public String title;
	@SerializedName("thumbnail_file_path")
	@Expose
	public String thumbnailFilePath;
	@SerializedName("ko")
	@Expose
	public List<StoryItem> ko;
	@SerializedName("en")
	@Expose
	public List<StoryItem> en;
	@SerializedName("jp")
	@Expose
	public List<StoryItem> jp;
	@SerializedName("cn")
	@Expose
	public List<StoryItem> cn;
	 */

	@SerializedName("tourTaxiInfo")
	@Expose
	public TourTaxiInfo tourTaxiInfo;

}

