package app.deserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

@Dependent
@Named("SimplePartitionMapper")
public class SimplePartitionMapper implements PartitionMapper {

    private final static Logger logger = Logger.getLogger("test");

    @Inject
    JobContext jobCtx;

    @Override
    public PartitionPlan mapPartitions() throws Exception {
        int numRecords = ReaderData.outerLoop.length * ReaderData.innerLoop.length;
        logger.fine("In mapper, # of records = " + numRecords);

        int numPartitions = ReaderData.outerLoop.length;
        logger.fine("In mapper, # of partitions = " + numPartitions);

        PartitionPlanImpl plan = new PartitionPlanImpl();
        plan.setPartitions(numPartitions);

        List<Properties> partitionProperties = new ArrayList<Properties>(numPartitions);

        // If the number of records is evenly divisible by the number of partitions, then
        // all partitions are the same size (so 'extraRecords' = 0).
        int minNumRecordsPerPartition = numRecords / numPartitions;
        int extraRecords = numRecords % numPartitions;

        logger.fine("In mapper, # each partition will have at least = " + minNumRecordsPerPartition +
                    " # of records and " + extraRecords + " partitions will have " + minNumRecordsPerPartition + 1 + " records");

        int i;
        for (i = 0; i < numPartitions; i++) {

            Properties p = new Properties();

            p.setProperty("startAtIndex", Integer.toString(i));
            p.setProperty("partitionSize", Integer.toString(ReaderData.innerLoop.length));

            logger.fine("In mapper, partition # " + i + " will start at outer loop index of: " +
                        i + " and contain " + ReaderData.innerLoop.length + " # of records.");

            partitionProperties.add(p);
        }

        if (i != ReaderData.outerLoop.length) {
            String errorMsg = "Messed up calculation in mapper.  Outer loop size = " + ReaderData.outerLoop.length + " but only assigned partitions # = " + i;
            throw new IllegalStateException(errorMsg);
        }

        plan.setPartitionProperties(partitionProperties.toArray(new Properties[0]));

        return plan;
    }
}
