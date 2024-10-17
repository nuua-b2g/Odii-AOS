package kto.smarttour.ui.player;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.scottyab.rootbeer.RootBeer;
import com.socks.library.KLog;
import com.triggertrap.seekarc.SeekArc;

import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.URLS;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.BottomSheetDialogUtil;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.ImageUtil;
import kto.smarttour.common.utils.InnerStorageSingleton;
import kto.smarttour.common.utils.NetworkUtil;
import kto.smarttour.common.utils.OnSwipeTouchListener;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.StorageUtil;
import kto.smarttour.common.utils.SystemUtils;
import kto.smarttour.common.utils.ViewUtils;
import kto.smarttour.databinding.ActivityPlayerBinding;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.location.CurrentLocation;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.MainActivity;
import kto.smarttour.ui.PermissionCheckActivity;
import kto.smarttour.ui.StoryDetailWebActivity;
import kto.smarttour.ui.StoryLockerActivity;
import kto.smarttour.ui.StoryPlayerListActivity;

public class Player extends BaseActivity implements View.OnClickListener {

	private ActivityPlayerBinding mBind;
	private PlayerControllReceiver receiver;
	private IntentFilter playerIntentFilter;

	private List<StoryItem> playList = new ArrayList<>();
	private int playIndex = 0;

	private AppCompatActivity activity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = this;

		mBind = DataBindingUtil.setContentView(this, R.layout.activity_player);
		mBind.setLifecycleOwner(this);

		if (getResources().getDisplayMetrics().densityDpi <= DisplayMetrics.DENSITY_HIGH) {
			mBind.title.setMaxLines(1);
			mBind.title.setSingleLine(true);
			mBind.title.setEllipsize(TextUtils.TruncateAt.END);
			mBind.layoutTitle.setMinimumHeight(ViewUtils.dp2px(40));
			LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			ll.setMargins(ViewUtils.dp2px(20), 0, ViewUtils.dp2px(20), 0);
			mBind.layoutTitle.setLayoutParams(ll);
		}


		((LinearLayout.LayoutParams) mBind.topbar.getLayoutParams()).setMargins(0, statusBarHeight, 0, 0);

		receiver = new PlayerControllReceiver();

		mBind.btnPlayerPlay.setOnClickListener(this);
		mBind.btnPlayerPause.setOnClickListener(this);
		mBind.btnPlayerNext.setOnClickListener(this);
		mBind.btnPlayerPrev.setOnClickListener(this);
		mBind.btnPlayerShuffle.setOnClickListener(this);
		mBind.btnPlayerReplay.setOnClickListener(this);
		mBind.btnPlayer2x.setOnClickListener(this);
		mBind.btnPlayerMute.setOnClickListener(this);
		mBind.btnPlayerMore.setOnClickListener(this); //기능숨김 (xml GONE, 2022.11.09)
		mBind.btnPlayerList.setOnClickListener(this); //기능숨김 (xml GONE, 2022.11.09)

		mBind.btnClose.setOnClickListener(v -> {
			AnalyticsInterface.getInstance().logEvent(activity, "player_close");
			finish();
		});

		//---- 추가 5초 앞뒤 이동
		mBind.btnPlayerBackwardSec.setOnClickListener(this);
		mBind.btnPlayerForwardSec.setOnClickListener(this);
		//----------------------------------------------------------------------

