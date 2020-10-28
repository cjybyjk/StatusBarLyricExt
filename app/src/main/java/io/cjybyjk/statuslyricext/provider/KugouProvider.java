package io.cjybyjk.statuslyricext.provider;

import android.media.MediaMetadata;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import android.util.Base64;
import android.util.Log;

import io.cjybyjk.statuslyricext.provider.utils.HttpRequestUtil;
import io.cjybyjk.statuslyricext.provider.utils.LyricSearchUtil;

public class KugouProvider implements ILrcProvider {

    private static final String KUGOU_BASE_URL = "http://lyrics.kugou.com/";
    private static final String KUGOU_SEARCH_URL_FORMAT = KUGOU_BASE_URL + "search?ver=1&man=yes&client=pc&keyword=%s&duration=%d";
    private static final String KUGOU_LRC_URL_FORMAT = KUGOU_BASE_URL + "download?ver=1&client=pc&id=%d&accesskey=%s&fmt=lrc&charset=utf8";

    @Override
    public String getLyric(MediaMetadata data) throws IOException {
        String searchUrl = String.format(Locale.getDefault(), KUGOU_SEARCH_URL_FORMAT, LyricSearchUtil.getSearchKey(data), data.getLong(MediaMetadata.METADATA_KEY_DURATION));
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getLong("status") == 200) {
                JSONArray array = searchResult.getJSONArray("candidates");
                String lrcUrl = getLrcUrl(array, data.getString(MediaMetadata.METADATA_KEY_TITLE));
                JSONObject lrcJson = HttpRequestUtil.getJsonResponse(lrcUrl);
                return new String(Base64.decode(lrcJson.getString("content").getBytes(), Base64.DEFAULT));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private static String getLrcUrl(JSONArray jsonArray, String title) throws JSONException {
        String currentAccessKey = "";
        long minDistance = title.length(), currentId = -1;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String soundName = jsonObject.getString("soundname");
            long dis = LyricSearchUtil.levenshtein(soundName, title);
            if (dis < minDistance) {
                minDistance = dis;
                currentId = jsonObject.getLong("id");
                currentAccessKey = jsonObject.getString("accesskey");
            }
        }
        return String.format(Locale.getDefault(), KUGOU_LRC_URL_FORMAT, currentId, currentAccessKey);
    }
}
