/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws;


/**
 * The queuing API used by a partitioned step to send and recv data between the
 * top-level thread and the partitioned threads.
 * 
 * There are two implementations:
 * 1. PartitionReplyQueueLocal - for local partitions (same JVM)
 * 2. PartitionReplyQueueJms - for remote partitions (multi-JVM)
 * 
 */
public interface PartitionReplyQueue  {

    /**
     * Receive a msg from the queue.  
     * 
     * The top-level thread calls this method to recv data from the sub-job partition threads.
     */
    public PartitionReplyMsg take() throws InterruptedException;
    
    /**
     * Add a msg to the queue.
     * 
     * The partition threads call this method to send data back to the top-level thread.
     */
    public boolean add(PartitionReplyMsg msg) ;
    
    /**
     * Close the queue.
     */
    public void close() ;

    /**
     * Receive a msg from the queue without waiting for it indefinitely. 
     * 
     * The top-level thread calls this method to recv data from the sub-job partition threads.
     */
	public PartitionReplyMsg takeWithoutWaiting();

}
