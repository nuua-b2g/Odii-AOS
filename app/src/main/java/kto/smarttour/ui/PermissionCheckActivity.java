package kto.smarttour.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import kto.smarttour.R;
import kto.smarttour.common.BaseActivity;
import kto.smarttour.common.consts.URLS;
import kto.smarttour.common.utils.DialogPerUtil;
import kto.smarttour.common.utils.DialogUtil;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.SystemUtils;
import kto.smarttour.common.utils.ViewUtils;

/**
 * 권한체크 화면
 */
public class PermissionCheckActivity extends BaseActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

	private AlertDialog dialogWarning;
	boolean drive = false;
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.permission_layout);

		boolean onlyGps = getIntent().getBooleanExtra("onlyGps", false);
		drive = getIntent().getBooleanExtra("drive", false);

		if(onlyGps) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				//	내용 중 발도장 이벤트에 관련된 내용이 있으므로 팝업 제거
//				dialogWarning = DialogPerUtil.showWarning(this, getString(R.string.permission_title_3), getString(R.string.permission_content_3), getString(R.string.ok), getString(R.string.cancel), () -> {

					//안드로이드 11 Build.VERSION_CODES.R 부터
					//주의: 앱이 Android 11(API 수준 30) 이상을 타겟팅하면 시스템에서는 이 권장사항을 적용합니다. 포그라운드 위치 정보 액세스 권한과 백그라운드 위치 정보 액세스 권한을 동시에 요청하면 시스템이 요청을 무시하고 앱에 어떤 권한도 부여하지 않습니다.
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

						//ACCESS_BACKGROUND_LOCATION 동시요청 금지 제외
						String[] permissionList = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };

						ActivityCompat.requestPermissions(this, permissionList, 6153);

					}else {

						//지오팬스 ACCESS_BACKGROUND_LOCATION
						//String[] permissionList = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
						String[] permissionList = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };

						ActivityCompat.requestPermissions(this, permissionList, 5555);

					}

//				}, () -> {
//					finish();
//				});
			} else {
				dialogWarning = DialogPerUtil.showWarning(this, getString(R.string.permission_title), getString(R.string.permission_content_4), getString(R.string.ok), getString(R.string.cancel), () -> {
					String[] permissionList = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };
					ActivityCompat.requestPermissions(this, permissionList, 5555);
				}, () -> {
					finish();
				});
			}
		} else if(drive) {
			dialogWarning = DialogPerUtil.showWarning(this, getString(R.string.permission_title), getString(R.string.permission_content_4), getString(R.string.ok), getString(R.string.cancel), () -> {
				String[] permissionList = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };
				ActivityCompat.requestPermissions(this, permissionList, 4600);
			}, () -> {
				finish();
			});
		} else {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//				dialogWarning = DialogPerUtil.showWarning(this, getString(R.string.permission_title), getString(R.string.permission_content), getString(R.string.ok), getString(R.string.cancel), () -> {

					//안드로이드 11 Build.VERSION_CODES.R 부터
					//주의: 앱이 Android 11(API 수준 30) 이상을 타겟팅하면 시스템에서는 이 권장사항을 적용합니다. 포그라운드 위치 정보 액세스 권한과 백그라운드 위치 정보 액세스 권한을 동시에 요청하면 시스템이 요청을 무시하고 앱에 어떤 권한도 부여하지 않습니다.
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
						//ACCESS_BACKGROUND_LOCATION 동시요청 금지 제외
						String[] permissionList = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };

						ActivityCompat.requestPermissions(this, permissionList, 6153);

					}else {
						//String[] permissionList = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
						String[] permissionList = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };

						ActivityCompat.requestPermissions(this, permissionList, 5555);

					}

