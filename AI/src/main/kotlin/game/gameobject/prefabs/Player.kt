package game.gameobject.prefabs

import game.gameobject.GameObject
import game.utils.Constants
import java.awt.Color
import java.awt.Graphics
import java.io.File
import javax.imageio.ImageIO

abstract class Player : GameObject() {


    init {
        reset()
        texture = ImageIO.read(File("textures/bird.png"))
        texture = resizeToTexture(texture)
    }

    override fun onDraw(graphics: Graphics) {
        graphics.drawImage(texture, x - width / 2, y - height / 2, null)
    }

    override fun onDrawBasic(graphics: Graphics) {
        graphics.color = color
        graphics.fillRect(x - width / 2, y - height / 2, width, height)
    }

    fun jump() {
        force[1] -= 40 //40 for equal forces
    }

    fun stopJump() {
        force[1] = Constants.GRAVITY * weight
    }

    fun move(action: Int) {
        speedY = action*10
    }

    fun stopMove() {
        speedY = 0
    }

    fun reset() {
        x = 100
        y = 250
        speedX = 0
        speedY = 0
        width = 35
        height = 35
        weight = 10
        force[1] = Constants.GRAVITY * weight
        color = Color.RED
    }

}