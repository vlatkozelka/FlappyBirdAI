package game.gameobject.prefabs

import game.gameobject.GameObject
import game.utils.Constants.SPEED_X
import java.awt.Color
import java.awt.Graphics

abstract class Obstacle(x: Int, y: Int, private val destroyAtX: Int) : GameObject(x, y) {

    init {
        speedX = -SPEED_X
        speedY = 0
        force = arrayOf(0, 0)
        width = OBSTACLE_WIDTH
        height = OBSTACLE_HEIGHT
        weight = 0
        color = Color.GREEN
    }

    override fun onDraw(graphics: Graphics) {
        graphics.color = color
        graphics.fillRect(x - width / 2, y - height / 2, width, height)
    }

    override fun onUpdate() {
        super.onUpdate()
        if (x <= destroyAtX) {
            onDestroy()
        }
    }


    companion object {
        val OBSTACLE_WIDTH = 100
        val OBSTACLE_HEIGHT = 250
    }

}