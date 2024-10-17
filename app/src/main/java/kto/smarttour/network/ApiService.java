package kto.smarttour.network;

import java.util.Map;

import kto.smarttour.common.consts.URLS;
import kto.smarttour.network.response.IntroImageData;
import kto.smarttour.network.response.LogData;
import kto.smarttour.network.response.NoticeData;
import kto.smarttour.network.response.TaxiData;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

public class ApiService extends APIAdapter {

	public static ApiInterface get() {
		return (ApiInterface) retrofit(ApiInterface.class);
	}

	/*
	public static ApiInterface get(String baseUrl) {
		return (ApiInterface) retrofit(baseUrl, ApiInterface.class);
	}
	*/

	public interface ApiInterface {
		@Headers("Content-Type: application/x-www-form-urlencoded")
		@GET(URLS.API_NOTICE)
		Call<NoticeData> notice(@QueryMap Map<String, String> map);

		@FormUrlEncoded
		@POST(URLS.ADDFOOTSTAMP)
		Call<ResponseBody> stamp(@FieldMap Map<String, Object> map);

		@Headers("Content-Type: application/x-www-form-urlencoded")
		@GET(URLS.TOUR_TAXI)
		Call<TaxiData> tourTaxi(@QueryMap Map<String, String> map);

		@FormUrlEncoded
		@POST(URLS.TOUR_TAXI_ANALYTICS)
		Call<ResponseBody> tourTaxiAnalytics(@FieldMap Map<String, String> map);

		@FormUrlEncoded
		@POST(URLS.ANALYTICS_PLAY)
		Call<ResponseBody> analyticsPlay(@FieldMap Map<String, Object> map);

		@FormUrlEncoded
		@POST(URLS.ANALYTICS_FINISH)
		Call<ResponseBody> analyticsFinish(@FieldMap Map<String, Object> map);

		//인트로 이미지 IntroImageData
		@Headers("Content-Type: application/x-www-form-urlencoded")
		@GET(URLS.API_INTRO_IMAGES)
		Call<IntroImageData> introImages(@QueryMap Map<String, String> map);

		//click_log_url , FCM - 노티 클릭 횟수 카운팅용 전송
		//다이나믹URL 이용하기..baseURL대신 직접 전달 @Url
		//@GET @POST ,@QueryMap @FieldMap
		@POST
		Call<ResponseBody> sendFeedbackFcm_click_log_url(@Url String url, @QueryMap Map<String, String> map);

		@POST
		Call<ResponseBody> sendFeedbackFcm_click_log_url(@Url String url);
	}
}
