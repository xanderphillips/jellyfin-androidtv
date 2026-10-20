package org.jellyfin.androidtv.ui.playback.overlay;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.leanback.media.PlayerAdapter;

import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.preference.UserSettingPreferences;
import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment;
import org.jellyfin.androidtv.ui.playback.SkipAccumulator;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.StreamHelper;
import org.jellyfin.sdk.model.api.ChapterInfo;
import org.jellyfin.sdk.model.api.MediaSourceInfo;
import org.koin.java.KoinJavaComponent;

import java.util.List;

import kotlin.Lazy;

public class VideoPlayerAdapter extends PlayerAdapter {

    private final PlaybackController playbackController;
    private CustomPlaybackOverlayFragment customPlaybackOverlayFragment;
    private LeanbackOverlayFragment leanbackOverlayFragment;
    private final Lazy<UserSettingPreferences> userSettingPreferences = KoinJavaComponent.inject(UserSettingPreferences.class);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SkipAccumulator skipAccumulator = new SkipAccumulator(400L);
    private static final int MAX_SKIP_RETRIES = 30;
    private int skipRetries = 0;

    VideoPlayerAdapter(PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment) {
        this.playbackController = playbackController;
        this.leanbackOverlayFragment = leanbackOverlayFragment;
    }

    @Override
    public void play() {
        playbackController.play(playbackController.getCurrentPosition());
    }

    @Override
    public void pause() {
        playbackController.pause();
    }

    @Override
    public void rewind() {
        if (customPlaybackOverlayFragment != null && customPlaybackOverlayFragment.consumePointerControlClick()) {
            queuePointerSkip(-userSettingPreferences.getValue().get(UserSettingPreferences.Companion.getSkipBackLength()));
            return;
        }
        playbackController.rewind();
        updateCurrentPosition();
    }

    @Override
    public void fastForward() {
        if (customPlaybackOverlayFragment != null && customPlaybackOverlayFragment.consumePointerControlClick()) {
            queuePointerSkip(userSettingPreferences.getValue().get(UserSettingPreferences.Companion.getSkipForwardLength()));
            return;
        }
        playbackController.fastForward();
        updateCurrentPosition();
    }

    /**
     * Pointer clicks on rewind/fast-forward are collected for a moment and sent as one seek, retried
     * briefly if the player is still busy with the previous one. Remote and gamepad skips are unaffected.
     */
    private void queuePointerSkip(int deltaMs) {
        skipAccumulator.add(deltaMs, SystemClock.uptimeMillis());
        skipRetries = 0;
        handler.removeCallbacks(pointerSkipTick);
        handler.postDelayed(pointerSkipTick, 100);
    }

    private final Runnable pointerSkipTick = new Runnable() {
        @Override
        public void run() {
            Integer delta = skipAccumulator.takeIfQuiet(SystemClock.uptimeMillis());
            if (delta != null) {
                if (playbackController.skipNow(delta)) {
                    updateCurrentPosition();
                } else if (skipRetries++ < MAX_SKIP_RETRIES) {
                    skipAccumulator.restore(delta);
                }
            }
            if (skipAccumulator.getHasPending()) handler.postDelayed(this, 100);
        }
    };

    @Override
    public void seekTo(long positionInMs) {
        playbackController.seekQueued(positionInMs);
        updateCurrentPosition();
    }

    @Override
    public void next() {
        playbackController.next();
    }

    @Override
    public void previous() {
        playbackController.prev();
    }

    @Override
    public long getDuration() {
        Long runTimeTicks = null;
        if (getCurrentMediaSource() != null) runTimeTicks = getCurrentMediaSource().getRunTimeTicks();
        if (runTimeTicks == null && getCurrentlyPlayingItem() != null) runTimeTicks = getCurrentlyPlayingItem().getRunTimeTicks();
        if (runTimeTicks != null) return runTimeTicks / 10000;
        return -1;
    }

    @Override
    public long getCurrentPosition() {
        return playbackController.getCurrentPosition();
    }

    @Override
    public boolean isPlaying() {
        return playbackController.isPlaying();
    }

    @Override
    public long getBufferedPosition() {
        return playbackController.getBufferedPosition();
    }

    void updateCurrentPosition() {
        getCallback().onCurrentPositionChanged(this);
        getCallback().onBufferedPositionChanged(this);
    }

    void updatePlayState() {
        getCallback().onPlayStateChanged(this);
    }

    void updateDuration() {
        getCallback().onDurationChanged(this);
    }

    public boolean hasSubs() {
        return StreamHelper.getSubtitleStreams(playbackController.getCurrentMediaSource()).size() > 0;
    }

    public boolean hasMultiAudio() {
        return StreamHelper.getAudioStreams(playbackController.getCurrentMediaSource()).size() > 1;
    }

    boolean hasNextItem() {
        return playbackController.hasNextItem();
    }

    boolean hasPreviousItem() {
        return playbackController.hasPreviousItem();
    }

    boolean canSeek() {
        return playbackController.canSeek();
    }

    boolean isLiveTv() {
        return playbackController.isLiveTv();
    }

    void setMasterOverlayFragment(CustomPlaybackOverlayFragment customPlaybackOverlayFragment) {
        this.customPlaybackOverlayFragment = customPlaybackOverlayFragment;
    }

    @NonNull
    public CustomPlaybackOverlayFragment getMasterOverlayFragment() {
        return customPlaybackOverlayFragment;
    }

    @NonNull
    public LeanbackOverlayFragment getLeanbackOverlayFragment() {
        return leanbackOverlayFragment;
    }

    @Override
    public void onDetachedFromHost() {
        customPlaybackOverlayFragment = null;
        leanbackOverlayFragment = null;
    }

    boolean canRecordLiveTv() {
        org.jellyfin.sdk.model.api.BaseItemDto currentlyPlayingItem = getCurrentlyPlayingItem();
        return currentlyPlayingItem.getCurrentProgram() != null
                && Utils.canManageRecordings(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue());
    }

    public void toggleRecording() {
        org.jellyfin.sdk.model.api.BaseItemDto currentlyPlayingItem = getCurrentlyPlayingItem();
        getMasterOverlayFragment().toggleRecording(currentlyPlayingItem);
    }

    boolean isRecording() {
        org.jellyfin.sdk.model.api.BaseItemDto currentProgram = getCurrentlyPlayingItem().getCurrentProgram();
        if (currentProgram == null) {
            return false;
        } else {
            return currentProgram.getTimerId() != null;
        }
    }

    org.jellyfin.sdk.model.api.BaseItemDto getCurrentlyPlayingItem() {
        return playbackController.getCurrentlyPlayingItem();
    }

    MediaSourceInfo getCurrentMediaSource() {
        return playbackController.getCurrentMediaSource();
    }

    boolean hasChapters() {
        org.jellyfin.sdk.model.api.BaseItemDto item = getCurrentlyPlayingItem();
        List<ChapterInfo> chapters = item.getChapters();
        return chapters != null && chapters.size() > 0;
    }
}
