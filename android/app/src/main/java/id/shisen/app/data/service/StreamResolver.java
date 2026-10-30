package id.shisen.app.data.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import id.shisen.app.data.cache.FeedCache;
import id.shisen.app.data.model.Anime;
import id.shisen.app.data.model.Episode;
import id.shisen.app.data.model.StreamSource;
import id.shisen.app.ui.player.PlaybackDataHolder;
import id.shisen.app.util.TitleMatcher;

public class StreamResolver {

    private static final Map<String, String> slugCache = new ConcurrentHashMap<>();

    public static void cacheSlug(String key, String hiAnimeSlug) {
        if (key != null && hiAnimeSlug != null && !hiAnimeSlug.isEmpty()) {
            slugCache.put(key.toLowerCase(Locale.US).trim(), hiAnimeSlug);
        }
    }

    public static String getCachedSlug(String key) {
        if (key == null) return null;
        return slugCache.get(key.toLowerCase(Locale.US).trim());
    }

    public interface Callback {
        void onSuccess(StreamSource source);
        void onError(Throwable t);
    }

    public static void resolve(Anime anime, Episode episode, Callback callback) {
        resolveWithServer(anime, episode, "hianime", callback);
    }

    public static void resolveWithServer(Anime anime, Episode episode, String preferredServer, Callback callback) {
        if (anime == null || episode == null) {
            callback.onError(new IllegalArgumentException("Anime atau episode tidak boleh kosong"));
            return;
        }

        if ("hianimes_se".equalsIgnoreCase(preferredServer)) {
            tryHianimesSe(anime, episode, new Callback() {
                @Override
                public void onSuccess(StreamSource source) {
                    callback.onSuccess(source);
                }

                @Override
                public void onError(Throwable t1) {
                    tryHiAnime(anime, episode, new Callback() {
                        @Override
                        public void onSuccess(StreamSource source) {
                            callback.onSuccess(source);
                        }

                        @Override
                        public void onError(Throwable t2) {
                            tryAniChan(anime, episode, new Callback() {
                                @Override
                                public void onSuccess(StreamSource source) {
                                    callback.onSuccess(source);
                                }

                                @Override
                                public void onError(Throwable t3) {
                                    tryOtakudesu(anime, episode, callback);
                                }
                            });
                        }
                    });
                }
            });
        } else if ("anichan".equalsIgnoreCase(preferredServer)) {
            tryAniChan(anime, episode, new Callback() {
                @Override
                public void onSuccess(StreamSource source) {
                    callback.onSuccess(source);
                }

                @Override
                public void onError(Throwable t1) {
                    tryHiAnime(anime, episode, new Callback() {
                        @Override
                        public void onSuccess(StreamSource source) {
                            callback.onSuccess(source);
                        }

                        @Override
                        public void onError(Throwable t2) {
                            tryOtakudesu(anime, episode, callback);
                        }
                    });
                }
            });
        } else if ("otakudesu".equalsIgnoreCase(preferredServer)) {
            tryOtakudesu(anime, episode, new Callback() {
                @Override
                public void onSuccess(StreamSource source) {
                    callback.onSuccess(source);
                }

                @Override
                public void onError(Throwable t1) {
                    tryHiAnime(anime, episode, new Callback() {
                        @Override
                        public void onSuccess(StreamSource source) {
                            callback.onSuccess(source);
                        }

                        @Override
                        public void onError(Throwable t2) {
                            tryAniChan(anime, episode, callback);
                        }
                    });
                }
            });
        } else {
            // TIER 1: HiAnime (Primary Stream Source)
            tryHiAnime(anime, episode, new Callback() {
                @Override
                public void onSuccess(StreamSource source) {
                    callback.onSuccess(source);
                }

                @Override
                public void onError(Throwable t1) {
                    // TIER 1.5: HiAnimes.se (Direct MAL / Alternative Embed Fallback)
                    tryHianimesSe(anime, episode, new Callback() {
                        @Override
                        public void onSuccess(StreamSource source) {
                            callback.onSuccess(source);
                        }

                        @Override
                        public void onError(Throwable t1b) {
                            // TIER 2: AniChan (Direct HLS Fallback)
                            tryAniChan(anime, episode, new Callback() {
                                @Override
                                public void onSuccess(StreamSource source) {
                                    callback.onSuccess(source);
                                }

                                @Override
                                public void onError(Throwable t2) {
                                    // TIER 3: Otakudesu (Safety Net Fallback)
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
                                                    "2. HiAnimes.se: " + t1b.getMessage() + "\n" +
                                                    "3. AniChan: " + t2.getMessage() + "\n" +
                                                    "4. Otakudesu: " + t3.getMessage()
                                            ));
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private static void tryHiAnime(Anime anime, Episode episode, Callback callback) {
        // Fast-path 1: Jika episode sudah memiliki epId HiAnime, selesaikan langsung (<500ms)
        if (episode.epId != null && !episode.epId.isEmpty() && "hianime".equalsIgnoreCase(episode.provider)) {
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

        // Fast-path 1b: Periksa FeedCache apakah daftar episode anime ini sudah pernah tersimpan dengan epId HiAnime
        FeedCache feedCache = FeedCache.getInstance(null);
        if (feedCache != null && anime.slug != null) {
            List<Episode> cachedEps = feedCache.getCachedEpisodes(anime.slug);
            if (cachedEps != null && !cachedEps.isEmpty()) {
                Episode matchedCached = null;
                for (Episode ce : cachedEps) {
                    if (ce.number == episode.number && ce.epId != null && !ce.epId.isEmpty() && "hianime".equalsIgnoreCase(ce.provider)) {
                        matchedCached = ce;
                        break;
                    }
                }
                if (matchedCached != null) {
                    episode.epId = matchedCached.epId;
                    episode.provider = "hianime";
                    if (PlaybackDataHolder.hasData()) {
                        PlaybackDataHolder.set(anime, cachedEps, PlaybackDataHolder.getIndex());
                    }
                    HiAnimeClient.resolveStream(matchedCached.epId, new HiAnimeClient.Callback<StreamSource>() {
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
        }

        // Fast-path 2: Jika slug HiAnime untuk anime ini sudah pernah dicocokkan & dicache
        String cachedSlug = getCachedSlug(anime.slug);
        if (cachedSlug == null && anime.id > 0) {
            cachedSlug = getCachedSlug(String.valueOf(anime.id));
        }
        if (cachedSlug != null) {
            fetchHiAnimeEpAndResolve(cachedSlug, anime, episode, callback);
            return;
        }

        searchHiAnimeKeyword(anime, episode, callback);
    }

    private static void searchHiAnimeKeyword(Anime anime, Episode episode, Callback callback) {
        String primaryQuery = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title : anime.romajiTitle;
        if (primaryQuery == null || primaryQuery.isEmpty() || "null".equalsIgnoreCase(primaryQuery)) {
            primaryQuery = "Anime";
        }

        final String targetTitle = primaryQuery;
        final String altTitle = (anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle))
                ? anime.romajiTitle : "";

        HiAnimeClient.searchAnime(targetTitle, new HiAnimeClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> results) {
                String pickedSlug = pickBestSlug(results, anime, targetTitle, altTitle);
                if (pickedSlug != null) {
                    cacheSlug(anime.slug, pickedSlug);
                    if (anime.id > 0) cacheSlug(String.valueOf(anime.id), pickedSlug);
                    fetchHiAnimeEpAndResolve(pickedSlug, anime, episode, callback);
                } else if (!altTitle.isEmpty() && !altTitle.equalsIgnoreCase(targetTitle)) {
                    HiAnimeClient.searchAnime(altTitle, new HiAnimeClient.Callback<List<Anime>>() {
                        @Override
                        public void onSuccess(List<Anime> res2) {
                            String picked2 = pickBestSlug(res2, anime, targetTitle, altTitle);
                            if (picked2 != null) {
                                cacheSlug(anime.slug, picked2);
                                if (anime.id > 0) cacheSlug(String.valueOf(anime.id), picked2);
                                fetchHiAnimeEpAndResolve(picked2, anime, episode, callback);
                            } else {
                                callback.onError(new IOException("Judul tidak cocok di HiAnime"));
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            callback.onError(t);
                        }
                    });
                } else {
                    callback.onError(new IOException("Anime tidak ditemukan di HiAnime"));
                }
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    private static String pickBestSlug(List<Anime> candidates, Anime anime, String target, String alt) {
        return TitleMatcher.pickBestSlug(candidates, anime, target, alt);
    }

    private static void fetchHiAnimeEpAndResolve(String slug, Anime anime, Episode episode, Callback callback) {
        HiAnimeClient.getEpisodes(slug, new HiAnimeClient.Callback<List<Episode>>() {
            @Override
            public void onSuccess(List<Episode> episodes) {
                if (episodes == null || episodes.isEmpty()) {
                    callback.onError(new IOException("Daftar episode kosong di HiAnime"));
                    return;
                }

                // Perkaya saudara-saudara episode di PlaybackDataHolder agar episode selanjutnya 0ms resolve
                if (PlaybackDataHolder.hasData()) {
                    List<Episode> holderEps = PlaybackDataHolder.getEpisodes();
                    if (holderEps != null) {
                        for (Episode he : holderEps) {
                            if (he.epId != null && !he.epId.isEmpty()) continue;
                            for (Episode fe : episodes) {
                                if (he.number == fe.number) {
                                    he.epId = fe.epId;
                                    he.provider = "hianime";
                                    break;
                                }
                            }
                        }
                        PlaybackDataHolder.set(PlaybackDataHolder.getAnime(), holderEps, PlaybackDataHolder.getIndex());
                    }
                }

                // Simpan juga ke FeedCache agar persisten di memory dan disk
                FeedCache fc = FeedCache.getInstance(null);
                if (fc != null && anime.slug != null) {
                    fc.saveEpisodeList(anime.slug, episodes);
                }

                for (Episode ep : episodes) {
                    if (ep.number == episode.number) {
                        episode.epId = ep.epId;
                        episode.provider = "hianime";
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
        String cleanTitle = (anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle))
                ? anime.romajiTitle : anime.title;
        cleanTitle = cleanTitle.replaceAll("(?i)(season|part|s\\d+|2nd|3rd|4th)\\b", "").trim();

        final String searchQ = cleanTitle;
        OtakudesuScraper.searchAnime(searchQ, new OtakudesuScraper.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> results) {
                if (results == null || results.isEmpty()) {
                    callback.onError(new IOException("Anime tidak ditemukan di Otakudesu"));
                    return;
                }

                String otakuSlug = TitleMatcher.pickBestSlug(results, anime, anime.title, anime.romajiTitle);
                if (otakuSlug == null) {
                    callback.onError(new IOException("Judul tidak cocok di Otakudesu"));
                    return;
                }

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

    private static void tryHianimesSe(Anime anime, Episode episode, Callback callback) {
        HianimesSeClient.searchAndResolve(anime, episode, new HianimesSeClient.Callback<StreamSource>() {
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
}
