package kto.smarttour.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import kto.smarttour.common.consts.Common;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.db.item.StoryData;
import kto.smarttour.db.item.StoryFolderItem;
import kto.smarttour.db.item.StoryItem;

public class StoryDbManager extends SQLiteOpenHelper {

	private static final String CREATE_TABLE_STORY_FOLDER = "CREATE TABLE STORY_FOLDER(seq INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, imgUrl TEXT, fileData TEXT)";

	private static final String CREATE_TABLE_STORY = "CREATE TABLE STORY(fid INTEGER DEFAULT 0, " + "audio_file_path TEXT, " + "slid INTEGER DEFAULT 0, " + "audio_play_time TEXT, " + "addr_list TEXT, " + "distance TEXT, " + "tlid INTEGER DEFAULT 0, " + "iid INTEGER DEFAULT 0, " + "title TEXT, " + "tid INTEGER DEFAULT 0, " + "sid INTEGER DEFAULT 0, " + "play_time TEXT DEFAULT 0, " + "pos_y TEXT, " + "pos_x TEXT, " + "audio_version INTEGER DEFAULT 0, " + "tag TEXT, " + "audio_file_size INTEGER DEFAULT 0, " + "audio_script TEXT, " + "audio_title TEXT, " + "thumbnail_file_path TEXT, " + "iflid INTEGER DEFAULT 0, " + "aid INTEGER DEFAULT 0 )";
	private static final String CREATE_TABLE_DOWNLOAD = "CREATE TABLE DOWNLOAD(seq INTEGER PRIMARY KEY AUTOINCREMENT, " + "audio_file_path TEXT, " + "slid INTEGER DEFAULT 0, " + "audio_play_time TEXT, " + "addr_list TEXT, " + "distance TEXT, " + "tlid INTEGER DEFAULT 0, " + "iid INTEGER DEFAULT 0, " + "title TEXT, " + "tid INTEGER DEFAULT 0, " + "sid INTEGER DEFAULT 0, " + "play_time TEXT DEFAULT 0, " + "pos_y TEXT, " + "pos_x TEXT, " + "audio_version INTEGER DEFAULT 0, " + "tag TEXT, " + "audio_file_size INTEGER DEFAULT 0, " + "audio_script TEXT, " + "audio_title TEXT, " + "thumbnail_file_path TEXT, " + "iflid INTEGER DEFAULT 0, " + "aid INTEGER DEFAULT 0, " + "LANG_CODE TEXT DEFAULT 'ko', " + "title_ko TEXT, " + "radius INTEGER DEFAULT 100, " + "version INTEGER DEFAULT 0, " + "thumbnail_update_date_sec INTEGER DEFAULT 0)";

	private static final String CREATE_TABLE_STORY_PLAY_LIST = "CREATE TABLE PLAY_LIST(seq INTEGER PRIMARY KEY AUTOINCREMENT, pindex INTEGER DEFAULT 0," + "audio_file_path TEXT, " + "slid INTEGER DEFAULT 0, " + "audio_play_time TEXT, " + "addr_list TEXT, " + "distance TEXT, " + "tlid INTEGER DEFAULT 0, " + "iid INTEGER DEFAULT 0, " + "title TEXT, " + "tid INTEGER DEFAULT 0, " + "sid INTEGER DEFAULT 0, " + "play_time TEXT DEFAULT 0, " + "pos_y TEXT, " + "pos_x TEXT, " + "audio_version INTEGER DEFAULT 0, " + "tag TEXT, " + "audio_file_size INTEGER DEFAULT 0, " + "audio_script TEXT, " + "audio_title TEXT, " + "thumbnail_file_path TEXT, " + "play_type TEXT, " + "iflid INTEGER DEFAULT 0, " + "aid INTEGER DEFAULT 0, " + "LANG_CODE TEXT DEFAULT 'ko', " + "title_ko TEXT, " + "radius INTEGER DEFAULT 100)";

	public static StoryDbManager dbmgr;
	public Context context;

	enum TABLE_NAME {
		STORY, STORY_FOLDER, DOWNLOAD, PLAY_LIST
	}

	private static final int DB_VERSION = 2;
	private static final String DB_NAME = "odii.sqlite.db";

	public static StoryDbManager getInstance(Context context) {
		if (dbmgr == null) {
			dbmgr = new StoryDbManager(context, null, null, DB_VERSION);
		}
		dbmgr.context = context;
		return dbmgr;
	}

