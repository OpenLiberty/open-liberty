/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package app.misc1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.listener.AbstractStepListener;
import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.api.partition.AbstractPartitionReducer;
import javax.batch.api.partition.PartitionCollector;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

public class Artifacts {

    private final static Logger logger = Logger.getLogger("test");

    @Dependent
    public static class Reader extends AbstractItemReader {

        //@Inject @BatchProperty
        // Smaller than 1 chunk
        String numToRead = "3";

        @Inject
        @BatchProperty(name = "failOn")
        String failOnStr;

        int i = 0;

        @Override
        public Object readItem() {

            i++;

            if (i <= Integer.parseInt(numToRead)) {
                possiblyFail(i);
                return i;
            } else {
                return null;
            }
        }

        private void possiblyFail(int i) {
            if (failOnStr.equals("skip")) {
                logger.finest("skip force failure");
                return;
            }
            int failOn = Integer.parseInt(failOnStr);
            logger.finer("i = " + i + ", failOn = " + failOn);
            if (i == failOn) {
                throw new IllegalStateException("Forcing failure in reader on item # : " + i);
            }
        }
    }

    @Dependent
    public static class NoOpWriter extends AbstractItemWriter {
        @Override
        public void writeItems(List<Object> items) throws Exception {
        }
    }

    @Dependent
    public static class Analyzer extends AbstractPartitionAnalyzer {

        @Inject
        @BatchProperty
        String jobParam;

        @Inject
        @BatchProperty
        String jobProp;

        @Inject
        @BatchProperty
        String stepProp;

        @Inject
        StepContext stepCtx;

        @Override
        public void analyzeCollectorData(Serializable data) throws Exception {

            // Validate "static" part same as every partition
            String prefix = jobParam + "," + jobProp + "," + stepProp + ",";
            String collectorData = (String) data;
            assertTrue("Expected collected string = " + collectorData + " to start with prefix = " + prefix,
                       collectorData.startsWith(prefix));

            Set<String> userData = (Set<String>) stepCtx.getTransientUserData();
            String userDataEntry = collectorData.replaceFirst(prefix, "");
            logger.fine("Adding: " + userDataEntry + " to user data obj.");
            userData.add(userDataEntry);
        }

    }

    @Dependent
    public static class StepListener extends AbstractStepListener {

        @Inject
        @BatchProperty(name = "numPartitions")
        String numPartitionsStr;

        @Inject
        @BatchProperty(name = "doEndOfStepValidation")
        String doEndOfStepValidationStr;

        @Inject
        StepContext stepCtx;

        @Override
        public void beforeStep() throws Exception {
            stepCtx.setTransientUserData(new HashSet<String>());
        }

        @Override
        public void afterStep() throws Exception {

            boolean doEndOfStepValidation = Boolean.parseBoolean(doEndOfStepValidationStr);

            if (!doEndOfStepValidation) {
                logger.fine("Returning without doing validation, prop = " + doEndOfStepValidationStr);
                return;
            }

            int numPartitions = Integer.parseInt(numPartitionsStr);
            logger.fine("Doing validation, for " + numPartitions + " # of partitions");

            Set<String> expected = new HashSet<String>();
            for (int i = 0; i < numPartitions; i++) {
                expected.add("part" + new Integer(i).toString());
            }
            assertEquals("Expected set doesn't match set of data collected via collector->analyzer", expected, stepCtx.getTransientUserData());
        }

    }

    @Dependent
    public static class Collector implements PartitionCollector {

        @Inject
        @BatchProperty
        String jobParam;

        @Inject
        @BatchProperty
        String jobProp;

        @Inject
        @BatchProperty
        String stepProp;

        @Inject
        @BatchProperty
        String i;

        @Override
        public Serializable collectPartitionData() throws Exception {
            return jobParam + "," + jobProp + "," + stepProp + "," + i;
        }
    }

    @Dependent
    public static class Mapper implements PartitionMapper {

        @Inject
        @BatchProperty(name = "numPartitions")
        String numPartitionsStr;

        @Override
        public PartitionPlan mapPartitions() throws Exception {

            int numPartitions = Integer.parseInt(numPartitionsStr);

            PartitionPlanImpl plan = new PartitionPlanImpl();
            plan.setPartitions(numPartitions);

            Properties[] partitionProperties = new Properties[numPartitions];
            plan.setPartitionProperties(partitionProperties);
            for (int i = 0; i < numPartitions; i++) {
                Properties p = new Properties();
                p.setProperty("idx", "part" + Integer.toString(i));
                partitionProperties[i] = p;
            }
            return plan;
        }
    }

    @Dependent
    public static class Reducer extends AbstractPartitionReducer {

        @Inject
        JobContext ctx;

        @Inject
        @BatchProperty
        String stepProp;

        @Override
        public void afterPartitionedStepCompletion(PartitionStatus status) throws Exception {
            ctx.setExitStatus(stepProp + "," + status.name());
        }

    }

}
