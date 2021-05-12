package processitem.artifacts;

import javax.batch.api.listener.StepListener;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import batch.fat.common.util.TestForcedException;
import batch.fat.util.BatchFATHelper;

public class MyStepListener implements StepListener {
    @Inject
    protected transient StepContext stepContext;

    protected long stTime = -1L;

    @Override
    public void afterStep() throws Exception {
        Exception lastException = stepContext.getException();
        if (lastException != null && lastException instanceof TestForcedException) {
            stepContext.setExitStatus(BatchFATHelper.SUCCESS_MESSAGE);
        } else {
            stepContext.setExitStatus("FAILURE: step listener did not find expected exception on step context, actual exception is " + lastException);
        }
    }

    @Override
    public void beforeStep() throws Exception {}

}
