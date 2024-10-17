package kto.smarttour.download;

public class FileDownLoadType {
	int fileType;
	int count;
	String langCode;

	public FileDownLoadType(int fileType, int count, String langCode) {
		this.fileType = fileType;
		this.count = count;
		this.langCode = langCode;
	}

	public int getFileType() {
		return fileType;
	}

	public int getCount() {
		return count;
	}

	public String getLangCode() {
		return langCode;
	}
}
