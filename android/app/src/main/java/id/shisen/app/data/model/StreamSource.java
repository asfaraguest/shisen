package id.shisen.app.data.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StreamSource implements Serializable {
    public String url = "";
    public String quality = "Auto";
    public String server = "";
    public boolean isHls = true;
    public String provider = "";
    public java.util.List<SubtitleTrack> subtitles = new java.util.ArrayList<>();
    public Map<String, String> headers = new HashMap<>();
    public id.shisen.app.data.service.AniSkipClient.SkipResult skipTimes;

    public StreamSource(String url, String quality, String server, boolean isHls) {
        this.url = url;
        this.quality = quality;
        this.server = server;
        this.isHls = isHls;
    }
}
