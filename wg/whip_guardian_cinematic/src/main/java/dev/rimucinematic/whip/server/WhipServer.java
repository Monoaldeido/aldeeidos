package dev.rimucinematic.whip.server;

import cyou.rimuru.rimulib.mc1211.api.AimApi;
import cyou.rimuru.rimulib.mc1211.skill.Tuning;
import dev.rimucinematic.whip.WhipCinematic;
import dev.rimucinematic.whip.entity.WhipGuardian;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.tags.BlockTags;

/** Server-authoritative gameplay. Rendering/film never controls the destruction tick. */
public final class WhipServer {
    private WhipServer() {}
    private static final int CRATER_RADIUS = 18;
    // Permission gating is intentional: the power remains cosmetic for non-operators.
    public static final boolean REQUIRE_OP_FOR_TERRAIN = true;
    public static final boolean DESTROY_TERRAIN = true;
    private static final int MAX_CHANGED_PER_TICK = 3800;

    public static boolean cast(ServerPlayer player, WhipCinematic.GuardianSkill skill, Tuning tuning) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        Vec3 target = AimApi.ground(player, 192.0, 32.0);
        if (target == null || !world.hasChunkAt(BlockPos.containing(target))) return false;
        WhipGuardian guardian = WhipCinematic.GUARDIAN.get().create(world);
        if (guardian == null) return false;
        guardian.configure(target, player.getUUID(), world.getGameTime());
        if (!world.addFreshEntity(guardian)) return false;
        world.playSound(null, BlockPos.containing(target), SoundEvents.END_PORTAL_SPAWN,
            SoundSource.PLAYERS, 2.0f, 0.55f);
        return true;
    }

    public static void tick(WhipGuardian scene) {
        if (!(scene.level() instanceof ServerLevel world)) return;
        int age = scene.sceneAge();
        Vec3 c = scene.position();
        if (age < 35 && age % 3 == 0)
            world.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 15, c.z, 65, 9, 12, 9, 0.12);
        if (age >= 35 && age < 220 && age % 5 == 0)
            world.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 11, c.z, 22, 11, 10, 11, 0.03);
        if (age >= WhipGuardian.SPIN_START && age < WhipGuardian.SPIN_STOP) {
            whirl(scene, age);
            if (age % 4 == 0)
                world.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 1, c.z, 56, 24, 4, 24, 0.04);
        }
        if (age == WhipGuardian.SLAM_START)
            world.playSound(null, BlockPos.containing(c), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.2f, 0.55f);
        if (age == WhipGuardian.IMPACT) {
            world.playSound(null, BlockPos.containing(c), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4, 0.5f);
            shock(scene);
        }
        if (age >= WhipGuardian.IMPACT && age < WhipGuardian.IMPACT + 14) {
            if (allowTerrain(scene)) carveRing(world, c, age - WhipGuardian.IMPACT);
            world.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y+2, c.z, 26, 22, 9, 22, 0.06);
        }
    }

    private static boolean allowTerrain(WhipGuardian scene) {
        if (!DESTROY_TERRAIN) return false;
        if (!(scene.level() instanceof ServerLevel world)) return false;
        if (!world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        ServerPlayer caster = world.getServer().getPlayerList().getPlayer(scene.caster());
        return caster != null && (!REQUIRE_OP_FOR_TERRAIN || caster.hasPermissions(2));
    }

    private static void whirl(WhipGuardian scene, int age) {
        ServerLevel world = (ServerLevel)scene.level();
        Vec3 c = scene.position();
        for (Entity e : world.getEntities(scene, new AABB(c, c).inflate(67),
            ent -> ent instanceof LivingEntity && !ent.getUUID().equals(scene.caster()))) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z;
            double r = Math.sqrt(dx*dx + dz*dz);
            if (r < 4 || r > 60) continue;
            double theta = Math.atan2(dz, dx);
            double swirl = age * 0.075;
            double best = 1e12, vx = 0, vz = 0;
            for (int i=0;i<5;i++) {
                double a = swirl + i * Math.PI*2/5;
                double tx = c.x + 28*Math.cos(a), tz = c.z+28*Math.sin(a);
                double ex = e.getX()-tx, ez=e.getZ()-tz;
                double d2=ex*ex+ez*ez;
                if(d2<best){best=d2;vx=ex;vz=ez;}
            }
            if (best > 12*12) continue;
            double length = Math.max(1,Math.sqrt(best));
            Vec3 push = new Vec3((vx / length - vz / length*.65)*.8,
                .35+(1-Math.min(1,length/12))*.6,
                (vz / length + vx / length*.65)*.8);
            e.setDeltaMovement(e.getDeltaMovement().add(push));
            e.hurtMarked=true;
        }
    }

    private static void shock(WhipGuardian scene) {
        ServerLevel world = (ServerLevel)scene.level();
        Vec3 c = scene.position();
        for (Entity e : world.getEntities(scene, new AABB(c,c).inflate(62),
            ent -> ent instanceof LivingEntity && !ent.getUUID().equals(scene.caster()))) {
            Vec3 away=e.position().subtract(c);
            double r=away.horizontalDistance();
            if(r>60)continue;
            e.setDeltaMovement(e.getDeltaMovement().add(new Vec3(away.x/(r+0.1)*3.5,1.3,away.z/(r+0.1)*3.5)));
            e.hurt(world.damageSources().magic(), 24f);
            e.hurtMarked=true;
        }
        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x,c.y+1,c.z,2,3,2,3,.05);
    }

    /** Annular carving: progresses outward from the impact instead of changing the whole chunk in one tick. */
    private static void carveRing(ServerLevel world, Vec3 at, int step) {
        // Each ring handled exactly once. Two block wide rings, 18-block maximum.
        final int inner=step*2, outer=Math.min(CRATER_RADIUS,inner+2);
        if (inner>=CRATER_RADIUS)return;
        final int cx=Mth.floor(at.x), cy=Mth.floor(at.y), cz=Mth.floor(at.z);
        int changed=0;
        for(int x=-outer;x<=outer;x++)for(int z=-outer;z<=outer;z++){
            double d=Math.sqrt(x*x+z*z);
            if(d<inner || d>=outer || d>CRATER_RADIUS)continue;
            int depth=Math.max(2,(int)(8*(1-d/CRATER_RADIUS)));
            for(int y=cy+8; y>=cy-depth; y--){
                if(changed>=MAX_CHANGED_PER_TICK)return;
                BlockPos p=new BlockPos(cx+x,y,cz+z);
                if(!world.hasChunkAt(p) || y<=world.getMinBuildHeight() || y>=world.getMaxBuildHeight())continue;
                BlockState old=world.getBlockState(p);
                if(old.isAir() || old.hasBlockEntity() || old.is(BlockTags.WITHER_IMMUNE)
                    || old.getDestroySpeed(world,p)<0)continue;
                if(world.setBlock(p,Blocks.AIR.defaultBlockState(),3))changed++;
            }
        }
    }
}
