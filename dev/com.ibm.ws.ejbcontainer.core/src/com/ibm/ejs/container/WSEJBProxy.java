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
package com.ibm.ejs.container;

/**
 * Provides a mechanism for the WebServices component to invoke a
 * method on an EJB instance for a WebService Endpoint request and
 * receive EJB interceptor support. <p>
 * 
 * An EJB Proxy differs from an EJB Wrapper, in that it does not perform
 * any EJB preInovke or postInvoke processing. It either invokes the
 * EJB method directly (when no interceptors) or invokes the EJB Container
 * interceptor support. <p>
 * 
 * A WSEJBProxy instance would actually be used in conjunction with an
 * EJB Wrapper, specifically the WSEJBWrapper. A WSEJBProxy would be returned
 * from WSEJBWrapper.ejbPreInvoke(...), instead of the actual EJB instance.
 * This allows the EJB Container to intercept the EJB method call, and
 * apply the appropriate EJB interceptor processing. <p>
 * 
 * A WSEJBProxy could always be returned from WSEJBWrapper.ejbPreInvoke,
 * however, for performance and footprint, it will only be used when
 * EJB interceptors are present. There is currently no benefit when
 * there are no EJB interceptors. <p>
 * 
 * Since a WSEJBProxy must implement all of the methods configured for
 * the WebService Endpoint interface, JITDeploy will generate a class
 * for every WebService Endpoint, which will be an implementation of
 * this abstract class. <p>
 * 
 * The EJB Container, specifically the WSEJBWrapper, will provide a new
 * instance of the WSEJBProxy implementation for every WebService Endpoint
 * request. <p>
 **/
public abstract class WSEJBProxy
{
    /**
     * A reference to the singleton EJSContainer instance for use
     * by Proxy implementations to invoke EJB Interceptors.
     **/
    protected EJSContainer ivContainer = null;

    /**
     * MethodContext, obtained in WSEJBWrapper.ejbPreInvoke(), and set here
     * for Proxy implementations to use to invoke EJB Interceptors.
     **/
    protected EJSDeployedSupport ivMethodContext = null;

    /**
     * Actual EJB Instance associated with the Proxy. All methods on the
     * proxy will eventually be invoked on this instance.
     **/
    protected Object ivEjbInstance = null;

}
