package kto.smarttour.ui.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;


import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import kto.smarttour.R;
import kto.smarttour.common.utils.ViewUtils;

public class SearchEditText extends AppCompatEditText implements TextWatcher, View.OnTouchListener, View.OnFocusChangeListener, TextView.OnEditorActionListener {
	private Drawable clearDrawable;
	private OnFocusChangeListener onFocusChangeListener;
	private OnTouchListener onTouchListener;
	private OnEditTextValueListener callback;

	private boolean editing;

	private static final int HANDLE_SEARCH_KEYWORD = 0xA1;
	private static final long DELAY_TIME_INTERACTION = 500L;

	private boolean enableTextWatcher = true;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			if(msg.what == HANDLE_SEARCH_KEYWORD && callback != null) {
				callback.onAutoCompleteResult((String)msg.obj);
			}

			super.handleMessage(msg);
		}
	};

	public SearchEditText(final Context context) {
		super(context);
		init();
	}

	public SearchEditText(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SearchEditText(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	@Override
	public void setOnFocusChangeListener(OnFocusChangeListener onFocusChangeListener) {
		this.onFocusChangeListener = onFocusChangeListener;
	}

	@Override
	public void setOnTouchListener(OnTouchListener onTouchListener) {
		this.onTouchListener = onTouchListener;
	}

	private void init() {
		Drawable tempDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_bt_form_del);
		clearDrawable = DrawableCompat.wrap(tempDrawable);
		DrawableCompat.setTintList(clearDrawable, getHintTextColors());
		clearDrawable.setBounds(0, 0, ViewUtils.dp2px(32), ViewUtils.dp2px(32));
		setClearIconVisible(false);
		super.setOnTouchListener(this);
		super.setOnFocusChangeListener(this);
		super.setOnEditorActionListener(this);
		addTextChangedListener(this);
	}

	@Override
	public void onFocusChange(final View view, final boolean hasFocus) {
		if (hasFocus) {
			setClearIconVisible(getText().length() > 0);
		} else {
			setClearIconVisible(false);
		}
		if (onFocusChangeListener != null) {
			onFocusChangeListener.onFocusChange(view, hasFocus);
		}

		if(callback != null) {
			callback.onEditFocusChange(view, hasFocus);
		}
	}

	@Override
	public boolean onTouch(final View view, final MotionEvent motionEvent) {
		final int x = (int) motionEvent.getX();
		if (clearDrawable.isVisible() && x > getWidth() - getPaddingRight() - clearDrawable.getIntrinsicWidth()) {
			if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
				setError(null);
				setText(null);
				if(callback != null) {
					callback.onAutoCompleteResult("");
				}
			}
			return true;
		}
		if (onTouchListener != null) {
			return onTouchListener.onTouch(view, motionEvent);
		} else {
			return false;
		}
	}

	@Override
	public final void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
		if (isFocused()) {
			setClearIconVisible(s.length() > 0);
		}

		if(s.length() > 0 && enableTextWatcher) {
			mHandler.removeMessages(HANDLE_SEARCH_KEYWORD);
			mHandler.sendMessageDelayed(mHandler.obtainMessage(HANDLE_SEARCH_KEYWORD, s.toString()), DELAY_TIME_INTERACTION);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	@Override
	public void afterTextChanged(Editable s) {
	}

	private void setClearIconVisible(boolean visible) {
		clearDrawable.setVisible(visible, false);
		setCompoundDrawables(null, null, visible ? clearDrawable : null, null);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_SEARCH) {
			String keyword = getText().toString();
			if("".equals(keyword)){
				return true;
			}

			if(callback != null) {
				callback.onEditTextResult(v, getText().toString());
			}

			ViewUtils.hideKeyboard(getContext(), v);
			return true;
		}
		return false;
	}

	public interface OnEditTextValueListener {
		void onEditTextResult(View v, String value);
		void onAutoCompleteResult(String value);
		void onEditFocusChange(View v, boolean hasFocus);
	}

	public void setOnEditTextValueListener(OnEditTextValueListener callback) {
		this.callback = callback;
	}


	public void setTextWatcher(boolean enable) {
		enableTextWatcher = enable;

		if(!enableTextWatcher) {
			mHandler.removeMessages(HANDLE_SEARCH_KEYWORD);
		}
	}
}