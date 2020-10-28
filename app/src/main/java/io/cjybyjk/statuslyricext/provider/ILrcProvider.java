package io.cjybyjk.statuslyricext.provider;

import android.media.MediaMetadata;

import java.io.IOException;

public interface ILrcProvider {
    public String getLyric(MediaMetadata data) throws IOException;
}
