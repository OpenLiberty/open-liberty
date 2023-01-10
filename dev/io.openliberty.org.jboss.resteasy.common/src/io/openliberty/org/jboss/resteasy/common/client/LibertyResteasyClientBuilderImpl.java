/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package io.openliberty.org.jboss.resteasy.common.client;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

import io.openliberty.restfulWS.client.AsyncClientExecutorService;
import io.openliberty.restfulWS.client.ClientBuilderListener;

@SuppressWarnings("unchecked")
public class LibertyResteasyClientBuilderImpl extends ResteasyClientBuilderImpl {

    private final static Class<? extends ExecutorService> MANAGED_EXECUTOR_SERVICE_CLASS;

    static {
        Class<? extends ExecutorService> clazz;
        try {
            clazz = (Class<? extends ExecutorService>) Class.forName("javax.enterprise.concurrent.ManagedExecutorService", false,
                AccessController.doPrivileged((PrivilegedAction<ClassLoader>)() -> Thread.currentThread().getContextClassLoader()));
        } catch (Throwable t) {
            clazz = null;
        }
        MANAGED_EXECUTOR_SERVICE_CLASS = clazz;
    }

    @Override
    public ResteasyClient build() {
        // using facade to avoid trying OSGi services unless OSGi is available 
        Optional<Integer> key = OsgiFacade.instance().map(facade ->
            facade.invoke(ClientBuilderListener.class, cbl -> cbl.building(this)));


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
        List<Runnable> closeActions = new ArrayList<>();

        if (executor == null && MANAGED_EXECUTOR_SERVICE_CLASS != null) {
           cleanupExecutor = false;
           OsgiFacade facade = OsgiFacade.instance().orElse(null);
           if (facade != null) {
               Object serviceRef = facade.getServiceRef(MANAGED_EXECUTOR_SERVICE_CLASS).orElse(null);
               if (serviceRef != null) {
                   closeActions.add(() -> facade.ungetService(serviceRef));
                   executor = facade.getService(serviceRef, MANAGED_EXECUTOR_SERVICE_CLASS);
               }
           }
        }
        if (executor == null)
        {
           cleanupExecutor = true;
           executor = Executors.newCachedThreadPool();
        }
        
        executor = new AsyncClientExecutorService(executor);

        boolean resetProxy = false;
        if (this.defaultProxy == null) {
           resetProxy = true;
           // check for proxy config parameters
           setProxyIfNeeded(config);
        }

        if (resetProxy) {
           this.defaultProxy = null;
        }
        ResteasyClient client = createResteasyClient(null, executor, cleanupExecutor, scheduledExecutorService, config, closeActions);

        key.ifPresent(tupleKey -> OsgiFacade.instance().ifPresent(facade -> 
            facade.invoke(tupleKey, ClientBuilderListener.class, cbl -> cbl.built(client))));
        return client;
    }

    @Override
    protected ResteasyClient createResteasyClient(ClientHttpEngine engine, ExecutorService executor, boolean cleanupExecutor,
                                                  ScheduledExecutorService scheduledExecutorService, ClientConfiguration config) {
        return createResteasyClient(engine, executor, cleanupExecutor, scheduledExecutorService, config, Collections.emptyList());
    }

    protected ResteasyClient createResteasyClient(ClientHttpEngine engine, ExecutorService executor, boolean cleanupExecutor,
                                                  ScheduledExecutorService scheduledExecutorService, ClientConfiguration config,
                                                  List<Runnable> closeActions) {

        return new LibertyResteasyClientImpl(() -> new LibertyClientHttpEngineBuilder43().resteasyClientBuilder(this).build(),
                                             executor, cleanupExecutor, scheduledExecutorService, config, closeActions, this);
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
}
