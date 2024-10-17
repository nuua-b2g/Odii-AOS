package kto.smarttour.network.response;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import kto.smarttour.network.response.dao.DisasterNoticeList;
import kto.smarttour.network.response.dao.EventList;
import kto.smarttour.network.response.dao.MaintainNoticeList;
import kto.smarttour.network.response.dao.NormalNoticeList;
import kto.smarttour.network.response.dao.StampEventList;
import kto.smarttour.network.response.dao.UpdateNoticeList;

public class NoticeData {

	@SerializedName("normal_notice_list")
	@Expose
	private List<NormalNoticeList> normalNoticeList = new ArrayList<>();
	@SerializedName("disaster_notice_list")
	@Expose
	private List<DisasterNoticeList> disasterNoticeList = new ArrayList<>();
	@SerializedName("maintain_notice_list")
	@Expose
	private List<MaintainNoticeList> maintainNoticeList = new ArrayList<>();
	@SerializedName("update_notice_list")
	@Expose
	private List<UpdateNoticeList> updateNoticeList = new ArrayList<>();
	@SerializedName("event_notice_list")
	@Expose
	private ArrayList<EventList> eventList = new ArrayList<>();
	@SerializedName("stampEventList")
	@Expose
	private ArrayList<StampEventList> stampEventList = new ArrayList<>();


	public List<NormalNoticeList> getNormalNoticeList() {
		return normalNoticeList;
	}

	public void setNormalNoticeList(List<NormalNoticeList> normalNoticeList) {
		this.normalNoticeList = normalNoticeList;
	}

	public List<DisasterNoticeList> getDisasterNoticeList() {
		return disasterNoticeList;
	}

	public void setDisasterNoticeList(List<DisasterNoticeList> disasterNoticeList) {
		this.disasterNoticeList = disasterNoticeList;
	}

	public List<MaintainNoticeList> getMaintainNoticeList() {
		return maintainNoticeList;
	}

	public void setMaintainNoticeList(List<MaintainNoticeList> maintainNoticeList) {
		this.maintainNoticeList = maintainNoticeList;
	}

	public List<UpdateNoticeList> getUpdateNoticeList() {
		return updateNoticeList;
	}

	public void setUpdateNoticeList(List<UpdateNoticeList> updateNoticeList) {
		this.updateNoticeList = updateNoticeList;
	}

	public ArrayList<EventList> getEventList() {
		return eventList;
	}

	public void setEventList(ArrayList<EventList> eventList) {
		this.eventList = eventList;
	}

	public ArrayList<StampEventList> getStampEventList() {
		return stampEventList;
	}

	public void setStampEventList(ArrayList<StampEventList> stampEventList) {
		this.stampEventList = stampEventList;
	}
}