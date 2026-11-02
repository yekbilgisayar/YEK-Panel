package com.yekbilgisayar.yekpanel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class MeterView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int progress = 0;
    private int color = Color.parseColor("#22D3EE");

    public MeterView(Context context) {
        super(context);
        init();
    }

    public MeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setColor(Color.parseColor("#1B2740"));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(color);
    }

    public void setProgress(int progress, int color) {
        this.progress = Math.max(0, Math.min(100, progress));
        this.color = color;
        fillPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float radius = h / 2f;
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, radius, radius, trackPaint);
        float fillWidth = Math.max(radius * 2, w * progress / 100f);
        rect.set(0, 0, Math.min(w, fillWidth), h);
        canvas.drawRoundRect(rect, radius, radius, fillPaint);
    }
}
