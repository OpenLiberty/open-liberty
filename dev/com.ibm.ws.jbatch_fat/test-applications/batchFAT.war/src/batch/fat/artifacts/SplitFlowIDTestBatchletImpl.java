/*
 * Copyright 2014 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
