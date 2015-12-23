
package com.peter.coolcleaner;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.peter.coolcleaner.Main.AppInfo;
import com.peter.coolcleaner.Main.MyOnGestureListener;
import com.peter.coolcleaner.factory.ExplodeParticleFactory;
import com.peter.coolcleaner.factory.FallingParticleFactory;
import com.peter.coolcleaner.factory.VerticalAscentFactory;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeAnimator;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;


public class Board extends RelativeLayout {
	
	GestureDetector mGestureDetector;
	MyOnGestureListener mGestureListener;

	public Board(Context context, AttributeSet as) {
		super(context, as);
		setWillNotDraw(true);
		mGestureListener = new MyOnGestureListener((Main) context);
		mGestureDetector = new GestureDetector(context, mGestureListener);
//		setBackgroundResource(R.drawable.bg);
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mGestureListener.setCurrentView(Board.this);
		mGestureDetector.onTouchEvent(event);
		return true;
	}

	private Random sRNG = new Random();

	private float lerp(float a, float b, float f) {
		return (b - a) * f + a;
	}

	private float randfrange(float a, float b) {
		return lerp(a, b, sRNG.nextFloat());
	}

	private int randsign() {
		return sRNG.nextBoolean() ? 1 : -1;
	}

	private float mag(float x, float y) {
		return (float) Math.sqrt(x * x + y * y);
	}

	private float clamp(float x, float a, float b) {
		return ((x < a) ? a : ((x > b) ? b : x));
	}

	public class Head extends FrameLayout {
		
		float x, y, a;

		float va;
		float vx, vy;

		float z;

		int h, w;

		private boolean grabbed;
		private float grabx, graby;
		private float grabx_offset, graby_offset;

		public Rect mRect;

		public boolean killed;

		int boardWidth, boardHeight;
		
		ImageView lock;

		private Head(Context context, AttributeSet as) {
			super(context, as);
			lock = new ImageView(context);
			lock.setImageResource(R.drawable.lock);
			lock.setVisibility(View.GONE);
			addView(lock);
		}
		
		private void show() {
			PropertyValuesHolder pvScaleX = PropertyValuesHolder.ofFloat("scaleX", 0, 1);
			PropertyValuesHolder pvScaleY = PropertyValuesHolder.ofFloat("scaleY", 0, 1);
			ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(this, pvScaleX, pvScaleY).setDuration(500);
			anim.setInterpolator(new EaseInOutBackInterpolator());
			anim.start();
		}
		
		public boolean outEdges() {
			int cell = w / 2;
			if(x < cell//左
	        		||x > boardWidth - cell//右
	        		||y < cell//上
	        		||y > boardHeight - cell) {//下
				return true;
			}
			return false;
		}
		
		public boolean speedRemove() {
			if(vx > 3500 || vy > 3500 || vx < -3500 || vy < -3500) {
				return true;
			}
			return false;
		}
		
		public void getHitRect(Rect outRect) {
		    if (mRect == null){
		        super.getHitRect(outRect);
		    } else {
		        outRect.set(mRect);
		    }
		}
		
		public String toString() {
			return "x =" + x + "; y=" + y + "; a=" + a + "; va=" + va + "; vx=" + vx + "; vy=" + vy
					+ "; z=" + z + "; w=" + w + "; h=" + h + "; pvX=" + getPivotX()
					+ "; pvY=" + getPivotY();
		}

		private void reset(boolean anim) {

			a = randfrange(0, 360);
			va = randfrange(-30, 30);

			vx = randfrange(-20, 20) * z;
			vy = randfrange(-20, 20) * z;
			final float boardh = boardHeight;
			final float boardw = boardWidth;
			
			x = randfrange(w, boardw - w);
			y = randfrange(h, boardh - h);
			if(anim) {
				show();
			}
		}

		private void update(float dt, int width, int hight) {
			if (grabbed) {
				vx = (vx * 0.75f) + ((grabx - x) / dt) * 0.25f;
				x = grabx;
				vy = (vy * 0.75f) + ((graby - y) / dt) * 0.25f;
				y = graby;
			} else {
				x = (x + vx * dt);
				y = (y + vy * dt);
				a = (a + va * dt);
			}
		}

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouchEvent(MotionEvent e) {
			switch (e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				grabbed = true;
				grabx_offset = e.getRawX() - x;
				graby_offset = e.getRawY() - y;
				va = 0;
			case MotionEvent.ACTION_MOVE:
				grabx = e.getRawX() - grabx_offset;
				graby = e.getRawY() - graby_offset;
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				grabbed = false;
				float a = randsign() * clamp(mag(vx, vy) * 0.33f, 0, 1080f);
				va = randfrange(a * 0.5f, a);
				break;
			}
			mGestureListener.setCurrentView(Head.this);
			mGestureDetector.onTouchEvent(e);
			return true;
		}
	}
	
