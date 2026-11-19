package dev.rimucinematic.whip.client;

import dev.rimucinematic.whip.entity.WhipGuardian;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** At 11x the 2.84-block native model is roughly 31 blocks tall. */
public final class WhipGuardianRenderer extends GeoEntityRenderer<WhipGuardian> {
    public static final float SCALE = 11.0f;
    public WhipGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new WhipGuardianModel());
        withScale(SCALE);
    }
}
