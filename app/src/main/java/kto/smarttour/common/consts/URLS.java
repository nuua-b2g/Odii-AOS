package kto.smarttour.common.consts;

public class URLS {

	public static final String BASE_URL = "https://www.odii.kr";//운영서버 (내재화 ssl 2022.07월부터 앱에서 사용)
	//public static final String BASE_URL = "https://odii.witches.co.kr";//개발서버 (위치스 재구축)
//	public static final String BASE_URL = "http://121.131.208.44:18080"; // 엔티콘 개발 서버
//	public static final String BASE_URL = "http://dev.odii.ntcon.co:18080/";

	public static final String URL = BASE_URL + "/smarttour_web/home/main?lang=%s";
	public static final String SEARCH_URL = BASE_URL + "/smarttour_web/search/main";
	public static final String DETAIL_URL = BASE_URL + "/smarttour_web/story/detail";
	public static final String API_NOTICE = "/smarttour_web/home/ajax/noticeList";

	//추가 설정페이지 (2023.09.07)
	public static final String SETTING_URL = BASE_URL + "/smarttour_web/setting/main";

	public static final String API_INTRO_IMAGES = "/smarttour_web/app/api/intro";//인트로

	public static final String STORY_SHARE_URL = BASE_URL + "/smarttour_web/home/social?tlid=%s&slid=%s&lang=%s";

	public static final String ADDFOOTSTAMP = "/smarttour_web/event/ajax/addFootStamp";

	public static final String TOUR_TAXI = "/smarttour_web/tourTaxi/ajax/info";

	public static final String TOUR_TAXI_ANALYTICS = "/smarttour_web/tourTaxi/ajax/sendAnalytics";

	public static final String ANALYTICS_PLAY = "/smarttour_web/stat/add/audioplay/play";
	public static final String ANALYTICS_FINISH = "/smarttour_web/stat/add/audioplay/finish";

}
