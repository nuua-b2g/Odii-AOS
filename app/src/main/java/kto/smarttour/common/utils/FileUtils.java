package kto.smarttour.common.utils;

import android.content.Context;
import android.os.AsyncTask;

import com.bumptech.glide.Glide;
import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Security;
import java.util.List;

import kto.smarttour.db.item.StoryItem;

public class FileUtils {

	/**
	 * 파일 복사
	 *
	 * @param context  Context
	 * @param file     원본 이미지 경로
	 * @param fileName 저장할 파일 이름
	 * @return
	 */
	public static boolean copyFile(Context context, File file, String fileName) {
		boolean result;
		if (file != null && file.exists()) {
			try {
				/*
				File save_file = new File(context.getFilesDir() + File.separator + "ODII" + File.separator + fileName);
				File directory = new File(context.getFilesDir() + File.separator + "ODII");
				*/

				try {

					String t_save_file = context.getFilesDir() + File.separator + "ODII" + File.separator + fileName;
					File save_file = new File(t_save_file);

					ensureZipPathSafety(save_file,t_save_file); //확인오류시 throw발생

					String t_directory = context.getFilesDir() + File.separator + "ODII";
					File directory = new File(t_directory);

					ensureZipPathSafety(directory,t_directory); //확인오류시 throw발생

					if (!directory.exists()) {
						directory.mkdirs();
						save_file.createNewFile();
					} else if (!save_file.exists()) {
						save_file.createNewFile();
					}

					FileInputStream fis = new FileInputStream(file);
					FileOutputStream newfos = new FileOutputStream(save_file);
					int readcount = 0;
					byte[] buffer = new byte[1024];
					while ((readcount = fis.read(buffer, 0, 1024)) != -1) {
						newfos.write(buffer, 0, readcount);
					}
					newfos.close();
					fis.close();

				} catch (SecurityException e) {
					//
				} catch (Exception e) {
					//e.printStackTrace();
				}


			} catch (Exception e) {
				e.printStackTrace();
			}
			result = true;
		} else {
			result = false;
		}
		return result;
	}

	/**
	 * @param context
	 * @param url     다운로드할 파일 인터넷 URL
	 * @return 파일경로 및 파일 이름
	 */
	public static String[] getSaveFileInfo(Context context, int tid, String url) {

		/*
		String[] fileInfo = new String[2];
		String fileName = url.substring(url.lastIndexOf("/") + 1);
		*/

		String[] fileInfo = new String[2];
		try {
			String fileName = url.substring(url.lastIndexOf("/") + 1);


			String t_directory = context.getFilesDir() + File.separator + "ODII" + File.separator + tid;
			File directory = new File(t_directory);

			ensureZipPathSafety(directory,t_directory); //확인오류시 throw발생

			if (!directory.exists()) {
				directory.mkdirs();
			}

			fileInfo[0] = directory.getAbsolutePath();
			fileInfo[1] = fileName;
		} catch (SecurityException e) {
			//
		}
		catch (Exception e) {
		}
		return fileInfo;
	}

