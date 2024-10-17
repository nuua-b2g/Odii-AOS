package kto.smarttour.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.socks.library.KLog;

import kto.smarttour.webview.OdiiWebView;

public class UnCatchTaskService extends Service {
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		OdiiWebView webView = new OdiiWebView(this);
		webView.clearCache(true);
		stopSelf();
	}
}
