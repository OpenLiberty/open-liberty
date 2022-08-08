/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.spi.impl;

import javax.faces.context.ExternalContext;

import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderException;

/**
 * Delegation pattern to avoid direct instantiation
 */
public class WASCDIAnnotationDelegateInjectionProvider extends InjectionProvider
{
    private InjectionProvider delegate;
    private final boolean available;

    public WASCDIAnnotationDelegateInjectionProvider(ExternalContext externalContext)
    {
        try
        {
            Class clazz = ClassUtils.simpleClassForName(
                            "com.ibm.ws.jsf.cdi.WASCDIAnnotationInjectionProvider");
            delegate = (InjectionProvider) clazz.getConstructor(ExternalContext.class).newInstance(externalContext);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception exc) {
            //Ignore
        }
        available = ((delegate != null) && delegate.isAvailable());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object inject(Object instance) throws InjectionProviderException
    {
        if (available)
        {
            return delegate.inject(instance);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postConstruct(Object instance, Object creationMetaData) throws InjectionProviderException
    {
        if (available)
        {
            delegate.postConstruct(instance, creationMetaData);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preDestroy(Object instance, Object creationMetaData) throws InjectionProviderException
    {
        if (available)
        {
            delegate.preDestroy(instance, creationMetaData);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailable()
    {
        return available;
    }
}
