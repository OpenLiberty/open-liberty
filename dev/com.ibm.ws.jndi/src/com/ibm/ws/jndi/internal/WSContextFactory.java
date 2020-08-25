/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import static com.ibm.ws.jndi.WSNameUtil.normalize;
import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jndi.internal.JNDIServiceBinderManager.JNDIServiceBinderHolder;

/**
 * This provider should be instantiated (at least) once for each requesting bundle,
 * and should be considered to be for the sole use of that bundle. We keep track
 * of the requesting bundle and pass this to any created context objects.
 */
@Component(service = { InitialContextFactory.class, ObjectFactory.class, WSContextFactory.class },
           servicefactory = true,
           configurationPolicy = IGNORE,
           property = "service.vendor=IBM")
public class WSContextFactory implements InitialContextFactory, ObjectFactory, AllServiceListener {
    private final static TraceComponent tc = Tr.register(WSContextFactory.class);
    /** The bundle that is using this {@link InitialContextFactory}. */
    private BundleContext userContext;

    private final ConcurrentHashMap<ServiceReference<?>, Object> cache = new ConcurrentHashMap<>();

    /** called by DS to activate this component */
    protected void activate(ComponentContext cc) {
        Bundle usingBundle = cc.getUsingBundle();
        userContext = usingBundle.getBundleContext();
        try {
            /*
             * set up listener with the system bundle BC. The client BC passed in
             * is typically the gateway bundle's which may not have visibility to the events we care about.
             */
            userContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext().addServiceListener(this, JNDIServiceBinder.OSGI_JNDI_SERVICE_FILTER);
        } catch (InvalidSyntaxException e) {
        }
    }

    protected void deactivate(ComponentContext cc) {
        userContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext().removeServiceListener(this);
    }

    Object getService(final ServiceReference<?> ref) {
        return cache.computeIfAbsent(ref, (r) -> AccessController.doPrivileged((PrivilegedAction<Object>) () -> userContext.getService(r)));
    }

    ///////////////////////////////////////////
    // InitialContextFactory implementation //
    /////////////////////////////////////////

    /** {@inheritDoc} */
    // Sanction the implicit cast from Hashtable to Hashtable<String, Object>
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public WSContext getInitialContext(Hashtable env) throws NamingException {
        return new WSContext(userContext, JNDIServiceBinderHolder.HELPER.root, env, this);
    }

    ///////////////////////////////////
    // ObjectFactory implementation //
    /////////////////////////////////

    @Override
    public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt) throws Exception {
        @SuppressWarnings("unchecked")
        Hashtable<String, Object> env = (Hashtable<String, Object>) envmt;
        return (o instanceof Reference) ? resolve((Reference) o, env) : null;
    }

    private WSContext resolve(Reference ref, Hashtable<String, Object> env) throws NamingException {
        if (ref.getClassName().equals(WSContext.class.getName())) {
            RefAddr addr = ref.get("jndi");
            Object content = addr.getContent();
            if (content instanceof String) {
                String name = (String) content;
                Object o = JNDIServiceBinderHolder.HELPER.root.lookup(normalize(name));
                try {
                    ContextNode node = (ContextNode) o;
                    return new WSContext(userContext, node, env, this);
                } catch (ClassCastException e) {
                    // Auto-FFDC happens here!
                    throw new NotContextException(name);
                }
            }
        }
        return null;
    }

    static Reference makeReference(WSContext ctx) throws NamingException {
        RefAddr addr = new StringRefAddr("jndi", "" + ctx.myNode.fullName);
        return new Reference(WSContext.class.getName(), addr, WSContextFactory.class.getName(), null);
    }

    /**
     * Cleanup services in the cache that will have a stale ref after a JNDI unbind or rename.
     * Renamed services will be re-cached on next lookup under under new name.
     */
    @Override
    @FFDCIgnore(IllegalStateException.class)
    public void serviceChanged(ServiceEvent event) {
        ServiceReference<?> ref = event.getServiceReference();
        switch (event.getType()) {
            case ServiceEvent.MODIFIED:
            case ServiceEvent.UNREGISTERING:
            case ServiceEvent.MODIFIED_ENDMATCH:
                try {
                    userContext.ungetService(ref);
                } catch (IllegalStateException e) {
                    // The event listener was added using the SYSTEM_BUNDLE context, so we
                    // may get service changed events after the userContext is no longer valid.
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "UngetService failed: " + e);
                    }
                }
                cache.remove(ref);
                break;
            default:
                break;
        }
    }

}
