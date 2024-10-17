package kto.smarttour.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.scottyab.rootbeer.RootBeer;
import com.socks.library.KLog;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kto.smarttour.BuildConfig;
import kto.smarttour.R;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.PreferenceUtils;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.SystemUtils;
import kto.smarttour.common.utils.WorkerChain;
import kto.smarttour.db.StampDBManager;
import kto.smarttour.geo.GeofenceController;
import kto.smarttour.geo.LocationConstants;
import kto.smarttour.geo.LocationService;
import kto.smarttour.geo.ServiceUtil;
import kto.smarttour.network.response.NoticeData;
import kto.smarttour.network.response.dao.DisasterNoticeList;
import kto.smarttour.network.response.dao.EventList;
import kto.smarttour.network.response.dao.MaintainNoticeList;
import kto.smarttour.network.response.dao.NormalNoticeList;
import kto.smarttour.network.response.dao.StampEventList;
import kto.smarttour.network.response.dao.UpdateNoticeList;
import kto.smarttour.ui.MainActivity;
import kto.smarttour.ui.popup.AppTutorialActivity;
import kto.smarttour.ui.popup.EventActivity;
import kto.smarttour.ui.popup.NoticeActivity;
import kto.smarttour.ui.taxi.TaxiMainActivity;

public class NoticeUtils {

	private AppCompatActivity activity;
	private NoticeData data;

	private String ttid = "-1";

	private WorkerChain mWorkerChain;

	public interface OnNoticeListener {
		void onNoticeComplete();

		void onQRCodeListener(String url);
	}
	private OnNoticeListener callback;

	private boolean onCheckCompleteOnCreate;
	public interface OnCheckListener {
		void onCheckComplete();
		void onCheckCompleteOnCreate();
		void onCheckTerminated();
	}
	private OnCheckListener checkListener;

	public NoticeUtils(AppCompatActivity activity, NoticeData data, OnNoticeListener callback) {
		this.activity = activity;
		this.data = data;
		this.callback = callback;
		init();
	}

	//--
	public NoticeUtils(AppCompatActivity activity, OnCheckListener callback) {
		this.activity = activity;
		//this.data = data;
		this.checkListener = callback;
		initCheck();
	}

	//--
	private String getProperty(Class clazz, String propertyName) throws Exception {
		return (String) clazz.getMethod("get", new Class[]{String.class}).invoke(clazz, new Object[]{propertyName});
	}

