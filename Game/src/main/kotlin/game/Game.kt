package game

import game.gameobject.GameObject
import game.gameobject.prefabs.Obstacle
import game.gameobject.prefabs.Player
import game.utils.Constants
import game.utils.GamePanel
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import javax.swing.JFrame


fun main(args: Array<String>) {
    val gameObjects = mutableListOf<GameObject>()
    val objectsPendingRemoval = mutableListOf<GameObject>()
    var isObstacleDown = true

    var obstacleId: Long = 2

    val jumps = mutableListOf<Boolean>()
    val player = object : Player() {
        override fun onDestroy() {
            objectsPendingRemoval.add(this)
        }
    }

    player.id = 0


    val firstObstacle = object : Obstacle(1000 - OBSTACLE_WIDTH / 2, OBSTACLE_HEIGHT / 2, -OBSTACLE_WIDTH - 100) {
        override fun onDestroy() {
            objectsPendingRemoval.add(this)
        }
    }
    firstObstacle.id = 1

    var gameStarted = false
    var gameOver = false

    gameObjects.add(player)
    gameObjects.add(firstObstacle)
    val frame = JFrame()
    frame.size = Dimension(1000, 500)
    frame.setLocationRelativeTo(null)
    frame.isResizable = false


    val panel = GamePanel(gameObjects)
    panel.isFocusable = true
    panel.addKeyListener(object : KeyListener {
        override fun keyTyped(e: KeyEvent?) {

        }

        override fun keyPressed(e: KeyEvent?) {

        }

        override fun keyReleased(e: KeyEvent?) {
            jumps.add(true)
        }

    })

    panel.size = Dimension(1000, 500)
    frame.contentPane = panel

    panel.isVisible = true
    frame.isVisible = true
    panel.requestFocus()


    Thread {

        val imageFolder = File("images")
        imageFolder.mkdir()
        fun createAndAddObstacle() {
            val x = if (isObstacleDown) {
                object : Obstacle(1000 - Obstacle.OBSTACLE_WIDTH / 2, 500 - OBSTACLE_HEIGHT / 2, -Obstacle.OBSTACLE_WIDTH - 100) {
                    override fun onDestroy() {
                        objectsPendingRemoval.add(this)
                    }

                }
            } else {
                object : Obstacle(1000 - Obstacle.OBSTACLE_WIDTH / 2, Obstacle.OBSTACLE_HEIGHT / 2, -Obstacle.OBSTACLE_WIDTH - 100) {
                    override fun onDestroy() {
                        objectsPendingRemoval.add(this)
                    }
                }
            }
            x.id = obstacleId
            obstacleId++
            isObstacleDown = !isObstacleDown
            gameObjects.add(x)
        }

        var lastObstacleSpawnTime = System.currentTimeMillis()
        var currentTime: Long
        var firstUpdate = true
        var lastUpdateTime: Long


        while (true) {
            //cleanup
            for (i in objectsPendingRemoval.size - 1 downTo 0) {
                val x = objectsPendingRemoval[i]
                gameObjects.remove(x)
                objectsPendingRemoval.remove(x)
            }


            panel.requestFocus()
            if (gameStarted && !gameOver) {
                //check for collisions
                val obstacles = gameObjects.filterIndexed({ index, gameObject -> index > 0 })
                for (obstacle in obstacles) {
                    if (player.doesCollideWithOther(obstacle)) {
                        gameOver = true
                        panel.gameOver = true
                    }
                }

                //spawn obstacles if not first update
                if (!firstUpdate) {
                    currentTime = System.currentTimeMillis()
                    if (currentTime - lastObstacleSpawnTime >= 1500) {
                        createAndAddObstacle()
                        lastObstacleSpawnTime = currentTime
                    }

                } else {
                    lastObstacleSpawnTime = System.currentTimeMillis()
                    firstUpdate = false
                }

                if (jumps.isNotEmpty()) {
                    player.jump()
                    jumps.removeAt(jumps.size - 1)
                }


                for (gameObject in gameObjects) {
                    gameObject.onUpdate()
                }

                if (player.y - player.height / 2 < 0 || player.y + player.height / 2 > 500) {
                    gameOver = true
                    panel.gameOver = true
                }

                player.stopJump()
            } else if (!gameOver) {
                if (jumps.isNotEmpty()) {
                    gameStarted = true
                    panel.gameStarted = true
                    jumps.removeAt(jumps.size - 1)
                }
            }
            panel.repaint()

            //TODO separate UI update from physics update
            //sleep until it's time for a new update
            lastUpdateTime = System.currentTimeMillis()
            currentTime = System.currentTimeMillis()

            while (currentTime - lastUpdateTime < 1000L / Constants.FPS){
                Thread.sleep(1)
                currentTime = System.currentTimeMillis()
            }
        }
    }.start()


}

