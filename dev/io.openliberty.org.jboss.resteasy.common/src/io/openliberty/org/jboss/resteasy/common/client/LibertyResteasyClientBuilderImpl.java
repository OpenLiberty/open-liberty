/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.client;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Optional;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import io.openliberty.restfulWS.client.ClientBuilderListener;

public class LibertyResteasyClientBuilderImpl extends ResteasyClientBuilderImpl {
    @SuppressWarnings("unchecked")
    private final static ServiceReference<ClientBuilderListener>[] EMPTY_ARRAY = new ServiceReference[] {};
    private final boolean isSecurityManagerPresent = null != System.getSecurityManager();

    @Override
    public ResteasyClient build() {
        BundleContext ctx = getBundleContext();
        ServiceReference<ClientBuilderListener>[] refs = getServiceRefs(ctx).orElse(EMPTY_ARRAY);
        for (ServiceReference<ClientBuilderListener> ref : refs) {
            ClientBuilderListener listener = getService(ctx, ref);
            listener.building(this);
        }

        ResteasyClient client = super.build();

        for (ServiceReference<ClientBuilderListener> ref : refs) {
            ClientBuilderListener listener = getService(ctx, ref);
            listener.built(client);
        }
        return client;
    }
    
    private BundleContext getBundleContext() {
        if (isSecurityManagerPresent) {
            return AccessController.doPrivileged((PrivilegedAction<BundleContext>) () -> 
                FrameworkUtil.getBundle(getClass()).getBundleContext());
        }
        return FrameworkUtil.getBundle(getClass()).getBundleContext();
    }

    @SuppressWarnings("unchecked")
    private Optional<ServiceReference<ClientBuilderListener>[]> getServiceRefs(BundleContext ctx) {
        try {
            if (isSecurityManagerPresent) {
                return Optional.ofNullable(AccessController.doPrivileged(
                    (PrivilegedExceptionAction<ServiceReference<ClientBuilderListener>[]>) () -> 
                        (ServiceReference<ClientBuilderListener>[]) 
                        ctx.getServiceReferences(ClientBuilderListener.class.getName(), null)));
            }
            return Optional.ofNullable((ServiceReference<ClientBuilderListener>[])
                ctx.getServiceReferences(ClientBuilderListener.class.getName(), null));
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException(pae.getCause());
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    private <T> T getService(BundleContext ctx, ServiceReference<T> ref) {
        if (isSecurityManagerPresent) {
            return AccessController.doPrivileged((PrivilegedAction<T>) () -> ctx.getService(ref));
        }
        return ctx.getService(ref);
    }
}
