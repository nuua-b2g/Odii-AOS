package kto.smarttour.ui.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.StringPrepParseException;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import org.apache.commons.text.StringEscapeUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.databinding.PlayScenarioLayoutBinding;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.StoryDetailActivity;

public class PlayScenarioActivity extends BaseActivity {

	public static final String ACTION_SCENARIO_CHANGE = "ACTION_SHOW_MINI_PLAYER";

	private AppCompatActivity activity;
	private PlayScenarioLayoutBinding mBind;
	private ScenarioChangeReceiver receiver;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = this;
		mBind = DataBindingUtil.setContentView(activity, R.layout.play_scenario_layout);
		initView(getIntent().getIntExtra("position", -1));
		receiver = new ScenarioChangeReceiver();
		IntentFilter scenarioFilter = new IntentFilter();
		scenarioFilter.addAction(ACTION_SCENARIO_CHANGE);

		LocalBroadcastManager.getInstance(activity).registerReceiver(receiver, scenarioFilter);

		applyViewByLanguage();
	}

	private void applyViewByLanguage(){

		String locale = SettingsUtil.getLocale(this);
		//String locale_config = getResources().getConfiguration().locale.getLanguage();

		//폰트크게 T표현 숨김제어 (영어일때)
		mBind.tvTexticonT.setVisibility(View.VISIBLE);
		if(locale!=null){
			switch (locale) {
				case "ko":
					locale = "ko";
					break;
				case "zh":
				case "zh_CN":
				case "zh_TW":
					locale = "zh";
					break;
				case "tw":
					locale = "tw";
					break;
				case "ja":
					locale = "ja";
					break;
				default:
					locale = "en";
					break;
			}

			//==
			if(locale.equalsIgnoreCase("en")){
				mBind.tvTexticonT.setVisibility(View.GONE);
			}
		}


	}

	private void initView(int position) {

		if (position == -1) {
			finish();
			return;
		}

		StoryItem story = StoryDbManager.getInstance(activity).getPlayList().story.get(position);
		if (story == null) {
			finish();
			return;
		}

		mBind.tvTitle.setText(story.title);
		String audioScript = story.audioScript;

		// 1차 처리 - 예시 : "&lt;" 를 "<"로 치환
		// https://mvnrepository.com/artifact/org.apache.commons/commons-text
		// implementation group: 'org.apache.commons', name: 'commons-text', version: '1.9'
		String audioScriptUnEscapedHtml = StringEscapeUtils.unescapeHtml4(audioScript);
		//====
		// 2차 재처리 - 1차에서 보정하여 정상적인 html태그 문자열로 처리된 데이터를 기준으로 포멧처리(br개행 등)
		Spanned audioScriptSpanned2 = new SpannableString("");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			audioScriptSpanned2 = Html.fromHtml(audioScriptUnEscapedHtml.toString(), Html.FROM_HTML_MODE_LEGACY);
		} else {
			audioScriptSpanned2 = Html.fromHtml(audioScriptUnEscapedHtml.toString());
		}
		//====
		mBind.tvContent.setText( audioScriptSpanned2.toString() );

		mBind.btnClose.setOnClickListener(v -> finish());
		mBind.btnTextSizeChange.setOnClickListener(v -> {
			DialogUtil.showChoiceTextSize(activity, getString(R.string.text_size_change), getString(R.string.cancel), result -> {
				switch (result) {
					case 0:
						AnalyticsInterface.getInstance().logEvent(activity, "library_story_font_size_1x");
						mBind.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
						break;
					case 1:
						AnalyticsInterface.getInstance().logEvent(activity, "library_story_font_size_2x");
						mBind.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
						break;
					case 2:
						AnalyticsInterface.getInstance().logEvent(activity, "library_story_font_size_4x");
						mBind.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
						break;

				}
			});
		});
	}

	private class ScenarioChangeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (ACTION_SCENARIO_CHANGE.equals(intent.getAction())) {
				initView(intent.getIntExtra("position", -1));
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (receiver != null) {
			LocalBroadcastManager.getInstance(activity).unregisterReceiver(receiver);
		}
	}
}
