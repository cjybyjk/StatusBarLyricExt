package io.cjybyjk.statuslyricext.provider;

import android.media.MediaMetadata;

import java.io.IOException;

public interface ILrcProvider {
    LyricResult getLyric(MediaMetadata data) throws IOException;

    class LyricResult {
        public String mLyric;
        public long mDistance;
    }
}
