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
package com.ibm.ws.springboot.support.web.server.version20.container;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.util.ResourceUtils;

import com.ibm.ws.app.manager.springboot.container.SpringBootConfig;
import com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory;
import com.ibm.ws.app.manager.springboot.container.config.ConfigElementList;
import com.ibm.ws.app.manager.springboot.container.config.HttpEndpoint;
import com.ibm.ws.app.manager.springboot.container.config.HttpSession;
import com.ibm.ws.app.manager.springboot.container.config.KeyEntry;
import com.ibm.ws.app.manager.springboot.container.config.KeyStore;
import com.ibm.ws.app.manager.springboot.container.config.SSLConfig;
import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.VirtualHost;
import com.ibm.ws.springboot.support.web.server.initializer.WebInitializer;

/**
 *
 */
public class LibertyServletContainer implements WebServer {
    private static final Object token = new Object() {};
    private final SpringBootConfig springBootConfig;
    private final AtomicInteger port = new AtomicInteger();
    private static final String SECURITY_DIR = "resources/security/";
    private static final String SPRING_VIRTUALHOST = "springVirtualHost-";
    private static final String SPRING_HTTPENDPOINT = "springHttpEndpoint-";
    private static final String SPRING_SSLCONFIG = "springSslConfig-";
    private static final String SPRING_KEYSTORE = "springKeyStore-";
    private static final String SPRING_TRUSTSTORE = "springTrustStore-";
    private static final String SPRING_KEYENTRY = "springKeyEntry-";
    private static final String SPRING_CONFIG = "springConfig-";

