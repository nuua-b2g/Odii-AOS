package kto.smarttour.webview;


import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.socks.library.KLog;

import kto.smarttour.OdiiApplication;
import kto.smarttour.webview.javascript.OdiiInterface;

public class OdiiWebView extends WebView {

    /**
     * The Constant SVR_NAME.
     */
    private static final String SVR_NAME = "STG_SVR";

    /**
     * The Constant JAVASCRIPT_PREFIX.
     */
    private static final String JAVASCRIPT_PREFIX = "javascript:" + SVR_NAME + ".";

    private OdiiInterface odiiInterface;

    /**
     * Instantiates a new smart tour web view.
     *
     * @param context the context
     */
    public OdiiWebView(Context context) {
        super(context);
        init();
    }

    /**
     * Instantiates a new smart tour web view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public OdiiWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OdiiWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 웹뷰 설정 및 진행바 설정. 진행바 사용하지 않는 경우 null
     */
    public void init() {

        WebSettings webSetting = getSettings();
        //webSetting.setAppCacheEnabled(true);
        webSetting.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSetting.setDomStorageEnabled(true);
        webSetting.setAllowFileAccess(true);
        webSetting.setAllowContentAccess(true);
        webSetting.setLoadWithOverviewMode(true);
        webSetting.setUseWideViewPort(true);
        webSetting.setJavaScriptCanOpenWindowsAutomatically(true);
        webSetting.setSupportMultipleWindows(true);
        webSetting.setSupportZoom(false);
        webSetting.setBuiltInZoomControls(false);
        webSetting.setLoadsImagesAutomatically(true);
        webSetting.setJavaScriptEnabled(true);
        webSetting.setGeolocationEnabled(true);
        webSetting.setTextZoom(100);
        webSetting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSetting.setUserAgentString("OdiiWebview");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            webSetting.setDisplayZoomControls(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            //웹뷰디버그 디버그웹뷰
            //setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

            setWebContentsDebuggingEnabled(false);
            //릴리즈 서명도 잠시 허용해주자
            //setWebContentsDebuggingEnabled(true);
        }

        if (Build.VERSION_CODES.HONEYCOMB <= Build.VERSION.SDK_INT) {
            setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        }

        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        setNetworkAvailable(true);

        odiiInterface = new OdiiInterface(OdiiApplication.getWebActivity(), this);
        addJavascriptInterface(odiiInterface, OdiiInterface.APP_NAME);
    }

    public OdiiInterface getOdiiInterface() {
        return odiiInterface;
    }

    @Override
    public void loadUrl(@NonNull String url) {
        KLog.i("ODII_WEB", "ODII_WEB_URL : " + url);
        super.loadUrl(url);
    }

    public void resStoryDownloadCount(int count) {
        loadUrl(JAVASCRIPT_PREFIX + "resStoryDownloadCount('" + count + "')");
    }

    public void resStoryLockerCount(int count) {
        loadUrl(JAVASCRIPT_PREFIX + "resStoryLockerCount('" + count + "')");
    }

    public void requestFootStampList() {
        loadUrl("javascript:requestFootStampList()");
    }

    public void resMiniPlayer(boolean visibility) {
        loadUrl(JAVASCRIPT_PREFIX + "resMiniPlayer('" + visibility + "')");
    }

    public void closeRnbMenu() {
        loadUrl(JAVASCRIPT_PREFIX + "closeRnbMenu()");
    }

    public void resAppVersion(String version) {
        loadUrl(JAVASCRIPT_PREFIX + "resAppVersion('" + version + "')");
    }

    public void resPlatform(String os) {
        loadUrl(JAVASCRIPT_PREFIX + "resPlatform('" + os + "')");
    }

    public void resGPS(String coordinate) {
        loadUrl(JAVASCRIPT_PREFIX + "resGPS('" + coordinate + "')");
    }

    public void resDefaultNetworkUse(boolean useDataNetwork) {
        loadUrl(JAVASCRIPT_PREFIX + "resDefaultNetworkUse('" + useDataNetwork + "')");
    }

    public void resDefaultPushUse(boolean useFcm, String pushToken) {
        loadUrl(JAVASCRIPT_PREFIX + "resDefaultPushUse('" + useFcm + "', " + "'" + pushToken + "')");
    }

    public void resDefaultGPSUse(boolean useGps) {
        loadUrl(JAVASCRIPT_PREFIX + "resDefaultGPSUse('" + useGps + "')");
    }

    public void resPlaylistCount(int count) {
        loadUrl(JAVASCRIPT_PREFIX + "resPlaylistCount('" + count + "')");
    }

    public void scriptBackPress() {
        loadUrl(JAVASCRIPT_PREFIX + "backPress()");
    }

    public void setCountry(String country) {
        loadUrl(JAVASCRIPT_PREFIX + "setCountry('" + country + "')");
    }

    public void resUUIDAndAppVersion(String id, String finalVersion) {
        loadUrl(JAVASCRIPT_PREFIX + "resUUIDAndAppVersion('" + id + "'" + ", '" + finalVersion + "')");
    }

    public void updateAudioEndStatus(int tlid, int slid) {
        String javascript = "STG_SVR.updateAudioEndStatus('" + tlid + "','" + slid + "');";
        evaluateJavascript(javascript, value -> {
        });
    }
}
