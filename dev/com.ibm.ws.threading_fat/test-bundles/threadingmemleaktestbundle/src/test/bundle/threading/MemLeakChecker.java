/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.bundle.threading;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class MemLeakChecker {
    private static final int MAX = 10000;

    private ScheduledExecutorService scheduledExecutorService;

    @Reference
    protected void setScheduledExecutorService(ScheduledExecutorService ses) {
        scheduledExecutorService = ses;
    }

    protected void unsetScheduledExecutorService(ScheduledExecutorService ses) {
        scheduledExecutorService = null;
    }

    @Activate
    protected void activate() {
        runScheduleCancelTest();
        runScheduleExecuteTest();
    }

    private void runScheduleCancelTest() {
        List<WeakReference<ScheduledFuture<?>>> list = new ArrayList<WeakReference<ScheduledFuture<?>>>(MAX);
        for (int i = 0; i < MAX; i++) {
            ScheduledFuture<?> schedFuture = scheduledExecutorService.schedule(new Task("CancelMe", i, null), 10, TimeUnit.MINUTES);
            list.add(new WeakReference<ScheduledFuture<?>>(schedFuture));
            if (!schedFuture.cancel(false)) {
                System.out.println("Failed to cancel " + schedFuture);
                return;
            }
        }

        gc();
        for (int i = 0; i < MAX; i++) {
            WeakReference<ScheduledFuture<?>> weakRef = list.get(i);
            ScheduledFuture<?> schedFuture = weakRef.get();
            if (schedFuture != null) {
                System.out.println("runScheduleCancelTest FAILED - scheduledFuture[" + i + "] not GC'd");
                return;
            }
        }
        System.out.println(scheduledExecutorService.toString() + " executed and canceled " + MAX + " tasks");
        System.out.println("runScheduleCancelTest PASSED");
    }

    private void runScheduleExecuteTest() {
        // use a lower maximum since this test will actually be executing tasks
        final int MAX = MemLeakChecker.MAX / 100;

        final CountDownLatch latch = new CountDownLatch(MAX);
        List<WeakReference<ScheduledFuture<?>>> list = new ArrayList<WeakReference<ScheduledFuture<?>>>(MAX);
        for (int i = 0; i < MAX; i++) {
            ScheduledFuture<?> schedFuture = scheduledExecutorService.schedule(new Task("RunMe", i, latch), 100, TimeUnit.MICROSECONDS);
            list.add(new WeakReference<ScheduledFuture<?>>(schedFuture));
            schedFuture = null;
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        gc();

        for (int i = 0; i < MAX; i++) {
            WeakReference<ScheduledFuture<?>> weakRef = list.get(i);
            ScheduledFuture<?> schedFuture = weakRef.get();
            if (schedFuture != null) {
                System.out.println("runScheduleExecuteTest FAILED - scheduledFuture[" + i + "] not GC'd");
                return;
            }
        }
        System.out.println("runScheduleExecuteTest PASSED");
    }

    private static void gc() {
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
        System.gc();
    }

    private static class Task implements Runnable {
        private final String name;
        private final int id;
        private final CountDownLatch latch;

        Task(String name, int id, CountDownLatch latch) {
            this.name = name;
            this.id = id;
            this.latch = latch;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            if (latch != null) {
                latch.countDown();
            }
            if (id % 1000 == 0)
                System.out.println(name + id + " executed");

        }
    }
}
