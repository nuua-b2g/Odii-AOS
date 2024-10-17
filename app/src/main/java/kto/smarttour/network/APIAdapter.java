package kto.smarttour.network;

import com.socks.library.KLog;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import kto.smarttour.BuildConfig;
import kto.smarttour.OdiiApplication;
import kto.smarttour.common.consts.URLS;
import kto.smarttour.common.utils.NetworkUtil;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class APIAdapter {

	private static int TIME_OUT = 0;
	private static int NETWORK_TIMEOUT = 10;

	protected static Object retrofit(Class<?> serviceName) {
		OkHttpClient okHttpClient = getOkHttpClient();

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(URLS.BASE_URL)
				.addConverterFactory(GsonConverterFactory.create())
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
				.client(okHttpClient)
				.build();

		return retrofit.create(serviceName);
	}

	protected static Object retrofit(String baseUrl, Class<?> serviceName) {
		OkHttpClient okHttpClient = getOkHttpClient();

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(baseUrl)
				.addConverterFactory(GsonConverterFactory.create())
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
				.client(okHttpClient)
				.build();

		return retrofit.create(serviceName);
	}

	private static OkHttpClient getOkHttpClient() {
		try {
			final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new java.security.cert.X509Certificate[]{};
				}
			}};

			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			OkHttpClient.Builder builder = new OkHttpClient.Builder();

			File httpCacheDirectory = new File(OdiiApplication.getContext().getCacheDir(), "responses");
			int cacheSize = 10 * 1024 * 1024;
			Cache cache = new Cache(httpCacheDirectory, cacheSize);

			HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
			interceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
			builder.addInterceptor(interceptor);
			builder.retryOnConnectionFailure(false);
			builder.cache(cache);

			builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
			builder.hostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});

			builder.addInterceptor(new Interceptor() {
				@Override
				public Response intercept(Chain chain) throws IOException {

					if (NetworkUtil.isNetworkConnected(OdiiApplication.getContext())) {
						Request request = chain.request();

						Response response = null;
						boolean responseOK = false;
						int tryCount = 0;
						int maxAge = 60; // read from cache for 1 minute
						while (!responseOK && tryCount < 3) {
							try {
								response = chain.proceed(request.newBuilder().header("Cache-Control", "public, max-age=" + maxAge).build());
								responseOK = response.isSuccessful();
							} catch (Exception e) {
								KLog.d("intercept", "Request is not successful - " + tryCount);
							} finally {
								tryCount++;
							}
						}

						if(response != null) {
							return response;
						} else {
							return chain.proceed(chain.request());
						}
					} else {
						return chain.proceed(chain.request());
					}

				}
			});

			if(TIME_OUT == 0) {
				builder.connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS);
				builder.readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS);
				builder.writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS);
			} else {
				builder.connectTimeout(TIME_OUT, TimeUnit.SECONDS);
				builder.readTimeout(TIME_OUT, TimeUnit.SECONDS);
				builder.writeTimeout(TIME_OUT, TimeUnit.SECONDS);
				TIME_OUT = 0;
			}



			return builder.build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setTimeOut(int timeOut) {
		TIME_OUT = timeOut;
	}
}

