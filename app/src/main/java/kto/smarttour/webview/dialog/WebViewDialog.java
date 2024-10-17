package kto.smarttour.webview.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import java.util.Stack;

import kto.smarttour.R;

/**
 * 웹페이지의 자식 창 생성시 보여줄 다이알로그 화면.
 *
 * @author Changhyun Jeon
 * @since 2015. 3. 17
 */
public class WebViewDialog extends Dialog {

	/** @see UserActionListener */
	protected UserActionListener userActionListener = null;

	/** 웹뷰의 부모 레이아웃. */
	private RelativeLayout container;

	/** 추가된 웹뷰를 스텍으로 관리 */
	private Stack<WebView> stack = new Stack<>();

	/**
	 * Instantiates a new web view dialog.
	 *
	 * @param context the context
	 * @param webview the webview
	 */
	public WebViewDialog(Context context, WebView webview) {

		super(context, R.style.DialogNoWindowFrame);
		stack.push(webview);
	}

	/* (non-Javadoc)
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

		setContentView(R.layout.webview_dialog);

		getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

		container = (RelativeLayout)findViewById(R.id.webview_container);
		container.addView(stack.firstElement());
	}

	/* (non-Javadoc)
	 * @see android.app.Dialog#dismiss()
	 */
	@Override
	public void dismiss() {

		while(stack.size() > 0) {
			WebView webview = stack.pop();
			destoryWebview(webview);
		}
		super.dismiss();
	}

	/**
	 * Adds the web view.
	 *
	 * @param webview the webview
	 */
	public void addWebView(WebView webview) {

		container.addView(webview);
		stack.push(webview);
	}

	/**
	 * Removes the web view.
	 *
	 * @param webview the webview
	 */
	public void removeWebView(WebView webview) {

		container.removeView(webview);
		stack.remove(webview);
		destoryWebview(webview);
		if(stack.size() == 0) {
			if(null != userActionListener) {
				userActionListener.onClose();
			}
			dismiss();
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Dialog#onBackPressed()
	 */
	@Override
	public void onBackPressed() {

		if(stack.size() == 0) {
			super.onBackPressed();
		}

		// webView 에서 back key 처리
		WebView webview = stack.lastElement();
		if(webview.canGoBack()) {
			webview.goBack();
		} else {
			if(stack.size() == 1) {
				if(null != userActionListener) {
					userActionListener.onClose();
				}
				destoryWebview(webview);
				super.onBackPressed();
			} else {
				stack.pop();
				container.removeView(webview);
				destoryWebview(webview);
			}
		}
	}

	/**
	 * Destory webview.
	 *
	 * @param webview the webview
	 */
	private void destoryWebview(WebView webview) {

		webview.destroyDrawingCache();
		webview.destroy();
	}

	/* (non-Javadoc)
	 * @see android.app.Dialog#onDetachedFromWindow()
	 */
	@Override
	public void onDetachedFromWindow() {

		super.onDetachedFromWindow();
		while(stack.size() > 0) {
			WebView webview = stack.pop();
			destoryWebview(webview);
		}
	}

	/**
	 * Sets the user action listener.
	 *
	 * @param listener the new user action listener
	 */
	public void setUserActionListener(UserActionListener listener) {

		userActionListener = listener;
	}

	/**
	 * 다이알로그가 닫힐경우 이벤트를 받을 리스너.
	 */
	public interface UserActionListener {

		/**
		 * 다이알로그 닫힘 이벤트.
		 */
		 void onClose();
	}

}
