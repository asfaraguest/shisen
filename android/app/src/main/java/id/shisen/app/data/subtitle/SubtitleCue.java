package id.shisen.app.data.subtitle;

public class SubtitleCue {
    public long startMs;
    public long endMs;
    public String text;

    public SubtitleCue() {}

    public SubtitleCue(long startMs, long endMs, String text) {
        this.startMs = startMs;
        this.endMs = endMs;
        this.text = text != null ? text : "";
    }

    public boolean isActive(long positionMs) {
        return positionMs >= startMs && positionMs <= endMs;
    }
}
