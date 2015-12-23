package com.peter.coolcleaner.factory;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.peter.coolcleaner.particle.Particle;


public abstract class ParticleFactory {
    public abstract Particle[][] generateParticles(Bitmap bitmap, Rect bound);
}
