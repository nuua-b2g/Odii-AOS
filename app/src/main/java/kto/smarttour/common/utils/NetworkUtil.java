package kto.smarttour.common.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 네트워크 상태 체크.
 *
 * @author Changhyun Jeon
 * @since 2015. 3. 24
 */
public class NetworkUtil {

    final static private long HTTP_CONNECT_TIME_OUT = 5000;
    final static private long HTTP_READ_TIME_OUT = 30000;

    /**
     * 네트워크 사용 불가
     */
    final static public int NETWORK_DISCONNECTED = 0;
    /**
     * 네트워크가 연결 되어 있음
     */
    final static public int NETWORK_CONNECTED = 1;
    /**
     * 네트워크가 연결 되었거나 연결중
     */
    final static public int NETWORK_PROCESS_OF_BEING = 2;

    /**
     * 현재 사용중인 네트워크의 상태를 반환 합니다.
     *
     * @param context
     * @return 네트워크 상태 상수
     */
    static public int getNetworkState(Context context) {
        // 시스템 서비스로부터 Connectivitymanager를 얻는다.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // 현재 활성화된 네트워크 정보를 얻는다.
        NetworkInfo ni = cm.getActiveNetworkInfo();

        // 연결됨
        if (ni.isConnected()) return NETWORK_CONNECTED;
            // 연결중
        else if (ni.isConnectedOrConnecting()) return NETWORK_PROCESS_OF_BEING;
            // 끊어짐
        else return NETWORK_DISCONNECTED;
    }

    /**
     * 단순히 인터넷이 연결되어있는지만 체크한다.
     *
     * @param context
     * @return
     */
    static public boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        try {
            return ni.isConnectedOrConnecting();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * {@link ConnectivityManager#TYPE_WIFI}로 연결되어 있거나 연결중이면 true. 아니면 false
     *
     * @param context
     * @return
     */

    static public boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        try {
            return ni.isConnectedOrConnecting();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * {@link ConnectivityManager#TYPE_MOBILE}로 연결되어 있거나 연결중이면 true. 아니면 false
     *
     * @param context
     * @return
     */
    static public boolean isMobileConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        try {
            return ni.isConnectedOrConnecting();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * {@link ConnectivityManager#TYPE_WIMAX}로 연결되어 있거나 연결중이면 true. 아니면 false<br>
     * API8 미만에서는 사용불가능하다.
     *
     * @param context
     * @return
     */
    static public boolean isLTEConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);
        try {
            return ni.isConnectedOrConnecting();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * 웹 서버가 살아있는지 체크하는 메서드 입니다.<br>
     * 파라메터로 host값을 넘깁니다. 받는 서버의 ping포트가 열려있어야 합니다.
     *
     * @param url 조회할 URL 객체
     * @return 서버의 살아있는 상태
     */
    static public boolean isServerAlive(URL url) {
        boolean status = false;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            if (conn != null) {
                conn.setConnectTimeout((int) HTTP_CONNECT_TIME_OUT);
                conn.setReadTimeout((int) HTTP_READ_TIME_OUT);
                conn.setUseCaches(false);
                status = conn.getResponseCode() == HttpURLConnection.HTTP_OK;
            }
        } catch (Exception e) {
            status = false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return status;
    }

    /**
     * 웹 서버가 살아있는지 체크하는 메서드 입니다.<br>
     * 파라메터로 host값을 넘깁니다. 받는 서버의 ping포트가 열려있어야 합니다.
     *
     * @param strUrl 프로토콜을 포함한 URL주소의 문자열 *ex) "http://127.0.0.1"
     * @return 서버의 살아있는 상태(URL 형식 예외도 false를 반환합니다.)
     */
    static public boolean isServerAlive(String strUrl) {
        try {
            return isServerAlive(new URL(strUrl));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 해당 URL이 이미지 파일의 경로를 가르키는지에 대한 상태를 리턴합니다.
     *
     * @param url 이미지 파일 경로
     * @return <code>true</code> 이미지 파일임(png, gif, jpg, jpeg)
     */
    static public boolean isImageFileURL(String url) {
        final String lowerCaseURL = url.toLowerCase();
        return lowerCaseURL.endsWith("png") || lowerCaseURL.endsWith("jpg") || lowerCaseURL.endsWith("gif") || lowerCaseURL.endsWith("jpeg");
    }
}
