/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.artifacts;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

public class PartitionTestBatchletImpl extends AbstractBatchlet {

    public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION";

    @Inject
    JobContext ctx;

    @Override
    public String process() throws Exception {

        // Check job properties
        /*
         * <property name="topLevelJobProperty" value="topLevelJobProperty.value" />
         */
        String propVal = ctx.getProperties().getProperty("topLevelJobProperty");
        String expectedPropVal = "topLevelJobProperty.value";

        if (propVal == null || (!propVal.equals(expectedPropVal))) {
            throw new Exception("Expected propVal of " + expectedPropVal + ", but found: " + propVal);
        }

        // Check job name
        String jobName = ctx.getJobName();
        String expectedJobName = "partitionCtxPropagation";
        if (!jobName.equals(expectedJobName)) {
            throw new Exception("Expected jobName of " + expectedJobName + ", but found: " + jobName);
        }

        return GOOD_EXIT_STATUS;
    }

    @Override
    public void stop() throws Exception {}
}
