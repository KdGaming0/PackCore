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
import kotlin.math.cos

/**
 * UIPanorama provides a rotating panorama background.
 * It uses a set of panorama images and smoothly transitions them
 * while applying rotations and perspective transformations.
 */
class UIPanorama : UIContainer() {
    private val mc = Minecraft.getMinecraft()
    private var panoramaRotation = 0f

    private val panoramaTextures = arrayOf(
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_0.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_1.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_2.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_3.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_4.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_5.png")
    )

    /**
     * Renders the panorama background by drawing a textured cube
     * and applying rotation based on the current Minecraft timer.
     */
    override fun draw(matrixStack: UMatrixStack) {
        // Use reflection to access Minecraft's timer for smooth rotation
        val timerField = Minecraft::class.java.getDeclaredField("timer").apply {
            isAccessible = true
        }
        val timer = timerField.get(mc) as net.minecraft.util.Timer
        panoramaRotation += timer.renderPartialTicks

        GlStateManager.disableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.depthMask(false)
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        // Enable linear texture filtering for smoother output
        GlStateManager.enableTexture2D()
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

        // Set up a perspective projection for a cube effect
        GlStateManager.pushMatrix()
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GlStateManager.pushMatrix()
        GlStateManager.loadIdentity()
        Project.gluPerspective(
            90.0f,
            mc.displayWidth.toFloat() / mc.displayHeight.toFloat(),
            0.05f,
            10.0f
        )

        // Switch to modelview matrix and apply rotation
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GlStateManager.loadIdentity()
        GlStateManager.rotate(180.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(cos(panoramaRotation * 0.001f) * 5.0f + 5.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(-panoramaRotation * 0.1f, 0.0f, 1.0f, 0.0f)

        // Draw all six faces of the panorama
        for (index in panoramaTextures.indices) {
            GlStateManager.pushMatrix()

            // Rotate each face to its correct position
            when (index) {
                1 -> GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f)
                2 -> GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f)
                3 -> GlStateManager.rotate(-90.0f, 0.0f, 1.0f, 0.0f)
                4 -> GlStateManager.rotate(90.0f, 1.0f, 0.0f, 0.0f)
                5 -> GlStateManager.rotate(-90.0f, 1.0f, 0.0f, 0.0f)
            }

            // Bind panorama texture and enable mipmap filtering
            mc.textureManager.bindTexture(panoramaTextures[index])
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

            // Draw the face
            val tessellator = Tessellator.getInstance()
            val buffer = tessellator.worldRenderer.apply {
                begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
                pos(-1.0, -1.0, 1.0).tex(0.0, 0.0).endVertex()
                pos(1.0, -1.0, 1.0).tex(1.0, 0.0).endVertex()
                pos(1.0, 1.0, 1.0).tex(1.0, 1.0).endVertex()
                pos(-1.0, 1.0, 1.0).tex(0.0, 1.0).endVertex()
            }
            tessellator.draw()

            GlStateManager.popMatrix()
        }

        // Restore the projection matrix
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GlStateManager.popMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GlStateManager.popMatrix()

        // Reset texture filters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        GlStateManager.depthMask(true)
        GlStateManager.enableAlpha()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        // Draw any child components that might be added
        super.draw(matrixStack)
    }
}