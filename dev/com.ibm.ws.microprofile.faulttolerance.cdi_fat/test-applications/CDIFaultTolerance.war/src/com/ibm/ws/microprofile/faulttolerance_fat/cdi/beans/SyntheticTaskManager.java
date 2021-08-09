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
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import java.util.ArrayList;

/**
 * Creates {@link SyntheticTask}s and checks them and cleans up at the end of the test
 * <p>
 * An instance of this object should be created and then the test logic should be passed as a runnable to {@link #runTest(ThrowingRunnable)}
 * <p>
 * Ideally, this would be a JUnit test rule, but unfortunately they're not supported by the FATServlet class.
 */
public class SyntheticTaskManager {

    private ArrayList<SyntheticTask<?>> tasks = null;

    /**
     * Run a test, then check and clean up any synthetic tasks created during the test
     *
     * @param test the test to run
     * @throws Throwable if an exception occurs
     */
    public void runTest(ThrowingRunnable test) throws Exception {
        tasks = new ArrayList<>();
        try {
            test.run();
            checkTasks();
        } finally {
            completeOutstandingTasks();
            tasks = null;
        }
    }

    /**
     * Create a synthetic task
     * <p>
     * This may only be called within a runnable passed to {@link #runTest(ThrowingRunnable)}
     *
     * @param <V> the synthetic task result type
     * @return the new synthetic task
     */
    public <V> SyntheticTask<V> newTask() {
        if (tasks == null) {
            throw new AssertionError("newTask requested outside of test method");
        }
        SyntheticTask<V> task = new SyntheticTask<>();
        tasks.add(task);
        return task;
    }

    private void checkTasks() {
        ArrayList<AssertionError> errors = new ArrayList<>();
        int i = 0;
        for (SyntheticTask<?> task : tasks) {
            try {
                task.checkErrors();
            } catch (Throwable t) {
                errors.add(new AssertionError("Error in task " + i + ": " + t, t));
            }
            i++;
        }

        if (errors.size() == 1) {
            throw errors.get(0);
        } else if (errors.size() > 1) {
            MultipleFailureException e = new MultipleFailureException("Multiple failures occurred in synthetic tasks");
            for (Throwable error : errors) {
                e.addFailure(error);
            }
            throw e;
        }
    }

    private void completeOutstandingTasks() {
        for (SyntheticTask<?> task : tasks) {
            task.complete();
        }
    }

    /**
     * A runnable that can throw an exception
     */
    @FunctionalInterface
    public static interface ThrowingRunnable {
        public void run() throws Exception;
    }

}
