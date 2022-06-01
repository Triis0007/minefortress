package org.minefortress.renderer.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.minefortress.interfaces.FortressClientWorld;
import org.minefortress.interfaces.FortressMinecraftClient;
import org.minefortress.network.ServerboundSetCombatStatePacket;
import org.minefortress.network.helpers.FortressChannelNames;
import org.minefortress.network.helpers.FortressClientNetworkHelper;
import org.minefortress.renderer.gui.blueprints.BlueprintsScreen;
import org.minefortress.renderer.gui.widget.*;
import org.minefortress.selections.SelectionType;
import org.minefortress.tasks.ClientTasksHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ToolsGui extends FortressGuiScreen {

    private final FortressItemButtonWidget selectionType;
    private final FortressItemButtonWidget blueprints;
    private final FortressItemButtonWidget treeCutter;
    private final FortressItemButtonWidget roadsBuilder;
    private final FortressItemButtonWidget combatMode;
    private final ButtonWidget questionButton;
    private final ButtonWidget selectionVisibilityButton;

    private final List<ButtonWidget> selectionButtons = new ArrayList<>();

    protected ToolsGui(MinecraftClient client, ItemRenderer itemRenderer) {
        super(client, itemRenderer);
        this.selectionType = new FortressItemButtonWidget(
                0,
                0,
                Items.DIAMOND_PICKAXE,
                itemRenderer,
                btn -> {
                    final FortressItemButtonWidget fortressBtn = (FortressItemButtonWidget) btn;
                    fortressBtn.checked = !fortressBtn.checked;
                },
                (button, matrices, mouseX, mouseY) -> {
                    final FortressItemButtonWidget fortressButton = (FortressItemButtonWidget) button;
                    if(!fortressButton.checked) {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Selection Type"), mouseX, mouseY);
                    }
                },
                Text.of("")
        );
        final FortressMinecraftClient fortressClient = (FortressMinecraftClient) client;
        final Optional<ClientTasksHolder> clientTasksHolderOpt = Optional.ofNullable((FortressClientWorld) client.world)
                .map(FortressClientWorld::getClientTasksHolder);
        this.blueprints = new FortressBlueprintsButtonWidget(
                0,
                0,
                Items.OAK_DOOR,
                itemRenderer,
                btn -> {
                    if(blueprintSelected(fortressClient)) {
                        fortressClient.getBlueprintManager().clearStructure();
                    } else {
                        this.client.setScreen(new BlueprintsScreen());
//                        fortressClient.getBlueprintMetadataManager().selectFirst();
                    }
                },
                (button, matrices, mouseX, mouseY) -> {
                    if (blueprintSelected(fortressClient)) {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Cancel"), mouseX, mouseY);
                    } else {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Blueprints"), mouseX, mouseY);
                    }
                },
                Text.of("")
        );

        this.treeCutter = new FortressTreeCutterButtonWidget(
                0,
                0,
                Items.DIAMOND_AXE,
                itemRenderer,
                btn -> {
                    if(treeCutterSelected(fortressClient)) {
                        fortressClient.getSelectionManager().setSelectionType(SelectionType.SQUARES);
                    } else {
                        fortressClient.getSelectionManager().setSelectionType(SelectionType.TREE);
                    }
                },
                (button, matrices, mouseX, mouseY) -> {
                    if(treeCutterSelected(fortressClient)) {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Cancel"), mouseX, mouseY);
                    } else {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Cut the trees"), mouseX, mouseY);
                    }
                },
                Text.of("")
        );

        this.roadsBuilder = new FortressRoadsButtonWidget(
                0,
                0,
                Items.DIAMOND_SHOVEL,
                itemRenderer,
                btn -> {
                    if(roadsSelected(fortressClient)) {
                        fortressClient.getSelectionManager().setSelectionType(SelectionType.SQUARES);
                    } else {
                        fortressClient.getSelectionManager().setSelectionType(SelectionType.ROADS);
                    }
                },
                (button, matrices, mouseX, mouseY) -> {
                    if(roadsSelected(fortressClient)) {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Cancel"), mouseX, mouseY);
                    } else {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Build roads"), mouseX, mouseY);
                    }
                },
                Text.of("")
        );

        this.combatMode = new FortressCombatButtonWidget(
                0,
                0,
                Items.DIAMOND_SWORD,
                itemRenderer,
                btn -> {
                    final var inCombat = isInCombat(fortressClient);
                    final var packet = new ServerboundSetCombatStatePacket(!inCombat);
                    FortressClientNetworkHelper.send(FortressChannelNames.FORTRESS_SET_COMBAT_STATE, packet);
                },
                (button, matrices, mouseX, mouseY) -> {
                    if(isInCombat(fortressClient)) {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Cancel"), mouseX, mouseY);
                    } else {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Fight"), mouseX, mouseY);
                    }
                },
                Text.of("")
        );

        this.selectionVisibilityButton = new FortressSelectionVisibilityButtonWidget(
                0,
                0,
                (button, matrices, mouseX, mouseY) -> {
                    if (clientTasksHolderOpt.isPresent() && clientTasksHolderOpt.get().isSelectionHidden()) {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Show Tasks outline"), mouseX, mouseY);
                    } else {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Hide Tasks outline"), mouseX, mouseY);
                    }
                },
                itemRenderer
        );

        this.questionButton = new ButtonWidget(0, 0, 20, 20, new LiteralText("?"), btn -> {
            this.selectionType.checked = false;
            final BookScreen questionsScreen = new BookScreen(new FortressBookContents(FortressBookContents.HELP_BOOK));

            this.client.setScreen(questionsScreen);
        });


        final SelectionType[] values = Arrays
                .stream(SelectionType.values())
                .filter(type -> type != SelectionType.TREE)
                .filter(type -> type != SelectionType.ROADS)
                .toArray(SelectionType[]::new);

        for(final SelectionType type : values) {
            this.selectionButtons.add(
                    new ButtonWidget(0, 0, 20, 20, new LiteralText(type.getButtonText()), btn -> {
                        fortressClient.getSelectionManager().setSelectionType(type);
                    }, (button, matrices, mouseX, mouseY) -> renderTooltip(matrices, new LiteralText(type.getName()), mouseX, mouseY))
            );
        }
    }

    @Override
    void tick() {
        if(this.selectionType.checked) {
            this.selectionButtons.forEach(b -> b.visible = true);
        } else {
            this.selectionButtons.forEach(b -> b.visible = false);
        }
    }

    @Override
    void render(MatrixStack p, TextRenderer font, int screenWidth, int screenHeight, double mouseX, double mouseY, float delta) {
        final FortressMinecraftClient fortressClient = (FortressMinecraftClient) this.client;

        if(!blueprintSelected(fortressClient) && !treeCutterSelected(fortressClient) && !roadsSelected(fortressClient)) {
            this.selectionType.setPos(screenWidth - 25, 5);
            this.selectionType.render(p, (int)mouseX, (int)mouseY, delta);

            for (int i = 0; i < selectionButtons.size(); i++) {
                ButtonWidget btn = selectionButtons.get(i);
                btn.x = screenWidth - 60;
                btn.y = i * 25 + 5;

                btn.render(p, (int)mouseX, (int)mouseY, delta);
            }

            this.questionButton.x = screenWidth - 25;
            this.questionButton.y = 155;
            this.questionButton.render(p, (int)mouseX, (int)mouseY, delta);
        }

        if(!treeCutterSelected(fortressClient) && !roadsSelected(fortressClient) && !isInCombat(fortressClient)) {
            this.blueprints.setPos(screenWidth - 25, 30);
            this.blueprints.render(p, (int)mouseX, (int)mouseY, delta);
        }

        if(!blueprintSelected(fortressClient) && !roadsSelected(fortressClient) && !isInCombat(fortressClient)) {
            this.treeCutter.setPos(screenWidth - 25, 55);
            this.treeCutter.render(p, (int)mouseX, (int)mouseY, delta);
        }

        if(!blueprintSelected(fortressClient) && !treeCutterSelected(fortressClient) && !isInCombat(fortressClient)) {
            this.roadsBuilder.setPos(screenWidth - 25, 80);
            this.roadsBuilder.render(p, (int)mouseX, (int)mouseY, delta);
        }

        if(!blueprintSelected(fortressClient) && !treeCutterSelected(fortressClient) && !roadsSelected(fortressClient)) {
            this.combatMode.setPos(screenWidth - 25, 105);
            this.combatMode.render(p, (int)mouseX, (int)mouseY, delta);
        }

        if(!isInCombat(fortressClient)) {
            this.selectionVisibilityButton.x = screenWidth - 25;
            this.selectionVisibilityButton.y = 130;
            this.selectionVisibilityButton.render(p, (int)mouseX, (int)mouseY, delta);
        }
    }

    @Override
    boolean isHovered() {
        return this.selectionType.isHovered() ||
                this.questionButton.isHovered() ||
                this.selectionButtons.stream().anyMatch(btn -> btn.visible && btn.isHovered()) ||
                this.blueprints.isHovered() ||
                this.treeCutter.isHovered() ||
                this.roadsBuilder.isHovered() ||
                this.combatMode.isHovered() ||
                this.selectionVisibilityButton.isHovered();

    }

    @Override
    void onClick(double mouseX, double mouseY) {
        this.selectionType.onClick(mouseX, mouseY);
        if(questionButton.isHovered())
            this.questionButton.onClick(mouseX, mouseY);

        if(this.blueprints.isHovered())
            this.blueprints.onClick(mouseX, mouseY);

        if(this.selectionVisibilityButton.isHovered())
            this.selectionVisibilityButton.onClick(mouseX, mouseY);

        if(this.treeCutter.isHovered())
            this.treeCutter.onClick(mouseX, mouseY);

        if(this.roadsBuilder.isHovered())
            this.roadsBuilder.onClick(mouseX, mouseY);

        if(this.combatMode.isHovered())
            this.combatMode.onClick(mouseX, mouseY);

        for (ButtonWidget btn : selectionButtons) {
            if (btn.visible && btn.isHovered())
                btn.onClick(mouseX, mouseY);
        }
    }

    private boolean treeCutterSelected(FortressMinecraftClient fortressClient) {
        return fortressClient.getSelectionManager().getSelectionTypeIndex() == SelectionType.TREE.ordinal();
    }

    private boolean roadsSelected(FortressMinecraftClient fortressClient) {
        return fortressClient.getSelectionManager().getSelectionTypeIndex() == SelectionType.ROADS.ordinal();
    }

    private boolean blueprintSelected(FortressMinecraftClient fortressClient) {
        return fortressClient.getBlueprintManager().hasSelectedBlueprint();
    }

    private boolean isInCombat(FortressMinecraftClient fortressClient) {
        return fortressClient.getFortressClientManager().isInCombat();
    }
}
