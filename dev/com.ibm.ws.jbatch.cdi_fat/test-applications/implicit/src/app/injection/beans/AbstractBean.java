/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package app.injection.beans;

import javax.annotation.PostConstruct;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

import app.injection.NoActiveBatchJobException;

public abstract class AbstractBean {

    /**
     * Note it's not clear from the spec that you should be able to inject context into a non-batch artifact
     * by virtue of thread context.
     */
    @Inject
    private JobContext jobContext;

    /**
     * Note it's not clear from the spec that you should be able to inject properties into a non-batch artifact
     * by virtue of thread context. It could be of some use for @Dependent scoped, though maybe kind of a bad fit for @ApplicationScoped
     * since it would depend who loaded it first.
     */

    @Inject
    @BatchProperty(name = "color")
    protected String color;

    @PostConstruct
    private void incrementInstanceCount() {
        instanceCount++;
    }

    // Seems helpful in logging to note how many instances there are.
    private static int instanceCount;

    /**
     *
     * @return The value returned by calling getJobName() on the injected JobContext.
     * @throws NoActiveBatchJobException If and only if the JobContext is null.
     */
    public String getJobName() throws NoActiveBatchJobException {
        if (jobContext != null) {
            return jobContext.getJobName();
        } else {
            throw new NoActiveBatchJobException("From getJobName()");
        }
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return super.toString() + ", instanceCount = " + instanceCount + ",  jobCtx= " + jobContext + ", color =" + color;
    }

    public static int getInstanceCount() {
        return instanceCount;
    }

}
