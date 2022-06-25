package org.minefortress.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import org.minefortress.fortress.resources.server.ServerResourceManager;
import org.minefortress.interfaces.FortressServer;
import org.minefortress.interfaces.FortressServerPlayerEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DebugItemsCommand implements MineFortressCommand {

    private static final List<Item> ITEMS_TO_ADD = Arrays.asList(
            Items.OAK_PLANKS,
            Items.POTATO,
            Items.SALMON,
            Items.COOKED_SALMON,
            Items.COOKED_PORKCHOP,
            Items.STONE_SHOVEL,
            Items.STONE_PICKAXE,
            Items.STONE_AXE,
            Items.STONE_HOE,
            Items.COBBLESTONE,
            Items.STONE,
            Items.STONE_BRICKS,
            Items.STONE_SLAB,
            Items.STONE_STAIRS,
            Items.STONE_PRESSURE_PLATE,
            Items.STONE_BUTTON,
            Items.OAK_WOOD,
            Items.OAK_LOG,
            Items.OAK_SAPLING,
            Items.BONE,
            Items.BONE_BLOCK,
            Items.RED_WOOL,
            Items.WHITE_WOOL,
            Items.ORANGE_WOOL,
            Items.MAGENTA_WOOL,
            Items.LIGHT_BLUE_WOOL,
            Items.OAK_BOAT
    );

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("fortress")
                .then(literal("items")
                    .then(argument("num", IntegerArgumentType.integer())
                        .executes(
                            context -> {
                                int num = IntegerArgumentType.getInteger(context, "num");
                                final var srvPlayer = context.getSource().getPlayer();
                                final var server = (FortressServer) srvPlayer.getServer();
                                final var serverManager = server.getFortressModServerManager().getByPlayer(srvPlayer);
                                final var resourceManager = serverManager.getServerResourceManager();

                                final var random = context.getSource().getWorld().random;
                                for (int i = 0; i < num; i++) {
                                    final var item = ITEMS_TO_ADD.get(random.nextInt(ITEMS_TO_ADD.size()));
                                    resourceManager.increaseItemAmount(item, random.nextInt(250));
                                }
                                return 1;
                            }
                        )
                    )
                )
        );

        dispatcher.register(
            literal("fortress")
                .then(literal("items")
                        .then(literal("clear")
                                .executes(
                                        context -> {
                                                final var srvPlayer = context.getSource().getPlayer();
                                                final var server = (FortressServer) srvPlayer.getServer();
                                                final var serverManager = server.getFortressModServerManager().getByPlayer(srvPlayer);
                                                final var resourceManager = serverManager.getServerResourceManager();

                                            resourceManager
                                                    .getAllItems()
                                                    .stream()
                                                    .map(ItemStack::getItem)
                                                    .forEach(it -> resourceManager.setItemAmount(it, 0));
                                            return 1;
                                        }
                                )
                        )
                )
        );
    }
}