//				}, () -> {
//					finish();
//				});
			} else {
				dialogWarning = DialogPerUtil.showWarning(this, getString(R.string.permission_title), getString(R.string.permission_content_2), getString(R.string.ok), getString(R.string.cancel), () -> {
					String[] permissionList = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };
					ActivityCompat.requestPermissions(this, permissionList, 5555);
				}, () -> {
					finish();
				});
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

		if (grantResults.length > 0) {

			boolean showPermissionUI = false;
			//boolean neverAskAgin = false;
			boolean isRerequest = false;
			final boolean isRerequestFinal;

			//--
			//boolean isDenied_ACCESS_BACKGROUND_LOCATION = false;
			int size_PERMISSION_DENIED = 0;

			for (int i = 0; i < permissions.length; i++) {
				String permission = permissions[i];
				if (grantResults[i] == PackageManager.PERMISSION_DENIED) {

					size_PERMISSION_DENIED++;

					if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
						// 권한안내 UI
						showPermissionUI = true;
					}
					isRerequest = true;

					//검출
//					String deniedPermissionName = permissions[i];
//					if(deniedPermissionName.equalsIgnoreCase( Manifest.permission.ACCESS_BACKGROUND_LOCATION )){
//						isDenied_ACCESS_BACKGROUND_LOCATION = true;
//					}

				}

			}

			if (showPermissionUI) {
				//안드로이드 10 또는 11이상에서 권한요청, 위치팬스 '위치 항상허용' 필요
//				if(size_PERMISSION_DENIED==1 && isDenied_ACCESS_BACKGROUND_LOCATION){
//					//ACCESS_BACKGROUND_LOCATION 권한만 안내하는 경우
//					if(requestCode!=6155) {
////						dialogWarning = DialogPerUtil.showWarning(this, getString(R.string.permission_content_location_alltime_title), getString(R.string.permission_content_location_alltime), getString(R.string.settings), getString(R.string.cancel), () -> {
//							//안드로이드 10에서 한번더 사용자에게 권한 안내 및 설정요청
//							String[] permissionList = {Manifest.permission.ACCESS_BACKGROUND_LOCATION};
//							ActivityCompat.requestPermissions(this, permissionList, 6155);
////						}, () -> {
////							setResult(Activity.RESULT_OK);
////							finish();
////						});
////						return;
//					}
//
//				}else{
					//--
					//기존 짧은 올바른 권살설정안내 (저장소 및 일반위치 권한)
					DialogUtil.showWarning(this, "", getString(R.string.permission_rquest_message), getString(R.string.settings), getString(R.string.cancel), () -> {
						Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getPackageName()));
						startActivity(intent);
						finish();
					}, () -> {
						setResult(Activity.RESULT_OK);
						finish();
					});
					//return;
//				}

				//======================================================================
//				if (!isRerequest) {
//					setResult(Activity.RESULT_OK);
//				}
//				finish();

			} else {
				// 권한안내 불필요, 여러 권한의 shouldShowRequestPermissionRationale 결과 전부해당하지 않음
				//shouldShowRequestPermissionRationale 에서 교육용 UI를 표시하지 않아도 된다고 나타내면 권한을 요청합니다
				if(drive) {
					finish();
				} else {

					//조건추가 isDenied_ACCESS_BACKGROUND_LOCATION를 제외한 경우에만 호출
//					if(size_PERMISSION_DENIED==1 && isDenied_ACCESS_BACKGROUND_LOCATION){
//
//						if(requestCode!=6155) {
//							// 권한안내 UI표시 불필요, 개발문서 기준 바로권한 요청코드를 사용
//							String[] permissionList = {Manifest.permission.ACCESS_BACKGROUND_LOCATION};
//							ActivityCompat.requestPermissions(this, permissionList, 6155);
//						}else {
//							finish();
//						}
//
//					}else{
						if(isRerequest){
							//저장소 및 일반위치 권한
							DialogUtil.showWarning(this, "", getString(R.string.permission_rquest_message), getString(R.string.settings), getString(R.string.cancel), () -> {
								Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getPackageName()));
								startActivity(intent);
								finish();
							}, () -> {
								finish();
							});

						}else{
							//안드로이드 11이상에서 포어그라운드와 백그라운드 권한중 백그라운드ACCESS_BACKGROUND_LOCATION을 제외하고 권한요청시 거부된 권한없음 상태로 들어옴
							if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
								//ACCESS_BACKGROUND_LOCATION 권한사용가능 android 10이상
								//ACCESS_BACKGROUND_LOCATION 활성화 여부 체크
								String checkTargetPermissions[] = {Manifest.permission.ACCESS_BACKGROUND_LOCATION};
								ArrayList<String> resPermission = SystemUtils.checkSelfPermission(this, checkTargetPermissions);
								if (resPermission.size() == 0) {
									//ACCESS_BACKGROUND_LOCATION
									setResult(Activity.RESULT_OK);
									finish();
								}else{
									//거부된 일반권한은 없지만 안드로이드 11이상용 ACCESS_BACKGROUND_LOCATION 권한 획득 필요.
									//ACCESS_BACKGROUND_LOCATION 권한만 안내하는 경우에 해당, requestCode 6153은 ACCESS_BACKGROUND_LOCATION만 요청해였음.
//									dialogWarning = DialogPerUtil.showWarning(this, getString(R.string.permission_title), getString(R.string.permission_content_location_alltime), getString(R.string.settings), getString(R.string.cancel), () -> {
//										String[] permissionList = {Manifest.permission.ACCESS_BACKGROUND_LOCATION};
//										ActivityCompat.requestPermissions(this, permissionList, 6155);
//
//									}, () -> {
										setResult(Activity.RESULT_OK);
										finish();
//									});
								}
							}else{
								setResult(Activity.RESULT_OK);
								finish();
							}

						}

//					}

				}

			}

		} else {
			setResult(Activity.RESULT_OK);
			finish();
		}

		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public void finish() {

		//추가
		if(dialogWarning!=null){
			if(dialogWarning.isShowing()){
				dialogWarning.dismiss();
			}
		}

		super.finish();
		overridePendingTransition(0, 0);
	}

}