/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import cn.wildfire.chat.kit.BuildConfig;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.net.base.ResultWrapper;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfire.chat.kit.qrcode.GetStringResult;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfirechat.model.WebResponse;
import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Util;


/**
 * Created by imndx on 2017/12/15.
 */

public class OKHttpHelper {
    private static final String TAG = "OKHttpHelper";
    private static final String WFC_OKHTTP_COOKIE_CONFIG = "WFC_OK_HTTP_COOKIES";
    private static final Map<String, List<Cookie>> cookieStore = new ConcurrentHashMap<>();
    private static final String AUTHORIZATION_HEADER = "authToken";

    private static String authToken = "";
    private static String userAgent = "";

    private static WeakReference<Context> AppContext;

    private static OkHttpClient okHttpClient;

    public static void init(Context context) {
        AppContext = new WeakReference<>(context);
        SharedPreferences sp = context.getSharedPreferences(WFC_OKHTTP_COOKIE_CONFIG, Context.MODE_PRIVATE);
        authToken = sp.getString(AUTHORIZATION_HEADER, null);
        initUserAgent(context);
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS);

        // 优先使用token认证
        if (TextUtils.isEmpty(authToken)) {
            builder.cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url.host(), cookies);
                    if (AppContext != null && AppContext.get() != null) {
                        SharedPreferences sp = AppContext.get().getSharedPreferences(WFC_OKHTTP_COOKIE_CONFIG, 0);
                        Set<String> set = new HashSet<>();
                        for (Cookie k : cookies) {
                            set.add(gson.toJson(k));
                        }
                        sp.edit().putStringSet(url.host(), set).apply();
                    }
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url.host());
                    if (cookies == null) {
                        if (AppContext != null && AppContext.get() != null) {
                            SharedPreferences sp = AppContext.get().getSharedPreferences(WFC_OKHTTP_COOKIE_CONFIG, 0);
                            Set<String> set = sp.getStringSet(url.host(), new HashSet<>());
                            cookies = new ArrayList<>();
                            for (String s : set) {
                                Cookie cookie = gson.fromJson(s, Cookie.class);
                                cookies.add(cookie);
                            }
                            cookieStore.put(url.host(), cookies);
                        }
                    }

                    return cookies;
                }
            });
        }
        builder.addInterceptor(chain -> {
            Request request = chain.request();
            if (!TextUtils.isEmpty(authToken)) {
                request = request.newBuilder()
                        .addHeader(AUTHORIZATION_HEADER, authToken)
                        .build();
            }
            Response response = chain.proceed(request);
            String responseAuthToken = response.header(AUTHORIZATION_HEADER, null);
            if (!TextUtils.isEmpty(responseAuthToken)) {
                authToken = responseAuthToken;
                // 重新登录之后，清除cookie，采用token进行认证
                sp.edit().clear().putString(AUTHORIZATION_HEADER, authToken).apply();
            }
            return response;
        });
        okHttpClient = builder.build();
    }

    private static void initUserAgent(Context context) {
        try {
            WebSettings settings = new WebView(context).getSettings();
            userAgent = settings.getUserAgentString();
            if (!TextUtils.isEmpty(userAgent)) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            userAgent = WebSettings.getDefaultUserAgent(context);
            if (!TextUtils.isEmpty(userAgent)) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            userAgent = System.getProperty("http.agent");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Gson gson = new Gson();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static <T> void get(final String url, Map<String, String> params, final Callback<T> callback) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        // 加入全域变数
        Map<String, String> globalVariables = getParam();
        if (params == null) {
            params = globalVariables;
        }
        if (params != null) {
            globalVariables.forEach(params::put);
            HttpUrl.Builder builder = httpUrl.newBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.addQueryParameter(entry.getKey(), entry.getValue());
            }
            httpUrl = builder.build();
        }

        LogHelper.i(TAG, "Get Url ===> " + httpUrl.toString());

        Request.Builder builder = new Request.Builder();
        builder.url(httpUrl)
                .get();
        builder = addOKHeader(builder);

        final Request request = builder.build();

        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogHelper.i(TAG, "url = " + url + "\n" + "result:\n" + e.getMessage());
                if (callback != null) {
                    callback.onFailure(-1, e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(url, call, response, callback);
            }
        });
    }

    public static <T> void formBodyPost(final String url, Map<String, Object> param, String fileKey, File file, final Callback<T> callback) {
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        // 加入全域变数
        Map<String, String> globalVariables = getParam();
        globalVariables.forEach(param::put);

        for (Map.Entry<String, Object> entry : param.entrySet()) {
            bodyBuilder.addFormDataPart(entry.getKey(), entry.getValue().toString());
        }
        if (file != null) {
            String fileName = file.getPath();
            MediaType MEDIA_TYPE = fileName.endsWith("png") ? MediaType.parse("image/png") : MediaType.parse("image/jpeg");
            bodyBuilder.addFormDataPart(fileKey, fileName, RequestBody.create(MEDIA_TYPE, file));
        }
        RequestBody body = bodyBuilder.build();
        Request.Builder builder = new Request.Builder();
        builder.url(url)
                .post(body);
        builder = addOKHeader(builder);

        final Request request = builder.build();

        String json = gson.toJson(param);
        LogHelper.i(TAG, "post Url ===> " + url + "\nparam ===> \n file = " + file + " , " + body.toString());
        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogHelper.i(TAG, "url = " + url + "\n" + "result:\n" + e.getMessage());
                if (callback != null) {
                    callback.onFailure(-1, e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(url, call, response, callback);
            }
        });
    }

    public static <T> void post(final String url, Map<String, Object> param, final Callback<T> callback) {
        // 加入全域变数
        Map<String, String> globalVariables = getParam();
        globalVariables.forEach(param::put);

        String json = gson.toJson(param);
        RequestBody body = RequestBody.create(JSON, json);

        Request.Builder builder = new Request.Builder();
        builder.url(url)
                .post(body);
        builder = addOKHeader(builder);

        final Request request = builder.build();

        LogHelper.i(TAG, "post Url ===> " + url + "\nparam ===> \n" + json);
        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogHelper.i(TAG, "url = " + url + "\n" + "result:\n" + e.getMessage());
                if (callback != null) {
                    callback.onFailure(-1, e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(url, call, response, callback);
            }
        });
    }

    public static Call executeGet(String url, Map<String, String> params) {
        // 加入全域变数
        Map<String, String> globalVariables = getParam();
        if (params == null) {
            params = globalVariables;
        }
        globalVariables.forEach(params::put);

        String json = gson.toJson(params);
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (params != null) {
            HttpUrl.Builder builder = httpUrl.newBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.addQueryParameter(entry.getKey(), entry.getValue());
            }
            httpUrl = builder.build();
        }
        LogHelper.i(TAG, "url = " + httpUrl);

        Request.Builder builder = new Request.Builder();
        builder.url(httpUrl)
                .get();
        builder = addOKHeader(builder);

        final Request request = builder.build();

        LogHelper.i(TAG, "get Url ===> " + httpUrl + "\nparam ===> \n" + json);
        return okHttpClient.newCall(request);
    }

    public static <T> void put(final String url, Map<String, String> param, final Callback<T> callback) {
        String json = gson.toJson(param);
        RequestBody body = RequestBody.create(JSON, json);
        Request.Builder builder = new Request.Builder();
        builder.url(url)
                .put(body);
        builder = addOKHeader(builder);
        final Request request = builder.build();

        LogHelper.i(TAG, "put Url ===> " + url + "\nparam ===> \n" + json);
        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogHelper.i(TAG, "url = " + url + "\n" + "result:\n" + e.getMessage());
                if (callback != null) {
                    callback.onFailure(-1, e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(url, call, response, callback);
            }
        });
    }

    public static <T> void upload(String url, Map<String, String> params, File file, MediaType mediaType, final Callback<T> callback) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(mediaType, file));
        StringBuffer sb = new StringBuffer();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.addFormDataPart(entry.getKey(), entry.getValue());
                sb.append(entry.getKey());
                sb.append(":");
                sb.append(entry.getValue());
                sb.append("\n");
            }
        }
        LogHelper.i(TAG, "upload Url ===> " + url + "\nparam ===> \n" + sb.toString());

        RequestBody requestBody = builder.build();

        Request.Builder requestBuild = new Request.Builder();
        requestBuild.url(url)
                .post(requestBody);
        requestBuild = addOKHeader(requestBuild);
        final Request request = requestBuild.build();

        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogHelper.i(TAG, "url = " + url + "\n" + "result:\n" + e.getMessage());
                if (callback != null) {
                    callback.onFailure(-1, e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(url, call, response, callback);
            }
        });
    }

    private static Map<String, String> getParam() {
        Map<String, String> params = new HashMap<>();
        params.put("platform", "2");
        try {
            params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return params;
    }

    private static Request.Builder addOKHeader(Request.Builder builder) {
        return builder.removeHeader("User-Agent")
                .addHeader("User-Agent", getUserAgent());
    }

    private static String getUserAgent() {
        if (TextUtils.isEmpty(userAgent)) {
            return "Mozilla/5.0 (Linux; Android 8.0; LON-AL00 Build/HUAWEILON-AL00; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.132 MQQBrowser/6.2 TBS/044204 Mobile Safari/537.36 V1_AND_SQ_7.7.8_908_YYB_D QQ/7.7.8.3705 NetType/WIFI WebP/0.3.0 Pixel/1440";
        } else {
            return userAgent;
        }
    }

    public static void clearCookies() {
        SharedPreferences sp = AppContext.get().getSharedPreferences(WFC_OKHTTP_COOKIE_CONFIG, 0);
        sp.edit().clear().apply();
        cookieStore.clear();
    }

    private static <T> void handleResponse(String url, Call call, okhttp3.Response response, Callback<T> callback) {
        if (callback != null) {
            // 打印 Header log
            if (BuildConfig.DEBUG) {
                printRequestHeader(response);
            }
            if (!response.isSuccessful()) {
                String msg = response.message();
                LogHelper.i(TAG, "url = " + url + "\n error code = " + response.code() + "\nresult:\n" + msg);
                callback.onFailure(response.code(), msg);
                return;
            }

            Type type;
            if (callback instanceof SimpleCallback) {
                Type types = callback.getClass().getGenericSuperclass();
                type = ((ParameterizedType) types).getActualTypeArguments()[0];
            } else {
                Type[] types = callback.getClass().getGenericInterfaces();
                type = ((ParameterizedType) types[0]).getActualTypeArguments()[0];
            }

            if (type.equals(Void.class)) {
                LogHelper.i(TAG, "url = " + url + "\n" + "result:\n [empty]");
                callback.onSuccess((T) null);
                return;
            }

            if (type.equals(String.class)) {
                try {
                    String body = response.body().string();
                    LogHelper.i(TAG, "url = " + url + "\n" + "result:\n" + body);
                    if (body.contains("\"result\"") && body.contains("\"message\"")) {
                        JSONObject jsonObject = new JSONObject(body);
                        int code = jsonObject.optInt("code");
                        String msg = jsonObject.optString("message");
                        String result = jsonObject.optString("result");
                        if (code == 0) {
                            callback.onSuccess((T) result);
                        } else {
                            callback.onFailure(code, msg);
                        }
                        return;
                    }
                    callback.onSuccess((T) body);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                return;
            }


            try {
                WebResponse<T> webResponse;
                StatusResult statusResult;
                GetStringResult getStringResult;
                if (type instanceof Class && type.equals(StatusResult.class)) {
                    String body = response.body().string();
                    LogHelper.i(TAG, "url = " + url + "\n" + "result:\n" + body);
                    statusResult = gson.fromJson(body, StatusResult.class);
                    if (statusResult.isSuccess()) {
                        callback.onSuccess((T) statusResult);
                    } else {
                        callback.onFailure(statusResult.getCode(), statusResult.getMessage());
                    }
                } else if (type.equals(GetStringResult.class)) {
                    String body = response.body().string();
                    LogHelper.i(TAG, "url = " + url + "\n" + "result:\n" + body);
                    getStringResult = gson.fromJson(body, GetStringResult.class);
                    if (getStringResult.isSuccess()) {
                        callback.onSuccess((T) getStringResult);
                    } else {
                        callback.onFailure(getStringResult.getCode(), getStringResult.getMessage());
                    }
                } else {
                    String body = response.body().string();
                    LogHelper.i(TAG, "url = " + url + "\n" + "result:\n" + body);
                    ResultWrapper<T> wrapper = gson.fromJson(body, new ResultType(type));
                    if (wrapper == null) {
                        callback.onFailure(-1, "response is null");
                        return;
                    }
                    if (wrapper.isSuccess() && wrapper.getResult() != null) {
                        callback.onSuccess(wrapper.getResult());
                    } else {
                        callback.onFailure(wrapper.getCode(), wrapper.getMessage());
                    }
                }
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
                callback.onFailure(-1, e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                callback.onFailure(-1, e.getMessage());
            }
        }
    }

    /**
     * // 打印 Header log
     *
     * @param response
     */
    private static void printRequestHeader(Response response) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("requestHeader:\n");
        Headers requestHeaders = response.networkResponse().request().headers();
        int requestHeadersLength = requestHeaders.size();
        for (int i = 0; i < requestHeadersLength; i++) {
            String headerName = requestHeaders.name(i);
            String headerValue = requestHeaders.get(headerName);
            stringBuffer.append(headerName).append(":").append(headerValue).append("\n");
        }

        LogHelper.e(TAG, stringBuffer.toString());
        stringBuffer.delete(0, stringBuffer.length());
    }

    public static class ResultType implements ParameterizedType {
        private final Type type;

        public ResultType(Type type) {
            this.type = type;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{type};
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public Type getRawType() {
            return ResultWrapper.class;
        }
    }
}
