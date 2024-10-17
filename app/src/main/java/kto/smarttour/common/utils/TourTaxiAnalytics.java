package kto.smarttour.common.utils;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import kto.smarttour.network.ApiService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TourTaxiAnalytics {

	public static void init(Context context) {
		Map<String, String> map = new HashMap<>();
		map.put("ttid", String.valueOf(SettingsUtil.getTaxiTtid(context)));
		map.put("uuid", CCLUUIDHelper.id(context));
		map.put("osType", "android");
		map.put("statsType", "init");

		ApiService.get().tourTaxiAnalytics(map).enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

			}

			@Override
			public void onFailure(Call<ResponseBody> call, Throwable t) {

			}
		});
	}

	public static void download(Context context, String langCode, int storyCnt) {
		Map<String, String> map = new HashMap<>();
		map.put("ttid", String.valueOf(SettingsUtil.getTaxiTtid(context)));
		map.put("uuid", CCLUUIDHelper.id(context));
		map.put("osType", "android");
		map.put("langCode", langCode);
		map.put("statsType", "download");
		map.put("storyCnt", String.valueOf(storyCnt));

		ApiService.get().tourTaxiAnalytics(map).enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

			}

			@Override
			public void onFailure(Call<ResponseBody> call, Throwable t) {

			}
		});
	}

	public static void update(Context context, String langCode, int storyCnt) {
		Map<String, String> map = new HashMap<>();
		map.put("ttid", String.valueOf(SettingsUtil.getTaxiTtid(context)));
		map.put("uuid", CCLUUIDHelper.id(context));
		map.put("osType", "android");
		map.put("langCode", langCode);
		map.put("statsType", "update");
		map.put("storyCnt", String.valueOf(storyCnt));

		ApiService.get().tourTaxiAnalytics(map).enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

			}

			@Override
			public void onFailure(Call<ResponseBody> call, Throwable t) {

			}
		});
	}

	public static void storyPlayStart(Context context, String slid, String langCode) {
		Map<String, String> map = new HashMap<>();
		map.put("ttid", String.valueOf(SettingsUtil.getTaxiTtid(context)));
		map.put("slid", slid);
		map.put("uuid", CCLUUIDHelper.id(context));
		map.put("osType", "android");
		map.put("langCode", langCode);
		map.put("statsType", "storyPlayStart");

		ApiService.get().tourTaxiAnalytics(map).enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

			}

			@Override
			public void onFailure(Call<ResponseBody> call, Throwable t) {

			}
		});
	}

	public static void storyPlayEnd(Context context, String slid, String langCode) {
		Map<String, String> map = new HashMap<>();
		map.put("ttid", String.valueOf(SettingsUtil.getTaxiTtid(context)));
		map.put("slid", slid);
		map.put("uuid", CCLUUIDHelper.id(context));
		map.put("osType", "android");
		map.put("langCode", langCode);
		map.put("statsType", "storyPlayEnd");

		ApiService.get().tourTaxiAnalytics(map).enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

			}

			@Override
			public void onFailure(Call<ResponseBody> call, Throwable t) {

			}
		});
	}

}
