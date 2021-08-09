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
package fat.concurrent.spec.app;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Cancels tasks that execute for longer than the specified timeout
 */
public class WorkTimeout implements ManagedTaskListener {
    private final long timeout;
    private final TimeUnit unit;

    public WorkTimeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {}

    @Override
    public void taskStarting(final Future<?> future, ManagedExecutorService executor, Object task) {
        try {
            ScheduledExecutorService scheduledExecutor = (ScheduledExecutorService) new InitialContext().lookup("java:comp/DefaultManagedScheduledExecutorService");
            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    if (!future.isDone())
                        future.cancel(true);
                }
            }, timeout, unit);
        } catch (NamingException x) {
            x.printStackTrace(System.out);
        }
    }

    @Override
    public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {}

    @Override
    public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {}
}
