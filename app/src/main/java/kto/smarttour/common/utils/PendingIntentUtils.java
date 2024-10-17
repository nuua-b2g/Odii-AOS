package kto.smarttour.common.utils;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.app.TaskStackBuilder;


public class PendingIntentUtils {


    //Android 12이상 대응 PendingIntent반환
    /**
     PendingIntent getActivity
     안드로이드 12 플래그 기본지정
     PendingIntent.FLAG_IMMUTABLE
     */
    public static PendingIntent getActivity(@NonNull Context context, int requestCode, @NonNull Intent intent, int flags) {
        //@Flags int flags

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | flags);
        } else {
            pendingIntent = PendingIntent.getActivity(context, requestCode, intent, flags);
        }
        return pendingIntent;
    }

    /**
     PendingIntent TaskStackBuilder_getPendingIntent
     안드로이드 12 플래그 기본지정
     PendingIntent.FLAG_IMMUTABLE
     */
    public static PendingIntent TaskStackBuilder_getPendingIntent(@NonNull TaskStackBuilder stackBuilder, int requestCode, int flags){
        //@Flags int flags

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_IMMUTABLE | flags);
        } else {
            pendingIntent = stackBuilder.getPendingIntent(requestCode, flags);
        }
        return pendingIntent;
    }

    /**
     PendingIntent getBroadcast
     안드로이드 12 플래그 기본지정
     PendingIntent.FLAG_IMMUTABLE
     */
    public static PendingIntent getBroadcast(@NonNull Context context, int requestCode, @NonNull Intent intent, int flags){
        //@Flags int flags

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | flags);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        }
        return pendingIntent;
    }

}
