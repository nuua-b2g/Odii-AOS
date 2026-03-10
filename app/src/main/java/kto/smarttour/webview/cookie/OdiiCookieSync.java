package kto.smarttour.webview.cookie;

import android.content.Context;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

@SuppressWarnings("deprecation")
public class OdiiCookieSync {
    
    public static void sync(){
        CookieManager.getInstance().flush();
    }

}
