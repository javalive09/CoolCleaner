package com.peter.coolcleaner;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;


import com.peter.coolcleaner.factory.ParticleFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class ExplosionField extends View{
    private static final String TAG = "ExplosionField";
    private ArrayList<ExplosionAnimator> explosionAnimators;
    private HashMap<View,ExplosionAnimator> explosionAnimatorsMap;
    private OnClickListener onClickListener;
    private ParticleFactory mParticleFactory;
    
    public ExplosionField(Context context,ParticleFactory particleFactory) {
        super(context);
        init(particleFactory);
    }

    public ExplosionField(Context context, AttributeSet attrs,ParticleFactory particleFactory) {
        super(context, attrs);
        init(particleFactory);
    }

    private void init(ParticleFactory particleFactory) {
        explosionAnimators = new ArrayList<ExplosionAnimator>();
        explosionAnimatorsMap = new HashMap<View,ExplosionAnimator>();
        mParticleFactory = particleFactory;
        attach2Activity((Activity) getContext());
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (ExplosionAnimator animator : explosionAnimators) {
            animator.draw(canvas);
        }
    }

    /**
     * 爆破
     * @param view 使得该view爆破
     */
    public void explode(final View view , AnimatorListenerAdapter listener) {
        final Rect rect = new Rect();
        view.getGlobalVisibleRect(rect); //得到view相对于整个屏幕的坐标
        explode(view, rect, listener);
    }

    private void explode(final View view, Rect rect, final AnimatorListenerAdapter listener) {
        final ExplosionAnimator animator = new ExplosionAnimator(this, Utils.createBitmapFromView(view), rect,mParticleFactory);
        explosionAnimators.add(animator);
        explosionAnimatorsMap.put(view, animator);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setAlpha(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //动画结束时从动画集中移除
                explosionAnimators.remove(animation);
                explosionAnimatorsMap.remove(view);
                if(listener != null) {
                    listener.onAnimationEnd(animation);
                }
            }
        });

        animator.start();
    }
    
    /**
     * 给Activity加上全屏覆盖的ExplosionField
     */
    private void attach2Activity(Activity activity) {
        ViewGroup rootView = (ViewGroup) activity.findViewById(Window.ID_ANDROID_CONTENT);

        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rootView.addView(this, lp);
    }
    

}
