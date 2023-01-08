/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.session;

/**
 * Provides extensions used by instances of <code>MemoryStore</code>.
 * 
 * @see com.ibm.ws.session.store.memory.MemoryStore
 */
public interface MemoryStoreHelper {

    /**
     * Method used to prepare the Thread to handle requests.
     * This should set an appropriate classpath on the thread.
     * 
     * @see com.ibm.ws.session.store.memory.MemoryStore#setThreadContext()
     */
    public void setThreadContext();
    
    /**
     * Method used to prepare the Thread to handle requests.
     * This should be called by the invalidation thread only
     * 
     * @see com.ibm.ws.session.store.memory.MemoryStore#setThreadContext()
     */
    public void setThreadContextDuringRunInvalidation();

    /**
     * Method used to change the Thread's properties back to what they were before
     * a setThreadContext was called.
     * 
     * @see com.ibm.ws.session.store.memory.MemoryStore#unsetThreadContext()
     */
    public void unsetThreadContext();
   

}
