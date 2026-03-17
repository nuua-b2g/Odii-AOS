package kto.smarttour.common.consts;

import kto.smarttour.BuildConfig;

public class URLS {

    public static final String BASE_URL = BuildConfig.BASE_URL;

	public static final String URL = BASE_URL + "/smarttour_web/home/main?lang=%s";
	public static final String INFLOW_URL = BASE_URL + "/smarttour_web/inflow/log_new?ifwId=%s";
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
