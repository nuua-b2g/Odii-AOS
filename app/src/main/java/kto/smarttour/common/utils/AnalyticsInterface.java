package kto.smarttour.common.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class AnalyticsInterface {
	private static AnalyticsInterface instance;

	private AnalyticsInterface() {
	}

	private FirebaseAnalytics analytics;

	public static synchronized AnalyticsInterface getInstance() {
		if (instance == null) instance = new AnalyticsInterface();
		return instance;
	}

	public void init(Context context) {
		analytics = FirebaseAnalytics.getInstance(context);
		analytics.setAnalyticsCollectionEnabled(true);
	}

	public void screenView(Activity activity, String screen) {
		StringBuilder sb = new StringBuilder();

		switch (SettingsUtil.getLocale(activity)) {
			case "ko":
				sb.append("k_");
				break;
			case "zh":
				sb.append("z_");
				break;
			case "tw":
				sb.append("t_");
				break;
			case "ja":
				sb.append("j_");
				break;
			default:
				sb.append("e_");
				break;
		}
		sb.append(screen);
		analytics.setCurrentScreen(activity, sb.toString(), null);
	}



	public void logEvent(Context activity, String param) {

		StringBuilder sb = new StringBuilder("c_");

		switch (SettingsUtil.getLocale(activity)) {
			case "ko":
				sb.append("k_");
				break;
			case "zh":
				sb.append("z_");
				break;
			case "tw":
				sb.append("t_");
				break;
			case "ja":
				sb.append("j_");
				break;
			default:
				sb.append("e_");
				break;
		}
		sb.append(param);

		Bundle bundleAnalytics = new Bundle();
		analytics.logEvent(sb.toString(), bundleAnalytics);
	}
}
