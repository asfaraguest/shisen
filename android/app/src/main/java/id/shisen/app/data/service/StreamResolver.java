package id.shisen.app.data.service;

import java.io.IOException;
import java.util.List;

import id.shisen.app.data.model.Anime;
import id.shisen.app.data.model.Episode;
import id.shisen.app.data.model.StreamSource;
import id.shisen.app.util.TitleMatcher;

public class StreamResolver {

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

        if ("anichan".equalsIgnoreCase(preferredServer)) {
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
    }

    private static void tryHiAnime(Anime anime, Episode episode, Callback callback) {
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
                    fetchHiAnimeEpAndResolve(pickedSlug, episode, callback);
                } else if (!altTitle.isEmpty() && !altTitle.equalsIgnoreCase(targetTitle)) {
                    HiAnimeClient.searchAnime(altTitle, new HiAnimeClient.Callback<List<Anime>>() {
                        @Override
                        public void onSuccess(List<Anime> res2) {
                            String picked2 = pickBestSlug(res2, anime, targetTitle, altTitle);
                            if (picked2 != null) {
                                fetchHiAnimeEpAndResolve(picked2, episode, callback);
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
        if (candidates == null || candidates.isEmpty()) return null;
        boolean hasAlt = alt != null && !alt.isEmpty();
        boolean hasTarget = target != null && !target.isEmpty();
        for (Anime c : candidates) {
            if (c == null || c.title == null) continue;
            if (TitleMatcher.isMatchAny(anime, c)) {
                return c.slug;
            }
            if ((hasTarget && TitleMatcher.isTitleMatch(target, c.title)) || (hasAlt && TitleMatcher.isTitleMatch(alt, c.title))) {
                return c.slug;
            }
            if (c.romajiTitle != null && !c.romajiTitle.isEmpty()) {
                if ((hasTarget && TitleMatcher.isTitleMatch(target, c.romajiTitle)) || (hasAlt && TitleMatcher.isTitleMatch(alt, c.romajiTitle))) {
                    return c.slug;
                }
            }
        }
        // Season-consistent match: only if season matches AND title also matches target or alt!
        if (hasTarget || hasAlt) {
            int sTarget = hasTarget ? TitleMatcher.extractSeasonNumber(target) : TitleMatcher.extractSeasonNumber(alt);
            for (Anime c : candidates) {
                if (c == null || c.title == null) continue;
                int sCandidate = TitleMatcher.extractSeasonNumber(c.title);
                if (sTarget == sCandidate) {
                    if ((hasTarget && TitleMatcher.isTitleMatch(target, c.title)) || (hasAlt && TitleMatcher.isTitleMatch(alt, c.title))) {
                        return c.slug;
                    }
                }
            }
        }
        return null;
    }

    private static void fetchHiAnimeEpAndResolve(String slug, Episode episode, Callback callback) {
        HiAnimeClient.getEpisodes(slug, new HiAnimeClient.Callback<List<Episode>>() {
            @Override
            public void onSuccess(List<Episode> episodes) {
                if (episodes == null || episodes.isEmpty()) {
                    callback.onError(new IOException("Daftar episode kosong di HiAnime"));
                    return;
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

                String otakuSlug = null;
                for (Anime r : results) {
                    if (TitleMatcher.isTitleMatch(searchQ, r.title) || TitleMatcher.isMatchAny(anime, r)) {
                        otakuSlug = r.slug;
                        break;
                    }
                }
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
}
