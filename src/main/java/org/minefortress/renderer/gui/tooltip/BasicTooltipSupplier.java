package org.minefortress.renderer.gui.tooltip;

import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public class BasicTooltipSupplier extends BaseTooltipSupplier {

    private final Text text;

    public BasicTooltipSupplier(String text) {
        this.text = new LiteralText(text);
    }

    @Override
    protected List<OrderedText> getTooltip() {
        return List.of(text.asOrderedText());
    }
}
