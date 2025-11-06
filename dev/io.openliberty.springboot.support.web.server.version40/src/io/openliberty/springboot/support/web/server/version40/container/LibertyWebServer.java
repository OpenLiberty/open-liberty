/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.springboot.support.web.server.version40.container;

import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.ADDRESS;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.HTTP2;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.LIBERTY_USE_DEFAULT_HOST;
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.NEED;
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
import static com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory.WANT;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.server.servlet.ServletContextInitializers;
import org.springframework.boot.web.server.servlet.ServletWebServerSettings;
import org.springframework.boot.web.error.ErrorPage;
import org.springframework.util.ResourceUtils;

import com.ibm.ws.app.manager.springboot.container.SpringBootConfig;
import com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory;
import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.SpringConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.SpringErrorPageData;
import com.ibm.ws.springboot.support.web.server.initializer.ServerConfigurationFactory;
import com.ibm.ws.springboot.support.web.server.initializer.WebInitializer;

/**
 *
 */
public class LibertyWebServer implements WebServer {
    private static final Object token = new Object() {
    };
    private final SpringBootConfig springBootConfig;
    private final LibertyFactoryBase factoryBase;
    private final ServletWebServerSettings settings;
    private final AtomicInteger port = new AtomicInteger();

    public LibertyWebServer(AbstractConfigurableWebServerFactory factory, LibertyFactoryBase factoryBase, ServletContextInitializers initializers, ServletWebServerSettings settings) {
        this.factoryBase = factoryBase;
        this.settings = settings;
        
        port.set(factory.getPort());

        SpringBootConfigFactory configFactory = SpringBootConfigFactory.findFactory(token);
        springBootConfig = configFactory.createSpringBootConfig();
        ServerConfiguration serverConfig = getServerConfiguration(factory, factoryBase, configFactory, this);
        SpringConfiguration additionalConfig = collectAdditionalConfig(factory);

        final CountDownLatch initDone = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        springBootConfig.configure(serverConfig, new WebInitializer(factoryBase.getContextPath(), (sc) -> {
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
        }), WebInitializer.class, additionalConfig);

        try {
            initDone.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebServerException("Initialization of ServletContext got interrupted.", e);
        }
        if (exception.get() != null) {
            throw new WebServerException("Error occured initializing the ServletContext.", exception.get());
        }
    }

    private SpringConfiguration collectAdditionalConfig(AbstractConfigurableWebServerFactory factory) {
        SpringConfiguration configHolder = new SpringConfiguration();
       
        final boolean DEFAULT_COMPRESSION_ENABLED_SETTING = false;
        final boolean DEFAULT_SESSION_PERSISTENT_SETTING = false;
        final int DEFAULT_SESSION_TIMEOUT_SECONDS = 30 * 60;

        //check if spring compression configured so a not-supported warning may be issued.
        configHolder.setCompression_configured_in_spring_app(factory.getCompression().getEnabled() != DEFAULT_COMPRESSION_ENABLED_SETTING);
        // check if spring session config changes made so that a not-supported warning may be issued.
        configHolder.setSession_configured_in_spring_app(settings.getSession().getTimeout().getSeconds() != DEFAULT_SESSION_TIMEOUT_SECONDS ||
                                                         settings.getSession().isPersistent() != DEFAULT_SESSION_PERSISTENT_SETTING);
        Set<ErrorPage> errorPages = factory.getErrorPages();
        for (ErrorPage spring_ep : errorPages) {
            SpringErrorPageData ibm_spring_errpg = new SpringErrorPageData();
            ibm_spring_errpg.setLocation(spring_ep.getPath());
            if (spring_ep.getStatus() != null) {
                ibm_spring_errpg.setErrorCode(spring_ep.getStatusCode());
            } else if (spring_ep.getException() != null) {
                ibm_spring_errpg.setExceptionType(spring_ep.getExceptionName());
            } else if (spring_ep.isGlobal()) {
                ibm_spring_errpg.setGlobal(true);
            }
            configHolder.addErrorPage(ibm_spring_errpg);
        }

        for (MimeMappings.Mapping mapping : settings.getMimeMappings()) {
            configHolder.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
        }
        return configHolder;
    }

    @Override
    public int getPort() {
        return port.get();
    }

    @Override
    public void start() throws WebServerException {
        springBootConfig.start();
    }

    @Override
    public void stop() throws WebServerException {
        try {
            springBootConfig.stop();
        } finally {
            factoryBase.stopUsingDefaultHost(this);
        }
    }

    private static ServerConfiguration getServerConfiguration(AbstractConfigurableWebServerFactory factory, LibertyFactoryBase factoryBase, SpringBootConfigFactory configFactory,
                                                              LibertyWebServer container) {
        Map<String, Object> serverProperties = getServerProperties(factory, factoryBase, container);
        return ServerConfigurationFactory.createServerConfiguration(serverProperties, configFactory, (s) -> {
            try {
                return ResourceUtils.getURL(s);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Could not find the key store \"" + s + "\"", e);
            }
        });
    }

    private static Map<String, Object> getServerProperties(AbstractConfigurableWebServerFactory factory, LibertyFactoryBase factoryBase, LibertyWebServer container) {
        Map<String, Object> serverProperties = new HashMap<>();
        
        if (factoryBase.shouldUseDefaultHost(container)) {
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
            ClientAuth clientAuth = ssl.getClientAuth();
            if (clientAuth != null) {
                if (clientAuth == ClientAuth.NEED) {
                    serverProperties.put(SSL_CLIENT_AUTH, NEED);
                } else if (clientAuth == ClientAuth.WANT) {
                    serverProperties.put(SSL_CLIENT_AUTH, WANT);
                }
            }
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

        if (factory.getHttp2() != null) {
            serverProperties.put(HTTP2, factory.getHttp2().isEnabled());
        }
        return serverProperties;
    }
}
