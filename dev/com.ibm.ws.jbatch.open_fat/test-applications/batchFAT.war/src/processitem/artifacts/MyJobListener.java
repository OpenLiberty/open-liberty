package processitem.artifacts;

import java.util.logging.Logger;

import javax.batch.api.listener.JobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

public class MyJobListener implements JobListener {

    Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    protected long stTime = -1L;

    @Inject
    protected transient JobContext jobContext;

    @Override
    public void afterJob() throws Exception {
        long edTime = System.currentTimeMillis();
        long execId = jobContext.getExecutionId();
        logger.fine("======================================================");
        logger.fine("afterJob() job exec id: " + execId);
        logger.fine("JOB time: " + (edTime - stTime) + " ms");
        logger.fine("======================================================");
    }

    @Override
    public void beforeJob() throws Exception {
        stTime = System.currentTimeMillis();
        long execId = jobContext.getExecutionId();
        logger.fine("======================================================");
        logger.fine("beforeJob() job exec id: " + execId);
        logger.fine("======================================================");
    }

}
