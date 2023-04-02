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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.concurrent.spec.app;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTaskListener;

/**
 * Upon taskSubmitted, records the delay until the next execution.
 */
public class DelayListener implements ManagedTaskListener {
    final LinkedBlockingQueue<Long> delays = new LinkedBlockingQueue<Long>();

    @Override
    public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {
    }

    @Override
    public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {
    }

    @Override
    public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
    }

    @Override
    public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
        delays.add(((ScheduledFuture<?>) future).getDelay(TimeUnit.MILLISECONDS));
    }
}