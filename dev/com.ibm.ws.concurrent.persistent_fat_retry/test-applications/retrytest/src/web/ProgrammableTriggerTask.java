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
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * The programmable trigger task lets the user provide instructions as to how the task
 * should behave, with regards to pass/fail and when the next run time should be after
 * a passing run.
 * 
 * The behavior of this class with regards to storing the results and thread safety
 * are the same as the RetryCallable class.
 */
public class ProgrammableTriggerTask implements Callable<Void>, Trigger, Serializable {
	private static final long serialVersionUID = 4225974157844428345L;

	/** Possible actions when either getNextRunTime() or skipRun() are invoked. */
	static public enum Instruction { PASS, FAIL, SKIP, STATE_ERROR };
	
	/**
	 * Results from a given iteration of the task.  An iteration is logged here when
	 * the task completes a run (pass or fail), or when the task is skipped.
	 */
	private static final Map<Integer, List<Result>> resultMap = Collections.synchronizedMap(new HashMap<Integer, List<Result>>());

	/**
	 * Counter which provides a unique ID at object creation time.
	 */
	private static final AtomicInteger idGenerator = new AtomicInteger();
	
	/**
	 * Failure exception message
	 */
	private static final String FAILURE_MESSAGE = "Failing this task.";

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
	 * A single result from a single iteration of a ProgrammableTriggerTask.
	 */
	static public class Result {
		/** The time the result was created. */
		private Date callTime;
		
		/** The actual result.  We use the Instruction enum to record this. */
		private Instruction result;
		
		/** Constructor */
		public Result(Date callTime, Instruction result) {
			this.callTime = callTime;
			this.result = result;
		}
		
		/** Get the call time. */
		public Date getCallTime() { return callTime; }
		
		/** Get the result. */
		public Instruction getResult() { return result; }
	}
	
	/** My instructions. */
	private List<Instruction> myInstructions;
	
	/** My unique ID. */
	private final int id;
	
	/** Constructor. */
	public ProgrammableTriggerTask(List<Instruction> instructions) {
		myInstructions = instructions;
		id = ProgrammableTriggerTask.idGenerator.incrementAndGet();
	}
	
	/** Gets the ID. */
	public int getId() { return id; }
	
	/**
	 * Get the result list for the given id.
	 * 
	 * @return The list of results for the given id.
	 */
	List<Result> getResultList() {
		List<Result> resultList = null;
		synchronized(resultMap) {
			resultList = ProgrammableTriggerTask.resultMap.get(id);
			if (resultList == null) {
				resultList = Collections.synchronizedList(new LinkedList<Result>());
				ProgrammableTriggerTask.resultMap.put(id, resultList);
			}
		}
		return resultList;
	}

	/** 
	 * Figure out when we should run next.  Always run in 500 ms, until we reach then end of the instructions,
	 * at which point we stop running the task.
	 */
	@Override
	public Date getNextRunTime(LastExecution arg0, Date arg1) {
		Calendar currentTime = Calendar.getInstance();
		List<Result> resultsSoFar = getResultList();
		int expectedEntries = myInstructions.size();
		
        System.out.println("Task " + id + " getNextRunTime(" + resultsSoFar.size() +  ") " + currentTime.getTime());
		
        // We should not be called after a failure.  The retry interval should
        // determine the next run time.  If we are called after a failure,
        // add a state failure and throw an exception.
        int numberOfResults = resultsSoFar.size();
        if ((numberOfResults > 0) && (resultsSoFar.get(numberOfResults - 1).getResult() == Instruction.FAIL)) {
        	resultsSoFar.add(new Result(currentTime.getTime(), Instruction.STATE_ERROR));
        	throw new RuntimeException("Should not be driven for getNextRunTime if last result was FAIL");
        }
        
		if (resultsSoFar.size() == expectedEntries) {
			return null; // no further executions wanted
		} else {
			currentTime.add(Calendar.MILLISECOND, 500);
		}
		return currentTime.getTime();
	}

	/**
	 * Figure out if we should skip this run.  If our next instruction is to
	 * skip, then skip.  Otherwise, don't skip.
	 */
	@Override
	public boolean skipRun(LastExecution arg0, Date arg1) {
		Calendar currentTime = Calendar.getInstance();
		List<Result> resultsSoFar = getResultList();
		int numberOfResults = resultsSoFar.size();
		
		System.out.println("Task " + id + " skipRun(" + numberOfResults + ") " + currentTime.getTime());
		
		if (myInstructions.size() > numberOfResults) {
			Instruction currentInstruction = myInstructions.get(numberOfResults);
			if (currentInstruction == Instruction.SKIP) {
				resultsSoFar.add(new Result(currentTime.getTime(), Instruction.SKIP));
				return true;
			}
		}
		return false;
	}

	/**
	 * Run our task.
	 */
	@Override
	public Void call() throws Exception {
		Calendar currentTime = Calendar.getInstance();
		Date callDate = currentTime.getTime();
		List<Result> resultsSoFar = getResultList();
		int numberOfResults = resultsSoFar.size();
		
		System.out.println("Task " + id + " call(" + numberOfResults + ") " + currentTime.getTime());
		
		if (myInstructions.size() > numberOfResults) {
			Instruction currentInstruction = myInstructions.get(numberOfResults);
			switch(currentInstruction) {
			case SKIP:
				resultsSoFar.add(new Result(callDate, Instruction.STATE_ERROR));
				throw new Exception("State error");
			case FAIL:
				resultsSoFar.add(new Result(callDate, Instruction.FAIL));
				throw new Exception(ProgrammableTriggerTask.FAILURE_MESSAGE);
			case PASS:
				resultsSoFar.add(new Result(callDate, Instruction.PASS));
				return null;
			}
		}

		throw new Exception("I was driven too many times");
	}
}
