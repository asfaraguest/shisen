package id.menatap.app.data.model;

import java.io.Serializable;

public class SubtitleTrack implements Serializable {
    public String url = "";
    public String language = "id";
    public String label = "Indonesian";
    public boolean isDefault = true;

    public SubtitleTrack(String url, String language, String label, boolean isDefault) {
        this.url = url;
        this.language = language;
        this.label = label;
        this.isDefault = isDefault;
    }
}
