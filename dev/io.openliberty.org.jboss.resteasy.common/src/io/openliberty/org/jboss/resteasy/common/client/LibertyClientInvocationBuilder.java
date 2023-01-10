/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import java.net.URI;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;

public class LibertyClientInvocationBuilder extends ClientInvocationBuilder {
    /**
     * @param client
     * @param uri
     * @param configuration
     */
    public LibertyClientInvocationBuilder(ResteasyClient client, URI uri, ClientConfiguration configuration) {
        super(client, uri, configuration);
    }

    @Override
    public AsyncInvoker async() {
        setDefaultAcceptHeaderIfNecessary();
        return super.async();
    }

    @Override
    public Invocation build(String method, Entity<?> entity) {
        setDefaultAcceptHeaderIfNecessary();
        return super.build(method, entity);
    }

    @Override
    public CompletionStageRxInvoker rx() {
        setDefaultAcceptHeaderIfNecessary();
        return super.rx();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <T extends RxInvoker> T rx(Class<T> clazz) {
        setDefaultAcceptHeaderIfNecessary();
        return super.rx(clazz);
    }

    private void setDefaultAcceptHeaderIfNecessary() {
        //set Accept: */* if header is not already set
        if (getHeaders().getHeader(HttpHeaders.ACCEPT) == null) {
            this.accept(MediaType.WILDCARD);
            //setDefaultWildcardAcceptHeader.set(true);;
        }
    }
}
