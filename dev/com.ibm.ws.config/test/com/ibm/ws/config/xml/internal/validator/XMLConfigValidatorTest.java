/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal.validator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.config.ConfigValidationException;
import com.ibm.ws.config.xml.internal.ConfigVariableRegistry;
import com.ibm.ws.config.xml.internal.ServerConfiguration;
import com.ibm.ws.config.xml.internal.XMLConfigParser;
import com.ibm.ws.kernel.service.location.internal.VariableRegistryHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

/**
 * <code>EmbeddedXMLConfigValidator</code> unit tester.
 */
public class XMLConfigValidatorTest {
    final static String CONFIG_ROOT = "${server.config.dir}/server.xml";

    /**
     * Name of directory within RTC project that contains test files.
     */
    private static final String S_VALIDATOR_TEST_DIR = "test-resources" + File.separator + "test_xml_validator";

    static WsLocationAdmin libertyLocation;
    static XMLConfigParser configParser;
    static SharedOutputManager outputMgr;

    /**
     * Path name of "servers" directory within the xml validator test files
     */
    private static String testRootDir = null;

    /**
     * Indicates whether the current test completed.
     */
    private boolean testComplete = false;

    /**
     * Does one time initialization before an instance of this test class is
     * created.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        // Gets the path name of the root directory for this RTC project
        System.out.println("enter setUpBeforeClass");
        String curDir = System.getProperty("user.dir");
        System.out.println("curDir = " + curDir);

        // Gets the path name of the root directory that contains ConfigSigner
        // test files
        testRootDir = curDir + File.separator + S_VALIDATOR_TEST_DIR;
        System.out.println("testRootDir = " + testRootDir);

        // Invoke "copyMessageStream" (uncomment next statement) to get setup
        // debugging info
//        outputMgr.copyMessageStream();
        outputMgr.resetStreams();

    }

    /**
     * Clean up that is invoked after all tests have run.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();

        // Restore back to old kernel and let next test case set to new kernel
        // as needed
        SharedLocationManager.resetWsLocationAdmin();

        // Resets the config validator factory to use the default config validator
        XMLConfigValidatorFactory.resetTestEnvironment(false);
    }

    /**
     * Set up that is performed before each test method is invoked.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Before
    public void setUp() throws Exception {
        testComplete = false;
    }

    /**
     * Clean up that is performed after each test method is invoked.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @After
    public void tearDown() throws Exception {

        // Writes out the System.out and System.err messages if the last test
        // failed
        if (!testComplete)
            outputMgr.copyMessageStream();

        // Clear the output generated after each method invocation, this keeps
        // things sane
        outputMgr.resetStreams();
    }

    /**
     * Changes the location of the directory containing the active
     * <code>server.xml</code> document.
     *
     * @param profileName The name of the directory (within the
     *                        <code>test_xml_validator/usr/servers</code> directory) that contains
     *                        the active <code>server.xml</code> file.
     */
    private void changeLocationSettings(String profileName) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        SharedLocationManager.createDefaultLocations(testRootDir, profileName, map);
        System.out.println("map = " + map);

        libertyLocation = (WsLocationAdmin) SharedLocationManager.getLocationInstance();

