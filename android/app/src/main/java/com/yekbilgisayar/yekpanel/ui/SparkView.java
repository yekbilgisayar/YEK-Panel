package com.yekbilgisayar.yekpanel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Son CPU yükü örneklerini ince bir grafik olarak çizer. */
public class SparkView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Float> values = new ArrayList<>();
    private final Path path = new Path();
    private final Path fill = new Path();
    private LinearGradient fillGradient;

    public SparkView(Context context) {
        super(context);
        init();
    }

    public SparkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2 * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(Color.parseColor("#22D3EE"));

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1 * density);
        gridPaint.setColor(Color.parseColor("#1B2740"));

        fillPaint.setStyle(Paint.Style.FILL);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.parseColor("#E6EDF7"));
    }

    public void push(int load) {
        if (load < 0) {
            return;
        }
        if (values.size() >= 36) {
            values.remove(0);
        }
        values.add((float) load);
        invalidate();
    }

    public void clear() {
        values.clear();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fillGradient = h > 0
                ? new LinearGradient(0, 0, 0, h,
                        Color.parseColor("#4D22D3EE"), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                : null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float pad = 3 * getResources().getDisplayMetrics().density;
        canvas.drawLine(pad, h * 0.5f, w - pad, h * 0.5f, gridPaint);

        if (values.size() < 2) {
            return;
        }
        path.reset();
        fill.reset();
        float step = (w - pad * 2) / (values.size() - 1);
        for (int i = 0; i < values.size(); i++) {
            float x = pad + step * i;
            float y = h - pad - (h - pad * 2) * (values.get(i) / 100f);
            if (i == 0) {
                path.moveTo(x, y);
                fill.moveTo(x, h);
                fill.lineTo(x, y);
            } else {
                path.lineTo(x, y);
                fill.lineTo(x, y);
            }
        }
        fill.lineTo(pad + step * (values.size() - 1), h);
        fill.close();

        fillPaint.setShader(fillGradient);
        canvas.drawPath(fill, fillPaint);
        fillPaint.setShader(null);
        canvas.drawPath(path, linePaint);

        float lastX = pad + step * (values.size() - 1);
        float lastY = h - pad - (h - pad * 2) * (values.get(values.size() - 1) / 100f);
        canvas.drawCircle(lastX, lastY, 2.6f * getResources().getDisplayMetrics().density, dotPaint);
    }
}
