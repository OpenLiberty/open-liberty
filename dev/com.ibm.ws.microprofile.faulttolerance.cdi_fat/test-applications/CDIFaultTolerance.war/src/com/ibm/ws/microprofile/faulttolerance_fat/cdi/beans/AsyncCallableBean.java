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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

@ApplicationScoped
public class AsyncCallableBean implements Callable<Future<String>> {

    private SyntheticTask<String> task;

    public void setTask(SyntheticTask<String> task) {
        this.task = task;
    }

    /** {@inheritDoc} */
    @Override
    @Asynchronous
    public Future<String> call() throws Exception {
        return CompletableFuture.completedFuture(task.call());
    }

}
