package kto.smarttour.ui.popup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.scottyab.rootbeer.RootBeer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

//import kto.smarttour.BuildConfig;
import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.PreferenceUtils;
import kto.smarttour.common.utils.ViewUtils;
import kto.smarttour.databinding.EventMainLayoutBinding;
import kto.smarttour.network.response.dao.EventList;

public class EventActivity extends BaseActivity {

	private EventMainLayoutBinding mBind;
	private ArrayList<EventList> eventList;
	private EventAdapter adapter;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBind = DataBindingUtil.setContentView(this, R.layout.event_main_layout);
		overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
		mBind.btnClose.setOnClickListener(v -> finish());

		eventList = getIntent().getParcelableArrayListExtra("event_data");

		if(eventList == null || eventList.isEmpty()){
			finish();
		}

		adapter = new EventAdapter();
		mBind.viewPager.setAdapter(adapter);
		initPositionView(0);

		mBind.btnCloseDay.setOnClickListener(v -> {
			for(EventList item : eventList) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(System.currentTimeMillis());
				calendar.add(Calendar.DATE, 1);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				PreferenceUtils.addPreference(this, "notice.check", item.getNid() + ":" + calendar.getTimeInMillis());
				finish();
			}
		});

		mBind.viewPager.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		if(eventList.size() != 1) {
			mBind.viewPager.setContentDescription(String.format("이벤트 %s개의 페이지 중 %s번째 페이지입니다. 다음 페이지로 이동하려면 두손가락으로 스와이프 하세요.", eventList.size(), 1));
		}


	}

	private void initPositionView(int posistion) {
		mBind.layoutPos.removeAllViews();

		if(eventList.size() <= 1) {
			return;
		}
		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(ViewUtils.dp2px(16), ViewUtils.dp2px(2));
		ll.setMargins(0, 0, ViewUtils.dp2px(4), 0);
		View positionView;
		for (int i = 0; i < eventList.size(); i++) {
			positionView = View.inflate(this, R.layout.position_view, null);
			if(i == posistion){
				positionView.setBackgroundColor(Color.parseColor("#FFFFFF"));
			} else {
				positionView.setBackgroundColor(Color.parseColor("#80FFFFFF"));
			}
			mBind.layoutPos.addView(positionView, ll);
		}
		mBind.layoutPos.invalidate();

	}

	private Map<Integer, ImageView> registeredImageView = new HashMap<>();


	private class EventAdapter extends PagerAdapter {


		@Override
		public int getCount() {
			return eventList.size();
		}

		@Override
		public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
			return view == object;
		}

		@NonNull
		@Override
		public Object instantiateItem(@NonNull ViewGroup container, int position) {
			View layout = LayoutInflater.from(EventActivity.this).inflate(R.layout.event_layout, container, false);
			ImageView iv = layout.findViewById(R.id.iv_event);
			Glide.with(EventActivity.this).load(eventList.get(position).getImgUrl()).placeholder(R.drawable.ic_img_scrap_dummy).fitCenter().into(iv);
			registeredImageView.put(position, iv);
			if(eventList.size() == 1) {
				iv.setContentDescription(eventList.get(position).getImgAlt() + "참여하기 두번 탭해주세요.");
			} else {
				iv.setContentDescription(String.format("%s개의 이벤트 중 %s번째 이벤트입니다. %s 참여하기 두번 탭해주세요.다음 이벤트로 이동하려면 두손가락으로 스와이프 하세요.", eventList.size(), position + 1, eventList.get(position).getImgAlt()));
			}

			container.addView(layout, 0);

			iv.setOnClickListener(v -> {
				Intent result = new Intent();
				result.putExtra("Event_URL", eventList.get(position).getLinkUrl());
				setResult(Activity.RESULT_OK, result);
				finish();
			});
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
			initPositionView(position);
			registeredImageView.get(position).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
		}

		@Override
		public void onPageScrollStateChanged(int state) {

		}


	};

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

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.slide_down_reverse, R.anim.slide_up_reverse);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		registeredImageView = null;
	}
}
