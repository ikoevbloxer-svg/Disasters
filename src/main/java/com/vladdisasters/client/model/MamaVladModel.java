package com.vladdisasters.client.model;

import com.vladdisasters.VladDisasters;
import com.vladdisasters.entity.MamaVladEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * Модель Мамы Влада: огромное толстое тело, живот, короткие толстые ноги и руки,
 * голова с рыжей причёской (пучок, длинные волосы сзади и по бокам), открывающийся рот.
 * Размеры UV совпадают с генератором текстуры (tools/gen_assets.py).
 */
public class MamaVladModel<T extends MamaVladEntity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(VladDisasters.id("mama_vlad"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public MamaVladModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        // Тело: огромный торс + выпирающий живот
        r.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-11.0F, -16.0F, -8.0F, 22, 16, 16)
                        .texOffs(0, 32).addBox(-9.0F, -13.0F, -11.0F, 18, 11, 3),
                PartPose.offset(0.0F, 14.0F, 0.0F));

        // Голова с волосами
        PartDefinition head = r.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(76, 0).addBox(-6.0F, -12.0F, -6.0F, 12, 12, 12)
                        .texOffs(100, 50).addBox(-1.0F, -6.0F, -8.0F, 2, 3, 2),   // нос
                PartPose.offset(0.0F, -2.0F, 0.0F));
        head.addOrReplaceChild("hair_top", CubeListBuilder.create()
                .texOffs(44, 32).addBox(-7.0F, -14.0F, -7.0F, 14, 4, 14), PartPose.ZERO);
        head.addOrReplaceChild("hair_back", CubeListBuilder.create()
                .texOffs(0, 50).addBox(-7.0F, -12.0F, 4.0F, 14, 16, 4), PartPose.ZERO);
        head.addOrReplaceChild("hair_left", CubeListBuilder.create()
                .texOffs(36, 52).addBox(5.5F, -12.0F, -5.0F, 2, 12, 10), PartPose.ZERO);
        head.addOrReplaceChild("hair_right", CubeListBuilder.create()
                .texOffs(60, 52).addBox(-7.5F, -12.0F, -5.0F, 2, 12, 10), PartPose.ZERO);
        head.addOrReplaceChild("bun", CubeListBuilder.create()
                .texOffs(100, 32).addBox(-3.0F, -18.0F, -3.0F, 6, 4, 6), PartPose.ZERO);
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(100, 44).addBox(-4.0F, 0.0F, -1.5F, 8, 3, 2),
                PartPose.offset(0.0F, -3.0F, -5.0F));

        // Руки
        r.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 74).addBox(-2.0F, -2.0F, -3.0F, 6, 16, 6), PartPose.offset(13.0F, 0.0F, 0.0F));
        r.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(24, 74).addBox(-4.0F, -2.0F, -3.0F, 6, 16, 6), PartPose.offset(-13.0F, 0.0F, 0.0F));
        // Ноги
        r.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(48, 74).addBox(-3.0F, 0.0F, -3.0F, 6, 10, 6), PartPose.offset(5.0F, 14.0F, 0.0F));
        r.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(72, 74).addBox(-3.0F, 0.0F, -3.0F, 6, 10, 6), PartPose.offset(-5.0F, 14.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;

        float walk = limbSwing * 0.45F;
        leftLeg.xRot = Mth.cos(walk) * 1.0F * limbSwingAmount;
        rightLeg.xRot = Mth.cos(walk + Mth.PI) * 1.0F * limbSwingAmount;
        leftArm.xRot = Mth.cos(walk + Mth.PI) * 0.7F * limbSwingAmount;
        rightArm.xRot = Mth.cos(walk) * 0.7F * limbSwingAmount;
        leftArm.zRot = -0.18F;
        rightArm.zRot = 0.18F;

        // Живот колышется при ходьбе
        body.zRot = Mth.sin(walk) * 0.06F * limbSwingAmount;
        body.y += Mth.abs(Mth.sin(walk)) * 0.8F * limbSwingAmount;

        int eating = entity.eatingTicks();
        int screaming = entity.screamingTicks();
        if (eating > 0) {
            // Загребает еду руками в рот
            float e = Mth.sin(ageInTicks * 0.9F);
            leftArm.xRot = -1.9F + e * 0.5F;
            rightArm.xRot = -1.9F - e * 0.5F;
            leftArm.zRot = -0.45F;
            rightArm.zRot = 0.45F;
            jaw.xRot = 0.35F + Mth.abs(Mth.sin(ageInTicks * 1.2F)) * 0.5F;
        }
        if (screaming > 0) {
            // Истошный вопль: голова назад, рот нараспашку, руки в стороны
            jaw.xRot = 0.9F + Mth.sin(ageInTicks * 2.5F) * 0.1F;
            head.xRot = -0.35F + Mth.sin(ageInTicks * 3.0F) * 0.05F;
            head.zRot = Mth.sin(ageInTicks * 1.7F) * 0.08F;
            leftArm.zRot = -1.1F;
            rightArm.zRot = 1.1F;
        }
        if (eating == 0 && screaming == 0) {
            jaw.xRot = Mth.abs(Mth.sin(ageInTicks * 0.08F)) * 0.08F;
            // дыхание
            body.y += Mth.sin(ageInTicks * 0.1F) * 0.3F;
        }
    }
}
