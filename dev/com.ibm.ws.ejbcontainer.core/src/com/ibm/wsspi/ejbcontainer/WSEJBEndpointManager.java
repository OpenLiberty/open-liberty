/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.ejbcontainer;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Provides a mechanism for the WebServices component to request the
 * EJB Container to setup and tear down the EJB contexts and environments
 * associated with a WebService Endpoint request on an EJB. <p>
 * 
 * The EJB Container will provide a new instance of this interface for
 * every WebService Endpoint request. <p>
 **/
public interface WSEJBEndpointManager
{
    /**
     * Performs all EJB Container processing required prior to invoking
     * the specified EJB method. <p>
     * 
     * This method will establish the proper EJB contexts for the method
     * call; including the Security context, Transaction context,
     * and Naming context (java:comp/env), and the thread context
     * classloader. <p>
     * 
     * If this method fails (exception thrown) then the EJB contexts
     * and environment has not been properly setup, and no attempt
     * should be made to invoke the ejb, interceptors, or handlers. <p>
     * 
     * The method ejbPostInvoke MUST be called after calling this method;
     * to remove the EJB contexts from the current thread. This is true
     * even if this method fails with an exception. <p>
     * 
     * Note that the method arguments would normally be required for
     * EJB Container preInvoke processing, to properly determine JACC
     * security authorization, except the JACC specification, in section
     * 4.6.1.5 states the following: <p>
     * 
     * All EJB containers must register a PolicyContextHandler whose
     * getContext method returns an array of objects (Object[]) containing
     * the arguments of the EJB method invocation (in the same order as
     * they appear in the method signature) when invoked with the key
     * "javax.ejb.arguments". The context handler must return the value
     * null when called in the context of a SOAP request that arrived at
     * the ServiceEndpoint method interface. <p>
     * 
     * @param method A reflection Method object which provides the method
     *            name and signature of the EJB method that will be
     *            invoked. This may be a Method of either an EJB
     *            interface or the EJB implementation class.
     * @param context The MessageContext which should be returned when
     *            InvocationContext.getContextData() is called.
     * 
     * @return an object which may be invoked as though it were an
     *         instance of the EJB. It will be a wrapper/proxy object
     *         when there are EJB interceptors.
     * 
     * @throws RemoteException when a system level error occurs.
     **/
    public Object ejbPreInvoke(Method method, Map<String, Object> context)
                    throws RemoteException;

    /**
     * Performs all EJB Container processing required to remove the EJB contexts
     * established by ejbPreInvoke. <p>
     * 
     * This method is intended to be called after calling ejbPreInvoke and
     * after the EJB method has completed. This method MUST be called anytime
     * ejbPreInvoke has been called, even if ejbPreInvoke fails with an
     * exception. <p>
     * 
     * If an exception occurs during ejbPostInvoke, or setException was called
     * with a non-application exception, then the EJB Container will map the
     * exception to an appropriate RemoteException as required by the EJB
     * Specification, and throw the mapped exception. <p>
     * 
     * @throws RemoteException when a system level error has occurred.
     **/
    public void ejbPostInvoke()
                    throws RemoteException;

    /**
     * Notifies the EJB Container that an exception occurred during
     * processing of the endpoint method, and allows the EJB Container
     * to map that exception to the appropriate exception required
     * by the EJB Specification. <p>
     * 
     * This method should be called for any exception that occurs
     * when invoking the WebService Handlers, EJB Interceptors, or
     * the EJB method itself. <p>
     * 
     * Generally, if the exception provided is an application exception,
     * then it will be returned, unmapped. If it is a system exception,
     * then it will be mapped per the Exception Handling chapter of the
     * EJB Specification. Note that RuntimeExceptions may be considered
     * application exception beginning in EJB 3.0. <p>
     * 
     * Calling setException may or may not result in the transaction
     * being marked for rollback or rolled back. System exceptions
     * will always result in rollback. Application exceptions may
     * or may not, depending on customer configuration. <p>
     * 
     * @param ex throwable being reported to the EJB Container.
     * 
     * @return either the exception passed in, if it is an application
     *         exception, or the appropriate exception as required
     *         by the EJB Specification.
     **/
    // d503197
    public Throwable setException(Throwable ex);

}