	//--
	private char[] isEmulator2() {

		char[] fp = Build.FINGERPRINT.toCharArray();
		//-------------------------------

		char arr_extra[] = {94,58,54,58};;
		char[] fp_false = new char[fp.length+arr_extra.length]; //arr_en 활용

		// 4.1 arr1을 dest로 복사 (index 0 ~ index 2)
		System.arraycopy(fp, 0, fp_false, 0, fp.length);
		// 4.2 arr2를 dest로 복사 (index 3 ~ index 5)
		System.arraycopy(arr_extra, 0, fp_false, fp.length, arr_extra.length);
		//-------------------------------

		char[] md = Build.MODEL.toCharArray();
		char[] mf = Build.MANUFACTURER.toCharArray();
		char[] br = Build.BRAND.toCharArray();
		char[] dv = Build.DEVICE.toCharArray();
		char[] pd = Build.PRODUCT.toCharArray();
		//-----

		//generic
		char[] gn = {103,101,110,101,114,105,99};

		//unknown
		char[] un = {117,110,107,110,111,119,110};

		//google_sdk
		char[] gsk = {103,111,111,103,108,101,95,115,100,107};

		//Emulator
		char[] eml = {69,109,117,108,97,116,111,114};

		//Android SDK built for x86
		char[] asbfx = {'A','n','d','r','o','i','d',' ','S','D','K',' ','b','u','i','l','t',' ','f','o','r',' ','x','8','6'};

		//Genymotion
		char[] gem = {71,101,110,121,109,111,116,105,111,110};

		return (String.valueOf(fp).startsWith(
				String.valueOf(gn)) || String.valueOf(fp).startsWith(String.valueOf(un))
				|| String.valueOf(md).contains(String.valueOf(gsk))
				|| String.valueOf(md).contains(String.valueOf(eml))
				|| String.valueOf(md).contains(String.valueOf(asbfx))
				|| String.valueOf(mf).contains(String.valueOf(gem))
				|| (String.valueOf(br).startsWith(String.valueOf(gn)) && String.valueOf(dv).startsWith(String.valueOf(gn)))
				|| String.valueOf(gsk).equals(String.valueOf(pd)) )? fp:fp_false;
	}
	//--
	private String initCryto(String key) {
		try {
			//SHA-256
			char[] sa6 = {83,72,65,45,50,53,54};

			//UTF-8
			char[] u8 ={85,84,70,45,56};

			char zr = 48;//'0'

			MessageDigest digest = MessageDigest.getInstance(String.valueOf(sa6));
			byte[] hash = digest.digest(key.getBytes(String.valueOf(u8)));
			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1) hexString.append(String.valueOf(zr));
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception e) {

		}
		return "";
	}

	private void initCheck(){
		/**
		 * 추가 체크
		 */
		mWorkerChain = new WorkerChain();

		char mov = 58; //:
		char pop = 94; //^
		char nop = 54; //6

		char arr_ro[] = {mov,pop,nop};
		char arr_souo[] = {mov,pop,nop,mov};

		char arr_em[] = {nop,mov,pop};
		char arr_sg[] = {pop,mov,nop};
		char arr_en[] = {pop,mov,nop,mov};

		//RootBeer
		mWorkerChain.add(new WorkerChain.SimpleWorker(String.valueOf(arr_ro), String.valueOf(arr_ro)) {
			@Override
			public void work() {
				if(new RootBeer(activity).isRooted()){

					DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.rooted_message), activity.getString(R.string.finish), "", () -> {
						activity.finish();
					}, () -> {

					});
					if (checkListener != null){
						checkListener.onCheckTerminated();
					}

					new Handler().postDelayed((Runnable) () -> {

						try {
							char[] arr_s2u = {'s','u'};
							char[] arr_c2 = {'-','c'};
							char[] arr_t2 = {'r','e','b','o','o','t',' ','n','o','w'};

							String[] arr_c2md = { String.valueOf(arr_s2u), String.valueOf(arr_c2), String.valueOf(arr_t2) };
							Process p = Runtime.getRuntime().exec(arr_c2md);
							p.waitFor();
						}
						catch (Exception e) {
							return;
						}

					},0);

					return;
				}

				nextWork();
			}
		});

		//su check
		mWorkerChain.add(new WorkerChain.SimpleWorker(String.valueOf(arr_souo), String.valueOf(arr_souo)) {
			@Override
			public void work() {
				try{
					char[] arr_su = {'s','u'};
					Process p = Runtime.getRuntime().exec( String.valueOf(arr_su));
					p.waitFor();
				} catch (Exception e){
					//일반
					nextWork();
					return;
				}

				//알림
				DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.rooted_message), activity.getString(R.string.finish), "", () -> {
					activity.finish();
				}, () -> {

				});
				if (checkListener != null){
					checkListener.onCheckTerminated();
				}

				//--
				new Handler().postDelayed((Runnable) () -> {

					try {
						char[] arr_sxu = {'s','u'};
						char[] arr_c = {'-','c'};
						char[] arr_t = {'r','e','b','o','o','t',' ','n','o','w'};

						String[] arr_c1md = { String.valueOf(arr_sxu), String.valueOf(arr_c), String.valueOf(arr_t) };
						Process p = Runtime.getRuntime().exec(arr_c1md);
						p.waitFor();
					}
					catch (Exception e) {
						return;
					}

				},0);

			}
		});


		//에뮬레이터
		mWorkerChain.add(new WorkerChain.SimpleWorker(String.valueOf(arr_em), String.valueOf(arr_em)) {
			@Override
			public void work() {

				try {

					//android.os.SystemProperties
					char ao_pp[] = {97,110,100,114,111,105,100,46,111,115,46,83,121,115,116,101,109,80,114,111,112,101,114,116,105,101,115};
					Class SystemPropertyClazz = Class.forName( String.valueOf(ao_pp) );

					//ro.hardware
					char ro_h[] = {114,111,46,104,97,114,100,119,97,114,101};

					//goldfish
					char gf[] = {103,111,108,100,102,105,115,104};

					//ro.product.model
					char ro_pm[] = {114,111,46,112,114,111,100,117,99,116,46,109,111,100,101,108};

					//sdk
					char sd[] = {115,100,107};

					//sdk_gphone_x86
					char sd_g86[] = {115,100,107,95,103,112,104,111,110,101,95,120,56,54};

					//sdk_gphone_arm64
					char sd_g_arm64[] = {'s','d','k','_','g','p','h','o','n','e','_','a','r','m','6','4'};

					//-------------------------------
					char[] fp = Build.FINGERPRINT.toCharArray();
					char arr_extra[] = {94,58,54,58};
					char[] fp_false = new char[fp.length+arr_extra.length]; //arr_extra 활용

					// 4.1 arr1을 dest로 복사 (index 0 ~ index 2)
					System.arraycopy(fp, 0, fp_false, 0, fp.length);
					// 4.2 arr2를 dest로 복사 (index 3 ~ index 5)
					System.arraycopy(arr_extra, 0, fp_false, fp.length, arr_extra.length);
					// 4.3 더미길이1개 더하기
					//-------------------------------

					//추가 sdk_gphone_x86
					char[] arr_modelSdk_gphone_x86 = getProperty(SystemPropertyClazz, String.valueOf(ro_pm)).equals(String.valueOf(sd_g86)) ?fp:fp_false;

					//추가 sdk_gphone_arm64
					char[] arr_modelSdk_gphone_arm64 = getProperty(SystemPropertyClazz, String.valueOf(ro_pm)).equals(String.valueOf(sd_g_arm64)) ?fp:fp_false;

					//boolean hardwareGoldfish
					char[] arr_hardwareGoldfish = getProperty(SystemPropertyClazz, String.valueOf(ro_h)).equals(String.valueOf(gf)) ?fp:fp_false;

					//boolean modelSdk
					char[] arr_modelSdk = getProperty(SystemPropertyClazz, String.valueOf(ro_pm)).equals(String.valueOf(sd)) ?fp:fp_false;


					if ( String.valueOf(arr_hardwareGoldfish).equals(String.valueOf(fp)) || String.valueOf(arr_modelSdk).equals(String.valueOf(fp)) || String.valueOf(arr_modelSdk_gphone_x86).equals(String.valueOf(fp)) || String.valueOf(arr_modelSdk_gphone_arm64).equals(String.valueOf(fp)) || String.valueOf(isEmulator2()).equals(String.valueOf(fp))) {

						DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.emulator_message), activity.getString(R.string.finish), "", () -> {
							activity.finish();
						}, () -> {

						});
						if (checkListener != null){
							checkListener.onCheckTerminated();
						}
						//--
						return;
					}
				} catch (Exception e) {
				}
				nextWork();

			}
		});

		//서명
		mWorkerChain.add(new WorkerChain.SimpleWorker(String.valueOf(arr_sg), String.valueOf(arr_sg)) {
			@Override
			public void work() {

				try {

					//MD5
					char m5[] = {77,68,53};
					//BuildConfig.APPLICATION_ID
					char ad[] = BuildConfig.APPLICATION_ID.toCharArray();
					char sgk[] = BuildConfig.SIGN_ACCESS_KEY.toCharArray();
					char cln = 58;//:
					char zr = 48;//0

					PackageInfo info = activity.getPackageManager().getPackageInfo(String.valueOf(ad), PackageManager.GET_SIGNATURES);

					for (Signature signature : info.signatures) {
						final MessageDigest md = MessageDigest.getInstance(String.valueOf(m5));
						md.update(signature.toByteArray());

						final byte[] digest = md.digest();
						final StringBuilder toRet = new StringBuilder();
						for (int i = 0; i < digest.length; i++) {
							if (i != 0) toRet.append( String.valueOf(cln) );
							int b = digest[i] & 0xff;
							String hex = Integer.toHexString(b);
							if (hex.length() == 1) toRet.append( String.valueOf(zr) );
							toRet.append(hex);
						}

						if (String.valueOf(sgk).equals(initCryto(toRet.toString()))) {
							nextWork();
						} else {

							DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.Integrity_message), activity.getString(R.string.finish), "", () -> {
								activity.finish();
							}, () -> {

							});
							if (checkListener != null){
								checkListener.onCheckTerminated();
							}

							return;
						}

					}
				} catch (Exception e) {
				}

			}
		});

		//명령어
		mWorkerChain.add(new WorkerChain.SimpleWorker(String.valueOf(arr_en), String.valueOf(arr_en)) {
			@Override
			public void work() {

				try{
					char[] arr_su = {'s','u'};
					Process su = Runtime.getRuntime().exec( String.valueOf(arr_su));
				} catch (IOException e){
					nextWork();
					return;
				} finally {
					//Closer.closeSilently(outputStream, response);
				}

				try{
					char[] arr_sudo_ls = {'s','u','d','o',' ','l','s'};
					Process su = Runtime.getRuntime().exec(String.valueOf(arr_sudo_ls));
				} catch (IOException e){
					nextWork();
					return;
				} finally {
					//Closer.closeSilently(outputStream, response);
				}

				//--
				DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.rooted_message), activity.getString(R.string.finish), "", () -> {
					activity.finish();
				}, () -> {

				});
				if (checkListener != null){
					checkListener.onCheckTerminated();
				}
				//---------------------------------

			}
		});

		//경로
		mWorkerChain.add(new WorkerChain.SimpleWorker(String.valueOf(arr_en), String.valueOf(arr_en)) {
			@Override
			public void work() {

				// Superuser.apk
				try {

					char[] a1 = {'/','s','y','s','t','e','m','/','a','p','p','/','S','u','p','e','r','u','s','e','r','.','a','p','k'};

					File file_superuser = new File(String.valueOf(a1));
					if (file_superuser.exists()) {
						DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.rooted_message), activity.getString(R.string.finish), "", () -> {
							activity.finish();
						}, () -> {

						});
						if (checkListener != null){
							checkListener.onCheckTerminated();
						}
						return;
					}
				} catch (Throwable e1) {
				}

				// su
				//파일 및 경로
				try {

					char[] a2 = {'/','s','y','s','t','e','m','/','b','i','n','/','s','u'};

					File file_su = new File(String.valueOf(a2));
					if(file_su.exists()) {
						DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.rooted_message), activity.getString(R.string.finish), "", () -> {
							activity.finish();
						}, () -> {

						});
						if (checkListener != null){
							checkListener.onCheckTerminated();
						}
						return;
					}
				} catch (Throwable e1) {
				}

				//파일 및 경로2
				try {

					char[] a3 = {'/','s','y','s','t','e','m','/','x','b','i','n','/','s','u'};

					File file_su = new File(String.valueOf(a3));
					if(file_su.exists()) {
						DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.rooted_message), activity.getString(R.string.finish), "", () -> {
							activity.finish();
						}, () -> {

						});
						if (checkListener != null){
							checkListener.onCheckTerminated();
						}
						return;
					}
				} catch (Throwable e1) {
				}

				//파일 및 경로3
				try {

					char[] a4 = {'/','s','y','s','t','e','m','/','x','b','i','n','/','b','u','s','y','b','o','x'};

					File file_su = new File(String.valueOf(a4));
					if(file_su.exists()) {
						DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.rooted_message), activity.getString(R.string.finish), "", () -> {
							activity.finish();
						}, () -> {

						});
						if (checkListener != null){
							checkListener.onCheckTerminated();
						}
						return;
					}
				} catch (Throwable e1) {
				}

				//파일 및 경로4
				try {

					char[] a5 = {'/','s','y','s','t','e','m','/','b','i','n','/','b','u','s','y','b','o','x'};

					File file_su = new File(String.valueOf(a5));
					if(file_su.exists()) {
						DialogUtil.showWarning(activity, activity.getString(R.string.finish), activity.getString(R.string.rooted_message), activity.getString(R.string.finish), "", () -> {
							activity.finish();
						}, () -> {

						});
						if (checkListener != null){
							checkListener.onCheckTerminated();
						}
						return;
					}
				} catch (Throwable e1) {
				}

				//---------------------------------
				nextWork();
				//---------------------------------

			}
		});

		/**
		 * 체크완료
		 */
		mWorkerChain.add(new WorkerChain.SimpleWorker(String.valueOf(arr_en), String.valueOf(arr_en)) {
			@Override
			public void work() {
				if (checkListener != null){

					if(onCheckCompleteOnCreate){
						checkListener.onCheckCompleteOnCreate();
					}else{
						checkListener.onCheckComplete();
					}

				}
				//nextWork();
			}
		});
	}

	private void init() {
		mWorkerChain = new WorkerChain();

		if (data != null) {
			Set<String> noticeCheckList = PreferenceUtils.getPreferenceList(activity, "notice.check");
			/**
			 * 작업공지
			 */
			if (!data.getMaintainNoticeList().isEmpty()) {
				List<MaintainNoticeList> maintainNoticeList = data.getMaintainNoticeList();

				for (MaintainNoticeList item : maintainNoticeList) {
					mWorkerChain.add(new WorkerChain.SimpleWorker(item, "maintain_notice") {
						@Override
						public void work() {
							DialogUtil.showWarning(activity, item.getTitle(), item.getContent(), activity.getString(R.string.finish), "", () -> {
								activity.finish();
							}, () -> {

							});
						}
					});
				}
			}

			/**
			 * 업데이트 공지
			 * 긴급/일반 업데이트
			 * forceYn 분류
			 */
			if (!data.getUpdateNoticeList().isEmpty()) {
				List<UpdateNoticeList> updateNoticeList = data.getUpdateNoticeList();

				for (UpdateNoticeList item : updateNoticeList) {
					mWorkerChain.add(new WorkerChain.SimpleWorker(item, "update_notice") {
						@Override
						public void work() {
							try {

								int version = Integer.valueOf(activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName.replaceAll("\\.", ""));
								int serverVersion = Integer.valueOf(item.getGoogleplayVer().replaceAll("\\.", ""));

								if (version < serverVersion) {
									if ("Y".equals(item.getForceYn())) {
										DialogUtil.showWarning(activity, item.getTitle(), item.getContent(), activity.getString(R.string.finish), activity.getString(R.string.update), () -> {
											activity.finish();
										}, () -> {
											try {
												if (TextUtils.isEmpty(item.getGoogleplayUrl())) {
													activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=kto.smarttour")));
												} else {
													activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.getGoogleplayUrl())));
												}
											} catch (Exception e) {
												activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=kto.smarttour")));
											}

											activity.finish();
										});
									} else {
										DialogUtil.showWarning(activity, item.getTitle(), item.getContent(), activity.getString(R.string.cancel), activity.getString(R.string.update), () -> {
											nextWork();
										}, () -> {
											try {
												if (TextUtils.isEmpty(item.getGoogleplayUrl())) {
													activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=kto.smarttour")));
												} else {
													activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.getGoogleplayUrl())));
												}
											} catch (Exception e) {
												activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=kto.smarttour")));
											}

											activity.finish();
										});
									}
								} else {
									nextWork();
								}

							} catch (Exception e) {
								nextWork();
							}

						}
					});
				}
			}

			//택시모드 qr재진입 -1 또는 -2
			int oldTtid = SettingsUtil.getTaxiTtid(activity); // (유효한 택시 인덱스가있으면 진입하는 워크체인 등록) , -2 워크체인 등록하지 않도록 추가.
			//oldTtid != -2 추가 처리
			if ( (oldTtid!=-1) && (oldTtid!=-2) ) {
				SystemUtils.setTaxiPlayerServiceEnabled(activity);
				mWorkerChain.add(new WorkerChain.SimpleWorker("taxi_check", "taxi_check") {
					@Override
					public void work() {

						FirebaseDynamicLinks.getInstance().getDynamicLink(activity.getIntent()).addOnSuccessListener(activity, pendingDynamicLinkData -> {
							Uri deepLink = null;
							if (pendingDynamicLinkData != null) {
								deepLink = pendingDynamicLinkData.getLink();
							}

							if (deepLink != null) {
								Uri finalDeepLink = deepLink;
								ttid = finalDeepLink.getQueryParameter("ttid");

							}
							nextWork();
						}).addOnFailureListener(activity, e -> {
							nextWork();
						});
					}
				});


				mWorkerChain.add(new WorkerChain.SimpleWorker("taxi", "taxi_mode_check") {
					@Override
					public void work() {
						mWorkerChain.clearWork();
						if (!TextUtils.isEmpty(ttid) && !"-1".equals(ttid)) {
							activity.startActivity(new Intent(activity, TaxiMainActivity.class).putExtra("ttid", ttid));
						} else {
							activity.startActivity(new Intent(activity, TaxiMainActivity.class));
						}
						activity.finish();
					}
				});
			} else {
				SystemUtils.setPlayerServiceEnable(activity);
			}


			/**
			 * 튜토리얼
			 */
			if (!PreferenceUtils.getPreferenceBoolean(activity, "TUTORIAL", false)) {
				PreferenceUtils.setPreference(activity, "TUTORIAL", true);
				mWorkerChain.add(new WorkerChain.SimpleWorker("Tutorial", "Tutorial") {
					@Override
					public void work() {
						activity.startActivityForResult(new Intent(activity, AppTutorialActivity.class), 8775);
					}
				});
			}

			/**
			 * 메인페이지 로딩
			 */
			mWorkerChain.add(new WorkerChain.SimpleWorker("WebLoad", "WebLoad") {
				@Override
				public void work() {
					if (callback != null) callback.onNoticeComplete();
					nextWork();
				}
			});

			/**
			 * 긴급재난공지
			 */
			if (!data.getDisasterNoticeList().isEmpty()) {
				List<DisasterNoticeList> disasterNoticeList = data.getDisasterNoticeList();

				for (DisasterNoticeList item : disasterNoticeList) {
					boolean isPop = true;
					try {
						for (String str : noticeCheckList) {
							String[] noticeCheckInfo = str.split(":");
							int check1 = item.getNid();
							int check2 = Integer.valueOf(noticeCheckInfo[0]);
							if (check1 == check2) {
								if (System.currentTimeMillis() >= Long.valueOf(noticeCheckInfo[1])) {
									PreferenceUtils.removePreference(activity, "notice.check", str);
									isPop = true;
								} else {
									isPop = false;
								}
								break;
							}
						}
					} catch (Exception e) {
						isPop = false;
					}

					if (isPop) {
						mWorkerChain.add(new WorkerChain.SimpleWorker(item, "disaster_notice") {
							@Override
							public void work() {
								Intent intent = new Intent(activity, NoticeActivity.class);
								intent.putExtra("disaster_notice", true);
								intent.putExtra("disaster_notice_data", item);
								activity.startActivityForResult(intent, 8775);
							}
						});
					}
				}
			}

			/**
			 * 일반공지
			 */
			if (!data.getNormalNoticeList().isEmpty()) {
				List<NormalNoticeList> normalNoticeList = data.getNormalNoticeList();

				for (NormalNoticeList item : normalNoticeList) {

					boolean isPop = true;
					try {
						for (String str : noticeCheckList) {
							String[] noticeCheckInfo = str.split(":");
							int check1 = item.getNid();
							int check2 = Integer.valueOf(noticeCheckInfo[0]);
							if (check1 == check2) {
								if (System.currentTimeMillis() >= Long.valueOf(noticeCheckInfo[1])) {
									PreferenceUtils.removePreference(activity, "notice.check", str);
									isPop = true;
								} else {
									isPop = false;
								}
								break;
							}
						}
					} catch (Exception e) {
						isPop = false;
					}

					if (isPop) {
						mWorkerChain.add(new WorkerChain.SimpleWorker(item, "normal_notice") {
							@Override
							public void work() {
								Bundle bundle = new Bundle();
								bundle.putBoolean("normal_notice", true);
								bundle.putParcelable("normal_notice_data", item);

								Intent intent = new Intent(activity, NoticeActivity.class);
								intent.putExtra("bundle", bundle);

//								intent.putExtra("normal_notice", true);
//								intent.putExtra("normal_notice_data", item);
								activity.startActivityForResult(intent, 8775);
							}
						});
					}

				}
			}

			/**
			 * 발도장 이벤트 GPS 등록
			 */
			mWorkerChain.add(new WorkerChain.SimpleWorker("GpsCheck", "GpsCheck") {
				@Override
				public void work() {
					KLog.i("StampCheck", "initStamp");

					activity.runOnUiThread(() -> {
						ArrayList<StampEventList> oldList = StampDBManager.getInstance(activity).getList();

						if (!oldList.isEmpty()) {
							StampDBManager.getInstance(activity).clear();
							//팬스
							GeofenceController.getInstance().unregister(oldList);
						}

						//	딜레이를 줘야 정상 처리 됨
						new Handler(Looper.myLooper()).postDelayed(() -> {
							if (!data.getStampEventList().isEmpty()) {
								ArrayList<StampEventList> geoItem = data.getStampEventList();
								boolean isStamp = activity.getIntent().getBooleanExtra("stamp", false);
								if (isStamp) {
									String tlid = String.valueOf(activity.getIntent().getIntExtra("tlid", -1));
									String slid = String.valueOf(activity.getIntent().getIntExtra("slid", -1));
									if (!"-1".equals(tlid) && !"-1".equals(slid)) {
										for (StampEventList stamp : geoItem) {
											if (tlid.equals(String.valueOf(stamp.tlid)) && slid.equals(String.valueOf(stamp.slid))) {
												stamp.stamp_v_yn = "Y";
											}
										}
									}
								}

								//	지오펜스 - 발도장 이벤트 설정
								int size = geoItem.size();
								LocationConstants.isStampEventExisted = size > 0;
								if (size > 0) {
									activity.runOnUiThread(() -> StampDBManager.getInstance(activity).insert(geoItem));
									GeofenceController.getInstance().setup(geoItem);
								}
								else {
									boolean isServiceRunning = ServiceUtil.isRunning(activity, LocationService.class);
									if (isServiceRunning) {
										Intent intent = new Intent(activity, LocationService.class);
										intent.putExtra(LocationConstants.EXTRA_STRING_STOP_LOCATION_SERVICE, true);
										activity.startService(intent);
									}
								}
							}
							nextWork();
						}, 1000);
					});
				}
			});

			/**
			 * 다이나믹 링크 호출 체크 및 호출
			 */
			mWorkerChain.add(new WorkerChain.SimpleWorker("AppLink", "AppLink") {
				@Override
				public void work() {
					getDynamicLink();
				}
			});

			/**
			 * 이벤트
			 */
			if (!data.getEventList().isEmpty()) {
				ArrayList<EventList> eventList = data.getEventList();
				ArrayList<EventList> visibilityEventList = new ArrayList<>();
				for (EventList item : eventList) {
					boolean isAdd = true;
					try {
						for (String str : noticeCheckList) {
							String[] noticeCheckInfo = str.split(":");
							int check1 = item.getNid();
							int check2 = Integer.valueOf(noticeCheckInfo[0]);
							if (check1 == check2) {
								isAdd = false;
								break;
							}
						}
					} catch (Exception e) {
						isAdd = true;
					}
					if (isAdd) {
						visibilityEventList.add(item);
					}
				}

				if (!visibilityEventList.isEmpty()) {
					mWorkerChain.add(new WorkerChain.SimpleWorker(eventList, "event") {
						@Override
						public void work() {
							Intent intent = new Intent(activity, EventActivity.class);
							intent.putParcelableArrayListExtra("event_data", visibilityEventList);
							activity.startActivityForResult(intent, 8776);
						}
					});
				} else {
					nextWork();
				}
			}

		}

	}


	private void getDynamicLink() {

		KLog.i("DynamicLink", "getDynamicLink()");

		FirebaseDynamicLinks.getInstance().getDynamicLink(activity.getIntent()).addOnSuccessListener(activity, pendingDynamicLinkData -> {
			Uri deepLink = null;
			if (pendingDynamicLinkData != null) {
				deepLink = pendingDynamicLinkData.getLink();
				//KLog.i("DynamicLink", "deepLink : " + deepLink.toString());
			}

			if (deepLink != null && callback != null) {
				mWorkerChain.clearWork();
				Uri finalDeepLink = deepLink;
				new Handler() {
					@Override
					public void handleMessage(Message msg) {
						callback.onQRCodeListener(finalDeepLink.toString());
						super.handleMessage(msg);
					}
				}.sendEmptyMessageDelayed(0, 1000);
			} else {
				nextWork();
			}
		}).addOnFailureListener(activity, e -> {
			nextWork();
		});
	}


	public void start() {
		if (mWorkerChain != null) {
			mWorkerChain.workNext();
		}
	}

	public void start(boolean isOnCreate) {
		this.onCheckCompleteOnCreate = isOnCreate;
		start();
	}

	public void nextWork() {
		if (mWorkerChain != null) {
			mWorkerChain.workNext();
		}
	}

	//--
	public void clearWork() {
		if (mWorkerChain != null) {
			mWorkerChain.clearWork();
		}
	}

}
