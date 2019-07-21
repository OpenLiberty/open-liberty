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
package com.ibm.ws.request.timing.queue;

import com.ibm.ws.request.timing.RequestTimingConstants;
import com.ibm.wsspi.requestContext.RequestContext;

public class SlowRequest extends QueueableRequest {
	
	private final long slowRequestThreshold;
	
	private final int slowRequestIterationsReq;
	
	private volatile int iterationsCompleted;
	
	private final boolean includeContextInfo;

	public SlowRequest(RequestContext requestContext, long delay, long slowRequestThreshold, boolean includeContextInfo, boolean interruptRequest){
		super(requestContext, delay);
		this.slowRequestThreshold = slowRequestThreshold;
		slowRequestIterationsReq = RequestTimingConstants.SLOW_REQUEST_ITERATIONS_REQ;
		iterationsCompleted = 0;
		this.includeContextInfo = includeContextInfo;
	}

	public long getSlowRequestThreshold() {
		return slowRequestThreshold;
	}
	
	public int getSlowRequestIterationsReq() {
		return slowRequestIterationsReq;
	}
	
	public int getIterationsCompleted() {
		return iterationsCompleted;
	}

	public int incIterationCount(){
		return ++iterationsCompleted;
	}
	
	public boolean includeContextInfo() {
		return includeContextInfo;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	public String toString() {
		StringBuffer slowReq = new StringBuffer();
		slowReq.append(String.format("%n"));
		if(getInitialDelay() == slowRequestThreshold)
			slowReq.append("-----------------------Slow Request Details-------------------------" + String.format("%n"));
		else
			slowReq.append("-------------------Probable Slow Request Details-------------------" + String.format("%n"));
		slowReq.append("Request Id : " + getRequestContext().getRequestId().getId() + String.format("%n"));
		slowReq.append("Request type : " + getRequestContext().getRootEvent().getType() + String.format("%n"));
		slowReq.append("Request state : " + getRequestContext().getRequestState() + String.format("%n"));
		slowReq.append("Iterations completed : " + getIterationsCompleted() + String.format("%n"));
		slowReq.append("Initial Delay (ms) : " + getInitialDelay() + String.format("%n"));
		slowReq.append("Slow request threshold mean (ms) : " + slowRequestThreshold + String.format("%n"));
		slowReq.append("Include Context Info : " + includeContextInfo + String.format("%n"));
		slowReq.append("-----------------------------------------------------------------------");
		return slowReq.toString();
	}

}