	private TimeAnimator mAnim;
	
	private int random(int small, int big) {
		return (int)(small + Math.random()*(big - small + 1));
	}
	
	private void reset() {
		removeAllViews();
		Main main = (Main) getContext();
		
		Iterator<Map.Entry<AppInfo,View>> iterator = main.mforceStopMap.entrySet().iterator();
		int size = main.mforceStopMap.size();
		while(iterator.hasNext()) {
			Map.Entry<AppInfo,View> entry = iterator.next();
			AppInfo info = entry.getKey();
			
			Head nv = new Head(getContext(), null);
			nv.z = ((float) random(0, size) / size);
			nv.z *= nv.z;
			BitmapDrawable drawable = info.appIcon;
			nv.w = drawable.getBitmap().getWidth();
			nv.h= drawable.getBitmap().getHeight();
			nv.setBackground(drawable);
			nv.boardWidth = getMeasuredWidth();
			nv.boardHeight = getMeasuredHeight();
			nv.reset(false);
			nv.x = (randfrange(nv.w, nv.boardWidth - nv.w));
			nv.y = (randfrange(nv.h, nv.boardHeight - nv.h));
			if(info.isLock) {
				nv.lock.setVisibility(View.VISIBLE);
			}else {
				nv.lock.setVisibility(View.GONE);
			}
			
			nv.setTag(info);
			main.mforceStopMap.put(info, nv);
			addView(nv);
			Log.i("peter", nv.toString());
		}

		if (mAnim != null) {
			mAnim.cancel();
		}
		mAnim = new TimeAnimator();
		mAnim.setTimeListener(new TimeAnimator.TimeListener() {

			@Override
			public void onTimeUpdate(TimeAnimator animation, long totalTime,
					long deltaTime) {

				for (int i = 0, count = getChildCount(); i < count; i++) {
					View v = getChildAt(i);
					if (!(v instanceof Head)) {
						continue;
					}
					Head nv = (Head) v;
					if(nv.killed) {
						continue;
					}
					nv.update(deltaTime / 1000f, getWidth(), getHeight());
					nv.setRotation(nv.a);
					
					float pvX = nv.getPivotX();
					float pvY = nv.getPivotY();
					float x = nv.x - pvX;
					float y = nv.y - pvY;
					nv.setX(x);
					nv.setY(y);
			        RectF rect = new RectF();
			        rect.top = 0;
			        rect.bottom = (float) nv.h; 
			        rect.left = 0; 
			        rect.right = (float) nv.w;  
			        rect.offset(nv.getX(), nv.getY());

			        if (nv.mRect == null) {
			        	nv.mRect = new Rect();
			        }
			        
			        rect.round(nv.mRect);
			        
			        if(nv.outEdges()) {
			        	if(nv.speedRemove()) {
			        		final AppInfo info = (AppInfo) nv.getTag();
							if(!info.isLock) {
								final Main main = (Main) getContext();
								final View head = nv;
								nv.killed = true;
								Main.forceStop = true;

								//explosion
								ExplosionField explosionField = new ExplosionField(main, new ExplodeParticleFactory());
								explosionField.explode(nv, new AnimatorListenerAdapter() {
									@Override
									public void onAnimationEnd(Animator animation) {
										super.onAnimationEnd(animation);
										main.mforceStopMap.remove(info);
										Board.this.removeView(head);
										main.showForceStopView(info);
									}
								});

							}else {
								nv.reset(true);
							}
			        	}else {
			        		nv.reset(true);
			        	}
			        }
				}
			}
		});
	}

	public void startAnimation() {
		stopAnimation();
		if (mAnim == null) {
			post(new Runnable() {
				public void run() {
					reset();
					startAnimation();
				}
			});
		} else {
			mAnim.start();
		}
	}

	public void stopAnimation() {
		if (mAnim != null)
			mAnim.cancel();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		stopAnimation();
	}

	@Override
	public boolean isOpaque() {
		return false;
	}
}