package kto.smarttour.ui.popup;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.scottyab.rootbeer.RootBeer;

import java.util.Calendar;

//import kto.smarttour.BuildConfig;
import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.PreferenceUtils;
import kto.smarttour.databinding.NoticeLayoutBinding;
import kto.smarttour.network.response.dao.DisasterNoticeList;
import kto.smarttour.network.response.dao.NormalNoticeList;

public class NoticeActivity extends BaseActivity {

	private NoticeLayoutBinding mBind;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBind = DataBindingUtil.setContentView(this, R.layout.notice_layout);
		mBind.setLifecycleOwner(this);
		overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
		mBind.btnClose.setOnClickListener(v -> finish());

		Bundle bundle = getIntent().getBundleExtra("bundle");

		if(getIntent().getBooleanExtra("disaster_notice", false)){
			DisasterNoticeList data = getIntent().getParcelableExtra("disaster_notice_data");

			if(!TextUtils.isEmpty(data.getImgUrl())) {
				mBind.ivNotice.setVisibility(View.VISIBLE);
				Glide.with(this).load(data.getImgUrl()).placeholder(R.drawable.ic_img_scrap_dummy).centerInside().into(mBind.ivNotice);
				mBind.ivNotice.setContentDescription(data.getImgAlt());
			} else {
				mBind.ivNotice.setVisibility(View.GONE);
			}

			if(!TextUtils.isEmpty(data.getContent())) {
				mBind.tvNotice.setVisibility(View.VISIBLE);
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					mBind.tvNotice.setText(Html.fromHtml(data.getContent(), Html.FROM_HTML_MODE_LEGACY));
				} else {
					mBind.tvNotice.setText(Html.fromHtml(data.getContent()));
				}
			} else {
				mBind.tvNotice.setVisibility(View.GONE);
			}
			mBind.btnCloseDay.setOnClickListener(v -> {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(System.currentTimeMillis());
				calendar.add(Calendar.DATE, 1);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				PreferenceUtils.addPreference(this, "notice.check", data.getNid() + ":" + calendar.getTimeInMillis());
				finish();
			});
		} else if(bundle != null && bundle.getBoolean("normal_notice", false)) {
//		} else if(getIntent().getBooleanExtra("normal_notice", false)) {
			NormalNoticeList data = bundle.getParcelable("normal_notice_data");
//			NormalNoticeList data = getIntent().getParcelableExtra("normal_notice_data");
			if(!TextUtils.isEmpty(data.getImgUrl())) {
				mBind.ivNotice.setVisibility(View.VISIBLE);
				Glide.with(this).load(data.getImgUrl()).placeholder(R.drawable.ic_img_scrap_dummy).centerInside().into(mBind.ivNotice);
				mBind.ivNotice.setContentDescription(data.getImgAlt());
			} else {
				mBind.ivNotice.setVisibility(View.GONE);
			}

			if(!TextUtils.isEmpty(data.getContent())) {
				mBind.tvNotice.setVisibility(View.VISIBLE);
//				mBind.tvNotice.setMovementMethod(new ScrollingMovementMethod());
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					mBind.tvNotice.setText(Html.fromHtml(data.getContent(), Html.FROM_HTML_MODE_LEGACY));
				} else {
					mBind.tvNotice.setText(Html.fromHtml(data.getContent()));
				}
			} else {
				mBind.tvNotice.setVisibility(View.GONE);
			}
			mBind.btnCloseDay.setOnClickListener(v -> {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(System.currentTimeMillis());
				calendar.add(Calendar.DATE, 1);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				PreferenceUtils.addPreference(this, "notice.check", data.getNid() + ":" + calendar.getTimeInMillis());
				finish();
			});
		} else {
			finish();
		}

	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.slide_down_reverse, R.anim.slide_up_reverse);
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
	}
}
