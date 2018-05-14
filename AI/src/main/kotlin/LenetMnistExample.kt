package org.deeplearning4j.examples.convolution

import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.*
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Nesterovs
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Created by agibsonccc on 9/16/15.
 */
class LenetMnistExample {
}

fun main(args: Array<String>) {
    val nChannels = 1 // Number of input channels
    val outputNum = 10 // The number of possible outcomes
    val batchSize = 64 // Test batch size
    val nEpochs = 1 // Number of training epochs
    val seed = 123 //

    /*
        Create an iterator using the batch size for one iteration
     */
    println("Load data....")
    val mnistTrain = MnistDataSetIterator(batchSize, true, 12345)
    val mnistTest = MnistDataSetIterator(batchSize, false, 12345)

    /*
        Construct the neural network
     */
    println("Build model....")

    // learning rate schedule in the form of <Iteration #, Learning Rate>
    val lrSchedule = HashMap<Int, Double>()
    lrSchedule[0] = 0.01
    lrSchedule[1000] = 0.005
    lrSchedule[3000] = 0.001

    val conf = NeuralNetConfiguration.Builder()
            .seed(seed.toLong())
            .l2(0.0005)
            .weightInit(WeightInit.XAVIER)
            .updater(Nesterovs(0.01, 0.9))
            .list()
            .layer(0, ConvolutionLayer.Builder(5, 5)
                    //nIn and nOut specify depth. nIn here is the nChannels and nOut is the number of filters to be applied
                    .nIn(nChannels)
                    .stride(1, 1)
                    .nOut(20)
                    .activation(Activation.IDENTITY)
                    .build())
            .layer(1, SubsamplingLayer.Builder(PoolingType.MAX)
                    .kernelSize(2, 2)
                    .stride(2, 2)
                    .build())
            .layer(2, ConvolutionLayer.Builder(5, 5)
                    //Note that nIn need not be specified in later layers
                    .stride(1, 1)
                    .nOut(50)
                    .activation(Activation.IDENTITY)
                    .build())
            .layer(3, SubsamplingLayer.Builder(PoolingType.MAX)
                    .kernelSize(2, 2)
                    .stride(2, 2)
                    .build())
            .layer(4, DenseLayer.Builder().activation(Activation.RELU)
                    .nOut(500).build())
            .layer(5, OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                    .nOut(outputNum)
                    .activation(Activation.SOFTMAX)
                    .build())
            .setInputType(InputType.convolutionalFlat(28, 28, 1)) //See note below
            .backprop(true).pretrain(false).build()

    /*
    Regarding the .setInputType(InputType.convolutionalFlat(28,28,1)) line: This does a few things.
    (a) It adds preprocessors, which handle things like the transition between the convolutional/subsampling layers
        and the dense layer
    (b) Does some additional configuration validation
    (c) Where necessary, sets the nIn (number of input neurons, or input depth in the case of CNNs) values for each
        layer based on the size of the previous layer (but it won't override values manually set by the user)

    InputTypes can be used with other layer types too (RNNs, MLPs etc) not just CNNs.
    For normal images (when using ImageRecordReader) use InputType.convolutional(height,width,depth).
    MNIST record reader is a special case, that outputs 28x28 pixel grayscale (nChannels=1) images, in a "flattened"
    row vector format (i.e., 1x784 vectors), hence the "convolutionalFlat" input type used here.
    */

    val model = MultiLayerNetwork(conf)
    model.init()


    println("Train model....")
    model.setListeners(ScoreIterationListener(10)) //Print score every 10 iterations
    for (i in 0 until nEpochs) {
        model.fit(mnistTrain)
        println("*** Completed epoch $i ***")

        println("Evaluate model....")
        val eval = model.evaluate(mnistTest)
        println(eval.stats())
        mnistTest.reset()
    }
    println("****************Example finished********************")
}

