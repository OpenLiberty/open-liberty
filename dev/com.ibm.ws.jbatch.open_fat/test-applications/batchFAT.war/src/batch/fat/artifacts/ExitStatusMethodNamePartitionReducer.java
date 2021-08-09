/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
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
