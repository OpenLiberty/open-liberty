/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.security.thread;

import javax.security.auth.Subject;

/**
 *
 */
public interface J2CIdentityService {

    /**
     * The set method is invoked just prior to obtaining a connection from a J2C resource adapter.
     * The reset method is called just after obtaining the connection.
     * 
     * Note: isJ2CThreadIdentityEnabled is called first and must return true for set() to be called.
     * This method shall return null IF AND ONLY IF the thread identity support is disabled.
     */
    public Object set(Subject subject);

    /**
     * The reset method is called just after obtaining a connection from a J2C resource adapter.
     */
    public void reset(Object tokenReturnedFromSet);

    /**
     * The isJ2CThreadIdentityEnabled method is called before the set method and it must return true for set to be called.
     */
    public boolean isJ2CThreadIdentityEnabled();

    /**
     * Create and return a J2C subject based on the invocation subject.
     * 
     * If the invocation subject is null, the caller subject is used.
     * If the caller subject is null, the server subject is used.
     * 
     * @return Subject the J2C subject
     */
    public Subject getJ2CInvocationSubject();

}
