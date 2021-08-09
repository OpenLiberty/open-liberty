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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;

/**
 * Callable object used by the PersistentRetryTestServlet class.  The object instance
 * is assigned a unique index and when the call() method is invoked, will append the 
 * call time to a list of call times for this object.  The index is the task ID.
 * 
 * This class was intended to be used by scheduling the object instance with the
 * persistent scheduled executor, and then waiting for the scheduled task to be
 * finished (either in error, or completed).  At that point the call times can be
 * accessed and used to compute the duration between calls, or the overall call
 * count.
 * 
 * This class was written to be thread-safe, but this aspect of the code has not
 * been tested.
 */
public class RetryCallable implements Callable<Void>, Serializable, ManagedTask {
	private static final long serialVersionUID = -1548505531088564709L;

	/**
	 * Map of call times.  Since the task itself cannot keep any state,
	 * we cheat and keep it here.  Each instance gets a key, which is used
	 * to get its call count from the map.
	 */
	private static final Map<Long, List<Date>> callTimeMap = Collections.synchronizedMap(new HashMap<Long, List<Date>>());
	private Map<String, String> executionProperties = new HashMap<String, String>(); 
	
	/**
	 * Failure exception message
	 */
	private static final String FAILURE_MESSAGE = "Failing this task.";

	/**
	 * Get the number of times this task was called with the same task id.
	 * 
	 * @param id The ID of the task which we want to get the call count for
	 * 
	 * @return The number of times the task with the given ID was called.
	 */
	public static int getCallCount(Long id) {
		return getCallTimeList(id).size();
	}
	
	/**
	 * Get a specific call time.  The number 1 represents the first call.
	 * 
	 * @param id The ID of the task which we want to get the call time for.
	 * @param index The call number to get.  1 is the first call.
	 * 
	 * @return The time of the requested call.
	 */
	public static Date getCallTime(Long id, int index) {
		return getCallTimeList(id).get(index - 1);
	}
	
	/**
	 * Compute the difference in milliseconds between two calls for the same task.  The number 1
	 * represents the first call.
	 * 
	 * @param id The ID of the task which we want to get the call times for.
	 * @param firstCall The index of the earlier call.  1 is the first call.
	 * @param secondCall The index of the later call.  1 is the first call.
	 * 
	 * @return The number of milliseconds that elapsed between the two calls.
	 */
	public static long computeCallMillisecondDifference(Long id, int firstCall, int secondCall) {
		return getCallTime(id, secondCall).getTime() - getCallTime(id, firstCall).getTime();
	}

    /**
     * See if the exception we caught was generated from an instance of
     * this class.
     */
	public static boolean isOurException(Throwable t) {
		return ((t != null) &&
				(t instanceof Exception) &&
				(t.getMessage() != null) &&
				(t.getMessage().equals(FAILURE_MESSAGE))); 
	}

	/**
	 * Get the call time list for the given task id.
	 * 
	 * @param id The ID of the task whose call list we want to get.
	 * 
	 * @return The list of call times for the given task id.
	 */
	private static List<Date> getCallTimeList(Long id) {
		List<Date> callTimeList = null;
		synchronized(callTimeMap) {
			callTimeList = RetryCallable.callTimeMap.get(id);
			if (callTimeList == null) {
				callTimeList = Collections.synchronizedList(new LinkedList<Date>());
				RetryCallable.callTimeMap.put(id, callTimeList);
			}
		}
		return callTimeList;
	}
	
	/**
	 * Called by the persistent managed executor.
	 */
	@Override
	public Void call() throws Exception {
		Long taskId = TaskIdAccessor.get();
		if (taskId != null) {
			Date currentTime = Calendar.getInstance().getTime();
			getCallTimeList(taskId).add(currentTime);
			throw new Exception(FAILURE_MESSAGE);
		} else {
			throw new Exception("Not invoked on a persistent executor thread");
		}
	}

	@Override
	public Map<String, String> getExecutionProperties() {
		return executionProperties;
	}
	
	
	public void setExecutionProperty(String key, String value) {
		executionProperties.put(key, value);
	}

	@Override
	public ManagedTaskListener getManagedTaskListener() {
		return null;
	}
}