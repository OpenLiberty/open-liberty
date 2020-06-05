/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.ssl.internal.KeystoreConfig;
import com.ibm.ws.ssl.internal.LibertyConstants;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import test.common.SharedOutputManager;

/**
 *
 */
public class WSKeyStoreTest {
    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final WsLocationAdmin locMgr = mock.mock(WsLocationAdmin.class);

    KeystoreConfig testConfigService = new KeystoreConfig(null, null, null) {

        /** {@inheritDoc} */
        @Override
        public String getServerName() {
            return "dummy";
        }

        /** {@inheritDoc} */
        @Override
        public String resolveString(String path) {
            return locMgr.resolveString(path);
        }

    };

    // Test double that always throws java.io.Exception to test error handling and messages.
    @SuppressWarnings("serial")
    class WSKeyStoreTestDouble extends WSKeyStore {

        public WSKeyStoreTestDouble(String name, Dictionary<String, Object> properties, KeystoreConfig cfgSvc) throws Exception {
            super(name, properties, cfgSvc);
        }

        @Override
        protected KeyStore obtainKeyStore(String storeFile, boolean create) throws PrivilegedActionException {
            throw new PrivilegedActionException(new java.io.IOException("Errors encountered loading keyring. Keyring could not be loaded as a JCECCARACFKS or JCERACFKS keystore."));
        }
    }

    // Test double that always throws java.io.Exception to test error handling and messages.
    @SuppressWarnings("serial")
    class WSKeyStoreTestDoubleReturn extends WSKeyStore {

        public WSKeyStoreTestDoubleReturn(String name, Dictionary<String, Object> properties, KeystoreConfig cfgSvc) throws Exception {
            super(name, properties, cfgSvc);
        }

