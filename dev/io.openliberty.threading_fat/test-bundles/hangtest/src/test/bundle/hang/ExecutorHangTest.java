/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.bundle.hang;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ExecutorHangTest {

    private ExecutorService executorService;

    @Reference
    protected void setExecutorService(ExecutorService ses) {
        executorService = ses;
    }

    protected void unsetExecutorService(ExecutorService ses) {
        executorService = null;
    }

    @Activate
    protected void activate() {
        runExecutorHangTest();
    }

    class ReturnsTrueCallable implements Callable<Boolean> {
        @Override
        public Boolean call() {
            return true;
        }
    }

    class ReturnsBooleanCallable implements Callable<Boolean> {
        private final ExecutorService es;

        public ReturnsBooleanCallable(ExecutorService es) {
            this.es = es;
        }

        @Override
        public Boolean call() {
            try {
                Callable<Boolean> c = new ReturnsTrueCallable();
                Future<Boolean> f = es.submit(c);
                return f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void runExecutorHangTest() {

        // submit a bunch of quick running work so that the thread pool controller sees very high
        // throughput at a poolSize of 2 threads, making the base throughput algorithm reluctant
        // to increase the number of threads further
        for (int i = 0; i < 1000; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        ArrayList<ReturnsBooleanCallable> alc = new ArrayList<ReturnsBooleanCallable>(10);
        ArrayList<Future<Boolean>> alf = new ArrayList<Future<Boolean>>();
        for (int i = 0; i < 20; i++) {
            alc.add(new ReturnsBooleanCallable(executorService));
        }

        // each ReturnsBooleanCallable submits a child ReturnsTrueCallable and then waits on the result...
        // submitting so many of these at once when the pool size is low will deadlock the pool unless
        // the pool size is increased
        for (ReturnsBooleanCallable rbc : alc) {
            alf.add(executorService.submit(rbc));
        }
        try {
            for (Future<Boolean> f : alf) {
                f.get();
            }
        } catch (Exception ex) {
            System.out.println("runExecutorHangTest FAILED. Exception: ");
            ex.printStackTrace();
            return;
        }

        System.out.println("runExecutorHangTest PASSED");
    }

}