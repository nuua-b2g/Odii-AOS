package kto.smarttour.webview.cookie;

import android.content.Context;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

@SuppressWarnings("deprecation")
public class OdiiCookieSync {

    public static void startSync(Context context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            CookieSyncManager.createInstance(context);
            CookieSyncManager.getInstance().startSync();
        }
    }
    
    public static void sync(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            CookieSyncManager.getInstance().sync();
        }else{
            CookieManager.getInstance().flush();
        }
    }
    
    public static void stopSync(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            CookieSyncManager.getInstance().stopSync();
        }
    }
    
    public static void removeCookie(String url, String cookieName){
        CookieManager cookieManager =  CookieManager.getInstance();
        String cookie = cookieName + "=''";
        cookieManager.setCookie(url, cookie);
        sync();
    }

    public static void removeAllCookie() {
        CookieManager cookieManager = CookieManager.getInstance();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
        } else {
            cookieManager.removeAllCookie();
        }
    }
}
