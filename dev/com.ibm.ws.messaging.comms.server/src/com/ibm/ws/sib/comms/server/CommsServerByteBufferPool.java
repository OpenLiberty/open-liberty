/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsByteBufferPool;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The comms server byte buffer pool.
 * 
 * @author Gareth Matthews
 */
public class CommsServerByteBufferPool extends CommsByteBufferPool {
    /** Trace */
    private static final TraceComponent tc = SibTr.register(CommsServerByteBufferPool.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    /** The singleton instance of this class */
    private static CommsServerByteBufferPool instance = null;

    /**
     * @return Returns the byte buffer pool.
     */
    public static synchronized CommsServerByteBufferPool getInstance() {
        if (instance == null) {
            instance = new CommsServerByteBufferPool();
        }
        return instance;
    }

    /**
     * Gets a CommsString from the pool. Any CommsString returned
     * will be initially null.
     * 
     * @return CommsString
     */
    @Override
    public synchronized CommsServerByteBuffer allocate() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "allocate");

        CommsServerByteBuffer buff = (CommsServerByteBuffer) super.allocate();

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "allocate", buff);
        return buff;
    }

    /**
     * Creates a new server buffer.
     * 
     * @return Returns the new buffer.
     */
    @Override
    protected CommsByteBuffer createNew() {
        return new CommsServerByteBuffer(this);
    }

    /**
     * @return Returns the name for the pool.
     */
    @Override
    protected String getPoolName() {
        return "CommsServerByteBufferPool";
    }
}
