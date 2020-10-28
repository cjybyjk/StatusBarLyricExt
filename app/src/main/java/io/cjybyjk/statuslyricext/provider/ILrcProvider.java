package io.cjybyjk.statuslyricext.provider;

import android.media.MediaMetadata;

import java.io.IOException;

public interface ILrcProvider {
    String getLyric(MediaMetadata data) throws IOException;
}
