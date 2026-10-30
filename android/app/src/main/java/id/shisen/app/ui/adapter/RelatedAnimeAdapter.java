package id.shisen.app.ui.adapter;

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

import id.shisen.app.R;
import id.shisen.app.data.model.Anime;
import id.shisen.app.ui.detail.DetailActivity;
import id.shisen.app.util.HapticHelper;
import id.shisen.app.util.ImageLoader;

public class RelatedAnimeAdapter extends RecyclerView.Adapter<RelatedAnimeAdapter.RelatedViewHolder> {

    private final Context context;
    private final List<Anime> items = new ArrayList<>();

    public RelatedAnimeAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<Anime> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<Anime> getItems() {
        return items;
    }

    @NonNull
    @Override
    public RelatedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_related_card, parent, false);
        return new RelatedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RelatedViewHolder holder, int position) {
        Anime anime = items.get(position);

        String displayTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title
                : ((anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle))
                        ? anime.romajiTitle : "Anime");
        holder.tvTitle.setText(displayTitle);

        StringBuilder meta = new StringBuilder();
        if (anime.year > 0) {
            meta.append(anime.year).append(" · ");
        }
        meta.append(anime.format != null ? anime.format : "TV");
        holder.tvMeta.setText(meta.toString());

        if (anime.relationLabel != null && !anime.relationLabel.isEmpty() && !"null".equalsIgnoreCase(anime.relationLabel)) {
            holder.tvBadge.setText(getLocalizedRelationLabel(anime.relationLabel));
            holder.tvBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        String coverUrl = (anime.poster != null && !anime.poster.isEmpty() && !"null".equalsIgnoreCase(anime.poster))
                ? anime.poster : anime.banner;
        ImageLoader.load(coverUrl, holder.ivPoster);

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

    private String getLocalizedRelationLabel(String rawLabel) {
        if (context == null || rawLabel == null) return "";
        switch (rawLabel) {
            case "Prekuel":
                return context.getString(R.string.relation_prequel);
            case "Sekuel":
                return context.getString(R.string.relation_sequel);
            case "Seri Utama":
                return context.getString(R.string.relation_parent);
            case "Spesial":
                return context.getString(R.string.relation_special);
            case "Terkait":
                return context.getString(R.string.relation_related);
            case "Prekuel Film":
                return context.getString(R.string.relation_prequel_movie);
            case "Sekuel Film":
                return context.getString(R.string.relation_sequel_movie);
            default:
                return rawLabel;
        }
    }

    private void attachSpringTouch(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(120).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200)
                            .setInterpolator(new OvershootInterpolator(1.5f)).start();
                    break;
            }
            return false;
        });
    }

    static class RelatedViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPoster;
        final TextView tvBadge;
        final TextView tvTitle;
        final TextView tvMeta;

        RelatedViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_related_poster);
            tvBadge = itemView.findViewById(R.id.tv_related_badge);
            tvTitle = itemView.findViewById(R.id.tv_related_title);
            tvMeta = itemView.findViewById(R.id.tv_related_meta);
        }
    }
}
