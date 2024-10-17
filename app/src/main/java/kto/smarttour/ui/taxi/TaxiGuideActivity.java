package kto.smarttour.ui.taxi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.LinearLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.socks.library.KLog;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.Common;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.LocationUtils;
import kto.smarttour.common.utils.SystemUtils;
import kto.smarttour.databinding.TaxiGuideActivityBinding;
import kto.smarttour.ui.player.TaxiDriverPlayer;

public class TaxiGuideActivity extends BaseActivity {

    private TaxiGuideActivityBinding mBind;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private float filtSpeed;
    private float localspeed;

    private static final long UPDATE_INTERVAL = 1000;
    private static final long FASTEST_UPDATE_INTERVAL = 1000;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;


    @Override
    protected void onResume() {
        super.onResume();

        //	액티비티 확인 - 택시 가이드
        Common.gCurrentTaxtActivity = Common.ACTIVITY_TAXI_GUIDE;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBind = DataBindingUtil.setContentView(this, R.layout.taxi_guide_activity);
        mBind.setLifecycleOwner(this);
        mBind.btnClose.setOnClickListener(view -> finish());

        if (statusBarHeight > 0) {
            LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams) mBind.dumpView.getLayoutParams();
            ll.height = statusBarHeight;
            mBind.dumpView.setLayoutParams(ll);
        }

        createLocationRequest();

        if (!checkPermissions()) {
            requestPermissions();
        }
        else {
            requestLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeLocationUpdates();
    }

    private void requestPermissions() {
        boolean permissionAccessFineLocationApproved = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (permissionAccessFineLocationApproved) {
            requestLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(TaxiGuideActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private boolean checkPermissions() {
        int fineLocationPermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        //지오팬스 ACCESS_BACKGROUND_LOCATION
        int backgroundLocationPermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        return (fineLocationPermissionState == PackageManager.PERMISSION_GRANTED) && (backgroundLocationPermissionState == PackageManager.PERMISSION_GRANTED);

        //return (fineLocationPermissionState == PackageManager.PERMISSION_GRANTED);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    public void requestLocationUpdates() {
        try {
            LocationUtils.setRequestingLocationUpdates(this, true);
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            LocationUtils.setRequestingLocationUpdates(this, false);
            e.printStackTrace();
        }
    }

    public void removeLocationUpdates() {
        LocationUtils.setRequestingLocationUpdates(this, false);
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locations = locationResult.getLocations();
            Location location = locations.get(0);

            boolean hasSpeed = false;
            if (location.hasSpeed()) {
                hasSpeed = true;

                localspeed = location.getSpeed() * 3.6f;
                filtSpeed = speedFilter(filtSpeed, localspeed);

                runOnUiThread(() -> {
                    if (filtSpeed >= 10) {
                        //	이동중
                        Common.isTaxiDriving = true;
                    } else {
                        Common.isTaxiDriving = false;
                    }
                });
                KLog.i("LocationCheckValue", String.format("speed : %s", filtSpeed));
            }

            if (Common.isPushAvailable()) {
                Common.showNotification(getApplicationContext(), location);
            }

        }

    };

    private float speedFilter(final float prev, final float curr) {
        if (Float.isNaN(prev)) return curr;
        if (Float.isNaN(curr)) return prev;
        return (float) (curr / 2 + prev * (1.0 - 1.0 / 2));
    }

}
