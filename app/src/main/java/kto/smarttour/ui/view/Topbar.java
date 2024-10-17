package kto.smarttour.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import kto.smarttour.R;

public class Topbar extends RelativeLayout implements View.OnClickListener {

	public static final int TOPBAR_MODE_NORMAL = 0;
	public static final int TOPBAR_MODE_LEFT = 1;
	public static final int TOPBAR_MODE_RIGHT = 2;
	public static final int TOPBAR_MODE_CLOSE = 3;
	public static final int TOPBAR_MODE_RIGHT_CLOSE = 4;


	private AppCompatImageView btnLeft, btnRight;
	private AppCompatTextView title;

	public Topbar(Context context) {
		super(context);
		init(context);
	}

	public Topbar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public Topbar(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.topbar, this);
		setClickable(true);
	}

	public void setTitle(String title) {
		this.title.setText(title);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		btnLeft = findViewById(R.id.btn_left);
		btnRight = findViewById(R.id.btn_right);
		title = findViewById(R.id.title);

		btnLeft.setOnClickListener(this);
		btnRight.setOnClickListener(this);
	}

	public void setRightBackground(int resourceId) {
		btnRight.setBackgroundResource(resourceId);
	}

	public void setLeftBackGround(int resourceId) {
		btnLeft.setBackgroundResource(resourceId);
	}

	public void setTopbarMode(int mode) {
		switch (mode) {
			case TOPBAR_MODE_NORMAL:
				btnRight.setVisibility(View.VISIBLE);
				btnLeft.setVisibility(View.VISIBLE);
				title.setVisibility(View.VISIBLE);
				break;
			case TOPBAR_MODE_LEFT:
				btnRight.setVisibility(View.INVISIBLE);
				btnLeft.setVisibility(View.VISIBLE);
				title.setVisibility(View.VISIBLE);
				break;
			case TOPBAR_MODE_RIGHT:
				btnRight.setVisibility(View.VISIBLE);
				btnLeft.setVisibility(View.INVISIBLE);
				title.setVisibility(View.VISIBLE);
				break;
			case TOPBAR_MODE_CLOSE:
				btnRight.setVisibility(View.INVISIBLE);
				btnLeft.setVisibility(View.VISIBLE);
				title.setVisibility(View.INVISIBLE);
				break;
			case TOPBAR_MODE_RIGHT_CLOSE:
				btnRight.setVisibility(View.VISIBLE);
				btnLeft.setVisibility(View.INVISIBLE);
				title.setVisibility(View.INVISIBLE);
				break;
		}
	}

	private OnTopBarListener callback;

	public interface OnTopBarListener {
		void onTopbarClickListener(View v);
	}

	public void setOnTopBarListener(OnTopBarListener callback) {
		this.callback = callback;
	}

	@Override
	public void onClick(View v) {
		if (callback != null) {
			callback.onTopbarClickListener(v);
		}
	}
}
