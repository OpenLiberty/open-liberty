/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import com.ibm.ws.http.channel.h2internal.exceptions.FlowControlException;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 *
 */
public interface H2WorkQInterface {

    public static enum Q_STATUS {
        NOT_IN_USE, // queue is not being monitored/used, so process next write on calling thread
        BYPASSED, // a write calling thread is in progress, it bypassed the not in use queue
        STAND_BY, // a write was requested while a write was outstanding, so start using the queue once the outstanding write is complete
        ACTIVE, // a queue monitoring thread is now processing the queue
        QUIT, // stop using the queue, stop servicing additional writes
        FINISHED // this code is done using the queue after being told to quit
    };

    public static enum WRITE_ACTION {
        NOT_SET, // the initial state a write will start in
        COMPLETED, // the write code has completed this write request
        QUEUED, // this write request has been put on the queue
        PENDING_CALLBACK, // this write request has been invoked at the TCP Channel, could not complete right away, and the callback will be used
        CONNECTION_QUIT, // write was not completed because the connection has be told to quit
        CONFUSED // the code got to an unexpected place, a bug somewhere
    };

    public void init(TCPWriteRequestContext x, H2MuxTCPWriteCallback c);

    public void notifyStandBy();

    public void setToQuit(boolean inDrainQ);

    public WRITE_ACTION writeOrAddToQ(H2WriteQEntry n) throws FlowControlException;

    public void addNewNodeToQ(int streamID, int parentStreamID, int priority, boolean exclusive);

    public boolean removeNodeFromQ(int streamID);

    public boolean updateNodeFrameParameters(int streamID, int newPriority, int newParentStreamID, boolean exclusive);

    public void incrementConnectionWindowUpdateLimit(int increment) throws FlowControlException;

    public void decreaseConnectionWindowUpdateWriteLimit(int decrease);

    public int getConnectionWriteLimit();

}
