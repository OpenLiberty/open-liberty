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

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.ibm.wsspi.requestContext.RequestContext;

/**
 * Queueable implementation of a request
 */
public class QueueableRequest implements Delayed {
	
	/** Request context of the actual request **/
	private final RequestContext requestContext;
	
	/** Time in nanoseconds when this request was queued **/
	private volatile long queueTime;
	
	/** Time in milliseconds this request should spend waiting in the queue **/
	private volatile long delay;
	
	public QueueableRequest(RequestContext requestContext, long delay){
		this.requestContext = requestContext;
		queueTime = System.nanoTime();
		if(delay > 0)
			this.delay = delay;
		else
			this.delay = 0;
	}
	
	@Override
	public int compareTo(Delayed delayed) {
		if(this == delayed)
			return 0;
	    long diff = getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS);
	    return (diff == 0) ? 0 : (diff < 0) ? -1 : 1;
	}

	/**
	 * Returns the time remaining for the request to be picked up.
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return (delay - TimeUnit.MILLISECONDS.convert(System.nanoTime() - queueTime, TimeUnit.NANOSECONDS));
	}
	
	public RequestContext getRequestContext() {
		return requestContext;
	}

	public boolean interruptRequest() {
		return false;
	}

	public long getInitialDelay(){
		return delay;
	}
	
	public void resetDelay(long delay){
		queueTime = System.nanoTime();
		if(delay > 0)
			this.delay = delay;
		else
			this.delay = 0;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (queueTime ^ (queueTime >>> 32));
		long seqNumber = requestContext.getRequestId().getSequenceNumber();
		result = prime * result + (int) (seqNumber ^ (seqNumber >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueueableRequest other = (QueueableRequest) obj;
		if (requestContext == null) {
			if (other.requestContext != null)
				return false;
		} else if (!requestContext.getRequestId().getId().
				equals(other.requestContext.getRequestId().getId()))
			return false;
		return true;
	}
}
