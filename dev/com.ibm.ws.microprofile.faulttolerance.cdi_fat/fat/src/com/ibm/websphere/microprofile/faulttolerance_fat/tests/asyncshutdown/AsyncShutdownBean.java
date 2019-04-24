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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

@ApplicationScoped
public class AsyncShutdownBean {

    @Inject
    AsyncShutdownBean self;

    private CompletionStage<?> shutdownStage;

    @PostConstruct
    private void setup() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        cf.complete(null);
        shutdownStage = cf;
    }

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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        if (repeats > 0) {
            return self.runFiniteAsyncTask(repeats - 1);
        } else {
            return CompletableFuture.completedFuture("OK");
        }
    }

    public void waitBeforeShutdown(CompletionStage<?> stage) {
        System.out.println("Registering stage for wait before shutdown");
        shutdownStage = shutdownStage.runAfterBoth(stage, () -> {
        });
    }

    @PreDestroy
    private void waitForPendingStages() {
        try {
            System.out.println("I am waiting for all tasks to complete");
            shutdownStage.toCompletableFuture().get(20, TimeUnit.SECONDS);
            System.out.println("Successfully finished waiting");
        } catch (InterruptedException e) {
            // Interrupted, can't handle that here, reset flag and return
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            // Do nothing, expect test to fail anyway because server logs an error
            System.out.println("Failed waiting: " + e);
        }
    }

}
