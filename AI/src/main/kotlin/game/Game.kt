@file:JvmName("Launcher")

package game

import game.gameobject.GameObject
import game.gameobject.prefabs.Obstacle
import game.gameobject.prefabs.Player
import game.utils.Constants.SPEED_X
import game.utils.GamePanel
import org.deeplearning4j.gym.StepReply
import org.deeplearning4j.rl4j.learning.IHistoryProcessor
import org.deeplearning4j.rl4j.learning.async.a3c.discrete.A3CDiscrete
import org.deeplearning4j.rl4j.learning.async.a3c.discrete.A3CDiscreteConv
import org.deeplearning4j.rl4j.mdp.MDP
import org.deeplearning4j.rl4j.network.ac.ActorCriticFactoryCompGraphStdConv
import org.deeplearning4j.rl4j.space.*
import org.deeplearning4j.rl4j.util.DataManager
import java.awt.Dimension
import java.awt.event.WindowEvent
import java.awt.event.WindowEvent.WINDOW_CLOSING
import javax.swing.JFrame
import javax.swing.WindowConstants.EXIT_ON_CLOSE


class Game : MDP<GamePanel, Int, ActionSpace<Int>> {
    val screenWidth = SCREEN_WIDTH
    val screenHeight = SCREEN_HEIGHT

    val gameObjects = mutableListOf<GameObject>()
    val objectsPendingRemoval = mutableListOf<GameObject>()
    val jumps = mutableListOf<Boolean>()
    var isObstacleDown = true
    var gameOver = false
    var obstacleId: Long = 2
    val player = object : Player() {
        override fun onDestroy() {
            objectsPendingRemoval.add(this)
        }
    }

    var score = 0
    var ticksSinceLastSpawn = 0

    private val screenBuffer: ByteArray


    val panel: GamePanel

    val frame: JFrame

    protected var gameObservationSpace: ObservationSpace<GamePanel>

    /**
     * The actions the bot can take are only 0 or 1 (jump, don't jump)
     */
    val actions = arrayOf(0, 1)

    val discreteSpace: DiscreteSpace


    var shape: IntArray

    init {
        frame = JFrame("Flappy Bird AI")
        frame.size = Dimension(screenWidth, screenHeight)
        frame.setLocationRelativeTo(null)
        frame.isResizable = false
        frame.defaultCloseOperation = EXIT_ON_CLOSE

        panel = GamePanel(gameObjects)
        panel.isFocusable = true


        panel.size = Dimension(1000, 500)
        frame.contentPane = panel

        panel.isVisible = true
        frame.isVisible = true

        shape = intArrayOf(screenHeight, screenWidth, 3)

        discreteSpace = DiscreteSpace(actions.size)

        gameObservationSpace = ArrayObservationSpace<GamePanel>(shape)

        screenBuffer = ByteArray(shape[0] * shape[1] * shape[2])


    }

    fun closeGame() {
        frame.dispatchEvent(WindowEvent(frame, WINDOW_CLOSING))
    }

    fun resetGame() {
        ticksSinceLastSpawn = 0
        score = 0
        gameObjects.clear()
        objectsPendingRemoval.clear()
        player.reset()
        obstacleId = 2
        isObstacleDown = true
        val firstObstacle = object : Obstacle(1000 - OBSTACLE_WIDTH / 2, OBSTACLE_HEIGHT / 2, -OBSTACLE_WIDTH - 100) {
            override fun onDestroy() {
                objectsPendingRemoval.add(this)
            }
        }
        firstObstacle.id = 1
        gameObjects.add(player)
        gameObjects.add(firstObstacle)
        gameOver = false
    }

    fun nextStep(doJump: Boolean): Double {
        ticksSinceLastSpawn++
        jumps.add(true)
        fun createAndAddObstacle() {
            val x = if (isObstacleDown) {
                object : Obstacle(screenWidth - Obstacle.OBSTACLE_WIDTH / 2, screenHeight - OBSTACLE_HEIGHT / 2, -Obstacle.OBSTACLE_WIDTH - 100) {
                    override fun onDestroy() {
                        objectsPendingRemoval.add(this)
                    }

                }
            } else {
                object : Obstacle(screenWidth - Obstacle.OBSTACLE_WIDTH / 2, Obstacle.OBSTACLE_HEIGHT / 2, -Obstacle.OBSTACLE_WIDTH - 100) {
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

        //cleanup
        for (i in objectsPendingRemoval.size - 1 downTo 0) {
            val x = objectsPendingRemoval[i]
            gameObjects.remove(x)
            objectsPendingRemoval.remove(x)
        }

        if (!gameOver) {
            //check for collisions
            val obstacles = gameObjects.filterIndexed({ index, gameObject -> index > 0 })
            for (obstacle in obstacles) {
                if (player.doesCollideWithOther(obstacle)) {
                    gameOver = true
                    panel.gameOver = true
                }
            }

            //spawn obstacles if not first update


            if (SPEED_X * ticksSinceLastSpawn > Obstacle.OBSTACLE_WIDTH + Obstacle.OBSTACLE_WIDTH / 2) {
                createAndAddObstacle()
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
                jumps.removeAt(jumps.size - 1)
            }
        }
        panel.repaint()

        return if (gameOver) {
            -1000.0
        } else {
            1.0
        }

    }


    //MDP methods
    override fun getActionSpace(): ActionSpace<Int> {
        return discreteSpace
    }

    override fun getObservationSpace(): ObservationSpace<GamePanel> {
        return gameObservationSpace
    }

    override fun isDone(): Boolean {
        return gameOver
    }

    override fun newInstance(): MDP<GamePanel, Int, ActionSpace<Int>> {
        return Game()
    }

    override fun reset(): GamePanel {
        resetGame()
        return panel
    }

    override fun close() {
        closeGame()
    }

    override fun step(action: Int?): StepReply<GamePanel> {
        val reward = nextStep(action == 1)
        return StepReply(panel, reward, gameOver, null)
    }


    companion object {

        const val SCREEN_WIDTH = 1000
        const val SCREEN_HEIGHT = 500

        var ALE_HP = IHistoryProcessor.Configuration(
                4, //History length
                SCREEN_WIDTH / 10, //resize width
                SCREEN_HEIGHT / 10, //resize height
                SCREEN_WIDTH / 10, //crop width
                SCREEN_HEIGHT / 10, //crop height
                0, //cropping x offset
                0, //cropping y offset
                1        //skip mod (one frame is picked every x
        )

        var ALE_A3C = A3CDiscrete.A3CConfiguration(
                123, //Random seed
                10000, //Max step By epoch
                8000000, //Max step
                8, //Number of threads
                32, //t_max
                500, //num step noop warmup
                0.1, //reward scaling
                0.99, //gamma
                10.0            //td-error clipping
        )


        val ALE_NET_A3C = ActorCriticFactoryCompGraphStdConv.Configuration(
                0.00025, //learning rate
                null, null, false
        )


        @JvmStatic
        fun main(args: Array<String>) {

            //record the training data in rl4j-data in a new folder
            val manager = DataManager(true)

            val game = Game()


            //setup the training
            val a3c = A3CDiscreteConv(game, ALE_NET_A3C, ALE_HP, ALE_A3C, manager)

            //start the training
            a3c.train()

            //save the model at the end
            a3c.getPolicy().save("ale-a3c.model")

            //close the ALE env
            game.close()


        }

    }


}


