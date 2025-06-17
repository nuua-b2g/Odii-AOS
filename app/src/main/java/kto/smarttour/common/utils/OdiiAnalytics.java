package kto.smarttour.common.utils;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.text.TextUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.network.ApiService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OdiiAnalytics {

	public static void storyPlayStart(Context context, int tlid, int slid) {
		String langCode = context.getString(R.string.language);
		String country = CommonUtils.getCountry(context);
		//DB 오디오리소스 lang_code와도 같은값으로 치환중..
		if ("zh".equals(langCode)) {
			langCode = "cn1";
		} else if("tw".equals(langCode)) {
			langCode = "cn2";
		} else if("ja".equals(langCode)) {
			langCode = "jp";
		}

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("tlid", tlid);
		map.put("slid", slid);
		map.put("os", "android");
		map.put("lang_code", langCode);
		map.put("uuid", CCLUUIDHelper.id(context));
		if (OdiiApplication.getLocation() != null) {
			Location location = OdiiApplication.getLocation();
			map.put("latitude", String.valueOf(location.getLatitude()));
			map.put("longitude", String.valueOf(location.getLongitude()));
		}
		if (!TextUtils.isEmpty(country)) {
			map.put("country", country);
		}

		ApiService.get().analyticsPlay(map).enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

			}

			@Override
			public void onFailure(Call<ResponseBody> call, Throwable t) {

			}
		});
	}

	public static void storyPlayEnd(Context context, int tlid, int slid) {
		String langCode = context.getString(R.string.language);
		String country = CommonUtils.getCountry(context);
		if ("zh".equals(langCode)) {
			langCode = "cn1";
		} else if("tw".equals(langCode)) {
			langCode = "cn2";
		} else if("ja".equals(langCode)) {
			langCode = "jp";
		}

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("tlid", tlid);
		map.put("slid", slid);
		map.put("os", "android");
		map.put("lang_code", langCode);
		map.put("uuid", CCLUUIDHelper.id(context));
		if (OdiiApplication.getLocation() != null) {
			Location location = OdiiApplication.getLocation();
			map.put("latitude", String.valueOf(location.getLatitude()));
			map.put("longitude", String.valueOf(location.getLongitude()));
		}
		if (!TextUtils.isEmpty(country)) {
			map.put("country", country);
		}

		//오디오 재생 종료시 통계 전송
		ApiService.get().analyticsFinish(map).enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

			}

			@Override
			public void onFailure(Call<ResponseBody> call, Throwable t) {

			}
		});
	}

}
