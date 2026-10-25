package id.menatap.app.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
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

public class AnimeAdapter extends RecyclerView.Adapter<AnimeAdapter.AnimeViewHolder> {
    private final Context context;
    private final List<Anime> items = new ArrayList<>();

    public AnimeAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<Anime> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AnimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_anime_card, parent, false);
        return new AnimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimeViewHolder holder, int position) {
        Anime anime = items.get(position);

        String displayTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title
                : ((anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle))
                        ? anime.romajiTitle : "Anime");
        holder.tvTitle.setText(displayTitle);
        
        // Format meta string
        String seasonStr = (anime.season != null && !anime.season.isEmpty() && !"null".equalsIgnoreCase(anime.season))
                ? (anime.season.substring(0, 1).toUpperCase(Locale.US) + anime.season.substring(1).toLowerCase(Locale.US))
                : "";
        String metaText;
        if (!seasonStr.isEmpty() && anime.year > 0) {
            metaText = seasonStr + " " + anime.year + " · " + (anime.format != null ? anime.format : "TV");
        } else if (anime.year > 0) {
            metaText = anime.year + " · " + (anime.format != null ? anime.format : "TV");
        } else {
            metaText = (anime.format != null && !anime.format.isEmpty()) ? anime.format : "Anime";
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

        // Episode counter badge
        if (anime.epsCounter != null && !anime.epsCounter.isEmpty()) {
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

        // Spring touch feedback
        attachSpringTouchFeedback(holder.itemView);

        holder.itemView.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("anime", anime);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void attachSpringTouchFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(120).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(220)
                            .setInterpolator(new OvershootInterpolator(1.2f)).start();
                    break;
            }
            return false;
        });
    }

    public static class AnimeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle, tvMeta, tvBadgeEp, tvBadgeScore, tvBadgeType;

        public AnimeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvBadgeEp = itemView.findViewById(R.id.tv_badge_ep);
            tvBadgeScore = itemView.findViewById(R.id.tv_badge_score);
            tvBadgeType = itemView.findViewById(R.id.tv_badge_type);
        }
    }
}
