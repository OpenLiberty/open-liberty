/*
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package test.bundle.threading;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.ibm.wsspi.threading.ExecutorServiceTaskInterceptor;
import com.ibm.wsspi.threading.WorkContext;
import com.ibm.wsspi.threading.WorkContextService;

public class TaskInterceptorImpl implements ExecutorServiceTaskInterceptor {
    WorkContextService workContextService;

    protected void setWorkContextService(WorkContextService workContextService) {
        this.workContextService = workContextService;
    }

    protected void unsetWorkContextService(WorkContextService workContextService) {
        if (this.workContextService == workContextService) {
            this.workContextService = null;
        }
    }

    @Override
    public Runnable wrap(final Runnable oldTask) { 
        try {
            final WorkContext workContext = workContextService.getWorkContext();
            final boolean context = workContextService.getWorkContext() != null;
            final String fullContext;

            System.out.println(" -- iiop TaskInterceptor Runnable wrap Current Thread -- " + Thread.currentThread().toString());

            if (context) {
                fullContext = workContext.getWorkType();

                // What context & type is this, for IIOP ?
                System.out.println(" -- iiop TaskInterceptorImpl wrap - We have Context, worktype: " + fullContext);

            } else {
                fullContext = "";

            }

            Runnable newTask = new Runnable() {
                @Override
                public void run() {

                    System.out.println(" -- iiop TaskInterceptor Runnable newTask run Thread -- " + Thread.currentThread().toString());
                    if (context && fullContext.equals("IIOP")) {

                        System.out.println("This runnable has work context. The type is " + fullContext + ".");

                        // Also get the WorkContext information for IIOP
                        System.out.println("iiop context " + "requestID: " + workContext.get(WorkContext.IIOP_REQUEST_ID).toString() );
                        System.out.println("iiop context " + "operation: " + workContext.get(WorkContext.IIOP_OPERATION_NAME).toString() );
                        //System.out.println("iiop context " + "targetID: " + workContext.get(WorkContext.IIOP_TARGET_NAME).toString() );

                    } else {
                        System.out.println("This runnable has no work context. ");

                    }
                    try {
                        oldTask.run();
                    } finally {
                    }
                }
            };
            return newTask;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public <T> Callable<T> wrap(final Callable<T> oldTask) {
        Callable<T> newTask = new Callable<T>() {
            @Override
            public T call() {

                System.out.println(" -- iiop TaskInterceptor Callable<T> wrap Thread -- " + Thread.currentThread().toString());

                if (workContextService.getWorkContext() != null) {
                    System.out.println("Callable - This runnable has work context.");

                } else if (workContextService.getWorkContext() == null) {
                    System.out.println("Callable - This runnable has no work context.");
                }
                try {
                    System.out.println("Callable - com.ibm.ws.threading_fat_beforeTask");
                    return oldTask.call();
                } catch (Exception e) {
                    // Rethrow
                    System.out.println(" Callable exception ");
                    return null;
                } finally {
                    System.out.println("Callable - com.ibm.ws.threading_fat_afterTask");
                }
            }
        };
        return newTask;
    }
}
