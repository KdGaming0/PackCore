package com.kd_gaming1.screen.components

import gg.essential.elementa.components.UIContainer
import gg.essential.universal.UMatrixStack
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.CubeMapRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.RotatingCubeMapRenderer
import net.minecraft.util.Identifier

/**
 * A UI component that renders a rotating panorama skybox using custom textures.
 */
class UIPanorama(
    private val panoramaIdentifier: Identifier
) : UIContainer() {

    private val cubeMapRenderer: CubeMapRenderer = CubeMapRenderer(panoramaIdentifier)
    private val rotatingPanoramaRenderer: RotatingCubeMapRenderer = RotatingCubeMapRenderer(cubeMapRenderer)
    private var panoramaTime: Float = 0f
    private var lastTime: Long = System.currentTimeMillis()

    override fun draw(matrixStack: UMatrixStack) {
        super.draw(matrixStack) // Call super for Elementa's drawing pipeline and children

        val mc = MinecraftClient.getInstance()
        val currentWidth = getWidth().toInt().coerceAtLeast(1)
        val currentHeight = getHeight().toInt().coerceAtLeast(1)

        if (currentWidth <= 0 || currentHeight <= 0) {
            return // Don't attempt to draw if dimensions are invalid
        }

        // Create DrawContext. RotatingCubeMapRenderer manages its own matrices.
        val immediate = mc.bufferBuilders.entityVertexConsumers
        val drawContext = DrawContext(mc, immediate)

        // Calculate delta time using system time
        val currentTime = System.currentTimeMillis()
        val deltaMs = currentTime - lastTime
        val deltaTime = deltaMs / 1000.0f * 20.0f // Convert to Minecraft ticks (20 ticks per second)
        lastTime = currentTime

        this.panoramaTime += deltaTime

        // Render the panorama.
        rotatingPanoramaRenderer.render(
            drawContext,
            currentWidth,
            currentHeight,
            1.0f, // Alpha
            this.panoramaTime
        )
    }

    /**
     * Resets the panorama's rotation to its starting point.
     */
    fun resetRotation() {
        this.panoramaTime = 0f
        this.lastTime = System.currentTimeMillis()
    }
}