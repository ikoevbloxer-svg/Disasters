package com.vladdisasters.item;

import com.vladdisasters.disaster.DisasterManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Идентификатор бедствий. Пока лежит в инвентаре — над хотбаром показано,
 * какое бедствие сейчас идёт и сколько осталось (см. DisasterManager.identifierHud).
 */
public class DisasterIdentifierItem extends Item {
    public DisasterIdentifierItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.displayClientMessage(DisasterManager.statusLine(), false);
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BIT.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.6F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Пока в инвентаре — показывает текущее бедствие").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("ПКМ — подробности в чат").withStyle(ChatFormatting.GRAY));
    }
}
