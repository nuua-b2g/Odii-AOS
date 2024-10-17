package kto.smarttour.common.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import kto.smarttour.R;


/**
 * Created by TedPark on 15. 11. 26..
 */
public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
	public static final int DIVIDER_TOP_MARGIN = 0;
	public static final int DIVIDER_END_MARGIN = 1;
	public static final int DIVIDER_TOP_END_MARGIN = 2;
	public static final int DIVIDER_TOP2_END_MARGIN = 3;
	private int statusBarHeight = 0;
	private int addHeight = 0;
	private Drawable mDivider;
	private int type;


	public SimpleDividerItemDecoration(Context context) {
		mDivider = context.getResources().getDrawable(R.drawable.line_divider);
		this.type = DIVIDER_END_MARGIN;
	}

	public SimpleDividerItemDecoration(Context context, int type) {
		mDivider = context.getResources().getDrawable(R.drawable.line_divider);
		this.type = type;

		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");

		if (resourceId > 0) {
			statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
		}

	}

	public SimpleDividerItemDecoration(Context context, int type, int height) {
		mDivider = context.getResources().getDrawable(R.drawable.line_divider);
		this.type = type;

		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");

		if (resourceId > 0) {
			statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
		}

		addHeight = height;
	}

	public SimpleDividerItemDecoration(Context context, int type, int height, int dividerResource) {
		mDivider = context.getResources().getDrawable(dividerResource);
		this.type = type;

		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");

		if (resourceId > 0) {
			statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
		}

		addHeight = height;
	}

	@Override
	public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
		int left = parent.getPaddingLeft();
		int right = parent.getWidth() - parent.getPaddingRight();

		int childCount = parent.getChildCount();
		for (int i = 0; i < childCount; i++) {
			View child = parent.getChildAt(i);

			RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

			int top = child.getBottom() + params.bottomMargin;
			int bottom = top + mDivider.getIntrinsicHeight();

			mDivider.setBounds(left, top, right, bottom);
			mDivider.draw(c);
		}
	}

	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);

		if (type == DIVIDER_TOP_MARGIN) {
			if (parent.getChildAdapterPosition(view) == 0) {

				if(addHeight == 0) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
						outRect.top = ViewUtils.dp2px(76) + statusBarHeight;
					} else {
						outRect.top = ViewUtils.dp2px(76);
					}
				} else {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
						outRect.top = addHeight + statusBarHeight;
					} else {
						outRect.top = addHeight;
					}
				}
			}
		} else if (type == DIVIDER_END_MARGIN) {
			if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
				outRect.bottom = ViewUtils.dp2px(72);
			}
		} else if (type == DIVIDER_TOP_END_MARGIN) {
			if (parent.getChildAdapterPosition(view) == 0) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

					if(addHeight > 0) {
						outRect.top = ViewUtils.dp2px(76) + statusBarHeight + ViewUtils.dp2px(addHeight);
					} else {
						outRect.top = ViewUtils.dp2px(76) + statusBarHeight;
					}


				} else {
					if(addHeight > 0) {
						outRect.top = ViewUtils.dp2px(76) + ViewUtils.dp2px(addHeight);
					} else {
						outRect.top = ViewUtils.dp2px(76);
					}

				}
			} else if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
				outRect.bottom = ViewUtils.dp2px(72);
			}
		} else if (type == DIVIDER_TOP2_END_MARGIN) {
			if (parent.getChildAdapterPosition(view) == 0) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

					if(addHeight > 0) {
						outRect.top = ViewUtils.dp2px(116) + statusBarHeight + ViewUtils.dp2px(addHeight);
					} else {
						outRect.top = ViewUtils.dp2px(116) + statusBarHeight;
					}


				} else {
					if(addHeight > 0) {
						outRect.top = ViewUtils.dp2px(116) + ViewUtils.dp2px(addHeight);
					} else {
						outRect.top = ViewUtils.dp2px(116);
					}

				}
			} else if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
				outRect.bottom = ViewUtils.dp2px(72);
			}
		}


	}
}