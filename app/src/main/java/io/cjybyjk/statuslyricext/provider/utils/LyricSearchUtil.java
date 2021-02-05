package io.cjybyjk.statuslyricext.provider.utils;

import android.media.MediaMetadata;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public class LyricSearchUtil {

    private static final Pattern LyricContentPattern = Pattern.compile("(\\[\\d\\d:\\d\\d\\.\\d{0,3}]|\\[\\d\\d:\\d\\d])[^\\r\\n]");

    public static String getSearchKey(MediaMetadata metadata) {
        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String ret;
        if (!TextUtils.isEmpty(artist)) {
            ret = artist + "-" + title;
        } else if (!TextUtils.isEmpty(album)) {
            ret = album + "-" + title;
        } else {
            ret = title;
        }
        try {
            return URLEncoder.encode(ret, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return ret;
        }
    }

    public static String parseArtists(JSONArray jsonArray, String key) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < jsonArray.length(); i++) {
                stringBuilder.append(jsonArray.getJSONObject(i).getString(key));
                if (i < jsonArray.length() - 1) stringBuilder.append('/');
            }
            return stringBuilder.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static long getMetadataDistance(MediaMetadata metadata, String title, String artist, String album) {
        String realTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String realArtist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String realAlbum = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        if (!realTitle.contains(title) && !title.contains(realTitle) || TextUtils.isEmpty(title)) {
            return 10000;
        }
        long res = levenshtein(title, realTitle) * 100;
        res += levenshtein(artist, realArtist) * 10;
        res += levenshtein(album, realAlbum);
        return res;
    }

    public static int levenshtein(CharSequence a, CharSequence b) {
        if (TextUtils.isEmpty(a)) {
            return TextUtils.isEmpty(b) ? 0 : b.length();
        } else if (TextUtils.isEmpty(b)) {
            return TextUtils.isEmpty(a) ? 0 : a.length();
        }
        final int lenA = a.length(), lenB = b.length();
        int[][] dp = new int[lenA+1][lenB+1];
        int flag = 0;
        for (int i = 0; i <= lenA; i++) {
            for (int j = 0; j <= lenB; j++) dp[i][j] = lenA + lenB;
        }
        for(int i=1; i <= lenA; i++) dp[i][0] = i;
        for(int j=1; j <= lenB; j++) dp[0][j] = j;
        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                if (a.charAt(i-1) == b.charAt(j-1)) {
                    flag = 0;
                } else {
                    flag = 1;
                }
                dp[i][j] = Math.min(dp[i-1][j-1] + flag, Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1));
            }
        }
        return dp[lenA][lenB];
    }

    public static boolean isLyricContent(String content) {
        if (TextUtils.isEmpty(content)) return false;
        return LyricContentPattern.matcher(content).find();
    }
}
