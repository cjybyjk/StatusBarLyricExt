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
}
