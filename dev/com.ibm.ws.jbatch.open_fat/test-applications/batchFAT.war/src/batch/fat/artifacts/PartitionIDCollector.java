package batch.fat.artifacts;

import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class PartitionIDCollector implements PartitionCollector {

    @Inject
    JobContext jobCtx;
    @Inject
    StepContext stepCtx;

    @Override
    public String collectPartitionData() throws Exception {

        String stepName = stepCtx.getStepName();
        if (!stepName.equals("step1")) {
            throw new IllegalStateException("Expected stepName: step1, but found: " + stepName);
        }

        long jobid = jobCtx.getExecutionId();
        long instanceid = jobCtx.getInstanceId();
        long stepid = stepCtx.getStepExecutionId();

        String collectorData = ":J" + jobid + "I" + instanceid + "S" + stepid;
        return collectorData;
    }

}
