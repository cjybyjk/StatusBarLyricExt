package io.cjybyjk.statuslyricext;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import io.cjybyjk.statuslyricext.misc.Constants;

public class MusicListenerService extends NotificationListenerService {

    private static final int NOTIFICATION_ID_LRC = 1;

    private static final int MSG_LYRIC_UPDATE_DONE = 2;

    private MediaSessionManager mMediaSessionManager;
    private MediaController mMediaController;
    private NotificationManager mNotificationManager;

    private final ArrayList<String> mIgnoredPackageList = new ArrayList<>();
    private SharedPreferences mSharedPreferences;

    private Lyric mLyric;
    private String requiredLrcTitle;
    private Notification mLyricNotification;
    private long mLastSentenceFromTime = -1;

    private BroadcastReceiver mIgnoredPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.BROADCAST_IGNORED_APP_CHANGED)) {
                updateIgnoredPackageList();
                unBindMediaListeners();
                bindMediaListeners();
            }
        }
    };

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_LYRIC_UPDATE_DONE && msg.getData().getString("title", "").equals(requiredLrcTitle)) {
                mLyric = (Lyric) msg.obj;
                startLyric();
            }
        }
    };

    private Runnable mLyricUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaController == null || mMediaController.getPlaybackState().getState() != PlaybackState.STATE_PLAYING) {
                stopLyric();
                return;
            }
            updateLyric(mMediaController.getPlaybackState().getPosition());
            mHandler.postDelayed(mLyricUpdateRunnable, 250);
        }
    };

    private final MediaController.Callback mMediaCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            super.onPlaybackStateChanged(state);
            if (state != null) {
                if (state.getState() == PlaybackState.STATE_PLAYING) {
                    startLyric();
                } else {
                    stopLyric();
                }
            }
        }

        @Override
        public void onSessionDestroyed() {
            stopLyric();
            super.onSessionDestroyed();
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            stopLyric();
            mLyric = null;
            if (metadata == null) return;
            requiredLrcTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            new LrcUpdateThread(getApplicationContext(), mHandler, metadata).start();
        }
    };

    private final MediaSessionManager.OnActiveSessionsChangedListener onActiveSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
            if (mMediaController != null) mMediaController.unregisterCallback(mMediaCallback);
            if (controllers == null) return;
            for (MediaController controller : controllers) {
                if (mIgnoredPackageList.contains(controller.getPackageName())) continue;
                if (getMediaControllerPlaybackState(controller) == PlaybackState.STATE_PLAYING) {
                    mMediaController = controller;
                    break;
                }
            }
            if (mMediaController != null) {
                mMediaController.registerCallback(mMediaCallback);
                mMediaCallback.onMetadataChanged(mMediaController.getMetadata());
                mMediaCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
            }
        }
    };

    private int getMediaControllerPlaybackState(MediaController controller) {
        if (controller != null) {
            final PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                return playbackState.getState();
            }
        }
        return PlaybackState.STATE_NONE;
    }

    public MusicListenerService() {
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mLyricNotification = buildLrcNotification();
        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mIgnoredPackageReceiver, new IntentFilter(Constants.BROADCAST_IGNORED_APP_CHANGED));
        updateIgnoredPackageList();
        bindMediaListeners();
    }

    @Override
    public void onListenerDisconnected() {
        stopLyric();
        unBindMediaListeners();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mIgnoredPackageReceiver);
        super.onListenerDisconnected();
    }

    private Notification buildLrcNotification() {
        Notification.Builder builder = new Notification.Builder(this, Constants.NOTIFICATION_CHANNEL_LRC);
        builder.setSmallIcon(R.drawable.ic_music);
        builder.setOngoing(true);
        Notification notification = builder.build();
        notification.flags |= Constants.FLAG_ALWAYS_SHOW_TICKER;
        notification.flags |= Constants.FLAG_ONLY_UPDATE_TICKER;
        return notification;
    }

    private void bindMediaListeners() {
        ComponentName listener = new ComponentName(this, MusicListenerService.class);
        mMediaSessionManager.addOnActiveSessionsChangedListener(onActiveSessionsChangedListener, listener);
        onActiveSessionsChangedListener.onActiveSessionsChanged(mMediaSessionManager.getActiveSessions(listener));
    }

    private void unBindMediaListeners() {
        if (mMediaSessionManager != null) mMediaSessionManager.removeOnActiveSessionsChangedListener(onActiveSessionsChangedListener);
        if (mMediaController != null) mMediaController.unregisterCallback(mMediaCallback);
        mMediaController = null;
    }

    private void updateIgnoredPackageList() {
        mIgnoredPackageList.clear();
        String value = mSharedPreferences.getString(Constants.PREFERENCE_KEY_IGNORED_PACKAGES, "");
        String[] arr = value.split(";");
        for (String str : arr) {
            if (TextUtils.isEmpty(str)) continue;
            mIgnoredPackageList.add(str.trim());
        }
    }

    private void startLyric() {
        mLastSentenceFromTime = -1;
        mLyricNotification.tickerText = null;
        mHandler.post(mLyricUpdateRunnable);
    }

    private void stopLyric() {
        if (mLyric == null) return;
        mHandler.removeCallbacks(mLyricUpdateRunnable);
        mNotificationManager.cancel(NOTIFICATION_ID_LRC);
    }

    private void updateLyric(long position) {
        if (mNotificationManager == null || mLyric == null) return;
        Lyric.Sentence sentence = LyricUtils.getSentence(mLyric, position);
        if (sentence == null) return;
        if (sentence.fromTime != mLastSentenceFromTime) {
            mLyricNotification.tickerText = sentence.content;
            mLyricNotification.when = System.currentTimeMillis();
            mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
            mLastSentenceFromTime = sentence.fromTime;
        }
    }

    private static class LrcUpdateThread extends Thread {
        private Handler handler;
        private MediaMetadata data;
        private Context context;

        public LrcUpdateThread(Context context, Handler handler, MediaMetadata data) {
            super();
            this.data = data;
            this.handler = handler;
            this.context = context;
        }

        @Override
        public void run() {
            if (handler == null) return;
            Lyric lrc = LrcGetter.getLyric(context, data);
            Message message = new Message();
            message.what = MSG_LYRIC_UPDATE_DONE;
            message.obj = lrc;
            Bundle bundle = new Bundle();
            bundle.putString("title", data.getString(MediaMetadata.METADATA_KEY_TITLE));
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }
}