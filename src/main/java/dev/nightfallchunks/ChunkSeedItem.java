package dev.nightfallchunks;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Direction;

/** Server-authoritative right-click action: reveal the adjacent chunk in facing direction. */
final class ChunkSeedItem extends Item {
    ChunkSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.nightfallchunks.chunk_seed.use")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.nightfallchunks.chunk_seed.recipe")
            .withStyle(ChatFormatting.DARK_GREEN));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                   InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (!NightfallWorld.isSelected(serverLevel)) {
            serverPlayer.displayClientMessage(Component.translatable("message.nightfallchunks.wrong_world"), true);
            return InteractionResultHolder.fail(stack);
        }

        ChunkPos current = serverPlayer.chunkPosition();
        Direction facing = serverPlayer.getDirection();
        long targetX = (long) current.x + facing.getStepX();
        long targetZ = (long) current.z + facing.getStepZ();
        if (targetX < Integer.MIN_VALUE || targetX > Integer.MAX_VALUE
            || targetZ < Integer.MIN_VALUE || targetZ > Integer.MAX_VALUE) {
            serverPlayer.displayClientMessage(Component.translatable("message.nightfallchunks.outside_border"), true);
            return InteractionResultHolder.fail(stack);
        }
        ChunkUnlockService.Result result = ChunkUnlockService.unlock(serverLevel,
            current.x, current.z, (int) targetX, (int) targetZ);
        if (result != ChunkUnlockService.Result.UNLOCKED) {
            String key = switch (result) {
                case WRONG_WORLD -> "message.nightfallchunks.wrong_world";
                case NOT_ADJACENT, SOURCE_LOCKED -> "message.nightfallchunks.not_adjacent";
                case OUTSIDE_BORDER -> "message.nightfallchunks.outside_border";
                case ALREADY_UNLOCKED -> "message.nightfallchunks.already_unlocked";
                case GENERATION_FAILED -> "message.nightfallchunks.generation_failed";
                case UNLOCKED -> throw new IllegalStateException("Already handled successful unlock");
            };
            serverPlayer.displayClientMessage(Component.translatable(key), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
            SoundSource.PLAYERS, 0.45F, 1.15F);
        serverPlayer.displayClientMessage(Component.translatable("message.nightfallchunks.unlocked",
            ChunkUnlockState.get(serverLevel).unlockedCount()).withStyle(ChatFormatting.GREEN), true);
        return InteractionResultHolder.consume(stack);
    }
}
