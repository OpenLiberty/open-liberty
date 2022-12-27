/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.artifacts;

import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class PartitionIDCollector implements PartitionCollector {

    @Inject
    JobContext jobCtx;
    @Inject
    StepContext stepCtx;

    @Override
    public String collectPartitionData() throws Exception {

        String stepName = stepCtx.getStepName();
        if (!stepName.equals("step1")) {
            throw new IllegalStateException("Expected stepName: step1, but found: " + stepName);
        }

        long jobid = jobCtx.getExecutionId();
        long instanceid = jobCtx.getInstanceId();
        long stepid = stepCtx.getStepExecutionId();

        String collectorData = ":J" + jobid + "I" + instanceid + "S" + stepid;
        return collectorData;
    }

}
