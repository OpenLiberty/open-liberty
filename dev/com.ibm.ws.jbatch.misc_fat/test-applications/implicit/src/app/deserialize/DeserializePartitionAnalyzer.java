package app.deserialize;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

@Dependent
@Named("DeserializePartitionAnalyzer")
public class DeserializePartitionAnalyzer extends AbstractPartitionAnalyzer {

    private final static Logger logger = Logger.getLogger("test");

    @Inject
    private JobContext jobCtx;

    @Inject
    private StepContext stepCtx;

    private int partitionsCompleted = 0;

    /*
     * On each COLLECTOR_DATA
     */
    @Override
    public void analyzeCollectorData(Serializable data) throws Exception {

        Integer[] counts = (Integer[]) data;

        //validateCollectorDataSize(counts);

        Integer[] ud = (Integer[]) stepCtx.getPersistentUserData();
        // TODO
    }

    /*
     * Coalesce the counts coming in from each partition to the "top-level"
     * count held by the exitStatus on the top-level thread, on which the
     * analyzer always executes.
     */
    @Override
    public void analyzeStatus(BatchStatus batchStatus,
                              String nextPartitionExitStatus) throws Exception {

        if (batchStatus.equals(BatchStatus.COMPLETED)) {
            Integer[] ud = (Integer[]) stepCtx.getPersistentUserData();
            updateRecordCounts(nextPartitionExitStatus);
            partitionsCompleted++;
        } else {
            logger.fine("Noticing a non-completed partition exiting with batchStatus of: " + batchStatus);
        }

    }

    // No need to synchronize, the spec says there is only one thread processing all analyzer calls.
    private void updateRecordCounts(String nextPartitionExitStatus) {

        int numRecordsCumulativelyCompletedThisPartition = Integer.parseInt(nextPartitionExitStatus);

        updateExitStatus(numRecordsCumulativelyCompletedThisPartition);
        updatePersistentUserData(numRecordsCumulativelyCompletedThisPartition);
    }

    private void updatePersistentUserData(int numRecordsPartitionCompletedThisExecution) {
        Integer[] ud = (Integer[]) stepCtx.getPersistentUserData();

        if (ud == null) {
            ud = new Integer[ReaderData.outerLoop.length];
        }
        ud[partitionsCompleted] = numRecordsPartitionCompletedThisExecution;
        stepCtx.setPersistentUserData(ud);
    }

    private void updateExitStatus(int numRecordsPartitionCompletedThisExecution) {
        String overallStepExitStatus = stepCtx.getExitStatus();

        int currentCumulativeCount = overallStepExitStatus == null ? 0 : Integer.parseInt(overallStepExitStatus);

        int newCumulativeCount = currentCumulativeCount + numRecordsPartitionCompletedThisExecution;

        logger.fine("Current cumulative record count = " + currentCumulativeCount + ", adding: " + numRecordsPartitionCompletedThisExecution
                    + " more records to count for a total of: "
                    + newCumulativeCount);

        stepCtx.setExitStatus(String.valueOf(newCumulativeCount));
    }

}
