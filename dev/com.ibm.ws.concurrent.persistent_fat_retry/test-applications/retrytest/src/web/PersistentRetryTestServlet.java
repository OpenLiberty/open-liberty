/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import web.ProgrammableTriggerTask.Instruction;
import web.ProgrammableTriggerTask.Result;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

@WebServlet("/*")
public class PersistentRetryTestServlet extends HttpServlet {
    private static final long serialVersionUID = 8447513765214641067L;

    /**
     * Interval for polling task status (in milliseconds).
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(90);

    @Resource(name = "java:comp/env/concurrent/mySchedulerRef", lookup = "concurrent/myScheduler")
    private PersistentExecutor scheduler;

    /**
     * Default scheduling delay, in milliseconds.
     */
    private static final int DEFAULT_SCHEDULING_DELAY = 10;
    
    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        String idString = request.getParameter("taskid");
        PrintWriter out = response.getWriter();

        try {
            out.println(getClass().getSimpleName() + " is starting " + test + "<br>");
            System.out.println("-----> " + test + " starting");
            if (idString == null) {
            	getClass().getMethod(test, PrintWriter.class).invoke(this, out);
            } else {
            	long id = Long.parseLong(idString);
            	getClass().getMethod(test,  PrintWriter.class, Long.class).invoke(this, out, id);
            }
            System.out.println("<----- " + test + " successful");
            out.println(test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + " failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        } finally {
            out.flush();
            out.close();
        }
    }

    /**
     * Schedule a task that fails all execution attempts, and see that it is retried exactly once.
     */
    public void testRetryOnce(PrintWriter out) throws Exception {
    	RetryCallable task = new RetryCallable();

    	TaskStatus<Void> status = scheduler.schedule(task, DEFAULT_SCHEDULING_DELAY, TimeUnit.MILLISECONDS);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

    	if (!status.isDone() || status.isCancelled())
    		throw new Exception("Task did not complete. " + status);

    	Long taskID = status.getTaskId();
    	
    	try {
    		status.get();
    		throw new Exception("Task ought to exceed the failure limit and fail, not complete with result");
    	} catch (ExecutionException x) {
    		if (RetryCallable.isOurException(x.getCause()) == false)
    			throw x;
    	}

    	int callCount = RetryCallable.getCallCount(taskID);
    	if (callCount != 2) {
    		throw new Exception("Expected task to fail 2 times, but it failed " + callCount + " times.");
    	}
    }

    /**
     * Schedule a task that fails all execution attempts, and see that it is retried exactly once.
     * The retry interval is set to 60 seconds, make sure it's ignored and the retry happens
     * immediately (which is what is supposed to happen on the first retry).
     */
    public void testRetryOnceIgnoreInterval(PrintWriter out) throws Exception {
    	RetryCallable task = new RetryCallable();

    	TaskStatus<Void> status = scheduler.schedule(task, DEFAULT_SCHEDULING_DELAY, TimeUnit.MILLISECONDS);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

    	if (!status.isDone() || status.isCancelled())
    		throw new Exception("Task did not complete. " + status);

    	Long taskID = status.getTaskId();
    	
    	try {
    		status.get();
    		throw new Exception("Task ought to exceed the failure limit and fail, not complete with result");
    	} catch (ExecutionException x) {
    		if (RetryCallable.isOurException(x.getCause()) == false)
    			throw x;
    	}

    	int callCount = RetryCallable.getCallCount(taskID);
    	if (callCount != 2) {
    		throw new Exception("Expected task to fail 2 times, but it failed " + callCount + " times.");
    	}
    	
    	// Make sure that considerably less than the interval elapsed between the failures.
    	long elapsedTimeBetweenCalls = RetryCallable.computeCallMillisecondDifference(taskID, 1, 2); 
    	if (elapsedTimeBetweenCalls > (45L * 1000)) {
    		throw new Exception("Too much time between calls (" + elapsedTimeBetweenCalls + " ms)");
    	}
    }

