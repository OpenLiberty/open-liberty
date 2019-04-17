/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.asyncshutdown;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

@ApplicationScoped
public class AsyncShutdownBean {

    @Inject
    AsyncShutdownBean self;

    @Asynchronous
    public CompletionStage<String> runAsyncTask() {
        System.out.println("Running async task");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        return self.runAsyncTask();
    }

    @Asynchronous
    public CompletionStage<String> runFiniteAsyncTask(int repeats) {
        System.out.println("Running finite async task");
        if (repeats > 0) {
            return this.runFiniteAsyncTask(repeats - 1);
        } else {
            return CompletableFuture.completedFuture("OK");
        }
    }

}
