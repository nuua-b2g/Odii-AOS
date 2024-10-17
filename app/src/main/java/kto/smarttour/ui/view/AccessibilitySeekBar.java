package kto.smarttour.ui.view;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;

public class AccessibilitySeekBar extends AppCompatSeekBar {
	public AccessibilitySeekBar(@NonNull Context context) {
		super(context);
	}

	public AccessibilitySeekBar(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public AccessibilitySeekBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void sendAccessibilityEvent(int eventType) {

		if (eventType != AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION) {
			super.sendAccessibilityEvent(eventType);
		}
	}

	@Override
	public boolean performAccessibilityAction(int action, Bundle arguments) {
		switch (action) {
			case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
			case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD: {
				super.sendAccessibilityEvent(AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION);
			}
		}
		return super.performAccessibilityAction(action, arguments);
	}

	@Override
	public void setContentDescription(CharSequence contentDescription) {
		super.setContentDescription(contentDescription);
	}
}
