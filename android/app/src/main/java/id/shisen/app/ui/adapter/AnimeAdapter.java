package id.shisen.app.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import id.shisen.app.R;
import id.shisen.app.data.model.Anime;
import id.shisen.app.ui.detail.DetailActivity;
import id.shisen.app.util.HapticHelper;
import id.shisen.app.util.ImageLoader;

public class AnimeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_HERO = 0;
    public static final int VIEW_TYPE_CARD = 1;
    public static final int VIEW_TYPE_HEADER = 2;

    private final Context context;
    private final List<Anime> items = new ArrayList<>();
    private Anime heroAnime = null;

    public AnimeAdapter(Context context) {
        this.context = context;
    }

    public void setHeroAnime(Anime hero) {
        this.heroAnime = hero;
        notifyDataSetChanged();
    }

    public void setData(List<Anime> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    /**
     * Memperbarui feed (hero + grid items) secara atomik bebas kedipan.
     * Jika data identik dengan tampilan saat ini, operasi diabaikan (0 layout pass, 0 flicker).
     */
    public void updateFeed(Anime newHero, List<Anime> newItems) {
        if (isFeedIdentical(newHero, newItems)) {
            return;
        }

        this.heroAnime = newHero;
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public boolean isFeedIdentical(Anime newHero, List<Anime> newItems) {
        if (this.heroAnime == null && newHero != null) return false;
        if (this.heroAnime != null && newHero == null) return false;
        if (this.heroAnime != null && !this.heroAnime.isVisuallyEquivalent(newHero)) return false;

        int currentSize = this.items.size();
        int newSize = newItems != null ? newItems.size() : 0;
        if (currentSize != newSize) return false;

        if (newItems != null) {
            for (int i = 0; i < currentSize; i++) {
                Anime a = this.items.get(i);
                Anime b = newItems.get(i);
                if (a == null && b != null) return false;
                if (a != null && b == null) return false;
                if (a != null && !a.isVisuallyEquivalent(b)) return false;
            }
        }
        return true;
    }

    public void appendData(List<Anime> newItems) {
        if (newItems == null || newItems.isEmpty()) return;
        int startPos = items.size() + (heroAnime != null ? 1 : 0);
        items.addAll(newItems);
        notifyItemRangeInserted(startPos, newItems.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (heroAnime != null && position == 0) {
            return VIEW_TYPE_HERO;
        }
        int actualPos = (heroAnime != null) ? position - 1 : position;
        if (actualPos >= 0 && actualPos < items.size()) {
            if (items.get(actualPos).isHeader) {
                return VIEW_TYPE_HEADER;
            }
        }
        return VIEW_TYPE_CARD;
    }

    @Override
    public int getItemCount() {
        int count = items.size();
        if (heroAnime != null) {
            count += 1;
        }
        return count;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HERO) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_netflix_hero, parent, false);
            return new HeroViewHolder(view);
        } else if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_lane_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_anime_card, parent, false);
            return new AnimeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_HERO) {
            bindHero((HeroViewHolder) holder, heroAnime);
        } else if (viewType == VIEW_TYPE_HEADER) {
            int actualPos = (heroAnime != null) ? position - 1 : position;
            if (actualPos >= 0 && actualPos < items.size()) {
                bindHeader((HeaderViewHolder) holder, items.get(actualPos));
            }
        } else {
            int actualPos = (heroAnime != null) ? position - 1 : position;
            if (actualPos >= 0 && actualPos < items.size()) {
                bindCard((AnimeViewHolder) holder, items.get(actualPos));
            }
        }
    }

    private void bindHeader(HeaderViewHolder holder, Anime header) {
        if (header == null) return;
        if ("CUR".equalsIgnoreCase(header.headerType)) {
            String s = getLocalizedSeasonName(context, header.season);
            holder.tvLaneTitle.setText(context.getString(R.string.lane_header_current_season, s, header.year));
            holder.tvLaneSubtitle.setText(context.getString(R.string.lane_tag_current_season));
            holder.tvLaneSubtitle.setVisibility(View.VISIBLE);
        } else if ("PREV".equalsIgnoreCase(header.headerType)) {
            String s = getLocalizedSeasonName(context, header.season);
            holder.tvLaneTitle.setText(context.getString(R.string.lane_header_prev_season, s, header.year));
            holder.tvLaneSubtitle.setText(context.getString(R.string.lane_tag_prev_season));
            holder.tvLaneSubtitle.setVisibility(View.VISIBLE);
        } else if ("LONG".equalsIgnoreCase(header.headerType)) {
            holder.tvLaneTitle.setText(context.getString(R.string.lane_header_long_running));
            holder.tvLaneSubtitle.setText(context.getString(R.string.lane_tag_long_running));
            holder.tvLaneSubtitle.setVisibility(View.VISIBLE);
        } else {
            holder.tvLaneTitle.setText(header.headerTitle != null && !header.headerTitle.isEmpty() ? header.headerTitle : header.title);
            if (header.headerSubtitle != null && !header.headerSubtitle.isEmpty()) {
                holder.tvLaneSubtitle.setText(header.headerSubtitle);
                holder.tvLaneSubtitle.setVisibility(View.VISIBLE);
            } else {
                holder.tvLaneSubtitle.setVisibility(View.GONE);
            }
        }
    }

    public static String getLocalizedSeasonName(Context context, String season) {
        if (context == null || season == null) return "";
        if ("WINTER".equalsIgnoreCase(season)) return context.getString(R.string.season_winter);
        if ("SPRING".equalsIgnoreCase(season)) return context.getString(R.string.season_spring);
        if ("SUMMER".equalsIgnoreCase(season)) return context.getString(R.string.season_summer);
        if ("FALL".equalsIgnoreCase(season)) return context.getString(R.string.season_fall);
        return season;
    }

    private void bindHero(HeroViewHolder holder, Anime anime) {
        if (anime == null) return;

        String displayTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title
                : ((anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle))
                        ? anime.romajiTitle : "Anime Unggulan");
        holder.tvHeroTitle.setText(displayTitle);

        if (anime.genres != null && !anime.genres.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int count = Math.min(3, anime.genres.size());
            for (int i = 0; i < count; i++) {
                sb.append(anime.genres.get(i).toUpperCase(Locale.US));
                if (i < count - 1) sb.append(" • ");
            }
            holder.tvHeroGenres.setText(sb.toString());
            holder.tvHeroGenres.setVisibility(View.VISIBLE);
        } else {
            holder.tvHeroGenres.setVisibility(View.GONE);
        }

        holder.tvHeroFormat.setText(anime.format != null ? anime.format : "TV");

        if (anime.score > 0) {
            holder.tvHeroScore.setVisibility(View.VISIBLE);
            holder.tvHeroScore.setText(String.format(Locale.US, "%.1f", anime.score));
        } else {
            holder.tvHeroScore.setVisibility(View.GONE);
        }

        if (holder.tvHeroTrendingTag != null) {
            holder.tvHeroTrendingTag.setText(context.getString(R.string.hero_trending_today));
        }
        if (holder.btnHeroPlay != null) {
            holder.btnHeroPlay.setText(context.getString(R.string.hero_play_now));
        }
        if (holder.btnHeroInfo != null) {
            holder.btnHeroInfo.setText(context.getString(R.string.hero_info_detail));
        }

        String heroEps = getLocalizedHeroEps(context, anime);
        if (heroEps != null && !heroEps.isEmpty()) {
            holder.tvHeroEps.setText(heroEps);
            holder.tvHeroEps.setVisibility(View.VISIBLE);
        } else {
            holder.tvHeroEps.setVisibility(View.GONE);
        }

        String heroImg = (anime.banner != null && !anime.banner.isEmpty() && !"null".equalsIgnoreCase(anime.banner))
                ? anime.banner : anime.poster;
        ImageLoader.load(heroImg, holder.ivHeroBackdrop);

        // Immediate click navigation to Detail
        View.OnClickListener openDetailAction = v -> {
            HapticHelper.performClick(v);
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("anime", anime);
            context.startActivity(intent);
        };

        View.OnClickListener playHeroAction = v -> {
            HapticHelper.performClick(v);
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("anime", anime);
            intent.putExtra("autoPlay", true);
            context.startActivity(intent);
        };

        holder.itemView.setOnClickListener(openDetailAction);
        holder.cardHero.setOnClickListener(openDetailAction);
        holder.btnHeroPlay.setOnClickListener(playHeroAction);
        holder.btnHeroInfo.setOnClickListener(openDetailAction);
    }

    private void bindCard(AnimeViewHolder holder, Anime anime) {
        String displayTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title
                : ((anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle))
                        ? anime.romajiTitle : "Anime");
        holder.tvTitle.setText(displayTitle);

        // Format meta string
        String metaText;
        if (anime.lastPlayedEp > 0) {
            metaText = context.getString(R.string.card_last_played, anime.lastPlayedEp);
        } else if (anime.airingDay != null && !anime.airingDay.isEmpty()) {
            metaText = anime.airingDay + (anime.format != null ? " · " + anime.format : "");
        } else {
            String seasonStr = (anime.season != null && !anime.season.isEmpty() && !"null".equalsIgnoreCase(anime.season))
                    ? getLocalizedSeasonName(context, anime.season)
                    : "";
            if (!seasonStr.isEmpty() && anime.year > 0) {
                metaText = seasonStr + " " + anime.year + " · " + (anime.format != null ? anime.format : "TV");
            } else if (anime.year > 0) {
                metaText = anime.year + " · " + (anime.format != null ? anime.format : "TV");
            } else {
                metaText = (anime.format != null && !anime.format.isEmpty()) ? anime.format : "Anime";
            }
        }
        holder.tvMeta.setText(metaText);

        // Format format badge
        if (anime.format != null && !anime.format.isEmpty()) {
            holder.tvBadgeType.setVisibility(View.VISIBLE);
            holder.tvBadgeType.setText(anime.format.toUpperCase(Locale.US));
        } else {
            holder.tvBadgeType.setVisibility(View.GONE);
        }

        // Score badge
        if (anime.score > 0) {
            holder.tvBadgeScore.setVisibility(View.VISIBLE);
            holder.tvBadgeScore.setText(String.format(Locale.US, "%.1f", anime.score));
        } else {
            holder.tvBadgeScore.setVisibility(View.GONE);
        }

        // Episode counter / History / Localized Status badge
        String badge = getLocalizedBadge(context, anime);
        if (badge != null && !badge.isEmpty()) {
            holder.tvBadgeEp.setVisibility(View.VISIBLE);
            holder.tvBadgeEp.setText(badge);
            boolean isFinished = "FINISHED".equalsIgnoreCase(anime.status) || "TAMAT".equalsIgnoreCase(anime.status);
            holder.tvBadgeEp.setBackgroundResource(isFinished ? R.drawable.bg_badge_mint : R.drawable.bg_badge_frost);
        } else {
            holder.tvBadgeEp.setVisibility(View.GONE);
        }

        // Load image
        ImageLoader.load(anime.poster, holder.ivPoster);

        // Direct and reliable click handling
        View.OnClickListener cardClickListener = v -> {
            HapticHelper.performClick(v);
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("anime", anime);
            context.startActivity(intent);
        };

        holder.itemView.setOnClickListener(cardClickListener);
        if (holder.cardAnime != null) {
            holder.cardAnime.setOnClickListener(cardClickListener);
        }
    }

    public static class HeroViewHolder extends RecyclerView.ViewHolder {
        View cardHero;
        ImageView ivHeroBackdrop;
        TextView tvHeroTrendingTag, tvHeroGenres, tvHeroTitle, tvHeroFormat, tvHeroScore, tvHeroEps;
        Button btnHeroPlay, btnHeroInfo;

        public HeroViewHolder(@NonNull View itemView) {
            super(itemView);
            cardHero = itemView.findViewById(R.id.card_hero);
            ivHeroBackdrop = itemView.findViewById(R.id.iv_hero_backdrop);
            tvHeroTrendingTag = itemView.findViewById(R.id.tv_hero_trending_tag);
            tvHeroGenres = itemView.findViewById(R.id.tv_hero_genres);
            tvHeroTitle = itemView.findViewById(R.id.tv_hero_title);
            tvHeroFormat = itemView.findViewById(R.id.tv_hero_format);
            tvHeroScore = itemView.findViewById(R.id.tv_hero_score);
            tvHeroEps = itemView.findViewById(R.id.tv_hero_eps);
            btnHeroPlay = itemView.findViewById(R.id.btn_hero_play);
            btnHeroInfo = itemView.findViewById(R.id.btn_hero_info);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvLaneTitle, tvLaneSubtitle;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLaneTitle = itemView.findViewById(R.id.tv_lane_title);
            tvLaneSubtitle = itemView.findViewById(R.id.tv_lane_subtitle);
        }
    }

    public static class AnimeViewHolder extends RecyclerView.ViewHolder {
        View cardAnime;
        ImageView ivPoster;
        TextView tvTitle, tvMeta, tvBadgeEp, tvBadgeScore, tvBadgeType;

        public AnimeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardAnime = itemView.findViewById(R.id.card_anime);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvBadgeEp = itemView.findViewById(R.id.tv_badge_ep);
            tvBadgeScore = itemView.findViewById(R.id.tv_badge_score);
            tvBadgeType = itemView.findViewById(R.id.tv_badge_type);
        }
    }

    public static String getLocalizedBadge(Context context, Anime anime) {
        if (anime == null || context == null) return "";
        if (anime.isLongRunning) {
            return context.getString(R.string.badge_long_running);
        }
        if (anime.lastPlayedEp > 0) {
            return "Ep " + anime.lastPlayedEp;
        }

        boolean isOngoing = "RELEASING".equalsIgnoreCase(anime.status) || "AIRING".equalsIgnoreCase(anime.status);
        boolean isNotYetAired = "NOT_YET_RELEASED".equalsIgnoreCase(anime.status) ||
                (isOngoing && anime.nextAiringEpisode == 1 && anime.airedEpisodesCount == 0);

        if (isNotYetAired) {
            return context.getString(R.string.badge_not_yet_aired);
        }
        if (isOngoing) {
            if (anime.airedEpisodesCount > 0) {
                if (anime.episodesCount > 0) {
                    return "Ep " + anime.airedEpisodesCount + "/" + anime.episodesCount;
                }
                return "Ep " + anime.airedEpisodesCount;
            }
            return context.getString(R.string.badge_ongoing);
        }
        if ("FINISHED".equalsIgnoreCase(anime.status) || "TAMAT".equalsIgnoreCase(anime.status)) {
            if (anime.episodesCount > 0) {
                return context.getString(R.string.card_episodes_finished_format, anime.episodesCount);
            }
            return context.getString(R.string.badge_finished);
        }
        if (anime.episodesCount > 0) {
            return context.getString(R.string.card_episodes_format, anime.episodesCount);
        }
        if (anime.format != null && !anime.format.isEmpty() && !"null".equalsIgnoreCase(anime.format)) {
            return anime.format.toUpperCase(Locale.US);
        }
        return "";
    }

    public static String getLocalizedHeroEps(Context context, Anime anime) {
        if (anime == null || context == null) return "";
        if (anime.episodesCount > 0) {
            boolean isFinished = "FINISHED".equalsIgnoreCase(anime.status) || "TAMAT".equalsIgnoreCase(anime.status);
            if (isFinished) {
                return context.getString(R.string.card_episodes_finished_format, anime.episodesCount);
            }
            return context.getString(R.string.card_episodes_format, anime.episodesCount);
        }
        return getLocalizedBadge(context, anime);
    }
}
