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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.i18n.LogMessages;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.osgi.framework.Bundle;
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

        //ResteasyClient client = super.build();
        // The following lines basically do the same thing as super.build(), but avoids creating
        // the ClientHttpEngine.  RESTEasy creates it eagerly which is less efficient, but also 
        // breaks our config model where things like connection and read timeouts can be set
        // _after_ creating the Client instance.
        ClientConfiguration config = new ClientConfiguration(getProviderFactory());
        for (Map.Entry<String, Object> entry : properties.entrySet())
        {
           config.property(entry.getKey(), entry.getValue());
        }

        ExecutorService executor = asyncExecutor;

        if (executor == null)
        {
           cleanupExecutor = true;
           executor = Executors.newCachedThreadPool();
        }

        boolean resetProxy = false;
        if (this.defaultProxy == null) {
           resetProxy = true;
           // check for proxy config parameters
           setProxyIfNeeded(config);
        }

        if (resetProxy) {
           this.defaultProxy = null;
        }
        ResteasyClient client = createResteasyClient(null, executor, cleanupExecutor, scheduledExecutorService, config);

        for (ServiceReference<ClientBuilderListener> ref : refs) {
            ClientBuilderListener listener = getService(ctx, ref);
            listener.built(client);
        }
        return client;
    }

    @Override
    protected ResteasyClient createResteasyClient(ClientHttpEngine engine,ExecutorService executor, boolean cleanupExecutor, ScheduledExecutorService scheduledExecutorService, ClientConfiguration config ) {

        return new LibertyResteasyClientImpl(() -> new LibertyClientHttpEngineBuilder43().resteasyClientBuilder(this).build(), executor, cleanupExecutor, scheduledExecutorService, config, this);
    }

    private BundleContext getBundleContext() {
        if (isSecurityManagerPresent) {
            return AccessController.doPrivileged((PrivilegedAction<BundleContext>) () -> {
                Bundle b = FrameworkUtil.getBundle(getClass());
                return b == null ? null : b.getBundleContext(); 
            });
        }
        Bundle b = FrameworkUtil.getBundle(getClass());
        return b == null ? null : b.getBundleContext();
    }

    private void setProxyIfNeeded(ClientConfiguration clientConfig) {
        try {
            Object proxyHostProp = clientConfig.getProperty(ResteasyClientBuilder.PROPERTY_PROXY_HOST);
            if (proxyHostProp != null) {
                Object proxyPortProp = clientConfig.getProperty(ResteasyClientBuilder.PROPERTY_PROXY_PORT);
                // default if the port is not set or if it is not string or number
                Integer proxyPort = -1;
                if (proxyPortProp != null && proxyPortProp instanceof Number) {
                    proxyPort = ((Number) proxyPortProp).intValue();
                } else if (proxyPortProp != null && proxyPortProp instanceof String) {
                    proxyPort = Integer.parseInt((String) proxyPortProp);
                }
                Object proxySchemeProp = clientConfig.getProperty(ResteasyClientBuilder.PROPERTY_PROXY_SCHEME);
                defaultProxy((String)proxyHostProp, proxyPort, (String)proxySchemeProp);
            }
        } catch(Exception e) {
            // catch possible exceptions (in this case we do not set proxy at all)
            LogMessages.LOGGER.warn(Messages.MESSAGES.unableToSetHttpProxy(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Optional<ServiceReference<ClientBuilderListener>[]> getServiceRefs(BundleContext ctx) {
        if (ctx == null) {
            return Optional.empty();
        }
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