    /**
     * Schedule a task that fails all execution attempts, and see that it is not retried.
     */
    public void testRetryZero(PrintWriter out) throws Exception {
    	RetryCallable task = new RetryCallable();

    	TaskStatus<Void> status = scheduler.schedule(task, DEFAULT_SCHEDULING_DELAY, TimeUnit.MILLISECONDS);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

    	if (!status.isDone() || status.isCancelled())
    		throw new Exception("Task did not complete. " + status);

    	Long taskID = status.getTaskId();

    	try {
    		status.get();
    		throw new Exception("Task ought to exceed the failure limit and fail, not complete with result");
    	} catch (ExecutionException x) {
    		if (RetryCallable.isOurException(x.getCause()) == false)
    			throw x;
    	}

    	int callCount = RetryCallable.getCallCount(taskID);
    	if (callCount != 1) {
    		throw new Exception("Expected task to fail 1 time, but it failed " + callCount + " times.");
    	}
    }
    
    /**
     * Schedule a task that fails all execution attempts, and see that it's retried twice and
     * that the default interval is used for all retries after the first one.
     */
    public void testRetryTwiceDefaultInterval(PrintWriter out) throws Exception {
    	RetryCallable task = new RetryCallable();

    	TaskStatus<Void> status = scheduler.schedule(task, DEFAULT_SCHEDULING_DELAY, TimeUnit.MILLISECONDS);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

    	if (!status.isDone() || status.isCancelled())
    		throw new Exception("Task did not complete. " + status);

    	Long taskID = status.getTaskId();

    	try {
    		status.get();
    		throw new Exception("Task ought to exceed the failure limit and fail, not complete with result");
    	} catch (ExecutionException x) {
    		if (RetryCallable.isOurException(x.getCause()) == false)
    			throw x;
    	}

    	int callCount = RetryCallable.getCallCount(taskID);
    	if (callCount != 3) {
    		throw new Exception("Expected task to fail 3 times, but it failed " + callCount + " times.");
    	}
    	
    	// Make sure that at least 10ms elapsed between 2nd and 3rd schedule.  We need to be a bit sensitive
    	// here because while the scheduling might occur 10ms apart, the task itself might not get control
    	// at exactly that interval.
    	long elapsedTimeBetweenCalls = RetryCallable.computeCallMillisecondDifference(taskID, 2, 3); 
    	if (elapsedTimeBetweenCalls < 5L) {
    		throw new Exception("Not enough time between failures (" + elapsedTimeBetweenCalls + " ms)");
    	}
    }

    /**
     * Schedule a task that fails all execution attempts, and see that it's retried twice and
     * that at least 5 seconds elapses between the 2nd and 3rd schedule attempt.
     */
    public void testRetryTwiceLongInterval(PrintWriter out) throws Exception {
    	RetryCallable task = new RetryCallable();

    	TaskStatus<Void> status = scheduler.schedule(task, DEFAULT_SCHEDULING_DELAY, TimeUnit.MILLISECONDS);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

    	if (!status.isDone() || status.isCancelled())
    		throw new Exception("Task did not complete. " + status);

    	Long taskID = status.getTaskId();

    	try {
    		status.get();
    		throw new Exception("Task ought to exceed the failure limit and fail, not complete with result");
    	} catch (ExecutionException x) {
    		if (RetryCallable.isOurException(x.getCause()) == false)
    			throw x;
    	}

    	int callCount = RetryCallable.getCallCount(taskID);
    	if (callCount != 3) {
    		throw new Exception("Expected task to fail 3 times, but it failed " + callCount + " times.");
    	}
    	
    	// Make sure that at least 5s elapsed between 2nd and 3rd schedule.  We need to be a bit sensitive
    	// here because while the scheduling might occur 5s apart, the task itself might not get control
    	// at exactly that interval.
    	long elapsedTimeBetweenCalls = RetryCallable.computeCallMillisecondDifference(taskID, 2, 3); 
    	if (elapsedTimeBetweenCalls < (4L * 1000)) {
    		throw new Exception("Not enough time between failures (" + elapsedTimeBetweenCalls + " ms)");
    	}
    }

