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

class UIPanorama : UIContainer() {
    private val mc = Minecraft.getMinecraft()
    private var panoramaRotation = 0f
    private var lastUpdateTime = System.currentTimeMillis()

    private val panoramaTextures = arrayOf(
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_0.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_1.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_2.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_3.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_4.png"),
        ResourceLocation("packcore", "textures/gui/panorama_skyblock_hub/panorama_5.png")
    )

    override fun draw(matrixStack: UMatrixStack) {
        // Calculate delta time for smooth rotation without using timer field
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime

        // Update rotation based on time
        panoramaRotation += deltaTime * 20f // Adjust the multiplier to control rotation speed

        GlStateManager.disableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.depthMask(false)
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        // Rest of the code remains the same...
        GlStateManager.enableTexture2D()
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

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

        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GlStateManager.loadIdentity()
        GlStateManager.rotate(180.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(cos(panoramaRotation * 0.001f) * 5.0f + 5.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(-panoramaRotation * 0.1f, 0.0f, 1.0f, 0.0f)

        // Draw all six faces of the panorama
        for (index in panoramaTextures.indices) {
            GlStateManager.pushMatrix()

            when (index) {
                1 -> GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f)
                2 -> GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f)
                3 -> GlStateManager.rotate(-90.0f, 0.0f, 1.0f, 0.0f)
                4 -> GlStateManager.rotate(90.0f, 1.0f, 0.0f, 0.0f)
                5 -> GlStateManager.rotate(-90.0f, 1.0f, 0.0f, 0.0f)
            }

            mc.textureManager.bindTexture(panoramaTextures[index])
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

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

        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GlStateManager.popMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GlStateManager.popMatrix()

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        GlStateManager.depthMask(true)
        GlStateManager.enableAlpha()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        super.draw(matrixStack)
    }
}