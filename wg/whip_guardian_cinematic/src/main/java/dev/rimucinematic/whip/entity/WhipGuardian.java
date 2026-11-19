package dev.rimucinematic.whip.entity;

import dev.rimucinematic.whip.server.WhipServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.UUID;

/** Real world entity. All viewers see the model even if the caster skips the camera. */
public final class WhipGuardian extends Entity implements GeoEntity {
    public static final int AURA_START = 60;
    public static final int SPIN_START = 140; // exactly 4 s into aura_farm
    public static final int SPIN_STOP = 205;  // 7.25 s into aura_farm
    public static final int SLAM_START = 220;
    public static final int IMPACT = 263;     // ground_slam local 43 ticks = 2.15 s
    public static final int END = 350;

    private static final EntityDataAccessor<Long> ORIGIN_TICK =
        SynchedEntityData.defineId(WhipGuardian.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CASTER_MSB =
        SynchedEntityData.defineId(WhipGuardian.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CASTER_LSB =
        SynchedEntityData.defineId(WhipGuardian.class, EntityDataSerializers.LONG);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public WhipGuardian(EntityType<? extends WhipGuardian> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void configure(Vec3 position, UUID caster, long levelTick) {
        this.setPos(position.x, position.y, position.z);
        entityData.set(ORIGIN_TICK, levelTick);
        entityData.set(CASTER_MSB, caster.getMostSignificantBits());
        entityData.set(CASTER_LSB, caster.getLeastSignificantBits());
    }

    public int sceneAge() {
        long start = entityData.get(ORIGIN_TICK);
        return start < 0 ? -1 : (int)Math.max(0, level().getGameTime() - start);
    }

    public UUID caster() {
        return new UUID(entityData.get(CASTER_MSB), entityData.get(CASTER_LSB));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ORIGIN_TICK, -1L);
        builder.define(CASTER_MSB, 0L);
        builder.define(CASTER_LSB, 0L);
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) WhipServer.tick(this);
        if (sceneAge() >= END) discard();
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    // If a chunk is saved mid-scene, never reload as an orphaned entity whose
    // unset clock (-1) would prevent automatic expiry.
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OriginTick")) entityData.set(ORIGIN_TICK, tag.getLong("OriginTick"));
        if (tag.hasUUID("Caster")) {
            UUID uuid = tag.getUUID("Caster");
            entityData.set(CASTER_MSB, uuid.getMostSignificantBits());
            entityData.set(CASTER_LSB, uuid.getLeastSignificantBits());
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("OriginTick", entityData.get(ORIGIN_TICK));
        tag.putUUID("Caster", caster());
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "scene", 0, state -> {
            int age = sceneAge();
            if (age >= 0 && age < AURA_START)
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.whip_guardian.portal_arrival"));
            if (age >= AURA_START && age < SLAM_START)
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.whip_guardian.aura_farm"));
            if (age >= SLAM_START && age < SLAM_START + 60)
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.whip_guardian.ground_slam"));
            return PlayState.STOP;
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
}
