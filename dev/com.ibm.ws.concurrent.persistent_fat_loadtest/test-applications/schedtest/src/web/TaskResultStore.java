/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;

/**
 * This class keeps the results from task executions, so they can be
 * validated later. The tasks themselves cannot keep state. The store
 * is application scoped so that all tasks can be injected with the
 * same instance.
 */
@ApplicationScoped
public class TaskResultStore {
    /**
     * Map of a result ID to its results. Note that the result ID is not
     * the persistent executor task ID.
     */
    private final Map<Integer, Result> resultMap = Collections.synchronizedMap(new HashMap<Integer, Result>());

    /**
     * Result ID counter.
     */
    private final AtomicInteger nextResultID = new AtomicInteger(0);

    /**
     * Debugging flag for the application.
     */
    private final boolean debugMode = false;

    /**
     * A scheduled executor which tasks can use to perform work which must occur outside a
     * managed task execution. For example, a managed task might use this to drive a remove()
     * operation to remove a task result from the persistent store.
     */
    private final ScheduledExecutorService unmanagedScheduledExecutor = Executors.newScheduledThreadPool(3);

    /**
     * The persistent executor reference.
     */
    @Resource(lookup = "concurrent/myScheduler")
    private PersistentExecutor scheduler;

    /**
     * Public no-arg constructor for Weld.
     */
    public TaskResultStore() {}

    /**
     * Gets the debug mode flag.
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Gets our unmanaged executor.
     */
    public ScheduledExecutorService getUnmanagedScheduledExecutor() {
        return unmanagedScheduledExecutor;
    }

    /**
     * Gets the persistent executor.
     */
    public PersistentExecutor getPeristentExecutor() {
        return scheduler;
    }

    /**
     * Creates a new result object in the task store. The returned result ID
     * is used to store task executions (using the taskExecuted() method).
     */
    public int createResult(Result r) {
        int id = nextResultID.getAndIncrement();
        resultMap.put(id, r);
        return id;
    }

    /**
     * Add a result to the task store.
     */
    public void taskExecuted(int resultID, boolean success, Object data) {
        if (debugMode)
            System.out.println(" !!TDK taskExecuted: " + resultID);

        Result r = resultMap.get(resultID);

        if (r == null) {
            throw new IllegalArgumentException("Result ID not found: " + resultID);
        }

        r.add(new Date(), success, data);
    }

    /**
     * Check the results for all result IDs.
     * 
     * @param ps The PrintStream where pass/fail results should be printed. If null,
     *            the results will not be printed anywhere.
     */
    public boolean check(PrintStream ps) {
        int resultCount = 0;
        synchronized (resultMap) {
            resultCount = resultMap.size();
            for (Entry<Integer, Result> e : resultMap.entrySet()) {
                Result r = e.getValue();
                if (r.check() == false) {
                    if (ps != null)
                        ps.println("Result failed for result ID " + e.getKey() + ": " + r.toString());
                    return false;
                }
            }
        }

        if (ps != null)
            ps.println("Checked " + resultCount + " results.  Pass.");

        return true;
    }

    /**
     * Clears the result store for another run.
     */
    public void clear() {
        resultMap.clear();
        nextResultID.set(0);
    }

    /**
     * Shutting down...
     */
    @PreDestroy
    public void cleanup() {
        unmanagedScheduledExecutor.shutdown();
    }

    /**
     * A task result.
     */
    public static interface Result {
        /**
         * Adds a task execution to the result. For one-shot tasks
         * this should get driven exactly once, and for recurring tasks
         * this can get driven multiple times.
         * 
         * @param executionTime The time that this task ran.
         * @param success True if the execution was successful, false if not.
         * @param data Any other data that the result needs for its processing.
         */
        public void add(Date executionTime, boolean success, Object data);

        /**
         * Checks that the actual results match the expected results.
         * 
         * @return true if the results hold, false if not. If false, additional
         *         information should be written to System.out.
         */
        public boolean check();
    }
}
