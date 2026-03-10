package kto.smarttour.webview.clients;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.scottyab.rootbeer.RootBeer;
import com.socks.library.KLog;

//import kto.smarttour.BuildConfig;
import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.ui.MainActivity;
import kto.smarttour.webview.WebUrlDelegator;

/**
 * The Class OdiiWebViewClient.
 *
 * @author Changhyun Jeon
 * @since 2015. 3. 17
 */
public class OdiiWebViewClient extends WebViewClient {

	private Handler handler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(@NonNull Message msg) {

			if(msg.what == 35) {
				WebView view = (WebView)msg.obj;
				if(view != null) {
					view.requestFocus();
					view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
					view.loadUrl("javascript:focusHeader()");
				}
			}

			super.handleMessage(msg);
		}
	};

	/**
	 * @see WebUrlDelegator
	 */
	private WebUrlDelegator webUrlDelegator;

	/**
	 * 웹사이트 로딩시 진행바.
	 */
	private ProgressBar progressBar;

	public static final String ACTION_RECEIVED_ERROR = "kto.smarttour.webview.ACTION_RECEIVED_ERROR";

	public static final String EXTRA_URL = "url";

	private AppCompatActivity activity;
	private RootBeer rootBeer;

	/**
	 * Instantiates a new smart tour web view client.
	 *
	 * @param activity    the activity
	 * @param progressBar the progress bar
	 */
	public OdiiWebViewClient(AppCompatActivity activity, ProgressBar progressBar) {
		// TODO Auto-generated constructor stub
		this.webUrlDelegator = new WebUrlDelegator(activity);
		this.progressBar = progressBar;
		this.activity = activity;
		rootBeer = new RootBeer(activity);
	}

	/* (non-Javadoc)
	 * @see android.webkit.WebViewClient#shouldOverrideUrlLoading(android.webkit.WebView, java.lang.String)
	 */
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		if (!webUrlDelegator.delegateUrl(url)) {
			view.loadUrl(url);
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see android.webkit.WebViewClient#onPageStarted(android.webkit.WebView, java.lang.String, android.graphics.Bitmap)
	 */
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		// TODO Auto-generated method stub
		super.onPageStarted(view, url, favicon);
		if (progressBar != null) {
			progressBar.setVisibility(View.VISIBLE);
		}
	}

	/* (non-Javadoc)
	 * @see android.webkit.WebViewClient#onPageFinished(android.webkit.WebView, java.lang.String)
	 */
	@Override
	public void onPageFinished(WebView view, String url) {
		handler.sendMessageDelayed(handler.obtainMessage(35, view), 300);
		super.onPageFinished(view, url);
		((MainActivity) OdiiApplication.getWebActivity()).hideBlock();
        CookieManager.getInstance().flush();

		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}

		if(OdiiApplication.isGeofenceNotifcationClick && !TextUtils.isEmpty(OdiiApplication.stamp_url)) {
			view.loadUrl(OdiiApplication.stamp_url);
			OdiiApplication.isGeofenceNotifcationClick = false;
			OdiiApplication.stamp_url = null;
		}

		if (CommonUtils.isRooted(activity)) {
			DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.rooted_message), activity.getString(R.string.finish), "", () -> {
				ActivityCompat.finishAffinity(activity);
				System.exit(0);
			}, () -> {

			});
		} else if (CommonUtils.isEmulator()) {
			DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.emulator_message), activity.getString(R.string.finish), "", () -> {
				ActivityCompat.finishAffinity(activity);
				System.exit(0);
			}, () -> {

			});
		} else if (!CommonUtils.isKeyChecker(activity, "MD5")) {
			DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.Integrity_message), activity.getString(R.string.finish), "", () -> {
				ActivityCompat.finishAffinity(activity);
				System.exit(0);
			}, () -> {

			});
		}
	}

	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		view.post(() -> {
			((MainActivity) OdiiApplication.getWebActivity()).showBlock();
		});
		DialogUtil.showWarning(activity, activity.getString(R.string.confirm), activity.getString(R.string.network_error), activity.getString(R.string.retry), activity.getString(R.string.finish), () -> {
			view.reload();
		}, () -> {
			activity.finish();
		});
		super.onReceivedError(view, errorCode, description, failingUrl);
	}

	/* (non-Javadoc)
	 * @see android.webkit.WebViewClient#onFormResubmission(android.webkit.WebView, android.os.Message, android.os.Message)
	 */
	@Override
	public void onFormResubmission(WebView view, Message dontResend, Message resend) {
		//요청된 페이지가 POST DATA에 의한 결과 페이지 일 경우 Post Data를 다시 전송
		//Webview에서 history back시에 오류가 발생할 수 있어 추가
		resend.sendToTarget();
		super.onFormResubmission(view, dontResend, resend);
	}
}
