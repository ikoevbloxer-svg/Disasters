package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.network.ClientDisasterState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/** Кислотный дождь: разъедает броню, живых существ, траву, листву и посевы. */
public class AcidRainDisaster extends Disaster {
    private static final DustParticleOptions ACID = new DustParticleOptions(new Vector3f(0.45F, 1.0F, 0.1F), 1.4F);

    public AcidRainDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 90 * 20);
    }

    @Override
    public void start() {
        level.setWeatherParameters(0, duration + 100, true, false);
    }

    @Override
    public void end() {
        level.setWeatherParameters(6000, 0, false, false);
    }

    private boolean exposed(LivingEntity e) {
        BlockPos head = BlockPos.containing(e.getX(), e.getEyeY(), e.getZ());
        return level.canSeeSky(head) && !e.isInWater();
    }

    @Override
    public void tick() {
        for (ServerPlayer p : players()) {
            // Урон и разъедание шлема
            if (age % 20 == 0 && exposed(p)) {
                ItemStack helm = p.getItemBySlot(EquipmentSlot.HEAD);
                if (!helm.isEmpty()) {
                    helm.hurtAndBreak(3, p, EquipmentSlot.HEAD);
                    if (random.nextInt(3) == 0) p.hurt(level.damageSources().magic(), 1.0F);
                } else {
                    p.hurt(level.damageSources().magic(), 2.0F);
                }
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.WEATHER, 0.4F, 1.6F);
                level.sendParticles(ACID, p.getX(), p.getY() + 1.8, p.getZ(), 8, 0.4, 0.3, 0.4, 0);
            }
            // Мобы и животные
            if (age % 40 == 0) {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(48, 24, 48), e -> !(e instanceof ServerPlayer))) {
                    if (exposed(e)) e.hurt(level.damageSources().magic(), 2.0F);
                }
            }
            // Коррозия блоков
            if (age % 5 == 0) {
                for (int i = 0; i < 14; i++) corrode(p.blockPosition());
            }
            // Шипящие частицы вокруг
            if (age % 4 == 0) {
                BlockPos s = DisasterUtil.randomSurface(level, p.blockPosition(), 0, 16);
                if (s != null) level.sendParticles(ACID, s.getX() + 0.5, s.getY() + 1.1, s.getZ() + 0.5, 6, 0.4, 0.05, 0.4, 0);
            }
        }
    }

    private void corrode(BlockPos around) {
        BlockPos s = DisasterUtil.randomSurface(level, around, 0, 40);
        if (s == null || !level.canSeeSky(s.above())) return;
        BlockState st = level.getBlockState(s);
        BlockState rep = null;
        if (st.is(BlockTags.LEAVES)) rep = Blocks.AIR.defaultBlockState();
        else if (st.getBlock() instanceof CropBlock || st.is(BlockTags.FLOWERS) || st.is(BlockTags.SAPLINGS)
                || st.is(Blocks.SHORT_GRASS) || st.is(Blocks.TALL_GRASS) || st.is(Blocks.FERN)) {
            rep = random.nextInt(3) == 0 ? Blocks.DEAD_BUSH.defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (rep.is(Blocks.DEAD_BUSH) && !rep.canSurvive(level, s)) rep = Blocks.AIR.defaultBlockState();
        } else if (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT) || st.is(Blocks.PODZOL) || st.is(Blocks.FARMLAND)) {
            rep = Blocks.COARSE_DIRT.defaultBlockState();
        } else if (st.is(Blocks.STONE)) rep = Blocks.COBBLESTONE.defaultBlockState();
        else if (st.is(Blocks.COBBLESTONE) && random.nextInt(6) == 0) rep = Blocks.GRAVEL.defaultBlockState();
        else if (st.is(BlockTags.WOOL) || st.is(BlockTags.WOOL_CARPETS)) rep = Blocks.AIR.defaultBlockState();
        else if (st.is(BlockTags.PLANKS) && random.nextInt(8) == 0) rep = Blocks.AIR.defaultBlockState();
        if (rep != null) {
            level.setBlock(s, rep, 3);
            level.sendParticles(ACID, s.getX() + 0.5, s.getY() + 1, s.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0);
        }
    }

    @Override
    public int effectFlags(ServerPlayer player) {
        return ClientDisasterState.ACID;
    }
}
