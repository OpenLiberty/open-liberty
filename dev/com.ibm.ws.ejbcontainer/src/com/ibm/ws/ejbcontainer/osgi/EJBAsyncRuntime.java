/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi;

import java.rmi.RemoteException;
import java.util.concurrent.Future;

import javax.ejb.EJBException;

import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.EJSWrapperBase;

/**
 * The interface between the core container and the services provided by
 * the EJB Asynchronous Method runtime environment.
 */
public interface EJBAsyncRuntime {

    /**
     * Schedules an asynchronous method to be called.
     *
     * @param wrapper the wrapper that originated the asynchronous method call
     * @param methodInfo the method info
     * @param methodId the method info id
     * @param args the arguments to the wrapper
     *
     * @return the future to return to the client, or null if the method had a void return type
     *
     * @throws EJBException if asynchronous EJBs are not allowed by this
     *             runtime environment or if any exception occurs while trying to schedule
     *             the asynchronous method
     * @throws RemoteException if the bean implements RMI remote business interface, wrap
     *             the exception in a RemoteException instead of EJBException
     */
    Future<?> scheduleAsync(EJSWrapperBase wrapper, EJBMethodInfoImpl methodInfo, int methodId, Object[] args)
                    throws RemoteException;

}
