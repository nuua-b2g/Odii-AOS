package kto.smarttour.service;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.List;

import hybridmediaplayer.ExoMediaPlayer;
import hybridmediaplayer.HybridMediaPlayer;
import hybridmediaplayer.MediaSourceInfo;
import kto.smarttour.R;
import kto.smarttour.common.consts.Common;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.PreferenceUtils;
import kto.smarttour.common.utils.TourTaxiAnalytics;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.ui.MainActivity;
import kto.smarttour.ui.player.PlayListManager;
import kto.smarttour.ui.player.TaxiDriverPlayer;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;

public class TaxiPlayerService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener, HybridMediaPlayer.OnPreparedListener, HybridMediaPlayer.OnCompletionListener {

	public static final int STORY_MSG_HANDLER = 0;
	public static final int STORY_MSG_RECEIVE_PLAY = 1;
	public static final int STORY_MSG_RECEIVE_PAUSE = 2;
	public static final int STORY_MSG_RECEIVE_NEXT = 3;
	public static final int STORY_MSG_RECEIVE_PREV = 4;
	public static final int STORY_MSG_RECEIVE_POS = 5;
	public static final int STORY_MSG_RECEIVE_PLAY_INDEX = 6;
	public static final int STORY_MSG_RECEIVE_STATE_INFO = 7;
	public static final int STORY_MSG_RECEIVE_STOP = 8;

	public static final int STORY_MSG_SEND_INFO = 0;

	public static boolean isPlay = false;

	public ExoMediaPlayer player;
	//story list
	private ArrayList<StoryItem> storyList;
	//--
	public static boolean isStoryListComplete;

	//current position
	private int storyPosn;
	//binder
	private Messenger mMessenger;

	private Messenger activityHandler = null;

	private Thread updateThread;
	private Handler mainThreadHandler;
	private boolean isUpdatingThread;
	private boolean isPrepared;

	private MediaSessionCompat mediaSessionCompat;


	class StoryInfoHandler extends Handler {
		private Context applicationContext;

		StoryInfoHandler(Context context) {
			applicationContext = context.getApplicationContext();
		}

		@Override
		public void handleMessage(@NonNull Message msg) {
			switch (msg.what) {
				case STORY_MSG_HANDLER:
					activityHandler = msg.replyTo;
					break;
				case STORY_MSG_RECEIVE_PLAY:
					storyPlay();
					break;
				case STORY_MSG_RECEIVE_PLAY_INDEX:
					storyPlay(msg.arg1);
					break;
				case STORY_MSG_RECEIVE_PAUSE:
					storyPause();
					break;
				case STORY_MSG_RECEIVE_NEXT:
					storyNext();
					break;
				case STORY_MSG_RECEIVE_PREV:
					storyPrev();
					break;
				case STORY_MSG_RECEIVE_POS:
					storyPosn = msg.arg1;
					break;
				case STORY_MSG_RECEIVE_STATE_INFO:
					try {
						if (player != null && player.isPlaying()) {
							activityHandler.send(Message.obtain(null, TaxiDriverPlayer.STORY_MSG_RECEIVE_STORY_PLAY, storyPosn, 0));
						} else {
							activityHandler.send(Message.obtain(null, TaxiDriverPlayer.STORY_MSG_RECEIVE_STORY_PAUSE, -1, 0));
						}
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					break;
				case STORY_MSG_RECEIVE_STOP:
					try {
						activityHandler.send(Message.obtain(null, TaxiDriverPlayer.STORY_MSG_RECEIVE_STORY_PAUSE, -1, 0));
						release();
					} catch (RemoteException e) {
					}
					break;
			}

			super.handleMessage(msg);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		storyList = StoryDbManager.getInstance(this).getPlayList().story;
		mainThreadHandler = new Handler();
		initMediaSession();
	}

	//--------------------------Notification--------------------------
	private void initMediaSession() {
		ComponentName mediaButtonReceiver = new ComponentName(this, MediaButtonReceiver.class);
		mediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "MediaTAG", mediaButtonReceiver, null);

		mediaSessionCompat.setCallback(mediaSessionCallback);
		mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
		mediaSessionCompat.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
		mediaSessionCompat.setActive(true);

		setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

		setSessionToken(mediaSessionCompat.getSessionToken());
	}

	private void initStoryLoad() {
		storyList = StoryDbManager.getInstance(this).getPlayList().story;
	}

