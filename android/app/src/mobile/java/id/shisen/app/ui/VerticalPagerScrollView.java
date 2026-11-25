package id.shisen.app.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/** ScrollView vertikal satu-menu-penuh per halaman dengan snap otomatis antar halaman. */
public class VerticalPagerScrollView extends ScrollView {

    public interface OnPageSnapListener {
        void onPageSnapped(int pageIndex);
    }

    private int pageCount = 7;
    private int touchStartPage = 0;
    private int currentPage = 0;
    private boolean flingHandled = false;
    private OnPageSnapListener snapListener;

    public VerticalPagerScrollView(Context context) {
        super(context);
    }

    public VerticalPagerScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalPagerScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPageCount(int pageCount) {
        this.pageCount = Math.max(1, pageCount);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setOnPageSnapListener(OnPageSnapListener listener) {
        this.snapListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int pageH = getHeight();
        if (pageH > 0 && ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            touchStartPage = clampPage(Math.round(getScrollY() / (float) pageH));
            flingHandled = false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int pageH = getHeight();
        if (pageH <= 0) {
            return super.onTouchEvent(ev);
        }
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            touchStartPage = clampPage(Math.round(getScrollY() / (float) pageH));
            flingHandled = false;
        }
        boolean handled = super.onTouchEvent(ev);
        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && !flingHandled) {
            int delta = getScrollY() - (touchStartPage * pageH);
            int threshold = Math.max(1, pageH / 5);
            int target = touchStartPage;
            if (delta > threshold) {
                target = touchStartPage + 1;
            } else if (delta < -threshold) {
                target = touchStartPage - 1;
            }
            snapToPage(target, true, true);
        }
        return handled;
    }

    @Override
    public void fling(int velocityY) {
        int pageH = getHeight();
        if (pageH <= 0) return;
        flingHandled = true;
        int target = touchStartPage;
        if (velocityY > 0) {
            target = touchStartPage + 1;
        } else if (velocityY < 0) {
            target = touchStartPage - 1;
        }
        snapToPage(target, true, true);
    }

    public void snapToPage(int pageIndex, boolean smooth, boolean notify) {
        int clamped = clampPage(pageIndex);
        currentPage = clamped;
        int pageH = getHeight();
        if (pageH > 0) {
            int targetY = clamped * pageH;
            if (smooth) {
                smoothScrollTo(0, targetY);
            } else {
                scrollTo(0, targetY);
            }
        }
        if (notify && snapListener != null) {
            snapListener.onPageSnapped(clamped);
        }
    }

    private int clampPage(int page) {
        return Math.max(0, Math.min(pageCount - 1, page));
    }
}