        @Override
        protected KeyStore obtainKeyStore(String storeFile, boolean create) throws PrivilegedActionException {
            KeyStore ks = null;
            try {
                ks = KeyStore.getInstance(KeyStore.getDefaultType());

                ks.load(null, null);
            } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                e.printStackTrace();
            } catch (KeyStoreException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                e.printStackTrace();
            }
            return ks;
        }
    }

    /**
     * @return
     */
    private String getJCEKSProviderIfAvailable() {
        String providerName = null;
        Provider[] jceksProviders = Security.getProviders("KeyStore.PKCS12");
        if (jceksProviders.length > 0) {
            providerName = jceksProviders[0].getName();
        }
        return providerName;
    }

    /**
     * Test to make sure all Keystore properties are set from the map
     */
    @Test
    public void createWSKeyStoreWithAllCommonProperties() throws Exception {
        Hashtable<String, Object> storeconfig = new Hashtable<String, Object>();
        storeconfig.put("id", "allPropsKeyStore");
        storeconfig.put("password", "mytestpassword");
        storeconfig.put("location", "testKey.p12");
        storeconfig.put("type", "PKCS12");
        storeconfig.put("fileBased", Boolean.TRUE);
        storeconfig.put("readOnly", Boolean.TRUE);
        storeconfig.put("initializeAtStartup", "false");

        String providerName = getJCEKSProviderIfAvailable();
        if (providerName != null) {
            storeconfig.put("provider", providerName);
        }

        final File testKeyFileP12 = new File("test/files/testKey.p12");
        final String defaultKeyStore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        final File defaultKeyStoreJKS = new File(defaultKeyStore);
        final String defaultKeyStoreName = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        final File defaultKeyStorePKCS12 = new File(defaultKeyStoreName);

        mock.checking(new Expectations() {
            {
                one(locMgr).resolveString(defaultKeyStore);
                will(returnValue(defaultKeyStoreJKS.getAbsolutePath()));
                one(locMgr).resolveString(defaultKeyStoreName);
                will(returnValue(defaultKeyStorePKCS12.getAbsolutePath()));
                one(locMgr).resolveString("testKey.p12");
                will(returnValue(testKeyFileP12.getAbsolutePath()));
                one(locMgr).resolveString(testKeyFileP12.getAbsolutePath());
                will(returnValue(testKeyFileP12.getAbsolutePath()));
            }
        });

        WSKeyStore keystore = new WSKeyStore("allPropsKeyStore", storeconfig, testConfigService);

        assertEquals("allPropsKeyStore", keystore.getProperty("com.ibm.ssl.keyStoreName"));
        assertTrue(keystore.getProperty("com.ibm.ssl.keyStore").endsWith("testKey.p12"));
        assertEquals("PKCS12", keystore.getProperty("com.ibm.ssl.keyStoreType"));
        assertEquals("true", keystore.getProperty("com.ibm.ssl.keyStoreFileBased"));
        assertEquals("true", keystore.getProperty("com.ibm.ssl.keyStoreReadOnly"));
        assertEquals("false", keystore.getProperty("com.ibm.ssl.keyStoreInitializeAtStartup"));
        if (providerName != null) {
            assertEquals(providerName, keystore.getProperty("com.ibm.ssl.keyStoreProvider"));
        }
    }

    /**
     * Test to make sure that if keystore properties cantain a location and type it is not marked
     * as a default keystore
     */
    @Test
    public void createWSKeyStoreWithMinimalProperties() throws Exception {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("id", "testKeyStore");
        props.put("password", "mytestpassword");
        props.put("location", "testKey.p12");
        props.put("type", "PKCS12");

        final File testKeyFileP12 = new File("test/files/testKey.p12");
        final String defaultKeyStore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        final File defaultKeyStoreJKS = new File(defaultKeyStore);
        final String defaultKeyStoreName = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        final File defaultKeyStorePKCS12 = new File(defaultKeyStoreName);

        mock.checking(new Expectations() {
            {
                one(locMgr).resolveString(defaultKeyStore);
                will(returnValue(defaultKeyStoreJKS.getAbsolutePath()));
                one(locMgr).resolveString(defaultKeyStoreName);
                will(returnValue(defaultKeyStorePKCS12.getAbsolutePath()));
                one(locMgr).resolveString("testKey.p12");
                will(returnValue(testKeyFileP12.getAbsolutePath()));
                one(locMgr).resolveString(testKeyFileP12.getAbsolutePath());
                will(returnValue(testKeyFileP12.getAbsolutePath()));
            }
        });

        WSKeyStore keystore = new WSKeyStore("testKeyStore", props, testConfigService);

        assertEquals("testKeyStore", keystore.getProperty("com.ibm.ssl.keyStoreName"));
        assertEquals("mytestpassword", keystore.getProperty("com.ibm.ssl.keyStorePassword"));
        assertTrue(keystore.getProperty("com.ibm.ssl.keyStore").endsWith("testKey.p12"));
        assertEquals("PKCS12", keystore.getProperty("com.ibm.ssl.keyStoreType"));
        assertEquals("false", keystore.getProperty("com.ibm.ssl.keyStoreInitializeAtStartup"));

    }

    /**
     * Test to check if the configuration suggest a liberty default keystore. The
     * type and location should be filled in. The 'isDefault' property should be
     * set to true too.
     */
    @Test
    public void createWSKeyStoreWithDefaultProperties() throws Exception {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("id", LibertyConstants.DEFAULT_KEYSTORE_REF_ID);
        props.put("password", "mytestpassword");
        // location and type will always come from metatype
        props.put("location", LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE);
        props.put("type", "PKCS12");

        final String defaultFileName = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        final File projectBuild = new File("../com.ibm.ws.ssl/build/tmp/key.p12");

        final String defaultKeyStore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        final File defaultKeyStoreJKS = new File(defaultKeyStore);

        final String defaultKeyStoreName = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        final File defaultKeyStorePKCS12 = new File(defaultKeyStoreName);

        mock.checking(new Expectations() {
            {
                // default location (containing symbol) would get an absolute file back
                // there should be no other calls to resolve string once an absolute path
                // is returned.
                one(locMgr).resolveString(defaultKeyStore);
                will(returnValue(defaultKeyStoreJKS.getAbsolutePath()));
                one(locMgr).resolveString(defaultKeyStoreName);
                will(returnValue(defaultKeyStorePKCS12.getAbsolutePath()));
                one(locMgr).resolveString(defaultFileName);
                will(returnValue(projectBuild.getAbsolutePath()));
                one(locMgr).resolveString(LibertyConstants.DEFAULT_OUTPUT_LOCATION);
                will(returnValue(LibertyConstants.DEFAULT_OUTPUT_LOCATION));
                one(locMgr).resolveString(projectBuild.getAbsolutePath());
                will(returnValue(projectBuild.getAbsolutePath()));
            }
        });

        WSKeyStore keystore = new WSKeyStore(LibertyConstants.DEFAULT_KEYSTORE_REF_ID, props, testConfigService);

        assertEquals(LibertyConstants.DEFAULT_KEYSTORE_REF_ID, keystore.getProperty("com.ibm.ssl.keyStoreName"));
        assertEquals("PKCS12", keystore.getProperty("com.ibm.ssl.keyStoreType"));
        assertTrue(keystore.getProperty("com.ibm.ssl.keyStore").endsWith("key.p12"));
        assertEquals("true", keystore.getProperty("com.ibm.ssl.keyStoreInitializeAtStartup"));
    }

    /**
     * Test to check if the keystore is not a default keystore that exception is thrown
     * if the key pieces of information are missing.
     */
    @Test
    public void missingAllKeyStoreInfoForDefaultKeyStore() throws Exception {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("id", LibertyConstants.DEFAULT_KEYSTORE_REF_ID);
        // location and type will always come from metatype
        props.put("location", LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE);
        props.put("type", "PKCS12");

        final String defaultKeyStore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        final File defaultKeyStoreJKS = new File(defaultKeyStore);
        final String defaultKeyStoreName = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        final File defaultKeyStorePKCS12 = new File(defaultKeyStoreName);

        mock.checking(new Expectations() {
            {
                one(locMgr).resolveString(defaultKeyStore);
                will(returnValue(defaultKeyStoreJKS.getAbsolutePath()));
                one(locMgr).resolveString(defaultKeyStoreName);
                will(returnValue(defaultKeyStorePKCS12.getAbsolutePath()));
            }
        });

        try {
            new WSKeyStore(LibertyConstants.DEFAULT_KEYSTORE_REF_ID, props, testConfigService);
            fail("Expecting an exception");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Required keystore information is missing, must provide a password for the default keystore");
        }
    }

    /**
     * If the configuration is not the default and its missing the location
     * information, this should result in an IllegalArgumentException.
     */
    @Test
    public void missingPasswordForDefaultKeyStore() throws Exception {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("id", LibertyConstants.DEFAULT_KEYSTORE_REF_ID);
        // location and type will always come from metatype
        props.put("location", LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE);
        props.put("type", "PKCS12");

        final String defaultKeyStore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        final File defaultKeyStoreJKS = new File(defaultKeyStore);
        final String defaultKeyStoreName = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        final File defaultKeyStorePKCS12 = new File(defaultKeyStoreName);

        mock.checking(new Expectations() {
            {
                one(locMgr).resolveString(defaultKeyStore);
                will(returnValue(defaultKeyStoreJKS.getAbsolutePath()));
                one(locMgr).resolveString(defaultKeyStoreName);
                will(returnValue(defaultKeyStorePKCS12.getAbsolutePath()));
            }
        });

        try {
            new WSKeyStore(LibertyConstants.DEFAULT_KEYSTORE_REF_ID, props, testConfigService);
            fail("Expecting an IllegalArgumentException when the location is not defined");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Required keystore information is missing, must provide a password for the default keystore");
        }
    }

    /**
     * If the configuration is not the default and its missing the location
     * information, this should result in an IllegalArgumentException.
     */
    @Test
    public void missingLocation() throws Exception {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("id", "testKeyStore");
        props.put("type", "PKCS12");
        props.put("password", "mytestpassword");

        final String defaultKeyStore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        final File defaultKeyStoreJKS = new File(defaultKeyStore);
        final String defaultKeyStoreName = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        final File defaultKeyStorePKCS12 = new File(defaultKeyStoreName);

        mock.checking(new Expectations() {
            {
                one(locMgr).resolveString(defaultKeyStore);
                will(returnValue(defaultKeyStoreJKS.getAbsolutePath()));
                one(locMgr).resolveString(defaultKeyStoreName);
                will(returnValue(defaultKeyStorePKCS12.getAbsolutePath()));
            }
        });

        try {
            new WSKeyStore("testKeyStore", props, testConfigService);
            fail("Expecting an IllegalArgumentException when the location is not defined");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected warning about missing keystore information",
                       outputMgr.checkForStandardErr("CWPKI0806E"));
            assertEquals(e.getMessage(), "Required keystore information is missing, must provide a location and type.");
        }
    }

    /**
     * If the configuration is not the default and its missing the type
     * information, this should result in an IllegalArgumentException.
     */