    /**
     * Schedule a task that fails all execution attempts, and see that it's retried three times and
     * that the default interval is used for all retries after the first one.
     */
    public void testRetryThriceDefaultInterval(PrintWriter out) throws Exception {
    	RetryCallable task = new RetryCallable();

    	TaskStatus<Void> status = scheduler.schedule(task, DEFAULT_SCHEDULING_DELAY, TimeUnit.MILLISECONDS);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

    	if (!status.isDone() || status.isCancelled())
    		throw new Exception("Task did not complete. " + status);

    	Long taskID = status.getTaskId();

    	try {
    		status.get();
    		throw new Exception("Task ought to exceed the failure limit and fail, not complete with result");
    	} catch (ExecutionException x) {
    		if (RetryCallable.isOurException(x.getCause()) == false)
    			throw x;
    	}

    	int callCount = RetryCallable.getCallCount(taskID);
    	if (callCount != 4) {
    		throw new Exception("Expected task to fail 4 times, but it failed " + callCount + " times.");
    	}
    	
    	// Make sure that at least 10ms elapsed between 2nd and 3rd schedule, and 3rd and 4th schedule.  
    	// We need to be a bit sensitive here because while the scheduling might occur 10ms apart, the 
    	// task itself might not get control at exactly that interval.
    	long elapsedTimeBetweenCalls = RetryCallable.computeCallMillisecondDifference(taskID, 2, 3); 
    	if (elapsedTimeBetweenCalls < 5L) {
    		throw new Exception("Not enough time between 2nd and 3rd failures (" + elapsedTimeBetweenCalls + " ms)");
    	}
    	elapsedTimeBetweenCalls = RetryCallable.computeCallMillisecondDifference(taskID,3, 4);
    	if (elapsedTimeBetweenCalls < 5L) {
    		throw new Exception("Not enough time between 3rd and 4th failures (" + elapsedTimeBetweenCalls + " ms)");
    	}
    	elapsedTimeBetweenCalls = RetryCallable.computeCallMillisecondDifference(taskID, 2, 4);
    	if (elapsedTimeBetweenCalls < 15L) {
    		throw new Exception("Not enough time between 2nd and 4th failures (" + elapsedTimeBetweenCalls + " ms)");
    	}
    	elapsedTimeBetweenCalls = RetryCallable.computeCallMillisecondDifference(taskID, 1, 4);
    	if (elapsedTimeBetweenCalls < 15L) {
    		throw new Exception("Not enough time between 1st and 4th failures (" + elapsedTimeBetweenCalls + " ms)");
    	}
    }
    
    /**
     * Schedule a task with a trigger, that runs 6 times total.  Fails #1-3 and 5.  Retry limit is set
     * to 3, we're making sure that the limit is not reached since the 4 failures are not consecutive.
     */
    public void testRetrySixWithTwoPasses(PrintWriter out) throws Exception {
    	List<Instruction> instructions = new ArrayList<Instruction>(6);
    	instructions.addAll(Arrays.asList(Instruction.FAIL, Instruction.FAIL, Instruction.FAIL,
    			Instruction.PASS, Instruction.FAIL, Instruction.PASS));
    	ProgrammableTriggerTask task = new ProgrammableTriggerTask(instructions);
    	
    	TaskStatus<Void> status = scheduler.schedule(task, task);

        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

        if (status != null) {
        	status.cancel(true);
        	throw new Exception("Task did not end successfully and autopurge within allotted interval. " + status);
        }

    	final List<Instruction> results = new ArrayList<Instruction>();
    	for (Result result : task.getResultList())
    		results.add(result.getResult());
        if (!instructions.equals(results))
        	throw new Exception("Results were inconsistent: " + results);
    }

