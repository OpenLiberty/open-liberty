/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.Context;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jndi.WSName;

public class JNDIServiceBinder implements AllServiceListener {
    private static final TraceComponent tc = Tr.register(JNDIServiceBinder.class);
    static final String OSGI_JNDI_SERVICE_NAME = "osgi.jndi.service.name";
    static final String OSGI_JNDI_SERVICE_ORIGIN = "osgi.jndi.service.origin";
    static final String OSGI_JNDI_SERVICE_ORIGIN_VALUE = "jndi";
    static final String OSGI_JNDI_SERVICE_CLASS = "osgi.jndi.service.class";
    static final String OSGI_JNDI_SERVICE_FILTER = "(&(osgi.jndi.service.name=*)(!(osgi.jndi.service.origin=jndi)))";

    static Hashtable<String, Object> createServiceProperties(WSName wsName, String className) {
        Hashtable<String, Object> localEnv = new Hashtable<String, Object>();
        localEnv.put(OSGI_JNDI_SERVICE_NAME, wsName.toString());
        localEnv.put(OSGI_JNDI_SERVICE_ORIGIN, OSGI_JNDI_SERVICE_ORIGIN_VALUE);
        if (className != null)
            localEnv.put(OSGI_JNDI_SERVICE_CLASS, OSGI_JNDI_SERVICE_CLASS);
        return localEnv;
    }

    /**
     * Contexts are not represented in the service registry.
     * In order to implement the {@link Context} behaviour, we
     * need to know which contexts have been created, so we
     * maintain a local tree structure. This is the root.
     */
    final ContextNode root = new ContextNode();
    /**
     * Store the last known JNDI name for a service so that if
     * it is updated we can remove it from the old location.
     */
    private final ConcurrentMap<ServiceReference<?>, WSName> serviceNames = new ConcurrentHashMap<ServiceReference<?>, WSName>();

    protected void activate(BundleContext bundleContext) {
        try {
            bundleContext.addServiceListener(this, OSGI_JNDI_SERVICE_FILTER);
            ServiceReference<?>[] refs = getAllServiceReferences(bundleContext);
            if (refs != null)
                for (ServiceReference<?> ref : refs)
                    recordEntry(ref);
        } catch (InvalidSyntaxException ise) {
            //auto FFDC
        }
    }

    protected void deactivate(BundleContext bundleContext) {
        // remove anything we inserted into the service registry
        WSContext.scrubContents(root);
        bundleContext.removeServiceListener(this);
    }

    /** {@inheritDoc} */
    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference<?> ref = event.getServiceReference();
        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                recordEntry(ref);
                break;
            case ServiceEvent.MODIFIED:
                moveEntry(ref);
                break;
            case ServiceEvent.MODIFIED_ENDMATCH:
            case ServiceEvent.UNREGISTERING:
                removeEntry(ref);
                break;
            default:
                break;
        }
    }

    @FFDCIgnore({ ClassCastException.class, NamingException.class })
    private void recordEntry(ServiceReference<?> ref) {
        Object val = ref.getProperty(OSGI_JNDI_SERVICE_NAME);
        try {
            String name = (String) val;
            WSName wsname = new WSName(name);
            root.autoBind(wsname, ref);
            WSName existingName = serviceNames.putIfAbsent(ref, wsname);
            if (existingName != null)
                // another thread added something
                // just let the other thread win
                throw new NameAlreadyBoundException(name);
        } catch (ClassCastException e) {
            if (tc.isWarningEnabled())
                Tr.warning(tc, "jndi.osgi.bind.failed", val, e);
        } catch (NamingException e) {
            if (tc.isWarningEnabled())
                Tr.warning(tc, "jndi.osgi.bind.failed", val, e);
        }
    }

    @FFDCIgnore({ ClassCastException.class, NamingException.class })
    private void moveEntry(ServiceReference<?> ref) {
        Object val = ref.getProperty(OSGI_JNDI_SERVICE_NAME);
        try {
            WSName name = new WSName((String) val);
            WSName oldName = serviceNames.put(ref, name);
            if (oldName != null)
                // clean up the old name
                root.ensureNotBound(oldName, ref);
            // First unbind and then bind to prevent collision/removal if 
            // name is same.
            root.autoBind(name, ref);
        } catch (ClassCastException e) {
            if (tc.isWarningEnabled())
                Tr.warning(tc, "jndi.osgi.bind.failed", val, e);
        } catch (NamingException e) {
            if (tc.isWarningEnabled())
                Tr.warning(tc, "jndi.osgi.bind.failed", val, e);
        }
    }

    private void removeEntry(ServiceReference<?> ref) {
        WSName name = serviceNames.remove(ref);
        if (name != null)
            root.ensureNotBound(name, ref);
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private ServiceReference<?>[] getAllServiceReferences(final BundleContext bundleContext) throws InvalidSyntaxException {
        if (System.getSecurityManager() == null)
            return bundleContext.getAllServiceReferences(null, OSGI_JNDI_SERVICE_FILTER);
        else
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<ServiceReference<?>[]>() {
                    @Override
                    public ServiceReference<?>[] run() throws InvalidSyntaxException {
                        return bundleContext.getAllServiceReferences(null, OSGI_JNDI_SERVICE_FILTER);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException)
                    throw (InvalidSyntaxException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
    }
}
