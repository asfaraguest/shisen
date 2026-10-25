package id.menatap.app.ui.player;

import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/**
 * PlaybackService extends MediaSessionService to provide seamless background playback,
 * system media notification integration, and lockscreen playback controls.
 */
public class PlaybackService extends MediaSessionService {

    private static volatile MediaSession activeSession;

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return activeSession;
    }

    public static void setActiveSession(MediaSession session) {
        activeSession = session;
    }

    public static MediaSession getActiveSession() {
        return activeSession;
    }

    @Override
    public void onDestroy() {
        activeSession = null;
        super.onDestroy();
    }
}
