package kto.smarttour.webview.clients;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import kto.smarttour.ui.MainActivity;

/**
 * The Class OdiiWebChromeClient.
 *
 * @author Changhyun Jeon
 * @since 2015. 3. 17
 */
public class OdiiWebChromeClient extends WebChromeClient {

	private final AppCompatActivity activity;
	public ValueCallback<Uri[]> uploadMessage;

	/**
	 * 웹사이트 로딩시 진행바.
	 */
	private final ProgressBar progressBar;

	/**
	 * Instantiates a new smart tour web chrome client.
	 *
	 * @param progressBar the progress bar
	 */
	public OdiiWebChromeClient(AppCompatActivity activity, ProgressBar progressBar) {
		// TODO Auto-generated constructor stub
		this.activity = activity;
		this.progressBar = progressBar;
	}


	/* (non-Javadoc)
	 * @see android.webkit.WebChromeClient#onGeolocationPermissionsShowPrompt(java.lang.String, android.webkit.GeolocationPermissions.Callback)
	 */
	@Override
	public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
		// TODO Auto-generated method stub
		callback.invoke(origin, true, false);
	}

	/* (non-Javadoc)
	 * @see android.webkit.WebChromeClient#onProgressChanged(android.webkit.WebView, int)
	 */
	@Override
	public void onProgressChanged(WebView view, int newProgress) {
		// TODO Auto-generated method stub
		super.onProgressChanged(view, newProgress);

		if (progressBar != null) {
			progressBar.setProgress(newProgress);
		}
	}


	// For Lollipop 5.0+ Devices
	public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
		if (uploadMessage != null) {
			uploadMessage.onReceiveValue(null);
			uploadMessage = null;
		}

		uploadMessage = filePathCallback;

		Intent intent = fileChooserParams.createIntent();
		try {
            AppCompatActivity currentActivity = activity;
            if(currentActivity instanceof MainActivity) {
                ((MainActivity) currentActivity).getChromeClientLauncher()
                        .launch(intent);
            }
		} catch (ActivityNotFoundException e) {
			uploadMessage = null;
			Toast.makeText(activity, "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	@Override
	public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
		/*
		new AlertDialog.Builder(activity)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
				.create().show();
		*/

		//---> 웹 alert()에 대한 명시적인 취소동작이 result.cancel() 없을 경우, 이후 웹뷰가 반응하지않는다.(loadurl , 자바스크립트 호출 등..)
		//추가수정
		new AlertDialog.Builder(activity)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
				.setOnCancelListener(dialogInterface -> {
					result.cancel();
				})
				.create().show();

		//추가수정(취소 금지 방식, 명시적 confirm만)
		/*
		new AlertDialog.Builder(activity)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
				.setCancelable(false)
				.create().show();
		*/
		return true;
	}


	@Override
	public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
		/*
		new AlertDialog.Builder(activity)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
				.setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
				.create().show();
		*/
		//추가수정
		new AlertDialog.Builder(activity)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
				.setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
				.setOnCancelListener(dialogInterface -> {
					result.cancel();
				})
				.create().show();

		return true;
	}

	private Dialog onCreateWindowDialog;

	@Override
	public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {

		WebView newWebView = new WebView(activity);
		WebSettings webSettings = newWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		onCreateWindowDialog = new Dialog(activity);
		onCreateWindowDialog.setContentView(newWebView);
		onCreateWindowDialog.show();
		newWebView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onCloseWindow(WebView window) {
				onCreateWindowDialog.dismiss();
				onCreateWindowDialog = null;
			}
		});
		((WebView.WebViewTransport) resultMsg.obj).setWebView(newWebView);
		resultMsg.sendToTarget();
		return true;
	}

	public void closeCreateWindowDialog() {
		if(onCreateWindowDialog != null) {
			onCreateWindowDialog.dismiss();
			onCreateWindowDialog = null;
		}
	}

	public void onActivityResult(int resultCode, Intent intent) {
        if (uploadMessage == null)
            return;
        uploadMessage.onReceiveValue(FileChooserParams.parseResult(resultCode, intent));
        uploadMessage = null;
    }

}
