package io.cjybyjk.statuslyricext.provider;

import android.media.MediaMetadata;

import java.io.IOException;
import java.net.ProtocolException;

import cn.zhaiyifan.lyric.model.Lyric;

public interface ILrcProvider {
    public String getLyric(MediaMetadata data) throws IOException;
}