		playerIntentFilter = new IntentFilter();
		playerIntentFilter.addAction(PlayerConstants.GUI_UPDATE_ACTION);
		playerIntentFilter.addAction(PlayerConstants.ACTION_PLAY_INFO);
		playerIntentFilter.addAction(PlayerConstants.ACTION_STOP);
		playerIntentFilter.addAction(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE);
		playerIntentFilter.addAction(PlayerConstants.ACTION_PLAYER_STATUS_PLAY);
		playerIntentFilter.addAction(PlayerConstants.ACTION_PLAY_COMPLETE);

		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, playerIntentFilter);
		if (!ViewUtils.isAccessibility(this)) {
			mBind.viewMain.setOnTouchListener(new OnSwipeTouchListener(this) {
				@Override
				public void onSwipeRight() {
					PlayerService.startActionNextSong(Player.this);
				}

				@Override
				public void onSwipeLeft() {
					PlayerService.startActionPreviousSong(Player.this);
				}
			});
		}

		initSeekArc2();
		checkPlayer();
		mBind.btnScenario.setOnClickListener(v -> {
			Intent tmpIntent = new Intent(activity, PlayScenarioActivity.class);
			tmpIntent.putExtra("position", PlayListManager.getInstance().getPlayIndex());
			startActivity(tmpIntent);
		});


		// 드라이드 모드 숨김
		mBind.btnDriver.setVisibility(View.GONE);
		/* 드라이브 모드 숨김 (미사용으로 변경)
		mBind.btnDriver.setOnClickListener(v -> {
			if (SystemUtils.checkGps(activity)) {
				movePermissionGps();
			} else {
				DialogUtil.showWarning(activity, "", activity.getString(R.string.setting_non_check_location), activity.getString(R.string.cancel), activity.getString(R.string.settings), () -> {
					Intent tmpIntent = new Intent(activity, DriverPlayer.class);
					tmpIntent.putExtra("position", PlayListManager.getInstance().getPlayIndex());
					startActivity(tmpIntent);
				}, () -> {
					moveConfigGPS();
				});
			}
		});
		*/


		//mBind.seekArc2.setVisibility(View.VISIBLE);

	}


	boolean isSeekTouch = false;

	/*
	private void initSeekArc() {
		mBind.seekArc.setClockwise(false);
		mBind.seekArc.setArcRotation(0);
		mBind.seekArc.setStartAngle(90);
		mBind.seekArc.setSweepAngle(180);
		mBind.seekArc.setArcWidth(ViewUtils.dp2px(9));
		mBind.seekArc.setProgressWidth(ViewUtils.dp2px(9));
		mBind.seekArc.setArcColor(Color.parseColor("#464646"));
		mBind.seekArc.setProgressColor(Color.parseColor("#F15959"));
		mBind.seekArc.setRoundedEdges(true);

		mBind.seekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
			@Override
			public void onProgressChanged(SeekArc seekArc, int i, boolean b) {

				//기존 seek타임 사용자 이동시, 목표시간이 제대로 출력되지않음.
				//if (isSeekTouch && PlayerService.isPlay) {
				//	mBind.tvTimerStart.setText(String.format("%02d : %02d", i / (60 * 1000) % 60, i / 1000 % 60));
				//}

				//수정 - 사용자 시크타임 이동시 (목표)현재시간 출력
				if (b && isSeekTouch && PlayerService.isPlay) {
					//b 전달값이 isSeekTouch 같다.
					StoryItem item = PlayListManager.getInstance().getStoryItem();
					double endTime = Double.parseDouble(item.audioPlayTime);
					//int progress = seekBar.getProgress();
					int progress = i;

					int userSeekingTime  = (int) (progress * endTime / 100);
					int timeUnit = 1; //1000 , 1 ( 오디오정보가 밀리세컨드가 아닌 단위(초)로 들어옴)
					String userSeekingTimeString = String.format("%02d:%02d", userSeekingTime /(60 * timeUnit) % 60, userSeekingTime / timeUnit % 60);
					//KLog.i("TEST_LOG", "seekMiniPlayer userSeekingTime >> " + userSeekingTimeString );
					mBind.tvTimerStart.setText(userSeekingTimeString);
				}

			}

			@Override
			public void onStartTrackingTouch(SeekArc seekArc) {
				isSeekTouch = true;
			}

			@Override
			public void onStopTrackingTouch(SeekArc seekArc) {
				isSeekTouch = false;
				if (PlayerService.isPlay) {
					StoryItem item = PlayListManager.getInstance().getStoryItem();
					double endTime = Double.parseDouble(item.audioPlayTime);
					int currentTime = seekArc.getProgress();
					PlayerService.startActionSeekTo(Player.this, (int) (currentTime * endTime / 100));
				}
			}
		});
	}
	 */

	private void initSeekArc2() {
		//버그수정용 초기값 지정
		mBind.seekArc2.setMax(100);
		mBind.seekArc2.setProgress(0);


		mBind.seekArc2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				/* 기존, seek지점 변화시 재생요청하는부분
				if (PlayerService.isPlay) {
					mBind.tvTimerStart2.setText(String.format("%02d : %02d", i / (60 * 1000) % 60, i / 1000 % 60));

					if (isAccessibility && b) {
						StoryItem item = PlayListManager.getInstance().getStoryItem();
						double endTime = Double.parseDouble(item.audioPlayTime);
						int currentTime = seekBar.getProgress();
						PlayerService.startActionSeekTo(Player.this, (int) (currentTime * endTime / 100));
					}
				} else {
					mBind.seekArc2.setProgress(0);
				}
				*/

				//실제 seek지점 재생요청은  onStopTrackingTouch에서 처리하도록 수정
				if (b && isSeekTouch && PlayerService.isPlay) {
					//b 전달값이 isSeekTouch 같다.
					StoryItem item = PlayListManager.getInstance().getStoryItem();
					double endTime = Double.parseDouble(item.audioPlayTime);
					//int progress = seekBar.getProgress();
					int progress = i;

					int userSeekingTime  = (int) (progress * endTime / 100);
					int timeUnit = 1; //1000 , 1 ( 오디오정보가 밀리세컨드가 아닌 단위(초)로 들어옴)
					String userSeekingTimeString = String.format("%02d:%02d", userSeekingTime /(60 * timeUnit) % 60, userSeekingTime / timeUnit % 60);
					//KLog.i("TEST_LOG", "seekMiniPlayer userSeekingTime >> " + userSeekingTimeString );
					mBind.tvTimerStart2.setText(userSeekingTimeString);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				//수정추가
				isSeekTouch = true;
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//수정추가
				isSeekTouch = false;
				if (PlayerService.isPlay) {
					StoryItem item = PlayListManager.getInstance().getStoryItem();
					double endTime = Double.parseDouble(item.audioPlayTime);
					int currentTime = mBind.seekArc2.getProgress();
					PlayerService.startActionSeekTo(Player.this, (int) (currentTime * endTime / 100));
				}

			}
		});
	}

	private void checkPlayer() {

		//기존 playIndex = PlayListManager.getInstance().getPlayIndex();
		//추가 : 대본스크립트 스크롤 이동 ( 오디오 인덱스가 변경되었을 때 )
		int currentIndex = PlayListManager.getInstance().getPlayIndex();
		if(playIndex!=currentIndex){
			playIndex = currentIndex;
			//대본스크롤 초기위치로 이동
			mBind.scrollTvContent.scrollTo(0,0);
		}

		if (playIndex == -1) {
			finish();
		}
		playList = StoryDbManager.getInstance(this).getPlayList().story;

		//오디오 타이틀
		mBind.title.setText(playList.get(playIndex).title);
		//오디오 대본스크립트
		//String audioScript = story.audioScript;
		String audioScript = playList.get(playIndex).audioScript;

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
		//텍스트 변경시 스크롤 이동( 다른타이밍에 호출필요)
		//mBind.scrollTvContent.scrollTo(0,0);

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
		applyViewByLanguage();//글자크기 T표기
		//=============================

		//ImageUtil.loadImage(mBind.ivThumb, playList.get(playIndex).thumbnailFilePath, null);

		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(PlayScenarioActivity.ACTION_SCENARIO_CHANGE).putExtra("position", playIndex));
		if (PlayerService.isPlay) {
			mBind.btnPlayerPlay.setVisibility(View.INVISIBLE);
			mBind.btnPlayerPause.setVisibility(View.VISIBLE);
		} else {
			mBind.btnPlayerPlay.setVisibility(View.VISIBLE);
			mBind.btnPlayerPause.setVisibility(View.INVISIBLE);
		}

		if (PlayerService.isMute) {
			mBind.btnPlayerMute.setContentDescription("음소거 설정됨");
			mBind.btnPlayerMute.setBackgroundResource(R.drawable.ic_bt_player_muteon_v2);
		} else {
			mBind.btnPlayerMute.setContentDescription("음소거 설정안됨");
			mBind.btnPlayerMute.setBackgroundResource(R.drawable.ic_bt_player_mute_v2);
		}

		/*
		switch (PlayerService.getPlaySpeed()) {
			case PlayerService.PLAY_SPEED_TYPE_1_25:
				mBind.btnPlayer2x.setBackgroundResource(R.drawable.ic_bt_player_125x_white_v2);
				mBind.btnPlayer2x.setContentDescription("1.25배속 재생속도 설정됨");
				break;
			case PlayerService.PLAY_SPEED_TYPE_1_5:
				mBind.btnPlayer2x.setBackgroundResource(R.drawable.ic_bt_player_15x_white_v2);
				mBind.btnPlayer2x.setContentDescription("1.5배속 재생속도 설정됨");
				break;
			default:
			case PlayerService.PLAY_SPEED_TYPE_1:
				mBind.btnPlayer2x.setBackgroundResource(R.drawable.ic_bt_player_1x_white_v2);
				mBind.btnPlayer2x.setContentDescription("1배속 재생속도 설정됨");
				break;
		}
		*/
		//배속설정 현상태
		mBind.btnPlayer2x.setBackgroundResource(PlayerService.getCurrentPlayRes());
		mBind.btnPlayer2x.setContentDescription(PlayerService.getCurrentPlayspeedContentDescriptionString());

		checkPlayType();
		checkRandomPlay();
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

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (receiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
			receiver = null;
		}
	}

	private void checkPlayType() {
		switch (PlayerService.getPlayType()) {
			case PlayerService.PLAY_TYPE_NONE:
				mBind.btnPlayerReplay.setBackgroundResource(R.drawable.ic_bt_player_replay_v2);
				mBind.btnPlayerReplay.setContentDescription("반복재생 설정안됨");
				break;
			case PlayerService.PLAY_TYPE_REPLAY_ONE:
				mBind.btnPlayerReplay.setBackgroundResource(R.drawable.ic_bt_player_replayon_v2);
				mBind.btnPlayerReplay.setContentDescription("현재 이야기만 반복 설정됨");
				break;
			case PlayerService.PLAY_TYPE_REPLAY:
				mBind.btnPlayerReplay.setBackgroundResource(R.drawable.ic_bt_player_replayonall_v2);
				mBind.btnPlayerReplay.setContentDescription("반복재생 설정됨");
				break;
		}
	}

	private void checkRandomPlay() {
		if (PlayerService.isRandomPlay) {
			mBind.btnPlayerShuffle.setContentDescription("무작위재생 설정됨");
		} else {
			mBind.btnPlayerShuffle.setContentDescription("무작위재생 설정안됨");
		}
	}


	@Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
			case R.id.btn_player_play:
				AnalyticsInterface.getInstance().logEvent(activity, "player_play");
				PlayerService.startActionPlay(this);
				break;
			case R.id.btn_player_pause:
				AnalyticsInterface.getInstance().logEvent(activity, "player_pause");
				PlayerService.startActionPause(this);
				break;
			case R.id.btn_player_next:
				AnalyticsInterface.getInstance().logEvent(activity, "player_next");
				PlayerService.startActionNextSong(this);
				break;
			case R.id.btn_player_prev:
				AnalyticsInterface.getInstance().logEvent(activity, "player_pre");
				PlayerService.startActionPreviousSong(this);
				break;
			case R.id.btn_player_2x:
				//---------------------------------------------------------------
				/*
				switch (PlayerService.getPlaySpeed()) {
					case PlayerService.PLAY_SPEED_TYPE_1_25:
						PlayerService.setPlaySpeed(PlayerService.PLAY_SPEED_TYPE_1_5);
						mBind.btnPlayer2x.setBackgroundResource(R.drawable.ic_bt_player_15x);
						PlayerService.startActionPlay2x(this, 1.5f);
						AnalyticsInterface.getInstance().logEvent(activity, "player_1.5x");
						mBind.btnPlayer2x.setContentDescription("1.5배속 재생속도 설정");
						break;
					case PlayerService.PLAY_SPEED_TYPE_1_5:
						PlayerService.setPlaySpeed(PlayerService.PLAY_SPEED_TYPE_1);
						mBind.btnPlayer2x.setBackgroundResource(R.drawable.ic_bt_player_1x);
						PlayerService.startActionPlay2x(this, 1.0f);
						AnalyticsInterface.getInstance().logEvent(activity, "player_1.0x");
						mBind.btnPlayer2x.setContentDescription("1배속 재생속도 설정");
						break;
					default:
					case PlayerService.PLAY_SPEED_TYPE_1:
						PlayerService.setPlaySpeed(PlayerService.PLAY_SPEED_TYPE_1_25);
						mBind.btnPlayer2x.setBackgroundResource(R.drawable.ic_bt_player_125x);
						PlayerService.startActionPlay2x(this, 1.25f);
						AnalyticsInterface.getInstance().logEvent(activity, "player_1.25x");
						mBind.btnPlayer2x.setContentDescription("1.25배속 재생속도 설정");
						break;
				}
				*/
				//다음 재생속도 이동
				PlayerService.getNextPlayspeed();
				//이동후 세팅값 적용 - 배속설정 현상태
				mBind.btnPlayer2x.setBackgroundResource(PlayerService.getCurrentPlayRes());
				PlayerService.startActionPlay2x(this, PlayerService.getCurrentPlayspeedValue());
				AnalyticsInterface.getInstance().logEvent(activity, PlayerService.getCurrentPlayspeedLogString());

				mBind.btnPlayer2x.setContentDescription(PlayerService.getCurrentPlayspeedContentDescriptionString());
				//---------------------------------------------------------------
				mBind.btnPlayer2x.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
				break;
			case R.id.btn_player_replay:
				switch (PlayerService.getPlayType()) {
					case PlayerService.PLAY_TYPE_NONE: // 기본 > 전체 반복
						mBind.btnPlayerReplay.setBackgroundResource(R.drawable.ic_bt_player_replayonall_v2);
						PlayerService.setPlayType(PlayerService.PLAY_TYPE_REPLAY);
						AnalyticsInterface.getInstance().logEvent(activity, "player_replay_on");
						mBind.btnPlayerReplay.setContentDescription("반복 설정됨");
						mBind.btnPlayerReplay.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
						break;
					case PlayerService.PLAY_TYPE_REPLAY: // 전체 반복 > 현재 이야기만 반복
						mBind.btnPlayerReplay.setBackgroundResource(R.drawable.ic_bt_player_replayon_v2);
						PlayerService.setPlayType(PlayerService.PLAY_TYPE_REPLAY_ONE);
						AnalyticsInterface.getInstance().logEvent(activity, "player_replay_one");
						mBind.btnPlayerReplay.setContentDescription("현재 이야기만 반복 설정됨");
						mBind.btnPlayerReplay.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
						break;
					case PlayerService.PLAY_TYPE_REPLAY_ONE: // 현재 이야기만 반복 > 기본
						mBind.btnPlayerReplay.setBackgroundResource(R.drawable.ic_bt_player_replay_v2);
						PlayerService.setPlayType(PlayerService.PLAY_TYPE_NONE);
						AnalyticsInterface.getInstance().logEvent(activity, "player_replay_off");
						mBind.btnPlayerReplay.setContentDescription("반복재생 설정안됨");
						mBind.btnPlayerReplay.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
						break;
				}
				PlayerService.isRandomPlay = false;
				checkRandomPlay();
				break;
			case R.id.btn_player_shuffle:
				if (!PlayerService.isRandomPlay) {
					PlayerService.setPlayType(PlayerService.PLAY_TYPE_NONE);
					PlayerService.startActionPlayRandom(this);
					checkPlayType();
					AnalyticsInterface.getInstance().logEvent(activity, "player_shuffle_on");
					mBind.btnPlayerShuffle.setBackgroundResource(R.drawable.ic_bt_player_shuffleon_v2);
					mBind.btnPlayerShuffle.setContentDescription("무작위재생 설정됨");
					mBind.btnPlayerShuffle.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
				} else {
					PlayerService.startActionPlayRandom(this);
					AnalyticsInterface.getInstance().logEvent(activity, "player_shuffle_off");
					mBind.btnPlayerShuffle.setBackgroundResource(R.drawable.ic_bt_player_shuffle_v2);
					mBind.btnPlayerShuffle.setContentDescription("무작위재생 설정안됨");
					mBind.btnPlayerShuffle.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
				}
				break;
			case R.id.btn_player_mute:
				PlayerService.startActionMute(this);
				if (PlayerService.isMute) {
					AnalyticsInterface.getInstance().logEvent(activity, "player_mute_off");
					mBind.btnPlayerMute.setBackgroundResource(R.drawable.ic_bt_player_muteon_v2);
					mBind.btnPlayerMute.setContentDescription("음소거 설정안됨");
					mBind.btnPlayerMute.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
				} else {
					AnalyticsInterface.getInstance().logEvent(activity, "player_mute_on");
					mBind.btnPlayerMute.setBackgroundResource(R.drawable.ic_bt_player_mute_v2);
					mBind.btnPlayerMute.setContentDescription("음소거 설정됨");
					mBind.btnPlayerMute.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
				}
				break;
			case R.id.btn_player_list:
				AnalyticsInterface.getInstance().logEvent(activity, "player_playlist");
				intent = new Intent(Player.this, StoryPlayerListActivity.class);
				//추가
				//intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivityForResult(intent, 6487);
				break;
			case R.id.btn_player_more:
				AnalyticsInterface.getInstance().logEvent(activity, "player_menu");
				BottomSheetDialogUtil.showPlayerMenuDialog(Player.this, R.style.TransparentDialog, result -> {
					switch (result) {
						case BottomSheetDialogUtil.TOAST_MENU_PLAYER_DETAIL:
							AnalyticsInterface.getInstance().logEvent(activity, "player_menu_detail");

							StoryItem storyItemSearch = PlayListManager.getInstance().getStoryItem();

							/*
							Location location = new CurrentLocation(Player.this, null).getLastKnownLocation();

							String url;
							if (location == null) {
								url = URLS.DETAIL_URL + String.format("?tid=%s&tlid=%s&sid=%s&slid=%s&latitude=%s&longitude=%s", storyItemSearch.tid, storyItemSearch.tlid, storyItemSearch.sid, storyItemSearch.slid, CurrentLocation.DEFAULT_LAT, CurrentLocation.DEFAULT_LNG);
							} else {
								url = URLS.DETAIL_URL + String.format("?tid=%s&tlid=%s&sid=%s&slid=%s&latitude=%s&longitude=%s", storyItemSearch.tid, storyItemSearch.tlid, storyItemSearch.sid, storyItemSearch.slid, location.getLatitude(), location.getLongitude());
							}
							Intent storyDetailWebIntent = new Intent(activity, MainActivity.class);
							storyDetailWebIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
							storyDetailWebIntent.putExtra("url", url);
							startActivity(storyDetailWebIntent);
							finish();
							*/

							//상세 웹 엑티비티 호출로 변경
							StoryItem content = storyItemSearch;
							Intent contentIntent = new Intent(this, StoryDetailWebActivity.class);
							contentIntent.putExtra("content", content);
							contentIntent.putExtra("useLocation", true);
							//추가
							//contentIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							startActivity(contentIntent);

							break;
						case BottomSheetDialogUtil.TOAST_MENU_PLAYER_LOCKER:

							AnalyticsInterface.getInstance().logEvent(activity, "player_menu_fill");
							StoryItem storyItemLocker = PlayListManager.getInstance().getStoryItem();
							StoryData data = new StoryData();
							data.story.add(storyItemLocker);
							Intent lockerIntent = new Intent(Player.this, StoryLockerActivity.class);
							lockerIntent.putExtra("type", StoryLockerActivity.FOLDER_SELECT_TYPE);
							InnerStorageSingleton.getSingleton().setData(data);
							startActivity(lockerIntent);
							break;
						case BottomSheetDialogUtil.TOAST_MENU_PLAYER_DOWNLOAD:
							AnalyticsInterface.getInstance().logEvent(activity, "player_menu_download");
							if (!NetworkUtil.isNetworkConnected(this)) { // 인터넷 미접속
								DialogUtil.showWarning(this, R.string.ok, R.string.network_error, R.string.ok);
							} else if (NetworkUtil.isWifiConnected(this) && NetworkUtil.isNetworkConnected(this)) { // 와이파이 접속
								getDownlaod();
							} else if (SettingsUtil.isUseDataNetwork(this) && !NetworkUtil.isWifiConnected(this)) { //데이터 사용O 와이파이 접속X
								DialogUtil.showWarning(this, R.string.ok, R.string.network_use_message, R.string.ok, R.string.cancel, () -> {
									//									SettingsUtil.setUseDataNetwork(this, true);
									getDownlaod();
								}, () -> {

								});
							} else if (!SettingsUtil.isUseDataNetwork(this) && !NetworkUtil.isWifiConnected(this)) { //데이터 사용X 와이파이 접속X
								DialogUtil.showWarning(this, R.string.ok, R.string.network_use_message2, R.string.ok, R.string.cancel, () -> {
									//									SettingsUtil.setUseDataNetwork(this, true);
									getDownlaod();
								}, () -> {

								});
							}

							break;
					}
				});
				break;
			case R.id.btn_player_backward_sec:
				actionBtnMoveSeekTimeAmount(-5);
				break;
			case R.id.btn_player_forward_sec:
				actionBtnMoveSeekTimeAmount(5);
				break;
		}
	}

	//추가 n초 전후 이동처리 (현재 재생위치 등 고려)
	private void actionBtnMoveSeekTimeAmount(int moveTimeSecond){
		isSeekTouch = true;

		//===== 시크시작
		if (isSeekTouch && PlayerService.isPlay) {
			//현재 재생시간을 시크바의 위치값으로 가져오자
			//오디오 서비스에서 참조할때는 참조변수를 별도로 빼야한다.

			//현재 시간정보
			//b 전달값이 isSeekTouch 같다.
			StoryItem item = PlayListManager.getInstance().getStoryItem();

			//플레이어 정보
			double endTime = Double.parseDouble(item.audioPlayTime); //미디어 전채시간(초)
			int maxTime = (int)endTime;

			//int progress = seekBar.getProgress();
			//int progress = seekMiniPlayer.getProgress(); //접근성 플래그기준 접근 seekbar결정 필요


			int progress = mBind.seekArc2.getProgress(); // 0 to 100

			int currentTime  = (int) (progress * endTime / 100); //seek바 기준, 현재시간(초)
			int userSeekingTime  = currentTime;

			// 시간값 shift
			userSeekingTime += moveTimeSecond;
			// --오디오 재생범위 넘어가지 않도록 처리
			if( userSeekingTime < 0){
				userSeekingTime = 0;
			}
			if( userSeekingTime > maxTime){
				userSeekingTime = maxTime;
			}

			// seek 결과사용
			//(int) (currentTime * endTime / 100)
			PlayerService.startActionSeekTo(Player.this, userSeekingTime);// 시간(초) 전달
			//--

		}
		isSeekTouch = false;
	}

	private void getDownlaod() {
		StoryData downloadData = new StoryData();
		StoryItem storyItem = PlayListManager.getInstance().getStoryItem();
		downloadData.story.add(storyItem);
		//500000000 > 대략 500M
		if ((StorageUtil.GetAvailableInternalMemorySize() - storyItem.audioFileSize) >= 500000000) {
			DialogUtil.showDownLoad(Player.this, R.string.download, R.string.download_message, downloadData, R.string.cancel, () -> {
				((MainActivity) OdiiApplication.getWebActivity()).refreshDownloadCount();
			});
		} else {
			DialogUtil.showWarning(OdiiApplication.getWebActivity(), R.string.ok, R.string.storage_limit_error, R.string.ok);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 6487 && resultCode == Activity.RESULT_OK) {
			finish();
		} else if (requestCode == 4600) {
			Intent tmpIntent = new Intent(activity, DriverPlayer.class);
			tmpIntent.putExtra("position", PlayListManager.getInstance().getPlayIndex());
			startActivity(tmpIntent);
		} else if (requestCode == 4444) {
			if (SystemUtils.checkGps(activity)) {
				movePermissionGps();
			}
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
		AnalyticsInterface.getInstance().screenView(this, "player");
		PlayerService.setDriveMode(false);
		PlayerService.setGpsPlayComplete(true);
	}

	private class PlayerControllReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getAction()) {
				case PlayerConstants.GUI_UPDATE_ACTION:

					if (isSeekTouch) {
						return;
					}
					int t1 = intent.getIntExtra(PlayerConstants.ACTUAL_TIME_VALUE_EXTRA, 0);
					int t2 = intent.getIntExtra(PlayerConstants.TOTAL_TIME_VALUE_EXTRA, 0);
					float seekArcTime = ((float) t1 / (float) t2) * 100.0f;

					//--
					mBind.seekArc2.setProgress((int) seekArcTime);
					mBind.seekArc2.invalidate();

					mBind.tvTimerStart2.setText(String.format("%02d:%02d", t1 / (60 * 1000) % 60, t1 / 1000 % 60));
					mBind.tvTimerStart2.setContentDescription(String.format("현재 이야기 재생시간 %02d분%02d초", t1 / (60 * 1000) % 60, t1 / 1000 % 60));
					mBind.tvTimerEnd2.setText(String.format("%02d:%02d", t2 / (60 * 1000) % 60, t2 / 1000 % 60));
					mBind.tvTimerEnd2.setContentDescription(String.format("이야기 전체재생시간 %02d분%02d초", t2 / (60 * 1000) % 60, t2 / 1000 % 60));

					mBind.seekArc2.setContentDescription(String.format("%02d분%02d초 %02d분%02d초 불륨 키로 사용하여 조정하세요.", t1 / (60 * 1000) % 60, t1 / 1000 % 60, t2 / (60 * 1000) % 60, t2 / 1000 % 60));
					//--
					/*
					mBind.seekArc.setProgress((int) seekArcTime);
						mBind.tvTimerStart.setText(String.format("%02d:%02d", t1 / (60 * 1000) % 60, t1 / 1000 % 60));
						mBind.tvTimerStart.setContentDescription(String.format("현재 이야기 재생시간 %02d분%02d초", t1 / (60 * 1000) % 60, t1 / 1000 % 60));
						mBind.tvTimerEnd.setText(String.format("%02d:%02d", t2 / (60 * 1000) % 60, t2 / 1000 % 60));
						mBind.tvTimerEnd.setContentDescription(String.format("이야기 전체재생시간 %02d분%02d초", t2 / (60 * 1000) % 60, t2 / 1000 % 60));
					 */
					break;
				case PlayerConstants.ACTION_PLAY_COMPLETE:
					checkPlayer();

					mBind.seekArc2.setEnabled(false);
					/*
					mBind.seekArc.setEnabled(false);
					 */
					break;
				case PlayerConstants.ACTION_PLAYER_STATUS_PAUSE:
				case PlayerConstants.ACTION_PLAYER_STATUS_PLAY:
				case PlayerConstants.ACTION_PLAY_INFO:
					mBind.seekArc2.setEnabled(true);
					/*
					mBind.seekArc.setEnabled(true);
					 */

					PlayerService.isFloating = false;
					try {
						checkPlayer();
					} catch (Exception  e) {
						try {
							checkPlayer();
						} catch (Exception  e1) {
						}
					}
					break;
				case PlayerConstants.ACTION_STOP:
					finish();
					break;
			}
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	private void moveConfigGPS() {
		Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivityForResult(gpsOptionsIntent, 4444);
	}

	private void movePermissionGps() {
		ArrayList<String> resPermission = SystemUtils.checkSelfPermission(this, SystemUtils.PERMISSION_REQUEST_GPS);
		SettingsUtil.setCheckPermission(this, true);
		if (resPermission.size() == 0) {
			Intent tmpIntent = new Intent(activity, DriverPlayer.class);
			tmpIntent.putExtra("position", PlayListManager.getInstance().getPlayIndex());
			startActivity(tmpIntent);
		} else {
			Intent requestPermissionIntent = new Intent(this, PermissionCheckActivity.class);
			requestPermissionIntent.putExtra("drive", true);
			startActivityForResult(requestPermissionIntent, 4600);
		}
	}
}
