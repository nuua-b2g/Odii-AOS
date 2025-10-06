package kto.smarttour.service;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.Player;
import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import hybridmediaplayer.ExoMediaPlayer;
import hybridmediaplayer.HybridMediaPlayer;
import hybridmediaplayer.MediaSourceInfo;
import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.common.utils.CCLUUIDHelper;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.MediaStyleHelper;
import kto.smarttour.common.utils.OdiiAnalytics;
import kto.smarttour.common.utils.PendingIntentUtils;
import kto.smarttour.common.utils.PreferenceUtils;
import kto.smarttour.common.utils.WorkerChain;
import kto.smarttour.db.StampDBManager;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.network.ApiService;
import kto.smarttour.network.response.dao.StampEventList;
import kto.smarttour.ui.MainActivity;
import kto.smarttour.ui.player.PlayListManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener {

	public static boolean isPlay = false;
	public static ExoMediaPlayer player;
	public static float currentVolume = 0;
	public static boolean isMute = false;

	//	public static boolean isReplay = false;
	public static boolean isRandomPlay = false;
	public static boolean isFloating = false;

	//--
	public static boolean isOcToogleClosed = false;

	//--
	public static boolean isStampPlayUI = false;

	public final static int PLAY_TYPE_NONE = 0;
	public final static int PLAY_TYPE_REPLAY = 1;
	public final static int PLAY_TYPE_REPLAY_ONE = 2;

	private static int playType = PLAY_TYPE_NONE;

	public static int getPlayType() {
		return playType;
	}

	public static void setPlayType(int type) {
		playType = type;
	}

	public static boolean isPlay() {
		return isPlay || player != null;
	}

	public static boolean checkPlay() {
		return isPlay;
	}

	public final static int PLAY_SPEED_TYPE_1 = 0;
	public final static int PLAY_SPEED_TYPE_1_25 = 1;
	public final static int PLAY_SPEED_TYPE_1_5 = 2;
	//추가 2배속
	public final static int PLAY_SPEED_TYPE_2 = 3;

	//==================================================================
	//--
	private static ArrayList<Integer> arr_PLAY_SPEED_TYPE;
	private static int position_PLAY_SPEED_TYPE = 0;
	public static int getNextPlayspeed(){
		initArrays();

		int size = arr_PLAY_SPEED_TYPE.size();
		position_PLAY_SPEED_TYPE++;
		if(position_PLAY_SPEED_TYPE>=size){
			position_PLAY_SPEED_TYPE = 0;
		}
		return arr_PLAY_SPEED_TYPE.get(position_PLAY_SPEED_TYPE);
	}

	//--
	public static int getCurrentPlaySpeedType(){
		initArrays();

		int size = arr_PLAY_SPEED_TYPE.size();
		if(position_PLAY_SPEED_TYPE>=size){
			position_PLAY_SPEED_TYPE = 0;
		}
		return arr_PLAY_SPEED_TYPE.get(position_PLAY_SPEED_TYPE);
	}

	private static ArrayList<Integer> arr_PLAY_SPEED_RES_PLAYER;
	public static int getCurrentPlayRes(){
		initArrays();

		int size = arr_PLAY_SPEED_RES_PLAYER.size();
		if(position_PLAY_SPEED_TYPE>=size){
			position_PLAY_SPEED_TYPE = 0;
		}
		return arr_PLAY_SPEED_RES_PLAYER.get(position_PLAY_SPEED_TYPE);
	}
	private static ArrayList<Integer> arr_PLAY_SPEED_RES_MINIPLAYER;
	public static int getCurrentMiniPlayRes(){
		initArrays();

		int size = arr_PLAY_SPEED_RES_MINIPLAYER.size();
		if(position_PLAY_SPEED_TYPE>=size){
			position_PLAY_SPEED_TYPE = 0;
		}
		return arr_PLAY_SPEED_RES_MINIPLAYER.get(position_PLAY_SPEED_TYPE);
	}
	private static ArrayList<Float> arr_PLAY_SPEED_VALUE;
	public static float getCurrentPlayspeedValue(){
		initArrays();

		int size = arr_PLAY_SPEED_VALUE.size();
		if(position_PLAY_SPEED_TYPE>=size){
			position_PLAY_SPEED_TYPE = 0;
		}
		return arr_PLAY_SPEED_VALUE.get(position_PLAY_SPEED_TYPE);
	}
	public static String getCurrentPlayspeedLogString(){
		initArrays();

		int size = arr_PLAY_SPEED_VALUE.size();
		if(position_PLAY_SPEED_TYPE>=size){
			position_PLAY_SPEED_TYPE = 0;
		}

		double value = arr_PLAY_SPEED_VALUE.get(position_PLAY_SPEED_TYPE);

		//소숫점 줄재짜리 버림 1.0 1.2 1.5 2.0
		//String result = "player_"+Math.floor(value * 10) / 10.0 + "x";

		//일단 소숫점 셋째 자리까지 버림
		double tempValue = Math.floor(value * 100) / 100.0f;
		String result = "player_"+tempValue+ "x"; //결과 player_1.0x, player_1.25x, player_1.5x, player_2.0x
		return result;
	}
	public static String getCurrentPlayspeedContentDescriptionString(){

		getCurrentPlayspeedLogString();


		int size = arr_PLAY_SPEED_VALUE.size();
		if(position_PLAY_SPEED_TYPE>=size){
			position_PLAY_SPEED_TYPE = 0;
		}

		float value = arr_PLAY_SPEED_VALUE.get(position_PLAY_SPEED_TYPE);

		//우선 소숫점 불필요한 0제거 1.25000000000 을 1.25표기
		String result;
		if(value == (long) value)
		{
			result = String.format("%d배속 재생속도 설정됨",(long)value);
		}
		else
		{
			result = String.format("%s배속 재생속도 설정됨",value);
		}

		//결과 1, 1.25, 1.5 , 2
		return result;
	}

	//==================================================================

	private static boolean driveMode = false;

	private WorkerChain mWorkerChain = new WorkerChain();

	private Context mContext;

	public static void setDriveMode(boolean isDriveMode) {
		driveMode = isDriveMode;
	}

	public static boolean getDriveMode() {
		return driveMode;
	}

	private static boolean isGpsPlayComplete = true;

	public static boolean getGpsPlayComplete() {
		return isGpsPlayComplete;
	}

	public static void setGpsPlayComplete(boolean isGpsPlayComplete) {
		PlayerService.isGpsPlayComplete = isGpsPlayComplete;
	}

	//추가 Looper용 핸들러 플레이어
	//private Handler handlerFoorLooper;

	private static void initArrays(){

		if(position_PLAY_SPEED_TYPE < 0){
			position_PLAY_SPEED_TYPE = 0;
		}

		if(arr_PLAY_SPEED_TYPE==null){
			//배속 관리배열 초기화
			arr_PLAY_SPEED_TYPE = new ArrayList<>();
			arr_PLAY_SPEED_TYPE.add(PLAY_SPEED_TYPE_1);
			arr_PLAY_SPEED_TYPE.add(PLAY_SPEED_TYPE_1_25);
			arr_PLAY_SPEED_TYPE.add(PLAY_SPEED_TYPE_1_5);
			arr_PLAY_SPEED_TYPE.add(PLAY_SPEED_TYPE_2);
		}

		if(arr_PLAY_SPEED_VALUE==null){
			arr_PLAY_SPEED_VALUE = new ArrayList<>();
			arr_PLAY_SPEED_VALUE.add(1.0f);
			arr_PLAY_SPEED_VALUE.add(1.25f);
			arr_PLAY_SPEED_VALUE.add(1.5f);
			arr_PLAY_SPEED_VALUE.add(2.0f);
		}

		if(arr_PLAY_SPEED_RES_PLAYER==null){
			arr_PLAY_SPEED_RES_PLAYER = new ArrayList<>();
			/*
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_1x_white_v2);
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_125x_white_v2);
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_15x_white_v2);
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_2x_white_v2);
			*/

			/*
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_1x_white_v3);
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_125x_white_v3);
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_15x_white_v3);
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_2x_white_v3);
			*/

			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_1x_white_v4);
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_125x_white_v4);
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_15x_white_v4);
			arr_PLAY_SPEED_RES_PLAYER.add(R.drawable.ic_bt_player_2x_white_v4);
		}

		//---------------------------------------------------------------------------------------------------
		if(arr_PLAY_SPEED_RES_MINIPLAYER==null){
			arr_PLAY_SPEED_RES_MINIPLAYER = new ArrayList<>();
			/*
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_player_1x_v2);
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_player_125x_v2);
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_player_15x_v2);
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_player_2x_v2);
			*/

			/*
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_player_1x_white_v3);
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_player_125x_white_v3);
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_player_15x_white_v3);
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_player_2x_white_v3);
			*/

			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_mini_player_1x_white);
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_mini_player_125x_white);
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_mini_player_15x_white);
			arr_PLAY_SPEED_RES_MINIPLAYER.add(R.drawable.ic_bt_mini_player_2x_white);

		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;

		initArrays();


		//---------------------------------------

		PlayListManager.getInstance().load();
		initMediaSession();
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
	}

	@Override
	public void onDestroy() {
		stop();
		isUpdatingThread = false;
		stopForeground(true);
		abandonAudioFocus();
		cancelNotification();

		if (mediaSessionCompat != null) {
			mediaSessionCompat.setActive(false);
			mediaSessionCompat.release();
			mediaSessionCompat.setMediaButtonReceiver(null);
		}

		super.onDestroy();
	}

	@Nullable
	@Override
	public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
		if (TextUtils.equals(clientPackageName, getPackageName())) {
			return new BrowserRoot(getString(R.string.app_name), null);
		}
		return null;
	}

	@Override
	public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
		result.sendResult(null);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		mainThreadHandler = new Handler();
		if (intent != null) {

			if (intent.getAction().equals(PlayerConstants.ACTION_CLEAR_PLAY)) {
				isFloating = false;

				//--
				isStampPlayUI = intent.getBooleanExtra("isStampPlayUI",false);

				killPlayer();
				PlayListManager.getInstance().setPlayIndex(0);
				createPlayer();
			}

			if (intent.getAction().equals(PlayerConstants.ACTION_CLEAR_PLAY_STAMP)) {
				isFloating = false;

				//--
				isStampPlayUI = intent.getBooleanExtra("isStampPlayUI",false);

				killPlayer();
				PlayListManager.getInstance().setPlayIndex(0);
				createPlayer();
			}
			if (intent.getAction().equals(PlayerConstants.ACTION_PLAY_2X)) {
				float speed = intent.getFloatExtra(PlayerConstants.EXTRA_PARAM1, 0);
				play2x(speed);
			}
			if (intent.getAction().equals(PlayerConstants.ACTION_PAUSE)) {
				pause();
				return START_NOT_STICKY;
			}

			//--
			if (intent.getAction().equals(PlayerConstants.ACTION_PAUSE_EDIT)) {
				pause_edit();
				return START_NOT_STICKY;
			}

			if (intent.getAction().equals(PlayerConstants.ACTION_PLAY)) {
				isFloating = false;
				play();
				return START_NOT_STICKY;
			}
			if (intent.getAction().equals(PlayerConstants.ACTION_PLAY_INDEX)) {
				killPlayer();
				play();
			}
			if (intent.getAction().equals(PlayerConstants.ACTION_SET_PLAYLIST)) {
				setPlayListplay();
			}
			if (intent.getAction().equals(PlayerConstants.ACTION_NEXT)) {
				nextSong();
			}
			if (intent.getAction().equals(PlayerConstants.ACTION_PLAYER_INIT)) {
				init();
			}
			if (intent.getAction().equals(PlayerConstants.ACTION_SEEK_TO)) {
				int time = intent.getIntExtra(PlayerConstants.EXTRA_PARAM1, 0);
				seekTo(time);
			}

			if (intent.getAction().equals(PlayerConstants.ACTION_MUTE)) {
				playMute();
			}

			if (intent.getAction().equals(PlayerConstants.ACTION_PLAY_RANDOM)) {
				isRandomPlay = !isRandomPlay;
			}

			if (intent.getAction().equals(PlayerConstants.ACTION_STOP) || intent.getAction().equals(DELETE_ACTION)) {
				stop();
			}

			if (intent.getAction().equals(PlayerConstants.ACTION_STOP2)) {
				stop2();
			}

			if (intent.getAction().equals(PlayerConstants.ACTION_AUDIO_FOCUS_SKIP)) {
				abandonAudioFocus();
				new Handler(Looper.getMainLooper()).postDelayed(() -> {
					retrieveAudioFocus();
				}, 5000);
			}

			if (intent.getAction().equals(PlayerConstants.ACTION_PREVIOUS)) {
				previousSong();
			} else {
				MediaButtonReceiver.handleIntent(mediaSessionCompat, intent);
			}
			if (PlayListManager.getInstance().getPlayIndex() != -1) {
				//KLog.i("PlayerServiceACTION_PLAY_INFO", "onStartCommand ACTION_PLAY_INFO");
				sendBroad(new Intent(PlayerConstants.ACTION_PLAY_INFO));
			}
		}
		return START_NOT_STICKY;
	}

	private void init() {
		if (player == null) {
			player = makePlayer();
		}
		PlayListManager.getInstance().load();
		MediaSourceInfo mediaSourceInfo;
		StoryItem songItem;
		try {
			songItem = PlayListManager.getInstance().getStoryItem(PlayListManager.getInstance().getPlayIndex());
			if (songItem == null) {
				stop();
				return;
			}
		} catch (Exception e) {
			stop();
			return;
		}

		if ("L".equals(songItem.play_type)) {
			mediaSourceInfo = new MediaSourceInfo.Builder().setUrl(songItem.audioFilePath).setImageUrl(songItem.thumbnailFilePath).setTitle(songItem.audioTitle).build();
		} else {
			mediaSourceInfo = new MediaSourceInfo.Builder().setUrl(FileUtils.getFileName(this, songItem.tid, songItem.audioFilePath)).setImageUrl(FileUtils.getFileName(this, songItem.tid, songItem.thumbnailFilePath)).setTitle(songItem.audioTitle).build();
		}
		player.setDataSource(mediaSourceInfo);
	}

	private void play() {
		if (!retrieveAudioFocus()) return;

		if (player == null) {
			createPlayer();
		} else {
			if (!player.isPlaying()) {
				if (player.getCurrentPosition() > 0) {
					player.play();
				} else {
					player.prepare();

					/* TODO 이야기 재생 통계 2020-10-20 개발 미반영
					StoryItem songItem = PlayListManager.getInstance().getStoryItem();
					NetworkLog(songItem);
					 */
				}

				try {
					StoryItem songItem = PlayListManager.getInstance().getStoryItem(PlayListManager.getInstance().getPlayIndex());
					OdiiAnalytics.storyPlayStart(this, songItem.tlid, songItem.slid);
				} catch (Exception e) {}


			}
		}
		isPlay = true;
		makeNotification();
		setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
		sendBroad(new Intent(PlayerConstants.ACTION_SHOW_MINI_PLAYER));
		sendBroad(new Intent(PlayerConstants.ACTION_PLAYER_STATUS_PLAY));
	}

	private void setPlayListplay() {

		if (player == null) {
			createPlayer();
		} else {
			if (isPlay && player.isPlaying()) {
				player.play();
			} else {
				createPlayer();
			}
		}
		isPlay = true;
		sendBroad(new Intent(PlayerConstants.ACTION_SHOW_MINI_PLAYER));
		sendBroad(new Intent(PlayerConstants.ACTION_PLAYER_STATUS_PLAY));
	}

	private void play2x(float speed) {
		if (player != null) {
			player.setPlaybackParams(speed, 1.0f);
		}
	}

	private void playMute() {

		if (player != null) {
			if (isMute) {
				player.setVolume(currentVolume);
				isMute = false;
			} else {
				currentVolume = player.getVolume();
				player.setVolume(0.0f);
				isMute = true;
			}
		}
	}

	private int getRandomIndex() {
		int songTotalCount = PlayListManager.getInstance().getListSize();

		if (songTotalCount == 0) {
			return 0;
		}

		int index = 0;
		Random r = new Random();
		for (; ; ) {
			index = r.nextInt(songTotalCount);
			if (index != PlayListManager.getInstance().getPlayIndex()) {
				PlayListManager.getInstance().setPlayIndex(index);
				return index;
			}
		}
	}

	private void pause() {
		if (player != null) {
			player.pause();
		}
		isPlay = false;
		makeNotification();
		setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
		sendBroad(new Intent(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE));
	}
	private void pause_edit() {
		if (player != null) {
			player.pause();
		}
		isPlay = false;
		makeNotification();
		setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
		sendBroad(new Intent(PlayerConstants.ACTION_PLAYER_STATUS_PAUSE_EDIT));
	}

	private void seekTo(int time) {
		if (player != null) {
			player.getCurrentPlayer().seekTo(time * 1000);
		}
	}

	private void nextSong() {

		if (isRandomPlay) {
			PlayListManager.getInstance().setPlayIndex(getRandomIndex());
		} else {
			if (PlayListManager.getInstance().getPlayIndex() + 1 > StoryDbManager.getInstance(this).getPlayListCount() - 1) {
				return;
			}
			PlayListManager.getInstance().next();
		}

		isPlay = true;

		if (player != null) {
			player.stop();
		}

		createPlayer();
	}

	private void previousSong() {

		if (isRandomPlay) {
			PlayListManager.getInstance().setPlayIndex(getRandomIndex());
		} else {
			if (PlayListManager.getInstance().getPlayIndex() - 1 < 0) {
				PlayListManager.getInstance().setPlayIndex(0);
				return;
			}
			PlayListManager.getInstance().prev();
		}

		isPlay = true;

		if (player != null) {
			player.stop();
		}
		createPlayer();
	}

	private void stop() {
		sendBroad(new Intent(PlayerConstants.ACTION_STOP));
		setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
		PlayListManager.getInstance().setPlayIndex(-1);
		killPlayer();
		isPlay = false;
		cancelNotification();
	}

	private void stop2() {
		setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
		PlayListManager.getInstance().setPlayIndex(0);
		killPlayer();
		isPlay = false;
		cancelNotification();
	}

	private ExoMediaPlayer.OnPlayerStateChanged eventListener = new ExoMediaPlayer.OnPlayerStateChanged() {
		@Override
		public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
			switch (playbackState) {
				case Player.STATE_IDLE:
					KLog.i("onPlayerStateChanged", "onPlayerStateChanged : STATE_IDLE");
					break;
				case Player.STATE_BUFFERING:
					KLog.i("onPlayerStateChanged", "onPlayerStateChanged : STATE_BUFFERING");
					break;
				case Player.STATE_READY:
					KLog.i("onPlayerStateChanged", "onPlayerStateChanged : STATE_READY");
					break;
				case Player.STATE_ENDED:
					KLog.i("onPlayerStateChanged", "onPlayerStateChanged : STATE_ENDED");
					break;
			}
		}
	};

	private void createPlayer() {
		if (player == null) {
			player = makePlayer();
		}

		//--
		play2x(getCurrentPlayspeedValue());


		PlayListManager.getInstance().load();
		MediaSourceInfo mediaSourceInfo;
		StoryItem songItem;

		try {
			songItem = PlayListManager.getInstance().getStoryItem(PlayListManager.getInstance().getPlayIndex());
			if (songItem == null) {
				stop();
				return;
			}
		} catch (Exception e) {
			stop();
			return;
		}
		PreferenceUtils.setPreference(this, "PLAY_INDEX", PlayListManager.getInstance().getPlayIndex());

		if ("L".equals(songItem.play_type)) {
			mediaSourceInfo = new MediaSourceInfo.Builder().setUrl(songItem.audioFilePath).setImageUrl(songItem.thumbnailFilePath).setTitle(songItem.audioTitle).build();
		} else {
			mediaSourceInfo = new MediaSourceInfo.Builder().setUrl(FileUtils.getFileName(this, songItem.tid, songItem.audioFilePath)).setImageUrl(FileUtils.getFileName(this, songItem.tid, songItem.thumbnailFilePath)).setTitle(songItem.audioTitle).build();
		}
		Log.d("Media source check", mediaSourceInfo.toString());
		player.setDataSource(mediaSourceInfo);
		player.prepare();
		mediaSessionCompat.setActive(true);
		/* TODO 이야기 재생 통계 2020-10-20 개발 미반영
		NetworkLog(songItem);
		*/
		OdiiAnalytics.storyPlayStart(this, songItem.tlid, songItem.slid);
	}


	private ExoMediaPlayer makePlayer() {
		ExoMediaPlayer player = new ExoMediaPlayer(this);
		player.setOnPreparedListener(onPreparedListener);
		player.setOnCompletionListener(onCompletionListener);
		return player;
	}

	private HybridMediaPlayer.OnPreparedListener onPreparedListener = (player) -> {

		if (!retrieveAudioFocus()) return;

		player.play();
		setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
		sendBroad(new Intent(PlayerConstants.ACTION_SHOW_MINI_PLAYER));
		isPrepared = true;
		isPlay = true;

		//추가->재생 발생시 사용자 일시정지 플래그 해제
		userPause = false;

		startUiUpdateThread();
		KLog.i("PlayerServiceACTION_PLAY_INFO", "OnPreparedListener ACTION_PLAY_INFO");
		Intent intent = new Intent(PlayerConstants.ACTION_PLAY_INFO);
		intent.putExtra("currentWindowIndex", PlayListManager.getInstance().getPlayIndex());
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
		new NotificationAsyncTask().execute();
	};



	private HybridMediaPlayer.OnCompletionListener onCompletionListener = (player2) -> {

		if(!driveMode) {
			PlayListManager.getInstance().load();
			int playIndex = PlayListManager.getInstance().getPlayIndex();

			try {
				NetworkStamp(playIndex);
				StoryItem songItem = PlayListManager.getInstance().getStoryItem(playIndex);
				OdiiAnalytics.storyPlayEnd(this, songItem.tlid, songItem.slid);

				//오디오 재생 종료시 인터페이스 호출 to webview
				//--
				Intent intentAudioFinished = new Intent();
				intentAudioFinished.setAction("AUDIO_FINISHED");
				intentAudioFinished.putExtra("tlid",songItem.tlid);
				intentAudioFinished.putExtra("slid",songItem.slid);
				LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentAudioFinished);

			} catch (Exception e) {

			}


			if (playType == PLAY_TYPE_REPLAY_ONE) {
				PlayListManager.getInstance().getPlayIndex();
			} else {
				if (isRandomPlay) {
					playIndex = getRandomIndex();
					PlayListManager.getInstance().setPlayIndex(playIndex);
				} else {
					if (playType == PLAY_TYPE_REPLAY) {
						playIndex = PlayListManager.getInstance().setPlayIndex(playIndex + 1);
						if (playIndex >= PlayListManager.getInstance().getListSize()) {
							playIndex = PlayListManager.getInstance().setPlayIndex(0);
						}
					} else {
						playIndex = PlayListManager.getInstance().setPlayIndex(playIndex + 1);
					}


				}
			}

			killPlayer();
			if (playIndex > PlayListManager.getInstance().getListSize() - 1) {
				sendBroad(new Intent(PlayerConstants.ACTION_HIDE_MINI_PLAYER));
				sendBroad(new Intent(PlayerConstants.ACTION_PLAY_COMPLETE));
				isPlay = false;
				// 현 포지션 유지
				PlayListManager.getInstance().setPlayIndex(playIndex - 1);
				cancelNotification();
			} else {
				isPlay = true;
				createPlayer();
			}
		} else {
			isPlay = false;
			killPlayer();
			sendBroad(new Intent(PlayerConstants.ACTION_PLAY_COMPLETE));
			isGpsPlayComplete = true;
		}
	};

	private Thread updateThread;
	private Handler mainThreadHandler;
	private boolean isUpdatingThread;
	private boolean isPrepared;


	private void startUiUpdateThread() {
		if (mainThreadHandler == null) {
			isUpdatingThread = false;
			return;
		}

		isUpdatingThread = true;

		if (userPause) {
			return;
		}
		//--
		if (updateThread == null) {

			updateThread = new Thread(() -> {
				Intent guiUpdateIntent = new Intent();
				guiUpdateIntent.setAction(PlayerConstants.GUI_UPDATE_ACTION);

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				while (isUpdatingThread && isPrepared) {
					//--
					//Player is accessed on the wrong thread.
					try {
						if(player!=null){
							//SimpleExoPlayer
							Looper looper = player.getCurrentPlayer().getApplicationLooper();
							if(looper!=null){
								new Handler(looper).post(() -> {

									if(player!=null && !userPause){// 체크
										guiUpdateIntent.putExtra(PlayerConstants.ACTUAL_TIME_VALUE_EXTRA, player.getCurrentPosition());
										guiUpdateIntent.putExtra(PlayerConstants.TOTAL_TIME_VALUE_EXTRA, player.getDuration());
										sendBroad(guiUpdateIntent);
										//추가
										updateMediaPlaybackStateCurrentPosition();
									}

								});
							}
						}
					} catch (Exception e) {
						//--
					}


					try {
						Thread.sleep(400);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
				updateThread = null;

			});

			updateThread.start();
		}
	}

	private void sendBroad(Intent intent) {
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}


	public static void startActionPlay(Context context) {
		userPause = false;
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_PLAY);
		context.startService(intent);
	}

	public static void startActionPlayIndex(Context context) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_PLAY_INDEX);
		context.startService(intent);
	}

	public static boolean userPause = false;

	public static void startActionInit(Context context) {
		userPause = false;
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_PLAYER_INIT);
		context.startService(intent);
	}

	public static void startActionPause(Context context) {
		userPause = true;
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_PAUSE);
		context.startService(intent);
	}

	//--
	public static void startActionPause_Edit(Context context) {
		userPause = true;
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_PAUSE_EDIT);
		context.startService(intent);
	}

	public static void startActionNextSong(Context context) {
		userPause = false;
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_NEXT);
		context.startService(intent);
	}

	public static void startActionPreviousSong(Context context) {
		userPause = false;
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_PREVIOUS);
		context.startService(intent);
	}


	public static void startActionSeekTo(Context context, int time) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_SEEK_TO);
		intent.putExtra(PlayerConstants.EXTRA_PARAM1, time);
		context.startService(intent);
	}

	public static void startActionPlay2x(Context context, float speed) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_PLAY_2X);
		intent.putExtra(PlayerConstants.EXTRA_PARAM1, speed);
		context.startService(intent);
	}

	public static void startActionPlayRandom(Context context) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_PLAY_RANDOM);
		context.startService(intent);
	}

	public static void startActionMute(Context context) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_MUTE);
		context.startService(intent);
	}

	public static void startActionStop(Context context) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_STOP);
		context.startService(intent);
	}

	public static void startActionStop2(Context context) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_STOP2);
		context.startService(intent);
	}

	public static void startActionAudioFocusSkip(Context context) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_AUDIO_FOCUS_SKIP);
		context.startService(intent);
	}

	public static void startActionFlotMode(Context context) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(PlayerConstants.ACTION_PLAYER_FLOAT_MODE);
		context.startService(intent);
	}

	private boolean retrieveAudioFocus() {
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			result = audioManager.requestAudioFocus(new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build()).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(this).build());
		} else {
			result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		}

		return result == AudioManager.AUDIOFOCUS_GAIN;
	}

	private void abandonAudioFocus() {
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioManager.abandonAudioFocus(this);
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		//KLog.d("PlayerService", "focusChange - " + focusChange);
		if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
			pause();
		} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

			if (userPause) {
				return;
			}
			userPause = false;
			player.setVolume(1f);
			play();
			setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
		} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
			pause();
			abandonAudioFocus();
			mediaSessionCompat.setActive(false);
			setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
		} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
			player.setVolume(0.1f);
		}
	}

	private void killPlayer() {
		abandonAudioFocus();
		isUpdatingThread = false;
		if (player != null) {
			try {
				player.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		player = null;
	}

	//--------------------------Notification--------------------------
	private void initMediaSession() {

		ComponentName mediaButtonReceiver = new ComponentName(this, MediaButtonReceiver.class);

		mediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "MediaTAG", mediaButtonReceiver, null);
		mediaSessionCompat.setCallback(mediaSessionCallback);
		mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
		mediaSessionCompat.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
		mediaSessionCompat.setActive(true);

		//=====================> 락스크린?
		// TODO: 30.06.2017 zastąpić Activity
		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("notification", true);

		//PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT 또는 PendingIntent.FLAG_IMMUTABLE
		//PendingIntent pi = PendingIntent.getActivity(this, 99 /*request code*/, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		//--
		PendingIntent pi = PendingIntentUtils.getActivity(this, 99 /*request code*/, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		mediaSessionCompat.setSessionActivity(pi);

		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
		//PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
		//--
		PendingIntent pendingIntent = PendingIntentUtils.getBroadcast(this, 0, mediaButtonIntent, 0);

		mediaSessionCompat.setMediaButtonReceiver(pendingIntent);

		setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

		setSessionToken(mediaSessionCompat.getSessionToken());
	}

	private MediaSessionCompat mediaSessionCompat;

	private PlaybackStateCompat.Builder playbackstateBuilder;

	private PlaybackStateCompat playbackStateCompat;

	public static String NOTIFICATION_CHANNEL_ID = "123112341_odii";
	private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {


		@Override
		public void onPlay() {
			super.onPlay();
			KLog.i("PlayerService", "onPlay");
			userPause = false;
			isFloating = false;
			setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
			play();
		}

		@Override
		public void onPause() {
			super.onPause();
			KLog.i("PlayerService", "onPause");
			setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
			userPause = true;
			pause();
		}

		@Override
		public void onStop() {
			super.onStop();
			KLog.i("PlayerService", "onStop");
			sendBroad(new Intent(PlayerConstants.ACTION_NOTIFICATION_MUSIC_CONTORLL_CHANGE));
			userPause = false;
			isFloating = false;
			stop();
		}

		@Override
		public void onSkipToNext() {
			KLog.i("PlayerService", "onSkipToNext");
			sendBroad(new Intent(PlayerConstants.ACTION_NOTIFICATION_MUSIC_CONTORLL_CHANGE));
			super.onSkipToNext();
			userPause = false;
			isFloating = false;
			nextSong();
		}

		@Override
		public void onSkipToPrevious() {
			KLog.i("PlayerService", "onSkipToPrevious");
			sendBroad(new Intent(PlayerConstants.ACTION_NOTIFICATION_MUSIC_CONTORLL_CHANGE));
			super.onSkipToPrevious();
			userPause = false;
			isFloating = false;
			previousSong();
		}

		@Override
		public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
			KLog.d("PlayerService", "onMediaButtonEvent called: " + mediaButtonEvent);

			//락스크린?
			KeyEvent ke = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (ke != null && ke.getAction() == KeyEvent.ACTION_DOWN && ke.getKeyCode() == KeyEvent.KEYCODE_MEDIA_STOP) {
				userPause = true;

				stop();
			}

			KLog.i("PlayerService", "onMediaButtonEvent : " + mediaButtonEvent.getAction());
			return super.onMediaButtonEvent(mediaButtonEvent);
		}

		/**
		 * 탐색바 이용
		 * setActions PlaybackStateCompat.ACTION_SEEK_TO 대응 추가
		 * MediaSessionCallback 구현
		 */
		@Override
		public void onSeekTo(long pos) {
			super.onSeekTo(pos);
			//연동추가
			userPause = false;
			isFloating = false;
			//milisecond -seekTo(pos);
			if (player != null) {
				player.getCurrentPlayer().seekTo(pos);
			}

		}
	};

	private void setMediaPlaybackState(int state) {

		//PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
		playbackstateBuilder = new PlaybackStateCompat.Builder();

		switch (state) {
			case PlaybackStateCompat.STATE_PLAYING: {
				//playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);

				/**
				 * 탐색바 이용
				 * setActions PlaybackStateCompat.ACTION_SEEK_TO 추가
				 * MediaSessionCallback 구현 추가
				 */
				playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SEEK_TO);
				break;
			}
			default: {
				playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
				break;
			}
		}

		//현재 밀리서컨드
		//PlayerConstants.ACTUAL_TIME_VALUE_EXTRA, player.getCurrentPosition();
		long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
		if(player!=null){
			int currentPosition = player.getCurrentPosition();
			position = (long)currentPosition; //mils플레이어의 currentPosition
		}

		playbackstateBuilder.setState(state, position, 0);

		//mediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
		playbackStateCompat = playbackstateBuilder.build();
		mediaSessionCompat.setPlaybackState(playbackStateCompat);

	}

	private void updateMediaPlaybackStateCurrentPosition(){
		if(player!=null){
			//long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
			int currentPosition = player.getCurrentPosition();
			long position = (long)currentPosition; //mils플레이어의 currentPosition

			if(playbackstateBuilder!=null){

				int state = PlaybackStateCompat.STATE_PAUSED;
				if(player.isPlaying()){
					state = PlaybackStateCompat.STATE_PLAYING;
				}
				switch (state) {
					case PlaybackStateCompat.STATE_PLAYING: {
						//playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
						/**
						 * 탐색바 이용
						 * setActions PlaybackStateCompat.ACTION_SEEK_TO 추가
						 * MediaSessionCallback 구현 추가
						 */
						playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SEEK_TO);
						break;
					}
					default: {
						//playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
						break;
					}
				}

				if(state == PlaybackStateCompat.STATE_PLAYING){
					playbackstateBuilder.setState(state, position, 0);

					//mediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
					playbackStateCompat = playbackstateBuilder.build();
					if(mediaSessionCompat!=null){
						mediaSessionCompat.setPlaybackState(playbackStateCompat);
					}
				}


			}

		}


	}

	private Bitmap cover;
	protected int coverPlaceholderId = R.drawable.icon;
	protected int smallNotificationIconId = R.drawable.icon;
	public final static String DELETE_ACTION = "DELETE_ACTION";

	private void makeNotification() {
		if (cover == null) cover = BitmapFactory.decodeResource(getResources(), coverPlaceholderId);

		if (mediaSessionCompat == null) {
			initMediaSession();
		}

		updateMediaSessionMetaData();

		try {

			NotificationCompat.Builder builder = MediaStyleHelper.from(this, mediaSessionCompat);
			//--
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				builder.setColor(Color.parseColor("#00ffffff"));
				builder.setSmallIcon(smallNotificationIconId);
			} else {
				builder.setSmallIcon(smallNotificationIconId);
			}

			PendingIntent pplayIntent;
			if (isPlay) {
				pplayIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE);
			} else {
				pplayIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY);
			}

			builder.addAction(android.R.drawable.ic_media_previous, "previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
			if (isPlay) {
				builder.addAction(android.R.drawable.ic_media_pause, "pause", pplayIntent);
			} else {
				builder.addAction(android.R.drawable.ic_media_play, "play", pplayIntent);
			}

			builder.addAction(android.R.drawable.ic_media_next, "next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
			builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "close", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP));
			Intent deleteIntent = new Intent(this, PlayerService.class);
			deleteIntent.setAction(DELETE_ACTION);
			//PendingIntent pdeleteIntent = PendingIntent.getService(this, 0, deleteIntent, 0);
			PendingIntent pdeleteIntent;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				pdeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_IMMUTABLE);

			}else {
				pdeleteIntent = PendingIntent.getService(this, 0, deleteIntent, 0);
			}

			//락스트린x 노티 재생 컨트롤
			builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2, 3).setMediaSession(mediaSessionCompat.getSessionToken()).setShowCancelButton(true).setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)));
			builder.setDeleteIntent(pdeleteIntent);

			builder.setShowWhen(false);
			builder.setColor(ContextCompat.getColor(this, R.color.colorPrimary));

			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				int importance = NotificationManager.IMPORTANCE_LOW;
				NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
				notificationChannel.enableLights(false);
				notificationChannel.setLightColor(0xff3277ff);
				notificationChannel.enableVibration(true);
				notificationChannel.setVibrationPattern(new long[]{0});
				assert mNotificationManager != null;
				builder.setChannelId(NOTIFICATION_CHANNEL_ID);
				mNotificationManager.createNotificationChannel(notificationChannel);
			} else {
				builder.setPriority(NotificationCompat.PRIORITY_LOW);
			}

			try {
				new Handler(Looper.myLooper()).post(() -> {
					if (isPlay) {
						startForeground(345, builder.build());
					} else {
						//stopForeground(false);
						mNotificationManager.notify(345, builder.build());
					}
				});
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void cancelNotification() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(345);
		stopForeground(true);
	}

	private void updateMediaSessionMetaData() {

		StoryItem item = PlayListManager.getInstance().getStoryItem();

		if (item == null) {
			return;
		}
		//PlayerConstants.ACTUAL_TIME_VALUE_EXTRA, player.getCurrentPosition()
		long duration = 0;
		try {
			duration = Long.parseLong(item.audioPlayTime) * 1000;
		}
		catch (IllegalArgumentException e) {
			duration = 0;
		}
		catch (Exception e){
			duration = 0;
		}

		try {
			Bitmap bitmap = null;
			bitmap = Glide.with(getApplicationContext()).asBitmap().load(item.thumbnailFilePath).into(100, 100).get();

			MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
			builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.audioTitle);
			builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "오디 Odii"); //SoundCloud
			builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.title);
			builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, item.audioFilePath);
			if (bitmap != null) {
				builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
			}
			//		builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
			builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);

			mediaSessionCompat.setMetadata(builder.build());
		} catch (Exception e) {

			MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
			builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.audioTitle);
			builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "오디 Odii"); //SoundCloud
			builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.title);
			builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, item.audioFilePath);

			builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0);

			mediaSessionCompat.setMetadata(builder.build());
		}
	}


	class NotificationAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... voids) {
			updateMediaSessionMetaData();
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			makeNotification();
		}
	}

	private void NetworkStamp(int index) {
		StoryItem songItem = PlayListManager.getInstance().getStoryItem(index);
		if (songItem != null) {

			StampEventList stamp = StampDBManager.getInstance(this).checkStampStoryItem(songItem.tlid, songItem.slid);

			if(stamp == null || "Y".equals(stamp.stamp_p_yn)) {
				return;
			}


			Map<String, Object> map = new LinkedHashMap<>();

			map.put("os_type", "ANDROID");
			map.put("uuid", CCLUUIDHelper.id(getApplicationContext()));
			map.put("elid", stamp.elid);
			map.put("tlid", stamp.tlid);
			map.put("slid", stamp.slid);
			map.put("stamp_type", "P");

			ApiService.get().stamp(map).enqueue(new Callback<ResponseBody>() {
				@Override
				public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

					if (response.isSuccessful()) {
						if(OdiiApplication.getWebActivity() != null) {
							((MainActivity)OdiiApplication.getWebActivity()).loadStampPageChange();
						}
						StampDBManager.getInstance(getApplicationContext()).updateStampPlayComplete(stamp);
					} else {
						KLog.i("NetworkLog", "onResponse Failed " + response.body());
					}
				}

				@Override
				public void onFailure(Call<ResponseBody> call, Throwable t) {
					KLog.i("NetworkLog", "onFailure");
				}
			});
			return;
		}
	}
}
