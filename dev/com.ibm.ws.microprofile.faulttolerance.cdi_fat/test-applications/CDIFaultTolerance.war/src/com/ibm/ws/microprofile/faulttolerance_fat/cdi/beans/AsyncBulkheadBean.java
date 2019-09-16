/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;

@RequestScoped
@Asynchronous
public class AsyncBulkheadBean {

    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<Void> runTask(SyntheticTask<Void> task) {
        return CompletableFuture.completedFuture(task.call());
    }

    @Timeout(TestConstants.TIMEOUT)
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<Void> runTaskWithTimeout(SyntheticTask<Void> task) {
        return CompletableFuture.completedFuture(task.call());
    }

    /**
     * Set the Bulkhead value to 3, but this bean's config will be overridden to a value of 2 in microprofile-config.properties, which is what the test expects.
     */
    @Bulkhead(value = 3, waitingTaskQueue = 2)
    public Future<Void> runTaskWithConfig(SyntheticTask<Void> task) {
        return CompletableFuture.completedFuture(task.call());
    }

}
