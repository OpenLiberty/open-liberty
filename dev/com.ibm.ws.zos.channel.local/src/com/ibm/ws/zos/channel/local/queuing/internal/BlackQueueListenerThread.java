/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing.internal;

import java.io.PrintWriter;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.ws.zos.channel.local.internal.LocalCommConnLink;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequestType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
/**
 * Thread listening for WorkRequests arriving thru the native
 * Work Request queue (aka "black queue") anchored off BBGZLCOM.
 * 
 * This thread is activated when LocalCommChannel is started.
 * 
 * The thread dips native and blocks until black queue work requests become available.
 * Once available, the black queue work requests are returned to Java and wrapped in a List
 * of NativeWorkRequest objects.  The thread then schedules each work request to the
 * main Liberty executor service (to get the work off this thread). 
 * 
 * Once all work requests are scheduled, this thread goes back native to wait for
 * further black queue work requests.
 * 
 */
public class BlackQueueListenerThread extends Thread {

    /**
     * Trace component for this class.
     */
    private final static TraceComponent tc = Tr.register(BlackQueueListenerThread.class);

    /**
	 * The number of elements in 'blackQueueTasks' that we allow before telling the
	 * native code that we have work to do.  Ideally this is computed based on the
	 * number of current thread pool threads, but I haven't figured out how to do that
	 * yet.  Remember that one element in the queue = one connection, not one request.
	 */
	private static final int BLACK_QUEUE_TASKS_THRESHOLD = 1;
	
    /**
     * The backbone of the native side of the local comm channel.
     * Provides references to NativeRequestHandler and BlackQueueDemultiplexor.
     */
    private final LocalChannelProviderImpl localChannelProviderImpl;

    /**
     * Flag that's checked to see if we're terminating.
     */
    private volatile boolean keepGoing = true;

    /**
     * Counter used for sorting black queue tasks chronologically.
     */
    private final AtomicLong taskCounter = new AtomicLong(Long.MIN_VALUE);
    
    /**
     * All black queue tasks created by this listener thread are stored in this data
     * structure.  In the event the listener thread dips native to get more local comm
     * work, but there is none, it will return here and try to process one or more of
     * the black queue tasks in this data structure, starting with the oldest.
     */
    private final SortedSet<BlackQueueTask> blackQueueTasks = new TreeSet<BlackQueueTask>();
    
    private final AtomicLong executorHitCount = new AtomicLong(0L);
    private final AtomicLong executorMissCount = new AtomicLong(0L);
    private final AtomicLong listenerHitCount = new AtomicLong(0L);
    private final AtomicLong listenerMissCount = new AtomicLong(0L);
    
    /**
     * Constructor.
     */
    BlackQueueListenerThread(LocalChannelProviderImpl localChannelProviderImpl) {
        this.localChannelProviderImpl = localChannelProviderImpl;
        this.setDaemon(true);
        this.setName("z/OS LCOM Native WRQ Listener Thread");
    }

    /**
     * Retrieve black queue work requests from native and schedule them to the executor service.
     */
    @Override
    public void run() {
        try {
            localChannelProviderImpl.getNativeRequestHandler().initWRQFlags();
            while (keepGoing) {
            	// See if there's something we can do, in the event there are no more local comm
            	// events to read.
            	boolean canDoOtherWork = false;
            	int currentBlackQueueTasksSize = -1;
            	synchronized(blackQueueTasks) {
            		currentBlackQueueTasksSize = blackQueueTasks.size();
            		canDoOtherWork = currentBlackQueueTasksSize >= BLACK_QUEUE_TASKS_THRESHOLD;
            	}
            	
            	if (tc.isDebugEnabled()) {
            		Tr.debug(tc, "Going native to get work, taskQueueSize = " + currentBlackQueueTasksSize + ", otherWork = " + canDoOtherWork);
            	}
            	
            	// Go see if there's more local comm events to read and dispatch.
            	List<NativeWorkRequest> nativeWorkRequests = localChannelProviderImpl.getNativeRequestHandler().getWorkRequestElements(canDoOtherWork);
            	
            	if (tc.isDebugEnabled()) {
            		Tr.debug(tc, "Returned from native get work, nativeWorkRequests = " + nativeWorkRequests);
            	}
            	
            	if (nativeWorkRequests != null) {
            		for (NativeWorkRequest nativeWorkRequest : nativeWorkRequests) {
            			scheduleWorkRequest(nativeWorkRequest);
            		}
                } else {
                	// There were no local comm events to read, and we indicated we had other work
                	// we could be doing.  So, go do that.
                	synchronized(blackQueueTasks) {
                		if (blackQueueTasks.isEmpty() == false) {
                			listenerHitCount.incrementAndGet();
                			BlackQueueTask firstTask = blackQueueTasks.first();
                			if (blackQueueTasks.remove(firstTask)) { 
                				firstTask.processTask();
                			}
                		} else {
                			listenerMissCount.incrementAndGet();
                		}
                	}
                }
            }
        } catch (Exception e) {
            // FFDC.
        }

    }

