/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.request.timing.queue;

import com.ibm.wsspi.requestContext.RequestContext;

public class HungRequest extends QueueableRequest {
	
	private final long hungRequestThreshold;
	
	private final boolean includeContextInfo;
	
	private final boolean interruptRequest;
	
	private final boolean enableThreadDumps;
	
	public HungRequest(RequestContext requestContext, long delay, long hungRequestThreshold, boolean includeContextInfo, boolean interruptRequest, boolean enableThreadDumps){
		super(requestContext, delay);
		this.hungRequestThreshold = hungRequestThreshold;
		this.includeContextInfo = includeContextInfo;
		this.interruptRequest = interruptRequest;
		this.enableThreadDumps = enableThreadDumps;
	}
	
	public long getHungRequestThreshold() {
		return hungRequestThreshold;
	}
	
	public boolean includeContextInfo() {
		return includeContextInfo;
	}
	
	@Override
	public boolean interruptRequest() {
		return interruptRequest;
	}
	
	@Override
	public boolean isThreadDumpsEnabled() {
		return enableThreadDumps;
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
		StringBuffer hungReq = new StringBuffer();
		hungReq.append(String.format("%n"));
		if(getInitialDelay() == hungRequestThreshold)
			hungReq.append("-----------------------Hung Request Details-------------------------" + String.format("%n"));
		else
			hungReq.append("-------------------Probable Hung Request Details-------------------" + String.format("%n"));
		hungReq.append("Request Id : " + getRequestContext().getRequestId().getId() + String.format("%n"));
		hungReq.append("Request type : " + getRequestContext().getRootEvent().getType() + String.format("%n"));
		hungReq.append("Request state : " + getRequestContext().getRequestState() + String.format("%n"));
		hungReq.append("Initial Delay (ms) : " + getInitialDelay() + String.format("%n"));
		hungReq.append("Hung request threshold mean (ms) : " + hungRequestThreshold + String.format("%n"));
		hungReq.append("Include Context Info : " + includeContextInfo + String.format("%n"));
		hungReq.append("Enable Thread Dumps : " + enableThreadDumps + String.format("%n"));
		hungReq.append("-----------------------------------------------------------------------");
		return hungReq.toString();
	}
}
