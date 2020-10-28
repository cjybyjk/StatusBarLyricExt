package io.cjybyjk.statuslyricext.provider;

import android.media.MediaMetadata;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import io.cjybyjk.statuslyricext.provider.utils.HttpRequestUtil;
import io.cjybyjk.statuslyricext.provider.utils.LyricSearchUtil;

public class QQMusicProvider implements ILrcProvider {

    private static final String QM_BASE_URL = "https://c.y.qq.com/";
    private static final String QM_REFERER = "https://y.qq.com";
    private static final String QM_SEARCH_URL_FORMAT = QM_BASE_URL + "soso/fcgi-bin/client_search_cp?w=%s&format=json";
    private static final String QM_LRC_URL_FORMAT = QM_BASE_URL + "lyric/fcgi-bin/fcg_query_lyric_yqq.fcg?songmid=%s&format=json";

    @Override
    public String getLyric(MediaMetadata data) throws IOException {
        String searchUrl = String.format(Locale.getDefault(), QM_SEARCH_URL_FORMAT, LyricSearchUtil.getSearchKey(data));
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl, QM_REFERER);
            if (searchResult != null && searchResult.getLong("code") == 0) {
                JSONArray array = searchResult.getJSONObject("data").getJSONObject("song").getJSONArray("list");
                String lrcUrl = getLrcUrl(array, data.getString(MediaMetadata.METADATA_KEY_TITLE));
                JSONObject lrcJson = HttpRequestUtil.getJsonResponse(lrcUrl, QM_REFERER);
                return new String(Base64.decode(lrcJson.getString("lyric").getBytes(), Base64.DEFAULT));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private static String getLrcUrl(JSONArray jsonArray, String title) throws JSONException {
        String currentMID = "";
        long minDistance = title.length();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String soundName = jsonObject.getString("songname");
            long dis = LyricSearchUtil.levenshtein(soundName, title);
            if (dis < minDistance) {
                minDistance = dis;
                currentMID = jsonObject.getString("songmid");
            }
        }
        return String.format(Locale.getDefault(), QM_LRC_URL_FORMAT, currentMID);
    }
}
