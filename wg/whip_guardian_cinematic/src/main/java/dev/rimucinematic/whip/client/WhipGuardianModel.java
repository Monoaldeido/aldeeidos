package dev.rimucinematic.whip.client;

import dev.rimucinematic.whip.WhipCinematic;
import dev.rimucinematic.whip.entity.WhipGuardian;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class WhipGuardianModel extends GeoModel<WhipGuardian> {
    @Override public ResourceLocation getModelResource(WhipGuardian entity) {
        return WhipCinematic.id("geo/whip_guardian.geo.json");
    }
    @Override public ResourceLocation getTextureResource(WhipGuardian entity) {
        return WhipCinematic.id("textures/entity/whip_guardian.png");
    }
    @Override public ResourceLocation getAnimationResource(WhipGuardian entity) {
        return WhipCinematic.id("animations/whip_guardian.animation.json");
    }
}
