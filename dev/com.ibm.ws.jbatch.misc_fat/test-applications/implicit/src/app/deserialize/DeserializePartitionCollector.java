package app.deserialize;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

@Dependent
@Named("DeserializePartitionCollector")
public class DeserializePartitionCollector implements PartitionCollector {

    private final static Logger logger = Logger.getLogger("test");

    private Integer previousExitStatus = 0;

    private int chunkNum = 0;

    @Inject
    private StepContext stepCtx;

    @Override
    public Serializable collectPartitionData() throws Exception {

        Integer[] collectorData = new Integer[ReaderData.outerLoop.length];

        Integer currentChunkExitStatus = (stepCtx.getExitStatus() == null ? null : Integer.parseInt(stepCtx.getExitStatus()));
        collectorData[chunkNum] = currentChunkExitStatus;

        previousExitStatus = currentChunkExitStatus;
        chunkNum++;

        logger.finer("[DEBUG] stepCtx.getExitStatus() " + stepCtx.getExitStatus());
        logger.finer("[DEBUG] previousExitStatus " + previousExitStatus);
        logger.finer("[DEBUG] currentChunkExitStatus " + currentChunkExitStatus);
        logger.finer("[DEBUG] collectorData " + collectorData);

        return collectorData;
    }

}
