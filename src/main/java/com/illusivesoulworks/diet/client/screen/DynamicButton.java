/*
 * Copyright (C) 2021-2023 Illusive Soulworks
 *
 * Diet is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Diet is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Diet.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.illusivesoulworks.diet.client.screen;

import com.illusivesoulworks.diet.common.config.DietConfig;
import com.illusivesoulworks.diet.platform.ClientServices;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Port: ImageButton with a raw texture region no longer exists in 26.3 (sprites only), so this
 * is an AbstractButton that draws the same 20x18 region of textures/gui/icons.png (hover state
 * 19 px lower; resource packs can replace icons.png). Position follows the container screen every
 * frame like the original (e.g. when the recipe book moves the inventory).
 * Port feature (#164, #190): Shift + drag moves the button; the offset is saved to the client
 * config (buttonX / buttonY) when the mouse is released.
 */
public class DynamicButton extends AbstractButton {

  private final AbstractContainerScreen<?> containerScreen;
  private final Identifier texture;
  private final int xTexStart;
  private final int yTexStart;
  private final int yDiffTex;
  private final Consumer<DynamicButton> onPress;
  private boolean dragging = false;
  private double dragX;
  private double dragY;

  public DynamicButton(AbstractContainerScreen<?> screenIn, int xIn, int yIn, int widthIn,
                       int heightIn, int xTexStartIn, int yTexStartIn, int yDiffTextIn,
                       Identifier resourceLocationIn, Consumer<DynamicButton> onPressIn) {
    super(xIn, yIn, widthIn, heightIn, Component.translatable("gui.diet.title"));
    this.containerScreen = screenIn;
    this.texture = resourceLocationIn;
    this.xTexStart = xTexStartIn;
    this.yTexStart = yTexStartIn;
    this.yDiffTex = yDiffTextIn;
    this.onPress = onPressIn;
  }

  @Override
  public void onPress(@Nonnull InputWithModifiers input) {
    this.onPress.accept(this);
  }

  @Override
  public boolean mouseClicked(@Nonnull MouseButtonEvent event, boolean doubleClick) {

    if (this.isActive() && this.isValidClickButton(event.buttonInfo()) &&
        this.isMouseOver(event.x(), event.y()) && event.hasShiftDown() &&
        DietConfig.CLIENT.buttonDragging.get()) {
      this.dragging = true;
      this.dragX = DietConfig.CLIENT.buttonX.get();
      this.dragY = DietConfig.CLIENT.buttonY.get();
      return true;
    }
    return super.mouseClicked(event, doubleClick);
  }

  @Override
  protected void onDrag(@Nonnull MouseButtonEvent event, double dx, double dy) {

    if (this.dragging) {
      this.dragX += dx;
      this.dragY += dy;
      // live preview; clamped to the config range
      DietConfig.CLIENT.buttonX.set(clamp((int) Math.round(this.dragX)));
      DietConfig.CLIENT.buttonY.set(clamp((int) Math.round(this.dragY)));
    }
  }

  @Override
  public void onRelease(@Nonnull MouseButtonEvent event) {

    if (this.dragging) {
      this.dragging = false;
      DietConfig.CLIENT_SPEC.save();
    }
  }

  private static int clamp(int value) {
    return Math.max(-10000, Math.min(10000, value));
  }

  @Override
  protected void extractContents(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX,
                                 int mouseY, float partialTicks) {
    this.setX(
        ClientServices.INSTANCE.getGuiLeft(this.containerScreen) + DietConfig.CLIENT.buttonX.get());
    this.setY(
        ClientServices.INSTANCE.getGuiTop(this.containerScreen) + DietConfig.CLIENT.buttonY.get() +
            83);
    int v = this.yTexStart + (this.isHoveredOrFocused() || this.dragging ? this.yDiffTex : 0);
    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, this.getX(), this.getY(),
        this.xTexStart, v, this.width, this.height, 256, 256);

    if (this.isHovered() && !this.dragging) {
      List<Component> tooltips = DietTooltip.getEffects();

      if (DietConfig.CLIENT.buttonDragging.get()) {
        tooltips = new java.util.ArrayList<>(tooltips);

        if (!tooltips.isEmpty()) {
          tooltips.add(Component.empty());
        }
        tooltips.add(Component.translatable("tooltip.diet.button.move")
            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
      }

      if (!tooltips.isEmpty()) {
        // Port: deferred tooltip API replaces the ScreenEvent.Render.Post workaround.
        guiGraphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, tooltips,
            mouseX, mouseY);
      }
    }
  }

  @Override
  protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
    this.defaultButtonNarrationText(output);
  }
}
