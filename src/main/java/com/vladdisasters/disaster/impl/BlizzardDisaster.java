package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.network.ClientDisasterState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Суперметель: видимость почти нулевая, температура падает, вода замерзает, игрок рискует замёрзнуть.
 * Спасает: укрытие под крышей, огонь/лава/костёр рядом, кожаная броня.
 */
public class BlizzardDisaster extends Disaster {
    /** Температура тела каждого игрока, °C. */
    protected final Map<UUID, Float> bodyTemp = new HashMap<>();
    protected float airTemp = -28F;
    protected int freezeRadius = 18;
    protected boolean showTemperature = true;
    /** Множитель скорости остывания (ядерная зима мягче метели). */
    protected float coldRate = 1F;

    public BlizzardDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 120 * 20);
    }

    protected BlizzardDisaster(DisasterType type, ServerLevel level, ServerPlayer target, int duration) {
        super(type, level, target, duration);
    }

    @Override
    public void start() {
        level.setWeatherParameters(0, duration + 100, true, true);
    }

    @Override
    public void end() {
        level.setWeatherParameters(6000, 0, false, false);
    }

    protected boolean nearHeat(ServerPlayer p) {
        BlockPos c = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-4, -2, -4), c.offset(4, 2, 4))) {
            BlockState s = level.getBlockState(q);
            if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.LAVA) || s.is(Blocks.MAGMA_BLOCK)
                    || (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT))
                    || (s.is(Blocks.FURNACE) || s.is(Blocks.BLAST_FURNACE) || s.is(Blocks.SMOKER)) && s.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)) {
                return true;
            }
        }
        return false;
    }

    protected boolean wearsLeather(ServerPlayer p) {
        int n = 0;
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            var it = p.getItemBySlot(slot).getItem();
            if (it == net.minecraft.world.item.Items.LEATHER_HELMET || it == net.minecraft.world.item.Items.LEATHER_CHESTPLATE
                    || it == net.minecraft.world.item.Items.LEATHER_LEGGINGS || it == net.minecraft.world.item.Items.LEATHER_BOOTS) n++;
        }
        return n >= 2;
    }

    @Override
    public void tick() {
        for (ServerPlayer p : players()) {
            if (p.isCreative()) continue;
            // Метель в лицо
            if (age % 3 == 0) {
                level.sendParticles(p, ParticleTypes.SNOWFLAKE, false, p.getX(), p.getY() + 1.5, p.getZ(), 25, 6, 3, 6, 0.25);
            }
            if (age % 10 == 0) {
                UUID id = p.getUUID();
                float t = bodyTemp.getOrDefault(id, 36.6F);
                boolean sheltered = !level.canSeeSky(p.blockPosition().above());
                boolean heat = nearHeat(p);
                boolean leather = wearsLeather(p);
                float delta;
                if (heat) delta = +0.8F;
                else if (sheltered) delta = leather ? +0.3F : -0.05F;
                else delta = leather ? -0.12F : -0.45F;
                if (p.isInWater()) delta -= 0.8F;
                if (delta < 0) delta *= coldRate;
                t = Math.max(20F, Math.min(36.6F, t + delta));
                bodyTemp.put(id, t);

                if (t < 33F) {
                    // Замерзание: ванильный эффект инея и урон от холода
                    int add = (int) ((33F - t) * 6) + 25;
                    p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 40, p.getTicksFrozen() + add));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, t < 29F ? 2 : 1, false, false));
                    if (t < 27F) p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, false, false));
                }
                if (showTemperature && age % 40 == 0) {
                    ChatFormatting c = t > 35 ? ChatFormatting.GREEN : t > 32 ? ChatFormatting.YELLOW : ChatFormatting.RED;
                    p.displayClientMessage(Component.literal("❄ Воздух: " + Math.round(airTemp) + "°C  |  Тело: ")
                            .withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(String.format("%.1f°C", t)).withStyle(c, ChatFormatting.BOLD))
                            .append(Component.literal(heat ? "  🔥 греешься" : sheltered ? "  🏠 укрытие" : "  ⚠ найди огонь или укрытие").withStyle(ChatFormatting.GRAY)), true);
                }
            }
            if (age % 6 == 0) freezeAround(p.blockPosition());
        }
        // Температура воздуха медленно падает
        if (age % 100 == 0) airTemp = Math.max(-45F, airTemp - 1.2F);
    }

    /** Вода покрывается льдом, земля — снегом. */
    protected void freezeAround(BlockPos c) {
        for (int i = 0; i < 24; i++) {
            int x = c.getX() + random.nextInt(freezeRadius * 2 + 1) - freezeRadius;
            int z = c.getZ() + random.nextInt(freezeRadius * 2 + 1) - freezeRadius;
            BlockPos probe = new BlockPos(x, c.getY(), z);
            if (!level.isLoaded(probe)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            BlockPos p = new BlockPos(x, y, z);
            BlockState st = level.getBlockState(p);
            BlockPos above = p.above();
            if (st.is(Blocks.WATER) && st.getFluidState().isSource()) {
                level.setBlock(p, Blocks.ICE.defaultBlockState(), 3);
            } else if (st.is(Blocks.LAVA) && st.getFluidState().isSource() && random.nextInt(4) == 0) {
                level.setBlock(p, Blocks.OBSIDIAN.defaultBlockState(), 3);
            } else if (st.is(Blocks.SNOW)) {
                int layers = st.getValue(SnowLayerBlock.LAYERS);
                if (layers < 6 && random.nextInt(2) == 0) level.setBlock(p, st.setValue(SnowLayerBlock.LAYERS, layers + 1), 3);
            } else if (level.getBlockState(above).isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, above)) {
                level.setBlock(above, Blocks.SNOW.defaultBlockState(), 3);
            }
        }
    }

    @Override
    public int effectFlags(ServerPlayer player) {
        return ClientDisasterState.BLIZZARD;
    }

    @Override
    public float shake(ServerPlayer player) {
        return 0.05F;
    }
}
