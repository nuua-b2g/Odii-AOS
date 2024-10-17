package kto.smarttour.common.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class CustomViewPager extends ViewPager {

	private boolean isPagingEnabled = true;

	public CustomViewPager(Context context) {
		super(context);
	}

	public CustomViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {


		try {
			return this.isPagingEnabled && super.onTouchEvent(event);
		} catch (IllegalArgumentException ex) {
		}
		return false;

	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		try {
			return this.isPagingEnabled && super.onInterceptTouchEvent(event);
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
		}
		return false;

	}

	public void setPagingEnabled(boolean b) {
		this.isPagingEnabled = b;
	}
}
