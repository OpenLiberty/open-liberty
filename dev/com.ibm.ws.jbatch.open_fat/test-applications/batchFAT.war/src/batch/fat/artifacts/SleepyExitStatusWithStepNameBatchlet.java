package batch.fat.artifacts;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class SleepyExitStatusWithStepNameBatchlet extends AbstractBatchlet {

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
        if (sleepTime > 0) {
            Thread.sleep(sleepTime * 1000);
        }

        if (Boolean.parseBoolean(forceFailure)) {
            throw new IllegalStateException("Forcing failure in batchlet.");
        } else {
            return ctx.getStepName();
        }
    }
}
