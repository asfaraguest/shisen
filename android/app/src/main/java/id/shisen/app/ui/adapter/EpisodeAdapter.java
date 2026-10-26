package id.shisen.app.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import id.shisen.app.R;
import id.shisen.app.data.model.Episode;
import id.shisen.app.util.HapticHelper;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder> {

    public interface OnEpisodeClickListener {
        void onEpisodeClick(Episode episode, int position);
    }

    private final Context context;
    private final List<Episode> items = new ArrayList<>();
    private int currentPlayingNumber = -1;
    private OnEpisodeClickListener listener;

    public EpisodeAdapter(Context context) {
        this.context = context;
    }

    public void setListener(OnEpisodeClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Episode> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setCurrentPlayingNumber(int number) {
        int oldPos = findPositionByNumber(this.currentPlayingNumber);
        this.currentPlayingNumber = number;
        int newPos = findPositionByNumber(number);

        if (oldPos != -1) notifyItemChanged(oldPos);
        if (newPos != -1) notifyItemChanged(newPos);
    }

    public int findPositionByNumber(int number) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).number == number) {
                return i;
            }
        }
        return -1;
    }

    public Episode getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_episode_tile, parent, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        Episode ep = items.get(position);
        boolean isCurrent = (ep.number == currentPlayingNumber);

        holder.tvTitle.setText(String.valueOf(ep.number));

        if (isCurrent) {
            // Active playing state
            holder.card.setStrokeColor(ContextCompat.getColor(context, R.color.accent_glacial));
            holder.card.setStrokeWidth((int) (1.5f * context.getResources().getDisplayMetrics().density));
            holder.card.setCardBackgroundColor(Color.parseColor("#15233C"));
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.accent_glacial));

            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(R.string.episode_now_playing);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_glacial));
        } else if (ep.isWatched) {
            // Watched state
            holder.card.setStrokeColor(ContextCompat.getColor(context, R.color.frost_border));
            holder.card.setStrokeWidth((int) (1f * context.getResources().getDisplayMetrics().density));
            holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.frost_surface));
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_frost_subtle));

            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(R.string.episode_watched);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_mint));
        } else {
            // Default unplayed state
            holder.card.setStrokeColor(ContextCompat.getColor(context, R.color.frost_border));
            holder.card.setStrokeWidth((int) (1f * context.getResources().getDisplayMetrics().density));
            holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.frost_surface));
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_frost_bright));

            holder.tvStatusBadge.setVisibility(View.GONE);
        }

        attachSpringTouchFeedback(holder.card);

        holder.card.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            if (listener != null) {
                listener.onEpisodeClick(ep, holder.getBindingAdapterPosition());
            }
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
                    HapticHelper.performSelection(v);
                    v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .setDuration(220)
                            .start();
                    break;
            }
            return false;
        });
    }

    static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final LinearLayout layoutBody;
        final TextView tvStatusBadge;
        final TextView tvTitle;

        EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_episode);
            layoutBody = itemView.findViewById(R.id.layout_ep_body);
            tvStatusBadge = itemView.findViewById(R.id.tv_ep_status_badge);
            tvTitle = itemView.findViewById(R.id.tv_ep_title);
        }
    }
}
