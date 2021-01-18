/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class SplitFlowIDTestBatchletImpl extends AbstractBatchlet {

    private volatile static String data = "";

    public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION";

    @Inject
    JobContext jobCtx;

    @Inject
    StepContext stepCtx;

    @Override
    public String process() throws Exception {

        // Check job properties
        /*
         * <property name="topLevelJobProperty" value="topLevelJobProperty.value" />
         */
        String propVal = jobCtx.getProperties().getProperty("topLevelJobProperty");
        String expectedPropVal = "topLevelJobProperty.value";

        if (propVal == null || (!propVal.equals(expectedPropVal))) {
            throw new Exception("Expected propVal of " + expectedPropVal + ", but found: " + propVal);
        }

        // Check job name
        String jobName = jobCtx.getJobName();
        String expectedJobName = "splitFlowCtxPropagation";
        if (!jobName.equals(expectedJobName)) {
            throw new Exception("Expected jobName of " + expectedJobName + ", but found: " + jobName);
        }

        String data = stepExitStatus();
        stepCtx.setExitStatus(stepCtx.getExitStatus() + data);
        return GOOD_EXIT_STATUS;
    }

    private String stepExitStatus() {
        long execId = jobCtx.getExecutionId();
        long instanceId = jobCtx.getInstanceId();
        long stepExecId = stepCtx.getStepExecutionId();

        data = ":J" + execId + "I" + instanceId + "S" + stepExecId;
        return data;
    }
}
