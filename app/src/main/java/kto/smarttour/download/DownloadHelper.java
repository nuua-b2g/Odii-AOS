package kto.smarttour.download;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.google.android.exoplayer2.offline.Download;

import java.io.File;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.FileUtils;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.WorkerChain;
import kto.smarttour.db.StoryDbManager;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryItem;
import kto.smarttour.ui.MainActivity;

public class DownloadHelper {
	private ProgressBar progressBar;
	private AppCompatActivity activity;
	private AlertDialog dialog;
	private WorkerChain mWorkerChain;
	private Handler handler;

	private float totalCount = 0;
	private float currentCount = 0;
	private int file_download_type;
	private String langCode;

	//--
	private TextView tvPercentage;
	//--
	public DownloadHelper(AppCompatActivity activity, ProgressBar progressBar, AlertDialog dialog, Handler handler, int file_download_type, TextView tvPercentage) {
		this.progressBar = progressBar;
		this.activity = activity;
		this.dialog = dialog;
		this.handler = handler;
		this.file_download_type = file_download_type;

		this.tvPercentage = tvPercentage;
		init();
	}

	public DownloadHelper(AppCompatActivity activity, ProgressBar progressBar, AlertDialog dialog, Handler handler, int file_download_type) {

		this.progressBar = progressBar;
		this.activity = activity;
		this.dialog = dialog;
		this.handler = handler;
		this.file_download_type = file_download_type;
		init();
	}

	private void init() {
		PRDownloaderConfig config = PRDownloaderConfig.newBuilder().setDatabaseEnabled(false).setReadTimeout(30_000).setConnectTimeout(30_000).build();
		PRDownloader.initialize(activity, config);
	}

	public void cancel() {
		PRDownloader.cancelAll();
		if(mWorkerChain != null) {
			mWorkerChain.clearWork();
			if(handler != null) {
				handler.sendMessage(handler.obtainMessage(-1, new FileDownLoadType(file_download_type, (int)currentCount, langCode)));
			}
		}
	}

	public void start(StoryData downloadList) {
		totalCount = downloadList.story.size();
		mWorkerChain = new WorkerChain();

		if(!downloadList.story.isEmpty()) {
			langCode = downloadList.story.get(0).langCode;
		}


		for (StoryItem downloadItem : downloadList.story) {
			mWorkerChain.add(new WorkerChain.SimpleWorker(downloadItem.aid, "Download") {
				@Override
				public void work() {
					download(downloadItem);
				}
			});
		}

		if (dialog != null) {
			mWorkerChain.add(new WorkerChain.SimpleWorker("Complete", "Complete") {
				@Override
				public void work() {
					dialog.dismiss();
					if(handler != null) {
						handler.sendMessage(handler.obtainMessage(0, new FileDownLoadType(file_download_type, (int)currentCount, langCode)));
					} else {
						Toast.makeText(activity, R.string.downlaod_complete, Toast.LENGTH_SHORT).show();
					}
					if(((MainActivity) OdiiApplication.getWebActivity()) != null) {
						((MainActivity) OdiiApplication.getWebActivity()).refreshDownloadCount();
					}

					currentCount = 0;

				}
			});
		}
		mWorkerChain.workNext();
	}

	private void download(StoryItem downloadItem) {
		// MP3 다운로드
		String[] mp3File = FileUtils.getSaveFileInfo(activity, downloadItem.tid, downloadItem.audioFilePath);
		String[] thumbFile = FileUtils.getSaveFileInfo(activity, downloadItem.tid, downloadItem.thumbnailFilePath);

		// 동일한 파일 존재시 생략
		if (FileUtils.compare(activity, downloadItem)) {

			//	파일 다운로드 - 중복된 경로에 동일한 파일이 존재하는 경우
			//  1. 파일명 변경 후 저장
			//	2. DB에 변경된 내용으로 저장
			//	다운로드가 완료된 경우 파일명을 새로 생성하여 파일 복사
			mp3File[1] = "dup_" + mp3File[1];
			PRDownloader.download(downloadItem.audioFilePath, mp3File[0], mp3File[1]).build().setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel() {
					mWorkerChain.clearWork();
					currentCount = 0;

					//Zip Path Traversal Vulnerability 체크
					String t_mp3Path = mp3File[0] + File.separator + mp3File[1];
					File t_mp3File = new File(t_mp3Path);
					try {
						FileUtils.ensureZipPathSafety(t_mp3File,t_mp3Path);
						t_mp3File.delete();
					} catch (SecurityException e) {
						//
					} catch (Exception e) {
						//e.printStackTrace();
					}

					String t_thumbPath = thumbFile[0] + File.separator + thumbFile[1];
					File t_thumbFile = new File(t_thumbPath);
					try {
						FileUtils.ensureZipPathSafety(t_thumbFile,t_thumbPath);
						t_thumbFile.delete();
					} catch (SecurityException e) {
						//
					} catch (Exception e) {
						//e.printStackTrace();
					}


				}
			}).start(new OnDownloadListener() {
				@Override
				public void onDownloadComplete() {
					currentCount++;

					String fileName = downloadItem.audioFilePath.substring(downloadItem.audioFilePath.lastIndexOf("/") + 1);
					String dup_audioFilePath = downloadItem.audioFilePath.substring(0,downloadItem.audioFilePath.indexOf(fileName));
					downloadItem.audioFilePath = dup_audioFilePath + mp3File[1];

					if(SettingsUtil.getTaxiTtid(activity) != -1) {
						StoryDbManager.getInstance(activity).insertDownLoad2(downloadItem);
					} else {
						StoryDbManager.getInstance(activity).insertDownLoad(downloadItem);
					}

					new Handler(Looper.getMainLooper()).post(() ->{

						int progressValue = (int) (currentCount / totalCount * 100);
						progressBar.setProgress(progressValue);
						if(tvPercentage!=null){
							tvPercentage.setText(progressValue+"%");
						}
					} );

					mWorkerChain.workNext();
				}

				@Override
				public void onError(Error error) {
					mWorkerChain.clearWork();

					//Zip Path Traversal Vulnerability 체크
					String t_mp3Path = mp3File[0] + File.separator + mp3File[1];
					File t_mp3File = new File(t_mp3Path);
					try {
						FileUtils.ensureZipPathSafety(t_mp3File,t_mp3Path);
						t_mp3File.delete();
					} catch (SecurityException e) {
						//
					} catch (Exception e) {
						//e.printStackTrace();
					}

					String t_thumbPath = thumbFile[0] + File.separator + thumbFile[1];
					File t_thumbFile = new File(t_thumbPath);
					try {
						FileUtils.ensureZipPathSafety(t_thumbFile,t_thumbPath);
						t_thumbFile.delete();
					} catch (SecurityException e) {
						//
					} catch (Exception e) {
						//e.printStackTrace();
					}

					DialogUtil.showWarning(activity, R.string.ok, R.string.network_error, R.string.ok);
				}
			});

