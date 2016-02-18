package com.peter.coolcleaner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public class ProgressView extends View {

	private Path aboveWavePath = new Path();
	private Paint aboveWavePaint = new Paint();
	private int mHeight = 0;
	private int mPercentage;


	public ProgressView(Context context) {
		super(context);
		initializePainters();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		calculatePath();
		canvas.drawPath(aboveWavePath, aboveWavePaint);
	}

	private void initializePainters() {
		aboveWavePaint.setColor(Color.rgb(255, 255, 255));
		aboveWavePaint.setStyle(Paint.Style.FILL);
		aboveWavePaint.setAntiAlias(true);
		aboveWavePaint.setAlpha(100);
		aboveWavePaint.setTextSize(40f);
	}

	private void calculatePath() {
		aboveWavePath.reset();
		int right = getRight();

		int progressH = mHeight / 100 * mPercentage;

		aboveWavePath.moveTo(0, mHeight - progressH);
		aboveWavePath.lineTo(0, mHeight);

		aboveWavePath.lineTo(right, mHeight);
		aboveWavePath.lineTo(right, mHeight - progressH);
		aboveWavePath.lineTo(0, mHeight - progressH);

		aboveWavePath.close();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		mHeight = getMeasuredHeight();
	}

	public void setProgress(int percent) {
		mPercentage = percent;
		invalidate();
	}
}
