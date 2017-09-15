/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

/**
 * InvocationCallback is an interface that allows non-container components
 * to dynamically enlist for a callback during the execution of a J2EE component
 * and to be called back during various points after the enlistment of the callback.
 * The enlistment is done by a websphere cmvc component looking up one of the container
 * services and using the enlistInvocationCallback method on the container interface
 */
public interface InvocationCallback {
    /**
     * Called by the container after executing a component or method on a
     * component. In the EJB container, the callback occurs after the
     * transaction has completed. Note, the implementation of this callback
     * must should not throw any Throwable objects. If it does, FFDC is logged
     * and the Throwable is thrown away (e.g nothing is thrown to client).
     * 
     * @param cookie is the same object reference that was passed to the
     *            Container.enlistInvocationCallback method.
     */
    public void postInvoke(Object callbackCookie);

}