// Type is never going to be missing its set to pkcs12 by default in the
//    @Test
    public void missingType() throws Exception {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("id", "testKeyStore");
        props.put("location", "key.p12");
        props.put("password", "mytestpassword");

        final String defaultKeyStore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        final File defaultKeyStoreJKS = new File(defaultKeyStore);

        mock.checking(new Expectations() {
            {
                one(locMgr).resolveString(defaultKeyStore);
                will(returnValue(defaultKeyStoreJKS.getAbsolutePath()));
            }
        });

        try {
            new WSKeyStore("testKeyStore", props, testConfigService);
            fail("Expecting an IllegalArgumentException when the location is not defined");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected warning about missing keystore information",
                       outputMgr.checkForStandardErr("CWPKI0806E"));
            assertEquals(e.getMessage(), "Required keystore information is missing, must provide a location and type.");
        }
    }

    /**
     * Test to make sure the file based defaults to true does not change
     * when useing a type other then jks, jceks, or pkcs12
     */
    @Test
    public void createWSKeyStoreFileBasedSpecialType() throws Exception {
        Hashtable<String, Object> storeconfig = new Hashtable<String, Object>();
        storeconfig.put("id", "diffTypeKeyStore");
        storeconfig.put("password", "mytestpassword");
        storeconfig.put("location", "testKeyStoreFile");
        storeconfig.put("type", "different");

        String providerName = getJCEKSProviderIfAvailable();
        if (providerName != null) {
            storeconfig.put("provider", providerName);
        }

        final File testKeyFile = new File("test/files/testKeyStoreFile");
        final String defaultKeyStore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        final File defaultKeyStoreJKS = new File(defaultKeyStore);
        final String defaultKeyStoreName = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        final File defaultKeyStorePKCS12 = new File(defaultKeyStoreName);

        mock.checking(new Expectations() {
            {
                one(locMgr).resolveString(defaultKeyStore);
                will(returnValue(defaultKeyStoreJKS.getAbsolutePath()));
                one(locMgr).resolveString(defaultKeyStoreName);
                will(returnValue(defaultKeyStorePKCS12.getAbsolutePath()));
                one(locMgr).resolveString("testKeyStoreFile");
                will(returnValue(testKeyFile.getAbsolutePath()));
            }
        });

        WSKeyStore keystore = new WSKeyStoreTestDoubleReturn("diffTypeKeyStore", storeconfig, testConfigService);

        assertEquals("diffTypeKeyStore", keystore.getProperty("com.ibm.ssl.keyStoreName"));
        assertTrue(keystore.getProperty("com.ibm.ssl.keyStore").endsWith("testKeyStoreFile"));
        assertEquals("different", keystore.getProperty("com.ibm.ssl.keyStoreType"));
        assertEquals("true", keystore.getProperty("com.ibm.ssl.keyStoreFileBased"));
        assertEquals("false", keystore.getProperty("com.ibm.ssl.keyStoreReadOnly"));
        assertEquals("false", keystore.getProperty("com.ibm.ssl.keyStoreInitializeAtStartup"));
        if (providerName != null) {
            assertEquals(providerName, keystore.getProperty("com.ibm.ssl.keyStoreProvider"));
        }
    }

    /**
     * Test to make sure the a keyring's filebased attribute get set to
     * false when not set in configuration.
     */
    @Test
    public void createWSKeyStoreFileBasedFalseForKeyring() throws Exception {
        Hashtable<String, Object> storeconfig = new Hashtable<String, Object>();
        storeconfig.put("id", "keyring");
        storeconfig.put("password", "mytestpassword");
        storeconfig.put("location", "safkeyring:///doesNotExist");
        storeconfig.put("type", "JCERACFKS");

        String providerName = getJCEKSProviderIfAvailable();
        if (providerName != null) {
            storeconfig.put("provider", providerName);
        }

        final File testKeyFile = new File("safkeyring:///doesNotExist");
        final String defaultKeyStore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        final File defaultKeyStoreJKS = new File(defaultKeyStore);
        final String defaultKeyStoreName = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        final File defaultKeyStorePKCS12 = new File(defaultKeyStoreName);

        mock.checking(new Expectations() {
            {
                one(locMgr).resolveString(defaultKeyStore);
                will(returnValue(defaultKeyStoreJKS.getAbsolutePath()));
                one(locMgr).resolveString(defaultKeyStoreName);
                will(returnValue(defaultKeyStorePKCS12.getAbsolutePath()));
                one(locMgr).resolveString("testKeyStoreFile");
                will(returnValue(testKeyFile.getAbsolutePath()));
            }
        });

        WSKeyStore keystore = new WSKeyStoreTestDoubleReturn("keyring", storeconfig, testConfigService);

        assertEquals("keyring", keystore.getProperty("com.ibm.ssl.keyStoreName"));
        assertTrue(keystore.getProperty("com.ibm.ssl.keyStore").endsWith("safkeyring:///doesNotExist"));
        assertEquals("JCERACFKS", keystore.getProperty("com.ibm.ssl.keyStoreType"));
        assertEquals("false", keystore.getProperty("com.ibm.ssl.keyStoreFileBased"));
    }

    /**
     * There must be an exception thrown when the keyring cannot be found.
     * This is now expected in Liberty for z/OS since there is just a single process.
     */
    @Test
    public void nonExistentKeyringThrowsException() throws Exception {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("id", "safKeyringThatDoesNotExist");
        props.put("password", "mytestpassword");
        props.put("location", "safkeyring:///doesNotExist");
        props.put("type", "JCERACFKS");
        props.put("fileBased", Boolean.FALSE);

        final String defaultKeyStore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        final File defaultKeyStoreJKS = new File(defaultKeyStore);
        final String defaultKeyStoreName = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        final File defaultKeyStorePKCS12 = new File(defaultKeyStoreName);

        mock.checking(new Expectations() {
            {
                one(locMgr).resolveString(defaultKeyStore);
                will(returnValue(defaultKeyStoreJKS.getAbsolutePath()));
                one(locMgr).resolveString(defaultKeyStoreName);
                will(returnValue(defaultKeyStorePKCS12.getAbsolutePath()));
            }
        });

        try {
            new WSKeyStoreTestDouble("safKeyringThatDoesNotExist", props, testConfigService);
            fail("The expected java.io.IOException was not thrown.");
        } catch (Exception e) {
            assertTrue("There must be a java.io.IOException.", e instanceof java.io.IOException);
            assertTrue("Expected error message was not logged", outputMgr.checkForStandardErr("CWPKI0033E"));
        }
    }

}
