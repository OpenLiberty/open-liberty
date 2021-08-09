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
package com.ibm.ws.jsf.cdi;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.faces.context.ExternalContext;
import javax.servlet.ServletContext;

import org.apache.myfaces.cdi.dependent.BeanEntry;
import org.apache.myfaces.config.FacesConfigurator;
import org.apache.myfaces.spi.InjectionProviderException;

import com.ibm.ws.jsf.spi.WASInjectionProvider;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelper;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager;

/**
 * Used by MyFaces as an injection provider for CDI 1.2 support.
 * 
 */
public class WASCDIAnnotationInjectionProvider extends WASInjectionProvider
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
    public Object inject(Class Klass) throws InjectionProviderException {

        return this.inject(Klass, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object inject(Class Klass, boolean doPostConstruct) throws InjectionProviderException {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,
                        "inject(Class<?> Klass,boolean)", "Klass =" + Klass.getName(), ", doPostConstruct = " + doPostConstruct);
        }

        return this.inject(Klass, doPostConstruct, null);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object inject(Class Klass, boolean doPostConstruct, ExternalContext eContext) throws InjectionProviderException {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,
                        "inject(Class<?> Klass,boolean,econtext)", "Klass =" + Klass.getName(), ", doPostConstruct = " + doPostConstruct + ", eContext = " + eContext);
        }

        ManagedObject mo = null;
        if (isAvailable) {
            try {
                // boolean passed to runtimeAnnotationHelper indicates whether or not to delay postConstruct
                // so need the opposite value to doPostConstruct
                mo = runtimeAnnotationHelper.inject(Klass, !doPostConstruct);

                if (eContext != null) {

                    List<BeanEntry> injectedBeanStorage =
                                    (List<BeanEntry>) eContext.getApplicationMap().get(FacesConfigurator.INJECTED_BEAN_STORAGE_KEY);

                    injectedBeanStorage.add(new BeanEntry(mo.getObject(), mo.getContextData(CreationalContext.class)));

                }
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
    public Object inject(Object instance) throws InjectionProviderException
    {
        return this.inject(instance, false);
    }

    /**
     * {@inheritDoc}
     */             
    @Override
    public Object inject(Object instance, boolean doPostConstruct) throws InjectionProviderException
    {
        return inject(instance, doPostConstruct, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object inject(Object instance, boolean doPostConstruct, ExternalContext eContext) throws InjectionProviderException {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,
                        "inject(Object instance,boolean,eContxet)", "instance =" + instance.getClass().getName(), ", doPostConstruct = " + doPostConstruct + ", eContext = " + eContext);
        }

        ManagedObject mo = null;
        if (isAvailable) {
            try {
                // boolean passed to runtimeAnnotationHelper indicates whether or not to delay postConstruct
                // so need the opposite value to doPostConstruct
                mo = runtimeAnnotationHelper.inject(instance, !doPostConstruct);

                if (eContext != null) {

                    List<BeanEntry> injectedBeanStorage =
                                    (List<BeanEntry>) eContext.getApplicationMap().get(FacesConfigurator.INJECTED_BEAN_STORAGE_KEY);

                    injectedBeanStorage.add(new BeanEntry(mo.getObject(), mo.getContextData(CreationalContext.class)));

                }
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
