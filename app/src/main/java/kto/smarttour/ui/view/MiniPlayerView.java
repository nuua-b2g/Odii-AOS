package kto.smarttour.ui.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.socks.library.KLog;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.ImageUtil;
import kto.smarttour.common.utils.PreferenceUtils;
import kto.smarttour.common.utils.ViewUtils;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.service.PlayerConstants;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.MainActivity;
import kto.smarttour.ui.StoryPlayerListActivity;
import kto.smarttour.ui.player.PlayListManager;
import kto.smarttour.ui.player.Player;

public class MiniPlayerView extends LinearLayout implements View.OnClickListener {

	private Context context;

	private View viewSelf;

	//private AppCompatTextView title, subTitle, tvEmpty, tvToastMsg;
	private AppCompatTextView title, tvEmpty, tvToastMsg;
	//private CircleImageView ivThumb, ivThumb2;
	private CircleImageView ivThumb2;
	private AppCompatImageView btnPlay, btnPause, btnNext, btnPlayList, btnMiniPlayerClose;
	private AppCompatImageView btnOcToggle;//잠시 영역줄이기
	//private boolean b_OcToogle_Closed;
	private boolean b_OnAnimation_OcToogle;
	//추가
	private AppCompatImageView btnPrev;
	private AppCompatImageView btnBackwardSec,btnForwardSec;//임시 객체타입 및 연결 (n초 시크탕밈 이동)

	//=== 재생목록 엑티비티에서 미니플레이어 사용시 제어
	boolean isStoryPlayerListActivity = false;
	public boolean isStoryPlayerListActivity(){
		return this.isStoryPlayerListActivity;
	}

	//====> 앱 최초구동시 (메인화면)에서 재생목록이 있을떼 플레이어가 보임 -> 보이지않도록 처리
	private boolean isOnceHideEvenIfPlayListExist;
	public void setIsOnceHideEvenIfPlayListExist(boolean flag){
		this.isOnceHideEvenIfPlayListExist = flag;
	}
	public boolean getIsOnceHideEvenIfPlayListExist(){
		return this.isOnceHideEvenIfPlayListExist;
	}

	//====> (메인화면) initSetVisibilityGone 해체후, 마지막 보이기 상태값
	private int lastVisibility_bySetPlayerMode = View.GONE;
	public int getLastVisibility_bySetPlayerMode(){
		return this.lastVisibility_bySetPlayerMode;
	}

	//배속버튼
	private AppCompatImageView btnSpeed;
	//시크바
	private AppCompatSeekBar seekMiniPlayer;
	private boolean isSeekTouch = false;
	//시크바 thumb만을 이용하여 값 변경하기 mIsDragging
	private boolean mIsDragging;

	private AppCompatTextView tvTimerStart,tvTimerEnd;

	private RelativeLayout layoutMiniPlayer, layoutFloat, layoutToast;

	private View titleView;

	public MiniPlayerView(Context context) {
		super(context);
		initView(context);
	}

