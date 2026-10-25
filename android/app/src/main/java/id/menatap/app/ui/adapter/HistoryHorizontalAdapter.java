package id.menatap.app.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import id.menatap.app.R;
import id.menatap.app.data.model.Anime;
import id.menatap.app.ui.detail.DetailActivity;
import id.menatap.app.util.HapticHelper;
import id.menatap.app.util.ImageLoader;

public class HistoryHorizontalAdapter extends RecyclerView.Adapter<HistoryHorizontalAdapter.HistoryViewHolder> {

    private final Context context;
    private final List<Anime> items = new ArrayList<>();

    public HistoryHorizontalAdapter(Context context) {
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
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history_card_h, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Anime anime = items.get(position);

        String displayTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title
                : ((anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle))
                        ? anime.romajiTitle : "Anime");
        holder.tvTitle.setText(displayTitle);

        if (anime.lastPlayedEp > 0) {
            holder.tvEp.setText("Ep " + anime.lastPlayedEp);
        } else {
            holder.tvEp.setText("Mulai");
        }

        // Progress bar simulation / actual percentage
        int progress = 60;
        if (anime.lastPlayedProgressMs > 0) {
            // Assume average 24m episode = 1440000ms
            progress = (int) Math.min(100, Math.max(15, (anime.lastPlayedProgressMs * 100) / 1440000));
        }
        holder.pbProgress.setProgress(progress);

        String image = (anime.banner != null && !anime.banner.isEmpty() && !"null".equalsIgnoreCase(anime.banner))
                ? anime.banner : anime.poster;
        ImageLoader.load(image, holder.ivThumb);

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

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvEp;
        TextView tvTitle;
        ProgressBar pbProgress;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_history_thumb);
            tvEp = itemView.findViewById(R.id.tv_history_ep);
            tvTitle = itemView.findViewById(R.id.tv_history_title);
            pbProgress = itemView.findViewById(R.id.pb_history_progress);
        }
    }
}
