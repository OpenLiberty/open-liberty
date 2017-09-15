/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.jndi.liberty;

import java.lang.annotation.Annotation;
import java.util.Hashtable;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Provides the data to register an ObjectFactory override for
 * the BeanManager data type. <p>
 *
 * This supports injection and lookup of an instance of BeanManager
 * using the Resource annotation or resource-env xml stanza. <p>
 */
@Component(
                name = "com.ibm.ws.cdi.jndi.liberty.BeanManagerObjectFactoryInfo",
                service = { ObjectFactoryInfo.class, ObjectFactory.class, BeanManagerObjectFactoryInfo.class },
                property = { "service.vendor=IBM" })
public class BeanManagerObjectFactoryInfo extends ObjectFactoryInfo implements ObjectFactory
{

    private static final TraceComponent tc = Tr.register(BeanManagerObjectFactoryInfo.class);
    private final AtomicServiceReference<CDIService> cdiServiceRef = new AtomicServiceReference<CDIService>("cdiService");

    @Override
    public Class<? extends Annotation> getAnnotationClass()
    {
        return Resource.class;
    }

    @Override
    public Class<?> getType()
    {
        return BeanManager.class;
    }

    @Override
    public boolean isOverrideAllowed()
    {
        return false;
    }

    @Override
    public Class<? extends ObjectFactory> getObjectFactoryClass()
    {
        return BeanManagerObjectFactoryInfo.class;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
     */
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception
    {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "getObjectInstance" + Util.identity(obj));
        }

        BeanManager bm = null;

        if (obj instanceof javax.naming.Reference) {
            javax.naming.Reference objRef = (javax.naming.Reference) obj;
            String factoryClassName = objRef.getFactoryClassName();
            if (this.getClass().getName().equals(factoryClassName)) {
                CDIService cdiService = getCDIService();
                bm = cdiService.getCurrentBeanManager();
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "getObjectInstance", bm);
        }

        return bm;
    }

    public void activate(ComponentContext context)
    {
        cdiServiceRef.activate(context);
    }

    public void deactivate(ComponentContext context)
    {
        cdiServiceRef.deactivate(context);
    }

    CDIService getCDIService() {
        return cdiServiceRef.getServiceWithException();
    }

    @Reference(name = "cdiService", service = CDIService.class)
    protected void setCdiService(ServiceReference<CDIService> ref)
    {
        cdiServiceRef.setReference(ref);
    }

    protected void unsetCdiService(ServiceReference<CDIService> ref)
    {
        cdiServiceRef.unsetReference(ref);
    }
}
