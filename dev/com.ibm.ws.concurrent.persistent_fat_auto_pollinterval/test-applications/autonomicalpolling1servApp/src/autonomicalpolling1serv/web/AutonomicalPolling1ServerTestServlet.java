/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/
package autonomicalpolling1serv.web;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/AutonomicalPolling1ServerTestServlet")
public class AutonomicalPolling1ServerTestServlet extends FATServlet {

	/**
	 * Maximum number of nanoseconds to wait for a task to finish.
	 */
	private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

	/**
	 * Cancel a task so that no subsequent executions occur.
	 */
	public void testCancelTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String jndiName = request.getParameter("jndiName");
		long taskId = Long.parseLong(request.getParameter("taskId"));

		PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

		TaskStatus<?> status = executor.getStatus(taskId);
		if (status != null)
			status.cancel(false);
	}

	/**
	 * testPersistentExecPolling - Verify the persistent executor delays are spaced out by the poll interval (1 second).
	 * For example, if there are 5 persistent executors, then their poll delays should round to 0,1,2,3,and 4 seconds.
	 * Also, tolerate -1,0,1,2,3 to allow for a poll that has already fired and is in-progress before computing its next time.
	 */
	public void testPersistentExecPolling(HttpServletRequest request, HttpServletResponse response) throws Exception {
		int numPersistentExecs = Integer.parseInt(request.getParameter("numPersistentExecs"));
		final long[] expectedDelays = new long[numPersistentExecs];
		final long[] alternateExpectedDelays = new long[numPersistentExecs];
		long delays[] = new long[numPersistentExecs];
		int attempts = 0;

		// initialize expectedDelays to {0,1,2...}
		// initialize alternateExpectedDelays to {-1,0,1...}
		for (int i = 0; i < expectedDelays.length; i++) {
			expectedDelays[i] = i;
			alternateExpectedDelays[i] = i - 1;
		}

		while (attempts < 100) {
			attempts++;
			collectDelays(delays);

			Arrays.sort(delays);
			System.out.println("persistent executor delays: " + Arrays.toString(delays));

			if (Arrays.equals(expectedDelays, delays) || Arrays.equals(alternateExpectedDelays, delays)) {
				// We got passing results.
				response.getWriter().println("PASSED");
				return;
			}
			// Wait at least one poll interval to try again.
			Thread.sleep(1100);
		}
		
		// We failed to get expected delay values. Returns the delay values we got for debug.
		response.getWriter().println(Arrays.toString(delays));
	}
	
	private void collectDelays(long[] delays) throws Exception {
		InitialContext context = new InitialContext();
		for( int i = 0; i < delays.length; i++) {
			PersistentExecutor persistentExec = (PersistentExecutor)context.lookup("persistent/exec" + i);
			Field f = persistentExec.getClass().getDeclaredField("pollingFutureRef");
			f.setAccessible(true);
			AtomicReference<ScheduledFuture<?>> pollingFutureRef = (AtomicReference<ScheduledFuture<?>>)f.get(persistentExec);
			delays[i] = pollingFutureRef.get().getDelay(TimeUnit.SECONDS);
			System.out.println("persistentExec.pollingFutureRef.getDelay(): " + pollingFutureRef.get().getDelay(TimeUnit.MILLISECONDS));
		}
	}
	
	/**
	 * testPollIntervalStable - verify the poll interval is happening at the correct rate and persistent executors are taking turns pulling.
	 */
	public void testPollIntervalStable(HttpServletRequest request, HttpServletResponse response) throws Exception {
		IncTask.reset();
		long start = System.nanoTime();
		while (IncTask.counter < 13 && System.nanoTime() - start < TIMEOUT_NS) {
			Thread.sleep(1000);
		}
		
		if (IncTask.counter < 13) {
			fail("Test timed out!   start = " + start + "; now = " + System.nanoTime());
		}
		
		System.out.println("IncTask.ranWithExec = " + Arrays.toString(IncTask.ranWithExec));
		// Verify the same persistent executor isn't being used every time.
		if (IncTask.ranWithExec[0] == IncTask.ranWithExec[1]) {
			response.getWriter().println("The same persistent executor was used twice in a row.");
			return;
		}
		
		for (int i = 0; i < IncTask.ranWithExec.length-3; i ++) {
			assertTrue(-1 != IncTask.ranWithExec[i]); // Verify that we mapped to persistent/exec1, persistent/exec2, or persistent/exec3.
			assertTrue(0 != IncTask.ranWithExec[i]); // Verify that the task actually ran for this "time slot".
			
			//Verify the same persistent executor is being used every 3 polls.
			if (IncTask.ranWithExec[i] != IncTask.ranWithExec[i+3]) {
				response.getWriter().println("The persistent executors are not rotating correctly.");
				return;
			}
		}
		
		//Passed all requirements.
		response.getWriter().println("PASSED");
	}

	/**
	 * Schedules a repeating task. The task id is written to the servlet output
	 */
	public void testScheduleRepeatingTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String jndiName = request.getParameter("jndiName");
		long initialDelayMS = Long.parseLong(request.getParameter("initialDelayMS"));
		long delayMS = Long.parseLong(request.getParameter("delayMS"));
		String testIdentifier = request.getParameter("test");

		IncTask task = new IncTask(testIdentifier);

		PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
		TaskStatus<?> status = executor.scheduleAtFixedRate(task, initialDelayMS, delayMS, TimeUnit.MILLISECONDS);
		long taskId = status.getTaskId();

		response.getWriter().println("Task id is " + taskId + ".");
	}
}
