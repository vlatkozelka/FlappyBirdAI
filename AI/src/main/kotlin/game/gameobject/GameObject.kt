package game.gameobject

import game.gameobject.prefabs.Player
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle


abstract class GameObject() {

    constructor(x: Int, y: Int) : this() {
        this.x = x
        this.y = y
    }

    var speedX = 0
        protected set

    var speedY = 0
        protected set

    var x = 0
        protected set

    var y = 0
        protected set

    var width = 0
        protected set

    var height = 0
        protected set

    var weight = 0
        protected set

    var force: Array<Int> = arrayOf(0, 0)
        protected set

    var color: Color = Color.BLACK
    var id: Long = 0

    val rect: Rectangle
        get() {
            return Rectangle(x - width / 2, y - height / 2, width, height)
        }


    fun doesCollideWithOther(other: GameObject): Boolean {
        return rect.intersects(other.rect)
    }

    /**
     * Updates the object's speed and position
     */
    open fun onUpdate() {
        val accelerationX: Float = (this.force[0].toFloat() / weight.toFloat())
        val accelerationY: Float = -(this.force[1].toFloat() / weight.toFloat()) //negative because SWING is retarded, Y is inverted
        speedX += accelerationX.toInt()
        speedY += accelerationY.toInt()
        x += speedX
        y -= speedY

       /* when(this){
            is Player -> println("player force Y = ${this.force[1]}")
        }*/
    }


    /**
     * Draws the object on the given graphics
     */
    abstract fun onDraw(graphics: Graphics)


    /**
     * called when this object needs to be removed from the scene
     */
    abstract fun onDestroy()
}


