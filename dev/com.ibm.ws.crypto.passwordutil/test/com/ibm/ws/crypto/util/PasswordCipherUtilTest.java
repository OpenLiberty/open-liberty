/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.wsspi.security.crypto.CustomPasswordEncryption;
import com.ibm.wsspi.security.crypto.EncryptedInfo;

/**
 * Tests for the password utility class.
 */
public class PasswordCipherUtilTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final ComponentContext cc = context.mock(ComponentContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<CustomPasswordEncryption> cper = context.mock(ServiceReference.class);
    private final CustomPasswordEncryption cpe = context.mock(CustomPasswordEncryption.class);

    private static final String KEY_PROP_INSTALL_DIR = "wlp.install.dir";
    private static final String testBuildDir = System.getProperty("test.buildDir", "generated");
    private static final String VALUE_PROP_INSTALL_DIR = testBuildDir + "/test/test_data/simple_custom_encryption";
    private static final String VALUE_PROP_INSTALL_DIR_MULTIPLE = testBuildDir + "/test/test_data/custom_encryption";
    private static final String KEY_JAVA_CLASS_PATH = "java.class.path";
    private static final String VALUE_JAVA_CLASS_PATH = "/bin/tools/ws-securityutil.jar";

    @BeforeClass
    public static void traceSetUp() {
        outputMgr.trace("*=all");
    }

    @AfterClass
    public static void traceTearDown() {
        outputMgr.trace("*=all=disabled");
    }

    /**
     * Test initializeCustomEncryption via setCustomPasswordEncryption
     * 
     * @throws
     */
    @Test
    public void testInitializeCustomEncryption() {
        final byte[] data = { 0x30, 0x31, 0x32 };
        try {
            context.checking(new Expectations() {
                {
                    one(cper).getProperty(Constants.SERVICE_ID);
                    will(returnValue(0L));
                    one(cper).getProperty(Constants.SERVICE_RANKING);
                    will(returnValue(0));
                    one(cc).locateService("customPasswordEncryption", cper);
                    will(returnValue(cpe));
                    one(cpe).decrypt(with(any(EncryptedInfo.class)));
                    will(returnValue(data));
                }
            });
            PasswordCipherUtil pcu = new PasswordCipherUtil();
            pcu.activate(cc);
            pcu.setCustomPasswordEncryption(cper);
            byte[] output = PasswordCipherUtil.decipher(data, "custom");
            assertEquals("the custom class is not invoked", output.length, 3);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception is caught." + e);
        }
    }

    /**
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws UnsupportedConfigurationException
     *             Test ListCustom method
     * 
     * @throws
     */
    @Test
    public void testListCustom() throws UnsupportedConfigurationException, ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        final String expected = "[{\"name\":\"custom\",\"featurename\":\"usr:simpleCustomEncryption-1.0\",\"description\":\"simpleCustomEncryption default resource\"}]";

        String currentDir = System.clearProperty(KEY_PROP_INSTALL_DIR);
        PasswordCipherUtil.initialize();
        if (currentDir != null) {
            System.setProperty(KEY_PROP_INSTALL_DIR, currentDir);
        } else {
            System.clearProperty(KEY_PROP_INSTALL_DIR);
        }
        assertNull("If no custom encryption, listCustom should return null", PasswordCipherUtil.listCustom());
        currentDir = System.setProperty(KEY_PROP_INSTALL_DIR, VALUE_PROP_INSTALL_DIR);
        String currentCP = System.setProperty(KEY_JAVA_CLASS_PATH, VALUE_PROP_INSTALL_DIR + VALUE_JAVA_CLASS_PATH);
        PasswordCipherUtil.initialize();
        // put the values back to the original.
        if (currentDir != null) {
            System.setProperty(KEY_PROP_INSTALL_DIR, currentDir);
        } else {
            System.clearProperty(KEY_PROP_INSTALL_DIR);
        }
        if (currentCP != null) {
            System.setProperty(KEY_JAVA_CLASS_PATH, currentCP);
        } else {
            System.clearProperty(KEY_JAVA_CLASS_PATH);
        }
        assertEquals("If there is a custom encryption, listCustom should return the correct value", expected, PasswordCipherUtil.listCustom());

        currentDir = System.setProperty(KEY_PROP_INSTALL_DIR, VALUE_PROP_INSTALL_DIR_MULTIPLE);
        currentCP = System.setProperty(KEY_JAVA_CLASS_PATH, VALUE_PROP_INSTALL_DIR + VALUE_JAVA_CLASS_PATH);
        PasswordCipherUtil.initialize();
        // put the values back to the original.
        if (currentDir != null) {
            System.setProperty(KEY_PROP_INSTALL_DIR, currentDir);
        } else {
            System.clearProperty(KEY_PROP_INSTALL_DIR);
        }
        if (currentCP != null) {
            System.setProperty(KEY_JAVA_CLASS_PATH, currentCP);
        } else {
            System.clearProperty(KEY_JAVA_CLASS_PATH);
        }
        try {
            PasswordCipherUtil.listCustom();
            fail("An UnsupportedConfigurationException should be thrown when multiple custom encryption are installed.");
        } catch (UnsupportedConfigurationException uce) {
            // expected.
        }
    }

}
