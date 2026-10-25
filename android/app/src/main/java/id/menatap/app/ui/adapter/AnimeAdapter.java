package id.menatap.app.ui.adapter;

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

import id.menatap.app.R;
import id.menatap.app.data.model.Anime;
import id.menatap.app.ui.detail.DetailActivity;
import id.menatap.app.util.HapticHelper;
import id.menatap.app.util.ImageLoader;

public class AnimeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_HERO = 0;
    public static final int VIEW_TYPE_CARD = 1;

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
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_anime_card, parent, false);
            return new AnimeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_HERO) {
            bindHero((HeroViewHolder) holder, heroAnime);
        } else {
            int actualPos = (heroAnime != null) ? position - 1 : position;
            if (actualPos >= 0 && actualPos < items.size()) {
                bindCard((AnimeViewHolder) holder, items.get(actualPos));
            }
        }
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

        if (anime.epsCounter != null && !anime.epsCounter.isEmpty() && !"null".equalsIgnoreCase(anime.epsCounter)) {
            holder.tvHeroEps.setText(anime.epsCounter);
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

        holder.itemView.setOnClickListener(openDetailAction);
        holder.cardHero.setOnClickListener(openDetailAction);
        holder.btnHeroPlay.setOnClickListener(openDetailAction);
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
            metaText = "Terakhir diputar: Ep " + anime.lastPlayedEp;
        } else if (anime.airingDay != null && !anime.airingDay.isEmpty()) {
            metaText = anime.airingDay + (anime.format != null ? " · " + anime.format : "");
        } else {
            String seasonStr = (anime.season != null && !anime.season.isEmpty() && !"null".equalsIgnoreCase(anime.season))
                    ? (anime.season.substring(0, 1).toUpperCase(Locale.US) + anime.season.substring(1).toLowerCase(Locale.US))
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

        // Episode counter / History badge
        if (anime.lastPlayedEp > 0) {
            holder.tvBadgeEp.setVisibility(View.VISIBLE);
            holder.tvBadgeEp.setText("Ep " + anime.lastPlayedEp);
            holder.tvBadgeEp.setBackgroundResource(R.drawable.bg_badge_frost);
        } else if (anime.epsCounter != null && !anime.epsCounter.isEmpty()) {
            holder.tvBadgeEp.setVisibility(View.VISIBLE);
            holder.tvBadgeEp.setText(anime.epsCounter);
            if ("TAMAT".equalsIgnoreCase(anime.status) || "FINISHED".equalsIgnoreCase(anime.status)) {
                holder.tvBadgeEp.setBackgroundResource(R.drawable.bg_badge_mint);
                holder.tvBadgeEp.setText("TAMAT");
            } else {
                holder.tvBadgeEp.setBackgroundResource(R.drawable.bg_badge_frost);
            }
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
        TextView tvHeroGenres, tvHeroTitle, tvHeroFormat, tvHeroScore, tvHeroEps;
        Button btnHeroPlay, btnHeroInfo;

        public HeroViewHolder(@NonNull View itemView) {
            super(itemView);
            cardHero = itemView.findViewById(R.id.card_hero);
            ivHeroBackdrop = itemView.findViewById(R.id.iv_hero_backdrop);
            tvHeroGenres = itemView.findViewById(R.id.tv_hero_genres);
            tvHeroTitle = itemView.findViewById(R.id.tv_hero_title);
            tvHeroFormat = itemView.findViewById(R.id.tv_hero_format);
            tvHeroScore = itemView.findViewById(R.id.tv_hero_score);
            tvHeroEps = itemView.findViewById(R.id.tv_hero_eps);
            btnHeroPlay = itemView.findViewById(R.id.btn_hero_play);
            btnHeroInfo = itemView.findViewById(R.id.btn_hero_info);
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
}
