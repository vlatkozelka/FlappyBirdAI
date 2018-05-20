package game

import java.util.*

class RandomBenchmark {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val game = Game()
            game.frame.title = "NOT THE AI !!!! CLOSE ME !!!! IDIOT BLYAT !!!!"
            game.resetGame()
            val ran = Random()
            Thread {

                while (true) {
                   // game.nextStep(ran.nextBoolean())

                    Thread.sleep(1000 / 20)
                    if(game.gameOver){
                        game.resetGame()
                    }
                }

            }.start()
        }
    }
}