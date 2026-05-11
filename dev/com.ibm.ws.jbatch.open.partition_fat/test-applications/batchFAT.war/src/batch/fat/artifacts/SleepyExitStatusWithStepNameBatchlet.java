package batch.fat.artifacts;

import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class SleepyExitStatusWithStepNameBatchlet extends AbstractBatchlet {

    private final static Logger logger = Logger.getLogger(SleepyExitStatusWithStepNameBatchlet.class.getName());

    private volatile boolean stopRequested = false;

    @Inject
    StepContext ctx;
    @Inject
    @BatchProperty
    String forceFailure;

    @Inject
    @BatchProperty(name = "sleep.time.seconds")
    String sleepTimeSeconds;

    @Override
    public String process() throws Exception {

        int sleepTime = Integer.parseInt(sleepTimeSeconds);
        logger.info("process: entry" + ctx.getStepExecutionId() + "sleepTime=" + sleepTime + " forceFailure=" + forceFailure);


        for (int i = 0; i < sleepTime && !stopRequested; ++i) {
            logger.info("process: [" + i + "] sleeping for a second...");
            Thread.sleep(1 * 1000);
        }

        if (Boolean.parseBoolean(forceFailure)) {
            throw new IllegalStateException("Forcing failure in batchlet.");
        } else {
            return ctx.getStepName();
        }
    }

    /**
     * Called if the batchlet is stopped by the container.
     */
    @Override
    public void stop() throws Exception {
        logger.info("stop: " + ctx.getStepExecutionId());
        stopRequested = true;
    }
}
