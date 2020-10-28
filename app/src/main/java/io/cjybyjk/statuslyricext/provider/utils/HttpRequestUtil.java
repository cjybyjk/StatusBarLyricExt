package io.cjybyjk.statuslyricext.provider.utils;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRequestUtil {

    public static JSONObject getJsonResponse(String url) throws IOException, JSONException {
        return getJsonResponse(url, null);
    }

    public static JSONObject getJsonResponse(String url, String referer) throws IOException, JSONException {
        URL httpUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) httpUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept","text/html, application/xhtml+xml, */*");
        connection.setRequestProperty("Accept-Language","en-US,en;q=0.8,zh-Hans-CN;q=0.5,zh-Hans;q=0.3");
        connection.setRequestProperty("Accept-Encoding","deflate");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:77.0) Gecko/20100101 Firefox/77.0");
        if (!TextUtils.isEmpty(referer)) {
            connection.setRequestProperty("Referer", referer);
        }
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(1000);
        connection.connect();
        if (connection.getResponseCode() == 200) {
            // 处理搜索结果
            InputStream in = connection.getInputStream();
            byte[] data = readStream(in);
            JSONObject jsonObject = new JSONObject(new String(data));
            in.close();
            connection.disconnect();
            return jsonObject;
        }
        connection.disconnect();
        return null;
    }


    public static byte[] readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            bout.write(buffer, 0, len);
        }
        bout.close();
        inputStream.close();

        return bout.toByteArray();
    }
}