	public static void clearCacheStoryDelete(Context context) {

		//File directory = new File(context.getFilesDir() + File.separator + "ODII" + File.separator);

		try {

			String t_directory = context.getFilesDir() + File.separator + "ODII" + File.separator;
			File directory = new File(t_directory);
			ensureZipPathSafety(directory,t_directory); //확인오류시 throw발생

			deleteDir(directory);

			Glide.get(context).clearMemory();
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... voids) {
					Glide.get(context).clearDiskCache();
					return null;
				}
			}.execute();

		} catch (SecurityException e) {
			//
		} catch (Exception e) {
			//e.printStackTrace();
		}

	}

	public static void setGlideCacheClear(Context context) {
		Glide.get(context).clearMemory();
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... voids) {
				Glide.get(context).clearDiskCache();
				return null;
			}
		}.execute();
	}

	static void deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) {
				deleteDir(f);
			}
		}
		file.delete();
	}

	/**
	 * 파일 비교
	 *
	 * @param context
	 * @param item
	 * @return
	 */
	public static boolean compare(Context context, StoryItem item) {

		try {

			/*
			String fileName = item.audioFilePath.substring(item.audioFilePath.lastIndexOf("/") + 1);
			File file = new File(context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName);
			*/

			String fileName = item.audioFilePath.substring(item.audioFilePath.lastIndexOf("/") + 1);

			String t_file = context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName;
			File file = new File(t_file);
			ensureZipPathSafety(file,t_file); //확인오류시 throw발생

			if (file.exists()) {

				if (file.length() == item.audioFileSize) {
					return true;
				} else {
					file.delete();
					return false;
				}
			}
		} catch (SecurityException e) {
			//
		} catch (Exception e) {
		}

		return false;
	}

	public static String getFileName(Context context, StoryItem item) {
		String fileName = item.thumbnailFilePath.substring(item.thumbnailFilePath.lastIndexOf("/") + 1);
		return context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName;
	}

	public static String getFileName(Context context, int tid, String item) {
		String fileName = item.substring(item.lastIndexOf("/") + 1);
		return context.getFilesDir() + File.separator + "ODII" + File.separator + tid + File.separator + fileName;
	}

	public static void removeFile(Context context, StoryItem item) {
		try {
			String fileName = item.audioFilePath.substring(item.audioFilePath.lastIndexOf("/") + 1);

			/*
			File file = new File(context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName);
			*/

			String t_file = context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName;
			File file = new File(t_file);
			ensureZipPathSafety(file,t_file); //확인오류시 throw발생

			if (file.exists()) {
				file.delete();
			}

			String fileName2 = item.thumbnailFilePath.substring(item.thumbnailFilePath.lastIndexOf("/") + 1);

			//File file2 = new File(context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName2);

			String t_file2 = context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName2;
			File file2 = new File(t_file2);
			ensureZipPathSafety(file2,t_file2); //확인오류시 throw발생

			if (file2.exists()) {
				file2.delete();
			}
		} catch (SecurityException e) {
			//
		} catch (Exception e) {
		}

	}

	public static void removeFile(Context context, List<StoryItem> items) {
		for (StoryItem item : items) {
			try {
				String fileName = item.audioFilePath.substring(item.audioFilePath.lastIndexOf("/") + 1);

				//File file = new File(context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName);

				String t_file = context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName;
				File file = new File(t_file);
				ensureZipPathSafety(file,t_file); //확인오류시 throw발생

				if (file.exists()) {
					file.delete();
				}

				String fileName2 = item.thumbnailFilePath.substring(item.thumbnailFilePath.lastIndexOf("/") + 1);

				//File file2 = new File(context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName2);

				String t_file2 = context.getFilesDir() + File.separator + "ODII" + File.separator + item.tid + File.separator + fileName2;
				File file2 = new File(t_file2);
				ensureZipPathSafety(file2,t_file2); //확인오류시 throw발생

				if (file2.exists()) {
					file2.delete();
				}
			} catch (SecurityException e) {
				//
			} catch (Exception e) {
			}
		}
	}

	public static void updateThumbnail(Context context, StoryItem sourceItem, StoryItem targetItem) {

		/*
		String fileName2 = sourceItem.thumbnailFilePath.substring(sourceItem.thumbnailFilePath.lastIndexOf("/") + 1);
		File file2 = new File(context.getFilesDir() + File.separator + "ODII" + File.separator + sourceItem.tid + File.separator + fileName2);
		*/

		try {

			String fileName2 = sourceItem.thumbnailFilePath.substring(sourceItem.thumbnailFilePath.lastIndexOf("/") + 1);

			//File file2 = new File(context.getFilesDir() + File.separator + "ODII" + File.separator + sourceItem.tid + File.separator + fileName2);

			String t_file2 = context.getFilesDir() + File.separator + "ODII" + File.separator + sourceItem.tid + File.separator + fileName2;
			File file2 = new File(t_file2);

			FileUtils.ensureZipPathSafety(file2,t_file2);//확인오류시 throw발생

			if (file2.exists()) {
				file2.delete();
			}

			// 이미지 다운로드
			String[] thumbFile = FileUtils.getSaveFileInfo(context, targetItem.tid, targetItem.thumbnailFilePath);
			PRDownloader.download(targetItem.thumbnailFilePath, thumbFile[0], thumbFile[1]).build().start(new OnDownloadListener() {
				@Override
				public void onDownloadComplete() {
				}

				@Override
				public void onError(Error error) {
				}
			});

		} catch (SecurityException e) {
			//
		} catch (Exception e) {
			//e.printStackTrace();
		}

	}

	public static String getImageUrl(Context context, String imageFileName) {
		return context.getFilesDir() + File.separator + "ODII" + File.separator + imageFileName;
	}

	public static void LogWrite(String tag, String logs) {
		//		KLog.i(tag, logs);
		//		File file = new File(Environment.getExternalStorageDirectory() + "/odii_stamp_log.txt");
		//		try {
		//			if (!file.exists()) {
		//				file.createNewFile();
		//			}
		//			RandomAccessFile raf = new RandomAccessFile(file, "rw");
		//			raf.seek(file.length());
		//			raf.writeUTF(SystemUtils.getCurrentTime() + "-----" + logs + "\n\n");
		//			raf.close();
		//		} catch (IOException e) {
		//		}
	}

	//Fixing a Zip Path Traversal Vulnerability In Android 체크 추가 (2022. 02. 04)
	public static void ensureZipPathSafety(final File outputFile, final String destDirectory) throws Exception {
		String destDirCanonicalPath = (new File(destDirectory)).getCanonicalPath();
		String outputFilecanonicalPath = outputFile.getCanonicalPath();
		if( !outputFilecanonicalPath.startsWith(destDirCanonicalPath) ) {
			//throw new Exception(String.format("Found Zip Path Traversal Vulnerability with %s", outputFilecanonicalPath));
			throw new SecurityException("Found Zip Path Traversal Vulnerability Found");
		}
	}
}
