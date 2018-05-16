import game.Game;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.cudnn;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.deeplearning4j.optimize.listeners.checkpoint.CheckpointListener;
import org.deeplearning4j.rl4j.learning.IHistoryProcessor;
import org.deeplearning4j.rl4j.learning.async.a3c.discrete.A3CDiscrete;
import org.deeplearning4j.rl4j.learning.async.a3c.discrete.A3CDiscreteConv;
import org.deeplearning4j.rl4j.network.ac.ActorCriticFactoryCompGraphStdConv;
import org.deeplearning4j.rl4j.util.DataManager;
import org.nd4j.linalg.learning.config.Adam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static game.Game.SCREEN_HEIGHT;
import static game.Game.SCREEN_WIDTH;

public class Main {

    private final static IHistoryProcessor.Configuration ALE_HP =
            new IHistoryProcessor.Configuration(
                    15, //History length
                     100, //resize width
                     50, //resize height
                     100, //crop width
                     50, //crop height
                    0, //cropping x offset
                    0, //cropping y offset
                    10      //skip mod (one frame is picked every x
            );

    private final static A3CDiscrete.A3CConfiguration ALE_A3C =
            new A3CDiscrete.A3CConfiguration(
                    123, //Random seed
                    10000, //Max step By epoch
                    8000000, //Max step
                    0, //Number of threads
                    32, //t_max
                    500, //num step noop warmup
                    1, //reward scaling
                    0.99, //gamma
                    10.0            //td-error clipping
            );


    private final static ActorCriticFactoryCompGraphStdConv.Configuration ALE_NET_A3C =
            new ActorCriticFactoryCompGraphStdConv.Configuration(
                    0.00025, //learning rate
                    new Adam(0.0005), null, false
            );


    public static void main(String args[]) {
        try {
            Loader.load(cudnn.class);
        } catch (UnsatisfiedLinkError e) {
            try {
                new ProcessBuilder("C:\\depends", Loader.getTempDir() + "/jnicudnn.dll").start().waitFor();
            } catch (InterruptedException | IOException e1) {
                e1.printStackTrace();
            }
        }
        try {
            //record the training data in rl4j-data in a new folder
            DataManager manager = new DataManager(true);

            Game game = new Game();

            //setup the training


            A3CDiscreteConv a3c = new A3CDiscreteConv(game, ALE_NET_A3C, ALE_HP, ALE_A3C, manager);

            //start the training
            a3c.train();


            //save the model at the end
            a3c.getPolicy().save("ale-a3c.model");

            //close the ALE env
            game.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

       /* Game game = new Game();
        Thread thread = new Thread() {
            @Override
            public void run() {
                boolean first = true;
                while (true) {
                    game.nextStep(first);
                    first = false;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();*/

    }


}
