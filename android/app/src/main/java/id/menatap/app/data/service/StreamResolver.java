package id.menatap.app.data.service;

import java.io.IOException;
import java.util.List;

import id.menatap.app.data.model.Anime;
import id.menatap.app.data.model.Episode;
import id.menatap.app.data.model.StreamSource;

public class StreamResolver {

    public interface Callback {
        void onSuccess(StreamSource source);
        void onError(Throwable t);
    }

    public static void resolve(Anime anime, Episode episode, Callback callback) {
        if (anime == null || episode == null) {
            callback.onError(new IllegalArgumentException("Anime atau episode tidak boleh kosong"));
            return;
        }

        // TIER 1: HiAnime (Primary)
        tryHiAnime(anime, episode, new Callback() {
            @Override
            public void onSuccess(StreamSource source) {
                callback.onSuccess(source);
            }

            @Override
            public void onError(Throwable t1) {
                // TIER 2: AniChan (Fallback 1)
                tryAniChan(anime, episode, new Callback() {
                    @Override
                    public void onSuccess(StreamSource source) {
                        callback.onSuccess(source);
                    }

                    @Override
                    public void onError(Throwable t2) {
                        // TIER 3: Otakudesu (Fallback 2 / Safety Net Terakhir)
                        tryOtakudesu(anime, episode, new Callback() {
                            @Override
                            public void onSuccess(StreamSource source) {
                                callback.onSuccess(source);
                            }

                            @Override
                            public void onError(Throwable t3) {
                                callback.onError(new IOException(
                                        "Seluruh sumber stream gagal:\n" +
                                        "1. HiAnime: " + t1.getMessage() + "\n" +
                                        "2. AniChan: " + t2.getMessage() + "\n" +
                                        "3. Otakudesu: " + t3.getMessage()
                                ));
                            }
                        });
                    }
                });
            }
        });
    }

    private static void tryHiAnime(Anime anime, Episode episode, Callback callback) {
        if (episode.epId != null && !episode.epId.isEmpty()) {
            HiAnimeClient.resolveStream(episode.epId, new HiAnimeClient.Callback<StreamSource>() {
                @Override
                public void onSuccess(StreamSource result) {
                    callback.onSuccess(result);
                }

                @Override
                public void onError(Throwable t) {
                    callback.onError(t);
                }
            });
            return;
        }

        // Need to search HiAnime by title
        String query = (anime.title != null && !anime.title.isEmpty()) ? anime.title : anime.romajiTitle;
        HiAnimeClient.searchAnime(query, new HiAnimeClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> results) {
                if (results == null || results.isEmpty()) {
                    callback.onError(new IOException("Anime tidak ditemukan di HiAnime"));
                    return;
                }

                String slug = results.get(0).slug;
                HiAnimeClient.getEpisodes(slug, new HiAnimeClient.Callback<List<Episode>>() {
                    @Override
                    public void onSuccess(List<Episode> episodes) {
                        for (Episode ep : episodes) {
                            if (ep.number == episode.number) {
                                episode.epId = ep.epId;
                                HiAnimeClient.resolveStream(ep.epId, new HiAnimeClient.Callback<StreamSource>() {
                                    @Override
                                    public void onSuccess(StreamSource result) {
                                        callback.onSuccess(result);
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        callback.onError(t);
                                    }
                                });
                                return;
                            }
                        }
                        callback.onError(new IOException("Episode " + episode.number + " tidak ditemukan di HiAnime"));
                    }

                    @Override
                    public void onError(Throwable t) {
                        callback.onError(t);
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    private static void tryAniChan(Anime anime, Episode episode, Callback callback) {
        if (anime.id <= 0) {
            callback.onError(new IOException("AniList ID kosong untuk AniChan"));
            return;
        }

        AniChanClient.resolveStream(anime.id, episode.number, new AniChanClient.Callback<StreamSource>() {
            @Override
            public void onSuccess(StreamSource result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    private static void tryOtakudesu(Anime anime, Episode episode, Callback callback) {
        String cleanTitle = (anime.romajiTitle != null && !anime.romajiTitle.isEmpty()) ? anime.romajiTitle : anime.title;
        cleanTitle = cleanTitle.replaceAll("(?i)(season|part|s\\d+|2nd|3rd|4th)\\b", "").trim();

        OtakudesuScraper.searchAnime(cleanTitle, new OtakudesuScraper.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> results) {
                if (results == null || results.isEmpty()) {
                    callback.onError(new IOException("Anime tidak ditemukan di Otakudesu"));
                    return;
                }

                String otakuSlug = results.get(0).slug;
                OtakudesuScraper.getEpisodes(otakuSlug, new OtakudesuScraper.Callback<List<Episode>>() {
                    @Override
                    public void onSuccess(List<Episode> episodes) {
                        for (Episode ep : episodes) {
                            if (ep.number == episode.number) {
                                String epSlugOrUrl = (ep.url != null && !ep.url.isEmpty()) ? ep.url : ep.slug;
                                OtakudesuScraper.resolveStream(epSlugOrUrl, new OtakudesuScraper.Callback<StreamSource>() {
                                    @Override
                                    public void onSuccess(StreamSource result) {
                                        callback.onSuccess(result);
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        callback.onError(t);
                                    }
                                });
                                return;
                            }
                        }
                        callback.onError(new IOException("Episode " + episode.number + " belum dirilis di Otakudesu"));
                    }

                    @Override
                    public void onError(Throwable t) {
                        callback.onError(t);
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }
}
