package kto.smarttour.common.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

import com.socks.library.KLog;

import java.util.concurrent.CopyOnWriteArrayList;

@SuppressLint("NewApi")
public class ForegroundDetector implements Application.ActivityLifecycleCallbacks {

	public interface Listener {
		void onBecameForeground();

		void onBecameBackground();
	}

	private int refs;
	private CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
	private static ForegroundDetector Instance = null;

	public static ForegroundDetector getInstance() {
		return Instance;
	}

	public ForegroundDetector(Application application) {
		Instance = this;
		application.registerActivityLifecycleCallbacks(this);
	}

	public boolean isForeground() {
		return refs > 0;
	}

	public boolean isBackground() {
		return refs == 0;
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	@Override
	public void onActivityStarted(Activity activity) {
		if (++refs == 1) {
			KLog.e("TEST_LOG", "switch to foreground");
			for (Listener listener : listeners) {
				try {
					listener.onBecameForeground();
				} catch (Exception e) {
					KLog.e("TEST_LOG", e.toString());
				}
			}
		}
	}

	@Override
	public void onActivityStopped(Activity activity) {
		if (--refs == 0) {
			KLog.e("TEST_LOG", "switch to background");
			for (Listener listener : listeners) {
				try {
					listener.onBecameBackground();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
	}

	@Override
	public void onActivityResumed(Activity activity) {
	}

	@Override
	public void onActivityPaused(Activity activity) {
	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
	}

	@Override
	public void onActivityDestroyed(Activity activity) {
	}
}