        configParser = new XMLConfigParser(libertyLocation, new ConfigVariableRegistry(new VariableRegistryHelper(), new String[0], null));
    }

    /**
     * Tests the default configuration validator
     * (<code>DefaultXMLConfigValidator</code>). (This validator does nothing
     * and is used in the non-embedded environment.)
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testDefaultConfigValidator() throws Exception {

        // Use default config validator
        XMLConfigValidatorFactory.resetTestEnvironment(false);

        changeLocationSettings("singleton");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration serverConfig = configParser.parseServerConfiguration(resource);

        assertNotNull("Failed to parse default server configuration", serverConfig);

        // Ensure "Config validator class in use" msg isn't issued
        assertTrue(!outputMgr.checkForStandardOut("CWWKG0043I"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and no signature in config document.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testNoSignature() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("singleton");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        try {
            configParser.parseServerConfiguration(resource);
        } catch (ConfigValidationException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Configuration parsing encountered an invalid document"));
        }

        // "Config validator class in use" msg
        assertTrue(outputMgr.checkForMessages("CWWKG0043I"));

        // "Config doc does not contain a signature" msg
        assertTrue(outputMgr.checkForStandardErr("CWWKG0048E"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a valid
     * signature.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testGoodSignature() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("goodSignature");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        assertNotNull(configParser.parseServerConfiguration(resource));

        // "Valid signature" msg
        assertTrue(outputMgr.checkForMessages("CWWKG0055I"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a valid signature
     * in which variables have been updated after signing.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testVariablesUpdated() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("variablesUpdated");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        assertNotNull(configParser.parseServerConfiguration(resource));

        // "Valid signature" msg
        assertTrue(outputMgr.checkForMessages("CWWKG0055I"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a valid signature
     * in which white space has been added to a protected section of the
     * document.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testAddedWhiteSpace() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("addedWhiteSpace");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        assertNotNull(configParser.parseServerConfiguration(resource));

        // "Valid signature" msg
        assertTrue(outputMgr.checkForMessages("CWWKG0055I"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with an (originally)
     * valid signature in which a protected section of the document has been
     * modified after signing.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testProtectedSectionModified() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("protectedSectionModified");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        try {
            configParser.parseServerConfiguration(resource);
        } catch (ConfigValidationException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Configuration parsing encountered an invalid document"));
        }

        // "Protected section modified" msg
        assertTrue(outputMgr.checkForStandardErr("CWWKG0053E"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a valid signature
     * that contains includes to other documents (all of whom have valid
     * signatures).
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testGoodSignatureWithIncludes() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("goodSignatureWithIncludes");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        assertNotNull(configParser.parseServerConfiguration(resource));

        // "Valid signature" msg
        assertTrue(outputMgr.checkForMessages("CWWKG0055I"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a valid signature
     * that contains includes to other documents (one of which does not contain
     * a valid signature).
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testIncludesWithBadSignature() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("includesWithBadSignature");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        try {
            configParser.parseServerConfiguration(resource);
        } catch (ConfigValidationException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Configuration parsing encountered an invalid document"));
        }

        // "Valid signature" msg (some config docs are valid)
        assertTrue(outputMgr.checkForMessages("CWWKG0055I"));

        // "No signature" msg (for some includes)
        assertTrue(outputMgr.checkForStandardErr("CWWKG0048E"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a valid signature
     * that contains a certificate that was NOT issued by the Liberty organization.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testUnauthorizedSignature() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("unauthorizedSignature");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        try {
            configParser.parseServerConfiguration(resource);
        } catch (ConfigValidationException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Configuration parsing encountered an invalid document"));
        }

        // "Unauthorized signature" msg
        assertTrue(outputMgr.checkForStandardErr("CWWKG0050E"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a valid
     * signature but no applicationMonitor element.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testGoodSignatureWithOutApplicationMonitor() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("goodSignatureWithOutApplicationMonitor");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration configuration = configParser.parseServerConfiguration(resource);
        assertNotNull(configuration);

        try {
            configParser.getConfigValidator().validateConfig(configuration);
        } catch (ConfigValidationException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Drop-ins enabled in embedded environment"));
        }

        // "Valid signature" msg
        assertTrue(outputMgr.checkForMessages("CWWKG0055I"));

        // "Server terminated because drop-ins are enabled" msg present
        assertTrue(outputMgr.checkForStandardErr("CWWKG0056E"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a valid
     * signature and an applicationMonitor element that does not contain
     * a dropinsEnabled attribute.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testGoodSignatureWithOutDropins() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("goodSignatureWithOutDropins");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration configuration = configParser.parseServerConfiguration(resource);
        assertNotNull(configuration);

        try {
            configParser.getConfigValidator().validateConfig(configuration);
        } catch (ConfigValidationException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Drop-ins enabled in embedded environment"));
        }

        // "Valid signature" msg
        assertTrue(outputMgr.checkForMessages("CWWKG0055I"));

        // "Server terminated because drop-ins are enabled" msg present
        assertTrue(outputMgr.checkForStandardErr("CWWKG0056E"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a valid
     * signature and an applicationMonitor element that specifies
     * "dropinsEnabled=true".
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testGoodSignatureWithDropinsTrue() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("goodSignatureWithDropinsTrue");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration configuration = configParser.parseServerConfiguration(resource);
        assertNotNull(configuration);

        try {
            configParser.getConfigValidator().validateConfig(configuration);
        } catch (ConfigValidationException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Drop-ins enabled in embedded environment"));
        }

        // "Valid signature" msg
        assertTrue(outputMgr.checkForMessages("CWWKG0055I"));

        // "Server terminated because drop-ins are enabled" msg present
        assertTrue(outputMgr.checkForStandardErr("CWWKG0056E"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a valid
     * signature and an applicationMonitor element that specifies
     * "dropinsEnabled=false".
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testGoodSignatureWithDropinsFalse() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("goodSignatureWithDropinsFalse");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        ServerConfiguration configuration = configParser.parseServerConfiguration(resource);
        assertNotNull(configuration);
        configParser.getConfigValidator().validateConfig(configuration);

        // "Valid signature" msg
        assertTrue(outputMgr.checkForMessages("CWWKG0055I"));

        // "Server terminated because drop-ins are enabled" msg not present
        assertTrue(!outputMgr.checkForStandardErr("CWWKG0056E"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a signature
     * that does not contain a certificate.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testMissingCert() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("certMissing");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        try {
            configParser.parseServerConfiguration(resource);
        } catch (ConfigValidationException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Configuration parsing encountered an invalid document"));
        }

        // "An error occurred.." msg
        assertTrue(outputMgr.checkForStandardErr("CWWKG0047E"));

        testComplete = true;
    }

    /**
     * Test with embedded validator and config document with a signature that
     * cannot be unmarshalled.
     *
     * @throws Exception Thrown if an error occurs.
     */
    @Test
    public void testCannotUnmarshall() throws Exception {

        // Use embedded config validator
        XMLConfigValidatorFactory.resetTestEnvironment(true);

        changeLocationSettings("cannotUnmarshall");

        WsResource resource = libertyLocation.resolveResource(CONFIG_ROOT);

        try {
            configParser.parseServerConfiguration(resource);
        } catch (ConfigValidationException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Configuration parsing encountered an invalid document"));
        }

        // "Unable to parse" msg
        assertTrue(outputMgr.checkForStandardErr("CWWKG0046E"));

        testComplete = true;
    }

}