    public LibertyServletContainer(LibertyServletContainerFactory factory, ServletContextInitializer[] initializers) {
        port.set(factory.getPort());
        SpringBootConfigFactory configFactory = SpringBootConfigFactory.findFactory(token);
        springBootConfig = configFactory.createSpringBootConfig();
        String springBootConfigId = springBootConfig.getId();
        ServerConfiguration serverConfig = getServerConfiguration(factory, configFactory, springBootConfigId);
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
            throw new WebServerException("Initialization of ServletContext got interrupted.", e);
        }
        if (exception.get() != null) {
            throw new WebServerException("Error occured initializing the ServletContext.", exception.get());
        }
    }

    @Override
    public int getPort() {
        // TODO get real port when configured with zero
        return port.get();
    }

    @Override
    public void start() throws WebServerException {
        springBootConfig.start();
    }

    @Override
    public void stop() throws WebServerException {
        springBootConfig.stop();
    }

    private static ServerConfiguration getServerConfiguration(LibertyServletContainerFactory factory, SpringBootConfigFactory configFactory, String springBootConfigId) {
        ServerConfiguration serverConfig = new ServerConfiguration();
        configureEndpoint(serverConfig, factory, configFactory, springBootConfigId);
        configureVirtualHost(serverConfig, factory, springBootConfigId);
        serverConfig.setDescription(SPRING_CONFIG + springBootConfigId);
        return serverConfig;
    }

    private static void configureVirtualHost(ServerConfiguration serverConfig, LibertyServletContainerFactory factory, String springBootConfigId) {
        List<VirtualHost> virtualHosts = serverConfig.getVirtualHosts();
        virtualHosts.clear();
        VirtualHost virtualHost = new VirtualHost();
        virtualHost.setId(SPRING_VIRTUALHOST + springBootConfigId);
        HttpEndpoint httpEndpoint = serverConfig.getHttpEndpoints().iterator().next();
        virtualHost.setAllowFromEndpoint(httpEndpoint.getId());
        Set<String> aliases = virtualHost.getHostAliases();
        aliases.clear();
        // TODO would be better to use *:* for wildcarding the port
        aliases.add("*:" + factory.getPort());
        virtualHosts.add(virtualHost);
    }

    private static void configureEndpoint(ServerConfiguration serverConfig, LibertyServletContainerFactory factory, SpringBootConfigFactory configFactory,
                                          String springBootConfigId) {
        List<HttpEndpoint> endpoints = serverConfig.getHttpEndpoints();
        endpoints.clear();
        HttpEndpoint endpoint = new HttpEndpoint();
        endpoint.setId(SPRING_HTTPENDPOINT + springBootConfigId);
        if (factory.getAddress() != null) {
            endpoint.setHost(factory.getAddress().getHostAddress());
        } else {
            endpoint.setHost("*");
        }

        Ssl ssl = factory.getSsl();
        // For ssl authentication a key store is always required for the server
        // to provide its digital certificate along with its private key.
        // If the key store is not provided then the server is not authenticated
        // to have a secure connection.
        if (ssl != null && ssl.getKeyStore() != null) {
            endpoint.setHttpPort(-1);
            endpoint.setHttpsPort(factory.getPort());
            endpoint.getSslOptions().setSslRef(SPRING_SSLCONFIG + springBootConfigId);
            configureSsl(serverConfig, ssl, springBootConfigId);

            ConfigElementList<KeyStore> keyStores = serverConfig.getKeyStores();
            keyStores.clear();

            configureKeyStore(keyStores, ssl, configFactory, springBootConfigId);

            if (ssl.getTrustStore() != null) {
                configureTrustStore(keyStores, ssl, configFactory, springBootConfigId);
            }
        } else {
            endpoint.setHttpPort(factory.getPort());
            endpoint.setHttpsPort(-1);
        }

        if (factory.getServerHeader() != null) {
            endpoint.getHttpOptions().setServerHeaderValue(factory.getServerHeader());
        }

        if (factory.getSession().getTimeout().getSeconds() > 0) {
            configureSession(serverConfig, factory);
        }
        endpoints.add(endpoint);
    }

    private static void configureSsl(ServerConfiguration serverConfig, Ssl ssl, String springBootConfigId) {
        ConfigElementList<SSLConfig> ssls = serverConfig.getSsls();
        ssls.clear();
        SSLConfig sslConfig = new SSLConfig();
        sslConfig.setId(SPRING_SSLCONFIG + springBootConfigId);
        sslConfig.setKeyStoreRef(SPRING_KEYSTORE + springBootConfigId);

        if (ssl.getTrustStore() != null) {
            sslConfig.setTrustStoreRef(SPRING_TRUSTSTORE + springBootConfigId);
        }
        if (ssl.getProtocol() != null) {
            sslConfig.setSslProtocol(ssl.getProtocol());
        }
        if (ssl.getClientAuth() != null) {
            configureClientAuthentication(sslConfig, ssl);
        }
        if (ssl.getCiphers() != null) {
            configureEnabledCiphers(sslConfig, ssl);
        }
        ssls.add(sslConfig);
    }

    private static void configureKeyStore(ConfigElementList<KeyStore> keyStores, Ssl ssl, SpringBootConfigFactory configFactory, String springBootConfigId) {
        URL keyStoreURL;
        KeyStore keyStore = new KeyStore();
        keyStore.setId(SPRING_KEYSTORE + springBootConfigId);

        try {
            keyStoreURL = ResourceUtils.getURL(ssl.getKeyStore());
        } catch (IOException e) {
            throw new WebServerException("Could not find the key store \"" + ssl.getKeyStore() + "\"", e);
        }
        String keyStoreURLString = keyStoreURL.toString();
        String keyStoreName = keyStoreURLString.substring(keyStoreURLString.lastIndexOf("/") + 1);
        int dot = keyStoreName.lastIndexOf(".");
        keyStoreName = keyStoreName.substring(0, dot) + "-" + springBootConfigId + keyStoreName.substring(dot);

        File securityDir = new File(configFactory.getServerDir(), SECURITY_DIR);

        File keyStoreFile = new File(securityDir, keyStoreName);

        try (InputStream in = keyStoreURL.openStream()) {
            writeFile(in, keyStoreFile);
        } catch (IOException e) {
            throw new WebServerException("Unable to copy keystore to server home/resources/security directory.", e);
        }
        keyStore.setLocation(keyStoreName);
        if (ssl.getKeyStorePassword() != null) {
            keyStore.setPassword(ssl.getKeyStorePassword());
        }
        if (ssl.getKeyStoreType() != null) {
            keyStore.setType(ssl.getKeyStoreType());
        }
        if (ssl.getKeyAlias() != null || ssl.getKeyPassword() != null) {
            configureKeyEntry(keyStore, ssl, springBootConfigId);
        }
        keyStores.add(keyStore);
    }

    private static void configureKeyEntry(KeyStore keystore, Ssl ssl, String springBootConfigId) {
        ConfigElementList<KeyEntry> keyEntries = keystore.getKeyEntries();
        keyEntries.clear();
        KeyEntry keyEntry = new KeyEntry();
        keyEntry.setId(SPRING_KEYENTRY + springBootConfigId);
        if (ssl.getKeyAlias() != null) {
            keyEntry.setName(ssl.getKeyAlias());
        } else {
            keyEntry.setName("keyEntry");
        }
        if (ssl.getKeyPassword() != null) {
            keyEntry.setKeyPassword(ssl.getKeyPassword());
        }
        keyEntries.add(keyEntry);
    }

    private static void configureTrustStore(ConfigElementList<KeyStore> keyStores, Ssl ssl, SpringBootConfigFactory configFactory, String springBootConfigId) {
        KeyStore keyStore = new KeyStore();
        URL trustStoreURL;
        try {
            trustStoreURL = ResourceUtils.getURL(ssl.getTrustStore());
        } catch (IOException e) {
            throw new WebServerException("Could not find the trust store \"" + ssl.getTrustStore() + "\"", e);
        }
        String trustStoreURLString = trustStoreURL.toString();
        String trustStoreName = trustStoreURLString.substring(trustStoreURLString.lastIndexOf("/") + 1);
        int dot = trustStoreName.lastIndexOf(".");
        trustStoreName = trustStoreName.substring(0, dot) + "-" + springBootConfigId + trustStoreName.substring(dot);

        File trustStoreDir = new File(configFactory.getServerDir(), SECURITY_DIR);
        File trustStoreFile = new File(trustStoreDir, trustStoreName);

        try (InputStream in = trustStoreURL.openStream()) {
            writeFile(in, trustStoreFile);
        } catch (IOException e) {
            throw new WebServerException("Unable to copy truststore to server home/resources/security directory.", e);
        }

        keyStore.setId(SPRING_TRUSTSTORE + springBootConfigId);
        keyStore.setLocation(trustStoreName);
        if (ssl.getTrustStorePassword() != null) {
            keyStore.setPassword(ssl.getTrustStorePassword());
        }
        if (ssl.getTrustStoreType() != null) {
            keyStore.setType(ssl.getTrustStoreType());
        }
        keyStores.add(keyStore);
    }

    private static void configureClientAuthentication(SSLConfig sslConfig, Ssl ssl) {
        if (ssl.getClientAuth() == ClientAuth.NEED) {
            sslConfig.setClientAuthentication(Boolean.TRUE);
        } else if (ssl.getClientAuth() == ClientAuth.WANT) {
            sslConfig.setClientAuthenticationSupported(Boolean.TRUE);
        }
    }

    private static void configureEnabledCiphers(SSLConfig sslConfig, Ssl ssl) {
        String[] ciphers = ssl.getCiphers();
        String enabledCiphers = null;
        if (ciphers.length > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String cipher : ciphers) {
                stringBuilder.append(cipher).append(" ");
            }
            enabledCiphers = stringBuilder.toString();
        }
        if (enabledCiphers != null) {
            sslConfig.setEnabledCiphers(enabledCiphers);
        }
    }

    private static void configureSession(ServerConfiguration serverConfig, LibertyServletContainerFactory factory) {
        // TODO is this only configurable for all endpoints?
        HttpSession session = serverConfig.getHttpSession();
        session.setInvalidationTimeout((int) factory.getSession().getTimeout().getSeconds());
    }

    private static void writeFile(InputStream in, File dest) throws FileNotFoundException, IOException {
        dest.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            byte buffer[] = new byte[4096];
            int count;
            while ((count = in.read(buffer, 0, buffer.length)) > 0) {
                fos.write(buffer, 0, count);
            }
        }
    }
}
