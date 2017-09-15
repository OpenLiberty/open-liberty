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

import java.io.Serializable;

import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;

public class BusinessInterfaceDescriptorImpl<T> implements BusinessInterfaceDescriptor<T>, Serializable
{
    private static final long serialVersionUID = 8407700456763662820L;

    private final Class<T> interfaceClass;

    public BusinessInterfaceDescriptorImpl(Class<T> interfaceClass) throws ClassNotFoundException
    {
        this.interfaceClass = interfaceClass;
    }

    public static <K> BusinessInterfaceDescriptor<K> newInstance(Class<K> interfaceClass) throws ClassNotFoundException
    {
        return new BusinessInterfaceDescriptorImpl<K>(interfaceClass);
    }

    @Override
    public Class<T> getInterface()
    {
        return interfaceClass;
    }

    @Override
    public String toString() {
        return "BusinessInterfaceDescriptor: " + interfaceClass.getName();
    }
}
