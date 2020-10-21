/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package batch.fat.artifacts;

import javax.batch.api.partition.PartitionReducer;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

/**
 *
 */
public class ExitStatusMethodNamePartitionReducer implements PartitionReducer {

    @Inject
    JobContext jobCtx;

    @Inject
    StepContext stepCtx;

    @Override
    public void beginPartitionedStep() throws Exception {
        // Do Nothing
    }

    @Override
    public void beforePartitionedStepCompletion() throws Exception {
        stepCtx.setExitStatus("PartitionReducer method : beforePartitionedStepCompletion");

    }

    @Override
    public void rollbackPartitionedStep() throws Exception {
        stepCtx.setExitStatus("PartitionReducer method : rollbackPartitionedStep");
    }

    @Override
    public void afterPartitionedStepCompletion(PartitionStatus status) throws Exception {
        // Do Nothing
    }

}
