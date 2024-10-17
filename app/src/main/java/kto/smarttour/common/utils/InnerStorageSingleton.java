package kto.smarttour.common.utils;

public class InnerStorageSingleton {

	private static InnerStorageSingleton singleton;

	public static synchronized InnerStorageSingleton getSingleton() {
		if (singleton == null) {
			singleton = new InnerStorageSingleton();
		}
		return singleton;
	}

	public InnerStorageSingleton() {
	}

	private Object obj;

	public void setData(Object obj) {
		this.obj = obj;
	}

	public Object getData() {
		Object temp = obj;
		obj = null;
		return temp;
	}
}
