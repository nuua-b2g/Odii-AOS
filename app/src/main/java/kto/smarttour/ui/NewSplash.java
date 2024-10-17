package kto.smarttour.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.view.View;
import android.os.Bundle;
import android.widget.Button;
import android.content.Intent;
import kto.smarttour.R;


import android.content.SharedPreferences;
public class NewSplash extends AppCompatActivity {

    @Override
    public void onBackPressed() {
        // 아무 작업도 하지 않음
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_splash);

        SharedPreferences sharedPreferences = getSharedPreferences("local_prefer", Context.MODE_PRIVATE);
        boolean isFirst = sharedPreferences.getBoolean("ntcon_key_is_first", true);
        if (!isFirst) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
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
                Intent intent = new Intent(NewSplash.this, MainActivity.class);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("ntcon_key_is_first", false);
                editor.apply();
                startActivity(intent);
            }
        });
    }

}