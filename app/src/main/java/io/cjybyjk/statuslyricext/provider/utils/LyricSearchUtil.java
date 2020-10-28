package io.cjybyjk.statuslyricext.provider.utils;

import android.media.MediaMetadata;
import android.text.TextUtils;

public class LyricSearchUtil {
    public static String getSearchKey(MediaMetadata metadata) {
        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        if (!TextUtils.isEmpty(artist)) {
            return artist + "-" + title;
        } else if (!TextUtils.isEmpty(album)) {
            return album + "-" + title;
        } else {
            return title;
        }
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

}