    /**
     * Schedule a task with a trigger, that runs 4 times total.  Fails #1-3 and pass 4.  Put a skip
     * somewhere between 1 and 3 to make sure it's not counted as a failure.
     */
    public void testRetryFourWithOneSkip(PrintWriter out) throws Exception {
    	List<Instruction> instructions = new ArrayList<Instruction>(6);
    	instructions.addAll(Arrays.asList(Instruction.FAIL, Instruction.FAIL, Instruction.FAIL,
    			Instruction.SKIP, Instruction.PASS));
    	ProgrammableTriggerTask task = new ProgrammableTriggerTask(instructions);
    	
    	TaskStatus<Void> status = scheduler.schedule(task, task);

        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

        if (status != null) {
        	status.cancel(true);
        	throw new Exception("Task did not end successfully and autopurge within allotted interval. " + status);
        }

    	final List<Instruction> results = new ArrayList<Instruction>();
    	for (Result result : task.getResultList())
    		results.add(result.getResult());
        if (!instructions.equals(results))
        	throw new Exception("Results were inconsistent: " + results);
    }

    /**
     * Schedule a task with a trigger, that runs 4 times total.  Fails #1-4.  Put a skip
     * somewhere between to make sure that the skip does not reset the consecutive fail count.
     */
    public void testRetryFourWithOneSkipFail(PrintWriter out) throws Exception {
    	List<Instruction> instructions = new ArrayList<Instruction>(6);
    	instructions.addAll(Arrays.asList(Instruction.FAIL, Instruction.FAIL, Instruction.FAIL,
    			Instruction.SKIP, Instruction.FAIL));
    	ProgrammableTriggerTask task = new ProgrammableTriggerTask(instructions);
    	
    	TaskStatus<Void> status = scheduler.schedule(task, task);

        for (long start = System.nanoTime(); status.getNextExecutionTime() != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

        if (status.getNextExecutionTime() != null) {
        	status.cancel(true);
        	throw new Exception("Task should be finished after 4 failures" + status);
        }

		try {
			status.get();
			throw new Exception("Should have thrown exception from status.get()");
		} catch (ExecutionException x) {
    		if (ProgrammableTriggerTask.isOurException(x.getCause()) == false)
    			throw x;
		}
    	
    	final List<Instruction> results = new ArrayList<Instruction>();
    	for (Result result : task.getResultList())
    		results.add(result.getResult());
        if (!instructions.equals(results))
        	throw new Exception("Results were inconsistent: " + results);
    }
    
    
    /**
     * Schedule a task that fails all four execution attempts, with Autopurge set to True.
     * Confirm that TaskStatus is null;
     */
    public void testRetryFourTimesAutoPurgeAlways(PrintWriter out) throws Exception {    	
    	
    	RetryCallable task = new RetryCallable();
    	task.setExecutionProperty(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.toString());
    	
    	TaskStatus<Void> status = scheduler.schedule(task, DEFAULT_SCHEDULING_DELAY, TimeUnit.MILLISECONDS);
    	Long taskID = status.getTaskId();


        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());
   	
    	if (status != null)
    		throw new Exception("Task was not autopurged in the alloted time (" + TIMEOUT_NS + "). Status was:" + status);
    	
    	if ( RetryCallable.getCallCount(taskID) != 4)
    		throw new Exception("Task call count was not 4");
    }

    /**
     * Schedule a task with a trigger, that runs 5 times total.  Fails #1-4, pass 5.  Retry limit is set
     * to -1.
     */
    public void testRetryUnlimited(PrintWriter out) throws Exception {
    	List<Instruction> instructions = new ArrayList<Instruction>(6);
    	instructions.addAll(Arrays.asList(Instruction.FAIL, Instruction.FAIL, Instruction.FAIL,
    			Instruction.FAIL, Instruction.PASS));
    	ProgrammableTriggerTask task = new ProgrammableTriggerTask(instructions);
    	
    	TaskStatus<Void> status = scheduler.schedule(task, task);

        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

        if (status != null) {
        	status.cancel(true);
        	throw new Exception("Task did not end successfully and autopurge within allotted interval. " + status);
        }

    	final List<Instruction> results = new ArrayList<Instruction>();
    	for (Result result : task.getResultList())
    		results.add(result.getResult());
        if (!instructions.equals(results))
        	throw new Exception("Results were inconsistent: " + results);
    }
    
