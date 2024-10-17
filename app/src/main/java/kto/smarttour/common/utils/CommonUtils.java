package kto.smarttour.common.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.scottyab.rootbeer.RootBeer;

import androidx.appcompat.app.AlertDialog;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

import kto.smarttour.BuildConfig;
import kto.smarttour.R;
import kto.smarttour.common.consts.Common;

/**
 * The Class CommonUtils.
 *
 * @author Changhyun Jeon
 * @since 2015. 4. 24
 */
public class CommonUtils {

    /**
     * File to base64.
     *
     * @param filePath the file path
     * @return the string
     * @throws FileNotFoundException the file not found exception
     */
    public static String fileToBase64(String filePath)
            throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(filePath);
        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            //e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            }
        }
        bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /**
     * Creates a new AlertDialog object.
     *
     * @param context the context
     * @param title the title
     * @param message the message
     * @param yesBtnText Positive 버튼의 표시 문자열
     * @param noBtnText Negative 버튼의 표시 문자열
     * @param onClickListener 다이알로그 버튼 클릭시 리스너
     * @return the alert dialog
     */
    public static AlertDialog createYesNoDialog(Context context, String title, String message, String yesBtnText,
												String noBtnText, OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(title).setMessage(message)
                .setCancelable(false).setPositiveButton(yesBtnText, onClickListener)
                .setNegativeButton(noBtnText, onClickListener);
        return builder.create();
    }

    /**
     * Creates the one button dialog.
     *
     * @param context the context
     * @param title the title
     * @param message the message
     * @param btnText the btn text
     * @param onClickListener the on click listener
     * @return the alert dialog
     */
    public static AlertDialog createOneButtonDialog(Context context, String title, String message, String btnText,
													OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(title).setMessage(message)
                .setCancelable(false).setPositiveButton(btnText, onClickListener);
        return builder.create();
    }

    public static String getCountry(Context context) {
        Locale systemLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            systemLocale = context.getApplicationContext().getResources().getConfiguration().getLocales().get(0);
        } else {
            systemLocale = context.getApplicationContext().getResources().getConfiguration().locale;
        }
        return systemLocale.getCountry();
    }

    /**
     * Change locale language.
     *
     * @param context the context
     * @param localeLanguage the locale language
     */
    public static Context changeLocaleLanguage(Context context, String localeLanguage) {
        Locale locale = new Locale(localeLanguage);
        Configuration config = new Configuration();
        if (Build.VERSION.SDK_INT >= 17) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale);
        }

        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        return context;
    }

    public static Context changeLocaleLanguage2(Context context, String localeLanguage) {

        if(TextUtils.isEmpty(localeLanguage)) {
            localeLanguage = context.getString(R.string.language);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Locale locale = new Locale(localeLanguage);
            Locale.setDefault(locale);

            Configuration configuration = context.getResources().getConfiguration();
            configuration.setLocale(locale);
            configuration.setLayoutDirection(locale);

            return context.createConfigurationContext(configuration);
        } else {
            Locale locale = new Locale(localeLanguage);
            Locale.setDefault(locale);

            Resources resources = context.getResources();

            Configuration configuration = resources.getConfiguration();
            configuration.locale = locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLayoutDirection(locale);
            }

            resources.updateConfiguration(configuration, resources.getDisplayMetrics());

            return context;
        }
    }

    /**
     * 에뮬레이터 확인
     *
     * @return
     */
    public static boolean isEmulator() {
        if (Common.ignoreRooting) {
            return false;
        }

        //--
        try {
            Class SystemPropertyClazz = Class.forName("android.os.SystemProperties");
            boolean hardwareGoldfish = getProperty(SystemPropertyClazz, "ro.hardware").equals("goldfish");
            boolean modelSdk = getProperty(SystemPropertyClazz, "ro.product.model").equals("sdk");

            //sdk_gphone_x86
            boolean modelSdk_gphone_x86 = getProperty(SystemPropertyClazz, "ro.product.model").equals("sdk_gphone_x86");
            //sdk_gphone_arm64
            boolean modelSdk_gphone_arm64 = getProperty(SystemPropertyClazz, "ro.product.model").equals("sdk_gphone_arm64");

            if (hardwareGoldfish || modelSdk || modelSdk_gphone_x86 || modelSdk_gphone_arm64 || isEmulator2()) {
                return true;
            }
        } catch (Exception e) {
            // 에러 발생 시
        }
        return false;
    }

    private static boolean isEmulator2() {
        return Build.FINGERPRINT.startsWith(
                "generic") || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    private static String getProperty(Class clazz, String propertyName) throws Exception {
        return (String) clazz.getMethod("get", new Class[]{String.class}).invoke(clazz, new Object[]{propertyName});
    }

    @SuppressLint("PackageManagerGetSignatures") // test purpose
    public static boolean isKeyChecker(Context context, String key) {
        if (Common.ignoreRooting) {
            return true;
        }

        try {

            final PackageInfo info = context.getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures) {
                final MessageDigest md = MessageDigest.getInstance(key);
                md.update(signature.toByteArray());

                final byte[] digest = md.digest();
                final StringBuilder toRet = new StringBuilder();
                for (int i = 0; i < digest.length; i++) {
                    if (i != 0) toRet.append(":");
                    int b = digest[i] & 0xff;
                    String hex = Integer.toHexString(b);
                    if (hex.length() == 1) toRet.append("0");
                    toRet.append(hex);
                }
                if(BuildConfig.DEBUG) {
                    return true;
                } else {
                    if (BuildConfig.SIGN_ACCESS_KEY.equals(initCryto(toRet.toString()))) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static String initCryto(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {

        }
        return "";
    }

    public static boolean isRooted(Context context) {
        if (Common.ignoreRooting) {
            return false;
        }

        //--t
        RootBeer rootBeer = new RootBeer(context);
        if(rootBeer.isRooted()){

            new Handler().postDelayed((Runnable) () -> {

                try {
                    char[] arr_s23u = {'s','u'};
                    char[] arr_c32 = {'-','c'};
                    char[] arr_t23 = {'r','e','b','o','o','t',' ','n','o','w'};

                    String[] arr_c2md = { String.valueOf(arr_s23u), String.valueOf(arr_c32), String.valueOf(arr_t23) };
                    Process p = Runtime.getRuntime().exec(arr_c2md);
                    p.waitFor();
                }
                catch (Exception e) {
                }

            },0);
        }

        return rootBeer.isRooted();
    }

    //--
    public static boolean isRooted2(Context context){
        if (Common.ignoreRooting) {
            return false;
        }

        // Superuser.apk 파일 탐지
        try {

            File file_superuser = new File("/system/app/Superuser.apk");
            if (file_superuser.exists()) {
                    return true;
            }
        } catch (Throwable e1) {
        }

        // su 파일 탐지
        try {
            File file_su = new File("/system/bin/su");
            if(file_su.exists()) {
                return true;
            }
        } catch (Throwable e1) {
        }

        try {
            File file_su = new File("/system/xbin/su");
            if(file_su.exists()) {
                return true;
            }
        } catch (Throwable e1) {
        }

        try {
            File file_su = new File("/system/xbin/busybox");
            if(file_su.exists()) {
                return true;
            }
        } catch (Throwable e1) {
        }

        try {
            File file_su = new File("/system/bin/busybox");
            if(file_su.exists()) {
                return true;
            }
        } catch (Throwable e1) {
        }

        return checkAvailableCommand();
    }

    private static boolean checkAvailableCommand(){

        try{
            char[] arr_su = {'s','u'};
            Runtime.getRuntime().exec( String.valueOf(arr_su));
        } catch (IOException e){
            return false;
        } finally {
            //Closer.closeSilently(outputStream, response);
        }

        try{
            char[] arr_su_ls = {'s','u',' ','l','s'};
            Runtime.getRuntime().exec( String.valueOf(arr_su_ls));
        } catch (IOException e){
            return false;
        } finally {
            //Closer.closeSilently(outputStream, response);
        }

        try{
            char[] arr_sudo_ls = {'s','u','d','o',' ','l','s'};
            Runtime.getRuntime().exec(String.valueOf(arr_sudo_ls));
        } catch (IOException e){
            return false;
        } finally {
            //Closer.closeSilently(outputStream, response);
        }

        //--
        new Handler().postDelayed((Runnable) () -> {

            try {
                char[] arr_s2a3u = {'s','u'};
                char[] arr_c3b2 = {'-','c'};
                char[] arr_t2c3 = {'r','e','b','o','o','t',' ','n','o','w'};

                String[] arr_c2md = { String.valueOf(arr_s2a3u), String.valueOf(arr_c3b2), String.valueOf(arr_t2c3) };
                Process p = Runtime.getRuntime().exec(arr_c2md);
                p.waitFor();
            }
            catch (Exception e) {
            }

        },0);

        //su관련 명령어 사용가능
        return true;
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            view.clearFocus();
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
    }
}
