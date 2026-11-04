package com.vladdisasters.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vladdisasters.VladDisasters;
import com.vladdisasters.client.model.MamaVladModel;
import com.vladdisasters.entity.MamaVladEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MamaVladRenderer extends MobRenderer<MamaVladEntity, MamaVladModel<MamaVladEntity>> {
    private static final ResourceLocation TEXTURE = VladDisasters.id("textures/entity/mama_vlad.png");
    private static final ResourceLocation TEXTURE_ANGRY = VladDisasters.id("textures/entity/mama_vlad_angry.png");

    public MamaVladRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MamaVladModel<>(ctx.bakeLayer(MamaVladModel.LAYER)), 2.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(MamaVladEntity entity) {
        return entity.isDisaster() ? TEXTURE_ANGRY : TEXTURE;
    }

    @Override
    protected void scale(MamaVladEntity entity, PoseStack pose, float partialTick) {
        // Модель ~2.75 блока, увеличиваем вдвое. Вариант (дом/союзник) дополнительно масштабируется атрибутом SCALE.
        float s = 2.0F;
        if (entity.screamingTicks() > 0) {
            float j = (float) Math.sin((entity.tickCount + partialTick) * 2.3F) * 0.03F;
            pose.translate(j, 0, -j);
        }
        pose.scale(s, s, s);
    }
}
