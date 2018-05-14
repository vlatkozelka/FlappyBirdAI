package game.utils

import game.gameobject.GameObject
import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.JPanel

class GamePanel(private val gameObjects: MutableList<GameObject>) : JPanel() {

    var gameStarted = false
    var gameOver = false
    var screenshot: BufferedImage? = null
        private set

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        screenshot = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val sg = screenshot?.graphics
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
        if (!gameStarted) {
            g?.color = Color.BLACK
            g?.drawString("Press SPACE to start game!", this.width / 2, this.height / 2)
            sg?.color = Color.BLACK
            sg?.drawString("Press SPACE to start game!", this.width / 2, this.height / 2)
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