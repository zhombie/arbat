package kz.zhombie.cinema.exoplayer

import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.video.VideoSize
import kz.zhombie.cinema.logging.Logger

internal abstract class AbstractPlayerListener : Player.Listener {

    companion object {
        private val TAG = AbstractPlayerListener::class.java.simpleName
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        super.onVideoSizeChanged(videoSize)
        Logger.debug(TAG, "onVideoSizeChanged() -> videoSize: $videoSize")
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        super.onSurfaceSizeChanged(width, height)
        Logger.debug(TAG, "onSurfaceSizeChanged() -> width: $width, height: $height")
    }

    override fun onRenderedFirstFrame() {
        super.onRenderedFirstFrame()
        Logger.debug(TAG, "onRenderedFirstFrame()")
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        super.onAudioSessionIdChanged(audioSessionId)
        Logger.debug(TAG, "onAudioSessionIdChanged() -> audioSessionId: $audioSessionId")
    }

    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
        super.onAudioAttributesChanged(audioAttributes)
        Logger.debug(TAG, "onAudioAttributesChanged() -> audioAttributes: $audioAttributes")
    }

    override fun onVolumeChanged(volume: Float) {
        super.onVolumeChanged(volume)
        Logger.debug(TAG, "onVolumeChanged() -> volume: $volume")
    }

    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
        super.onSkipSilenceEnabledChanged(skipSilenceEnabled)
        Logger.debug(TAG, "onSkipSilenceEnabledChanged() -> " +
            "skipSilenceEnabled: $skipSilenceEnabled")
    }

    override fun onCues(cues: MutableList<Cue>) {
        super.onCues(cues)
        Logger.debug(TAG, "onCues() -> cues: $cues")
    }

    override fun onMetadata(metadata: Metadata) {
        super.onMetadata(metadata)
        Logger.debug(TAG, "onMetadata() -> metadata: $metadata")
    }

    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
        super.onDeviceInfoChanged(deviceInfo)
        Logger.debug(TAG, "onDeviceInfoChanged() -> deviceInfo: $deviceInfo")
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        super.onDeviceVolumeChanged(volume, muted)
        Logger.debug(TAG, "onDeviceVolumeChanged() -> volume: $volume, muted: $muted")
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        super.onTimelineChanged(timeline, reason)
        Logger.debug(TAG, "onTimelineChanged() -> timeline: $timeline, reason: $reason")
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        Logger.debug(TAG, "onMediaItemTransition() -> mediaItem: $mediaItem, reason: $reason")
    }

    override fun onTracksInfoChanged(tracksInfo: TracksInfo) {
        super.onTracksInfoChanged(tracksInfo)
        Logger.debug(TAG, "onTracksInfoChanged() -> tracksInfo: $tracksInfo")
    }

    override fun onTracksChanged(
        trackGroups: TrackGroupArray,
        trackSelections: TrackSelectionArray
    ) {
        super.onTracksChanged(trackGroups, trackSelections)
        Logger.debug(TAG, "onTracksChanged() -> " +
            "trackGroups: $trackGroups, " +
            "trackSelections: $trackSelections")
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        Logger.debug(TAG, "onMediaMetadataChanged() -> mediaMetadata: $mediaMetadata")
    }

    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onPlaylistMetadataChanged(mediaMetadata)
        Logger.debug(TAG, "onPlaylistMetadataChanged() -> mediaMetadata: $mediaMetadata")
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        super.onIsLoadingChanged(isLoading)
        Logger.debug(TAG, "onIsLoadingChanged() -> isLoading: $isLoading")
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        super.onLoadingChanged(isLoading)
        Logger.debug(TAG, "onLoadingChanged() -> isLoading: $isLoading")
    }

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        super.onAvailableCommandsChanged(availableCommands)
        Logger.debug(TAG, "onAvailableCommandsChanged() -> " +
            "availableCommands: $availableCommands")
    }

    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
        super.onTrackSelectionParametersChanged(parameters)
        Logger.debug(TAG, "onTrackSelectionParametersChanged() -> $parameters")
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        Logger.debug(TAG, "onPlayerStateChanged() -> " +
            "playWhenReady: $playWhenReady, " +
            "playbackState: $playbackState")
    }

    override fun onPlaybackStateChanged(state: Int) {
        super.onPlaybackStateChanged(state)
        Logger.debug(TAG, "onPlaybackStateChanged() -> state: $state")
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
        Logger.debug(TAG, "onPlayWhenReadyChanged() -> " +
            "playWhenReady: $playWhenReady, reason: $reason")
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        super.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
        Logger.debug(TAG, "onPlaybackSuppressionReasonChanged() -> " +
            "playbackSuppressionReason: $playbackSuppressionReason")
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        Logger.debug(TAG, "onIsPlayingChanged() -> isPlaying: $isPlaying")
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        Logger.debug(TAG, "onRepeatModeChanged() -> repeatMode: $repeatMode")
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        super.onShuffleModeEnabledChanged(shuffleModeEnabled)
        Logger.debug(TAG, "onShuffleModeEnabledChanged() -> " +
            "shuffleModeEnabled: $shuffleModeEnabled")
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Logger.debug(TAG, "onPlayerError() -> error: $error")
    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        super.onPlayerErrorChanged(error)
        Logger.debug(TAG, "onPlayerErrorChanged() -> error: $error")
    }

    override fun onPositionDiscontinuity(reason: Int) {
        super.onPositionDiscontinuity(reason)
        Logger.debug(TAG, "onPositionDiscontinuity() -> reason: $reason")
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
        Logger.debug(TAG, "onPositionDiscontinuity() -> " +
            "oldPosition: $oldPosition, " +
            "newPosition: $newPosition, " +
            "reason: $reason")
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        super.onPlaybackParametersChanged(playbackParameters)
        Logger.debug(TAG, "onPlaybackParametersChanged() -> " +
            "playbackParameters: $playbackParameters")
    }

    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        super.onSeekBackIncrementChanged(seekBackIncrementMs)
        Logger.debug(TAG, "onSeekBackIncrementChanged() -> " +
            "seekBackIncrementMs: $seekBackIncrementMs")
    }

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        super.onSeekForwardIncrementChanged(seekForwardIncrementMs)
        Logger.debug(TAG, "onSeekForwardIncrementChanged() -> " +
            "seekForwardIncrementMs: $seekForwardIncrementMs")
    }

    override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) {
        super.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)
        Logger.debug(TAG, "onMaxSeekToPreviousPositionChanged() -> $maxSeekToPreviousPositionMs")
    }

    override fun onSeekProcessed() {
        super.onSeekProcessed()
        Logger.debug(TAG, "onSeekProcessed()")
    }

    override fun onEvents(player: Player, events: Player.Events) {
        super.onEvents(player, events)
        Logger.debug(TAG, "onEvents() -> events: $events")
    }

}