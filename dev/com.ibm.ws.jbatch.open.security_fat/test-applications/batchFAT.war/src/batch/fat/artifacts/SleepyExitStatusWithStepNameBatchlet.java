/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.artifacts;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class SleepyExitStatusWithStepNameBatchlet extends AbstractBatchlet {

    @Inject
    StepContext ctx;
    @Inject
    @BatchProperty
    String forceFailure;

    @Inject
    @BatchProperty(name = "sleep.time.seconds")
    String sleepTimeSeconds;

    @Override
    public String process() throws Exception {

        int sleepTime = Integer.parseInt(sleepTimeSeconds);
        if (sleepTime > 0) {
            Thread.sleep(sleepTime * 1000);
        }

        if (Boolean.parseBoolean(forceFailure)) {
            throw new IllegalStateException("Forcing failure in batchlet.");
        } else {
            return ctx.getStepName();
        }
    }
}
