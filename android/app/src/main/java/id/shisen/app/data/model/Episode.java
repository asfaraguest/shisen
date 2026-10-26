package id.shisen.app.data.model;

import java.io.Serializable;

public class Episode implements Serializable {
    public int number;
    public String title = "";
    public String slug = "";
    public String epId = "";
    public String provider = "";
    public String url = "";
    public boolean isWatched = false;
    public long progressMs = 0;
    public long durationMs = 0;

    public Episode(int number) {
        this.number = number;
        this.title = "Episode " + number;
    }

    public Episode(int number, String title, String slug) {
        this.number = number;
        this.title = title;
        this.slug = slug;
    }
}
