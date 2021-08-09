/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.security.thread;

import javax.security.auth.Subject;

/**
 * Manages the security environment associated with the thread.
 */
public interface ThreadIdentityService {

    /**
     * The set method is invoked at the following times:
     * 
     * 1. In the context of a web container request, it is called AFTER authentication and BEFORE dispatching the request.
     * The reset method is called after the dispatch completes.
     * 2. From WSSubject.doAs/doAsPrivileged, the reset method is called after the doAs code block completes.
     * 3. In a WorkManager/AsyncBeans context, it is called if <syncToOSThreadContext> is enabled. The reset method is called after the async work completes.
     * 
     * Note: In all cases, isAppThreadIdentityEnabled is called first and must return true for set() to be called.
     * This method shall return null IF AND ONLY IF the thread identity support is disabled.
     * 
     * @param subject The subject to set as the thread identity.
     * @return A token representing the identity previously on the thread.
     */
    public Object set(Subject subject);

    /**
     * Reset the native thread identity with the SAF identity represented by the token
     * parameter. The token was returned from a previous call to set.
     * It represents the identity that was on the thread prior to being swapped off by
     * the call to setThreadIdentity.
     * 
     * @param tokenReturnedFromSet The token returned by a previous call to set.
     */
    public void reset(Object tokenReturnedFromSet);

    /**
     * Called whenever the server must perform an internal operation as the server ID.
     * The reset method is called after the internal operation is complete.
     * 
     * @return A token representing the identity previously on the thread.
     */
    public Object runAsServer();

    /**
     * Determines if application thread identity is enabled.
     * 
     * @return true if application thread identity is enabled; false otherwise.
     */
    public boolean isAppThreadIdentityEnabled();

}
