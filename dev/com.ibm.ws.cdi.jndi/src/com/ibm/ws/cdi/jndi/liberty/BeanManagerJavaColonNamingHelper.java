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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InvalidNameException;
import javax.naming.NameClassPair;
import javax.naming.NamingException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This {@link JavaColonNamingHelper} implementation provides support for
 * BeanManager lookup in the standard Java EE component naming context
 * (java:comp). <p>
 *
 * It is registered on the JNDI NamingHelper whiteboard and will be
 * consulted during object lookup in the appropriate namespace. <p>
 */
@Trivial
@Component(
                property = { "service.vendor=IBM" })
public class BeanManagerJavaColonNamingHelper implements JavaColonNamingHelper
{
    private static final TraceComponent tc = Tr.register(BeanManagerJavaColonNamingHelper.class);
    private final AtomicServiceReference<CDIService> cdiServiceRef = new AtomicServiceReference<CDIService>("cdiService");
    private static final String BEAN_MANAGER = "BeanManager";

    @Override
    public Object getObjectInstance(JavaColonNamespace namespace, String name) throws NamingException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // This helper only provides support for java:comp
        if (namespace != JavaColonNamespace.COMP) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "getObjectInstance : null (not COMP)");
            }
            return null;
        }

        if (!BEAN_MANAGER.equals(name)) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "getObjectInstance : null (not BeanManager )");
            }
            return null;
        }

        BeanManager bm = null;
        CDIService cdiService = getCdiService();
        if (cdiService == null) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "getObjectInstance : The CDIService is not available. The server cannot locate the CDI BeanManager until the CDIService has started.");
            }
        } else {
            bm = cdiService.getCurrentModuleBeanManager();
        }

        return bm;
    }

    @Override
    public boolean hasObjectWithPrefix(JavaColonNamespace namespace, String name) throws NamingException
    {
        if (name == null) {
            throw new InvalidNameException();
        }

        boolean result = false;
        // This helper only provides support for java:comp
        if (namespace == JavaColonNamespace.COMP && name.isEmpty()) {
            result = true;
        }

        return result;
    }

    @Override
    public Collection<? extends NameClassPair> listInstances(JavaColonNamespace namespace, String nameInContext) throws NamingException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        Collection<NameClassPair> retVal = null;

        // This helper only provides support for java:comp
        if ((namespace == JavaColonNamespace.COMP) && ("".equals(nameInContext))) {
            retVal = new ArrayList<NameClassPair>();
            retVal.add(new NameClassPair(BEAN_MANAGER, BeanManager.class.getName()));
            return retVal;
        } else {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "listInstances", "empty (not COMP)");
            }
            retVal = Collections.emptyList();
        }

        return retVal;
    }

    public void activate(ComponentContext context)
    {
        cdiServiceRef.activate(context);
    }

    public void deactivate(ComponentContext context)
    {
        cdiServiceRef.deactivate(context);
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

    private CDIService getCdiService()
    {
        return cdiServiceRef.getService();
    }
}