    /**
     * Stop the thread. Tell the native code to stop listening for black queue requests.
     */
    public void end() {
        // Need to wake this thread up from a Native wait.
        if (keepGoing) {
            keepGoing = false;
            int localRC = localChannelProviderImpl.getNativeRequestHandler().ntv_stopListeningForRequests();
            if (localRC == -1) {
                // Connect response failed ... issue message.
                LocalCommServiceResults.getLComServiceResult().issueLComServiceMessage();
            } 
        }
    }
    
    /**
     * Schedule black queue work request to the executor thread pool.
     */
    private void scheduleWorkRequest(NativeWorkRequest nativeWorkRequest) {
      
        obtainDisconnectLock(nativeWorkRequest);
        BlackQueueTask bqt = new BlackQueueTask(nativeWorkRequest);
        synchronized(blackQueueTasks) {
        	blackQueueTasks.add(bqt);
        }
        localChannelProviderImpl.getExecutorService().execute(bqt);
    }
    
    /**
     * See LocalCommConnLink.waitForDisconnectLock() for explanation.
     * 
     * Note that we're naturally serialized here (this thread is the only thread 
     * reading directly off the native black queue).  So we're guaranteed to
     * process the READREADY before the DISCONNECT (assuming they were put on 
     * the queue in order).  After submitting to the ExecutorService, however,
     * all bets are off wrt ordering.
     * 
     * @return true if the lock was obtained; false otherwise.
     */
    private boolean obtainDisconnectLock(NativeWorkRequest nativeWorkRequest) {
        
        if (nativeWorkRequest.getRequestType() == NativeWorkRequestType.REQUESTTYPE_READREADY) {
            LocalCommConnLink connLink = localChannelProviderImpl.getConnHandleToConnLinkMap().get( nativeWorkRequest.getClientConnectionHandle() );
            if (connLink != null) {
                return connLink.obtainDisconnectLock();
            }
        }
        
        return false;
    }

    /**
     * Run the black queue work request on its own task.  This task uses the
     * BlackQueueDemultiplexor to find the appropriate callback handler for the 
     * work request.
     */
    class BlackQueueTask implements Runnable, Comparable<BlackQueueTask> {
        private final NativeWorkRequest nativeWorkRequest;
        private final long relativeTime = taskCounter.getAndIncrement();
        
        public BlackQueueTask(NativeWorkRequest nativeWorkRequest) {
            this.nativeWorkRequest = nativeWorkRequest;
        }
        
        /**
         * See LocalCommConnLink.waitForDisconnectLock() for explanation.
         * 
         * Note: we must do the wait BEFORE calling BlackQueueDemultiplexor.dispatch()
         * because dispatch() will remove the DISCONNECT callback from the map, which will
         * prevent anyone else from registering a new callback on the connection (e.g another 
         * READREADY callback).
         * 
         */
        protected void waitForDisconnectLock() {
            if (nativeWorkRequest.getRequestType() == NativeWorkRequestType.REQUESTTYPE_DISCONNECT) {
                LocalCommConnLink connLink = localChannelProviderImpl.getConnHandleToConnLinkMap().get( nativeWorkRequest.getClientConnectionHandle() );
                if (connLink != null) {
                    connLink.waitForDisconnectLock(3 * 1000);    // at most 3 seconds
                }
            }
        }

        @Override
        public void run() {
        	boolean removed = false;
        	synchronized(blackQueueTasks) {
        		removed = blackQueueTasks.remove(this);
        	}
        	if (removed) {
        		executorHitCount.incrementAndGet();
        		processTask();
        	} else {
        		executorMissCount.incrementAndGet();
        	}
        }
        
        private void processTask() {
            try {
                waitForDisconnectLock();
                ((BlackQueueDemultiplexorImpl)localChannelProviderImpl.getBlackQueueDemultiplexor()).dispatch(nativeWorkRequest);
            } catch (CallbackNotFoundException cnfe) {
                // Issue message? Or just FFDC it?
                // Tr.warning(tc, "INVALID_NATIVE_REQUESTTYPE", Integer.valueOf(localReqType));
            } catch (Throwable t) {
                // Just FFDC it.
            }
        }

        @Override
        public int hashCode() {
        	return (int)(relativeTime % 31L);
        }
        
        @Override
        public boolean equals(Object o) {
        	if (o instanceof BlackQueueTask) {
        		return (this.compareTo((BlackQueueTask) o) == 0);
        	} else {
        		return false;
        	}
        }
        
		@Override
		public int compareTo(BlackQueueTask o) {
			if (this.relativeTime < o.relativeTime) {
				return -1;
			} else if (this.relativeTime > o.relativeTime) {
				return 1;
			} else {
				return 0;
			}
		}
    }

	public void printStatistics(PrintWriter writer) {
		writer.println("------------------------------------");
		writer.println("BlackQueueListenerThread statistics:");
		writer.println();
		writer.println("Executor hit/miss " + executorHitCount.get() + "/" + executorMissCount.get());
		writer.println("Listener hit/miss " + listenerHitCount.get() + "/" + listenerMissCount.get());
		writer.println("------------------------------------");
	}
}