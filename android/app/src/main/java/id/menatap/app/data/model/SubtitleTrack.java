package id.menatap.app.data.model;

import java.io.Serializable;

public class SubtitleTrack implements Serializable {
    public String url = "";
    public String language = "id";
    public String lang = "id";
    public String label = "Indonesian";
    public boolean isDefault = true;
    public String referer = "";

    public SubtitleTrack(String url, String language, String label, boolean isDefault) {
        this(url, language, label, isDefault, "");
    }

    public SubtitleTrack(String url, String language, String label, boolean isDefault, String referer) {
        this.url = url;
        this.language = language;
        this.lang = language;
        this.label = label;
        this.isDefault = isDefault;
        this.referer = referer != null ? referer : "";
    }
}
