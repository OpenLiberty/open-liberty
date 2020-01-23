/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class AsyncMethodsReturnNullBean {

    int futureRetryCounter = 0;
    int completionStageRetryCounter = 0;

    @Asynchronous
    public Future<String> getNullFuture() {
        return null;
    }

    @Asynchronous
    public CompletionStage<String> getNullCompletionStage() {
        return null;
    }

    @Asynchronous
    @Retry(maxRetries = 2)
    public Future<String> getNullFutureRetry() {
        futureRetryCounter++;
        return null;
    }

    @Asynchronous
    @Retry(maxRetries = 2)
    public CompletionStage<String> getNullCompletionStageRetry() {
        completionStageRetryCounter++;
        return null;
    }

    public int getFutureRetryCounter() {
        return futureRetryCounter;
    }

    public int getCompletionStageRetryCounter() {
        return completionStageRetryCounter;
    }

}
