/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.faces40.internal.spi.impl;

import org.apache.myfaces.util.lang.ClassUtils;
import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderException;

import jakarta.faces.context.ExternalContext;

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
                            "io.openliberty.faces40.internal.cdi.WASCDIAnnotationInjectionProvider");
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
