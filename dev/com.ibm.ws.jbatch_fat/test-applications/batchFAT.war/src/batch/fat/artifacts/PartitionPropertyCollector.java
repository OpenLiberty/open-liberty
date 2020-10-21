package batch.fat.artifacts;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class PartitionPropertyCollector implements PartitionCollector {

    @Inject
    JobContext jobCtx;

    @Inject
    StepContext stepCtx;

    @Inject
    @BatchProperty(name = "xx")
    String stepProp;

    @Override
    public String collectPartitionData() throws Exception {
        return stepProp;
    }

}
