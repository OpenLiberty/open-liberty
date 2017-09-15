/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import test.common.SharedOutputManager;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
@SuppressWarnings("unchecked")
public class KeystoreConfigTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final BundleContext bc = mock.mock(BundleContext.class);
    private final ServiceRegistration<KeystoreConfig> registration = mock.mock(ServiceRegistration.class);
    private final AtomicServiceReference<WsLocationAdmin> locSrvRef = mock.mock(AtomicServiceReference.class);
    private final WsLocationAdmin locSrv = mock.mock(WsLocationAdmin.class);
    private final Dictionary<String, Object> props = new Hashtable<String, Object>();
    private KeystoreConfig ksConfig;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(locSrvRef).getServiceWithException();
                will(returnValue(locSrv));
            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#getPid()}.
     */
    @Test
    public void getPid() {
        String pid = "myPid";
        ksConfig = new KeystoreConfig(pid, null, null);
        assertEquals("Did not get back expected pid",
                     pid, ksConfig.getPid());
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#getId()}.
     */
    @Test
    public void getId() {
        String id = "myPid";
        ksConfig = new KeystoreConfig(null, id, null);
        assertEquals("Did not get back expected id",
                     id, ksConfig.getId());
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#getKeyStore()}.
     */
    @Test
    public void getKeyStore_notRegistered() {
        ksConfig = new KeystoreConfig(null, null, null);
        assertNull("WSKeyStore was not null when not registered",
                   ksConfig.getPid());
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#resolveString(java.lang.String)}.
     */
    @Test
    public void resolveString() {
        final String unresolvedString = "unresolvedString";
        final String resolvedString = "resolvedString";
        mock.checking(new Expectations() {
            {
                one(locSrv).resolveString(unresolvedString);
                will(returnValue(resolvedString));
            }
        });
        ksConfig = new KeystoreConfig(null, null, locSrvRef);
        assertEquals("Did not get back expected id",
                     resolvedString, ksConfig.resolveString(unresolvedString));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#getServerName()}.
     */
    @Test
    public void getServerName() {
        final String serverName = "myServer";
        mock.checking(new Expectations() {
            {
                one(locSrv).getServerName();
                will(returnValue(serverName));
            }
        });
        ksConfig = new KeystoreConfig(null, null, locSrvRef);
        assertEquals("Did not get back expected id",
                     serverName, ksConfig.getServerName());
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#updateKeystoreConfig(java.util.Dictionary)}.
     */
    @Ignore("This test requires file system access and needs to be properly doubled")
    @Test
    public void updateKeystoreConfig_nullLocation() {
        ksConfig = new KeystoreConfig("myPid", LibertyConstants.DEFAULT_KEYSTORE_REF_ID, locSrvRef);

        props.put("id", LibertyConstants.DEFAULT_KEYSTORE_REF_ID);
        props.put("password", "Liberty");

        mock.checking(new Expectations() {
            {
                one(locSrv).resolveString(LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE);
                will(returnValue("/key.jks"));
            }
        });
        assertTrue("Valid configuration should return true",
                   ksConfig.updateKeystoreConfig(props));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#updateKeystoreConfig(java.util.Dictionary)}.
     */
    @Test
    public void updateKeystoreConfig_badConfig() {
        ksConfig = new KeystoreConfig(null, null, locSrvRef);

        props.put("id", LibertyConstants.DEFAULT_KEYSTORE_REF_ID);
        props.put(LibertyConstants.KEY_KEYSTORE_LOCATION, "someBadLoc");

        mock.checking(new Expectations() {
            {
                one(locSrv).resolveString("someBadLoc");
                will(returnValue("key.jks"));
            }
        });

        assertFalse("Bad configurations should result in false",
                    ksConfig.updateKeystoreConfig(props));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#updateKeystoreConfig(java.util.Dictionary)}.
     */
    @Ignore("This test requires file system access and needs to be properly doubled")
    @Test
    public void updateKeystoreConfig_goodConfig() {
        ksConfig = new KeystoreConfig("myPid", LibertyConstants.DEFAULT_KEYSTORE_REF_ID, locSrvRef);

        props.put("id", LibertyConstants.DEFAULT_KEYSTORE_REF_ID);
        props.put(LibertyConstants.KEY_KEYSTORE_LOCATION, "alternateKey.jks");
        props.put("type", "JKS");
        props.put("password", "Liberty");

        mock.checking(new Expectations() {
            {
                one(locSrv).resolveString(LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE);
                will(returnValue("key.jks"));
                one(locSrv).resolveString(LibertyConstants.DEFAULT_CONFIG_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE);
                will(returnValue("key.jks"));
            }
        });
        assertTrue("Valid configuration should return true",
                   ksConfig.updateKeystoreConfig(props));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#updateRegistration(org.osgi.framework.BundleContext)}.
     */
    @Test
    public void updateRegistration_notRegistered() {
        ksConfig = new KeystoreConfig(null, null, locSrvRef);

        mock.checking(new Expectations() {
            {
                one(bc).registerService(KeystoreConfig.class, ksConfig, null);
            }
        });

        ksConfig.updateRegistration(bc);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#updateRegistration(org.osgi.framework.BundleContext)}.
     */
    @Test
    public void updateRegistration_registered() {
        ksConfig = new KeystoreConfig(null, null, locSrvRef);

        mock.checking(new Expectations() {
            {
                one(bc).registerService(KeystoreConfig.class, ksConfig, null);
                will(returnValue(registration));
            }
        });

        ksConfig.updateRegistration(bc);

        mock.checking(new Expectations() {
            {
                one(registration).setProperties(null);
            }
        });
        ksConfig.updateRegistration(bc);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#updateRegistration(org.osgi.framework.BundleContext)}.
     */
    @Test
    public void updateRegistration_registeredWithProps() {
        ksConfig = new KeystoreConfig(null, null, locSrvRef);

        ksConfig.updateKeystoreConfig(props);

        mock.checking(new Expectations() {
            {
                one(bc).registerService(KeystoreConfig.class, ksConfig, props);
                will(returnValue(registration));
            }
        });

        ksConfig.updateRegistration(bc);

        mock.checking(new Expectations() {
            {
                one(registration).setProperties(props);
            }
        });
        ksConfig.updateRegistration(bc);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#unregister()}.
     */
    @Test
    public void unregister_notRegistered() {
        ksConfig = new KeystoreConfig(null, null, locSrvRef);
        ksConfig.unregister();
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeystoreConfig#unregister()}.
     */
    @Test
    public void unregister_registered() {
        ksConfig = new KeystoreConfig(null, null, locSrvRef);

        mock.checking(new Expectations() {
            {
                one(bc).registerService(KeystoreConfig.class, ksConfig, null);
                will(returnValue(registration));
            }
        });

        ksConfig.updateRegistration(bc);

        mock.checking(new Expectations() {
            {
                one(registration).unregister();
            }
        });
        ksConfig.unregister();
    }
}