    /**
     * This is the first part of the retry count wrap test.
     * We'll schedule a task that always fails, retrieve the ID, and return it
     * so that the FAT can manually modify it.
     */
    public void testRetryCountWrap_1(PrintWriter out) throws Exception {
    	RetryCallable task = new RetryCallable();

    	// Schedule an 'always fail' task.
    	TaskStatus<Void> status = scheduler.schedule(task, DEFAULT_SCHEDULING_DELAY, TimeUnit.MILLISECONDS);

    	// Let it run at least once.  Also get the task ID so that we can pass it
    	// back to the FAT and check on it after we recycle the server.
    	Long taskID = status.getTaskId();
        for (long start = System.nanoTime(); ((RetryCallable.getCallCount(taskID) <= 0) && (System.nanoTime() - start < TIMEOUT_NS)); Thread.sleep(POLL_INTERVAL))
    		status = scheduler.getStatus(status.getTaskId());

        // Make sure we're not done somehow.
    	if (status.getNextExecutionTime() == null)
    		throw new Exception("Task should not have completed. " + status);

    	// Return the task ID in the servlet's output so that the FAT can find
    	// it.
    	out.println("TASK_ID=" + taskID);
    }
    
    /* Note: testRetryCountWrap_2 is in UpdateDatabaseServlet. */
    
    /**
     * This is the third part of the retry count wrap test.
     * We'll check that the task is running (it should run after the initial poll
     * delay fires) and then that the retry count does not increment past the max.
     */
    public void testRetryCountWrap_3(PrintWriter out, Long id) throws Exception {
    	// Wait for our task to start running
        for (long start = System.nanoTime(); ((RetryCallable.getCallCount(id) < 5) && (System.nanoTime() - start < TIMEOUT_NS)); Thread.sleep(POLL_INTERVAL))
    		;
        if (RetryCallable.getCallCount(id) < 5) {
        	throw new Exception("Our task did not run.  ID=" + id);
        }
        
    	// Go get the status of our task.
        TaskStatus<Void> status = scheduler.getStatus(id);
    	if (status == null) {
    		throw new Exception("Could not retrieve status for our task id " + id);
    	}
    	
        // Make sure we're not done somehow.
    	if (status.getNextExecutionTime() == null)
    		throw new Exception("Task should not have completed. " + status);

    	// Check on the retry count.  After 5 intervals we should have reached the max
    	// value.
    	Context ic = new javax.naming.InitialContext();
    	DataSource ds = (DataSource)ic.lookup("java:comp/env/jdbc/db");
    	Connection c = ds.getConnection();
    	c.setAutoCommit(true);

    	String query = new String("SELECT RFAILS FROM APP.SCHDTASK WHERE ID=" + id.toString());
    	System.out.println("Running SQL: " + query);
    	Statement s = c.createStatement();
    	boolean sqlResult = s.execute(query);
    	if (sqlResult == false) {
    		throw new Exception("Could not retrieve failure count from database.");
    	}
    	
    	ResultSet rs = s.getResultSet();
    	rs.next();
    	short retryCount = rs.getShort(1);
    	rs.close();
    	s.close();
    	c.close();

    	if (retryCount != Short.MAX_VALUE) {
    		throw new Exception("Retry count for task " + id + " should have been " + Short.MAX_VALUE + ", but it was " + retryCount);
    	}
    	
    	// Go be nice and cancel our task.
    	scheduler.remove(id);
    }
}
