package kto.smarttour.ui.taxi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.tabs.TabLayout;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.socks.library.KLog;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import kto.smarttour.R;
import kto.smarttour.adapter.TaxiStoryListAdapter;
import kto.smarttour.binding.BindingAdapters;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.Common;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.LocationUtils;
import kto.smarttour.common.utils.NetworkUtil;
import kto.smarttour.common.utils.SimpleDividerItemDecoration;
import kto.smarttour.common.utils.StorageUtil;
import kto.smarttour.common.utils.TourTaxiAnalytics;
import kto.smarttour.common.utils.ViewUtils;
import kto.smarttour.databinding.ActivityTaxiStoryLocationPopupBinding;

import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.download.FileDownLoadType;
import kto.smarttour.network.response.dao.TourTaxiInfo;
import kto.smarttour.ui.player.TaxiDriverPlayer;

public class TaxiStoryLocationPopupActivity extends BaseActivity {

    private ActivityTaxiStoryLocationPopupBinding mBind;

    private String lang;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private float filtSpeed;
    private float localspeed;

    private static final long UPDATE_INTERVAL = 1000;
    private static final long FASTEST_UPDATE_INTERVAL = 1000;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private boolean modeTabSelection = true; //기본값
    private int colorTvTabSelected = Color.parseColor("#ff6755bb");
    private int colorTvTabNormal = Color.parseColor("#ff8e8e8e");

    //-------------------------------------------------------------------
    //--
    private AppCompatActivity activity;
    private StoryItem item;

    //--
    private MapView mapView;
    private NaverMap mNaverMap;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;

        mBind = DataBindingUtil.setContentView(this, R.layout.activity_taxi_story_location_popup);
        mBind.setLifecycleOwner(this);

        mapView = findViewById(R.id.map_view);
        if(mapView!=null){
            mapView.onCreate(savedInstanceState);
        }

        //-------------------------------------------------------------------------------------------------
        mBind.btnMapPopupClose.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });
        mBind.layoutMapPopupEmpty.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });
        //-------------------------------------------------------------------------------------------------

        lang = getIntent().getStringExtra("lang");

        Object obj = getIntent().getSerializableExtra("StoryItem");
        if(obj!=null){
            if(obj instanceof StoryItem){
                item = (StoryItem)obj;
            }
        }

        //-------------------------------------------------------------------------------------------------
        //@{!TextUtils.isEmpty(story.titleKo)?story.titleKo:story.title }
        String title = !TextUtils.isEmpty(item.titleKo)?item.titleKo:item.title;
        mBind.tvMapPopupTitle.setText(title);
        //-------------------------------------------------------------------------------------------------
        mapView.getMapAsync(onMapReadyCallback);
    }

    //네이버지도뷰에서 네이버지도 객체 준비콜백
    OnMapReadyCallback onMapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull @NotNull NaverMap naverMap) {

            mNaverMap = naverMap;

            if(mNaverMap!=null){

                if(item!=null){

                    double lat = -1.0f;
                    double lon = -1.0f;
                    try {
                        lat = Double.valueOf(item.posY);
                        lon = Double.valueOf(item.posX);
                    } catch (NumberFormatException e){
                        lat = -1.0f;
                        lon = -1.0f;
                    }

                    if(lat>0 && lon>0){

                        LatLng position = new LatLng(lat,lon);
                        PointF anchor = new PointF(0.5f,1.0f);
                        OverlayImage icon = OverlayImage.fromResource(R.drawable.tx_img_txmap_pin);

                        Marker marker = new Marker();
                        marker.setPosition( position );
                        marker.setAnchor( anchor );
                        marker.setIcon( icon );
                        marker.setMap(mNaverMap);

                        //--
                        CameraUpdate cameraUpdate = CameraUpdate.scrollTo( position ).animate(CameraAnimation.Easing);
                        mNaverMap.moveCamera(cameraUpdate);

                    }
                }

            }

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if(mapView!=null){
            mapView.onStart();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(mapView!=null){
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mapView!=null){
            mapView.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mapView!=null){
            mapView.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mapView!=null){
            mapView.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mapView!=null){
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if(mapView!=null){
            mapView.onLowMemory();
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(0, 0);
    }

    private void requestPermissions() {
        boolean permissionAccessFineLocationApproved = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (permissionAccessFineLocationApproved) {
            requestLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(TaxiStoryLocationPopupActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private boolean checkPermissions() {
        int fineLocationPermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        int backgroundLocationPermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        return (fineLocationPermissionState == PackageManager.PERMISSION_GRANTED) && (backgroundLocationPermissionState == PackageManager.PERMISSION_GRANTED);

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
                //KLog.i("LocationCheckValue", String.format("speed : %s", filtSpeed));
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