			// 이미지 다운로드
			PRDownloader.download(downloadItem.thumbnailFilePath, thumbFile[0], thumbFile[1]).build().start(new OnDownloadListener() {
				@Override
				public void onDownloadComplete() {
				}

				@Override
				public void onError(Error error) {
				}
			});

			return;
		}

		PRDownloader.download(downloadItem.audioFilePath, mp3File[0], mp3File[1]).build().setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel() {
				mWorkerChain.clearWork();
				currentCount = 0;

				//Zip Path Traversal Vulnerability 체크
				String t_mp3Path = mp3File[0] + File.separator + mp3File[1];
				File t_mp3File = new File(t_mp3Path);
				try {
					FileUtils.ensureZipPathSafety(t_mp3File,t_mp3Path);
					t_mp3File.delete();
				} catch (SecurityException e) {
					//
				} catch (Exception e) {
					//e.printStackTrace();
				}

				String t_thumbPath = thumbFile[0] + File.separator + thumbFile[1];
				File t_thumbFile = new File(t_thumbPath);
				try {
					FileUtils.ensureZipPathSafety(t_thumbFile,t_thumbPath);
					t_thumbFile.delete();
				} catch (SecurityException e) {
					//
				} catch (Exception e) {
					//e.printStackTrace();
				}

			}
		}).start(new OnDownloadListener() {
			@Override
			public void onDownloadComplete() {
				currentCount++;
				if(SettingsUtil.getTaxiTtid(activity) != -1) {
					StoryDbManager.getInstance(activity).insertDownLoad2(downloadItem);
				} else {
					StoryDbManager.getInstance(activity).insertDownLoad(downloadItem);
				}

				new Handler(Looper.getMainLooper()).post(() ->{

					int progressValue = (int) (currentCount / totalCount * 100);
					progressBar.setProgress(progressValue);
					if(tvPercentage!=null){
						tvPercentage.setText(progressValue+"%");
					}
				} );



				mWorkerChain.workNext();
			}

			@Override
			public void onError(Error error) {
				mWorkerChain.clearWork();

				//Zip Path Traversal Vulnerability 체크
				String t_mp3Path = mp3File[0] + File.separator + mp3File[1];
				File t_mp3File = new File(t_mp3Path);
				try {
					FileUtils.ensureZipPathSafety(t_mp3File,t_mp3Path);
					t_mp3File.delete();
				} catch (SecurityException e) {
					//
				} catch (Exception e) {
					//e.printStackTrace();
				}

				String t_thumbPath = thumbFile[0] + File.separator + thumbFile[1];
				File t_thumbFile = new File(t_thumbPath);
				try {
					FileUtils.ensureZipPathSafety(t_thumbFile,t_thumbPath);
					t_thumbFile.delete();
				} catch (SecurityException e) {
					//
				} catch (Exception e) {
					//e.printStackTrace();
				}

				DialogUtil.showWarning(activity, R.string.ok, R.string.network_error, R.string.ok);
			}
		});

		// 이미지 다운로드
		PRDownloader.download(downloadItem.thumbnailFilePath, thumbFile[0], thumbFile[1]).build().start(new OnDownloadListener() {
			@Override
			public void onDownloadComplete() {
			}

			@Override
			public void onError(Error error) {
			}
		});
	}
}
