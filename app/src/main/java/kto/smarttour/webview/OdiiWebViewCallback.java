package kto.smarttour.webview;

import android.webkit.WebResourceRequest;

import androidx.annotation.Nullable;

public interface OdiiWebViewCallback {

    void onProgressChanged(int newProgress);

    void onPageStart();

    void onPageFinished();

    @Nullable
    Boolean shouldOverrideUrlLoading(WebResourceRequest request);

}
