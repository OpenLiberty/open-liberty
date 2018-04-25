/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.collector.manager;

public interface SynchronousHandler extends Handler {
    /**
     * Synchronous handlers will rely on this method to synchronously write message
     * events/objects that are passed through from the BufferManager/Conduit.
     * 
     * @param event Object that holds the message event
     */
    void synchronousWrite(Object event);
    
    
    /**
     * Synchronous handlers do not need to implement this or use this
     * 
     * @param sourceId Source identifier.
     * @param bufferMgr Buffer manager instance for the source.
     */
    void setBufferManager(String sourceId, BufferManager bufferMgr);
}
