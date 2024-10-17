package kto.smarttour.common.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

public class CCLUUIDHelper {
	private static String uuid = null;
	private static final String INSTALLATION = "INSTALLATION";
	private static Object LOCK = CCLUUIDHelper.class;

	public synchronized static String id(Context context) {
		if (uuid == null) {
			synchronized (LOCK) {
				if (uuid == null) {
					//File installation = new File(context.getFilesDir(), INSTALLATION);

					String t_installation = context.getFilesDir() + File.separator + INSTALLATION;
					File installation = new File(context.getFilesDir(), INSTALLATION);

					try {
						FileUtils.ensureZipPathSafety(installation,t_installation);

						if (!installation.exists()) {
							writeInstallationFile(installation);
						}
						uuid = readInstallationFile(installation);
					} catch (SecurityException e) {
						throw new RuntimeException(e);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				}
			}
		}
		return uuid;
	}

	private static String readInstallationFile(File installation) throws IOException {
		RandomAccessFile f = new RandomAccessFile(installation, "r");
		byte[] bytes = new byte[(int) f.length()];
		f.readFully(bytes);
		f.close();
		return new String(bytes);
	}

	private static void writeInstallationFile(File installation) throws IOException {
		FileOutputStream out = new FileOutputStream(installation);
		String id = UUID.randomUUID().toString();
		out.write(id.getBytes());
		out.close();
	}

}