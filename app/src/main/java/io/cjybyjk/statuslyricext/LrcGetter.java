package io.cjybyjk.statuslyricext;

import android.content.Context;
import android.media.MediaMetadata;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import io.cjybyjk.statuslyricext.provider.ILrcProvider;
import io.cjybyjk.statuslyricext.provider.KugouProvider;
import io.cjybyjk.statuslyricext.provider.NeteaseProvider;

public class LrcGetter {

    private static final ILrcProvider[] providers = {
            new NeteaseProvider(),
            new KugouProvider()
    };

    private static MessageDigest messageDigest;
    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    public static Lyric getLyric(Context context, MediaMetadata mediaMetadata) {
        if (messageDigest == null) {
            try {
                messageDigest =  MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        }
        File cachePath = context.getCacheDir();
        String meta = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE) + "," + mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST) + "," +
                mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM) + ", " + mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        File requireLrcPath = new File(cachePath, printHexBinary(messageDigest.digest(meta.getBytes())) + ".lrc");
        if (requireLrcPath.exists()) {
            return LyricUtils.parseLyric(requireLrcPath, "UTF-8");
        }
        for (ILrcProvider provider : providers) {
            try {
                String lrc = provider.getLyric(mediaMetadata);
                if (!TextUtils.isEmpty(lrc)) {
                    FileOutputStream lrcOut = new FileOutputStream(requireLrcPath);
                    lrcOut.write(lrc.getBytes());
                    lrcOut.close();
                    return LyricUtils.parseLyric(requireLrcPath, "UTF-8");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    };

    public static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[b & 0xF]);
        }
        return r.toString();
    }
}
