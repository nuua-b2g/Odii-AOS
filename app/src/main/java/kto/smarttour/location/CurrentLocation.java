package kto.smarttour.location;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * 위치정보 제공.
 *
 * @author Changhyun Jeon
 * @since 2015. 3. 18
 */
public class CurrentLocation implements LocationListener {

	public static final double DEFAULT_LAT = 37.566668;
	public static final double DEFAULT_LNG = 126.978371;

	/**
	 * 위치정보 수신시 이벤트 리스너.
	 */
	public interface OnLocationListener {

		/**
		 * On recieved location.
		 *
		 * @param location the location
		 */
		void onRecievedLocation(Location location);
	}

	/**
	 * The context.
	 */
	private Context context;

	/**
	 * The on location listener.
	 *
	 * @see OnLocationListener
	 */
	private OnLocationListener onLocationListener;

	/**
	 * The location manager.
	 */
	private LocationManager locationManager;

	/**
	 * The location update interval.
	 */
	private final long LOCATION_UPDATE_INTERVAL = 10000;

	/**
	 * The location update distance.
	 */
	private final float LOCATION_UPDATE_DISTANCE = 10;

	private String currentProvider = null;

	/**
	 * Instantiates a new current location.
	 *
	 * @param context            the context
	 * @param onLocationListener the on location listener
	 */
	public CurrentLocation(Context context, OnLocationListener onLocationListener) {
		this.context = context;
		this.onLocationListener = onLocationListener;

		this.locationManager = (LocationManager) context.getSystemService(AppCompatActivity.LOCATION_SERVICE);
	}

	/**
	 * Sets the on location listener.
	 *
	 * @param onLocationListener the new on location listener
	 */
	public void setOnLocationListener(OnLocationListener onLocationListener) {
		this.onLocationListener = onLocationListener;
	}

	/**
	 * Checks if is location enabled.
	 *
	 * @param context the context
	 * @return true, if is location enabled
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	@SuppressWarnings("deprecation")
	public boolean isLocationEnabled(Context context) {

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			String providers = Secure.getString(context.getContentResolver(), Secure.LOCATION_PROVIDERS_ALLOWED);
			return !TextUtils.isEmpty(providers);
		} else {
			final int locationMode;
			try {
				locationMode = Secure.getInt(context.getContentResolver(), Secure.LOCATION_MODE);
			} catch (SettingNotFoundException e) {
				e.printStackTrace();
				return false;
			}
			switch (locationMode) {

				case Secure.LOCATION_MODE_HIGH_ACCURACY:
				case Secure.LOCATION_MODE_SENSORS_ONLY:
				case Secure.LOCATION_MODE_BATTERY_SAVING:
					return true;
				case Secure.LOCATION_MODE_OFF:
				default:
					return false;
			}
		}
	}

	/**
	 * Find current location immediatley.
	 *
	 * @return the location
	 */
	public Location findCurrentLocationImmediatley() {
		locationManager = (LocationManager) context.getSystemService(AppCompatActivity.LOCATION_SERVICE);

		String locationProvider = getLocationProvider();
		Location location = null;
		if (locationProvider != null) {
			if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				location = locationManager.getLastKnownLocation(locationProvider);
			}
		}

		return location;
	}

	/**
	 * Gets the location provider.
	 *
	 * @return the location provider
	 */
	public String getLocationProvider() {
		String bestProvider;
		Criteria criteria = new Criteria(); // Criteria는 위치 정보를 가져올 때 고려되는 옵션 정도로 생각하면 된다.
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setSpeedRequired(false);
		criteria.setCostAllowed(true);

		bestProvider = locationManager.getBestProvider(criteria, true);

		return bestProvider;
	}

	/**
	 * Gets the last known location.
	 *
	 * @return the last known location
	 */
	public Location getLastKnownLocation() {
		locationManager = (LocationManager) context.getSystemService(AppCompatActivity.LOCATION_SERVICE);
		List<String> providers = locationManager.getProviders(true);
		Location bestLocation = null;
		for (String provider : providers) {
			Location l = null;
			if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				l = locationManager.getLastKnownLocation(provider);
			}
			if (l == null) {
				continue;
			}
			if (bestLocation == null || l.getTime() > bestLocation.getTime()) {
				// Found best last known location: %s", l);
				bestLocation = l;
			}
		}
		return bestLocation;
	}

	/**
	 * Start location update.
	 */
	public void startLocationUpdate() {
		String locationProvider = getLocationProvider();

		if (isLocationEnabled(context)) {
			Location location = getLastKnownLocation();

			if (location != null && onLocationListener != null) {
				onLocationListener.onRecievedLocation(location);
			}
			currentProvider = locationProvider;
			if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				locationManager.requestLocationUpdates(locationProvider, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISTANCE, this);
			}
			/**
			 * 2016.06.13 add
			 */
			else {
				onLocationListener.onRecievedLocation(null);
			}
			/**
			 * end add 2016.06.13
			 */

		} else {
			if (onLocationListener != null) {
				onLocationListener.onRecievedLocation(null);
			}
		}
	}

	/**
	 * Stop location update.
	 */
	public void stopLocationUpdate() {
		try {
			if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				locationManager.removeUpdates(this);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		if (onLocationListener != null) {
			onLocationListener.onRecievedLocation(location);
		}
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		if (provider.equals(currentProvider)) {
			stopLocationUpdate();
			startLocationUpdate();
		}
	}

	public static boolean checkGps(Context context) {
		LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		boolean gps_enabled = false;

		try {
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}

		return gps_enabled;
	}
}
