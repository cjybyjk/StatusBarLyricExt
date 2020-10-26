package io.cjybyjk.statuslyricext.provider;

import android.media.MediaMetadata;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;

import io.cjybyjk.statuslyricext.provider.utils.HttpRequestUtil;

public class KugouProvider implements ILrcProvider {

    private static final String KUGOU_BASE_URL = "http://lyrics.kugou.com/";
    private static final String KUGOU_SEARCH_URL_FORMAT = KUGOU_BASE_URL + "search?ver=1&man=yes&client=pc&keyword=%s&duration=%d";
    private static final String KUGOU_LRC_URL_FORMAT = KUGOU_BASE_URL + "download?ver=1&client=pc&id=%d&accesskey=%s&fmt=lrc&charset=utf8";

    @Override
    public String getLyric(MediaMetadata data) throws IOException {
        String searchUrl = String.format(KUGOU_SEARCH_URL_FORMAT, data.getString(MediaMetadata.METADATA_KEY_TITLE), data.getLong(MediaMetadata.METADATA_KEY_DURATION));
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getLong("status") == 200) {
                JSONArray array = searchResult.getJSONArray("candidates");
                String lrcUrl = getLrcUrl(array);
                JSONObject lrcJson = HttpRequestUtil.getJsonResponse(lrcUrl);
                return new String(Base64.getDecoder().decode(lrcJson.getString("content")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private static String getLrcUrl(JSONArray jsonArray) throws JSONException {
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        return String.format(KUGOU_LRC_URL_FORMAT, jsonObject.getLong("id"), jsonObject.getString("accesskey"));
    }
}
