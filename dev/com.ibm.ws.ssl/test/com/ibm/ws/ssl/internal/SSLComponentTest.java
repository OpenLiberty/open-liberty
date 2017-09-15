/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.component.ExtComponentContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

import com.ibm.websphere.ssl.Constants;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.ssl.config.WSKeyStore;
import com.ibm.ws.ssl.optional.SSLSupportOptional;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class SSLComponentTest {

    /**  */
    private static final String TEST_SSL = "testSsl";
    /**  */
    private static final String[] TEST_SSL_ARRAY = new String[] { TEST_SSL };
    /**  */
    private static final String TEST_KEYSTORE = "testKeystore";
    /**  */
    private static final String[] TEST_KEYSTORE_ARRAY = new String[] { TEST_KEYSTORE };
    private static final String[] EMPTY = new String[] {};

    private static SharedOutputManager outputMgr;
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final RepertoireConfigService repConfig = mock.mock(RepertoireConfigService.class);

    private final KeystoreConfig keyConfig = mock.mock(KeystoreConfig.class);

    private final WSKeyStore keyStore = mock.mock(WSKeyStore.class);
    private final WsLocationAdmin locSvc = mock.mock(WsLocationAdmin.class);
    private final FeatureProvisioner provisionerService = mock.mock(FeatureProvisioner.class);

    private SSLComponent sslComponent;

    private final ComponentContext componentContext = new ExtComponentContext() {

        private Dictionary<String, Object> properties;

        @Override
        public Object[] locateServices(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <S> S locateService(String name, ServiceReference<S> reference) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object locateService(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle getUsingBundle() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceReference<?> getServiceReference() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Dictionary<String, Object> getProperties() {
            return properties;
        }

        @Override
        public ComponentInstance getComponentInstance() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public BundleContext getBundleContext() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void enableComponent(String name) {
            // TODO Auto-generated method stub

        }

        @Override
        public void disableComponent(String name) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setServiceProperties(Dictionary<String, ?> properties) {
            this.properties = copyToDictionary(properties, false);

        }

        protected Dictionary<String, Object> copyToDictionary(final Dictionary<String, ?> source,
                                                              final boolean allProps) {
            Hashtable<String, Object> target = new Hashtable<String, Object>();

            if (source != null && !source.isEmpty()) {
                for (Enumeration<String> ce = source.keys(); ce.hasMoreElements();) {
                    // cast is save, because key must be a string as per the spec
                    String key = ce.nextElement();
                    if (allProps || key.charAt(0) != '.') {
                        target.put(key, source.get(key));
                    }
                }
            }

            return target;
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        sslComponent = new SSLComponent();

        mock.checking(new Expectations() {
            {
                Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(SSLSupportOptional.KEYSTORE_IDS, TEST_KEYSTORE_ARRAY);
                props.put(SSLSupportOptional.REPERTOIRE_IDS, TEST_SSL_ARRAY);
                allowing(keyConfig).getId();
                will(returnValue(TEST_KEYSTORE));
                allowing(keyConfig).getKeyStore();
                will(returnValue(keyStore));
                allowing(keyConfig).getPid();
                will(returnValue("testKeystorePID"));

                allowing(repConfig).getAlias();
                will(returnValue(TEST_SSL));
                allowing(repConfig).getPID();
                allowing(repConfig).getProperties();
                HashMap<String, Object> repProps = new HashMap<String, Object>();
                repProps.put("id", TEST_SSL);
                will(returnValue(repProps));
                allowing(repConfig).getKeyStore();
                will(returnValue(keyConfig));
                allowing(repConfig).getTrustStore();
                will(returnValue(keyConfig));
                allowing(locSvc).resolveString("${wlp.process.type}");
                will(returnValue("server"));
                Set set = new HashSet();
                allowing(provisionerService).getInstalledFeatures();
                will(returnValue(set));

            }
        });

        sslComponent.setKernelProvisioner(provisionerService);
        sslComponent.setLocMgr(locSvc);
    }

    @After
    public void tearDown() throws Exception {

        mock.assertIsSatisfied();

        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    /**
     * Test method for {@link com.ibm.websphere.ssl.osgi.GenericSSLConfig#getProperties()}.
     *
     * Make sure getGlobalProps will replace "defaultSSLConfig" with "com.ibm.ssl.defaultAlias".
     */
    @Test
    public void getGlobalProps_DefaultSSLConfigValue() throws Exception {
        final String m = "";
        try {
            // ConfigAdmin provided maps are always String, Object
            final Map<String, Object> map = new HashMap<String, Object>();
            map.put(LibertyConstants.KEY_DEFAULT_REPERTOIRE, "DefaultSSLSettings");

            // We need to activate inside the test as we have different expectations
            // on each activate call.
            sslComponent.setKeyStore(keyConfig);
            sslComponent.setRepertoire(repConfig);
            sslComponent.activate(componentContext, map);

            Map<String, Object> globalPropMap = sslComponent.getGlobalProps();
            assertNotNull("Generic config info should be non-null",
                          globalPropMap.get("com.ibm.ssl.defaultAlias"));
            assertEquals("We should get back DefaultSSLSettings",
                         "DefaultSSLSettings", globalPropMap.get("com.ibm.ssl.defaultAlias"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.websphere.ssl.osgi.GenericSSLConfig#getProperties()}.
     *
     * Make sure getGlobalProps does not return anything that does not start with com.ibm or "DefaultSSLConfig"
     */
    @Test
    public void getGlobalProps_DoNotPassSomeProps() throws Exception {
        final String m = "";
        try {
            final Map<String, Object> map = new HashMap<String, Object>();
            map.put(LibertyConstants.KEY_DEFAULT_REPERTOIRE, "DefaultSSLSettings");
            map.put("com.ibm.test", "testValue");

            sslComponent.activate(componentContext, map);
            sslComponent.setKeyStore(keyConfig);
            sslComponent.setRepertoire(repConfig);

            Map<String, Object> globalPropMap = sslComponent.getGlobalProps();
            assertNotNull("Generic config info should be non-null",
                          globalPropMap.get("com.ibm.ssl.defaultAlias"));
            assertNotNull("Generic config info should be non-null",
                          globalPropMap.get("com.ibm.test"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.websphere.ssl.osgi.GenericSSLConfig#getProperties()}.
     *
     * Make sure the default repertoire is returned when there are no properties
     */
    @Test
    public void getGlobalProps_EmptyMapReturnedWhenNoProps() throws Exception {
        final String m = "";
        try {
            final Map<String, Object> map = new HashMap<String, Object>();

            sslComponent.setRepertoire(repConfig);
            sslComponent.activate(componentContext, map);
            sslComponent.setKeyStore(keyConfig);

            Map<String, Object> globalPropMap = sslComponent.getGlobalProps();
            assertNotNull("Generic config info should be non-null",
                          globalPropMap);
            assertEquals("globalPropMap should contain default ssl config id",
                         LibertyConstants.DEFAULT_SSL_CONFIG_ID, globalPropMap.get(Constants.SSLPROP_DEFAULT_ALIAS));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.websphere.ssl.osgi.GenericSSLConfig#getProperties()}.
     *
     * Make sure getRepertoireProps returns all the properties in a Map.
     */
    @Test
    public void getRepertoireProps_ReturnPropsInMap() throws Exception {
        final String m = "";
        try {
            final Map<String, String> repProps = new HashMap<String, String>();
            repProps.put("id", "default");
            repProps.put("keystore", "defaultKeystore");
            repProps.put("truststore", "defaultTruststore");

            final RepertoireConfigService myRepConfig = mock.mock(RepertoireConfigService.class, "foo");

            mock.checking(new Expectations() {
                {
                    allowing(myRepConfig).getAlias();
                    will(returnValue("default"));
                    allowing(myRepConfig).getProperties();
                    will(returnValue(repProps));
                    allowing(myRepConfig).getKeyStore();
                    will(returnValue(keyConfig));
                    allowing(myRepConfig).getTrustStore();
                    will(returnValue(keyConfig));
                    allowing(myRepConfig).getPID();
                }
            });

            sslComponent.activate(componentContext, new HashMap<String, Object>());

            Dictionary<String, Object> props = componentContext.getProperties();
            assertTrue(Arrays.equals(EMPTY, (String[]) props.get(SSLSupportOptional.KEYSTORE_IDS)));
            assertTrue(Arrays.equals(EMPTY, (String[]) props.get(SSLSupportOptional.REPERTOIRE_IDS)));
            sslComponent.setRepertoire(repConfig);

            props = componentContext.getProperties();
            assertEquals(4, props.size());
            assertTrue(Arrays.equals(TEST_KEYSTORE_ARRAY, (String[]) props.get(SSLSupportOptional.KEYSTORE_IDS)));
            assertTrue(Arrays.equals(TEST_SSL_ARRAY, (String[]) props.get(SSLSupportOptional.REPERTOIRE_IDS)));
            assertEquals("active", props.get("SSLSupport"));

            sslComponent.setKeyStore(keyConfig);
            props = componentContext.getProperties();
            assertEquals(4, props.size());
            assertTrue(Arrays.equals(TEST_KEYSTORE_ARRAY, (String[]) props.get(SSLSupportOptional.KEYSTORE_IDS)));
            assertTrue(Arrays.equals(TEST_SSL_ARRAY, (String[]) props.get(SSLSupportOptional.REPERTOIRE_IDS)));

            sslComponent.setRepertoire(myRepConfig);
            props = componentContext.getProperties();
            assertEquals(4, props.size());
            assertTrue(Arrays.equals(TEST_KEYSTORE_ARRAY, (String[]) props.get(SSLSupportOptional.KEYSTORE_IDS)));
            assertTrue(Arrays.equals(new String[] { "default", TEST_SSL }, (String[]) props.get(SSLSupportOptional.REPERTOIRE_IDS)));

            Map<String, Map<String, Object>> repertoirePropMap = sslComponent.getRepertoireProps();

            //repertoire name default
            Map<String, Object> repertoireProps = repertoirePropMap.get("default");
            assertFalse("repertoireProps should not be emtpy",
                        repertoireProps.isEmpty());
            assertEquals("keystore attribute should be set to defaultKeystore",
                         "defaultKeystore", repertoireProps.get("keystore"));
            assertEquals("truststore attribute should be set to defaultTruststore",
                         "defaultTruststore", repertoireProps.get("truststore"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

}
