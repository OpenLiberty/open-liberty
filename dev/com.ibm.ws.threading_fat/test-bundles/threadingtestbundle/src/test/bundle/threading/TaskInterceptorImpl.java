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
                }
                finally {
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
                }
                catch (Exception e) {
                    return null;
                }
                finally {
                    System.out.println("com.ibm.ws.threading_fat_afterTask");
                }
            }
        };
        return newTask;
    }
}
