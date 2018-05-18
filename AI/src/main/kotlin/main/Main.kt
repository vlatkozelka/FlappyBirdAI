package main

import game.Game
import game.utils.GamePanel
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteConv
import org.deeplearning4j.rl4j.util.DataManager
import java.io.File
import java.util.*

class Main {
    companion object {
        val sc = Scanner(System.`in`)
        @JvmStatic
        fun main(args: Array<String>) {
            val options = arrayOf("1", "2", "3", "4")
            var line: String
            do {
                println("""
                Please Enter:
                1 to start a new QDN
                2 to load a QDN
                3 to test a QDN (no learning)
                4 to run the benchmark (a stupid bot that does random shit)

                """)
                line = sc.nextLine()
            } while (!options.any { it == line })


            when (line) {
                "1" -> startQDN()
                "2" -> loadQDN()
                "3" -> testQDN()
                "4" -> runBenchmark()
            }
        }


        @JvmStatic
        fun startQDN() {
            var file = File("batata")
            var line: String
            do {
                println("Please enter path to save QDN")
                line = sc.nextLine()
                if (line == "stop") return
                file = File(line)
            } while (!file.isDirectory)

            file = File("batata")

            var didFirstTry = false
            do {
                if (didFirstTry) {
                    println("Please enter the name of the file to save the QDN")
                } else {
                    println("File already found, please enter a different name")
                }
                didFirstTry = true
                line = sc.nextLine()
                if (line == "stop") return
                file = File(line)
            } while (file.exists())

            val dataManager = DataManager()

            val mdp = Game()

            val dql = QLearningDiscreteConv<GamePanel>(mdp, Game.FLAPPY_NET, Game.FLAPPY_HPROC, Game.Flappy_QL, dataManager)

            dql.train()

            val policy = dql.policy

            policy.save("dqlpolicy")

            mdp.close()



        }


        @JvmStatic
        fun loadQDN() {

        }


        @JvmStatic
        fun testQDN() {

        }


        @JvmStatic
        fun runBenchmark() {

        }

    }
}