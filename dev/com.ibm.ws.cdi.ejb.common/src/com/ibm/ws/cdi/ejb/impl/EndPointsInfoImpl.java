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
package com.ibm.ws.cdi.ejb.impl;

import java.util.HashSet;
import java.util.Set;

import org.jboss.weld.ejb.spi.EjbDescriptor;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.EndPointsInfo;
import com.ibm.ws.cdi.internal.interfaces.ManagedBeanDescriptor;

/**
 * The implementation class of the EndPointsInf, which holds the mangedbean descriptors, ejb descriptors, no-cdi interceptors and the module classloader
 */
public class EndPointsInfoImpl implements EndPointsInfo {

    private final Set<ManagedBeanDescriptor<?>> managedBeanDescs;
    private final Set<EjbDescriptor<?>> ejbDescs;
    private Set<Class<?>> nonCDIInterceptors;
    private final Set<String> nonCDIInterceptorClassNames;
    private final ClassLoader classloader;

    public EndPointsInfoImpl(Set<ManagedBeanDescriptor<?>> managedBeanDescs, Set<EjbDescriptor<?>> ejbDescs, Set<String> nonCDIinterceptors, ClassLoader classloader) {
        this.managedBeanDescs = managedBeanDescs;
        this.ejbDescs = ejbDescs;
        this.nonCDIInterceptorClassNames = nonCDIinterceptors;
        this.classloader = classloader;
    }

    /** {@inheritDoc} */
    @Override
    public Set<ManagedBeanDescriptor<?>> getManagedBeanDescriptors() {
        return this.managedBeanDescs;
    }

    /** {@inheritDoc} */
    @Override
    public Set<EjbDescriptor<?>> getEJBDescriptors() {
        return this.ejbDescs;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Class<?>> getNonCDIInterceptors() throws CDIException {
        if (nonCDIInterceptors == null) {
            nonCDIInterceptors = new HashSet<Class<?>>();
            try {
                for (String interceptor : nonCDIInterceptorClassNames) {
                    Class<?> interceptorClass = classloader.loadClass(interceptor);
                    nonCDIInterceptors.add(interceptorClass);
                }
            } catch (ClassNotFoundException e) {
                throw new CDIException(e);
            }
        }
        return this.nonCDIInterceptors;
    }

}