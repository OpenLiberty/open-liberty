/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.bvt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import test.common.SharedOutputManager;

public class ConfigTest {

    private static SharedOutputManager outputMgr;

    private final Tester nestedMergeRules = new Tester("nested-merge-rules");
    private final Tester attributeCopy = new Tester("attribute-copy");
    private final Tester serviceObjectClassTest = new Tester("serviceObjectClass-test");

    private class Tester {
        private final String root;

        public Tester(String contextRoot) {
            this.root = contextRoot;
        }

        public void run(String testName) {
            StringBuilder url = new StringBuilder();
            url.append('/');
            url.append(root);
            url.append('?');
            url.append("testName=");
            url.append(testName);
            test(url.toString(), testName);
        }
    }

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.resetStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    private String getPort() {
        return System.getProperty("HTTP_default", "8010");
    }

    private String read(InputStream in) throws IOException {
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    @Test
    public void testHideFromParent() {
        test("/hidden-test?testName=testHiddenChild", "testHiddenChild");
    }

    @Test
    public void testHideFromParentExtends() {
        test("/hidden-test?testName=testHiddenChildWithExtends", "testHiddenChildWithExtends");
    }

    @Test
    public void testMultipleIDsCardinalityZero() {
        nestedMergeRules.run("testMultipleIDsCardinalityZero");
    }

    @Test
    public void testNullIDsCardinalityZero() {
        nestedMergeRules.run("testNullIDsCardinalityZero");
    }

    @Test
    public void testSingleIDCardinalityZero() {
        nestedMergeRules.run("testSingleIDCardinalityZero");
    }

    @Test
    public void testMultipleIDsCardinalityMultiple() {
        nestedMergeRules.run("testMultipleIDsCardinalityMultiple");
    }

    @Test
    public void testNullIDsCardinalityMultiple() {
        nestedMergeRules.run("testNullIDsCardinalityMultiple");
    }

    @Test
    public void testSingleIDCardinalityMultiple() {
        nestedMergeRules.run("testSingleIDCardinalityMultiple");
    }

    @Test
    public void testMixedIDsCardinalityMultiple() {
        nestedMergeRules.run("testMixedIDsCardinalityMultiple");
    }

    @Test
    public void testMultipleParentsNullIDsCardinalityZero() {
        nestedMergeRules.run("testMultipleParentsNullIDsCardinalityZero");
    }

    @Test
    public void testMultipleParentsSingleIDCardinalityZero() {
        nestedMergeRules.run("testMultipleParentsSingleIDCardinalityZero");
    }

    @Test
    public void testMultipleParentsMixedIDsCardinalityMultiple() {
        nestedMergeRules.run("testMultipleParentsMixedIDsCardinalityMultiple");
    }

    @Test
    public void testDefaultIDMerging() {
        nestedMergeRules.run("testDefaultIDMerging");
    }

    @Test
    public void testChildFirstExtends() {
        test("/child-first-extends?testName=testChildFirstExtends", "testChildFirstExtends");
    }

    @Test
    public void testConfiguration() {
        test("/parser-test", "testConfiguration");
    }

    @Test
    public void testSchemaGenerator() {
        test("/schema-test", "testSchemaGenerator");
    }

    @Test
    public void testUpdateSingleton() {
        test("/dynamic-config-test?testName=testSingleton", "testUpdateSingleton");
    }

    @Test
    public void testUpdateFactory() {
        test("/dynamic-config-test?testName=testFactory", "testUpdateFactory");
    }

    @Test
    public void testUpdateSingletonMetatype() {
        test("/dynamic-config-test?testName=testSingletonMetatype", "testUpdateSingletonMetatype");
    }

    @Test
    public void testUpdateFactoryMetatype() {
        test("/dynamic-config-test?testName=testFactoryMetatype", "testUpdateFactoryMetatype");
    }

    @Test
    public void testNested() {
        test("/nested-config-test?testName=testNested", "testNested");
    }

    @Test
    public void testNestedMetatype() {
        test("/nested-config-test?testName=testNestedMetatype", "testNestedMetatype");
    }

    @Test
    public void testNestedReferences() {
        test("/nested-config-test?testName=testNestedReferences", "testNestedReferences");
    }

    @Test
    public void testNestedFactoryReferences() {
        test("/nested-config-test?testName=testNestedFactoryReferences", "testNestedFactoryReferences");
    }

    @Test
    public void testNestedMetatypeFactoryReferences() {
        test("/nested-config-test?testName=testNestedMetatypeFactoryReferences", "testNestedMetatypeFactoryReferences");
    }

    @Test
    public void testFactoryOptionalId() {
        test("/dynamic-config-test?testName=testFactoryOptionalId", "testFactoryOptionalId");
    }

    @Test
    public void testNestedNonUniqueReferences() {
        test("/nested-config-test?testName=testNestedNonUniqueReferences", "testNestedNonUniqueReferences");
    }

    @Test
    public void testTwoReferencesOnePid() {
        test("/reference-config-test?testName=testTwoReferencesOnePid", "testTwoReferencesOnePid");
    }

    @Test
    public void testReferenceAttribute() {
        test("/reference-config-test?testName=testReferenceAttribute", "testReferenceAttribute");
    }

    @Test
    public void testReferenceElement() {
        test("/reference-config-test?testName=testReferenceElement", "testReferenceElement");
    }

    @Test
    public void testReferenceUpdates() {
        test("/reference-config-test?testName=testReferenceUpdates", "testReferenceUpdates");
    }

    @Test
    public void testDefaultConfigSingleton() {
        test("/default-config-test?testName=testDefaultConfigSingleton", "testDefaultConfigSingleton");
    }

    @Test
    public void testDefaultConfigFactory() {
        test("/default-config-test?testName=testDefaultConfigFactory", "testDefaultConfigFactory");
    }

    @Test
    public void testVariableChange() {
        test("/variable-config-test?testName=testVariableChange", "testVariableChange");
    }

    @Test
    @Ignore
    public void testUpdateOrder() {
        test("/nested-config-test?testName=testUpdateOrder", "testUpdateOrder");
    }

    @Test
    public void testNestedDefaults1() {
        test("/nested-defaults-test?testName=testRemoveAttribute", "testRemoveAttribute");
    }

    @Test
    public void testNestedDefaults2() {
        test("/nested-defaults-test?testName=testRemoveAttributeZeroCardinality", "testRemoveAttributeZeroCardinality");
    }

    @Test
    public void testNestedDefaults3() {
        test("/nested-defaults-test?testName=testRemoveAttributeNegativeCardinality", "testRemoveAttributeNegativeCardinality");
    }

    @Test
    public void testNestedSingleton() {
        test("/nested-singleton-test?testName=testInitialConfiguration", "testInitialConfiguration");
        test("/nested-singleton-test?testName=testUpdateReceived", "testUpdateReceived");
    }

    @Test
    public void testSimpleAttributeCopy() {
        attributeCopy.run("testSimpleAttributeCopy");
    }

    @Test
    public void testSimpleAttributeCopyArray() {
        attributeCopy.run("testSimpleAttributeCopyArray");
    }

    @Test
    public void testNestedAttributeCopy() {
        attributeCopy.run("testNestedAttributeCopy");
    }

    @Test
    public void testDoubleNestedAttributeCopy() {
        attributeCopy.run("testDoubleNestedAttributeCopy");
    }

    @Test
    public void testExtendsAttributeCopy() {
        attributeCopy.run("testExtendsAttributeCopy");
    }

    @Test
    public void testSimpleServiceObjectClass() {
        serviceObjectClassTest.run("testSimpleServiceObjectClass");
    }

    @Test
    public void testServiceObjectClassConflict() {
        serviceObjectClassTest.run("testServiceObjectClassConflict");
    }

    private void test(String testUri, String testName) {
        HttpURLConnection con = null;
        URL url = null;
        try {
            url = new URL("http://localhost:" + getPort() + testUri);
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            assertNotNull(is);

            String output = read(is);
            System.out.println("Standard: " + output);
            assertEquals(url + " should return OK", "OK", output.trim());

        } catch (Throwable t) {
            System.err.println("url = " + url);
            outputMgr.failWithThrowable(testName, t);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