	/**
	 * 테이블생성
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_STORY_FOLDER);
		db.execSQL(CREATE_TABLE_STORY);
		db.execSQL(CREATE_TABLE_DOWNLOAD);
		db.execSQL(CREATE_TABLE_STORY_PLAY_LIST);
	}

	public StoryDbManager(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
		getSQLiteDatabase();
	}

	class getWritableDatabaseAsyncTask extends AsyncTask<Void, Void, SQLiteDatabase> {

		@Override
		protected SQLiteDatabase doInBackground(Void... voids) {
			return getWritableDatabase();
		}

		@Override
		protected void onPostExecute(SQLiteDatabase sqLiteDatabase) {
			super.onPostExecute(sqLiteDatabase);
		}
	}

	private SQLiteDatabase getSQLiteDatabase() {
		SQLiteDatabase db;
		try {
			db = (SQLiteDatabase) new getWritableDatabaseAsyncTask().execute().get();
		} catch (Exception e) {
			db = getWritableDatabase();
		}

		return db;
	}


	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		if (oldVersion < newVersion) {
			db.execSQL("ALTER TABLE " + TABLE_NAME.PLAY_LIST.toString() + " ADD COLUMN LANG_CODE TEXT DEFAULT 'ko'");
			db.execSQL("ALTER TABLE " + TABLE_NAME.PLAY_LIST.toString() + " ADD COLUMN title_ko TEXT");
			db.execSQL("ALTER TABLE " + TABLE_NAME.PLAY_LIST.toString() + " ADD COLUMN radius INTEGER");


			db.execSQL("ALTER TABLE " + TABLE_NAME.DOWNLOAD.toString() + " ADD COLUMN LANG_CODE TEXT DEFAULT 'ko'");
			db.execSQL("ALTER TABLE " + TABLE_NAME.DOWNLOAD.toString() + " ADD COLUMN title_ko TEXT");
			db.execSQL("ALTER TABLE " + TABLE_NAME.DOWNLOAD.toString() + " ADD COLUMN version INTEGER");
			db.execSQL("ALTER TABLE " + TABLE_NAME.DOWNLOAD.toString() + " ADD COLUMN thumbnail_update_date_sec INTEGER");
			db.execSQL("ALTER TABLE " + TABLE_NAME.DOWNLOAD.toString() + " ADD COLUMN radius INTEGER");
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

	//-------------------------------STORY FOLDER---------------------------------------------------
	public int getStoryFolderCount() {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {
			return db.rawQuery("SELECT * FROM " + TABLE_NAME.STORY_FOLDER.toString(), null).getCount();
		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}

		return 0;
	}

	public void insertFolder(String title, String imgUrl, String fileData) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put("title", title);
			values.put("imgUrl", imgUrl);
			values.put("fileData", fileData);
			db.insert(TABLE_NAME.STORY_FOLDER.toString(), null, values);
		} catch (Exception e) {

		} finally {
			close(db, null);
		}
	}

	public void updateFolder(String fid, String title, String imgUrl) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			if (!TextUtils.isEmpty(title)) {
				values.put("title", title);
			}

			if (!TextUtils.isEmpty(imgUrl)) {
				values.put("imgUrl", imgUrl);
			}

			db.update(TABLE_NAME.STORY_FOLDER.toString(), values, "seq=" + fid, null);
		} catch (Exception e) {

		} finally {
			close(db, null);
		}
	}


	public ArrayList<StoryFolderItem> getStoryFolderList() {
		ArrayList<StoryFolderItem> data = new ArrayList<>();
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("SELECT * from " + TABLE_NAME.STORY_FOLDER.toString(), null);
			StoryFolderItem item;
			while (cursor.moveToNext()) {
				item = new StoryFolderItem();
				item.seq = cursor.getString(cursor.getColumnIndex("seq"));
				item.title = cursor.getString(cursor.getColumnIndex("title"));
				item.imgUrl = cursor.getString(cursor.getColumnIndex("imgUrl"));
				item.fileData = String.valueOf(getStoryCount(item.seq));
				data.add(item);
			}

		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}
		return data;
	}

	public StoryFolderItem getStoryFolder(String seq) {
		SQLiteDatabase db = getWritableDatabase();
		StoryFolderItem item = new StoryFolderItem();
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("SELECT * from " + TABLE_NAME.STORY_FOLDER.toString() + " WHERE seq=" + seq, null);

			cursor.moveToFirst();
			item.seq = cursor.getString(cursor.getColumnIndex("seq"));
			item.title = cursor.getString(cursor.getColumnIndex("title"));
			item.imgUrl = cursor.getString(cursor.getColumnIndex("imgUrl"));
			item.fileData = cursor.getString(cursor.getColumnIndex("fileData"));

		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}
		return item;
	}

	public void removeFolder(StoryFolderItem item) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.execSQL(String.format("DELETE FROM %s WHERE seq = %s", TABLE_NAME.STORY_FOLDER.toString(), item.seq));
			removeStory(item.seq);
		} catch (Exception e) {
			KLog.i("TEST_LOG", "removeDownLoad error");
		} finally {
			close(db, null);
		}
	}

	//-------------------------------STORY----------------------------------------------------------
	public int getStoryCount() {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {
			return db.rawQuery("SELECT * FROM " + TABLE_NAME.STORY.toString(), null).getCount();
		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}

		return 0;
	}

	public int getStoryCount(String seq) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {
			return db.rawQuery("SELECT * FROM " + TABLE_NAME.STORY.toString() + " WHERE fid = " + seq, null).getCount();
		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}

		return 0;
	}

	public void insertStory(String fid, StoryData data) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values;
			for (StoryItem item : data.story) {
				values = new ContentValues();
				values.put("fid", fid);
				values.put("audio_file_path", item.audioFilePath);
				values.put("slid", item.slid);
				values.put("audio_play_time", item.audioPlayTime);
				values.put("addr_list", item.addrList);
				values.put("distance", item.distance);
				values.put("tlid", item.tlid);
				values.put("iid", item.iid);
				values.put("title", item.title);
				values.put("tid", item.tid);
				values.put("sid", item.sid);
				values.put("play_time", item.playTime);
				values.put("pos_y", item.posY);
				values.put("pos_x", item.posX);
				values.put("audio_version", item.audioVersion);
				values.put("tag", item.tag);
				values.put("audio_file_size", item.audioFileSize);
				values.put("audio_script", item.audioScript);
				values.put("audio_title", item.audioTitle);
				values.put("thumbnail_file_path", item.thumbnailFilePath);
				values.put("iflid", item.iflid);
				values.put("aid", item.aid);
				//values.put("LANG_CODE",item.langCode); //추가

				db.insert(TABLE_NAME.STORY.toString(), null, values);
			}
			db.setTransactionSuccessful();

		} catch (Exception e) {
			KLog.i("TEST_LOG", "insertDownLoad error");
		} finally {
			db.endTransaction();
			close(db, null);
		}
	}

	public StoryData getStoryList(StoryFolderItem sfItem) {
		StoryData data = new StoryData();
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("SELECT * from " + TABLE_NAME.STORY.toString() + " WHERE fid = " + sfItem.seq, null);
			StoryItem item;
			while (cursor.moveToNext()) {
				item = new StoryItem();
				item.audioFilePath = cursor.getString(cursor.getColumnIndex("audio_file_path"));
				item.slid = cursor.getInt(cursor.getColumnIndex("slid"));
				item.audioPlayTime = cursor.getString(cursor.getColumnIndex("audio_play_time"));
				item.addrList = cursor.getString(cursor.getColumnIndex("addr_list"));
				item.distance = cursor.getString(cursor.getColumnIndex("distance"));
				item.tlid = cursor.getInt(cursor.getColumnIndex("tlid"));
				item.iid = cursor.getInt(cursor.getColumnIndex("iid"));
				item.title = cursor.getString(cursor.getColumnIndex("title"));
				item.tid = cursor.getInt(cursor.getColumnIndex("tid"));
				item.sid = cursor.getInt(cursor.getColumnIndex("sid"));
				item.playTime = cursor.getString(cursor.getColumnIndex("play_time"));
				item.posY = cursor.getString(cursor.getColumnIndex("pos_y"));
				item.posX = cursor.getString(cursor.getColumnIndex("pos_x"));
				item.audioVersion = cursor.getInt(cursor.getColumnIndex("audio_version"));
				item.tag = cursor.getString(cursor.getColumnIndex("tag"));
				item.audioFileSize = cursor.getInt(cursor.getColumnIndex("audio_file_size"));
				item.audioScript = cursor.getString(cursor.getColumnIndex("audio_script"));
				item.audioTitle = cursor.getString(cursor.getColumnIndex("audio_title"));
				item.thumbnailFilePath = cursor.getString(cursor.getColumnIndex("thumbnail_file_path"));
				item.iflid = cursor.getInt(cursor.getColumnIndex("iflid"));
				item.aid = cursor.getInt(cursor.getColumnIndex("aid"));
				//item.langCode = cursor.getString(cursor.getColumnIndex("LANG_CODE")); //추가

				data.story.add(item);
			}

		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}
		return data;
	}

	public void removeStory(String fid, StoryItem item) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.execSQL(String.format("DELETE FROM %s WHERE fid = %s AND aid = %d AND sid = %d", TABLE_NAME.STORY.toString(), fid, item.aid, item.sid));
		} catch (Exception e) {
			KLog.i("TEST_LOG", "removeDownLoad error");
		} finally {
			close(db, null);
		}
	}

	public void removeStory(String fid) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.execSQL(String.format("DELETE FROM %s WHERE fid = %s", TABLE_NAME.STORY.toString(), fid));
		} catch (Exception e) {
			KLog.i("TEST_LOG", "removeDownLoad error");
		} finally {
			close(db, null);
		}
	}

	//-------------------------------DOWNLOAD-------------------------------------------------------

	public int getDownloadCount() {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {
			//return db.rawQuery("SELECT * FROM " + TABLE_NAME.DOWNLOAD.toString(), null).getCount();

			cursor = db.rawQuery("SELECT count(*) FROM " + TABLE_NAME.DOWNLOAD.toString(), null);
			cursor.moveToFirst();
			return cursor.getInt(0);
		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}

		return 0;
	}

	public StoryData getDownloadList() {
		StoryData data = new StoryData();
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("SELECT * from " + TABLE_NAME.DOWNLOAD.toString(), null);
			StoryItem item;
			while (cursor.moveToNext()) {
				item = new StoryItem();
				item.audioFilePath = cursor.getString(cursor.getColumnIndex("audio_file_path"));
				item.slid = cursor.getInt(cursor.getColumnIndex("slid"));
				item.audioPlayTime = cursor.getString(cursor.getColumnIndex("audio_play_time"));
				item.addrList = cursor.getString(cursor.getColumnIndex("addr_list"));
				item.distance = cursor.getString(cursor.getColumnIndex("distance"));
				item.tlid = cursor.getInt(cursor.getColumnIndex("tlid"));
				item.iid = cursor.getInt(cursor.getColumnIndex("iid"));
				item.title = cursor.getString(cursor.getColumnIndex("title"));
				item.tid = cursor.getInt(cursor.getColumnIndex("tid"));
				item.sid = cursor.getInt(cursor.getColumnIndex("sid"));
				item.playTime = cursor.getString(cursor.getColumnIndex("play_time"));
				item.posY = cursor.getString(cursor.getColumnIndex("pos_y"));
				item.posX = cursor.getString(cursor.getColumnIndex("pos_x"));
				item.audioVersion = cursor.getInt(cursor.getColumnIndex("audio_version"));
				item.tag = cursor.getString(cursor.getColumnIndex("tag"));
				item.audioFileSize = cursor.getInt(cursor.getColumnIndex("audio_file_size"));
				item.audioScript = cursor.getString(cursor.getColumnIndex("audio_script"));
				item.audioTitle = cursor.getString(cursor.getColumnIndex("audio_title"));
				item.thumbnailFilePath = cursor.getString(cursor.getColumnIndex("thumbnail_file_path"));
				item.iflid = cursor.getInt(cursor.getColumnIndex("iflid"));
				item.aid = cursor.getInt(cursor.getColumnIndex("aid"));
				item.langCode = cursor.getString(cursor.getColumnIndex("LANG_CODE"));
				item.titleKo = cursor.getString(cursor.getColumnIndex("title_ko"));
				item.version = cursor.getInt(cursor.getColumnIndex("version"));
				item.thumbnailUpdateDateSec = cursor.getInt(cursor.getColumnIndex("thumbnail_update_date_sec"));
				item.radius = cursor.getInt(cursor.getColumnIndex("radius"));
				data.story.add(item);
			}

		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}
		return data;
	}

	public List<StoryItem> getDownloadList(String langCode) {
		List<StoryItem> stories = new ArrayList<>();
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("SELECT * from " + TABLE_NAME.DOWNLOAD.toString() + " WHERE LANG_CODE = '" + langCode + "'", null);
			StoryItem item;
			while (cursor.moveToNext()) {
				item = new StoryItem();
				item.audioFilePath = cursor.getString(cursor.getColumnIndex("audio_file_path"));
				item.slid = cursor.getInt(cursor.getColumnIndex("slid"));
				item.audioPlayTime = cursor.getString(cursor.getColumnIndex("audio_play_time"));
				item.addrList = cursor.getString(cursor.getColumnIndex("addr_list"));
				item.distance = cursor.getString(cursor.getColumnIndex("distance"));
				item.tlid = cursor.getInt(cursor.getColumnIndex("tlid"));
				item.iid = cursor.getInt(cursor.getColumnIndex("iid"));
				item.title = cursor.getString(cursor.getColumnIndex("title"));
				item.tid = cursor.getInt(cursor.getColumnIndex("tid"));
				item.sid = cursor.getInt(cursor.getColumnIndex("sid"));
				item.playTime = cursor.getString(cursor.getColumnIndex("play_time"));
				item.posY = cursor.getString(cursor.getColumnIndex("pos_y"));
				item.posX = cursor.getString(cursor.getColumnIndex("pos_x"));
				item.audioVersion = cursor.getInt(cursor.getColumnIndex("audio_version"));
				item.tag = cursor.getString(cursor.getColumnIndex("tag"));
				item.audioFileSize = cursor.getInt(cursor.getColumnIndex("audio_file_size"));
				item.audioScript = cursor.getString(cursor.getColumnIndex("audio_script"));
				item.audioTitle = cursor.getString(cursor.getColumnIndex("audio_title"));
				item.thumbnailFilePath = cursor.getString(cursor.getColumnIndex("thumbnail_file_path"));
				item.iflid = cursor.getInt(cursor.getColumnIndex("iflid"));
				item.aid = cursor.getInt(cursor.getColumnIndex("aid"));
				item.langCode = cursor.getString(cursor.getColumnIndex("LANG_CODE"));
				item.titleKo = cursor.getString(cursor.getColumnIndex("title_ko"));
				item.version = cursor.getInt(cursor.getColumnIndex("version"));
				item.thumbnailUpdateDateSec = cursor.getInt(cursor.getColumnIndex("thumbnail_update_date_sec"));
				item.radius = cursor.getInt(cursor.getColumnIndex("radius"));
				stories.add(item);
			}

		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}
		return stories;
	}

	public void insertDownLoad(StoryItem item) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			ContentValues values;
			values = new ContentValues();

			values.put("audio_file_path", item.audioFilePath);
			values.put("slid", item.slid);
			values.put("audio_play_time", item.audioPlayTime);
			values.put("addr_list", item.addrList);
			values.put("distance", item.distance);
			values.put("tlid", item.tlid);
			values.put("iid", item.iid);
			values.put("title", item.title);
			values.put("tid", item.tid);
			values.put("sid", item.sid);
			values.put("play_time", item.playTime);
			values.put("pos_y", item.posY);
			values.put("pos_x", item.posX);
			values.put("audio_version", item.audioVersion);
			values.put("tag", item.tag);
			values.put("audio_file_size", item.audioFileSize);
			values.put("audio_script", item.audioScript);
			values.put("audio_title", item.audioTitle);
			values.put("thumbnail_file_path", item.thumbnailFilePath);
			values.put("iflid", item.iflid);
			values.put("aid", item.aid);
			values.put("LANG_CODE", item.langCode);
			values.put("title_ko", item.titleKo);
			values.put("version", item.version);
			values.put("thumbnail_update_date_sec", item.thumbnailUpdateDateSec);
			values.put("radius", item.radius);

			int result = db.update(TABLE_NAME.DOWNLOAD.toString(), values, "tlid=" + item.tlid + " AND slid=" + item.slid, null);
			if( result == 0) {
				db.insert(TABLE_NAME.DOWNLOAD.toString(), null, values);
			}

		} catch (Exception e) {
			KLog.i("TEST_LOG", "insertDownLoad error");
		} finally {
			close(db, null);
		}
	}

	public void insertDownLoad2(StoryItem item) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			ContentValues values;
			values = new ContentValues();

			values.put("audio_file_path", item.audioFilePath);
			values.put("slid", item.slid);
			values.put("audio_play_time", item.audioPlayTime);
			values.put("addr_list", item.addrList);
			values.put("distance", item.distance);
			values.put("tlid", item.tlid);
			values.put("iid", item.iid);
			values.put("title", item.title);
			values.put("tid", item.tid);
			values.put("sid", item.sid);
			values.put("play_time", item.playTime);
			values.put("pos_y", item.posY);
			values.put("pos_x", item.posX);
			values.put("audio_version", item.audioVersion);
			values.put("tag", item.tag);
			values.put("audio_file_size", item.audioFileSize);
			values.put("audio_script", item.audioScript);
			values.put("audio_title", item.audioTitle);
			values.put("thumbnail_file_path", item.thumbnailFilePath);
			values.put("iflid", item.iflid);
			values.put("aid", item.aid);
			values.put("LANG_CODE", item.langCode);
			values.put("title_ko", item.titleKo);
			values.put("version", item.version);
			values.put("thumbnail_update_date_sec", item.thumbnailUpdateDateSec);
			values.put("radius", item.radius);

			int result = db.update(TABLE_NAME.DOWNLOAD.toString(), values, "slid=" + item.slid, null);
			if( result == 0) {
				db.insert(TABLE_NAME.DOWNLOAD.toString(), null, values);
			}

		} catch (Exception e) {
			KLog.i("TEST_LOG", "insertDownLoad error");
		} finally {
			close(db, null);
		}
	}

	public void updateStory(int slid, StoryItem targetItem) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			ContentValues values;
			values = new ContentValues();
			values.put("audio_file_path", targetItem.audioFilePath);
			values.put("audio_play_time", targetItem.audioPlayTime);
			values.put("addr_list", targetItem.addrList);
			values.put("distance", targetItem.distance);
			values.put("tlid", targetItem.tlid);
			values.put("iid", targetItem.iid);
			values.put("title", targetItem.title);
			values.put("tid", targetItem.tid);
			values.put("sid", targetItem.sid);
			values.put("play_time", targetItem.playTime);
			values.put("pos_y", targetItem.posY);
			values.put("pos_x", targetItem.posX);
			values.put("audio_version", targetItem.audioVersion);
			values.put("tag", targetItem.tag);
			values.put("audio_file_size", targetItem.audioFileSize);
			values.put("audio_script", targetItem.audioScript);
			values.put("audio_title", targetItem.audioTitle);
			values.put("thumbnail_file_path", targetItem.thumbnailFilePath);
			values.put("iflid", targetItem.iflid);
			values.put("aid", targetItem.aid);
			values.put("LANG_CODE", targetItem.langCode);
			values.put("title_ko", targetItem.titleKo);
			values.put("version", targetItem.version);
			values.put("thumbnail_update_date_sec", targetItem.thumbnailUpdateDateSec);
			values.put("radius", targetItem.radius);
			db.update(TABLE_NAME.DOWNLOAD.toString(), values, "slid=" + slid, null);
		} catch (Exception e) {
			KLog.i("TEST_LOG", "insertDownLoad error");
		} finally {
			close(db, null);
		}
	}

	public void removeDownLoad(StoryItem item) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.execSQL(String.format("DELETE FROM %s WHERE aid = %d AND sid = %d", TABLE_NAME.DOWNLOAD.toString(), item.aid, item.sid));
		} catch (Exception e) {
			KLog.i("TEST_LOG", "removeDownLoad error");
		} finally {
			close(db, null);
		}
	}

	public void removeDownLoad(List<StoryItem> datas) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			for (StoryItem item : datas) {
				db.execSQL(String.format("DELETE FROM %s WHERE aid = %d AND sid = %d", TABLE_NAME.DOWNLOAD.toString(), item.aid, item.sid));
			}
			db.setTransactionSuccessful();
		} catch (Exception e) {
			KLog.i("TEST_LOG", "removeDownLoad error");
		} finally {
			db.endTransaction();
			close(db, null);
		}
	}

	public void removeDownLoad2(List<StoryItem> datas) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			for (StoryItem item : datas) {
				db.execSQL(String.format("DELETE FROM %s WHERE tlid = %d AND slid = %d", TABLE_NAME.DOWNLOAD.toString(), item.tlid, item.slid));
			}
			db.setTransactionSuccessful();
		} catch (Exception e) {
			KLog.i("TEST_LOG", "removeDownLoad error");
		} finally {
			db.endTransaction();
			close(db, null);
		}
	}

	//-------------------------------PLAY_LIST-------------------------------------------------------

	//언어별 수정필요*가능성
	public int getPlayListCount() {

		String item_langCode = getLANG_CODE_For_StoryItem();

		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {
			if(Common.isPlayListOnlyForCurrentLanguage){
				//return db.rawQuery("SELECT * FROM " + TABLE_NAME.PLAY_LIST.toString() +" WHERE LANG_CODE LIKE "+item_langCode, null).getCount();
				//return db.rawQuery("SELECT * FROM " + TABLE_NAME.PLAY_LIST.toString() +" WHERE LANG_CODE = "+ "'"+item_langCode+"'", null).getCount();

				cursor = db.rawQuery("SELECT count(*) FROM " + TABLE_NAME.PLAY_LIST.toString() +" WHERE LANG_CODE = "+ "'"+item_langCode+"'", null);
				cursor.moveToFirst();
				return cursor.getInt(0);
			}else{
				//return db.rawQuery("SELECT * FROM " + TABLE_NAME.PLAY_LIST.toString(), null).getCount();

				cursor = db.rawQuery("SELECT count(*) FROM " + TABLE_NAME.PLAY_LIST.toString(), null);
				cursor.moveToFirst();
				return cursor.getInt(0);
			}

		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}

		return 0;
	}

	// 플레이 아이템 (DB관리  LANG_CODE 기준값)
	private String getLANG_CODE_For_StoryItem(){
		String sLocale = SettingsUtil.getLocale(context);
		String item_langCode = "";
		if(sLocale.contains("ko"))
		{
			//item_langCode = "'%ko%'";
			item_langCode = "ko";
		}
		else if(sLocale.contains("zh"))
		{
			//item_langCode = "'%cn%'"; //"cn1" 인데 "cn"을 포함하는것으로 처리하자
			//item_langCode = "cn";
			item_langCode = "cn1";
		}
		else if(sLocale.contains("tw"))
		{
			//item_langCode = "'%cn%'"; //"cn1" 인데 "cn"을 포함하는것으로 처리하자
			//item_langCode = "cn";
			item_langCode = "cn2";
		}
		else if(sLocale.contains("ja"))
		{
			//item_langCode = "'%jp%'";
			item_langCode = "jp";
		}
		else
		{
			//item_langCode = "'%en%'";
			item_langCode = "en";
		}
		return item_langCode;
	}
	//언어별 수정필요 가능성
	public StoryData getPlayList() {
		String item_langCode = getLANG_CODE_For_StoryItem();

		//--------------------------------------------

		StoryData data = new StoryData();
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = null;
		try {

			//cursor = db.rawQuery("SELECT * from " + TABLE_NAME.PLAY_LIST.toString() + " ORDER BY pindex", null);

			if(Common.isPlayListOnlyForCurrentLanguage){
				//cursor = db.rawQuery("SELECT * from " + TABLE_NAME.PLAY_LIST.toString() + " WHERE LANG_CODE LIKE "+item_langCode + " ORDER BY pindex", null);

				//cursor = db.rawQuery("SELECT * from " + TABLE_NAME.PLAY_LIST.toString() +" WHERE LANG_CODE = "+ "'"+item_langCode+"'" + " ORDER BY pindex DESC", null);
				cursor = db.rawQuery("SELECT * from " + TABLE_NAME.PLAY_LIST.toString() +" WHERE LANG_CODE = "+ "'"+item_langCode+"'" + " ORDER BY pindex", null);

			}else{
				cursor = db.rawQuery("SELECT * from " + TABLE_NAME.PLAY_LIST.toString() + " ORDER BY pindex", null);
			}


			StoryItem item;
			while (cursor.moveToNext()) {

				item = new StoryItem();
				item.seq = cursor.getInt(cursor.getColumnIndex("seq"));
				item.pindex = cursor.getInt(cursor.getColumnIndex("pindex"));
				item.audioFilePath = cursor.getString(cursor.getColumnIndex("audio_file_path"));
				item.slid = cursor.getInt(cursor.getColumnIndex("slid"));
				item.audioPlayTime = cursor.getString(cursor.getColumnIndex("audio_play_time"));
				item.addrList = cursor.getString(cursor.getColumnIndex("addr_list"));
				item.distance = cursor.getString(cursor.getColumnIndex("distance"));
				item.tlid = cursor.getInt(cursor.getColumnIndex("tlid"));
				item.iid = cursor.getInt(cursor.getColumnIndex("iid"));
				item.title = cursor.getString(cursor.getColumnIndex("title"));
				item.tid = cursor.getInt(cursor.getColumnIndex("tid"));
				item.sid = cursor.getInt(cursor.getColumnIndex("sid"));
				item.playTime = cursor.getString(cursor.getColumnIndex("play_time"));
				item.posY = cursor.getString(cursor.getColumnIndex("pos_y"));
				item.posX = cursor.getString(cursor.getColumnIndex("pos_x"));
				item.audioVersion = cursor.getInt(cursor.getColumnIndex("audio_version"));
				item.tag = cursor.getString(cursor.getColumnIndex("tag"));
				item.audioFileSize = cursor.getInt(cursor.getColumnIndex("audio_file_size"));
				item.audioScript = cursor.getString(cursor.getColumnIndex("audio_script"));
				item.audioTitle = cursor.getString(cursor.getColumnIndex("audio_title"));
				item.thumbnailFilePath = cursor.getString(cursor.getColumnIndex("thumbnail_file_path"));
				item.iflid = cursor.getInt(cursor.getColumnIndex("iflid"));
				item.aid = cursor.getInt(cursor.getColumnIndex("aid"));
				item.play_type = cursor.getString(cursor.getColumnIndex("play_type"));
				item.langCode = cursor.getString(cursor.getColumnIndex("LANG_CODE"));
				item.titleKo = cursor.getString(cursor.getColumnIndex("title_ko"));
				item.radius = cursor.getInt(cursor.getColumnIndex("radius"));

				data.story.add(item);
			}

		} catch (Exception e) {

		} finally {
			close(db, cursor);
		}
		return data;
	}

	public void addPlayList(List<StoryItem> items) {

		String item_langCode = getLANG_CODE_For_StoryItem();
		//--
		StoryData storyData = getPlayList();
		//--
		ArrayList<StoryItem> newItems = new ArrayList<>();
		if( items!=null && !items.isEmpty() ){
			newItems.addAll(items);

		}

		//-- 중복 항목 제거
		for (int i = 0 ; i < storyData.story.size() ; i ++) {
			StoryItem item = storyData.story.get(i);
			for (int j = newItems.size() - 1 ; j >= 0 ; j --) {
				StoryItem newItem = newItems.get(j);
				if (newItem.isEquals(item)) {
					newItems.remove(j);
				}

			}
		}

		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values;
			ContentValues updateValues;
			for (StoryItem item : newItems) {

				if (item == null) {
					continue;
				}
				//=============================================================================
				values = new ContentValues();

				values.put("audio_file_path", item.audioFilePath);
				values.put("slid", item.slid);
				values.put("audio_play_time", item.audioPlayTime);
				values.put("addr_list", item.addrList);
				values.put("distance", item.distance);
				values.put("tlid", item.tlid);
				values.put("iid", item.iid);
				values.put("title", item.title);
				values.put("tid", item.tid);
				values.put("sid", item.sid);
				values.put("play_time", item.playTime);
				values.put("pos_y", item.posY);
				values.put("pos_x", item.posX);
				values.put("audio_version", item.audioVersion);
				values.put("tag", item.tag);
				values.put("audio_file_size", item.audioFileSize);
				values.put("audio_script", item.audioScript);
				values.put("audio_title", item.audioTitle);
				values.put("thumbnail_file_path", item.thumbnailFilePath);
				values.put("play_type", item.play_type);
				values.put("iflid", item.iflid);
				values.put("aid", item.aid);

				//--단일이 아닌 전체데이터를 추가할때 누락?되어보이는 언어코드를 상시 강제대입.(공통으로, 웹 단일추가도 강제대입 처리됨)
				item.langCode = item_langCode;

				values.put("LANG_CODE", item.langCode);
				values.put("title_ko", item.titleKo);
				values.put("radius", item.radius);

				long result = db.insert(TABLE_NAME.PLAY_LIST.toString(), null, values);

				updateValues = new ContentValues();
				updateValues.put("pindex", result);

				db.update(TABLE_NAME.PLAY_LIST.toString(), updateValues, "seq=" + result, null);
			}
			db.setTransactionSuccessful();


		} catch (Exception e) {
			KLog.i("TEST_LOG", "addPlayList error");
		} finally {
			db.endTransaction();
			close(db, null);
		}
	}

	public void addPlayList(String play_type, List<StoryItem> items) {
		String item_langCode = getLANG_CODE_For_StoryItem();
		//--------------------------------------------

		//--
		StoryData storyData = getPlayList();
		//--

		ArrayList<StoryItem> newItems = new ArrayList<>();

		if( items!=null && !items.isEmpty() ){
			newItems.addAll(items);
		}

		//--중복 항목 제거
		for (int i = 0 ; i < storyData.story.size() ; i ++) {
			StoryItem item = storyData.story.get(i);
			for (int j = newItems.size() - 1 ; j >= 0 ; j --) {
				StoryItem newItem = newItems.get(j);
				if (newItem.isEquals(item)) {
					newItems.remove(j);
				}
			}
		}

		//	순서 재조정
		Collections.sort(newItems, new Comparator<StoryItem>() {
			@Override
			public int compare(StoryItem item, StoryItem t1) {
				if (item.pindex > t1.pindex) {
					return 1;
				}
				else if(item.pindex < t1.pindex) {
					return -1;
				}
				else {
					return 0;
				}
			}
		});

		//	새로운 아이템 넣기
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues values;
			ContentValues updateValues;

			for (StoryItem item : newItems) {

				if (item == null) {
					continue;
				}

				//=============================================================================

				values = new ContentValues();
				values.put("slid", item.slid);
				values.put("audio_play_time", item.audioPlayTime);
				values.put("iid", item.iid);
				values.put("title", item.title);
				values.put("tid", item.tid);
				values.put("sid", item.sid);
				values.put("pos_x", item.posX);
				values.put("pos_y", item.posY);
				values.put("audio_version", item.audioVersion);
				values.put("tag", item.tag);
				values.put("audio_script", item.audioScript);
				values.put("audio_file_path", item.audioFilePath);
				values.put("addr_list", item.addrList);
				values.put("tlid", item.tlid);
				values.put("audio_file_size", item.audioFileSize);
				values.put("audio_title", item.audioTitle);
				values.put("thumbnail_file_path", item.thumbnailFilePath);
				values.put("iflid", item.iflid);
				values.put("aid", item.aid);
				values.put("distance", item.distance);
				values.put("play_time", item.playTime);
				values.put("play_type", play_type);
				values.put("LANG_CODE", item.langCode);
				values.put("title_ko", item.titleKo);
				values.put("radius", item.radius);

				long result = db.insert(TABLE_NAME.PLAY_LIST.toString(), null, values);

				updateValues = new ContentValues();
				updateValues.put("pindex", result);

				//--단일이 아닌 전체데이터를 추가할때 누락?되어보이는 언어코드를 상시 강제대입.(공통으로, 웹 단일추가도 강제대입 처리됨)
				item.langCode = item_langCode;
				updateValues.put("LANG_CODE", item_langCode);

				db.update(TABLE_NAME.PLAY_LIST.toString(), updateValues, "seq=" + result, null);
			}
			db.setTransactionSuccessful();

		} catch (Exception e) {
			KLog.i("TEST_LOG", "insertDownLoad error");
		} finally {
			db.endTransaction();
			close(db, null);
		}
	}

	public void removePlayList(int seq) {

		SQLiteDatabase db = getWritableDatabase();
		try {
			String queryString = String.format("DELETE FROM %s WHERE seq = %d", TABLE_NAME.PLAY_LIST.toString(), seq);
			db.execSQL(queryString);
		} catch (Exception e) {
			KLog.i("TEST_LOG", "removePlayLIst error");
		} finally {
			close(db, null);
		}
	}

	public void removePlayList(int tlid, int slid) {

		SQLiteDatabase db = getWritableDatabase();
		try {
			String queryString = String.format("DELETE FROM %s WHERE tlid = %d AND slid = %d", TABLE_NAME.PLAY_LIST.toString(), tlid, slid);
			db.execSQL(queryString);
		} catch (Exception e) {
			KLog.i("TEST_LOG", "removePlayLIst error");
		} finally {
			close(db, null);
		}
	}

	public void removePlayList_Slid(int slid) {

		SQLiteDatabase db = getWritableDatabase();
		try {
			String queryString = String.format("DELETE FROM %s WHERE slid = %d", TABLE_NAME.PLAY_LIST.toString(), slid);
			db.execSQL(queryString);
		} catch (Exception e) {
			KLog.i("TEST_LOG", "removePlayLIst error");
		} finally {
			close(db, null);
		}
	}

	public void movePlayList(StoryItem fromItem, StoryItem toItem) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues updateValues;
		try {
			updateValues = new ContentValues();
			updateValues.put("pindex", fromItem.seq);
			db.update(TABLE_NAME.PLAY_LIST.toString(), updateValues, "seq=" + toItem.seq, null);

			updateValues = new ContentValues();
			updateValues.put("pindex", toItem.seq);
			db.update(TABLE_NAME.PLAY_LIST.toString(), updateValues, "seq=" + fromItem.seq, null);

		} catch (Exception e) {
			KLog.i("TEST_LOG", "movePlayList error");
		} finally {
			close(db, null);
		}
	}

	public int isDupulicated(StoryItem item) {
		int count = 0;
		SQLiteDatabase db = getWritableDatabase();
		try {
			String query = "SELECT COUNT(*)" +
					" FROM " + TABLE_NAME.PLAY_LIST.toString() +
					" WHERE tid=" + item.tid +
					" AND sid=" + item.sid +
					" AND tlid=" + item.tlid +
					" AND slid=" + item.slid +
					";";

			Cursor mCount= db.rawQuery(query, null);
			mCount.moveToFirst();
			count = mCount.getInt(0);
			mCount.close();
		} catch (Exception e) {
			KLog.i("TEST_LOG", "movePlayList error");
		} finally {
			close(db, null);
			return count;
		}
	}

	public void clearPlayList() {

		String item_langCode = getLANG_CODE_For_StoryItem();
		//--------------------------------------------

		SQLiteDatabase db = getWritableDatabase();

		//클리어 및 교체 재생시에도 호출됨. (언어별로 조건을 걸자)
		if(Common.isPlayListOnlyForCurrentLanguage){
			//WHERE LANG_CODE = "+item_langCode
			//db.delete(TABLE_NAME, "name=?", new String[]{courseName});
			db.delete(TABLE_NAME.PLAY_LIST.toString(), "LANG_CODE=?", new String[]{item_langCode});
		}else{
			db.delete(TABLE_NAME.PLAY_LIST.toString(), null, null);
		}

		close(db, null);
	}

	public void clearAll() {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_NAME.STORY);
		db.execSQL("DELETE FROM " + TABLE_NAME.STORY_FOLDER);
		db.execSQL("DELETE FROM " + TABLE_NAME.DOWNLOAD);
		db.execSQL("DELETE FROM " + TABLE_NAME.PLAY_LIST);
		close(db, null);
	}
}