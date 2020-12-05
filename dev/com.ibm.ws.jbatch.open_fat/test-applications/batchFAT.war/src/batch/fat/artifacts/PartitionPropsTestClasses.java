/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.artifacts;

import java.util.Properties;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import batch.fat.util.PartitionSetChecker;

public class PartitionPropsTestClasses {

    private static PartitionSetChecker propChecker = new PartitionSetChecker();

    public static class TestValidationListener extends AbstractJobListener {

        @Inject
        @BatchProperty
        String xxPrefixProp;

        @Override
        public void afterJob() throws Exception {

            String[] expectedPropertyVals = {
                                             xxPrefixProp + "plannedStep0",
                                             xxPrefixProp + "plannedStep1",
                                             xxPrefixProp + "mappedStep0",
                                             xxPrefixProp + "mappedStep1",
                                             xxPrefixProp + "mappedStep2" };

            propChecker.setExpectedValues(expectedPropertyVals);

            propChecker.assertExpected();
        }
    }

    public static class Batchlet extends AbstractBatchlet {
        @Inject
        @BatchProperty
        String xx;

        @Override
        public String process() throws Exception {
            propChecker.add(xx);
            return null;
        }
    }

    private static class PartitionPlanImpl implements PartitionPlan {

        public PartitionPlanImpl() {
            super();
        }

        int count;
        Properties[] props;

        @Override
        public void setPartitions(int count) {
            this.count = count;
        }

        @Override
        public int getPartitions() {
            return count;
        }

        @Override
        public void setPartitionProperties(Properties[] props) {
            this.props = props;
        }

        @Override
        public Properties[] getPartitionProperties() {
            return props;
        }

        @Override
        public void setPartitionsOverride(boolean override) {}

        @Override
        public boolean getPartitionsOverride() {
            return false;
        }

        @Override
        public void setThreads(int count) {}

        @Override
        public int getThreads() {
            return count;
        }

    }

    public static class Mapper implements PartitionMapper {

        @Inject
        @BatchProperty
        String xxPrefixProp;

        @Inject
        StepContext stepCtx;

        @Override
        public PartitionPlan mapPartitions() throws Exception {
            PartitionPlanImpl retVal = new PartitionPlanImpl();

            Properties[] props = new Properties[3];
            props[0] = new Properties();
            props[1] = new Properties();
            props[2] = new Properties();
            props[0].setProperty("xx", xxPrefixProp + stepCtx.getStepName() + "0");
            props[1].setProperty("xx", xxPrefixProp + stepCtx.getStepName() + "1");
            props[2].setProperty("xx", xxPrefixProp + stepCtx.getStepName() + "2");
            retVal.setPartitionProperties(props);
            retVal.setPartitions(props.length);
            return retVal;
        }
    }
}
