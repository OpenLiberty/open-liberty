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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Wraps around the delay queue to provide the relevant functionality for request timing **/
public class DelayedRequestQueue<R extends QueueableRequest> {

	/** Delay queue for holding requests **/
	private final DelayQueue<R> requestQueue;
	
	/** Used for keeping track of requests which are being processed **/
	private final Set<String> queuedRequests;
	
	private final ReadWriteLock rwl = new ReentrantReadWriteLock();
	
	private final Lock r = rwl.readLock();
	
	private final Lock w = rwl.writeLock();
	
	public DelayedRequestQueue(){
		requestQueue = new DelayQueue<R>();
		queuedRequests = new HashSet<String>();
	}
	
	/** 
	 * Method for adding a new request.
	 * @param request : Queueable request
	 * @return true if the operation was successful else false.
	 */
	public boolean addRequest(R request){
		String requestId = request.getRequestContext().getRequestId().getId();
		boolean retVal = false;
		w.lock();
		try{
			if(!queuedRequests.contains(requestId)){
				queuedRequests.add(requestId);
				requestQueue.put(request);
				retVal = true;
			}
		}finally{
			w.unlock();
		}
		return retVal;
	}
	
	/** 
	 * Method for removing the request. 
	 * @param request : Queueable request
	 **/
	public void removeRequest(R request){
		String requestId = request.getRequestContext().getRequestId().getId();
		w.lock();
		try{
			queuedRequests.remove(requestId);
		}finally{
			w.unlock();
		}
	}
	
	/**
	 * Method for re-queuing requests. This should only be used for adding existing requests. 
	 * For adding new requests, addRequest(R request) method should be used.
	 * @param request : Queueable request
	 */
	public void requeueRequest(R request){
		String requestId = request.getRequestContext().getRequestId().getId();
		r.lock();
		try{
			if(queuedRequests.contains(requestId))
				requestQueue.put(request);
		}finally{
			r.unlock();
		}
	}
	
	/** 
	 * Method for getting the next request in line for processing.
	 * This will discard any request which was removed using the removeRequest(R request) method.
	 * @return Next request in line for processing.
	 **/
	public R processNext() throws InterruptedException{
		R request = requestQueue.take();
		r.lock();
		try{
			if(queuedRequests.contains(request.getRequestContext().getRequestId().getId()))
				return request;
		}finally{
			r.unlock();
		}
		return processNext();
	}
	
	/**
	 * Removes all requests from this queue. 
	 * Requests with have not been processed are not waited for; they are simply discarded from the queue.
	 */
	public void clear(){
		w.lock();
		try{
			requestQueue.clear();
			queuedRequests.clear();
		}finally{
			w.unlock();
		}
	}
}
