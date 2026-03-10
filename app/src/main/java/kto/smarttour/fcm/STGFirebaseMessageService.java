package kto.smarttour.fcm;

/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.collection.ArrayMap;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import kto.smarttour.R;
import kto.smarttour.common.utils.PendingIntentUtils;
import kto.smarttour.ui.MainActivity;

/**
 * Created by ttb-02 on 2016-11-02.
 */
public class STGFirebaseMessageService extends FirebaseMessagingService {
    Bitmap bitmap;
    NotificationManager notificationManager = null;

    public void onMessageReceived(RemoteMessage remoteMessage) {

        /*
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE );
        PowerManager.WakeLock wakeLock = pm.newWakeLock( PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG" );
        wakeLock.acquire(3000);
        */

        /* 기존코드
        try {
            String imageUri = remoteMessage.getData().get("image");
            bitmap = getBitmapfromUrl(imageUri);
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody(), bitmap);
        } catch (Exception e) {
            Log.d("TAG",">>> remoteMessage e = "+e.getMessage());
        }
         */

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();

            String title = data.get("title");
            String body = data.get("body");

            String imageUri = data.get("image");
            bitmap = getBitmapfromUrl(imageUri);

            try {
                sendNotification(title, body, bitmap, data);
            } catch (Exception e) {
                Log.d("TAG",">>> remoteMessage ,sendNotification e = "+e.getMessage());
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d("TAG", ">>> Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

    }


    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String title, String messageBody, Bitmap image, Map<String,String> data) {
        Log.d("TAG",">>> remoteMessage ->sendNotification messageBody = "+messageBody);

        NotificationCompat.Builder notificationBuilder;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        //PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);
        /*
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0 , intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
        }else{
            pendingIntent = PendingIntent.getActivity(this, 0 , intent, PendingIntent.FLAG_ONE_SHOT);
        }
        */

        //fcm수신데이터 Map 전달.
        HashMap<String, String> fcmData = new HashMap<>(data);
        intent.putExtra("fcmData",fcmData); //Serializable

        PendingIntent pendingIntent = PendingIntentUtils.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);


        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        title = title != null ? title : getString(R.string.app_name);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String chId = getString(R.string.default_notification_channel_id);
        NotificationCompat.Style style = null;
        if(image!=null) {
            NotificationCompat.BigPictureStyle bigStyle = new NotificationCompat.BigPictureStyle();
            bigStyle.setBigContentTitle(title);
            bigStyle.setSummaryText(messageBody);
            bigStyle.bigPicture(image);
            style = bigStyle;

        }else{

            NotificationCompat.BigTextStyle bigxtstyle =  new NotificationCompat.BigTextStyle();
            bigxtstyle.setBigContentTitle(title);
            bigxtstyle.bigText(messageBody);
            style = bigxtstyle;

        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = notificationManager.getNotificationChannel(chId);
            if (mChannel == null) {
                mChannel = new NotificationChannel(chId, title, importance);
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{0});
                mChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.GREEN);

                notificationManager.createNotificationChannel(mChannel);
            }

            notificationBuilder = new NotificationCompat.Builder(this, chId)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setColor(0xff3277ff)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setStyle(style)
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setContentIntent(pendingIntent);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this, chId)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setColor(0xff3277ff)
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setStyle(style)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);
        }
        notificationManager.notify(0, notificationBuilder.build());
    }

    public Bitmap getBitmapfromUrl(String imageUrl) {
        if(imageUrl==null)
            return null;
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return bitmap;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;

        }
    }
}
