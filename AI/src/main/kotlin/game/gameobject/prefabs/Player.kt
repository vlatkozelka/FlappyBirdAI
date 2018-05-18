package game.gameobject.prefabs

import game.gameobject.GameObject
import game.utils.Constants
import java.awt.Color
import java.awt.Graphics

abstract class Player : GameObject() {


    init {
        reset()
    }

    override fun onDraw(graphics: Graphics) {
        graphics.color = color
        graphics.fillOval(x - width / 2, y - height / 2, width, height)
    }

    fun jump() {
        force[1] -= 40 //40 for equal forces
    }

    fun stopJump() {
        force[1] = Constants.GRAVITY * weight
    }

    fun reset() {
        x = 100
        y = 250
        speedX = 0
        speedY = 0
        width = 25
        height = 25
        weight = 10
        force[1] = Constants.GRAVITY * weight
        color = Color.RED
    }

}