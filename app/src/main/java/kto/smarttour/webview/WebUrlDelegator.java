package kto.smarttour.webview;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import kto.smarttour.R;
import kto.smarttour.common.consts.URLS;

/**
 * http와 https외의 URL에 대한 분기 처리.
 *
 * @author Changhyun Jeon
 * @since 2015. 3. 18
 */
public class WebUrlDelegator {

	/**
	 * The context.
	 */
	private Context context;

	/**
	 * Instantiates a new web url delegator.
	 *
	 * @param context the context
	 */
	public WebUrlDelegator(Context context) {
		// TODO Auto-generated constructor stub
		this.context = context;
	}

	/**
	 * Delegate url.
	 *
	 * @param url the url
	 * @return true, if successful
	 */
	public boolean delegateUrl(String url) {
		//return !url.toLowerCase().startsWith(URLS.BASE_URL) && !url.toLowerCase().startsWith("https://m.facebook.com") && !url.toLowerCase().startsWith("http://map.naver.com") && delegateUriScheme(url);
		boolean b_foundBaseHost = false;
		if(url!=null){
			try {
				URL baseURL = new URL(URLS.BASE_URL);
				String baseHost = baseURL.getHost();

				URL targetURL = new URL(url);
				String targetHost = targetURL.getHost();

				/*
				!url.toLowerCase().startsWith(URLS.BASE_URL)
				https , http를 구분못하는 기존 BASE_URL체크 변경
				*/

				//https , http관계없이 호스트명이 같으면 허용
				if(baseHost.equalsIgnoreCase(targetHost) && url.startsWith("http")){
					b_foundBaseHost = true;
				}

			} catch (MalformedURLException e) {
				//e.printStackTrace();
			}
		}
		//각 조건에 포함되지 않는 주소는 내부웹 로드
		return !b_foundBaseHost && !url.toLowerCase().startsWith("https://m.facebook.com") && !url.toLowerCase().startsWith("https://map.naver.com") && delegateUriScheme(url);
	}

	/**
	 * Delegate uri scheme.
	 *
	 * @param url the url
	 * @return true, if successful
	 */
	private boolean delegateUriScheme(String url) {
		String packageName = null;
		try {
			Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			packageName = intent.getPackage();
			context.startActivity(intent);
			return true;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ActivityNotFoundException e) {
			// TODO: handle exception
			if (packageName != null) {
				try {
					Intent marketLaunch = new Intent(Intent.ACTION_VIEW);
					marketLaunch.setData(Uri.parse("market://details?id=" + packageName));
					context.startActivity(marketLaunch);
				} catch (ActivityNotFoundException e1) {
					e1.printStackTrace();
				}
				return true;
			} else if (url.startsWith("kakaolink://")) {
				try {
					Intent marketLaunch = new Intent(Intent.ACTION_VIEW);
					marketLaunch.setData(Uri.parse("market://details?id=com.kakao.talk"));
					context.startActivity(marketLaunch);
					return true;
				} catch (ActivityNotFoundException e1) {
					e1.printStackTrace();
				}
			} else if (url.startsWith("sms://")) {
				Toast.makeText(context, R.string.not_available_sms, Toast.LENGTH_SHORT).show();
				return true;
			} else {
				return true;
			}
		}

		return false;
	}
}
