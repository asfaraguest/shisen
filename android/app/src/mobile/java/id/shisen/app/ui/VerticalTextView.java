package id.shisen.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/** TextView ringan yang dapat merotasi label -90 derajat agar terbaca vertikal pada sidebar collapsed. */
public class VerticalTextView extends AppCompatTextView {

    private boolean verticalText = false;

    public VerticalTextView(@NonNull Context context) {
        super(context);
    }

    public VerticalTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setVerticalText(boolean verticalText) {
        if (this.verticalText != verticalText) {
            this.verticalText = verticalText;
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!verticalText) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        // Tukar sumbu ukur supaya ellipsize dan panjang teks mengikuti ruang vertikal aktual
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!verticalText) {
            super.onDraw(canvas);
            return;
        }
        Layout layout = getLayout();
        if (layout == null) {
            super.onDraw(canvas);
            return;
        }
        TextPaint textPaint = getPaint();
        textPaint.setColor(getCurrentTextColor());
        textPaint.drawableState = getDrawableState();

        canvas.save();
        canvas.translate(0f, getHeight());
        canvas.rotate(-90f);
        float transX = Math.max(0f, (getHeight() - layout.getWidth()) / 2f);
        float transY = Math.max(0f, (getWidth() - layout.getHeight()) / 2f);
        canvas.translate(transX, transY);
        layout.draw(canvas);
        canvas.restore();
    }
}
