/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client;

import java.net.URL;
import java.util.Map;

import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.client.spec.ClientConfigurableImpl;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

/**
 *
 */
public class RestClientBuilderImpl extends RestClientBuilder {

    private final JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    private final Configurable<RestClientBuilder> configImpl = new ClientConfigurableImpl<RestClientBuilder>(this);
    private URL baseUrl;

    public RestClientBuilderImpl() {
        //bean.setProperties(properties);
    }
    /* (non-Javadoc)
     * @see javax.ws.rs.core.Configurable#getConfiguration()
     */
    @Override
    public Configuration getConfiguration() {
        return configImpl.getConfiguration();
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.core.Configurable#property(java.lang.String, java.lang.Object)
     */
    @Override
    public RestClientBuilder property(String key, Object value) {
        configImpl.property(key, value);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.core.Configurable#register(java.lang.Class)
     */
    @Override
    public RestClientBuilder register(Class<?> componentClass) {
        configImpl.register(componentClass);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.core.Configurable#register(java.lang.Object)
     */
    @Override
    public RestClientBuilder register(Object component) {
        configImpl.register(component);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.core.Configurable#register(java.lang.Class, int)
     */
    @Override
    public RestClientBuilder register(Class<?> componentClass, int priority) {
        configImpl.register(componentClass, priority);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.core.Configurable#register(java.lang.Class, java.lang.Class[])
     */
    @Override
    public RestClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        configImpl.register(componentClass, contracts);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.core.Configurable#register(java.lang.Class, java.util.Map)
     */
    @Override
    public RestClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        configImpl.register(componentClass, contracts);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.core.Configurable#register(java.lang.Object, int)
     */
    @Override
    public RestClientBuilder register(Object component, int priority) {
        configImpl.register(component, priority);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.core.Configurable#register(java.lang.Object, java.lang.Class[])
     */
    @Override
    public RestClientBuilder register(Object component, Class<?>... contracts) {
        configImpl.register(component, contracts);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.core.Configurable#register(java.lang.Object, java.util.Map)
     */
    @Override
    public RestClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        configImpl.register(component, contracts);
        return this;
    }

    /* (non-Javadoc)
     * @see org.eclipse.microprofile.rest.client.RestClientBuilder#baseUrl(java.net.URL)
     */
    @Override
    public RestClientBuilder baseUrl(URL url) {
        baseUrl = url;
        bean.setAddress(url.toString());
        return this;
    }

    /* (non-Javadoc)
     * @see org.eclipse.microprofile.rest.client.RestClientBuilder#build(java.lang.Class)
     */
    @Override
    public synchronized <T> T build(Class<T> clazz) throws IllegalStateException {
        if (baseUrl == null) {
            throw new IllegalStateException("baseUrl not set");
        }
        bean.getServiceFactory().setResourceClass(clazz);
        T client = bean.create(clazz);

        //TODO: using ClientImpl.WebTargetImpl.request() as a guide, push the config
        //      into WebClient.getConfig(client)'s ClientConfiguration

        return client;
    }

}
