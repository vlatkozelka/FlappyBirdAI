@file:JvmName("Launcher")

package game

import game.gameobject.GameObject
import game.gameobject.prefabs.Obstacle
import game.gameobject.prefabs.Player
import game.utils.Constants.SPEED_X
import game.utils.GamePanel
import org.deeplearning4j.gym.StepReply
import org.deeplearning4j.rl4j.learning.IHistoryProcessor
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteConv
import org.deeplearning4j.rl4j.mdp.MDP
import org.deeplearning4j.rl4j.network.dqn.DQN
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdConv
import org.deeplearning4j.rl4j.space.ArrayObservationSpace
import org.deeplearning4j.rl4j.space.DiscreteSpace
import org.deeplearning4j.rl4j.space.ObservationSpace
import org.deeplearning4j.rl4j.util.DataManager
import org.nd4j.linalg.learning.config.Adam
import java.awt.Color
import java.awt.Dimension
import java.awt.event.WindowEvent
import java.awt.event.WindowEvent.WINDOW_CLOSING
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.WindowConstants.EXIT_ON_CLOSE


class Game : MDP<GamePanel, Int, DiscreteSpace> {
    val screenWidth = SCREEN_WIDTH
    val screenHeight = SCREEN_HEIGHT

    val gameObjects = mutableListOf<GameObject>()
    val objectsPendingRemoval = mutableListOf<GameObject>()
    var isObstacleDown = true
    var gameOver = false
    var obstacleId: Long = 2
    val player = object : Player() {
        override fun onDestroy() {
            objectsPendingRemoval.add(this)
        }
    }

    var score = 0
    var highScore = 0
    var ticksSinceLastSpawn = 0
    var ticksSinceLastJump = 0
    var didJump = false
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
        println("opened new frame")
        frame = JFrame("Flappy Bird AI")
        frame.size = Dimension(screenWidth + 20, screenHeight + 50)
        frame.setLocationRelativeTo(null)
        frame.isResizable = false
        frame.defaultCloseOperation = EXIT_ON_CLOSE

        panel = GamePanel(gameObjects)
        panel.background = Color.BLACK
        panel.isFocusable = true


        panel.size = Dimension(screenWidth, screenHeight)
        frame.contentPane = panel

        panel.isVisible = true
        frame.isVisible = true

        shape = intArrayOf(screenHeight, screenWidth, 3)

        discreteSpace = DiscreteSpace(actions.size)

        gameObservationSpace = ArrayObservationSpace<GamePanel>(shape)

        screenBuffer = ByteArray(shape[0] * shape[1] * shape[2])

