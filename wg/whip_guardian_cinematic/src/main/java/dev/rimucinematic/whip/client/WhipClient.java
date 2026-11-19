package dev.rimucinematic.whip.client;

import cyou.rimuru.rimulib.mc1211.client.api.FilmApi;
import cyou.rimuru.rimulib.mc1211.client.api.FxApi;
import cyou.rimuru.rimulib.mc1211.client.api.MenuApi;
import cyou.rimuru.rimulib.mc1211.client.cinematic.Shot;
import cyou.rimuru.rimulib.mc1211.client.cinematic.Subject;
import dev.rimucinematic.whip.WhipCinematic;
import dev.rimucinematic.whip.entity.WhipGuardian;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client-only: one reusable postprocessing stage; cinema is local, server action independent. */
public final class WhipClient {
    public static final FxApi.Stage STAGE = FxApi.stage(WhipCinematic.ID);
    private static final FxApi.Layer LAYER = WhipClient::draw;
    private static UUID lastScene = null;
    private static Subject filmSubject = null;
    private static boolean reduceFlashes = false;
    private static boolean disableCamera = false;

    private WhipClient() { }

    @EventBusSubscriber(modid = WhipCinematic.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                FilmApi.enableKey();
                MenuApi.source(WhipCinematic.ID).order(2).credit("Whip Guardian", "custom cinematic addon");
                STAGE.add(LAYER);
            });
        }
        @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(WhipCinematic.GUARDIAN.get(), WhipGuardianRenderer::new);
        }
    }

    @EventBusSubscriber(modid = WhipCinematic.ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent public static void clientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                lastScene = null;
                if (filmSubject != null) FilmApi.stop(filmSubject);
                filmSubject = null;
                return;
            }
            WhipGuardian g = nearest();
            if (g == null) return;
            if (g.sceneAge() < 10 && !g.getUUID().equals(lastScene)
                && g.caster().equals(mc.player.getUUID())) {
                lastScene = g.getUUID();
                if (!disableCamera && FilmApi.automatic()) startFilm(mc, g);
            }
        }
    }

    private static WhipGuardian nearest() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        return mc.level.getEntitiesOfClass(WhipGuardian.class, new AABB(cam,cam).inflate(320),
                g -> g.sceneAge() >= 0 && g.sceneAge() < WhipGuardian.END).stream()
            .min((a,b) -> Double.compare(a.distanceToSqr(cam),b.distanceToSqr(cam)))
            .orElse(null);
    }

    private static void startFilm(Minecraft mc, WhipGuardian g) {
        Subject sub = FilmApi.follow(mc.player, g.position());
        filmSubject = sub;
        List<Shot> shots = List.of(
            FilmApi.cut(0, 60, (s, partial, k) -> FilmApi.pose(
                s.target().add(-44 + k*10, 14+k*8, -55+k*16), s.target().add(0,18,0), 62f)),
            FilmApi.sweep(60, 140, 15, (s, partial, k) -> FilmApi.pose(
                s.target().add(35*Math.cos(k*1.9), 22, 35*Math.sin(k*1.9)), s.target().add(0,17,0), 53f)),
            FilmApi.sweep(140, 204, 10, (s, partial, k) -> FilmApi.pose(
                s.target().add(55*Math.cos(k*2.5), 36+k*7, 55*Math.sin(k*2.5)), s.target().add(0,12,0), 58f)),
            FilmApi.sweep(204, 262, 12, (s, partial, k) -> FilmApi.pose(
                s.target().add(31+k*21, 7+k*3, -27-k*16), s.target().add(0,23-16*k,0), 44f)),
            FilmApi.cut(262, 295, (s, partial, k) -> FilmApi.pose(
                s.target().add(27+k*54, 19+k*22, 38+k*54), s.target().add(0,8,0), 58+k*13)),
            FilmApi.sweep(295, 345, 8, (s, partial, k) -> FilmApi.pose(
                s.target().add(85, 48+k*8, 90), s.target(), 63f))
        );
        FilmApi.play(FilmApi.film(sub, "ABYSSAL", "WORLD BREAKER", 0xFF62E4CF, shots));
    }

    private static void draw(FxApi.Frame frame) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null) return;
        WhipGuardian g=nearest();
        if(g==null) return;
        float age=g.sceneAge()+frame.partial();
        Vec3 camera=mc.gameRenderer.getMainCamera().getPosition();
        Vec3 centre=g.position().subtract(camera);
        frame.pass("whip_world", u -> {
            u.v3("Centre", centre);
            u.f1("SceneAge", age);
            u.f1("Quality", 1.0f);
        });
        var screen=frame.screen();
        // Distinct phases instead of continuous overexposure.
        float portal=ramp(age, 14, 46)*(1-ramp(age, 61, 82));
        float spin=ramp(age, 140, 153)*(1-ramp(age, 198, 214));
        float contact=(float)Math.exp(-Math.abs(age-WhipGuardian.IMPACT)*0.55);
        float aftermath=ramp(age, 264, 268)*(1-ramp(age, 310, 335));
        screen.bloom=Math.max(screen.bloom,0.22f + portal*.3f+spin*.23f+contact*.55f);
        screen.aberration=Math.max(screen.aberration,portal*.07f+spin*.10f+contact*.45f);
        screen.zoomBlur=Math.max(screen.zoomBlur,spin*.08f+contact*.22f);
        screen.shake=Math.max(screen.shake,spin*.045f+contact*(reduceFlashes ? .15f : .72f));
        screen.flash=Math.max(screen.flash, contact*(reduceFlashes?.08f:.68f));
        screen.vignette=Math.max(screen.vignette,.18f + aftermath*.18f);
    }

    private static float ramp(float t, float a, float b) {
        float x=Math.max(0,Math.min(1,(t-a)/(b-a)));
        return x*x*(3-2*x);
    }
}
