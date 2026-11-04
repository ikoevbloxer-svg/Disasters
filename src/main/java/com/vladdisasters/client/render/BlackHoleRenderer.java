package com.vladdisasters.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vladdisasters.VladDisasters;
import com.vladdisasters.entity.BlackHoleEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Чёрная сфера + светящийся аккреционный диск. */
public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
    private static final ResourceLocation WHITE = VladDisasters.id("textures/entity/white.png");
    private static final int LAT = 14, LON = 24;

    public BlackHoleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    @Override
    public void render(BlackHoleEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
        float r = e.getRadius();
        float t = e.tickCount + pt;
        pose.pushPose();
        pose.translate(0, e.getBbHeight() / 2F, 0);

        // Чёрная сфера
        VertexConsumer vc = buf.getBuffer(RenderType.entitySolid(WHITE));
        PoseStack.Pose p = pose.last();
        for (int i = 0; i < LAT; i++) {
            float th0 = Mth.PI * i / LAT, th1 = Mth.PI * (i + 1) / LAT;
            for (int j = 0; j < LON; j++) {
                float ph0 = 2 * Mth.PI * j / LON, ph1 = 2 * Mth.PI * (j + 1) / LON;
                sphereVertex(vc, p, r, th0, ph0);
                sphereVertex(vc, p, r, th1, ph0);
                sphereVertex(vc, p, r, th1, ph1);
                sphereVertex(vc, p, r, th0, ph1);
            }
        }

        // Аккреционный диск (светится, вращается)
        pose.mulPose(Axis.XP.rotationDegrees(18F));
        pose.mulPose(Axis.YP.rotationDegrees(t * 3F));
        VertexConsumer glow = buf.getBuffer(RenderType.lightning());
        PoseStack.Pose gp = pose.last();
        int seg = 48;
        float in = r * 1.25F, mid = r * 2.1F, out = r * 3.4F;
        for (int k = 0; k < seg; k++) {
            float a0 = 2 * Mth.PI * k / seg, a1 = 2 * Mth.PI * (k + 1) / seg;
            float flick = 0.75F + 0.25F * Mth.sin(t * 0.3F + k);
            ring(glow, gp, a0, a1, in, mid, 255, 170, 60, (int) (220 * flick), 255, 90, 20, (int) (140 * flick));
            ring(glow, gp, a0, a1, mid, out, 255, 90, 20, (int) (140 * flick), 120, 30, 160, 0);
        }
        pose.popPose();
        super.render(e, yaw, pt, pose, buf, light);
    }

    private static void sphereVertex(VertexConsumer vc, PoseStack.Pose p, float r, float th, float ph) {
        float x = Mth.sin(th) * Mth.cos(ph), y = Mth.cos(th), z = Mth.sin(th) * Mth.sin(ph);
        vc.addVertex(p, x * r, y * r, z * r)
                .setColor(0, 0, 0, 255)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(p, x, y, z);
    }

    private static void ring(VertexConsumer vc, PoseStack.Pose p, float a0, float a1, float r0, float r1,
                             int ir, int ig, int ib, int ia, int or, int og, int ob, int oa) {
        float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
        // верхняя сторона
        vc.addVertex(p, c0 * r0, 0, s0 * r0).setColor(ir, ig, ib, ia);
        vc.addVertex(p, c0 * r1, 0, s0 * r1).setColor(or, og, ob, oa);
        vc.addVertex(p, c1 * r1, 0, s1 * r1).setColor(or, og, ob, oa);
        vc.addVertex(p, c1 * r0, 0, s1 * r0).setColor(ir, ig, ib, ia);
        // нижняя сторона
        vc.addVertex(p, c1 * r0, 0, s1 * r0).setColor(ir, ig, ib, ia);
        vc.addVertex(p, c1 * r1, 0, s1 * r1).setColor(or, og, ob, oa);
        vc.addVertex(p, c0 * r1, 0, s0 * r1).setColor(or, og, ob, oa);
        vc.addVertex(p, c0 * r0, 0, s0 * r0).setColor(ir, ig, ib, ia);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return WHITE;
    }
}
