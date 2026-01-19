package kto.smarttour.ui.popup;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

//import kto.smarttour.BuildConfig;
import java.util.HashMap;

import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.ViewUtils;
import kto.smarttour.databinding.TutorialMainLayoutBinding;

public class AppTutorialActivity extends BaseActivity {

	private TutorialMainLayoutBinding mBind;

	private TutorialAdapter adapter;
	private int[] layouts = new int[]{R.layout.tutorial_layout_1, R.layout.tutorial_layout_2, R.layout.tutorial_layout_3, R.layout.tutorial_layout_4};
	private HashMap<String,String> mapLayoutContentDesc = new HashMap<>();
	private int positionCurrent = 0;
	private final String mSelectedColor = "#696CFF";
	private final String mUnselectedColor = "#C0C0C0";

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initTutorialDescriptionMap();

		mBind = DataBindingUtil.setContentView(this, R.layout.tutorial_main_layout);
		mBind.tutorialSkipBtn.setOnClickListener(v -> finish());
		mBind.btnTutorialPlay.setOnClickListener(v -> finish());
		mBind.btnTutorialPlay.setVisibility(View.GONE);


		adapter = new TutorialAdapter();
		mBind.viewPager.setAdapter(adapter);
		mBind.viewPager.setContentDescription("오디 사용자 가이드 4개의 페이지 중 1번째 페이지입니다. 다음 페이지로 이동하려면 스와이프 하세요.");

		//pager accsibility
		//mBind.viewPager.setAccessibilityDelegate(accessibilityDelegate);

		mBind.viewPager.setOnClickListener(mOnClickListener);
        mBind.tutorialNextPageBtn.setOnClickListener(mOnClickListener);
		mBind.tutorialControlLayout.setOnClickListener(mOnClickListener);
		mBind.tutorialControlLayout.setFocusable(true);
		mBind.tutorialControlLayout.setFocusableInTouchMode(true);
		mBind.tutorialControlLayout.setContentDescription("다음 페이지로 이동");

