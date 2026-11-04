package com.vladdisasters.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.vladdisasters.entity.MeteorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Метеор рисуется как вращающаяся раскалённая глыба из нескольких повёрнутых блоков. */
public class MeteorRenderer extends EntityRenderer<MeteorEntity> {
    private final BlockRenderDispatcher blocks;

    public MeteorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blocks = ctx.getBlockRenderDispatcher();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(MeteorEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
        float size = e.getSize();
        float t = e.tickCount + pt;
        BlockState outer = e.getKind() == MeteorEntity.KIND_GIGA ? Blocks.BLACKSTONE.defaultBlockState()
                : e.getKind() == MeteorEntity.KIND_LAVA_BOMB ? Blocks.MAGMA_BLOCK.defaultBlockState()
                : Blocks.NETHERRACK.defaultBlockState();
        BlockState inner = Blocks.MAGMA_BLOCK.defaultBlockState();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(t * 7F));
        pose.mulPose(Axis.XP.rotationDegrees(t * 4F));
        cube(pose, buf, outer, size);
        pose.mulPose(Axis.ZP.rotationDegrees(45F));
        pose.mulPose(Axis.XP.rotationDegrees(35F));
        cube(pose, buf, inner, size * 0.92F);
        if (e.getKind() == MeteorEntity.KIND_GIGA) {
            pose.mulPose(Axis.YP.rotationDegrees(30F));
            cube(pose, buf, Blocks.BASALT.defaultBlockState(), size * 0.97F);
            pose.mulPose(Axis.ZP.rotationDegrees(60F));
            cube(pose, buf, Blocks.MAGMA_BLOCK.defaultBlockState(), size * 0.9F);
        }
        pose.popPose();
        super.render(e, yaw, pt, pose, buf, light);
    }

    private void cube(PoseStack pose, MultiBufferSource buf, BlockState state, float s) {
        pose.pushPose();
        pose.scale(s, s, s);
        pose.translate(-0.5F, -0.5F, -0.5F);
        blocks.renderSingleBlock(state, pose, buf, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @SuppressWarnings("unused")
    private static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
