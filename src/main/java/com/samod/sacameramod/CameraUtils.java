package com.samod.sacameramod;

public class CameraUtils {
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
