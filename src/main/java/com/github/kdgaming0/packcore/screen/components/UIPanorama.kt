package com.github.kdgaming0.packcore.screen.components

import gg.essential.elementa.components.UIContainer
import gg.essential.universal.UMatrixStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.Project

class UIPanorama : UIContainer() {

    private val mc = Minecraft.getMinecraft()
    var panoramaTimer = 0  // Drives rotation

    // Your custom panorama textures (ensure these exist)
    private val panoramaTextures = arrayOf(
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_0.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_1.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_2.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_3.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_4.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_5.png")
    )

    override fun draw(matrixStack: UMatrixStack) {
        // Update timer for animation
        panoramaTimer++

        // Using a default partialTicks value since UIContainer doesn't provide one
        val partialTicks = 0.0f

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        // Use a fixed aspect ratio (as before) to reproduce the vanilla perspective:
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        Project.gluPerspective(90.0f, mc.displayWidth.toFloat() / mc.displayHeight.toFloat(), 0.05f, 10.0f)

        // Now set up the modelview matrix
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        // Apply the initial rotations:
        GL11.glRotatef(180.0f, 1.0f, 0.0f, 0.0f)

        // Apply additional rotation for the animation
        GL11.glRotatef(-((panoramaTimer.toFloat() + partialTicks) * 0.05f), 0.0f, 1.0f, 0.0f)

        // Configure GL state for smooth rendering.
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.disableCull()
        GlStateManager.depthMask(false)
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)

        // Draw each of the six cube faces in a single pass (no blur iterations)
        for (k in 0 until 6) {
            GL11.glPushMatrix()
            when (k) {
                1 -> GL11.glRotatef(90.0f, 0.0f, 1.0f, 0.0f)
                2 -> GL11.glRotatef(180.0f, 0.0f, 1.0f, 0.0f)
                3 -> GL11.glRotatef(-90.0f, 0.0f, 1.0f, 0.0f)
                4 -> GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f)
                5 -> GL11.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)
            }

            // Bind the texture for this face, cycling through the provided textures if needed
            val texture = panoramaTextures[k % panoramaTextures.size]
            mc.textureManager.bindTexture(texture)

            // Begin drawing the face quad
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)
            // Use full opacity since we're not using iterative blending for blur
            val alpha = 255
            worldRenderer.pos(-1.0, -1.0, 1.0)
                .tex(0.0, 0.0)
                .color(255, 255, 255, alpha)
                .endVertex()
            worldRenderer.pos(1.0, -1.0, 1.0)
                .tex(1.0, 0.0)
                .color(255, 255, 255, alpha)
                .endVertex()
            worldRenderer.pos(1.0, 1.0, 1.0)
                .tex(1.0, 1.0)
                .color(255, 255, 255, alpha)
                .endVertex()
            worldRenderer.pos(-1.0, 1.0, 1.0)
                .tex(0.0, 1.0)
                .color(255, 255, 255, alpha)
                .endVertex()
            tessellator.draw()

            GL11.glPopMatrix()
        }

        // Restore GL state
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPopMatrix()

        GlStateManager.depthMask(true)
        GlStateManager.enableCull()
        GlStateManager.enableDepth()
        GlStateManager.enableAlpha()

        // Render child UI elements on top (if any)
        super.draw(matrixStack)
    }
}