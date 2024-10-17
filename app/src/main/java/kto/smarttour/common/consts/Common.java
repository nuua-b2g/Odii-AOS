package kto.smarttour.common.consts;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;

import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryItem;

public class Common {

	public static final int ACTIVITY_TAXI_OTHERS = 10000;
	public static final int ACTIVITY_TAXI_MAIN = 10001;
	public static final int ACTIVITY_TAXI_GUIDE = 10002;
	public static final int ACTIVITY_TAXI_STORY = 10003;
	public static final int ACTIVITY_TAXI_PLAYER = 10004;

	public static int gCurrentTaxtActivity = ACTIVITY_TAXI_OTHERS;

	public static boolean isAudioPlaying = false;
	public static boolean isTaxiDriving = false;

	/**
	 * 재생목록 오디오플레이 언어한정 - 현재 언어에 한함 true
	 * DB 기록된 재생목록, 재생목록 수 쿼리에 영향
	 */
	public static boolean isPlayListOnlyForCurrentLanguage = true;

	/**
	 * 루팅 확인 무시 여부: 배포 시 false로 변경할 것
	 */
	public static boolean ignoreRooting = false;

	/**
	 * 요청사항 : 재생목록 화면에서 상세화면으로 이동 시 다시 돌아오는 경우 재생목록이 보이도록 처리
	 */
	public static boolean isDetailActivityShowing = false;

	public static boolean isPushAvailable() {
		if (isAudioPlaying) {
			return false;
		}
		if (isTaxiDriving) {
			return false;
		}
		return true;
	}

	public static void showNotification(Context context, Location location) {
		//Log.d("TAG", ">>> Activity : " + gCurrentTaxtActivity);
		//Log.d("TAG", ">>> Current : " + location.toString());

		/*

		//	관광지 data
		StoryData storyData = StoryDbManager.getInstance(context).getPlayList();
		if (storyData != null) {
			ArrayList<StoryItem> arrayList = storyData.story;
			for (StoryItem item : arrayList) {
				Log.d("TAG", ">>> item : " + item.toString());
			}
		}

		*/
	}

}