	public MiniPlayerView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	public MiniPlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView(context);
	}

	private void initView(Context context) {
		this.context = context;
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.mini_player_layout, this);

		if(context!=null && context instanceof StoryPlayerListActivity){
			isStoryPlayerListActivity = true;
		}
	}

	/**
	 * 1: mini player
	 * 2: float buttom
	 * 3: toast msg
	 */
	public void setPlayerMode(int mode) {

		setVisibility(View.VISIBLE);

		switch (mode) {
			case 1:
				layoutMiniPlayer.setVisibility(View.VISIBLE);
				//OcToggle 상태유지
				/* 내리기 토글 미사용
				layoutMiniPlayer.setVisibility( PlayerService.isOcToogleClosed? View.GONE:View.VISIBLE );
				*/

				//추가
				btnOcToggle.setVisibility(View.VISIBLE);

				layoutFloat.setVisibility(View.GONE);
				layoutToast.setVisibility(View.GONE);
				break;
			case 2:
				// 기존 모드2 (플롯팅 버튼 활성화)
				/*
				PlayerService.isFloating = true;
				layoutMiniPlayer.setVisibility(View.GONE);
				layoutFloat.setVisibility(View.VISIBLE);
				layoutToast.setVisibility(View.GONE);
				btnMiniPlayerClose.setVisibility(View.GONE);
				*/

				// 모드2 (setPlayerMode true) 일때, 변화가 발생하면 모드1로 토글시킨다.
				// 결론 처음부터 모드2를 모드1로 동작시켜버리자.
				// 모드 1 코드 수행

				layoutMiniPlayer.setVisibility(View.VISIBLE);
				//OcToggle 상태유지
				/* 내리기 토글 미사용
				layoutMiniPlayer.setVisibility( PlayerService.isOcToogleClosed? View.GONE:View.VISIBLE );
				*/

				//추가
				btnOcToggle.setVisibility(View.VISIBLE);

				layoutFloat.setVisibility(View.GONE);
				layoutToast.setVisibility(View.GONE);
				// 모드1 로 동작시키고 isFloating로 무조건 상태반영.
				PlayerService.isFloating = false;

				break;
			case 3:
				layoutToast.setVisibility(View.VISIBLE);

				if(layoutToast.getVisibility() == View.VISIBLE) {
					btnMiniPlayerClose.setVisibility(View.GONE);
				} else if(layoutMiniPlayer.getVisibility() == View.VISIBLE && PlayerService.isPlay) {
					btnMiniPlayerClose.setVisibility(View.GONE);
				}
				break;
			case 4:
				PlayerService.isFloating = false;
				PlayerService.userPause = false;
				layoutMiniPlayer.setVisibility(View.GONE);
				layoutFloat.setVisibility(View.GONE);
				layoutToast.setVisibility(View.GONE);
				btnMiniPlayerClose.setVisibility(View.GONE);

				//추가
				PlayerService.isOcToogleClosed = false; //다음에 보이게될때 일반상태로 보일수 있도록
				btnOcToggle.setVisibility(View.GONE);


				setVisibility(View.GONE);
				break;
		}

		//----

		//공통호출 - setCloseVisibility호 내부, 상시 닫기버튼 보이기 고정처리됨
		setCloseVisibility(View.VISIBLE); //상위라인 공통 호출중..(내부 VISIBLE로 고정 처리함)

		//추가 (재생목록 엑티비티)등에서 지정한 플래그가 있을경우
		if(isStoryPlayerListActivity){
			//setCloseVisibility(View.GONE); //상위라인 공통 호출중.

			if(mode == 4){
				//플레이어중단 맟 안보이도록
				setVisibility(View.GONE);
			}else{
				setVisibility(View.VISIBLE);
				//추가
				/* 내리기 토글 미사용
				layoutMiniPlayer.setVisibility( PlayerService.isOcToogleClosed? View.GONE:View.VISIBLE );
				*/
			}
		}else{
			//재생목록 상시보이기에서 버튼제어중 alwaysGone_btnMiniPlayerClose == true
			//일반 메인용 -> 최초숨김상태 인경우
			if(isOnceHideEvenIfPlayListExist){
				if(getVisibility()!=View.GONE){
					setVisibility(View.GONE);
				}
			}else{
				//일반 메인용 -> 최초 숨김상태가 해제된 이후,
				//setPlayerMode에서 발생한 마지막 상태복원
				//=====================
				lastVisibility_bySetPlayerMode = getVisibility();

				// ocToogle상태에 따라 결정하도록 수정
				// 내리기 토글 미사용
				//lastVisibility_bySetPlayerMode = PlayerService.isOcToogleClosed?View.GONE:View.VISIBLE;

				// baseActivity 라이프 사이클에서  instanceOf로 미니플레이뷰 숨김제어
				if(!isStoryPlayerListActivity ){
					//플레이어가 kill( 오디오 플레이어 종료) 또는 재생중이 아닐때
					if(PlayerService.player==null)
					{
						lastVisibility_bySetPlayerMode = View.GONE;
					}

				}

				if(mode == 4){
					//플레이어중단 맟 안보이도록
					setVisibility(View.GONE);
				}else{
					setVisibility(lastVisibility_bySetPlayerMode);

					// self전체를 치우지않고 >>>>>>>>>>> layoutMiniPlayer 영역만 제어해보자.
					/* 내리기 토글 미사용
					setVisibility(View.VISIBLE);//기본적으로 보이고
					layoutMiniPlayer.setVisibility(lastVisibility_bySetPlayerMode); //필요한 부분만 제어
					*/
					//=====================
				}

			}
		}

		//갱신
		/* 내리기 토글 미사용
		requestLayoutPlayer();
		*/
	}


	/**
	 추가- 스탬프 모드 재생UI on off
	 ( 다음 , 목록 )
	 */
	public void setPlayerStampUI(boolean moseStamp){
		//btnNext, btnPlayList
		if(moseStamp){
			if(btnNext.getVisibility()!=View.GONE){
				btnNext.setVisibility(View.GONE);
			}
			if(btnPlayList.getVisibility()!=View.GONE){
				btnPlayList.setVisibility(View.GONE);
			}
		}else{

			//스탬프모드가 아닌 (구 일반, 다음오디오 재생기능 ) -> 배속버튼으로 대체됨
			/*
			if(btnNext.getVisibility()!=View.VISIBLE){
				btnNext.setVisibility(View.VISIBLE);
			}
			*/

			if(btnPlayList.getVisibility()!=View.VISIBLE){
				btnPlayList.setVisibility(View.VISIBLE);
			}

		}
	}


	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		viewSelf = findViewById(R.id.view_mini_player_self);

		layoutMiniPlayer = findViewById(R.id.layout_mini_player);
		layoutFloat = findViewById(R.id.layout_float);
		layoutToast = findViewById(R.id.toast_view);

		title = findViewById(R.id.mini_player_title);
		//subTitle = findViewById(R.id.mini_player_subtitle); //서브타이틀 UI숨김
		//--

		tvEmpty = findViewById(R.id.view_empty_play);
		tvToastMsg = findViewById(R.id.tv_toast_msg);

		//ivThumb = findViewById(R.id.player_thumb); //섬네일 UI숨김
		ivThumb2 = findViewById(R.id.player_thumb2);

		btnPlay = findViewById(R.id.btn_mini_player_play);
		btnPause = findViewById(R.id.btn_mini_player_pause);

		btnNext = findViewById(R.id.btn_mini_player_next);
		//추가 btnPrev
		btnPrev = findViewById(R.id.btn_mini_player_prev);
		//추가 btnOcToggle
		btnOcToggle = findViewById(R.id.btn_oc_toggle);

		//btnNext위치의 대체기능
		btnSpeed = findViewById(R.id.btn_mini_player_speed);

		btnPlayList = findViewById(R.id.btn_mini_player_list);

		btnMiniPlayerClose = findViewById(R.id.btn_mini_player_close);

		titleView = findViewById(R.id.view_play);

		//접근성 메시지 설정 - 일반 LearLayout의 focus로 내부 텍스트를 하나로 읽는중. 다른 UI의 클릭이벤트(화면이동)기능을 titleView가 대체텍스트 포커스 사용함.
		/*
		titleView.setAccessibilityDelegate( new AccessibilityDelegate(){
			@Override
			public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
				super.onInitializeAccessibilityNodeInfo(host, info);
				//일반뷰에 클릭이벤트 성격의 안내를 추가 - titleView는 클릭이벤트가 설정되지않았으므로 수동으로 접근성 메시지에 추가시킴 (layoutMiniPlayer 클릭이벤트)
				AccessibilityNodeInfo.AccessibilityAction actionClick = AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK;
				info.addAction(actionClick);

				//사용자 메시지
				//int actionId = AccessibilityNodeInfo.ACTION_CLICK;
				//AccessibilityNodeInfo.AccessibilityAction myActionMessage = new AccessibilityNodeInfo.AccessibilityAction(actionId,"link"); //텍스트텍스트 ~ 링크~
				//info.addAction(myActionMessage);
			}
		});
		*/

		//titleView 클릭기능 숨김 -> 접근성 설명도 btnOcToggle으로 대체
		btnOcToggle.setAccessibilityDelegate( new AccessibilityDelegate(){
			@Override
			public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
				super.onInitializeAccessibilityNodeInfo(host, info);
				//일반뷰에 클릭이벤트 성격의 안내를 추가 - titleView는 클릭이벤트가 설정되지않았으므로 수동으로 접근성 메시지에 추가시킴 (layoutMiniPlayer 클릭이벤트)
				//AccessibilityNodeInfo.AccessibilityAction actionClick = AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK;
				//info.addAction(actionClick);

				//compat 및 사용자 메시지
				AccessibilityNodeInfoCompat infoCompat = AccessibilityNodeInfoCompat.wrap(info);
				infoCompat.setText(context.getString(R.string.read_script));
				AccessibilityNodeInfoCompat.AccessibilityActionCompat actionClickCompat =
						new AccessibilityNodeInfoCompat.AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_CLICK, context.getString(R.string.read_script));
				infoCompat.addAction(actionClickCompat);

				//사용자 메시지
				//int actionId = AccessibilityNodeInfo.ACTION_CLICK;
				//AccessibilityNodeInfo.AccessibilityAction myActionMessage = new AccessibilityNodeInfo.AccessibilityAction(actionId,"link"); //텍스트텍스트 ~ 링크~
				//info.addAction(myActionMessage);
			}
		});

		btnPlay.setOnClickListener(this);
		btnPause.setOnClickListener(this);
		btnNext.setOnClickListener(this);
		//추가
		btnPrev.setOnClickListener(this);
		//추가
		btnOcToggle.setOnClickListener(this);

		//배속
		btnSpeed.setOnClickListener(this);

		btnPlayList.setOnClickListener(this);
		btnMiniPlayerClose.setOnClickListener(this);

		//추가 n초 이동 (tvBackwardSec,tvForwardSec)
		btnBackwardSec = findViewById(R.id.btn_backward_sec);
		btnBackwardSec.setOnClickListener(this);
		btnForwardSec = findViewById(R.id.btn_forward_sec);
		btnForwardSec.setOnClickListener(this);

		//미니플레이어 전체 클릭시 , 일반 플레이어화면으로 이동
		/* btnOcToggle버튼클릭으로 대체
		titleView.setOnClickListener(v ->{
			openNormalPlayer()
		});
		*/

		layoutMiniPlayer.setOnClickListener(null);//더미이벤트( 미니플레이어에 겹쳐 배치된 웹뷰까지 이벤트가 넘어감 벙자처리 )

		ivThumb2.setOnClickListener(this);

		//추가 시크바 초기화
		initSeekArc2_MiniPlayer();
	}

	private void initSeekArc2_MiniPlayer() {
		seekMiniPlayer = findViewById(R.id.seek_mini_player);
		tvTimerStart = findViewById(R.id.tv_timer_start);
		tvTimerEnd = findViewById(R.id.tv_timer_end);

		seekMiniPlayer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

				if (b && isSeekTouch && PlayerService.isPlay) {
					//b 전달값이 isSeekTouch 같다.
					StoryItem item = PlayListManager.getInstance().getStoryItem();
					double endTime = Double.parseDouble(item.audioPlayTime);
					//int progress = seekBar.getProgress();
					int progress = i;

					int userSeekingTime  = (int) (progress * endTime / 100);
					int timeUnit = 1; //1000 , 1 ( 오디오정보가 밀리세컨드가 아닌 단위(초)로 들어옴)
					String userSeekingTimeString = String.format("%02d:%02d", userSeekingTime /(60 * timeUnit) % 60, userSeekingTime / timeUnit % 60);

					tvTimerStart.setText(userSeekingTimeString);
				}

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

				//추가
				isSeekTouch = true;
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//추가
				isSeekTouch = false;
				if (PlayerService.isPlay) {
					StoryItem item = PlayListManager.getInstance().getStoryItem();
					double endTime = Double.parseDouble(item.audioPlayTime);
					int currentTime = seekMiniPlayer.getProgress();
					PlayerService.startActionSeekTo(context, (int) (currentTime * endTime / 100));
				}

			}
		});
		//actionBtnMoveSeekTimeAmount 사용시, 시크바 고유동작에 의해 오작동 --> 시크바 thumb만을 이용하여 변경하도록 보완)
		seekMiniPlayer.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (isWithinThumb(event, seekMiniPlayer)) {
							mIsDragging = true;
							return false;
						}
					case MotionEvent.ACTION_UP:
						if (mIsDragging) {
							mIsDragging = false;
							return false;
						}
						if (isWithinThumb(event, seekMiniPlayer)) {
							return false;
						} else {
							return true;
						}
					case MotionEvent.ACTION_MOVE:
						if (!mIsDragging) {
							return true;
						}
				}
				return onTouchEvent(event);
			}
		});

	}

	//actionBtnMoveSeekTimeAmount 사용시, 시크바 고유동작에 의해 오작동 --> 시크바 thumb만을 이용하여 변경하도록 보완)
	private boolean isWithinThumb(MotionEvent event, SeekBar seekBar) {
		Rect rcThumb = seekBar.getThumb().getBounds();
		Rect rcDetectTouchArea = new Rect();
		int iWidth = rcThumb.width();
		int iHeight = rcThumb.height();
		rcDetectTouchArea.left = rcThumb.left - iWidth;
		rcDetectTouchArea.right = rcThumb.right + iWidth;
		rcDetectTouchArea.bottom = rcThumb.bottom + iHeight;
		return rcDetectTouchArea.contains((int) event.getX(), (int) event.getY());
	}

	private Handler handlerCheckPlayer = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(@NonNull Message msg) {
			if(msg.what == 15152 ) {
				int playIndex = 0;
				List<StoryItem> playList;
				//---------------------------------------------------------
				//-- prepare
				//추가
				setPlayerStampUI(PlayerService.isStampPlayUI);

				playIndex = PlayListManager.getInstance().getPlayIndex();
				//-- doing
				playList = StoryDbManager.getInstance(context).getPlayList().story;
				//-- result
				if (playList == null || playList.isEmpty()) {
					return;
				}

				if (playIndex != -1) {
					title.setText(playList.get(playIndex).title);
					//subTitle.setText(playList.get(playIndex).audioTitle); //서브타이틀 UI숨김 , 리소스 로드x
					//ImageUtil.loadImage(ivThumb, playList.get(playIndex).thumbnailFilePath, null); //섬네일 UI숨김 , 리소스 로드x
				}

				if (PlayerService.isPlay) {
					btnPlay.setVisibility(View.INVISIBLE);

					btnPause.setVisibility(View.VISIBLE);
					setCloseVisibility(View.GONE);
				} else {
					btnPlay.setVisibility(View.VISIBLE);
					btnPause.setVisibility(View.INVISIBLE);

					if(PlayerService.isFloating) {
						setCloseVisibility(View.GONE);
					} else {
						if(layoutMiniPlayer.getVisibility() == View.VISIBLE) {
							setCloseVisibility(View.VISIBLE);
						} else {
							setCloseVisibility(View.GONE);
						}
					}
				}

				//-------------- 시크바
				if(btnSpeed!=null){
					//배속설정 현상태
					btnSpeed.setBackgroundResource(PlayerService.getCurrentMiniPlayRes());
					btnSpeed.setContentDescription(PlayerService.getCurrentPlayspeedContentDescriptionString());
				}
				//===============

				//추가 (재생목록 엑티비티)등에서 지정한 플래그가 있을경우
				if(isStoryPlayerListActivity){
					setCloseVisibility(View.GONE);
					setVisibility(View.VISIBLE);
				}else{
					//재생목록 상시보이기에서 버튼제어중 alwaysGone_btnMiniPlayerClose == true
					//일반 메인용 -> 최초숨김상태 인경우
					if(isOnceHideEvenIfPlayListExist){
						if(getVisibility()!=View.GONE){
							setVisibility(View.GONE);
						}
					}else{
						//일반 메인용 -> 최초 숨김상태가 해제된 이후,
						//setPlayerMode에서 발생한 마지막 상태복원

						setVisibility(lastVisibility_bySetPlayerMode);
						// self전체를 치우지않고 >>>>>>>>>>> layoutMiniPlayer 영역만 제어해보자.
						/* 내리기 토글 미사용
						setVisibility(View.VISIBLE);//기본적으로 보이고
						layoutMiniPlayer.setVisibility(lastVisibility_bySetPlayerMode); //필요한 부분만 제어
						*/
					}
				}

			}

			super.handleMessage(msg);
		}
	};
	public void checkPlayer() {
		//handlerCheckPlayer.sendEmptyMessageDelayed(0, 200);
		handlerCheckPlayer.sendEmptyMessage(15152);
	}

	public void setCloseVisibility(int visiable) {
		//btnMiniPlayerClose.setVisibility(visiable);

		//상시 닫기버튼 보이기 수정2
		btnMiniPlayerClose.setVisibility(View.VISIBLE);
	}

	private Handler handler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(@NonNull Message msg) {
			if(msg.what == 0 ) {
				//titleView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
				btnOcToggle.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
			} else if(msg.what == 1) {
				ivThumb2.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
			}

			super.handleMessage(msg);
		}
	};

	@Override
	public void onClick(View v) {

		if(b_OnAnimation_OcToogle){
			return;
		}

		Intent intent;
		switch (v.getId()) {

			case R.id.player_thumb2:
				PlayerService.isFloating = false;
				AnalyticsInterface.getInstance().logEvent(context, "mini_player_play");
				PlayerService.startActionPause(context);
				handler.sendEmptyMessageDelayed(0, 200);
				break;
			case R.id.btn_mini_player_list:

				if(context instanceof StoryPlayerListActivity){
					//현재 엑티비티 호출금지
					return;
				}

				AnalyticsInterface.getInstance().logEvent(context, "mini_player_playlist");
				intent = new Intent(context, StoryPlayerListActivity.class);
				//intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				context.startActivity(intent);

				break;
			case R.id.btn_mini_player_next:
				AnalyticsInterface.getInstance().logEvent(context, "mini_player_next");
				PlayerService.startActionNextSong(context);
				break;
			case R.id.btn_mini_player_prev:
				//추가 prev
				AnalyticsInterface.getInstance().logEvent(context, "mini_player_prev");
				PlayerService.startActionPreviousSong(context);
				break;
			case R.id.btn_oc_toggle:
				//추가 btnOcToggle
				/* 내리기 토글 미사용
				if(layoutMiniPlayer.getVisibility()==View.VISIBLE){
					PlayerService.isOcToogleClosed = false;
				}
				btnOcToogleAnimate(!PlayerService.isOcToogleClosed);
				*/

				//일반플레이어로 기능연결
				openNormalPlayer();
				break;
			case R.id.btn_mini_player_speed:
				//-- 시크바 추가 및 관련 기능 (배속)
				//다음 재생속도 이동
				PlayerService.getNextPlayspeed();

				btnSpeed.setBackgroundResource(PlayerService.getCurrentMiniPlayRes());
				PlayerService.startActionPlay2x(context, PlayerService.getCurrentPlayspeedValue());
				AnalyticsInterface.getInstance().logEvent(context, PlayerService.getCurrentPlayspeedLogString());
				btnSpeed.setContentDescription(PlayerService.getCurrentPlayspeedContentDescriptionString());

				btnSpeed.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);

				//========================
				break;
			case R.id.btn_mini_player_pause:
				AnalyticsInterface.getInstance().logEvent(context, "mini_player_pause");
				PlayerService.startActionPause(context);
				break;
			case R.id.btn_mini_player_play:
				AnalyticsInterface.getInstance().logEvent(context, "mini_player_play");
				PlayerService.startActionPlay(context);
				break;
			case R.id.btn_mini_player_close:
				PlayerService.startActionStop(context);
				break;
			case R.id.btn_backward_sec:
				//추가 5초전 이동
				actionBtnMoveSeekTimeAmount(-5);
				break;
			case R.id.btn_forward_sec:
				//추가 5초후 이동
				actionBtnMoveSeekTimeAmount(5);
				break;
		}
	}

	private void openNormalPlayer(){
		Intent intent = new Intent(context, Player.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		context.startActivity(intent);
	}

	//추가 n초 전후 이동처리 (현재 재생위치 등 고려)
	private void actionBtnMoveSeekTimeAmount(int moveTimeSecond){
		isSeekTouch = true;

		//===== 시크시작
		if (isSeekTouch && PlayerService.isPlay) {
			//현재 재생시간을 시크바의 위치값으로 가져오자

			if(seekMiniPlayer!=null){

				//오디오 서비스에서 참조할때는 참조변수를 별도로 빼야한다.

				//현재 시간정보
				//b 전달값이 isSeekTouch 같다.
				StoryItem item = PlayListManager.getInstance().getStoryItem();
				double endTime = Double.parseDouble(item.audioPlayTime); //미디어 전채시간(초)
				int maxTime = (int)endTime;

				//int progress = seekBar.getProgress();
				int progress = seekMiniPlayer.getProgress();
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

				//===== 시크 결과위치
				PlayerService.startActionSeekTo(context, userSeekingTime);// 시간(초) 전달

			}

		}
		isSeekTouch = false;
	}

	//추가 -시크바와 관련된 제어추가 GUI_UPDATE_ACTION 브로드캐스트를 미니플레이어로 전달받을때
	public boolean getIsSeekTouch(){
		return isSeekTouch;
	}
	public void apply_MiniPlayer_GUI_UPDATE_ACTION(Intent intent){
		if(intent!=null){

			if(isSeekTouch){
				return;
			}
			//
			int t1 = intent.getIntExtra(PlayerConstants.ACTUAL_TIME_VALUE_EXTRA, 0);
			int t2 = intent.getIntExtra(PlayerConstants.TOTAL_TIME_VALUE_EXTRA, 0);
			float seekArcTime = (float) t1 / (float) t2 * 100.0f;

			//--
			if(seekMiniPlayer!=null){
				seekMiniPlayer.setProgress((int) seekArcTime);
			}

			if(tvTimerStart!=null){
				tvTimerStart.setText(String.format("%02d:%02d", t1 / (60 * 1000) % 60, t1 / 1000 % 60));
				tvTimerStart.setContentDescription(String.format("현재 이야기 재생시간 %02d분%02d초", t1 / (60 * 1000) % 60, t1 / 1000 % 60));

			}
			if(tvTimerEnd!=null){
				tvTimerEnd.setText(String.format("%02d:%02d", t2 / (60 * 1000) % 60, t2 / 1000 % 60));
				tvTimerEnd.setContentDescription(String.format("이야기 전체재생시간 %02d분%02d초", t2 / (60 * 1000) % 60, t2 / 1000 % 60));
			}
			//====================

		}
	}


	public void showToast(String msg) {

		if(getVisibility()!=View.VISIBLE){
			//임시로 보이도록 설정
			setVisibility(View.VISIBLE);//self visibility
			//임시로 오디오플레이어 숨김
			layoutMiniPlayer.setVisibility(View.GONE);
		}

		handler.sendEmptyMessageDelayed(0, 200);
		layoutToast.setVisibility(View.VISIBLE);
		tvToastMsg.setText(msg);
		layoutToast.postDelayed(() -> {
			layoutToast.setVisibility(View.GONE);


			setVisibility(lastVisibility_bySetPlayerMode);//self visibility
			/*
			//추가, 초상위의 (임시x) 알려지만 마지막 출력상태로 복원 - setPlayerMode에서 발생한 마지막 상태복원
			setVisibility(lastVisibility_bySetPlayerMode);//self visibility
			//임시로 오디오플레이어 숨김을 해제
			layoutMiniPlayer.setVisibility(View.VISIBLE);
			*/

			//일단보이기 유지하고 OcToggle상태기준으로 유지
			/* 내리기 토글 미사용
			setVisibility(View.VISIBLE);
			layoutMiniPlayer.setVisibility( PlayerService.isOcToogleClosed? View.GONE:View.VISIBLE );
			//갱신
			requestLayoutPlayer();
			*/
		}, 1000);

	}

	private void requestLayoutPlayer(){
		invalidate();
	}
}