		//초기값 지정
		applyStateOnPageSelected(0,true);
	}

	private void initTutorialDescriptionMap(){

		for(int i=0; i<layouts.length; i++){

			String key = String.valueOf(i);
			switch (i) {
				case 0:
				{
					if(!mapLayoutContentDesc.containsKey( key )){

						//android:text="@string/tutorial_text_1"
						//android:text="@string/tutorial_text_1_2"
						//mBind.viewPager.setContentDescription("오디 사용자 가이드 4개의 페이지 중 1번째 페이지입니다. 다음 페이지로 이동하려면 스와이프 하세요.");

						StringBuilder sb = new StringBuilder();
						sb.append( getString(R.string.tutorial_text_1) );
						sb.append( getString(R.string.tutorial_text_1_2) );
						sb.append( "오디 사용자 가이드 4개의 페이지 중 1번째 페이지입니다. 다음 페이지로 이동하려면 스와이프 하세요." );
						mapLayoutContentDesc.put( key, sb.toString() );
					}
					break;
				}
				case 1:
				{
					if(!mapLayoutContentDesc.containsKey( key )){

						//android:text="@string/tutorial_text_2"
						//mBind.viewPager.setContentDescription("오디 사용자 가이드 4개의 페이지 중 2번째 페이지입니다. 다음 페이지로 이동하려면 스와이프 하세요.");

						StringBuilder sb = new StringBuilder();
						sb.append( getString(R.string.tutorial_text_2) );
						sb.append( "오디 사용자 가이드 4개의 페이지 중 2번째 페이지입니다. 다음 페이지로 이동하려면 스와이프 하세요." );
						mapLayoutContentDesc.put( key, sb.toString() );

					}


					break;
				}
				case 2:
				{
					if(!mapLayoutContentDesc.containsKey( key )){

						//android:text="@string/tutorial_text_3"
						//mBind.viewPager.setContentDescription("오디 사용자 가이드 4개의 페이지 중 3번째 페이지입니다. 다음 페이지로 이동하려면 스와이프 하세요.");

						StringBuilder sb = new StringBuilder();
						sb.append( getString(R.string.tutorial_text_3) );
						sb.append( "오디 사용자 가이드 4개의 페이지 중 3번째 페이지입니다. 다음 페이지로 이동하려면 스와이프 하세요." );
						mapLayoutContentDesc.put( key, sb.toString() );
					}

					break;
				}
				case 3:
				{
					if(!mapLayoutContentDesc.containsKey( key )){

						//android:text="@string/tutorial_text_4"
						//mBind.viewPager.setContentDescription("오디 사용자 가이드 4개의 페이지 중 4번째 페이지입니다. 다음 페이지로 이동하려면 스와이프 하세요.");

						StringBuilder sb = new StringBuilder();
						sb.append( getString(R.string.tutorial_text_4) );
						//sb.append( "오디 사용자 가이드 4개의 페이지 중 4번째 페이지입니다. 다음 페이지로 이동하려면 스와이프 하세요." );
						sb.append( "오디 사용자 가이드 4개의 페이지 중 4번째 페이지입니다." );
						sb.append( "여행 플레이 시작하기 실행하려면 두번 누르세요");

						mapLayoutContentDesc.put( key, sb.toString() );
					}

					break;
				}

				default:
				{
					break;
				}

			}

		}

	}

	View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			runOnUiThread(() -> {
				int totalCount = adapter.getCount();
				if (positionCurrent < totalCount) {

					if( positionCurrent == totalCount-1 ){
						finish();
					}else{
						mBind.viewPager.setCurrentItem(positionCurrent + 1, true);
					}

				}
				else {
					finish();
				}
			});
		}
	};

	private class TutorialAdapter extends PagerAdapter {


		@Override
		public int getCount() {
			return layouts.length;
		}

		@Override
		public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
			return view == object;
		}

		@NonNull
		@Override
		public Object instantiateItem(@NonNull ViewGroup container, int position) {
			View layout = LayoutInflater.from(AppTutorialActivity.this).inflate(layouts[position], container, false);
			container.addView(layout, 0);
			return layout;
		}

		@Override
		public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
			container.removeView((View) object);
		}
	}

	private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

		}

		@Override
		public void onPageSelected(int position) {
			applyStateOnPageSelected(position);
		}

		@Override
		public void onPageScrollStateChanged(int state) {



		}
	};

	private void applyStateOnPageSelected(int position){
		applyStateOnPageSelected(position, false);
	}
	private void applyStateOnPageSelected(int position, boolean onInit){

		mBind.btnTutorialPlay.setVisibility(View.GONE);
		mBind.tutorialControlLayout.setVisibility(View.VISIBLE);
        mBind.tutorialNextPageBtn.setText(
                getString(R.string.tutorial_next_btn, position + 1, layouts.length)
        );

		positionCurrent = position;
		//Log.d("TAG","onPageSelected 위치 = " + position);

		if(position==3){
			mBind.tutorialControlLayout.setFocusable(false);
			mBind.tutorialControlLayout.setFocusableInTouchMode(false);
		}else{
			mBind.tutorialControlLayout.setFocusable(true);
			mBind.tutorialControlLayout.setFocusableInTouchMode(true);
		}

		//==========
		String key = String.valueOf(position);
		//mBind.viewPager.setContentDescription("");
		if( mapLayoutContentDesc.containsKey( key )){
			mBind.viewPager.setContentDescription( mapLayoutContentDesc.get(key) );
		}
		mBind.viewPager.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
		mBind.tutorialControlLayout.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);

		//==========
		int maxCount = layouts.length;
		mBind.layoutIndicator.setContentDescription(""+maxCount+"페이지 중 "+ (position+1)+"번 페이지 입니다. 다음 페이지로 이동");

        //indicator control
        View[] viewPosArray = {mBind.viewPos1, mBind.viewPos2, mBind.viewPos3, mBind.viewPos4 };
        if(position == layouts.length - 1) {
            mBind.btnTutorialPlay.setVisibility(View.VISIBLE);
            mBind.tutorialControlLayout.setVisibility(View.GONE);
        } else {
            //dot 모양으로 변경
            for (int i = 0; i < viewPosArray.length; i++) {
                View view = viewPosArray[i];
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.OVAL);
                if(position == i) {
                    drawable.setColor(Color.parseColor(mSelectedColor));
                } else {
                    drawable.setColor(Color.parseColor(mUnselectedColor));
                }
                view.setBackground(drawable);
            }
        }
		//========
		if(!onInit){
			mBind.tutorialControlLayout.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
			mBind.viewPager.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
		}

		CharSequence desc = mBind.viewPager.getContentDescription();
		if(desc!=null){
			ViewUtils.announceForAccessibility(this, desc.toString() );
		}


	}

	@Override
	protected void onResume() {
			//RootBeer rootBeer = new RootBeer(this);
			if (CommonUtils.isRooted(this)) {
				DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.rooted_message), getString(R.string.finish), "", () -> {
					ActivityCompat.finishAffinity(this);
					System.exit(0);
				}, () -> {

				});
			} else if (CommonUtils.isEmulator()) {
				DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.emulator_message), getString(R.string.finish), "", () -> {
					ActivityCompat.finishAffinity(this);
					System.exit(0);
				}, () -> {

				});
			} else if (!CommonUtils.isKeyChecker(this, "MD5")) {
				DialogUtil.showWarning(this, getString(R.string.finish), getString(R.string.Integrity_message), getString(R.string.finish), "", () -> {
					ActivityCompat.finishAffinity(this);
					System.exit(0);
				}, () -> {

				});
			}
		super.onResume();
		mBind.viewPager.addOnPageChangeListener(onPageChangeListener);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mBind.viewPager.removeOnPageChangeListener(onPageChangeListener);
	}


}
