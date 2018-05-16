package game.utils

import game.Game
import game.gameobject.GameObject
import org.deeplearning4j.rl4j.space.Encodable
import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.JPanel

class GamePanel(private val gameObjects: MutableList<GameObject>) : JPanel(), Encodable {

    override fun toArray(): DoubleArray {
        val array = DoubleArray(Game.SCREEN_WIDTH * Game.SCREEN_HEIGHT * 3)
        var counter = 0
        var argb: Int
        for (i in 0 until Game.SCREEN_WIDTH) {
            for (j in 0 until Game.SCREEN_HEIGHT) {
                try {
                    argb = screenshot.getRGB(i, j)
                    val r = argb shr 16 and 0xFF
                    val g = argb shr 8 and 0xFF
                    val b = argb shr 0 and 0xFF

                    array[counter] = r.toDouble()
                    array[counter + 1] = g.toDouble()
                    array[counter + 2] = b.toDouble()
                    counter++
                } catch (ex: ArrayIndexOutOfBoundsException) {
                    ex.printStackTrace()
                }
            }
        }
        return array
    }

    var gameOver = false
    lateinit var screenshot: BufferedImage
        private set

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        screenshot = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val sg = screenshot.graphics
        val tmpColor = g?.color ?: Color.lightGray

        for (gameObject in gameObjects) {
            g?.color = gameObject.color
            sg?.color = gameObject.color
            g?.let {
                gameObject.onDraw(it)
            }
            sg?.let {
                gameObject.onDraw(it)
            }
        }

        if (gameOver) {
            g?.color = Color.RED
            sg?.color = Color.RED
            g?.drawString("GAME OVER!", this.width / 2, this.height / 2)
            sg?.drawString("GAME OVER!", this.width / 2, this.height / 2)

        }


        g?.color = tmpColor
        sg?.color = tmpColor
    }


}