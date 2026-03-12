package kto.smarttour.webview;

public interface OdiiWebViewCallback {

    void onProgressChanged(int newProgress);

    void onPageStart();

    void onPageFinished();

}
