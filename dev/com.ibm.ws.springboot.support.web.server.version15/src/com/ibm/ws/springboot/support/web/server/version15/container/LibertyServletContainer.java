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
package com.ibm.ws.springboot.support.web.server.version15.container;

import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.ADDRESS;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.LIBERTY_USE_DEFAULT_HOST;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.PORT;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SERVER_HEADER;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_CIPHERS;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_CLIENT_AUTH;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_ENABLED;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_KEY_ALIAS;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_KEY_PASSWORD;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_KEY_STORE;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_KEY_STORE_PASSWORD;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_KEY_STORE_PROVIDER;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_KEY_STORE_TYPE;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_PROTOCOL;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_TRUST_STORE;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_TRUST_STORE_PASSWORD;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_TRUST_STORE_PROVIDER;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.SSL_TRUST_STORE_TYPE;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.util.ResourceUtils;
import org.springframework.util.SocketUtils;

import com.ibm.ws.app.manager.springboot.container.SpringBootConfig;
import com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory;
import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;
import com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory;
import com.ibm.ws.springboot.support.web.server.initializer.WebInitializer;

/**
 *
 */
public class LibertyServletContainer implements EmbeddedServletContainer {
    private static final Object token = new Object() {};
    private final SpringBootConfig springBootConfig;
    private final LibertyServletContainerFactory factory;
    private final AtomicInteger port = new AtomicInteger();

    public LibertyServletContainer(LibertyServletContainerFactory factory, ServletContextInitializer[] initializers) {
        this.factory = factory;
        port.set(factory.getPort());
        // The Internet Assigned Numbers Authority (IANA) suggests the range 49152 to 65535 (215+214 to 216−1) for dynamic or private ports
        if (port.get() == 0) {
            port.set(SocketUtils.findAvailableTcpPort(49152));
        }
        SpringBootConfigFactory configFactory = SpringBootConfigFactory.findFactory(token);
        springBootConfig = configFactory.createSpringBootConfig();
        ServerConfiguration serverConfig = getServerConfiguration(factory, configFactory, this);

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

    @Override
    public int getPort() {
        return port.get();
    }

    @Override
    public void start() throws EmbeddedServletContainerException {
        springBootConfig.start();
    }

    @Override
    public void stop() throws EmbeddedServletContainerException {
        try {
            springBootConfig.stop();
        } finally {
            factory.stopUsingDefaultHost(this);
        }
    }

    private static ServerConfiguration getServerConfiguration(LibertyServletContainerFactory factory, SpringBootConfigFactory configFactory, LibertyServletContainer container) {
        Map<String, Object> serverProperties = getServerProperties(factory, container);
        return ServerConfigurationFactory.createServerConfiguration(serverProperties, configFactory, (s) -> {
            try {
                return ResourceUtils.getURL(s);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Could not find the key store \"" + s + "\"", e);
            }
        });
    }

    private static Map<String, Object> getServerProperties(LibertyServletContainerFactory factory, LibertyServletContainer container) {
        Map<String, Object> serverProperties = new HashMap<>();
        if (factory.shouldUseDefaultHost(container)) {
            serverProperties.put(LIBERTY_USE_DEFAULT_HOST, Boolean.TRUE);
        }

        serverProperties.put(PORT, factory.getPort());

        if (factory.getAddress() != null) {
            serverProperties.put(ADDRESS, factory.getAddress().getHostAddress());
        }

        serverProperties.put(SERVER_HEADER, factory.getServerHeader());

        Ssl ssl = factory.getSsl();
        if (ssl != null) {
            serverProperties.put(SSL_CIPHERS, ssl.getCiphers());
            serverProperties.put(SSL_CLIENT_AUTH, ssl.getClientAuth());
            serverProperties.put(SSL_ENABLED, ssl.isEnabled());
            serverProperties.put(SSL_KEY_ALIAS, ssl.getKeyAlias());
            serverProperties.put(SSL_KEY_PASSWORD, ssl.getKeyPassword());
            serverProperties.put(SSL_KEY_STORE, ssl.getKeyStore());
            serverProperties.put(SSL_KEY_STORE_PASSWORD, ssl.getKeyStorePassword());
            serverProperties.put(SSL_KEY_STORE_PROVIDER, ssl.getKeyStoreProvider());
            serverProperties.put(SSL_KEY_STORE_TYPE, ssl.getKeyStoreType());
            serverProperties.put(SSL_PROTOCOL, ssl.getProtocol());
            serverProperties.put(SSL_TRUST_STORE, ssl.getTrustStore());
            serverProperties.put(SSL_TRUST_STORE_PASSWORD, ssl.getTrustStorePassword());
            serverProperties.put(SSL_TRUST_STORE_PROVIDER, ssl.getTrustStoreProvider());
            serverProperties.put(SSL_TRUST_STORE_TYPE, ssl.getTrustStoreType());
        }
        return serverProperties;
    }
}
