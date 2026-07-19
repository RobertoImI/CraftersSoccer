package org.crafterscr.crafterssoccer.registry;

import org.crafterscr.crafterssoccer.CraftersSoccer;
import org.crafterscr.crafterssoccer.item.RefereeCardItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registro de objetos de CraftersSoccer.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    Registries.ITEM,
                    CraftersSoccer.MOD_ID
            );

    public static final DeferredHolder<Item, Item> YELLOW_CARD =
            ITEMS.register(
                    "yellow_card",
                    () -> new RefereeCardItem(
                            RefereeCardItem.CardType.YELLOW,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredHolder<Item, Item> RED_CARD =
            ITEMS.register(
                    "red_card",
                    () -> new RefereeCardItem(
                            RefereeCardItem.CardType.RED,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    private ModItems() {
    }
}
