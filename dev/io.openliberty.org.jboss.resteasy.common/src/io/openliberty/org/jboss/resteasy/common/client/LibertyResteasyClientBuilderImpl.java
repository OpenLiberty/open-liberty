/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.client;

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

import io.openliberty.restfulWS.client.ClientBuilderListener;

public class LibertyResteasyClientBuilderImpl extends ResteasyClientBuilderImpl {

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

        key.ifPresent(tupleKey -> OsgiFacade.instance().ifPresent(facade -> 
            facade.invoke(tupleKey, ClientBuilderListener.class, cbl -> cbl.built(client))));
        return client;
    }

    @Override
    protected ResteasyClient createResteasyClient(ClientHttpEngine engine,ExecutorService executor, boolean cleanupExecutor, ScheduledExecutorService scheduledExecutorService, ClientConfiguration config ) {

        return new LibertyResteasyClientImpl(() -> new LibertyClientHttpEngineBuilder43().resteasyClientBuilder(this).build(), executor, cleanupExecutor, scheduledExecutorService, config, this);
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
