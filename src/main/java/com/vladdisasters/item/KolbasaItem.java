package com.vladdisasters.item;

import com.vladdisasters.entity.MamaVladEntity;
import com.vladdisasters.registry.ModEntities;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Кусок колбасы: призывает Маму Влада на 120 секунд, она дерётся за тебя. */
public class KolbasaItem extends Item {
    public KolbasaItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            MamaVladEntity mama = ModEntities.MAMA_VLAD.get().create(sl);
            if (mama == null) return InteractionResultHolder.fail(stack);
            Vec3 look = player.getLookAngle();
            Vec3 pos = player.position().add(look.x * 3, 0, look.z * 3);
            mama.moveTo(pos.x, pos.y, pos.z, player.getYRot() + 180F, 0);
            mama.setupVariant(MamaVladEntity.VARIANT_ALLY);
            mama.setOwner(player.getUUID());
            sl.addFreshEntity(mama);
            sl.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 1, pos.z, 50, 1.2, 1.5, 1.2, 0.05);
            sl.sendParticles(ParticleTypes.HEART, pos.x, pos.y + 3, pos.z, 10, 1, 1, 1, 0.1);
            sl.playSound(null, pos.x, pos.y, pos.z, ModSounds.MAMA_EAT.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
            mama.scream();
            player.displayClientMessage(Component.literal("Мама Влада учуяла колбасу и пришла защищать тебя! (120 сек)").withStyle(ChatFormatting.GOLD), true);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.getCooldowns().addCooldown(this, 20 * 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ — призвать Маму Влада на 120 секунд").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Она сражается на твоей стороне").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("«Докторская», между прочим").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
