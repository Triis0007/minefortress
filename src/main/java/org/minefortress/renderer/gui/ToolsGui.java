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
import org.minefortress.interfaces.FortressMinecraftClient;
import org.minefortress.renderer.gui.blueprints.BlueprintsScreen;
import org.minefortress.renderer.gui.widget.FortressBlueprintsButtonWidget;
import org.minefortress.renderer.gui.widget.FortressItemButtonWidget;
import org.minefortress.renderer.gui.widget.FortressSelectionVisibilityButtonWidget;
import org.minefortress.selections.SelectionType;

import java.util.ArrayList;
import java.util.List;

public class ToolsGui extends FortressGuiScreen {

    private final FortressItemButtonWidget selectionType;
    private final FortressItemButtonWidget blueprints;
    private final FortressItemButtonWidget treeCutter;
    private final FortressItemButtonWidget roadsBuilder;
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
        this.blueprints = new FortressBlueprintsButtonWidget(
                0,
                0,
                Items.BOOK,
                itemRenderer,
                btn -> {
                    if(fortressClient.getBlueprintManager().hasSelectedBlueprint()) {
                        fortressClient.getBlueprintManager().clearStructure();
                    } else {
                        this.client.setScreen(new BlueprintsScreen());
//                        fortressClient.getBlueprintMetadataManager().selectFirst();
                    }
                },
                (button, matrices, mouseX, mouseY) -> {
                    if (fortressClient.getBlueprintManager().hasSelectedBlueprint()) {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Cancel"), mouseX, mouseY);
                    } else {
                        ToolsGui.super.renderTooltip(matrices, new LiteralText("Blueprints"), mouseX, mouseY);
                    }
                },
                Text.of("")
        );

        this.treeCutter = new FortressItemButtonWidget(
                0,
                0,
                Items.DIAMOND_AXE,
                itemRenderer,
                btn -> {fortressClient.getSelectionManager().setSelectionType(SelectionType.TREE);},
                (button, matrices, mouseX, mouseY) -> {
                    ToolsGui.super.renderTooltip(matrices, new LiteralText("Cut the trees"), mouseX, mouseY);
                },
                Text.of("")
        );

        this.roadsBuilder = new FortressItemButtonWidget(
                0,
                0,
                Items.DIAMOND_SHOVEL,
                itemRenderer,
                btn -> {fortressClient.getSelectionManager().setSelectionType(SelectionType.SQUARES);},
                (button, matrices, mouseX, mouseY) -> {
                    ToolsGui.super.renderTooltip(matrices, new LiteralText("Build the roads"), mouseX, mouseY);
                },
                Text.of("")
        );

        this.selectionVisibilityButton = new FortressSelectionVisibilityButtonWidget(
                0,
                0,
                (button, matrices, mouseX, mouseY) -> {
                    if (fortressClient.getSelectionManager().isSelectionHidden()) {
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


        for(final SelectionType type : SelectionType.values()) {
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

        if(!fortressClient.getBlueprintManager().hasSelectedBlueprint()) {
            this.selectionType.setPos(screenWidth - 25, 5);
            this.selectionType.render(p, (int)mouseX, (int)mouseY, delta);

            for (int i = 0; i < selectionButtons.size(); i++) {
                ButtonWidget btn = selectionButtons.get(i);
                btn.x = screenWidth - 60;
                btn.y = i * 25 + 5;

                btn.render(p, (int)mouseX, (int)mouseY, delta);
            }

            this.treeCutter.x = screenWidth - 25;
            this.treeCutter.y = 55;
            this.treeCutter.render(p, (int)mouseX, (int)mouseY, delta);

            this.roadsBuilder.x = screenWidth - 25;
            this.roadsBuilder.y = 80;
            this.roadsBuilder.render(p, (int)mouseX, (int)mouseY, delta);

            this.questionButton.x = screenWidth - 25;
            this.questionButton.y = 130;
            this.questionButton.render(p, (int)mouseX, (int)mouseY, delta);
        }

        this.blueprints.setPos(screenWidth - 25, 30);
        this.blueprints.render(p, (int)mouseX, (int)mouseY, delta);

        this.selectionVisibilityButton.x = screenWidth - 25;
        this.selectionVisibilityButton.y = 105;
        this.selectionVisibilityButton.render(p, (int)mouseX, (int)mouseY, delta);

    }

    @Override
    boolean isHovered() {
        final FortressMinecraftClient client = (FortressMinecraftClient) this.client;

        return this.selectionType.isHovered() ||
                this.questionButton.isHovered() ||
                this.selectionButtons.stream().anyMatch(btn -> btn.visible && btn.isHovered()) ||
                this.blueprints.isHovered() ||
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

        for (ButtonWidget btn : selectionButtons) {
            if (btn.visible && btn.isHovered())
                btn.onClick(mouseX, mouseY);
        }
    }
}
