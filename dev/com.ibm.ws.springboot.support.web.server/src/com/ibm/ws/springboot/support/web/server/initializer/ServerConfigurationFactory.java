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
package com.ibm.ws.springboot.support.web.server.initializer;

import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.ID_HTTP_ENDPOINT;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.ID_KEY_STORE;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.ID_SSL;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.ID_TRUST_STORE;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.ID_VIRTUAL_HOST;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import com.ibm.ws.app.manager.springboot.container.ApplicationError;
import com.ibm.ws.app.manager.springboot.container.ApplicationTr.Type;
import com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory;
import com.ibm.ws.app.manager.springboot.container.config.ConfigElementList;
import com.ibm.ws.app.manager.springboot.container.config.HttpEndpoint;
import com.ibm.ws.app.manager.springboot.container.config.KeyEntry;
import com.ibm.ws.app.manager.springboot.container.config.KeyStore;
import com.ibm.ws.app.manager.springboot.container.config.SSLConfig;
import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.VirtualHost;

/**
 *
 */
@SuppressWarnings("restriction")
public class ServerConfigurationFactory {
    private static final String SECURITY_DIR = "resources/security/";
    public static final String LIBERTY_USE_DEFAULT_HOST = "server.liberty.use-default-host";
    public static final String PORT = "port";
    public static final String ADDRESS = "address";
    public static final String SERVER_HEADER = "server.header";
    public static final String SSL_CIPHERS = "ssl.ciphers";
    public static final String SSL_CLIENT_AUTH = "ssl.client-auth";
    public static final String SSL_ENABLED = "ssl.enabled";
    public static final String SSL_ENABLED_PROTOCOLS = "ssl.enabled-protocols";
    public static final String SSL_KEY_ALIAS = "ssl.key-alias";
    public static final String SSL_KEY_PASSWORD = "ssl.key-password";
    public static final String SSL_KEY_STORE = "ssl.key-store";
    public static final String SSL_KEY_STORE_PASSWORD = "ssl.key-store-password";
    public static final String SSL_KEY_STORE_PROVIDER = "ssl.key-store-provider";
    public static final String SSL_KEY_STORE_TYPE = "ssl.key-store-type";
    public static final String SSL_PROTOCOL = "ssl.protocol";
    public static final String SSL_TRUST_STORE = "ssl.trust-store";
    public static final String SSL_TRUST_STORE_PASSWORD = "ssl.trust-store-password";
    public static final String SSL_TRUST_STORE_PROVIDER = "ssl.trust-store-provider";
    public static final String SSL_TRUST_STORE_TYPE = "ssl.trust-store-type";
    public static final String HTTP2 = "http2";
    public static final String NEED = "NEED";
    public static final String WANT = "WANT";
    private static final String HTTP_11 = "http/1.1";
    private static final String HTTP_2 = "http/2";

    public static ServerConfiguration createServerConfiguration(Map<String, Object> serverProperties, SpringBootConfigFactory configFactory, Function<String, URL> urlGetter) {
        ServerConfiguration sc = new ServerConfiguration();
        Boolean useDefaultHost = (Boolean) serverProperties.get(LIBERTY_USE_DEFAULT_HOST);
        if (useDefaultHost != null && useDefaultHost) {
            // going to use the default host; return empty config
            return sc;
        }
        Integer port = (Integer) serverProperties.get(PORT);
        if (port == null) {
            throw new IllegalArgumentException("No port specified.");
        }
        configureVirtualHost(sc, port);
        configureSSL(sc, port, serverProperties, configFactory, urlGetter);
        configureHttpEndpoint(sc, port, serverProperties);
        return sc;
    }

    public static void checkSpringBootVersion(String min, String max, String actual) {
        VersionRange range = null;
        Version vActual = null;
        try {
            vActual = Version.valueOf(actual);
            if (max == null) {
                range = new VersionRange(min);
            } else {
                range = new VersionRange('[' + min + ',' + max + ')');
            }
        } catch (IllegalArgumentException e) {
            // version parsing issues; auto-FFDC here
        }
        if (!range.includes(vActual)) {
            throw new ApplicationError(Type.ERROR_UNSUPPORTED_SPRING_BOOT_VERSION, actual, range.toString());
        }

    }

