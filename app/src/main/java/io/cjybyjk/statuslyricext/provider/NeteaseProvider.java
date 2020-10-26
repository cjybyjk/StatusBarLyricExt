package io.cjybyjk.statuslyricext.provider;

import android.media.MediaMetadata;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.cjybyjk.statuslyricext.provider.utils.HttpRequestUtil;

public class NeteaseProvider implements ILrcProvider {

    private static final String NETEASE_BASE_URL = "http://music.163.com/api/";

    private static final String NETEASE_SEARCH_URL_FORMAT = NETEASE_BASE_URL + "search/pc?s=%s %s&type=1&offset=0&limit=1";
    private static final String NETEASE_LRC_URL_FORMAT = NETEASE_BASE_URL + "song/lyric?os=pc&id=%d&lv=-1&kv=-1&tv=-1";

    @Override
    public String getLyric(MediaMetadata data) throws IOException {
        String searchUrl = String.format(NETEASE_SEARCH_URL_FORMAT, data.getString(MediaMetadata.METADATA_KEY_TITLE), data.getString(MediaMetadata.METADATA_KEY_ARTIST));
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getLong("code") == 200) {
                JSONArray array = searchResult.getJSONObject("result").getJSONArray("songs");
                String lrcUrl = getLrcUrl(array, data);
                JSONObject lrcJson = HttpRequestUtil.getJsonResponse(lrcUrl);
                return lrcJson.getJSONObject("lrc").getString("lyric");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private static String getLrcUrl(JSONArray jsonArray, MediaMetadata data) throws JSONException {
        String url = null;
        String title = data.getString(MediaMetadata.METADATA_KEY_TITLE);
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        return String.format(NETEASE_LRC_URL_FORMAT, jsonObject.getLong("id"));
    }
}
