/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.ejb;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.OperationNotSupportedException;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.naming.LocalColonEJBNamingHelper;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * LocalColonContextFactory is an implementation of {@link ObjectFactory} that is
 * intended to be registered in the SR with the osgi.jndi.url.scheme=local
 * property. As such it is called for handling JNDI names starting with "local:".
 *
 */
@Component(configurationPolicy = IGNORE, property = { "service.vendor=ibm", "osgi.jndi.url.scheme=local" })
public class LocalColonURLContextFactory implements ObjectFactory {

    static final TraceComponent tc = Tr.register(LocalColonURLContextFactory.class);

    private final ConcurrentServiceReferenceSet<LocalColonEJBNamingHelper> helperServices = new ConcurrentServiceReferenceSet<LocalColonEJBNamingHelper>("helpers");

    @Reference(name = "helpers", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "addHelper", unbind = "removeHelper")
    public void addHelper(ServiceReference<LocalColonEJBNamingHelper> reference) {
        helperServices.addReference(reference);
    }

    public void removeHelper(ServiceReference<LocalColonEJBNamingHelper> reference) {
        helperServices.removeReference(reference);
    }

    public void activate(ComponentContext cc) {
        helperServices.activate(cc);
    }

    public void deactivate(ComponentContext cc) {
        helperServices.deactivate(cc);
    }

    /**
     * Creates a LocalColonURLContext for resolving local: urls. It should only be
     * called by an OSGi JNDI spec implementation. There is no support for
     * non-null Name and Context parameters for this method in accordance with
     * the OSGi specification for JNDI.
     *
     * <UL>
     * <LI>If the parameter o is null a new {@link LocalColonURLContext} is returned.
     * <LI>If o is a URL String then the Object returned is the result of
     * looking up the String on a {@link LocalColonURLContext}.
     * <LI>If o is an array of URL Strings then an {@link OperationNotSupportedException} is thrown as there is no
     * sub-context support in this implementation from which to lookup multiple
     * names.
     * <LI>If o is any other Object an {@link OperationNotSupportedException} is
     * thrown.
     * </UL>
     *
     * @param o
     *            {@inheritDoc}
     * @param n
     *            must be null (OSGi JNDI spec)
     * @param c
     *            must be null (OSGi JNDI spec)
     * @param envmt
     *            {@inheritDoc}
     * @return
     * @throws OperationNotSupportedException
     *             if the Object passed in is not null or a String.
     */
    @Override
    public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> env) throws Exception {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "getObjectInstance:");
        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "Object: " + o);
            Tr.debug(tc, "Name: " + n);
            Tr.debug(tc, "Context: " + c);
            Tr.debug(tc, "Env: " + env);
        }

        // by OSGi JNDI spec Name and Context should be null
        // if they are not then this code is being called in
        // the wrong way
        if (n != null || c != null) {
            if (isTraceOn && tc.isEntryEnabled()) {
                Tr.exit(tc, "getObjectInstance: null");
            }
            return null;
        }
        // Object is String, String[] or null
        // Hashtable contains any environment properties
        Object context;
        if (o == null) {
            context = new LocalColonURLContext(env, helperServices);
        } else if (o instanceof String) {
            context = new LocalColonURLContext(env, helperServices).lookup((String) o);
        } else {
            throw new OperationNotSupportedException();
        }
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "getObjectInstance: " + context);
        }
        return context;
    }

}
