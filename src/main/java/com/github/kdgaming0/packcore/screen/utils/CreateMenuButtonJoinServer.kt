package com.github.kdgaming0.packcore.screen.utils

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.CopyConstraintFloat
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerData
import java.awt.Color

fun CreateMenuButtonJoinServer(text: String, serverAddress: String, serverPort: Int, action: () -> Unit) = UIContainer().constrain {
        x = CenterConstraint()
        y = SiblingConstraint()
        width = 128.pixels()
        height = 20.pixels()
    }.also { button ->
        // Add text to button
        val buttonText = UIText(text).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.pixels()
            color = Color.WHITE.toConstraint()
        } childOf button

        val underline = UIRoundedRectangle(6f).constrain {
            x = CenterConstraint()
            y = CenterConstraint() + 8.pixels()
            width = CopyConstraintFloat().to(buttonText) as WidthConstraint + 16.pixels()
            height = 0.8.pixels()
            color = Color(252, 189, 56, 0).toConstraint()
        } childOf buttonText

        button.onMouseClick {
            joinServer(serverAddress, serverPort)
        }.onMouseEnter {
            buttonText.animate {
                setTextScaleAnimation(Animations.OUT_EXP, 0.3f, 1.pixels())
            }
            underline.animate {
                setColorAnimation(Animations.OUT_EXP, 0.5f, Color(252, 189, 56, 255).toConstraint())
            }
        }.onMouseLeave {
            buttonText.animate {
                setTextScaleAnimation(Animations.OUT_EXP, 0.3f, 1.pixels())
            }
            underline.animate {
                setColorAnimation(Animations.OUT_EXP, 0.5f, Color(252, 189, 56, 0).toConstraint())
            }
        }
    }

/**
 * Handles the server connection logic.
 *
 * @param address The IP address or domain of the server.
 * @param port The port number of the server.
 */
private fun joinServer(address: String, port: Int) {
    try {
        val minecraft = Minecraft.getMinecraft()

        // Create ServerData with the specified address and port
        val serverData = ServerData("Custom Server", "$address:$port", false)
        serverData.serverIP = "$address:$port"

        // Initiate connection to the server
        minecraft.displayGuiScreen(GuiConnecting(GuiMainMenu(), minecraft, serverData))
    } catch (e: Exception) {
        // Handle potential exceptions, such as invalid server address
        e.printStackTrace()
    }
}