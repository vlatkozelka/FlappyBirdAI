package main.kotlin

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.Loader
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacpp.cuda
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator
import org.deeplearning4j.eval.Evaluation
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.jita.conf.CudaEnvironment
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Nesterovs
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File

fun main(args: Array<String>) {
    CudaEnvironment.getInstance().configuration.allowMultiGPU(true)
    try {
        Loader.load(cuda::class.java)
    } catch (e: UnsatisfiedLinkError) {
        val path = Loader.cacheResource(cuda::class.java, "windows-x86_64/jnicuda.dll").path
        ProcessBuilder("c:/depends/depends.exe", path).start().waitFor()
    }




    val numRows = 28 // The number of rows of a matrix.
    val numColumns = 28 // The number of columns of a matrix.
    val outputNum = 10 // Number of possible outcomes (e.g. labels 0 through 9).
    val batchSize = 128 // How many examples to fetch with each step.
    val rngSeed = 123 // This random-number generator applies a seed to ensure that the same initial weights are used when training. We’ll explain why this matters later.
    val numEpochs = 15 // An epoch is a complete pass through a given dataset.

    val mnistTest = MnistDataSetIterator(batchSize, false, rngSeed)


    val modelFile = File("model")
    var aiModel: MultiLayerNetwork? = null
    if (modelFile.exists()) {
        aiModel = MultiLayerNetwork.load(modelFile, false)
    }


    if (aiModel == null) {


        val mnistTrain = MnistDataSetIterator(batchSize, true, rngSeed)

        val configuration = NeuralNetConfiguration.Builder()
                .seed(rngSeed.toLong())
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(Nesterovs(0.006, 0.9))
                .l2(1e-4)
                .list()
                .layer(0, DenseLayer.Builder()
                        .nIn(numRows * numColumns)
                        .nOut(1000)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .build()
                )
                .layer(1, DenseLayer.Builder()
                        .nIn(1000)
                        .nOut(1000)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .build()
                )
                .layer(2, OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(1000)
                        .nOut(outputNum)
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .build()
                )
                .pretrain(false)
                .backprop(true)
                .build()


        aiModel = MultiLayerNetwork(configuration)
        aiModel.init()
        aiModel.setListeners(ScoreIterationListener(1))

        println("Training...")

        for (i in 0..numEpochs) {
            aiModel.fit(mnistTrain)
        }
        aiModel.save(modelFile)
    }

    println("Evaluation...")

    val eval = Evaluation(outputNum) //create an evaluation object with 10 possible classes
    while (mnistTest.hasNext()) {
        val next = mnistTest.next()
        val output = aiModel.output(next.featureMatrix) //get the networks prediction
        eval.eval(next.labels, output) //check the prediction against the true class
    }

    println(eval.stats())


}