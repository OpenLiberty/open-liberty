/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer;

import com.ibm.websphere.csi.J2EEName;

/**
 * JCDIHelper is an abstraction layer to avoid dependencies on the real JCDI
 * service from the core EJB container runtime and the embeddable container. <p>
 *
 * The WAS specific runtime will provide an implementation that access the
 * JCDI Service. <p>
 */
public interface JCDIHelper
{
    /**
     * Returns the EJB interceptor class which is to be invoked as the first
     * interceptor in the chain of EJB interceptors. <p>
     *
     * A null value will be returned if the EJB does not require a JCDI
     * interceptor.
     *
     * @param j2eeName
     *            the unique JavaEE name containing the application, module, and
     *            EJB name
     * @param ejbImpl
     *            the EJB implementation class
     **/
    // F743-29169
    public Class<?> getFirstEJBInterceptor(J2EEName j2eeName, Class<?> ejbImpl);

    /**
     * Returns the EJB interceptor class which is to be invoked as the last
     * interceptor in the chain of EJB interceptors. <p>
     *
     * A null value will be returned if the EJB does not require a JCDI
     * interceptor.
     *
     * @param j2eeName
     *            the unique JavaEE name containing the application, module, and
     *            EJB name
     * @param ejbImpl
     *            the EJB implementation class
     **/
    public Class<?> getEJBInterceptor(J2EEName j2eeName, Class<?> ejbImpl);
}
