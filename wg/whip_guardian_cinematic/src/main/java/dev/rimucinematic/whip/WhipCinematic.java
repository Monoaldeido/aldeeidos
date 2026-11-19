package dev.rimucinematic.whip;

import cyou.rimuru.rimulib.mc1211.api.RimulibApi;
import cyou.rimuru.rimulib.mc1211.api.SkillApi;
import cyou.rimuru.rimulib.mc1211.skill.Skill;
import dev.rimucinematic.whip.entity.WhipGuardian;
import dev.rimucinematic.whip.server.WhipServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(WhipCinematic.ID)
public final class WhipCinematic {
    public static final String ID = "whip_cinematic";
    public static final String BASE_MOD_ID = "shooting_star_demo";
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);

    public static final DeferredHolder<EntityType<?>, EntityType<WhipGuardian>> GUARDIAN =
        ENTITIES.register("whip_guardian", () ->
            EntityType.Builder.<WhipGuardian>of(WhipGuardian::new, MobCategory.MISC)
                .sized(16.0F, 36.0F).clientTrackingRange(28).updateInterval(1)
                .build("whip_cinematic:whip_guardian"));

    public WhipCinematic(IEventBus bus) {
        ENTITIES.register(bus);
        RimulibApi.connect(ID, WhipCinematic.class, "1.3.0");
        // Rimulib 1.3.0: the actual REMOTE's skill set gets extended; no mixin or item replacement.
        SkillApi.whenRegistered(BASE_MOD_ID, set -> {
            // TSS 1.6.x exposes a single remote set. Name is deliberately not hardcoded.
            if (!set.hidden && set.skills.stream().anyMatch(s -> s instanceof Enum<?>)) {
                SkillApi.extend(set, GuardianSkill.values(), WhipServer::cast);
            }
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public enum GuardianSkill implements Skill {
        WORLD_BREAKER;
        @Override public String id() { return "world_breaker"; }
        @Override public String tier() { return "Abyssal"; }
        @Override public String title() { return "Whip Guardian — World Breaker"; }
        @Override public int color() { return 0xFF62E4CF; }
        @Override public int cooldown() { return 20 * 90; }
        @Override public int duration() { return 350; }
        @Override public int defaultKey() { return 74; } // GLFW J; change in Remote menu
    }
}
