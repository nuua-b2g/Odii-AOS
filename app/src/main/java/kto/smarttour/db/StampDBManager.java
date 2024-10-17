package kto.smarttour.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import kto.smarttour.geo.GeofenceController;
import kto.smarttour.network.response.dao.StampEventList;

public class StampDBManager extends SQLiteOpenHelper {

	public static StampDBManager dbmgr;
	public Context con;

	enum TABLE_NAME {
		STAMP
	}

	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "stamp.db";

	public static StampDBManager getInstance(Context con) {
		if (dbmgr == null) {
			dbmgr = new StampDBManager(con, null, null, DB_VERSION);
		}
		dbmgr.con = con;
		return dbmgr;
	}


	public StampDBManager(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
		super(context, DB_NAME, null, version);
		this.con = context;
		getWritableDatabase();
	}

	/**
	 * 테이블생성
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuilder STAMP = new StringBuilder();
		STAMP.append("CREATE TABLE " + StampDBManager.TABLE_NAME.STAMP.toString() + " ");
		STAMP.append("(" + "_ID INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL " + "UNIQUE, ");  //_id unique한 값
		STAMP.append("end_date TEXT, ");
		STAMP.append("eid TEXT, ");
		STAMP.append("slid TEXT, ");
		STAMP.append("tlid TEXT, ");
		STAMP.append("stamp_p_yn TEXT, ");
		STAMP.append("elid TEXT, ");
		STAMP.append("tid TEXT, ");
		STAMP.append("pos_y TEXT, ");
		STAMP.append("pos_x TEXT, ");
		STAMP.append("stamp_v_yn TEXT, ");
		STAMP.append("end_date_millis TEXT, ");
		STAMP.append("link_url TEXT, ");
		STAMP.append("radius TEXT, ");
		STAMP.append("start_date_millis TEXT, ");
		STAMP.append("start_date TEXT)");
		db.execSQL(STAMP.toString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		if (oldVersion < newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME.STAMP.toString());
		}
		onCreate(db);
	}

	public void insert(ArrayList<StampEventList> dao) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			for (int i = 0; i < dao.size(); i++) {
				ContentValues values = new ContentValues();
				values.put("end_date", dao.get(i).end_date);
				values.put("eid", dao.get(i).eid);
				values.put("slid", dao.get(i).slid);
				values.put("tlid", dao.get(i).tlid);
				values.put("stamp_p_yn", dao.get(i).stamp_p_yn);
				values.put("elid", dao.get(i).elid);
				values.put("tid", dao.get(i).tid);

				values.put("pos_y", dao.get(i).posY);
				values.put("pos_x", dao.get(i).posX);

				values.put("stamp_v_yn", dao.get(i).stamp_v_yn);
				values.put("end_date_millis", dao.get(i).endDateMillis);
				values.put("link_url", dao.get(i).linkUrl);
				values.put("radius", dao.get(i).radius);
				values.put("start_date_millis", dao.get(i).startDateMillis);
				values.put("start_date", dao.get(i).startDate);
				long id = db.insert(TABLE_NAME.STAMP.toString(), null, values);

			}
		} catch (SQLiteException e) {
		} finally {
			close(db, null);
		}
	}

	public ArrayList<StampEventList> getList() {

		ArrayList<StampEventList> list = new ArrayList<>();
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("SELECT * from " + TABLE_NAME.STAMP.toString(), null);

			StampEventList item;
			while (cursor.moveToNext()) {
				item = new StampEventList();
				item._id = cursor.getInt(cursor.getColumnIndex("_ID"));
				item.end_date = Long.valueOf(cursor.getString(cursor.getColumnIndex("end_date")));
				item.eid = Integer.valueOf(cursor.getString(cursor.getColumnIndex("eid")));
				item.slid = Integer.valueOf(cursor.getString(cursor.getColumnIndex("slid")));
				item.tlid = Integer.valueOf(cursor.getString(cursor.getColumnIndex("tlid")));
				item.stamp_p_yn = cursor.getString(cursor.getColumnIndex("stamp_p_yn"));
				item.elid = Integer.valueOf(cursor.getString(cursor.getColumnIndex("elid")));
				item.tid = Integer.valueOf(cursor.getString(cursor.getColumnIndex("tid")));
				item.posY = cursor.getString(cursor.getColumnIndex("pos_y"));
				item.posX = cursor.getString(cursor.getColumnIndex("pos_x"));
				item.stamp_v_yn = cursor.getString(cursor.getColumnIndex("stamp_v_yn"));
				item.endDateMillis = Long.valueOf(cursor.getString(cursor.getColumnIndex("end_date_millis")));
				item.linkUrl = cursor.getString(cursor.getColumnIndex("link_url"));
				item.radius = Integer.valueOf(cursor.getString(cursor.getColumnIndex("radius")));
				item.startDateMillis = Long.valueOf(cursor.getString(cursor.getColumnIndex("start_date_millis")));
				item.startDate = Long.valueOf(cursor.getString(cursor.getColumnIndex("start_date")));
				list.add(item);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(db, cursor);
		} return list;
	}

	public StampEventList checkStampStoryItem(int tlid, int slid) {
		SQLiteDatabase db = getWritableDatabase();
		StampEventList item = new StampEventList();
		Cursor cursor = null;

		try {
			cursor = db.rawQuery("SELECT * from " + TABLE_NAME.STAMP.toString() + " where tlid=" + tlid + " AND slid=" + slid, null);

			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
			} else {
				return null;
			}

			item.end_date = Long.valueOf(cursor.getString(cursor.getColumnIndex("end_date")));
			item.eid = Integer.valueOf(cursor.getString(cursor.getColumnIndex("eid")));
			item.slid = Integer.valueOf(cursor.getString(cursor.getColumnIndex("slid")));
			item.tlid = Integer.valueOf(cursor.getString(cursor.getColumnIndex("tlid")));
			item.stamp_p_yn = cursor.getString(cursor.getColumnIndex("stamp_p_yn"));
			item.elid = Integer.valueOf(cursor.getString(cursor.getColumnIndex("elid")));
			item.tid = Integer.valueOf(cursor.getString(cursor.getColumnIndex("tid")));
			item.posY = cursor.getString(cursor.getColumnIndex("pos_y"));
			item.posX = cursor.getString(cursor.getColumnIndex("pos_x"));
			item.stamp_v_yn = cursor.getString(cursor.getColumnIndex("stamp_v_yn"));
			item.endDateMillis = Long.valueOf(cursor.getString(cursor.getColumnIndex("end_date_millis")));
			item.linkUrl = cursor.getString(cursor.getColumnIndex("link_url"));
			item.radius = Integer.valueOf(cursor.getString(cursor.getColumnIndex("radius")));
			item.startDateMillis = Long.valueOf(cursor.getString(cursor.getColumnIndex("start_date_millis")));
			item.startDate = Long.valueOf(cursor.getString(cursor.getColumnIndex("start_date")));
		} catch (Exception e) {
			return null;
		} finally {
			close(db, cursor);
		}


		return item;
	}

	public void updateStampComplete(int tlid, int slid) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put("stamp_v_yn", "Y");
			db.update(TABLE_NAME.STAMP.toString(), values, "tlid=" + tlid + " AND slid=" + slid, null);

			GeofenceController.getInstance().boot();
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			close(db, null);
		}
	}

	public void updateStampPlayComplete(StampEventList stamp) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put("end_date", stamp.end_date);
			values.put("eid", stamp.eid);
			values.put("slid", stamp.slid);
			values.put("tlid", stamp.tlid);
			values.put("stamp_p_yn", "Y");
			values.put("elid", stamp.elid);
			values.put("tid", stamp.tid);
			values.put("pos_y", stamp.posY);
			values.put("pos_x", stamp.posX);
			values.put("stamp_v_yn", stamp.stamp_v_yn);
			values.put("end_date_millis", stamp.endDateMillis);
			values.put("link_url", stamp.linkUrl);
			values.put("radius", stamp.radius);
			values.put("start_date_millis", stamp.startDateMillis);
			values.put("start_date", stamp.startDate);
			db.update(TABLE_NAME.STAMP.toString(), values, "tlid=" + stamp.tlid + " AND slid=" + stamp.slid, null);
		} catch (SQLiteException e) {
		} finally {
			close(db, null);
		}
	}

	public void clear() {
		SQLiteDatabase db = null;
		int result = 0;
		try {
			db = getWritableDatabase();
			db.delete(TABLE_NAME.STAMP.toString(), null, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(db, null);
		}
	}

	public void delete(int jobId) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.delete(TABLE_NAME.STAMP.toString(), "_ID == " + jobId, null);
		} catch (SQLiteException e) {
		} finally {
			close(db, null);
		}
	}

	private void close(SQLiteDatabase db, Cursor c) {
		if (c != null) {
			c.close();
		}
		if (db != null) {
			db.close();
		}
	}
}
