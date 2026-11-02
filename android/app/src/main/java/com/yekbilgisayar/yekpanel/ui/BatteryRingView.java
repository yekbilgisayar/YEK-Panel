package com.yekbilgisayar.yekpanel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class BatteryRingView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private int percent = -1;
    private boolean charging = false;

    public BatteryRingView(Context context) {
        super(context);
        init();
    }

    public BatteryRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(10 * density);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(Color.parseColor("#1B2740"));

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(10 * density);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(Color.parseColor("#22D3EE"));

        valuePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(Color.parseColor("#E6EDF7"));

        labelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(Color.parseColor("#8EA1BD"));
        labelPaint.setLetterSpacing(0.12f);
    }

    public void setData(int percent, boolean charging) {
        this.percent = Math.max(0, Math.min(100, percent));
        this.charging = charging;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float width = getWidth();
        float height = getHeight();
        float size = Math.min(width, height);
        float stroke = 10 * density;
        float padding = stroke / 2f + 4 * density;

        oval.set((width - size) / 2f + padding, (height - size) / 2f + padding,
                (width + size) / 2f - padding, (height + size) / 2f - padding);

        float start = 135f;
        float sweep = 270f;
        canvas.drawArc(oval, start, sweep, false, trackPaint);

        int value = percent < 0 ? 0 : percent;
        int color = Color.parseColor("#22D3EE");
        if (value <= 15) {
            color = Color.parseColor("#F87171");
        } else if (value <= 30) {
            color = Color.parseColor("#F59E0B");
        } else if (charging) {
            color = Color.parseColor("#34D399");
        }
        progressPaint.setColor(color);
        if (value > 0) {
            canvas.drawArc(oval, start, sweep * value / 100f, false, progressPaint);
        }

        float cx = width / 2f;
        float cy = height / 2f;
        valuePaint.setTextSize(size * 0.24f);
        String text = percent < 0 ? "—" : String.valueOf(percent);
        float valueOffset = size * 0.03f;
        canvas.drawText(text, cx, cy + valueOffset, valuePaint);

        valuePaint.setTextSize(size * 0.10f);
        valuePaint.setColor(Color.parseColor("#8EA1BD"));
        canvas.drawText("%", cx + valuePaint.measureText(String.valueOf(value)) * 1.9f,
                cy - size * 0.06f, valuePaint);
        valuePaint.setColor(Color.parseColor("#E6EDF7"));

        labelPaint.setTextSize(size * 0.075f);
        canvas.drawText(charging ? "ŞARJ" : "PİL", cx, cy + size * 0.22f, labelPaint);
    }
}
