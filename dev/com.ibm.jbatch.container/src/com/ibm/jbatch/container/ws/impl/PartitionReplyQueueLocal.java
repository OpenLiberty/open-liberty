/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws.impl;

import java.util.concurrent.LinkedBlockingQueue;

import com.ibm.jbatch.container.ws.PartitionReplyMsg;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;

/**
 * The local (same JVM) version of the PartitionReplyQueue.
 * 
 * It uses a LinkedBlockingQueue to send messages from the partition threads to
 * the top-level thread.
 */
public class PartitionReplyQueueLocal extends LinkedBlockingQueue<PartitionReplyMsg> implements PartitionReplyQueue {

    /**
     * Does nothing in the local case.
     */
    @Override
    public void close() {
        // no-op.
    }

	@Override
	public PartitionReplyMsg takeWithoutWaiting() {
		return null;
	}
}
