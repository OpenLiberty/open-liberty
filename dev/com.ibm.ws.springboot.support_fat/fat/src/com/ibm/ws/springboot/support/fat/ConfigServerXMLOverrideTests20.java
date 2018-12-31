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
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.KeyStore;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.SpringBootApplication;
import com.ibm.websphere.simplicity.config.VirtualHost;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ConfigServerXMLOverrideTests20 extends AbstractSpringTests {
    private static final String OVERRIDE_VIRTUAL_HOST = "OverrideVirtualHost";
    private static final String OVERRIDE_HTTP_ENDPOINT = "OverrideHttpEndpoint";
    private static final String OVERRIDE_SSL = "OverrideSSL";
    private static final String OVERRIDE_KEYSTORES = "OverrideKeyStores";

    private static final int REQUESTED_PORT = 8095;
    private static final int TEST_PORT = 8083;

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1", "transportSecurity-1.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        List<String> appArgs = appConfig.getApplicationArguments();
        appArgs.add("--server.port=" + REQUESTED_PORT);
        String methodName = testName.getMethodName();
        if (methodName == null) {
            return;
        }
        if (methodName.endsWith(OVERRIDE_SSL) || methodName.endsWith(OVERRIDE_KEYSTORES)) {
            appArgs.add("--server.ssl.key-store=classpath:server-keystore.jks");
            appArgs.add("--server.ssl.key-store-password=secret");
            appArgs.add("--server.ssl.key-password=secret");
            appArgs.add("--server.ssl.trust-store=classpath:server-truststore.jks");
            appArgs.add("--server.ssl.trust-store-password=secret");
        }
    }

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {
        String methodName = testName.getMethodName();
        if (methodName == null) {
            return;
        }
        List<VirtualHost> virtualHosts = config.getVirtualHosts();
        virtualHosts.clear();
        List<HttpEndpoint> endpoints = config.getHttpEndpoints();
        endpoints.clear();
        List<SSL> ssls = config.getSsls();
        ssls.clear();
        List<KeyStore> keystores = config.getKeyStores();
        keystores.clear();

        if (methodName.endsWith(OVERRIDE_VIRTUAL_HOST)) {
            VirtualHost virtualHost = new VirtualHost();
            virtualHosts.add(virtualHost);
            virtualHost.setId(ID_VIRTUAL_HOST + REQUESTED_PORT);
            virtualHost.getHostAliases().add("*:" + TEST_PORT);

            HttpEndpoint endpoint = new HttpEndpoint();
            endpoints.add(endpoint);
            endpoint.setHttpPort(Integer.toString(TEST_PORT));
        }

        else if (methodName.endsWith(OVERRIDE_HTTP_ENDPOINT)) {

            // the test overrides the http endpoint, but to verify we also
            // configure that endpoint to use ssl
            HttpEndpoint endpoint = new HttpEndpoint();
            endpoints.add(endpoint);
            endpoint.setId(ID_HTTP_ENDPOINT + REQUESTED_PORT);
            // configure SSL for this test
            endpoint.setHttpsPort(Integer.toString(REQUESTED_PORT));
            endpoint.getSslOptions().setSslRef("ssl-test");

            SSL ssl = new SSL();
            ssls.add(ssl);
            ssl.setId("ssl-test");
            ssl.setKeyStoreRef("keystore-test");
            ssl.setTrustStoreRef("truststore-test");

            KeyStore keyStore = new KeyStore();
            keystores.add(keyStore);
            keyStore.setId("keystore-test");
            keyStore.setLocation("override-keystore.jks");
            keyStore.setPassword("secret");

            KeyStore trustStore = new KeyStore();
            keystores.add(trustStore);
            trustStore.setId("truststore-test");
            trustStore.setLocation("override-truststore.jks");
            trustStore.setPassword("secret");
        }

        else if (methodName.endsWith(OVERRIDE_SSL)) {
            SSL ssl = new SSL();
            ssls.add(ssl);
            ssl.setId(ID_SSL + REQUESTED_PORT);
            ssl.setKeyStoreRef("keystore-test");
            ssl.setTrustStoreRef("truststore-test");

            KeyStore keyStore = new KeyStore();
            keystores.add(keyStore);
            keyStore.setId("keystore-test");
            keyStore.setLocation("override-keystore.jks");
            keyStore.setPassword("secret");

            KeyStore trustStore = new KeyStore();
            keystores.add(trustStore);
            trustStore.setId("truststore-test");
            trustStore.setLocation("override-truststore.jks");
            trustStore.setPassword("secret");
        }

        else if (methodName.endsWith(OVERRIDE_KEYSTORES)) {
            KeyStore keyStore = new KeyStore();
            keystores.add(keyStore);
            keyStore.setId(ID_KEY_STORE + REQUESTED_PORT);
            keyStore.setLocation("override-keystore.jks");
            keyStore.setPassword("secret");

            KeyStore trustStore = new KeyStore();
            keystores.add(trustStore);
            trustStore.setId(ID_TRUST_STORE + REQUESTED_PORT);
            trustStore.setLocation("override-truststore.jks");
            trustStore.setPassword("secret");
        }
    }

    @Override
    public String getLogMethodName() {
        return "-" + testName.getMethodName();
    }

    @After
    public void stopOverrideServer() throws Exception {
        super.stopServer();
    }

    @Test
    public void configServerXMLOverrideVirtualHost() throws Exception {
        server.setHttpDefaultPort(TEST_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }

    @Test
    public void configServerXMLOverrideHttpEndpoint() throws Exception {
        doSSLRequest();
    }

    @Test
    public void configureServerXMLOverrideSSL() throws Exception {
        doSSLRequest();
    }

    @Test
    public void configureServerXMLOverrideKeyStores() throws Exception {
        doSSLRequest();
    }

    private void doSSLRequest() throws Exception {
        server.setHttpDefaultSecurePort(REQUESTED_PORT);
        String result = SSLCommonTests.sendHttpsGet("/", server);
        assertNotNull(result);
        assertEquals("Expected response not found.", "HELLO SPRING BOOT!!", result);
    }
}
