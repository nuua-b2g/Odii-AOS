package kto.smarttour.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.os.Bundle;
import android.widget.Button;
import android.content.Intent;
import kto.smarttour.R;
import kto.smarttour.common.consts.URLS;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.SystemUtils;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.taxi.TaxiMainActivity;


import android.content.SharedPreferences;

import com.socks.library.KLog;

public class NewSplash extends AppCompatActivity {

    @Override
    public void onBackPressed() {
        // 아무 작업도 하지 않음
    }

    private String ifwId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();

        // 관광택시 딥링크
        if(data != null) {
            String ttid = data.getQueryParameter("ttid");
            String path = data.getPath();
            Context activity = this;
            if (!TextUtils.isEmpty(ttid)) {

                int newTtid = Integer.valueOf(ttid);
                int oldTtid = SettingsUtil.getTaxiTtid(activity);

                //-1 택시모드 qr진입, 기존 다운로드 데이터 초기화됨.
                //-2 초기화 하지않음
                if (oldTtid == -1 || newTtid != oldTtid) {
                    if (oldTtid == -2) {
                        //nothing to do
                    } else {
                        StoryDbManager.getInstance(activity).clearAll();
                        FileUtils.clearCacheStoryDelete(activity);
                    }
                    SettingsUtil.setTaxiTtid(activity, Integer.valueOf(ttid));
                }

                stopService(new Intent(activity, PlayerService.class));
                SystemUtils.setTaxiPlayerServiceEnabled(activity);

                FileUtils.setGlideCacheClear(activity);

                startActivity(new Intent(NewSplash.this, TaxiMainActivity.class));
                finish();
                return;
            } else if(path != null && path.contains("inflow")) {
                ifwId = data.getQueryParameter("ifwId");
            }
        }

        setContentView(R.layout.activity_new_splash);

        SharedPreferences sharedPreferences = getSharedPreferences("local_prefer", Context.MODE_PRIVATE);
        boolean isFirst = sharedPreferences.getBoolean("ntcon_key_is_first", true);
        if (!isFirst) {
            startMainActivity(ifwId);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("ntcon_key_is_first", false);
            editor.apply();
            finish();
        } else {
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putBoolean("ntcon_key_is_first", false);
//            editor.apply();
        }
        Button confirmButton = findViewById(R.id.confirmButton);



        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("ntcon_key_is_first", false);
                editor.apply();
                startMainActivity(ifwId);
            }
        });
    }


    private void startMainActivity(String ifwId) {
        Intent intent = new Intent(NewSplash.this, MainActivity.class);
        KLog.i("startMainActivity", "ifwId: " + ifwId);
        if(ifwId != null) {
            intent.putExtra(
                    "mainNextUrl",
                    String.format(URLS.INFLOW_URL, ifwId)
            );
        }
        startActivity(intent);
    }

}