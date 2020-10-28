package io.cjybyjk.statuslyricext.provider;

import android.media.MediaMetadata;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import io.cjybyjk.statuslyricext.provider.utils.HttpRequestUtil;
import io.cjybyjk.statuslyricext.provider.utils.LyricSearchUtil;

public class NeteaseProvider implements ILrcProvider {

    private static final String NETEASE_BASE_URL = "http://music.163.com/api/";

    private static final String NETEASE_SEARCH_URL_FORMAT = NETEASE_BASE_URL + "search/pc?s=%s&type=1&offset=0&limit=10";
    private static final String NETEASE_LRC_URL_FORMAT = NETEASE_BASE_URL + "song/lyric?os=pc&id=%d&lv=-1&kv=-1&tv=-1";

    @Override
    public String getLyric(MediaMetadata data) throws IOException {
        String searchUrl = String.format(NETEASE_SEARCH_URL_FORMAT, LyricSearchUtil.getSearchKey(data));
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getLong("code") == 200) {
                JSONArray array = searchResult.getJSONObject("result").getJSONArray("songs");
                String lrcUrl = getLrcUrl(array, data.getString(MediaMetadata.METADATA_KEY_TITLE));
                JSONObject lrcJson = HttpRequestUtil.getJsonResponse(lrcUrl);
                return lrcJson.getJSONObject("lrc").getString("lyric");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private static String getLrcUrl(JSONArray jsonArray, String title) throws JSONException {
        long currentID = -1;
        long minDistance = title.length();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String soundName = jsonObject.getString("name");
            long dis = LyricSearchUtil.levenshtein(soundName, title);
            if (dis < minDistance) {
                minDistance = dis;
                currentID = jsonObject.getLong("id");
            }
        }
        return String.format(Locale.getDefault(), NETEASE_LRC_URL_FORMAT, currentID);
    }
}