    private static void configureVirtualHost(ServerConfiguration sc, Integer port) {
        List<VirtualHost> virtualHosts = sc.getVirtualHosts();
        virtualHosts.clear();
        VirtualHost virtualHost = new VirtualHost();
        virtualHost.setId(ID_VIRTUAL_HOST + port);
        Set<String> aliases = virtualHost.getHostAliases();
        aliases.clear();
        aliases.add("*:" + port);
        virtualHosts.add(virtualHost);
    }

    private static void configureHttpEndpoint(ServerConfiguration sc, Integer port, Map<String, Object> serverProperties) {
        List<HttpEndpoint> endpoints = sc.getHttpEndpoints();
        endpoints.clear();
        HttpEndpoint endpoint = new HttpEndpoint();
        endpoints.add(endpoint);
        endpoint.setId(ID_HTTP_ENDPOINT + port);

        String address = (String) serverProperties.get(ADDRESS);
        if (address != null) {
            endpoint.setHost(address);
        } else {
            endpoint.setHost("*");
        }

        if (sc.getSsls().isEmpty()) {
            endpoint.setHttpPort(port);
            endpoint.setHttpsPort(-1);
        } else {
            endpoint.setHttpPort(-1);
            endpoint.setHttpsPort(port);
            endpoint.getSslOptions().setSslRef(ID_SSL + port);
        }

        String serverHeader = (String) serverProperties.get(SERVER_HEADER);
        if (serverHeader != null) {
            endpoint.getHttpOptions().setServerHeaderValue(serverHeader);
        }

        Boolean isHttp2Enabled = (Boolean) serverProperties.get(HTTP2);
        if (isHttp2Enabled != null) {
            if (isHttp2Enabled) {
                endpoint.setProtocolVersion(HTTP_2);
            } else {
                endpoint.setProtocolVersion(HTTP_11);
            }
        }
    }

    private static void configureSSL(ServerConfiguration sc, Integer port, Map<String, Object> serverProperties, SpringBootConfigFactory configFactory,
                                     Function<String, URL> urlGetter) {
        Boolean enabled = (Boolean) serverProperties.get(SSL_ENABLED);
        if (enabled != null && !enabled) {
            return;
        }
        String keyStore = (String) serverProperties.get(SSL_KEY_STORE);
        if (keyStore == null) {
            return;
        }
        ConfigElementList<SSLConfig> ssls = sc.getSsls();
        ssls.clear();

        SSLConfig sslConfig = new SSLConfig();
        sslConfig.setId(ID_SSL + port);
        sslConfig.setKeyStoreRef(ID_KEY_STORE + port);
        ssls.add(sslConfig);

        String protocol = (String) serverProperties.get(SSL_PROTOCOL);
        if (protocol != null) {
            sslConfig.setSslProtocol(protocol);
        }

        String[] ciphers = (String[]) serverProperties.get(SSL_CIPHERS);
        if (ciphers != null && ciphers.length > 0) {
            StringBuilder enabledCiphers = new StringBuilder();
            for (String cipher : ciphers) {
                enabledCiphers.append(cipher).append(" ");
            }
            sslConfig.setEnabledCiphers(enabledCiphers.toString());
        }

        ConfigElementList<KeyStore> keyStores = sc.getKeyStores();
        keyStores.clear();

        configureKeyStore(keyStores, port, serverProperties, configFactory, urlGetter);

        String trustStore = (String) serverProperties.get(SSL_TRUST_STORE);
        if (trustStore != null) {
            sslConfig.setTrustStoreRef(ID_TRUST_STORE + port);
            configureTrustStore(keyStores, port, serverProperties, configFactory, urlGetter);
        }

        String clientAuth = (String) serverProperties.get(SSL_CLIENT_AUTH);
        if (clientAuth != null) {
            if (NEED.equals(clientAuth)) {
                sslConfig.setClientAuthentication(true);
            } else if (WANT.equals(clientAuth)) {
                sslConfig.setClientAuthenticationSupported(true);
            }
        }
    }