        resetGame()


    }

    fun closeGame() {
        println("closing game")
        frame.dispatchEvent(WindowEvent(frame, WINDOW_CLOSING))
    }

    fun resetGame() {
        println("calling reset")
        ticksSinceLastJump = 0
        ticksSinceLastSpawn = 0
        score = 0
        gameObjects.clear()
        objectsPendingRemoval.clear()
        player.reset()
        obstacleId = 2
        isObstacleDown = true
        val firstObstacle = object : Obstacle(SCREEN_WIDTH + OBSTACLE_WIDTH / 2, OBSTACLE_HEIGHT / 2, true, -OBSTACLE_WIDTH - 100) {
            override fun onDestroy() {
                objectsPendingRemoval.add(this)
            }
        }
        firstObstacle.id = 1
        gameObjects.add(player)
        gameObjects.add(firstObstacle)
        gameOver = false
        panel.gameOver = false
    }

    fun nextStep(doJump: Boolean): Double {
        var reward = 1.0
        ticksSinceLastJump++

        /*val jump = doJump  && ticksSinceLastJump > 6
        if (jump) {
            ticksSinceLastJump = 0
            println("bot was allowed to jump")
        }else if(doJump){
            println("bot tried to jump but wasnt allowed to")
        }*/

        ticksSinceLastSpawn++
        fun createAndAddObstacle() {
            ticksSinceLastSpawn = 0
            val x = if (isObstacleDown) {
                object : Obstacle(screenWidth + OBSTACLE_WIDTH / 2, screenHeight - OBSTACLE_HEIGHT / 2, !isObstacleDown, -Obstacle.OBSTACLE_WIDTH - 100) {
                    override fun onDestroy() {
                        objectsPendingRemoval.add(this)
                    }

                }
            } else {
                object : Obstacle(screenWidth + OBSTACLE_WIDTH / 2, Obstacle.OBSTACLE_HEIGHT / 2, !isObstacleDown, -Obstacle.OBSTACLE_WIDTH - 100) {
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
            val obstacles = gameObjects.filterIndexed({ index, _ -> index > 0 })
            val playerPosX = player.x + player.width / 2.0
            var pipePos: Double
            for (obstacle in obstacles) {
                if (player.doesCollideWithOther(obstacle)) {
                    gameOver = true
                    panel.gameOver = true
                }
                if (!gameOver) {
                    pipePos = obstacle.x + obstacle.width / 2.0
                    if (pipePos <= playerPosX && playerPosX <= pipePos + 15) {
                        println("Bot just got a reward of 1000 for passing an obstacle!")
                        reward = 1000.0
                    }
                }

            }

            //spawn obstacles if not first update


            if (SPEED_X * ticksSinceLastSpawn > Obstacle.OBSTACLE_WIDTH + Obstacle.OBSTACLE_WIDTH * 2) {
                createAndAddObstacle()
            }


            if (doJump) {
                player.jump()
            }


            for (gameObject in gameObjects) {
                gameObject.onUpdate()
            }

            //println("force Y ${player.force[1]}")
            // println("speed Y ${player.speedY}")


            if (player.y - player.height / 2 < 0 || player.y + player.height / 2 > 500) {
                gameOver = true
                panel.gameOver = true
            }

            player.stopJump()
        } else if (!gameOver) {

        }
        panel.repaint()

        if (!gameOver) {
            score++
        } else {
            highScore = Math.max(score, highScore)
            println("game over, score: $score!")
            println("Highest score: $highScore")
        }

        if (gameOver) {
            reward = -1000.0
        }

        return reward
    }


    //MDP methods
    override fun getActionSpace(): DiscreteSpace {
        return discreteSpace
    }

    override fun getObservationSpace(): ObservationSpace<GamePanel> {
        return gameObservationSpace
    }

    override fun isDone(): Boolean {
        return gameOver
    }

    override fun newInstance(): MDP<GamePanel, Int, DiscreteSpace> {
        println("called new instance")
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
        //println("calling step with action $action")
        //println("Game over value is $gameOver")
        //val reward = nextStep(action == 1)
        val reward = nextStep(actions[action ?: 0] == 1)
        //println("rewarded $reward")
        return StepReply(panel, reward, gameOver, null)
    }


    companion object {
        const val SCREEN_WIDTH = 1000
        const val SCREEN_HEIGHT = 500

        var Flappy_QL = QLearning.QLConfiguration(
                123, //Random seed
                Int.MAX_VALUE, //Number of steps before BOT resets the game
                Int.MAX_VALUE, //Number of rounds before BOT stops the game
                50000, //Max size of experience replay
                32, //size of batches
                500, //target update (hard)
                10, //num step noop warmup
                1.0, //reward scaling
                0.99, //gamma
                1.0, //td-error clipping
                0.1f, //min epsilon
                10000, //num step for eps greedy anneal
                true //double DQN
        )

        var FLAPPY_NET = DQNFactoryStdConv.Configuration(
                0.005, //learning rate
                0.0,//l2 regularization
                Adam(0.005) // updater
                , null// Listeners
        )


        var FLAPPY_HPROC = IHistoryProcessor.Configuration(
                1, // Number of frames
                100, // Scaled width
                50, // Scaled height
                100, // Cropped width
                50, // Cropped height
                0, // X offset
                0, // Y offset
                1 // Number of frames to skip
        )

        @JvmStatic
        fun main(args: Array<String>) {
            val dialogButton = JOptionPane.YES_NO_OPTION
            val dataManager = DataManager()
            val mdp: Game
            var dql: QLearningDiscreteConv<GamePanel>? = null

            var option = JOptionPane.showConfirmDialog(null,
                    "Would you like to load a saved network", "Flappy Bird AI", dialogButton)
            if (option == 0) {
                val fileChooser = JFileChooser()
                fileChooser.showOpenDialog(null)
                val loadFile = fileChooser.selectedFile
                val dqn = DQN.load(loadFile.absolutePath)
                option = JOptionPane.showConfirmDialog(null,
                        "Would you like to continue training?", "Flappy Bird AI", dialogButton)
                if (option == 1) {
                    mdp = Game()
                    dql = QLearningDiscreteConv<GamePanel>(mdp, dqn, FLAPPY_HPROC, Flappy_QL, dataManager)
                    dql.train()
                    //dql.policy.save("dqlpolicy")
                    mdp.close()
                } else {
                    mdp = Game()
                    dql = QLearningDiscreteConv<GamePanel>(mdp, dqn, FLAPPY_HPROC, Flappy_QL, dataManager)
                    dql.policy.play(mdp)
                }
            } else {
                mdp = Game()
                val saveFrame = JFrame()
                saveFrame.setSize(200, 200)
                saveFrame.isResizable = false
                val saveButton = JButton("Save neural network")
                saveButton.setSize(200, 200)
                saveFrame.contentPane = saveButton
                saveButton.addActionListener({
                    val fileChooser = JFileChooser()
                    fileChooser.showOpenDialog(saveFrame)
                    val f = fileChooser.selectedFile
                    val out = f.outputStream()
                    dql?.neuralNet?.save(out)
                    out.close()
                })

                saveFrame.isVisible = true
                saveFrame.setLocationRelativeTo(null)
                dql = QLearningDiscreteConv<GamePanel>(mdp, FLAPPY_NET, FLAPPY_HPROC, Flappy_QL, dataManager)
                dql.train()
                // dql.policy.save("dqlpolicy")
                mdp.close()
            }
        }

    }


}


