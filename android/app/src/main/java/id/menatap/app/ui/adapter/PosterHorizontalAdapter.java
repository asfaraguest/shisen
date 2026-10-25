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

public class PosterHorizontalAdapter extends RecyclerView.Adapter<PosterHorizontalAdapter.PosterViewHolder> {

    private final Context context;
    private final List<Anime> items = new ArrayList<>();

    public PosterHorizontalAdapter(Context context) {
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
    public PosterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_anime_poster_h, parent, false);
        return new PosterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PosterViewHolder holder, int position) {
        Anime anime = items.get(position);

        String displayTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title
                : ((anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle))
                        ? anime.romajiTitle : "Anime");
        holder.tvTitle.setText(displayTitle);

        String meta = (anime.year > 0 ? (anime.year + " · ") : "") + (anime.format != null ? anime.format : "TV");
        holder.tvMeta.setText(meta);

        if (anime.score > 0) {
            holder.tvBadgeScore.setVisibility(View.VISIBLE);
            holder.tvBadgeScore.setText(String.format(Locale.US, "%.1f", anime.score));
        } else {
            holder.tvBadgeScore.setVisibility(View.GONE);
        }

        if (anime.epsCounter != null && !anime.epsCounter.isEmpty() && !"null".equalsIgnoreCase(anime.epsCounter)) {
            holder.tvBadgeEp.setVisibility(View.VISIBLE);
            holder.tvBadgeEp.setText(anime.epsCounter);
        } else {
            holder.tvBadgeEp.setVisibility(View.GONE);
        }

        ImageLoader.load(anime.poster, holder.ivPoster);
        attachSpringTouch(holder.itemView);

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

    private void attachSpringTouch(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(120).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(220)
                            .setInterpolator(new OvershootInterpolator(1.8f)).start();
                    break;
            }
            return false;
        });
    }

    static class PosterViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvBadgeScore;
        TextView tvBadgeEp;
        TextView tvTitle;
        TextView tvMeta;

        public PosterViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvBadgeScore = itemView.findViewById(R.id.tv_badge_score);
            tvBadgeEp = itemView.findViewById(R.id.tv_badge_ep);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvMeta = itemView.findViewById(R.id.tv_meta);
        }
    }
}
