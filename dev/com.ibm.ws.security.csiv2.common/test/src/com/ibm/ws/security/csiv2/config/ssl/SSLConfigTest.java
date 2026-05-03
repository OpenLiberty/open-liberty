/*
 * Copyright 2014, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.security.csiv2.config.ssl;

import static org.junit.Assert.assertEquals;

import java.security.KeyManagementException;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CSIIOP.Confidentiality;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.EstablishTrustInTarget;
import org.omg.CSIIOP.Integrity;

import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.transport.iiop.security.config.tss.OptionsKey;

public class SSLConfigTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final String sslConfigName = "defaultSSLConfig";
//    private SSLSupport sslSupport;
    private JSSEHelper jsseHelper;
    private SSLServerSocketFactory sslServerSocketFactory;
    private SSLSocketFactory sslSocketFactory;
    private SSLContext sslContext;

    @Before
    public void setUp() throws Exception {
        //Set mock objects for each test to avoid intermittent defects if tests run in a different order.
//        sslSupport = mockery.mock(SSLSupport.class);
        jsseHelper = mockery.mock(JSSEHelper.class);
        sslServerSocketFactory = mockery.mock(SSLServerSocketFactory.class);
        sslSocketFactory = mockery.mock(SSLSocketFactory.class);
        SSLContextSpi sslContextSpi = new SSLContextSpiDouble();
        Provider provider = mockery.mock(Provider.class);
        sslContext = new SSLContextDouble(sslContextSpi, provider, "SSL");
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void createSSLServerFactory() throws Exception {
        createJSSEHelperExpectations();

        SSLConfig sslConfig = new SSLConfig(jsseHelper);
        SSLServerSocketFactory factory = sslConfig.createSSLServerFactory(sslConfigName);
        assertEquals("There must be an SSLServerSocketFactory.", sslServerSocketFactory, factory);
    }

    @Test
    public void createSSLFactory() throws Exception {
        createJSSEHelperExpectations();

        SSLConfig sslConfig = new SSLConfig(jsseHelper);
        SSLSocketFactory factory = sslConfig.createSSLFactory(sslConfigName);
        assertEquals("There must be an SSLSocketFactory.", sslSocketFactory, factory);
    }

    @Test
    public void getAssociationOptions_clientAuthenticationRequired() throws Exception {
        Properties props = new Properties();
        props.put(Constants.SSLPROP_CLIENT_AUTHENTICATION, "true");
        props.put(Constants.SSLPROP_CLIENT_AUTHENTICATION_SUPPORTED, "false");
        props.put(Constants.SSLPROP_SECURITY_LEVEL, Constants.SECURITY_LEVEL_HIGH);
        createJSSEHelperGetPropertiesExpectations(props);

        SSLConfig sslConfig = new SSLConfig(jsseHelper);

        OptionsKey options = sslConfig.getAssociationOptions(sslConfigName);
        assertEquals("There supported association options must include .", (Integrity.value | Confidentiality.value | EstablishTrustInTarget.value | EstablishTrustInClient.value),
                     options.supports);
        assertEquals("There required association options must include .", (Integrity.value | Confidentiality.value | EstablishTrustInClient.value), options.requires);
    }

    @Test
    public void getAssociationOptions_clientAuthenticationSupported() throws Exception {
        Properties props = new Properties();
        props.put(Constants.SSLPROP_CLIENT_AUTHENTICATION, "false");
        props.put(Constants.SSLPROP_CLIENT_AUTHENTICATION_SUPPORTED, "true");
        props.put(Constants.SSLPROP_SECURITY_LEVEL, Constants.SECURITY_LEVEL_HIGH);
        createJSSEHelperGetPropertiesExpectations(props);

        SSLConfig sslConfig = new SSLConfig(jsseHelper);

        OptionsKey options = sslConfig.getAssociationOptions(sslConfigName);
        assertEquals("There supported association options must include .", (Integrity.value | Confidentiality.value | EstablishTrustInTarget.value | EstablishTrustInClient.value),
                     options.supports);
        assertEquals("There required association options must include .", (Integrity.value | Confidentiality.value), options.requires);
    }

    private void createJSSEHelperExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getSSLContext(sslConfigName, null, null, false);
                will(returnValue(sslContext));
            }
        });
    }

    private void createJSSEHelperGetPropertiesExpectations(final Properties props) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getProperties(sslConfigName);
                will(returnValue(props));
            }
        });
    }

    /*
     * Test double to return the SSLServerSocketFactory and SSLSocketFactory test instances.
     * Cannot use JMock to mock SSLContext since relevant SSLContextSpi's methods are not visible.
     */
    class SSLContextDouble extends SSLContext {

        protected SSLContextDouble(SSLContextSpi contextSpi, Provider provider, String protocol) {
            super(contextSpi, provider, protocol);
        }

    }

    /*
     * Test double to return the SSLServerSocketFactory and SSLSocketFactory test instances.
     * Cannot use JMock to mock SSLContextSpi's methods that are not visible.
     */
    class SSLContextSpiDouble extends SSLContextSpi {

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            return null;
        }

        @Override
        protected SSLEngine engineCreateSSLEngine(String host, int port) {
            return null;
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            return null;
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            return null;
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            return sslServerSocketFactory;
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return sslSocketFactory;
        }

        @Override
        protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) throws KeyManagementException {}

    }

}
