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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientImpl;

public class LibertyResteasyClientImpl extends ResteasyClientImpl {
    private final LibertyResteasyClientBuilderImpl builder;
    private final Supplier<ClientHttpEngine> httpEngineSupplier;
    private AtomicReference<ClientHttpEngine> httpEngine = new AtomicReference<>();

    protected LibertyResteasyClientImpl(final Supplier<ClientHttpEngine> httpEngine,
                                        final ExecutorService asyncInvocationExecutor,
                                        final boolean cleanupExecutor,
                                        final ScheduledExecutorService scheduledExecutorService,
                                        final ClientConfiguration configuration,
                                        final LibertyResteasyClientBuilderImpl builder) {
       super(null, asyncInvocationExecutor, cleanupExecutor, scheduledExecutorService, configuration);
       this.builder = builder;
       this.httpEngineSupplier = httpEngine;
    }

    @Override
    protected ResteasyWebTarget createClientWebTarget(ResteasyClientImpl client, String uri, ClientConfiguration configuration) {
        return new LibertyClientWebTarget(client, uri, configuration, builder);
    }

    @Override
    protected ResteasyWebTarget createClientWebTarget(ResteasyClientImpl client, URI uri, ClientConfiguration configuration) {
        return new LibertyClientWebTarget(client, uri, configuration, builder);
    }

    @Override
    protected ResteasyWebTarget createClientWebTarget(ResteasyClientImpl client, UriBuilder uriBuilder, ClientConfiguration configuration) {
        return new LibertyClientWebTarget(client, uriBuilder, configuration, builder);
    }

    @Override
    public ClientHttpEngine httpEngine() {
        ClientHttpEngine engine = httpEngine.get();
        if (engine == null) {
            httpEngine.compareAndSet(null, httpEngineSupplier.get());
            engine = httpEngine.get();
        }
        return engine;
    }

    @Override
    public void close() {
        closed = true;
        try {
            ClientHttpEngine engine = httpEngine.get();
            if (engine != null) {
                engine.close();
            }
            if (cleanupExecutor) {
                if (System.getSecurityManager() == null) {
                    asyncInvocationExecutor.shutdown();
                } else {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            asyncInvocationExecutor.shutdown();
                            return null;
                        }
                    });
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
