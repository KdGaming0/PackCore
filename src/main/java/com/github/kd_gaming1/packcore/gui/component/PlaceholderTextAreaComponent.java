package com.github.kd_gaming1.packcore.gui.component;

import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Extended TextAreaComponent that supports placeholder text
 */
public class PlaceholderTextAreaComponent extends TextAreaComponent {
    @Nullable
    private Text placeholder;
    private int placeholderColor = 0x808080; // Gray color for placeholder text

    protected PlaceholderTextAreaComponent(Sizing horizontalSizing, Sizing verticalSizing) {
        super(horizontalSizing, verticalSizing);
    }

    /**
     * Sets the placeholder text that will be displayed when the text area is empty and not focused
     */
    public PlaceholderTextAreaComponent placeholder(Text placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    /**
     * Sets the color of the placeholder text
     */
    public PlaceholderTextAreaComponent placeholderColor(int color) {
        this.placeholderColor = color;
        return this;
    }

    @Override
    protected void renderOverlay(DrawContext context) {
        // Call the parent render method first
        super.renderOverlay(context);

        // Render placeholder if text is empty and component is not focused
        if (this.placeholder != null && this.getText().isEmpty() && !this.isFocused()) {
            renderPlaceholder(context);
        }
    }

    private void renderPlaceholder(DrawContext context) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;

        // Calculate position similar to how TextFieldWidget does it
        int x = this.getX() + 4; // Add padding
        int y = this.getY() + 4; // Add padding

        // Draw the placeholder text
        context.drawTextWithShadow(textRenderer, this.placeholder, x, y, this.placeholderColor);
    }

    /**
     * Factory method to create a new PlaceholderTextAreaComponent
     */
    public static PlaceholderTextAreaComponent create(Sizing horizontalSizing, Sizing verticalSizing) {
        return new PlaceholderTextAreaComponent(horizontalSizing, verticalSizing);
    }

    /**
     * Factory method to create a new PlaceholderTextAreaComponent with placeholder text
     */
    public static PlaceholderTextAreaComponent create(Sizing horizontalSizing, Sizing verticalSizing, Text placeholder) {
        return new PlaceholderTextAreaComponent(horizontalSizing, verticalSizing).placeholder(placeholder);
    }
}