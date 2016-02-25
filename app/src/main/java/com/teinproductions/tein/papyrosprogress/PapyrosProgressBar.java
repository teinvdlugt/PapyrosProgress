/*
 * Papyros Progress: An Android application showing the development progress of Papyros
 * Copyright (C) 2016  Tein van der Lugt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.teinproductions.tein.papyrosprogress;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.util.AttributeSet;
import android.view.View;

public class PapyrosProgressBar extends View {

    private int progress = 0;
    private final double angle = .85; // The inner angle of the upper corners of the triangle, in radians
    private float progressBarHeight;
    private float whitespace;
    private float indicatorRadius;
    private Paint progressPaintFull, progressPaintEmpty, indicatorPaint, textPaint, percentageTextPaint;

    @Override
    protected void onDraw(Canvas canvas) {
        float progressBarWidth = getWidth() - 2 * indicatorRadius;
        float progressLocation = indicatorRadius + progress / 100f * progressBarWidth;

        // Draw circle
        canvas.drawCircle(progressLocation, indicatorRadius, indicatorRadius, indicatorPaint);

        // Calculate triangle points:
        // The x difference from progressLocation to the upper corners of the triangle:
        float diffX = (float) (indicatorRadius * Math.cos(Math.PI / 2 - angle));
        // The y difference from indicatorRadius (center point of circle) to the upper corners oft he triangle:
        float diffY = (float) (indicatorRadius * Math.sin(Math.PI / 2 - angle));
        // The y position of the lower corner of the triangle:
        float indicatorPointY = (float) (indicatorRadius + diffY + diffX * Math.tan(angle));

        // Draw triangle:
        Path path = new Path();
        path.moveTo(progressLocation - diffX, indicatorRadius + diffY);
        path.lineTo(progressLocation + diffX, indicatorRadius + diffY);
        path.lineTo(progressLocation, indicatorPointY);
        path.close();
        canvas.drawPath(path, indicatorPaint);

        // Draw text:
        Rect textBounds = new Rect();
        String text = Integer.toString(progress);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        float textWidth = textBounds.width();
        float textHeight = textBounds.height();
        // For debugging: draw the text bounds
        // canvas.drawRect(progressLocation - textWidth / 2, indicatorRadius - textHeight / 2,
        //         progressLocation + textWidth / 2, indicatorRadius + textHeight / 2, progressPaintEmpty);
        canvas.drawText(Integer.toString(progress), progressLocation - textWidth / 2,
                indicatorRadius + textHeight / 2, textPaint);

        // Draw percentage sign:
        Rect percBounds = new Rect();
        percentageTextPaint.getTextBounds("%", 0, 1, percBounds);
        float percWidth = percBounds.width(), percHeight = percBounds.height();
        canvas.drawText("%", progressLocation - percWidth / 2,
                indicatorRadius + diffY + percHeight / 3 * 2, percentageTextPaint);

        // Draw progress bar
        float top = indicatorPointY + whitespace;
        float bottom = top + progressBarHeight;
        canvas.drawRect(indicatorRadius, top, progressLocation, bottom, progressPaintFull);
        canvas.drawRect(progressLocation, top, indicatorRadius + progressBarWidth, bottom, progressPaintEmpty);
    }

    private void init() {
        int accentColor = getColor(getContext(), R.color.colorAccent);

        progressPaintFull = new Paint();
        progressPaintFull.setColor(accentColor);
        progressPaintFull.setAntiAlias(true);

        progressPaintEmpty = new Paint();
        progressPaintEmpty.setColor(Color.LTGRAY);
        progressPaintEmpty.setAntiAlias(true);

        indicatorPaint = new Paint();
        indicatorPaint.setColor(accentColor);
        indicatorPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        percentageTextPaint = new Paint();
        percentageTextPaint.setAntiAlias(true);
        percentageTextPaint.setColor(Color.WHITE);

        applyDefaultDimensions();
    }

    private void applyDefaultDimensions() {
        float density = getResources().getDisplayMetrics().density;

        progressBarHeight = density * 4; // 4 dp
        whitespace = density * 7;
        indicatorRadius = density * 17;
        // Don't take scaledDensity into account because all elements are sized according to each other
        textPaint.setTextSize(density * 15);
        percentageTextPaint.setTextSize(density * 10);
    }

    private void multiplyDimensions(float factor) {
        progressBarHeight *= factor;
        whitespace *= factor;
        indicatorRadius *= factor;
        textPaint.setTextSize(textPaint.getTextSize() * factor);
        percentageTextPaint.setTextSize(percentageTextPaint.getTextSize() * factor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = 0, height = 0;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        } else if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED) {
            width = (int) (2 * indicatorRadius + 1);
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            applyDefaultDimensions();
            float currentHeight = calculateHeight();
            int desiredHeight = MeasureSpec.getSize(heightMeasureSpec);
            float factor = desiredHeight / currentHeight;
            multiplyDimensions(factor);
            height = desiredHeight;
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            applyDefaultDimensions();
            height = (int) calculateHeight();
        } else if (heightMode == MeasureSpec.AT_MOST) {
            applyDefaultDimensions();
            float currentHeight = calculateHeight();
            int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
            if (currentHeight <= maxHeight) {
                height = (int) currentHeight;
            } else {
                float factor = maxHeight / currentHeight;
                multiplyDimensions(factor);
                height = maxHeight;
            }
        }

        setMeasuredDimension(width, height);
    }

    private float calculateHeight() {
        return (float) (indicatorRadius + indicatorRadius * Math.sin(Math.PI / 2 - angle) +
                indicatorRadius * Math.cos(Math.PI / 2 - angle) * Math.tan(angle) +
                whitespace + progressBarHeight);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();
    }

    public PapyrosProgressBar(Context context) {
        super(context);
        init();
    }

    public PapyrosProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PapyrosProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    public static int getColor(Context context, @ColorRes int resource) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getResources().getColor(resource, context.getTheme());
        } else {
            return context.getResources().getColor(resource);
        }
    }
}