	private ExoMediaPlayer makePlayer() {
		ExoMediaPlayer player = new ExoMediaPlayer(this);
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
		return player;
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
			storyPause();
		} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

		} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
			storyPause();
			abandonAudioFocus();
			mediaSessionCompat.setActive(false);
			setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
		} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
			player.setVolume(0.1f);
		}
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
	public void onCompletion(HybridMediaPlayer player) {
		try {
			//도착 히스토리 롼료시
			if(isStoryListComplete){
				activityHandler.send(Message.obtain(null, TaxiDriverPlayer.STORY_MSG_RECEIVE_STORY_LIST_COMPLETE));
			}


			activityHandler.send(Message.obtain(null, TaxiDriverPlayer.STORY_MSG_RECEIVE_STORY_COMPLETE, storyPosn, 0));
		} catch (RemoteException e) {
		}
		release();
	}

	@Override
	public void onPrepared(HybridMediaPlayer player) {
		if (!retrieveAudioFocus()) return;

		isPrepared = true;
		player.play();

		startUiUpdateThread();
		try {
			activityHandler.send(Message.obtain(null, TaxiDriverPlayer.STORY_MSG_RECEIVE_STORY_PLAY, -1, 0));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		mMessenger = new Messenger(new StoryInfoHandler(this));
		return mMessenger.getBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		release();
		return false;
	}

	@Override
	public void onDestroy() {
		isUpdatingThread = false;
		abandonAudioFocus();
		stopForeground(true);
	}

	public void release() {
		isUpdatingThread = false;
		if (player != null) {
			player.stop();
			player.release();
			player = null;
		}
	}


	private void storyPlay() {
		if (!retrieveAudioFocus()) return;

		if (player == null) {
			isPrepared = true;
			player = makePlayer();
			initStoryLoad();

			if (storyList.size() - 1 < storyPosn) {
				storyPosn = storyList.size();
			}

			StoryItem songItem = storyList.get(storyPosn); // IOOB
			MediaSourceInfo mediaSourceInfo = new MediaSourceInfo.Builder().setUrl(FileUtils.getFileName(this, songItem.tid, songItem.audioFilePath)).setImageUrl(FileUtils.getFileName(this, songItem.tid, songItem.thumbnailFilePath)).setTitle(songItem.audioTitle).build();
			player.setDataSource(mediaSourceInfo);
			player.prepare();
			TourTaxiAnalytics.storyPlayStart(this, String.valueOf(songItem.slid), songItem.langCode);
			try {
				activityHandler.send(Message.obtain(null, TaxiDriverPlayer.STORY_MSG_RECEIVE_STORY_INFO, storyPosn, 0, songItem));
			} catch (RemoteException e) {
			}
		} else {
			try {
				activityHandler.send(Message.obtain(null, TaxiDriverPlayer.STORY_MSG_RECEIVE_STORY_PLAY, -1, 0));
			} catch (RemoteException e) {
			}
			isPrepared = true;
			StoryItem songItem = storyList.get(storyPosn);
			TourTaxiAnalytics.storyPlayStart(this, String.valueOf(songItem.slid), songItem.langCode);
			player.play();
		}
		isPlay = true;

		Common.isAudioPlaying = true;
	}

	private void storyPlay(int pos) {
		storyPosn = pos;
		release();
		storyPlay();
	}

	private void storyPause() {
		if (player != null) {
			try {
				activityHandler.send(Message.obtain(null, TaxiDriverPlayer.STORY_MSG_RECEIVE_STORY_PAUSE, -1, 0));
			} catch (RemoteException e) {
			}
			player.pause();
			isPlay = false;

			Common.isAudioPlaying = false;
		}
	}

	private void storyPrev() {
		storyPosn--;
		if (storyPosn < 0) storyPosn = storyList.size() - 1;
		release();
		storyPlay();

	}

	private void storyNext() {
		storyPosn++;
		if (storyPosn >= storyList.size()) storyPosn = 0;
		release();
		storyPlay();
	}

	private void startUiUpdateThread() {

		if (mainThreadHandler == null) {
			isUpdatingThread = false;
			return;
		}

		isUpdatingThread = true;
		if (updateThread == null) {
			updateThread = new Thread(() -> {

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				while (isUpdatingThread && isPrepared) {
					try {
						activityHandler.send(Message.obtain(null, TaxiDriverPlayer.STORY_MSG_RECEIVE_STORY_UIUPDATE, player.getCurrentPosition(), player.getDuration()));
					} catch (RemoteException e) {
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				updateThread = null;
			});

			updateThread.start();
		}
	}

	private void setMediaPlaybackState(int state) {
		PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
		switch (state) {
			case PlaybackStateCompat.STATE_PLAYING: {
				playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
				break;
			}
			default: {
				playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
				break;
			}
		}

		playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
		mediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
	}

	private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {


		@Override
		public void onPlay() {
			super.onPlay();
			if (storyList == null || storyList.isEmpty()) {
				return;
			}
			setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
			storyPlay();
		}

		@Override
		public void onPause() {
			super.onPause();
			if (storyList == null || storyList.isEmpty()) {
				return;
			}
			setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
			storyPause();
		}

		@Override
		public void onStop() {
			super.onStop();
			release();
		}

		@Override
		public void onSkipToNext() {
			super.onSkipToNext();
			if (storyList == null || storyList.isEmpty()) {
				return;
			}
			storyNext();
		}

		@Override
		public void onSkipToPrevious() {
			super.onSkipToPrevious();
			if (storyList == null || storyList.isEmpty()) {
				return;
			}
			storyPrev();
		}

		@Override
		public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
			KeyEvent ke = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (ke != null && ke.getAction() == KeyEvent.ACTION_DOWN && ke.getKeyCode() == KeyEvent.KEYCODE_MEDIA_STOP) {
				release();
			}
			return super.onMediaButtonEvent(mediaButtonEvent);
		}
	};

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

}
