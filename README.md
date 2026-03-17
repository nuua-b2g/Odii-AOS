# 오디 프로젝트

작성자 paul

업데이트 2024.03.08

# 개발전 확인사항

### 1. Local 환경별 BASE_URL 변경 방법.
local.properties에 다음과 같이 프로퍼티 추가
```properties
BASE_URL_LOCAL="https://odii.web.dev"
```

# 변경 사항 검색 [주석]
Ntcon 으로 검색시 변경사항 확인이 가능합니다.

# 업데이트 로그 (2024년 5월 10일):

NewSplash 페이지 생성:
    - 새로운 페이지인 NewSplash가 생성되었습니다. 이 페이지는 권한 안내를 위한 페이지로 구성되어 있습니다.
    - NewSplash 페이지는 앱의 새로운 메인 화면으로 지정되었습니다.
AndroidManifest 변경:
    - AndroidManifest 파일의 48-89번 라인이 수정되었습니다. 이 부분은 새로운 메인 화면인 NewSplash 페이지를 반영하기 위한 변경입니다.
번역화 작업 완료:
    - 새로운 화면의 번역화 작업 또한 완료되었습니다.
이슈 :
    - 첫 번째 문제로, string 리소스 안에 번역이 되지 않은 키값들이 여러 개 존재합니다. 이 문제를 해결하기 위해 번역되지 않은 키값들을 확인하고, 번역을 추가하거나 수정할 필요가 있습니다.
    - 두 번째 문제로, 현재 스크린에는 안드로이드 권한을 4개 요청하도록 설정되어 있지만, 스크린에는 2개만 나타나고 있습니다. 추가적인 두 개의 권한이 필요한지 여부를 고려해야 합니다. 새로운 권한을 추가할 경우, iOS와의 디자인 일관성이 깨지게 됩니다.

# 업데이트 로그 (2026.03.17):

## ignoreRooting을 자동으로 설정하도록 변경.
기존 개발 전 debug에서 `ignoreRooting = true` 로 변경해야 하는 작업을.
```java
/** 변경 전 **/
//public static boolean ignoreRooting = false;
public static boolean ignoreRooting = true;

/** 변경 후 **/
public static boolean ignoreRooting = "debug".equals(BuildConfig.BUILD_TYPE);
```
로 변경함으로써 debug 모드에서 자동으로 ignoreRooting이 적용되도록 변경함.

## CommonUrl.BASE_URL local 환경 구축

기존 주석으로 관리하던 URL을
```java
//public static final String BASE_URL = "https://www.odii.kr";//운영서버 (내재화 ssl 2022.07월부터 앱에서 사용)
//public static final String BASE_URL = "https://odii.witches.co.kr";//개발서버 (위치스 재구축)
public static final String BASE_URL = "http://121.131.208.44:18080"; // 엔티콘 개발 서버 
```

BuildConfig로 옮김으로써 별도의 수정사항 없이도 로컬 환경에서 손쉽게 변경할 수 있도록 함. 
```java
public static final String BASE_URL = BuildConfig.BASE_URL;
```

```groovy
buildTypes {
    release {
//      ...

        String releaseUrl = "\"https://www.odii.kr\""//운영서버 (내재화 ssl 2022.07월부터 앱에서 사용)
        buildConfigField("String", "BASE_URL", releaseUrl)
    }
    debug {
//      ...

        Properties localProperties = new Properties()
        File file = rootProject.file("local.properties")
        if (file.exists()) {
            FileInputStream inputStream = new FileInputStream(file)
            localProperties.load(inputStream)
            inputStream.close()
        }
        String debugUrl = localProperties.getProperty("BASE_URL_LOCAL")
                ?: "\"http://121.131.208.44:18080\"" //엔티콘 개발 서버
        buildConfigField("String", "BASE_URL", debugUrl)
    }
}
```

## AGP 버전 변경 8.13.2

### 변경 사유: 구글 권장
```groovy
/* 변경 전 */
classpath 'com.android.tools.build:gradle:8.9.1'

/* 변경 후 */
classpath 'com.android.tools.build:gradle:8.13.2'
```

```properties
# 변경 전
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip

# 변경 후
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
```