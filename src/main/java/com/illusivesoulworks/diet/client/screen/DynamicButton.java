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
/*
 * Modified 2026 by DEHunter58711 for the unofficial NeoForge 26.3 port of Diet
 * (original: Copyright (C) 2021-2023 Illusive Soulworks, LGPL-3.0-or-later).
 * - AbstractButton instead of texture-region ImageButton, deferred tooltip
 */




package com.illusivesoulworks.diet.client.screen;

import com.illusivesoulworks.diet.client.DietClientEvents;
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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Port: ImageButton with a raw texture region no longer exists in 26.3 (sprites only), so this
 * is an AbstractButton that draws the same 20x18 region of textures/gui/icons.png (hover state
 * 19 px lower). Position follows the container screen every frame like the original.
 */
public class DynamicButton extends AbstractButton {

  private final AbstractContainerScreen<?> containerScreen;
  private final Identifier texture;
  private final int xTexStart;
  private final int yTexStart;
  private final int yDiffTex;
  private final Consumer<DynamicButton> onPress;

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
  protected void extractContents(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX,
                                 int mouseY, float partialTicks) {
    this.setX(
        ClientServices.INSTANCE.getGuiLeft(this.containerScreen) + DietConfig.CLIENT.buttonX.get());
    this.setY(
        ClientServices.INSTANCE.getGuiTop(this.containerScreen) + DietConfig.CLIENT.buttonY.get() +
            83);
    int v = this.yTexStart + (this.isHoveredOrFocused() ? this.yDiffTex : 0);
    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, this.getX(), this.getY(),
        this.xTexStart, v, this.width, this.height, 256, 256);

    if (this.isHovered()) {
      List<Component> tooltips = DietTooltip.getEffects();

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
