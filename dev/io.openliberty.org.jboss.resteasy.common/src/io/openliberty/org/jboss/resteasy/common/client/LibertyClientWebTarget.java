/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.client;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ClientWebTarget;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class LibertyClientWebTarget extends ClientWebTarget {
    private static final TraceComponent tc = Tr.register(LibertyClientWebTarget.class);

    private final LibertyResteasyClientBuilderImpl builder;
    
    public LibertyClientWebTarget(final ResteasyClient client, final String uri, final ClientConfiguration configuration, LibertyResteasyClientBuilderImpl builder) throws IllegalArgumentException, NullPointerException
    {
       super(client, uri, configuration);
       this.builder = builder;
       applyConfiguredProperties();
    }

    public LibertyClientWebTarget(final ResteasyClient client, final URI uri, final ClientConfiguration configuration, LibertyResteasyClientBuilderImpl builder) throws NullPointerException
    {
       super(client, uri, configuration);
       this.builder = builder;
       applyConfiguredProperties();
    }

    public LibertyClientWebTarget(final ResteasyClient client, final UriBuilder uriBuilder, final ClientConfiguration configuration, LibertyResteasyClientBuilderImpl builder) throws NullPointerException
    {
       super(client, uriBuilder, configuration);
       this.builder = builder;
       applyConfiguredProperties();
    }

    @Override
    protected ClientWebTarget newInstance(ResteasyClient client, UriBuilder uriBuilder, ClientConfiguration configuration) {
        return new LibertyClientWebTarget(client, uriBuilder, configuration, builder);
    }

    @Override
    protected ClientInvocationBuilder createClientInvocationBuilder(ResteasyClient client, URI uri, ClientConfiguration configuration) {
        return new LibertyClientInvocationBuilder(client, uri, configuration);
    }
    
    private void applyConfiguredProperties() {
        //apply config properties that were supplied in server.xml.
        try {
            Map<String, String> props = JAXRSClientConfigHolder.getURIProps(uriBuilder);
            if (props != null && props.size() > 0) {
                for (String key : props.keySet()) {
                    configuration.property(key, props.get(key));
                }
            }
        } catch (IllegalArgumentException iaex) {
            // incomplete url encountered from uriBuilder.build, we can't act on it
        }

        JAXRSClientConstants.mapProperties(configuration);
        
        // for timeouts and proxy settings, update ClientBuilder
        Long timeout = toLong(configuration, JAXRSClientConstants.CONNECTION_TIMEOUT);
        if (timeout != null) {
            builder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
        }
        timeout = toLong(configuration, JAXRSClientConstants.RECEIVE_TIMEOUT);
        if (timeout != null) {
            builder.readTimeout(timeout, TimeUnit.MILLISECONDS);
        }
        Boolean followRedirects = toBoolean(configuration, JAXRSClientConstants.AUTO_FOLLOW_REDIRECTS);
        if (followRedirects != null) {
            ((LibertyResteasyClientImpl)client).setAutoFollowRedirects(followRedirects);
        }
        String proxyHost = (String) configuration.getProperty(ResteasyClientBuilder.PROPERTY_PROXY_HOST);
        if (proxyHost != null) {
            Integer proxyPort = toInt(configuration, ResteasyClientBuilder.PROPERTY_PROXY_PORT);
            String proxyScheme = (String) configuration.getProperty(ResteasyClientBuilder.PROPERTY_PROXY_SCHEME);
            builder.defaultProxy(proxyHost, proxyPort == null ? -1 : proxyPort, proxyScheme);
        }
    }

    private static Long toLong(ClientConfiguration configuration, String key) {
        Object o = configuration.getProperty(key);
        if (o == null) return null;
        if (o instanceof Long) return (Long)o;
        
        try {
            if (o instanceof String) {
                return Long.parseLong((String)o);
            }
            return (Long) o;
        } catch (ClassCastException | NumberFormatException ex) {
            Tr.warning(tc, "INVALID_LONG_PROPERTY_CWWKW1302W", key, o);
        }
        return null;
    }

    private static Integer toInt(ClientConfiguration configuration, String key) {
        Object o = configuration.getProperty(key);
        if (o == null) return null;
        if (o instanceof Integer) return (Integer)o;
        
        try {
            if (o instanceof String) {
                return Integer.parseInt((String)o);
            }
            return (Integer) o;
        } catch (ClassCastException | NumberFormatException ex) {
            Tr.warning(tc, "INVALID_INT_PROPERTY_CWWKW1303W", key, o);
        }
        return null;
    }

    private static Boolean toBoolean(ClientConfiguration configuration, String key) {
        Object o = configuration.getProperty(key);
        if (o == null) return null;
        if (o instanceof Boolean) return (Boolean)o;
        if (o instanceof String) return Boolean.parseBoolean((String)o);
        
        try {
            // try direct cast - log a warning if this fails
            return (Boolean) o;
        } catch (ClassCastException ex) {
            Tr.warning(tc, "INVALID_BOOLEAN_PROPERTY_CWWKW1304W", key, o);
        }
        return null;
    }
}
