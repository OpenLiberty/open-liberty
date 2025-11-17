/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.bundle.threading;

import java.util.concurrent.Callable;

import com.ibm.wsspi.threading.ExecutorServiceTaskInterceptor;

public class TaskInterceptorImpl implements ExecutorServiceTaskInterceptor {
    @Override
    public Runnable wrap(final Runnable oldTask) {
        Runnable newTask = new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("com.ibm.ws.threading_fat_beforeTask");
                    oldTask.run();
                } finally {
                    System.out.println("com.ibm.ws.threading_fat_afterTask");
                }
            }
        };
        return newTask;
    }

    @Override
    public <T> Callable<T> wrap(final Callable<T> oldTask) {
        Callable<T> newTask = new Callable<T>() {
            @Override
            public T call() {
                try {
                    System.out.println("com.ibm.ws.threading_fat_beforeTask");
                    return oldTask.call();
                } catch (Exception e) {
                    return null;
                } finally {
                    System.out.println("com.ibm.ws.threading_fat_afterTask");
                }
            }
        };
        return newTask;
    }
}
