package id.menatap.app.data.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StreamSource implements Serializable {
    public String url = "";
    public String quality = "Auto";
    public String server = "";
    public boolean isHls = true;
    public Map<String, String> headers = new HashMap<>();

    public StreamSource(String url, String quality, String server, boolean isHls) {
        this.url = url;
        this.quality = quality;
        this.server = server;
        this.isHls = isHls;
    }
}
