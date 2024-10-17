package kto.smarttour.common.utils;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.text.DecimalFormat;

/**
 * 저장소 용량 체크.
 *
 * @author Changhyun Jeon
 * @since 2015. 3. 24
 */
@SuppressWarnings("deprecation")
public class StorageUtil {

	/**
	 * Checks if is external memory available.
	 *
	 * @return true, if successful
	 */
	public static boolean IsExternalMemoryAvailable() {
		return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}

	/**
	 * Gets the total internal memory size.
	 *
	 * @return the long
	 */
	public static long GetTotalInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();

		return totalBlocks * blockSize;
	}

	/**
	 * Gets the available internal memory size.
	 *
	 * @return the long
	 */
	public static long GetAvailableInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();

		return availableBlocks * blockSize;
	}

	/**
	 * Gets the total external memory size.
	 *
	 * @return the long
	 */
	public static long GetTotalExternalMemorySize() {
		if (IsExternalMemoryAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long totalBlocks = stat.getBlockCount();

			return totalBlocks * blockSize;
		} else {
			return -1;
		}
	}

	/**
	 * Gets the available external memory size.
	 *
	 * @return the long
	 */
	public static long GetAvailableExternalMemorySize() {
		if (IsExternalMemoryAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long availableBlocks = stat.getAvailableBlocks();

			return availableBlocks * blockSize;
		} else {
			return -1;
		}
	}

	public static String getFileSize(long size) {
		String retFormat = "0";

		String[] s = { "bytes", "KB", "MB", "GB"};


		if (size != 0) {
			int idx = (int) Math.floor(Math.log(size) / Math.log(1024));
			DecimalFormat df = new DecimalFormat("#,###.##");
			double ret = ((size / Math.pow(1024, Math.floor(idx))));
			retFormat = df.format(ret) + " " + s[idx];
		} else {
			retFormat += " " + s[0];
		}

		return retFormat;


		/*
		String gubn[] = {"Byte", "KB", "MB"};
		String returnSize = new String();
		int gubnKey = 0;
		double changeSize = 0;
		long fileSize = 0;
		try {
			fileSize = size;
			for (int x = 0; (fileSize / (double) 1024) > 0; x++, fileSize /= (double) 1024) {
				gubnKey = x;
				changeSize = fileSize;
			}
			returnSize = String.format("%.2f %s", changeSize, gubn[gubnKey]);
		} catch (Exception ex) {
			returnSize = "";
		}
		return returnSize;
		 */
	}

}
