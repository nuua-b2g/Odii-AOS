package kto.smarttour.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import kto.smarttour.geo.GeofenceController;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();

		if ("android.intent.action.BOOT_COMPLETED".equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
			//지오팬스
			GeofenceController.getInstance().init(context);
			GeofenceController.getInstance().boot();
		}

	}
}