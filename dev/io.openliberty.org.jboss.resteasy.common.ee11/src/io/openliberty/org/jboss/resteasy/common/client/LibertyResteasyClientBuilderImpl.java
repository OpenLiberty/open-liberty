/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.i18n.LogMessages;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.concurrent.ContextualExecutorService;
import org.jboss.resteasy.concurrent.ContextualExecutors;

import io.openliberty.restfulWS.client.AsyncClientExecutorService;
import io.openliberty.restfulWS.client.ClientBuilderListener;
import io.openliberty.restfulWS.client.internal.AsyncClientExecutorHelper;

public class LibertyResteasyClientBuilderImpl extends ResteasyClientBuilderImpl {

    private static AsyncClientExecutorHelper executorHelper;
    
    public static void setAsyncClientExecutorHelper(AsyncClientExecutorHelper helper) {
        executorHelper = helper;
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

        List<Runnable> closeActions = new ArrayList<>();
        AsyncClientExecutorHelper helper = executorHelper;
        if (asyncExecutor == null && helper != null) {
            cleanupExecutor = false;
            ExecutorService executorService = helper.getExecutorService();
            if (executorService != null) {
                asyncExecutor = new AsyncClientExecutorService(executorService);
            }
        }

        boolean resetProxy = false;
        if (getDefaultProxyHostname() == null) {
           resetProxy = true;
           // check for proxy config parameters
           setProxyIfNeeded(config);
        }

        if (resetProxy) {
           // Reset proxy by setting it to null
           defaultProxy(null, -1, null);
        }

        ExecutorService executor = getExecutorService();
        ResteasyClient client = createResteasyClient(null, executor, cleanupExecutor, ContextualExecutors.wrap(scheduledExecutorService, true), config, closeActions);

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
                                             executor, cleanupExecutor, ContextualExecutors.wrap(scheduledExecutorService), config, closeActions, this);
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

    private ContextualExecutorService getExecutorService() {
        if (asyncExecutor != null) {
           return ContextualExecutors.wrap(asyncExecutor, !cleanupExecutor);
        }
        return ContextualExecutors.threadPool();
     }
}
