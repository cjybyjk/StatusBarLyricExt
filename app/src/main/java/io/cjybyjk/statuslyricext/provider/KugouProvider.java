package io.cjybyjk.statuslyricext.provider;

import android.media.MediaMetadata;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import android.util.Base64;
import android.util.Pair;

import io.cjybyjk.statuslyricext.provider.utils.HttpRequestUtil;
import io.cjybyjk.statuslyricext.provider.utils.LyricSearchUtil;

public class KugouProvider implements ILrcProvider {

    private static final String KUGOU_BASE_URL = "http://lyrics.kugou.com/";
    private static final String KUGOU_SEARCH_URL_FORMAT = KUGOU_BASE_URL + "search?ver=1&man=yes&client=pc&keyword=%s&duration=%d";
    private static final String KUGOU_LRC_URL_FORMAT = KUGOU_BASE_URL + "download?ver=1&client=pc&id=%d&accesskey=%s&fmt=lrc&charset=utf8";

    @Override
    public LyricResult getLyric(MediaMetadata data) throws IOException {
        String searchUrl = String.format(Locale.getDefault(), KUGOU_SEARCH_URL_FORMAT, LyricSearchUtil.getSearchKey(data), data.getLong(MediaMetadata.METADATA_KEY_DURATION));
        JSONObject searchResult;
        try {
            searchResult = HttpRequestUtil.getJsonResponse(searchUrl);
            if (searchResult != null && searchResult.getLong("status") == 200) {
                JSONArray array = searchResult.getJSONArray("candidates");
                Pair<String, Long> pair = getLrcUrl(array, data);
                JSONObject lrcJson = HttpRequestUtil.getJsonResponse(pair.first);
                LyricResult result = new LyricResult();
                result.mLyric = new String(Base64.decode(lrcJson.getString("content").getBytes(), Base64.DEFAULT));
                result.mDistance = pair.second;
                return result;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private static Pair<String, Long> getLrcUrl(JSONArray jsonArray, MediaMetadata mediaMetadata) throws JSONException {
        String currentAccessKey = "";
        long minDistance = 10000;
        long currentId = -1;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String soundName = jsonObject.getString("soundname");
            String artist = jsonObject.getString("singer");
            long dis = LyricSearchUtil.getMetadataDistance(mediaMetadata, soundName, artist, null);
            if (dis < minDistance) {
                minDistance = dis;
                currentId = jsonObject.getLong("id");
                currentAccessKey = jsonObject.getString("accesskey");
            }
        }
        return new Pair<>(String.format(Locale.getDefault(), KUGOU_LRC_URL_FORMAT, currentId, currentAccessKey), minDistance);
    }
}