    private static void configureKeyStore(ConfigElementList<KeyStore> keyStores, Integer port, Map<String, Object> serverProperties, SpringBootConfigFactory configFactory,
                                          Function<String, URL> urlGetter) {
        KeyStore keyStore = new KeyStore();
        keyStores.add(keyStore);
        keyStore.setId(ID_KEY_STORE + port);

        URL keyStoreURL = urlGetter.apply((String) serverProperties.get(SSL_KEY_STORE));

        String keyStoreURLString = keyStoreURL.toString();
        String keyStoreName = keyStoreURLString.substring(keyStoreURLString.lastIndexOf("/") + 1);
        int dot = keyStoreName.lastIndexOf(".");
        keyStoreName = keyStoreName.substring(0, dot) + "-" + port + keyStoreName.substring(dot);

        File securityDir = new File(configFactory.getServerDir(), SECURITY_DIR);

        File keyStoreFile = new File(securityDir, keyStoreName);

        try (InputStream in = keyStoreURL.openStream()) {
            writeFile(in, keyStoreFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy keystore to server home/resources/security directory.", e);
        }
        keyStore.setLocation(keyStoreName);
        String ksPass = (String) serverProperties.get(SSL_KEY_STORE_PASSWORD);
        if (ksPass != null) {
            keyStore.setPassword(ksPass);
        }
        String ksType = (String) serverProperties.get(SSL_KEY_STORE_TYPE);
        if (ksType != null) {
            keyStore.setType(ksType);
        }

        String provider = (String) serverProperties.get(SSL_KEY_STORE_PROVIDER);
        if (provider != null) {
            keyStore.setExtraAttribute("provider", provider);
        }

        String keyAlias = (String) serverProperties.get(SSL_KEY_ALIAS);
        String keyPass = (String) serverProperties.get(SSL_KEY_PASSWORD);
        if (keyAlias != null || keyPass != null) {
            ConfigElementList<KeyEntry> keyEntries = keyStore.getKeyEntries();
            keyEntries.clear();
            KeyEntry keyEntry = new KeyEntry();
            keyEntries.add(keyEntry);

            if (keyAlias != null) {
                keyEntry.setName(keyAlias);
            } else {
                keyEntry.setName("keyEntry");
            }
            if (keyPass != null) {
                keyEntry.setKeyPassword(keyPass);
            }
            keyEntries.add(keyEntry);
        }
    }

    private static void configureTrustStore(ConfigElementList<KeyStore> keyStores, Integer port, Map<String, Object> serverProperties, SpringBootConfigFactory configFactory,
                                            Function<String, URL> urlGetter) {
        KeyStore keyStore = new KeyStore();
        keyStores.add(keyStore);
        keyStore.setId(ID_TRUST_STORE + port);

        URL trustStoreURL = urlGetter.apply((String) serverProperties.get(SSL_TRUST_STORE));;

        String trustStoreURLString = trustStoreURL.toString();
        String trustStoreName = trustStoreURLString.substring(trustStoreURLString.lastIndexOf("/") + 1);
        int dot = trustStoreName.lastIndexOf(".");
        trustStoreName = trustStoreName.substring(0, dot) + "-" + port + trustStoreName.substring(dot);

        File trustStoreDir = new File(configFactory.getServerDir(), SECURITY_DIR);
        File trustStoreFile = new File(trustStoreDir, trustStoreName);

        try (InputStream in = trustStoreURL.openStream()) {
            writeFile(in, trustStoreFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy truststore to server home/resources/security directory.", e);
        }

        keyStore.setLocation(trustStoreName);
        String tsPass = (String) serverProperties.get(SSL_TRUST_STORE_PASSWORD);
        if (tsPass != null) {
            keyStore.setPassword(tsPass);
        }
        String tsType = (String) serverProperties.get(SSL_TRUST_STORE_TYPE);
        if (tsType != null) {
            keyStore.setType(tsType);
        }

        String provider = (String) serverProperties.get(SSL_TRUST_STORE_PROVIDER);
        if (provider != null) {
            keyStore.setExtraAttribute("provider", provider);
        }
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
