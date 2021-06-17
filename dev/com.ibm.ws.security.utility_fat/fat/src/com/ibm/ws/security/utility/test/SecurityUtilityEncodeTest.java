/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.test;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

@RunWith(FATRunner.class)
public class SecurityUtilityEncodeTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("PasswordUtilityEncodeTest");
    private final static String CUSTOM_PASSWORD_ENCRYPTION_BUNDLE_NAME = "com.ibm.websphere.crypto.sample.customencryption_1.0";
    private final static String CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME = "customEncryption-1.0";
    private final static String CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_ROOT = "usr/extension/bin/tools/extensions";
    private final static String CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_PATH = CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_ROOT + "/ws-customPasswordEncryption";
    private final static String CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_NAME = "com.ibm.ws.security.utility_fat.custom_encryption_cmdline.jar";
    private final static String PROPERTY_KEY_INSTALL_DIR = "install.dir";
    private static String installDir = null;

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Log an inventory of the features directory. Contamination of this directory by other buckets can cause this bucket to fail. 
        RemoteFile rf = server.getFileFromLibertyInstallRoot("/lib/features");
        Log.info(SecurityUtilityEncodeTest.class, "setUp", "Dumping lib/features directory");
        for ( RemoteFile feature : rf.list(false) ) {
            Log.info(SecurityUtilityEncodeTest.class, "setUp", feature.toString());
        }
     
        server.installUserBundle(CUSTOM_PASSWORD_ENCRYPTION_BUNDLE_NAME);
        server.installUserFeature(CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME);
        server.installUserFeatureL10N(CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME);
        server.copyFileToLibertyInstallRoot(CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_PATH, CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_NAME);
        installDir = System.setProperty(PROPERTY_KEY_INSTALL_DIR, server.getInstallRoot());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.uninstallUserBundle(CUSTOM_PASSWORD_ENCRYPTION_BUNDLE_NAME);
        server.uninstallUserFeature(CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME);
        server.uninstallUserFeatureL10N(CUSTOM_PASSWORD_ENCRYPTION_FEATURE_NAME);
        server.deleteDirectoryFromLibertyInstallRoot(CUSTOM_PASSWORD_ENCRYPTION_EXTENSION_ROOT);
        if (installDir == null) {
            System.clearProperty(PROPERTY_KEY_INSTALL_DIR);
        } else {
            System.setProperty(PROPERTY_KEY_INSTALL_DIR, installDir);
        }
    }

    /**
     * Tests that the help text contains the custom encryption feature name
     * if the custom password encryption is installed.
     */
    @Test
    public void testCustomHelp() throws Exception {
        // load custom..
        List<String> output = SecurityUtilityScriptUtils.execute(null, Arrays.asList("help", "encode"));
        Assert.assertTrue("Help for encode should contain custom encoding feature name.",
                          SecurityUtilityScriptUtils.findMatchingLine(output, "\\s*usr:customEncryption-1.0*"));
    }

    /**
     * Tests that the listCustom option
     */
    @Test
    public void testListCustom() throws Exception {
        String expected = "\\[\\{\"name\"\\:\"custom\"\\,\"featurename\"\\:\"usr\\:customEncryption-1\\.0\"\\,\"description\"\\:\"this sample custom encryption code uses AES encryption with the predefined key\\.\"\\}\\]";

        List<String> output = SecurityUtilityScriptUtils.execute(null, Arrays.asList("encode", "--listCustom"));
        Assert.assertTrue("The output contains the contents of listCustom", SecurityUtilityScriptUtils.findMatchingLine(output, expected));
    }

    /**
     * Tests that the password is being encrypted by using the custom encryption.
     */
    @Test
    public void testCustomEncode() throws Exception {
        final String textToEncode = "textToEncode";
        final String encodedText = "\\{xor\\}KzonKwswGjE8MDs6";
        final String customEncodedText = "\\{custom\\}NkshbYjxhL2z1Yc5dv\\+wDg\\=\\=";

        // make sure that the default is still xor.
        List<String> output = SecurityUtilityScriptUtils.execute(null, Arrays.asList("encode", textToEncode));
        Assert.assertTrue("encode arg result", SecurityUtilityScriptUtils.findMatchingLine(output, encodedText));

        // Now try custom.
        output = SecurityUtilityScriptUtils.execute(null, Arrays.asList("encode", "--encoding=custom", textToEncode));
        Assert.assertTrue("encode arg result", SecurityUtilityScriptUtils.findMatchingLine(output, customEncodedText));
    }

    /**
     * Tests that the appropriate error is reported when the code detects the error condition.
     * Some negative tests have done by the bvt, so in here run some additional test to broaden the coverage.
     * The English locale is used for this test since the error messages might be translated.
     */
    //@Test
    public void testEncodeError() throws Exception {
        final String invalidArgument = "Error: Invalid argument --unknown.";
        final String invalidAlgorithm = "com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException";
        final String invalidAlgorithm2 = "Error: com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException";

        // check invalid argument error.
        List<String> output = SecurityUtilityScriptUtils.execute(Arrays.asList(new SecurityUtilityScriptUtils.EnvVar("JVM_ARGS", "-Duser.language=en")),
                                                                 Arrays.asList("encode", "--unknown=invalid", "aaa"), true);
        Assert.assertTrue("The invalid argument message should be reported.", SecurityUtilityScriptUtils.findMatchingLine(output, invalidArgument));

        // check invalid encoding error.
        output = SecurityUtilityScriptUtils.execute(Arrays.asList(new SecurityUtilityScriptUtils.EnvVar("JVM_ARGS", "-Duser.language=en")),
                                                    Arrays.asList("encode", "--encoding=invalid", "aaa"), true);
        if (!SecurityUtilityScriptUtils.findMatchingLine(output, invalidAlgorithm)) {
            // Some of the later JDKs added Error to the actuall error so check for that as well
            if (!SecurityUtilityScriptUtils.findMatchingLine(output, invalidAlgorithm2)) {
                Assert.fail("The UnsupportedCryptoAlgorithmException should be reported.");
            }
        }
    }
}