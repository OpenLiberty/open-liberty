/**
 *
 */
package com.ibm.ws.jpa.fvt.injection.mdb;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class TestCoordinator {
    private static TestCoordinator singleton = new TestCoordinator();

    private CountDownLatch cdl = new CountDownLatch(1);
    private volatile TestExecutionResult tre = null;

    private TestCoordinator() {

    }

    public static synchronized void initalize() {
        singleton = new TestCoordinator();
    }

    public static TestExecutionResult blockForMDBExecutionCompletion() throws InterruptedException {
        TestCoordinator tc = null;

        synchronized (TestCoordinator.class) {
            tc = singleton;
        }

        TestExecutionResult tre = null;
        try {
            if (tc.cdl.await(30, TimeUnit.SECONDS)) {
                tre = tc.tre;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw e;
        }

        return tre;
    }

    public static synchronized void notifyCompletion(TestExecutionResult tre) {
        singleton.tre = tre;
        singleton.cdl.countDown();
    }
}
