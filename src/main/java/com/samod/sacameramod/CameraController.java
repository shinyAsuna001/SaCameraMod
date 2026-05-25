package com.samod.sacameramod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class CameraController {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final CameraController INSTANCE = new CameraController();

    private final List<CameraEffect> effects = new ArrayList<>();
    private boolean active = false;
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    private double prevOffsetX;
    private double prevOffsetY;
    private double prevOffsetZ;
    // accumulated local-space offsets (left, up, forward) from relative MoveEffects
    // separate accumulators for pitch-agnostic and pitch-aware moves
    private double relNoPitchX;
    private double relNoPitchY;
    private double relNoPitchZ;
    private double prevRelNoPitchX;
    private double prevRelNoPitchY;
    private double prevRelNoPitchZ;
    private double relPitchX;
    private double relPitchY;
    private double relPitchZ;
    private double prevRelPitchX;
    private double prevRelPitchY;
    private double prevRelPitchZ;
    private float yawOffset;
    private float pitchOffset;
    private float rollOffset;
    private float prevYawOffset;
    private float prevPitchOffset;
    private float prevRollOffset;
    private CameraController() {}

    public static CameraController get() {
        return INSTANCE;
    }

    public void addEffect(CameraEffect effect) {
        this.effects.add(effect);
        this.active = true;
        LOGGER.info("Added camera effect: {} (totalTicks={})", effect.getClass().getSimpleName(), effect.totalTicks);
    }

    public void activate(Vector3d startPos, float startYaw, float startPitch) {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
        this.effects.clear();
    }

    public boolean isActive() {
        return active;
    }

    public void tick() {
        prevOffsetX = offsetX;
        prevOffsetY = offsetY;
        prevOffsetZ = offsetZ;
        prevRelNoPitchX = relNoPitchX;
        prevRelNoPitchY = relNoPitchY;
        prevRelNoPitchZ = relNoPitchZ;
        prevRelPitchX = relPitchX;
        prevRelPitchY = relPitchY;
        prevRelPitchZ = relPitchZ;
        prevYawOffset = yawOffset;
        prevPitchOffset = pitchOffset;
        prevRollOffset = rollOffset;

        offsetX = 0;
        offsetY = 0;
        offsetZ = 0;
        relNoPitchX = 0;
        relNoPitchY = 0;
        relNoPitchZ = 0;
        relPitchX = 0;
        relPitchY = 0;
        relPitchZ = 0;
        yawOffset = 0;
        pitchOffset = 0;
        rollOffset = 0;

        Iterator<CameraEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            CameraEffect effect = iterator.next();
            effect.tick();
            if (effect.isFinished()) {
                iterator.remove();
            } else {
                if (effect instanceof MoveEffect && ((MoveEffect) effect).relative) {
                    MoveEffect me = (MoveEffect) effect;
                    if (me.usePitch) {
                        relPitchX += effect.getOffsetX();
                        relPitchY += effect.getOffsetY();
                        relPitchZ += effect.getOffsetZ();
                    } else {
                        relNoPitchX += effect.getOffsetX();
                        relNoPitchY += effect.getOffsetY();
                        relNoPitchZ += effect.getOffsetZ();
                    }
                } else if (effect instanceof CinematicEffect) {
                    // CinematicEffect uses absolute world-space path targets and direct yaw/pitch overrides.
                } else {
                    offsetX += effect.getOffsetX();
                    offsetY += effect.getOffsetY();
                    offsetZ += effect.getOffsetZ();
                }
                if (!(effect instanceof CinematicEffect)) {
                    yawOffset += effect.getYawOffset();
                    pitchOffset += effect.getPitchOffset();
                    rollOffset += effect.getRollOffset();
                }
            }
        }
        if (effects.isEmpty()) {
            active = false;
        }
    }

    public void applyToCamera(EntityViewRenderEvent.CameraSetup event) {
        ActiveRenderInfo info = event.getInfo();
        if (info == null) {
            return;
        }

        float partialTicks = Minecraft.getInstance().getFrameTime();
        partialTicks = Math.max(0f, Math.min(1f, partialTicks));

        float cinematicYaw = Float.NaN;
        float cinematicPitch = Float.NaN;
        Vector3d cameraPosition = info.getPosition();
        double worldTargetX = 0;
        double worldTargetY = 0;
        double worldTargetZ = 0;
        if (cameraPosition != null) {
            for (CameraEffect effect : effects) {
                if (effect instanceof CinematicEffect) {
                    CinematicEffect ce = (CinematicEffect) effect;
                    worldTargetX += ce.getRenderTargetWorldX(partialTicks) - cameraPosition.x;
                    worldTargetY += ce.getRenderTargetWorldY(partialTicks) - cameraPosition.y;
                    worldTargetZ += ce.getRenderTargetWorldZ(partialTicks) - cameraPosition.z;
                    cinematicYaw = ce.getRenderYaw(partialTicks);
                    cinematicPitch = ce.getRenderPitch(partialTicks);
                }
            }
        }

        // apply world-space offsets directly
        double interpOffsetX = lerp(prevOffsetX, offsetX, partialTicks);
        double interpOffsetY = lerp(prevOffsetY, offsetY, partialTicks);
        double interpOffsetZ = lerp(prevOffsetZ, offsetZ, partialTicks);
        if (interpOffsetX != 0 || interpOffsetY != 0 || interpOffsetZ != 0) {
            translateView(info, interpOffsetX, interpOffsetY, interpOffsetZ);
        }

        if (worldTargetX != 0 || worldTargetY != 0 || worldTargetZ != 0) {
            translateView(info, worldTargetX, worldTargetY, worldTargetZ);
        }

        double interpRelNoPitchX = lerp(prevRelNoPitchX, relNoPitchX, partialTicks);
        double interpRelNoPitchY = lerp(prevRelNoPitchY, relNoPitchY, partialTicks);
        double interpRelNoPitchZ = lerp(prevRelNoPitchZ, relNoPitchZ, partialTicks);
        double interpRelPitchX = lerp(prevRelPitchX, relPitchX, partialTicks);
        double interpRelPitchY = lerp(prevRelPitchY, relPitchY, partialTicks);
        double interpRelPitchZ = lerp(prevRelPitchZ, relPitchZ, partialTicks);
        double interpYawOffset = lerp(prevYawOffset, yawOffset, partialTicks);
        double interpPitchOffset = lerp(prevPitchOffset, pitchOffset, partialTicks);
        double interpRollOffset = lerp(prevRollOffset, rollOffset, partialTicks);

        // convert local (left, up, forward) accumulated offsets into world-space
        // first handle yaw-only (no pitch) moves
        if (interpRelNoPitchX != 0 || interpRelNoPitchY != 0 || interpRelNoPitchZ != 0) {
            try {
                float yawDeg = info.getYRot();
                double yawRad = Math.toRadians(yawDeg);
                double forwardX = -Math.sin(yawRad);
                double forwardZ = Math.cos(yawRad);
                double leftX = -forwardZ;
                double leftZ = forwardX;

                double worldX = leftX * interpRelNoPitchX + 0.0 * interpRelNoPitchY + forwardX * interpRelNoPitchZ;
                double worldY = interpRelNoPitchY;
                double worldZ = leftZ * interpRelNoPitchX + 0.0 * interpRelNoPitchY + forwardZ * interpRelNoPitchZ;
                translateView(info, worldX, worldY, worldZ);
            } catch (Throwable t) {
                // ignore conversion errors
            }
        }

        // then handle pitch-aware moves (consider pitch for forward/back) and use view-up
        if (interpRelPitchX != 0 || interpRelPitchY != 0 || interpRelPitchZ != 0) {
            try {
                float yawDeg = info.getYRot();
                float pitchDeg = info.getXRot();
                double yawRad = Math.toRadians(yawDeg);
                double pitchRad = Math.toRadians(pitchDeg);
                // forward vector with pitch
                double forwardX = -Math.cos(pitchRad) * Math.sin(yawRad);
                double forwardY = -Math.sin(pitchRad);
                double forwardZ = Math.cos(pitchRad) * Math.cos(yawRad);
                // compute right = forward x worldUp (then normalize)
                double rx = forwardY * 0.0 - forwardZ * 1.0; // = -forwardZ
                double ry = forwardZ * 0.0 - forwardX * 0.0; // = 0
                double rz = forwardX * 1.0 - forwardY * 0.0; // = forwardX
                double rlen = Math.sqrt(rx * rx + ry * ry + rz * rz);
                if (rlen > 1e-6) {
                    rx /= rlen; ry /= rlen; rz /= rlen;
                }
                // view-up = right cross forward
                double upX = ry * forwardZ - rz * forwardY;
                double upY = rz * forwardX - rx * forwardZ;
                double upZ = rx * forwardY - ry * forwardX;
                // left = -right
                double leftX = -rx;
                double leftY = -ry;
                double leftZ = -rz;

                double worldX = leftX * interpRelPitchX + upX * interpRelPitchY + forwardX * interpRelPitchZ;
                double worldY = leftY * interpRelPitchX + upY * interpRelPitchY + forwardY * interpRelPitchZ;
                double worldZ = leftZ * interpRelPitchX + upZ * interpRelPitchY + forwardZ * interpRelPitchZ;
                translateView(info, worldX, worldY, worldZ);
            } catch (Throwable t) {
                // ignore conversion errors
            }
        }

        if (!Float.isNaN(cinematicYaw)) {
            event.setYaw(cinematicYaw);
        } else if (interpYawOffset != 0) {
            event.setYaw(event.getYaw() + (float) interpYawOffset);
        }
        if (!Float.isNaN(cinematicPitch)) {
            event.setPitch(cinematicPitch);
        } else if (interpPitchOffset != 0) {
            event.setPitch(event.getPitch() + (float) interpPitchOffset);
        }
        if (interpRollOffset != 0) {
            event.setRoll(event.getRoll() + (float) interpRollOffset);
        }
    }

    private static double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void translateView(ActiveRenderInfo info, double x, double y, double z) {
        try {
            Vector3d position = info.getPosition();
            if (position == null) {
                return;
            }
            Vector3d next = position.add(x, y, z);
            try {
                if (setInfoPosition(info, next)) {
                    return;
                }
            } catch (Throwable ignored) {
                // fallback to field access
            }

            Field positionField = findFieldByType(ActiveRenderInfo.class, Vector3d.class);
            if (positionField != null) {
                positionField.setAccessible(true);
                positionField.set(info, next);
                updateCameraBlockPosition(info, next);
            }
        } catch (ReflectiveOperationException e) {
            // Ignore; if the field or method is unavailable in this runtime, no camera translation is applied.
        }
    }

    private static boolean setInfoPosition(ActiveRenderInfo info, Vector3d next) throws ReflectiveOperationException {
        Method setPosition = findSetPositionMethod();
        if (setPosition != null) {
            if (setPosition.getParameterCount() == 1) {
                setPosition.invoke(info, next);
            } else {
                setPosition.invoke(info, next.x, next.y, next.z);
            }
            return true;
        }
        return false;
    }

    private static Method findSetPositionMethod() {
        Method setPosition = getMethod(ActiveRenderInfo.class, "setPosition", Vector3d.class);
        if (setPosition != null) {
            return setPosition;
        }
        setPosition = getMethod(ActiveRenderInfo.class, "setPosition", double.class, double.class, double.class);
        if (setPosition != null) {
            return setPosition;
        }
        Class<?> clazz = ActiveRenderInfo.class;
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getReturnType() == void.class) {
                    Class<?>[] parameters = method.getParameterTypes();
                    if (parameters.length == 1 && parameters[0] == Vector3d.class) {
                        method.setAccessible(true);
                        return method;
                    }
                    if (parameters.length == 3 && parameters[0] == double.class && parameters[1] == double.class && parameters[2] == double.class) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static Field findFieldByType(Class<?> clazz, Class<?> type) {
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (type.isAssignableFrom(field.getType())) {
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static void updateCameraBlockPosition(ActiveRenderInfo info, Vector3d next) {
        try {
            Field blockPositionField = findFieldByType(ActiveRenderInfo.class, BlockPos.class);
            if (blockPositionField == null) {
                return;
            }
            blockPositionField.setAccessible(true);
            Object blockPosition = blockPositionField.get(info);
            if (blockPosition == null) {
                return;
            }
            Method setMethod = null;
            for (Method method : blockPosition.getClass().getMethods()) {
                if (!method.getName().equals("set")) {
                    continue;
                }
                if (method.getParameterCount() == 3) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params[0] == double.class && params[1] == double.class && params[2] == double.class) {
                        setMethod = method;
                        break;
                    }
                    if (params[0] == int.class && params[1] == int.class && params[2] == int.class) {
                        setMethod = method;
                        break;
                    }
                }
            }
            if (setMethod != null) {
                if (setMethod.getParameterTypes()[0] == double.class) {
                    setMethod.invoke(blockPosition, next.x, next.y, next.z);
                } else {
                    setMethod.invoke(blockPosition, (int)Math.floor(next.x), (int)Math.floor(next.y), (int)Math.floor(next.z));
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Vector3d getViewPosition(ActiveRenderInfo info) {
        try {
            return info.getPosition();
        } catch (Throwable t) {
            try {
                Field positionField = findFieldByType(ActiveRenderInfo.class, Vector3d.class);
                if (positionField != null) {
                    positionField.setAccessible(true);
                    return (Vector3d) positionField.get(info);
                }
            } catch (ReflectiveOperationException e) {
                // fall through
            }
            return null;
        }
    }

    private static Method getMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        while (clazz != null) {
            try {
                Method method = clazz.getDeclaredMethod(name, paramTypes);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    public boolean hasActiveEffects() {
        return !effects.isEmpty();
    }

    public void startCinematic(int totalDuration, String payload) {
        try {
            List<CinematicKey> keys = new ArrayList<>();
            String[] parts = payload.split(";");
            for (String p : parts) {
                String s = p.trim();
                if (s.isEmpty()) continue;
                String seq = s;
                boolean world = false;
                if (s.startsWith("world:")) {
                    world = true;
                    seq = s.substring(6);
                }
                String[] vals = seq.split(",");
                if (world) {
                    if (vals.length < 6) continue;
                    double x = Double.parseDouble(vals[0]);
                    double y = Double.parseDouble(vals[1]);
                    double z = Double.parseDouble(vals[2]);
                    double lx = Double.parseDouble(vals[3]);
                    double ly = Double.parseDouble(vals[4]);
                    double lz = Double.parseDouble(vals[5]);
                    Ease ease = Ease.LINEAR;
                    if (vals.length >= 7) {
                        ease = parseEase(vals[6]);
                    }
                    keys.add(new CinematicKey(x, y, z, lx, ly, lz, ease));
                } else {
                    if (vals.length < 5) continue;
                    double x = Double.parseDouble(vals[0]);
                    double y = Double.parseDouble(vals[1]);
                    double z = Double.parseDouble(vals[2]);
                    float yaw = Float.parseFloat(vals[3]);
                    float pitch = Float.parseFloat(vals[4]);
                    Ease ease = Ease.LINEAR;
                    if (vals.length >= 6) {
                        ease = parseEase(vals[5]);
                    }
                    keys.add(new CinematicKey(x, y, z, yaw, pitch, ease));
                }
            }
            if (!keys.isEmpty()) {
                this.effects.clear();
                this.effects.add(new CinematicEffect(keys, totalDuration));
                this.active = true;
            }
        } catch (Throwable t) {
            // ignore parse errors
        }
    }

    private static Ease parseEase(String token) {
        if (token == null) {
            return Ease.LINEAR;
        }
        switch (token.trim().toLowerCase()) {
            case "easein": return Ease.EASE_IN;
            case "easeout": return Ease.EASE_OUT;
            case "easeinout": return Ease.EASE_IN_OUT;
            default: return Ease.LINEAR;
        }
    }

    public abstract static class CameraEffect {
        protected final int fadeIn;
        protected final int hold;
        protected final int fadeOut;
        protected final int totalTicks;
        protected int age;

        protected CameraEffect(int fadeIn, int hold, int fadeOut) {
            this.fadeIn = fadeIn;
            this.hold = hold;
            this.fadeOut = fadeOut;
            this.totalTicks = Math.max(1, fadeIn + hold + fadeOut);
        }

        public void tick() {
            age = Math.min(totalTicks, age + 1);
        }

        public boolean isFinished() {
            return age >= totalTicks;
        }

        protected float getWeight() {
            if (totalTicks == 0) {
                return 1f;
            }
            if (age <= fadeIn) {
                return fadeIn == 0 ? 1f : (float) age / fadeIn;
            }
            if (age <= fadeIn + hold) {
                return 1f;
            }
            int fadeAge = age - fadeIn - hold;
            return fadeOut == 0 ? 1f : 1f - (float) fadeAge / fadeOut;
        }

        public double getOffsetX() { return 0; }
        public double getOffsetY() { return 0; }
        public double getOffsetZ() { return 0; }
        public float getYawOffset() { return 0; }
        public float getPitchOffset() { return 0; }
        public float getRollOffset() { return 0; }
    }

    public static class ShakeEffect extends CameraEffect {
        private final boolean rotationed;
        private final float strength;
        private final Random random = new Random();

        public ShakeEffect(boolean rotationed, float strength, int duration) {
            super(0, duration, 0);
            this.rotationed = rotationed;
            this.strength = strength * 0.5f; // reduce global shake strength
        }

        @Override
        public double getOffsetX() {
            if (rotationed) return 0;
            return (random.nextDouble() - 0.5) * 2.0 * strength * getWeight();
        }

        @Override
        public double getOffsetY() {
            if (rotationed) return 0;
            return (random.nextDouble() - 0.5) * 2.0 * strength * getWeight();
        }

        @Override
        public double getOffsetZ() {
            if (rotationed) return 0;
            return (random.nextDouble() - 0.5) * 2.0 * strength * getWeight();
        }

        @Override
        public float getYawOffset() {
            if (!rotationed) return 0;
            return (random.nextFloat() - 0.5f) * 2f * strength * getWeight();
        }

        @Override
        public float getPitchOffset() {
            if (!rotationed) return 0;
            return (random.nextFloat() - 0.5f) * 2f * strength * getWeight();
        }
    }

    public static class MoveEffect extends CameraEffect {
        private final float dx;
        private final float dy;
        private final float dz;
        public final boolean relative;
        public final boolean usePitch;

        public MoveEffect(float dx, float dy, float dz, boolean relative, boolean usePitch, int fadeIn, int hold, int fadeOut) {
            super(fadeIn, hold, fadeOut);
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.relative = relative;
            this.usePitch = usePitch;
        }

        @Override
        public double getOffsetX() {
            return dx * getWeight();
        }

        @Override
        public double getOffsetY() {
            return dy * getWeight();
        }

        @Override
        public double getOffsetZ() {
            return dz * getWeight();
        }
    }

    // Cinematic player: supports per-keyframe world-space and easing
    public enum Ease { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

    public static class CinematicKey {
        public final double x, y, z;
        public final float targetYaw;
        public final float targetPitch;
        public final boolean hasLookTarget;
        public final double lookX, lookY, lookZ;
        public final Ease ease;

        public CinematicKey(double x, double y, double z, double lookX, double lookY, double lookZ, Ease ease) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.hasLookTarget = true;
            this.lookX = lookX;
            this.lookY = lookY;
            this.lookZ = lookZ;
            this.targetYaw = computeYaw(x, y, z, lookX, lookY, lookZ);
            this.targetPitch = computePitch(x, y, z, lookX, lookY, lookZ);
            this.ease = ease;
        }

        public CinematicKey(double x, double y, double z, float yaw, float pitch, Ease ease) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.hasLookTarget = false;
            this.lookX = 0;
            this.lookY = 0;
            this.lookZ = 0;
            this.targetYaw = yaw;
            this.targetPitch = pitch;
            this.ease = ease;
        }

        private static float computeYaw(double x, double y, double z, double lookX, double lookY, double lookZ) {
            Vector3d lookDir = new Vector3d(lookX - x, lookY - y, lookZ - z);
            if (lookDir.equals(Vector3d.ZERO)) {
                return 0f;
            }
            lookDir = lookDir.normalize();
            return (float) Math.toDegrees(Math.atan2(-lookDir.x, lookDir.z));
        }

        private static float computePitch(double x, double y, double z, double lookX, double lookY, double lookZ) {
            Vector3d lookDir = new Vector3d(lookX - x, lookY - y, lookZ - z);
            if (lookDir.equals(Vector3d.ZERO)) {
                return 0f;
            }
            lookDir = lookDir.normalize();
            return (float) Math.toDegrees(Math.asin(-lookDir.y));
        }
    }

    public static class CinematicEffect extends CameraEffect {
        private final List<CinematicKey> keys;
        private final int[] segmentDurations;
        private final int segmentCount;
        private int currentSegment = 0;
        private int tickInSegment = 0;
        private double curWorldX;
        private double curWorldY;
        private double curWorldZ;
        private double prevCurWorldX;
        private double prevCurWorldY;
        private double prevCurWorldZ;
        private float curYaw;
        private float curPitch;
        private float prevCurYaw;
        private float prevCurPitch;

        public CinematicEffect(List<CinematicKey> keys, int totalDuration) {
            super(0, Math.max(1, totalDuration), 0);
            this.keys = keys;
            this.segmentCount = Math.max(0, keys.size() - 1);
            this.segmentDurations = new int[this.segmentCount];
            calculateSegmentDurations();
            if (!keys.isEmpty()) {
                this.curWorldX = keys.get(0).x;
                this.curWorldY = keys.get(0).y;
                this.curWorldZ = keys.get(0).z;
                this.curYaw = keys.get(0).targetYaw;
                this.curPitch = keys.get(0).targetPitch;
            } else {
                this.curWorldX = 0;
                this.curWorldY = 0;
                this.curWorldZ = 0;
                this.curYaw = 0;
                this.curPitch = 0;
            }
            this.prevCurWorldX = curWorldX;
            this.prevCurWorldY = curWorldY;
            this.prevCurWorldZ = curWorldZ;
            this.prevCurYaw = curYaw;
            this.prevCurPitch = curPitch;
        }

        private void calculateSegmentDurations() {
            if (segmentCount == 0) {
                return;
            }
            double totalLength = 0;
            double[] lengths = new double[segmentCount];
            for (int i = 0; i < segmentCount; i++) {
                CinematicKey from = keys.get(i);
                CinematicKey to = keys.get(i + 1);
                double dx = to.x - from.x;
                double dy = to.y - from.y;
                double dz = to.z - from.z;
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (length < 1e-3) {
                    length = 1.0;
                }
                lengths[i] = length;
                totalLength += length;
            }
            if (totalLength <= 0) {
                for (int i = 0; i < segmentDurations.length; i++) {
                    segmentDurations[i] = Math.max(1, totalTicks / segmentDurations.length);
                }
                return;
            }
            int assigned = 0;
            for (int i = 0; i < segmentDurations.length; i++) {
                if (i == segmentDurations.length - 1) {
                    segmentDurations[i] = Math.max(1, totalTicks - assigned);
                } else {
                    segmentDurations[i] = Math.max(1, (int) Math.round((lengths[i] / totalLength) * totalTicks));
                    assigned += segmentDurations[i];
                }
            }
            if (assigned < totalTicks) {
                segmentDurations[segmentDurations.length - 1] += totalTicks - assigned;
            }
        }

        @Override
        public void tick() {
            snapshotState();
            if (keys.isEmpty()) {
                age = totalTicks;
                return;
            }
            if (segmentCount == 0) {
                // Single key: stay at the start position for the full duration.
                curWorldX = keys.get(0).x;
                curWorldY = keys.get(0).y;
                curWorldZ = keys.get(0).z;
                curYaw = keys.get(0).targetYaw;
                curPitch = keys.get(0).targetPitch;
                age = Math.min(totalTicks, age + 1);
                return;
            }
            if (currentSegment >= segmentCount) {
                age = totalTicks;
                return;
            }
            CinematicKey from = keys.get(currentSegment);
            CinematicKey target = keys.get(currentSegment + 1);
            int dur = Math.max(1, segmentDurations[currentSegment]);
            float t = dur <= 1 ? 1f : (float) tickInSegment / (dur - 1);
            t = Math.max(0f, Math.min(1f, t));
            t = applyEase(target.ease, t);

            curWorldX = lerp(from.x, target.x, t);
            curWorldY = lerp(from.y, target.y, t);
            curWorldZ = lerp(from.z, target.z, t);
            curYaw = lerpAngle(from.targetYaw, target.targetYaw, t);
            curPitch = lerp(from.targetPitch, target.targetPitch, t);

            tickInSegment++;
            age = Math.min(totalTicks, age + 1);
            if (tickInSegment >= dur) {
                currentSegment++;
                tickInSegment = 0;
            }
        }

        private void snapshotState() {
            prevCurWorldX = curWorldX;
            prevCurWorldY = curWorldY;
            prevCurWorldZ = curWorldZ;
            prevCurYaw = curYaw;
            prevCurPitch = curPitch;
        }

        public double getRenderTargetWorldX(float partial) {
            return lerp(prevCurWorldX, curWorldX, partial);
        }

        public double getRenderTargetWorldY(float partial) {
            return lerp(prevCurWorldY, curWorldY, partial);
        }

        public double getRenderTargetWorldZ(float partial) {
            return lerp(prevCurWorldZ, curWorldZ, partial);
        }

        public float getRenderYaw(float partial) {
            return lerpAngle(prevCurYaw, curYaw, partial);
        }

        public float getRenderPitch(float partial) {
            return lerp(prevCurPitch, curPitch, partial);
        }

        @Override
        public boolean isFinished() {
            if (keys.size() <= 1) {
                return age >= totalTicks;
            }
            return currentSegment >= segmentCount;
        }

        @Override
        public double getOffsetX() { return 0; }
        @Override
        public double getOffsetY() { return 0; }
        @Override
        public double getOffsetZ() { return 0; }
        @Override
        public float getYawOffset() { return 0; }
        @Override
        public float getPitchOffset() { return 0; }

        public boolean isCurrentSegmentWorld() {
            return true;
        }

        public double getTargetWorldX() { return curWorldX; }
        public double getTargetWorldY() { return curWorldY; }
        public double getTargetWorldZ() { return curWorldZ; }

        private static float applyEase(Ease e, float t) {
            if (t <= 0) return 0f;
            if (t >= 1) return 1f;
            switch (e) {
                case EASE_IN: return t * t;
                case EASE_OUT: return 1f - (1f - t) * (1f - t);
                case EASE_IN_OUT:
                    if (t < 0.5f) return 2f * t * t;
                    return 1f - 2f * (1f - t) * (1f - t);
                default: return t;
            }
        }

        private static double lerp(double a, double b, float t) {
            return a + (b - a) * t;
        }

        private static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }

        private static float lerpAngle(float a, float b, float t) {
            float diff = wrapDegrees(b - a);
            return a + diff * t;
        }

        private static float wrapDegrees(float value) {
            value %= 360f;
            if (value >= 180f) value -= 360f;
            if (value < -180f) value += 360f;
            return value;
        }
    }

    public static class RotateEffect extends CameraEffect {
        public enum Axis { X, Y, Z }

        private final Axis axis;
        private final float angle;

        public RotateEffect(Axis axis, float angle, int fadeIn, int hold, int fadeOut) {
            super(fadeIn, hold, fadeOut);
            this.axis = axis;
            this.angle = angle;
        }

        @Override
        public float getYawOffset() {
            if (axis == Axis.Y) {
                return angle * getWeight();
            }
            return 0;
        }

        @Override
        public float getPitchOffset() {
            if (axis == Axis.X) {
                return angle * getWeight();
            }
            return 0;
        }
        @Override
        public float getRollOffset() {
            if (axis == Axis.Z) {
                return angle * getWeight();
            }
            return 0;
        }
    }
}
