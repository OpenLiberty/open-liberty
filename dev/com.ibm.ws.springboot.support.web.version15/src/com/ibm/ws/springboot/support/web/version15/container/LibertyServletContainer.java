/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.web.version15.container;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import com.ibm.ws.app.manager.springboot.container.SpringBootConfig;
import com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory;
import com.ibm.ws.app.manager.springboot.container.config.HttpEndpoint;
import com.ibm.ws.app.manager.springboot.container.config.HttpSession;
import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.VirtualHost;
import com.ibm.ws.springboot.support.web.initializer.WebInitializer;

/**
 *
 */
public class LibertyServletContainer implements EmbeddedServletContainer {
    private static final Object token = new Object() {};
    private final SpringBootConfig springBootConfig;
    private final AtomicInteger port = new AtomicInteger();

    /**
     * @param libertyServletContainerFactory
     * @param mergeInitializers
     */
    public LibertyServletContainer(LibertyServletContainerFactory factory, ServletContextInitializer[] initializers) {
        port.set(factory.getPort());
        ServerConfiguration serverConfig = getServerConfiguration(factory);
        SpringBootConfigFactory configFactory = SpringBootConfigFactory.findFactory(token);
        springBootConfig = configFactory.createSpringBootConfig();
        final CountDownLatch initDone = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        springBootConfig.configure(serverConfig, new WebInitializer(factory.getContextPath(), (sc) -> {
            try {
                for (ServletContextInitializer servletContextInitializer : initializers) {
                    try {
                        servletContextInitializer.onStartup(sc);
                    } catch (Throwable t) {
                        exception.set(t);
                        break;
                    }
                }
            } finally {
                initDone.countDown();
            }
            return sc;
        }), WebInitializer.class);

        try {
            initDone.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddedServletContainerException("Initialization of ServletContext got interrupted.", e);
        }
        if (exception.get() != null) {
            throw new EmbeddedServletContainerException("Error occured initializing the ServletContext.", exception.get());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.boot.context.embedded.EmbeddedServletContainer#getPort()
     */
    @Override
    public int getPort() {
        // TODO get real port when configured with zero
        return port.get();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.boot.context.embedded.EmbeddedServletContainer#start()
     */
    @Override
    public void start() throws EmbeddedServletContainerException {
        springBootConfig.start();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.boot.context.embedded.EmbeddedServletContainer#stop()
     */
    @Override
    public void stop() throws EmbeddedServletContainerException {
        springBootConfig.stop();
    }

    private static ServerConfiguration getServerConfiguration(LibertyServletContainerFactory factory) {
        ServerConfiguration serverConfig = new ServerConfiguration();
        List<HttpEndpoint> endpoints = serverConfig.getHttpEndpoints();
        endpoints.clear();
        HttpEndpoint endpoint = new HttpEndpoint();
        if (factory.getAddress() != null) {
            endpoint.setHost(factory.getAddress().getHostAddress());
        } else {
            endpoint.setHost("0.0.0.0");
        }

        Ssl ssl = factory.getSsl();
        if (ssl == null || ssl.getKeyStore() == null) {
            endpoint.setHttpPort(factory.getPort());
            endpoint.setHttpsPort(-1);
            // TODO configure ssl
        } else {
            endpoint.setHttpPort(-1);
            endpoint.setHttpsPort(factory.getPort());
        }

        if (factory.getServerHeader() != null) {
            endpoint.getHttpOptions().setServerHeaderValue(factory.getServerHeader());
        }

        if (factory.getSessionTimeout() > 0) {
            configureSession(serverConfig, factory);
        }
        endpoints.add(endpoint);

        List<VirtualHost> virtualHosts = serverConfig.getVirtualHosts();
        virtualHosts.clear();
        VirtualHost virtualHost = new VirtualHost();
        Set<String> aliases = virtualHost.getHostAliases();
        aliases.clear();
        // TODO would be better to use *:* for wildcarding the port
        aliases.add("*:" + factory.getPort());
        virtualHosts.add(virtualHost);
        return serverConfig;
    }

    /**
     * @param serverConfig
     */
    private static void configureSession(ServerConfiguration serverConfig, LibertyServletContainerFactory factory) {
        // TODO is this only configurable for all endpoints?
        HttpSession session = serverConfig.getHttpSession();
        session.setInvalidationTimeout(factory.getSessionTimeout());
    }
}
