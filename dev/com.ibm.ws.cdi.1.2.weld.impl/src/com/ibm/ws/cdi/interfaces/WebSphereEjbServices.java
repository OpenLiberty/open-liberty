/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.interfaces;

import java.lang.reflect.Method;
import java.util.List;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;

import org.jboss.weld.ejb.spi.EjbServices;

import com.ibm.websphere.csi.J2EEName;

public interface WebSphereEjbServices extends EjbServices {

    /**
     * Find all the interceptors of a given type for a given method on an ejb
     * 
     * @param ejbName the J2EEName of the ejb
     * @param method the method to be intercepted
     * @param interceptionType the type of interception
     */
    public List<Interceptor<?>> getInterceptors(J2EEName ejbJ2EEName, Method method, InterceptionType interceptionType);
}
