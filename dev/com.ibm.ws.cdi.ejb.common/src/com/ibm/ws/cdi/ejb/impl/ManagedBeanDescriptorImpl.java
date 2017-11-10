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

import com.ibm.ws.cdi.internal.interfaces.ManagedBeanDescriptor;
import com.ibm.ws.ejbcontainer.ManagedBeanEndpoint;

public class ManagedBeanDescriptorImpl<T> implements ManagedBeanDescriptor<T> {
    private final Class<T> beanClass;
    private final String mbJ2EENameString;

    private ManagedBeanDescriptorImpl(ManagedBeanEndpoint mb, Class<T> beanClass) {
        this.beanClass = beanClass;
        this.mbJ2EENameString = mb.getJ2EEName().toString();
    }

    public static ManagedBeanDescriptor<?> newInstance(ManagedBeanEndpoint mb, ClassLoader classLoader) throws ClassNotFoundException {
        String beanClassName = mb.getClassName();
        Class<?> beanClass = classLoader.loadClass(beanClassName);

        return newInstance(mb, beanClass);
    }

    private static <K> ManagedBeanDescriptor<K> newInstance(ManagedBeanEndpoint mb, Class<K> beanClass) {
        return new ManagedBeanDescriptorImpl<K>(mb, beanClass);
    }

    @Override
    public Class<T> getBeanClass() {
        return beanClass;
    }

    @Override
    public String toString() {
        return "ManagedBeanDescriptor: " + this.mbJ2EENameString;
    }
}
