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
package com.ibm.ws.request.interrupt.status;

import java.util.List;

public class InterruptibleThreadObjectStatus {
	
	long threadId; 
	String requestId; 
	Boolean interrupted; 
	boolean givenUp; 
	String dispatchTimeString;
	long dispatchTimeLong;
	List<InterruptibleThreadObjectOdiStatus> odiStatusList = null;
	
	/**
	 * constructor
	 * 
	 * @param threadId             The thread id of the request.
	 * @param requestId            The request id of the request.
	 * @param interrupted          The interrupted flag. When this flag is set to true, the request has been interrupted at least once.
	 * @param givenUp              The given up flag. When this flag is set to true, the feature has finished trying to interrupt the request.
	 * @param dispatchTimeString   The dispatch time as a string.
	 * @param dispatchTimeLong     The dispatch time as a long.
	 */
	public InterruptibleThreadObjectStatus(long threadId, String requestId, Boolean interrupted, boolean givenUp, String dispatchTimeString, long dispatchTimeLong) {
		this.threadId = threadId;
		this.requestId = requestId;
		this.interrupted = interrupted;
		this.givenUp = givenUp;
		this.dispatchTimeString = dispatchTimeString;
		this.dispatchTimeLong = dispatchTimeLong;
	}

	public void addOdiStatus(List<InterruptibleThreadObjectOdiStatus> odiStatusList) {
		this.odiStatusList = odiStatusList;
		
	}
	
	/**
	 * Get the thread ID.
	 * 
	 * @return the thread ID.
	 */
	public long getThreadId() { 
		return threadId;
	}
	
    /**
     * Get the request ID.
     * 
     * @return the request ID.
     */
	public String getRequestId() {
		return requestId;
	}

	/**
	 * Get the dispatch time in string format.
	 * 
	 * @return the dispatch time in string format.
	 */
	public String getDispatchTimeString() {
		return dispatchTimeString;
	}

	/**
	 * Get the interrupted flag. When this flag is set to true, the request has been interrupted at least once.
	 * 
	 * @return the interrupted flag.
	 */
	public Boolean getInterrupted() {
		return interrupted;
	}

    /**
     * Get the given up flag. When this flag is set to true, the feature has finished trying to interrupt the request.
     * 
     * @return the given up flag.
     */
	public Boolean getGivenUp() {
		if (givenUp == true) {
			return Boolean.TRUE;
			
		} else {
			return Boolean.FALSE;
			
		}
	}

    /**
     * Get an array of InterruptibleThreadObjectOdiStatus objects that represents the status of each individual InterruptObject that is currently registered for the request.
     * 
     * @return an array of InterruptibleThreadObjectOdiStatus objects that represents the status of each individual InterruptObject that is currently registered for the request.
     */
	public List<InterruptibleThreadObjectOdiStatus> getOdiStatus() {
		return odiStatusList;
	}

	/**
	 * Get the dispatch time as a long.
	 * 
	 * @return the dispatch time as a long.
	 */
	public long getDispatchTimeLong() {
		return dispatchTimeLong;
	}
	
}
