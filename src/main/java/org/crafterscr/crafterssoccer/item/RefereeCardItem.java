package org.crafterscr.crafterssoccer.item;

import org.crafterscr.crafterssoccer.referee.RefereeCardManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Tarjeta física utilizada directamente sobre un jugador.
 */
public final class RefereeCardItem extends Item {

    private final CardType cardType;

    public RefereeCardItem(
            CardType cardType,
            Properties properties
    ) {
        super(properties);
        this.cardType = cardType;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer referee)
                || !(target instanceof ServerPlayer targetPlayer)) {
            return InteractionResult.PASS;
        }

        boolean applied =
                cardType == CardType.YELLOW
                        ? RefereeCardManager.issueYellow(
                                referee.getServer(),
                                referee,
                                targetPlayer
                        )
                        : RefereeCardManager.issueRed(
                                referee.getServer(),
                                referee,
                                targetPlayer
                        );

        return applied
                ? InteractionResult.CONSUME
                : InteractionResult.FAIL;
    }

    public CardType getCardType() {
        return cardType;
    }

    public enum CardType {
        YELLOW,
        RED
    }
}
