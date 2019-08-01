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
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import java.time.Duration;

public class MockScheduledTask<V> {
    private final Runnable command;
    private final Duration delay;
    private final MockScheduledFuture<V> future;

    public MockScheduledTask(Runnable command, Duration delay, MockScheduledFuture<V> future) {
        super();
        this.command = command;
        this.delay = delay;
        this.future = future;
    }

    public Duration getDelay() {
        return delay;
    }

    public void run() {
        command.run();
    }

    public MockScheduledFuture<V> getFuture() {
        return future;
    }
}