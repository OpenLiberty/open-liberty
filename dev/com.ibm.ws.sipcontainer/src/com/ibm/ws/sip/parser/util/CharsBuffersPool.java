/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
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
package com.ibm.ws.sip.parser.util;

/**
 * @author Amir Perlman, Dec 2004
 *
 *  A pool of Char Buffers shared among different classes.  
 */
public class CharsBuffersPool extends ObjectPool implements ObjectPoolListener {

    /**
     * Singleton instance. 
     */
    private static final CharsBuffersPool c_pool = new CharsBuffersPool();
    
    /**
     * Construct a new Char buffer pool
     *
     */
    private CharsBuffersPool() {
        super(CharsBuffer.class);
        setObjectPoolListener(this);
    }

    /**
     * @see com.ibm.ws.sip.parser.util.ObjectPoolListener#objectReturned(java.lang.Object)
     */
    public void objectReturned(Object obj) {
        ((CharsBuffer)obj).reset();
    }
    
    /**
     * Get a Char Buffer from pool
     * @return
     */
    public static CharsBuffer getBuffer()
    {
        return (CharsBuffer) c_pool.get();
    }
    
    /**
     * Return the buffer to pool
     * @param buffer
     */
    public static void putBufferBack(CharsBuffer writer)
    {
        c_pool.putBack(writer);
    }
}
