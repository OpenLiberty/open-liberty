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
package com.ibm.ws.jsf.cdi;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.ExternalContext;
import javax.servlet.ServletContext;

import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderException;

import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelper;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager;

/**
 * Used by MyFaces as an injection provider for CDI 1.2 support.
 *
 */
public class WASCDIAnnotationInjectionProvider extends InjectionProvider
{
    private static final String CLASS_NAME = WASCDIAnnotationInjectionProvider.class.getName();
    private static final Logger logger = Logger.getLogger(WASCDIAnnotationInjectionProvider.class.getName());

    private AnnotationHelper runtimeAnnotationHelper;
    private AnnotationHelperManager annotationHelperManager;

    private boolean isAvailable = true;

    public WASCDIAnnotationInjectionProvider(ExternalContext externalContext)
    {
        Object context = externalContext.getContext();
        if (context instanceof ServletContext) {
            annotationHelperManager = AnnotationHelperManager.getInstance((ServletContext) context);
            runtimeAnnotationHelper = annotationHelperManager.getAnnotationHelper();
        } else {
            isAvailable = false;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,
                        "constructor", "isAvailable", isAvailable);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object inject(Object instance) throws InjectionProviderException
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,
                        "inject(Object instance)", "instance =" + instance.getClass().getName());
        }

        ManagedObject mo = null;
        if (isAvailable) {
            try {
                mo = runtimeAnnotationHelper.inject(instance);

            } catch (RuntimeException exc) {
                throw new InjectionProviderException(exc);
            }
        }
        return mo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postConstruct(Object instance, Object creationMetaData) throws InjectionProviderException
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,
                        "postConstruct(instance)", "Instance of = " + instance.getClass().getName());
        }
        if (isAvailable) {
            try {
                runtimeAnnotationHelper.doDelayedPostConstruct(instance);
            } catch (RuntimeException exc) {
                throw new InjectionProviderException(exc);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preDestroy(Object instance, Object creationMetaData) throws InjectionProviderException
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,
                        "preDestroy(instance)", "Instance of = " + instance.getClass().getName());
        }
        if (isAvailable) {
            try {
                runtimeAnnotationHelper.doPreDestroy(instance);
            } catch (RuntimeException exc) {
                throw new InjectionProviderException(exc);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailable() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "isAvailable()", "isAvailable = " + isAvailable);
        }
        return isAvailable;
    }
}
