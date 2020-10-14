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
package com.ibm.ws.zos.wlm.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;

/**
 * Unit tests for the WlmClassification engine
 */
public class WlmClassificationUnitTest {
    /**
     * Mock environment for these tests.
     */
    private static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    /*
     * This method is used by all tests to automate the creation of a WlmClassification. It takes in String arrays for
     * transactionClass, host, port, method and resource. It mocks all required objects (Dictionary, ConfigurationAdmin and Configuration)
     * and then creates a new classifier and drives all required lifecycle methods on it to new up all internal data structures and
     * populate the List<ClassificationData> of classification rules.
     */
    private WlmClassification setupExpectations(final String[] tclass, final String[] host, final String[] port, final String[] method, final String[] resource,
                                                String functionName) {
        /*
         * Setup all of the mock objects that will be required for the call to WlmClassification.
         * If this method is being called by a test that requires multiple configurations
         * more than one mockConfig is going to be required.
         */
        final Dictionary mockDict = mockery.mock(Dictionary.class, "Dictionary" + functionName);
        final ConfigurationAdmin mockConfigAdmin = mockery.mock(ConfigurationAdmin.class, "ConfigurationAdmin" + functionName);
        final Configuration[] mockConfig = new Configuration[tclass.length];
        for (int i = 0; i < tclass.length; i++) {
            mockConfig[i] = mockery.mock(Configuration.class, "Configuration" + functionName + i);
        }

        /*
         * Mopck MDB classification call.
         */
        mockery.checking(new Expectations() {
            {
                oneOf(mockDict).get(with(equal("mdbClassification")));
                will(returnValue(null));
            }
        });

        /*
         * Expectations for the mock Dictionary. If this method is being called with just one configuration we return
         * only one pid, and if this method is being called to setup multiple configs then we return a correctly
         * sized array of pids.
         */
        mockery.checking(new Expectations() {
            {
                oneOf(mockDict).get(with(equal("httpClassification")));

                if (tclass.length == 1) {
                    will(returnValue(new String[] { "defaultPid" }));
                } else {
                    String[] pids = new String[tclass.length];
                    for (int i = 0; i < tclass.length; i++) {
                        pids[i] = new String("defaultPid" + i);
                    }
                    will(returnValue(pids));
                }
            }
        });

        /*
         * Expectations for the mockConfigAdmin. If this method is being called with just one configuration we simply
         * return the only mockConfig available. If this method is being called to setup multiple configurations then we
         * return multiple mock Configurations for each call.
         */
        try {
            mockery.checking(new Expectations() {
                {
                    if (tclass.length == 1) {
                        // This is the case with only one configuration
                        oneOf(mockConfigAdmin).getConfiguration("defaultPid", null);
                        will(returnValue(mockConfig[0]));
                    } else {
                        // This is the case with an arbitrary number of configurations
                        for (int i = 0; i < tclass.length; i++) {
                            oneOf(mockConfigAdmin).getConfiguration("defaultPid" + i, null);
                            will(returnValue(mockConfig[i]));
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        mockery.checking(new Expectations() {
            {
                for (int i = 0; i < tclass.length; i++) {
                    oneOf(mockConfig[i]).getProperties();

                    Dictionary<String, String> returnDict = new Hashtable<String, String>();
                    returnDict.put("transactionClass", tclass[i]);
                    returnDict.put("host", host[i]);
                    returnDict.put("port", port[i]);
                    returnDict.put("method", method[i]);
                    returnDict.put("resource", resource[i]);

                    will(returnValue(returnDict));
                }
            }
        });

        /*
         * Expectations have now been setup. The WlmClassification that is being returned
         * needs to be created and have it's lifecycle methods called in order to put the
         * environment in a "good" state.
         */
        WlmClassification returnClassifier = new WlmClassification();
        returnClassifier.activate(null, null);
        returnClassifier.setConfigurationAdmin(mockConfigAdmin);

        try {
            returnClassifier.updated("defaultPid", mockDict);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            Assert.fail();
        }

        return returnClassifier;
    }

    static public boolean compareTranClassResults(String expectedTranClass, byte[] classifiedTranClass) {
        byte[] expectedResult;
        try {
            expectedResult = (expectedTranClass + "        ".substring(0, WLMNativeServices.WLM_MAXIMUM_TRANSACTIONCLASS_LENGTH - expectedTranClass.length())).getBytes("Cp1047");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("code page conversion error", uee);
        }

        return Arrays.equals(expectedResult, classifiedTranClass);
    }

    /**
     * Test the basic case. A single request being classified against a single configuration element.
     */
    @Test
    public void singleConfigMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "NEWCLASS";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMatch");

        // Call our classify method and store it's return value
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test the basic case. A single request being classified that shouldn't match the single configuration element.
     */
    @Test
    public void singleConfigNoMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigNoMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "POST");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the Host is a wildcard.
     */
    @Test
    public void singleConfigHostReplacedMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "*" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "NEWCLASS";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigHostReplacedMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification not matching with a single configuration element where the Host is a wildcard.
     */
    @Test
    public void singleConfigHostReplacedNoMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "*" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigHostReplacedNoMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "POST");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the Port is a wildcard.
     */
    @Test
    public void singleConfigPortReplacedMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "*" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "NEWCLASS";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigPortReplacedMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification not matching with a single configuration element where the Port is a wildcard.
     */
    @Test
    public void singleConfigPortReplacedNoMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "*" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigPortReplacedNoMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "POST");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the Port is a range.
     */
    @Test
    public void singleConfigPortRangeMatchMiddle() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9075-9085" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "NEWCLASS";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigPortRangeMatchMiddle");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the Port is a range. Match first.
     */
    @Test
    public void singleConfigPortRangeMatchFirst() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9075-9085" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "NEWCLASS";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigPortRangeMatchFirst");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9075, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the Port is a range. Match last.
     */
    @Test
    public void singleConfigPortRangeMatchLast() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9075-9085" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "NEWCLASS";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigPortRangeMatchLast");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9085, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification not matching with a single configuration element where the Port is a range.
     */
    @Test
    public void singleConfigPortRangeNoMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9075-9085" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigPortRangeNoMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "POST");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the Method is a wildcard.
     */
    @Test
    public void singleConfigMethodReplacedMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "*" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "NEWCLASS";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMethodReplacedMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification not matching with a single configuration element where the Method is a wildcard.
     */
    @Test
    public void singleConfigMethodReplacedNoMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "*" };
        String[] resource = new String[] { "/testResource" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMethodReplacedNoMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9085, "/testResource", "GET");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the Resource is a wildcard.
     */
    @Test
    public void singleConfigResourceReplacedMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "*" };

        String expectedResult = "NEWCLASS";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigResourceReplacedMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classificaiton not matching with a single configuration element where the Resource is a wildcard.
     */
    @Test
    public void singleConfigResourceReplacedNoMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "*" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigResourceReplacedNoMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "POST");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the Resource ends with a wildcard.
     */
    @Test
    public void singleConfigResourcePartialReplacedMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/test*" };

        String expectedResult = "NEWCLASS";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigResourcePartialReplacedMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification not matching with a single configuration element where the Resource ends with a wildcard.
     */
    @Test
    public void singleConfigResourcePartialReplacedNoMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "NEWCLASS" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/test*" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigResourcePartialReplacedNoMatch");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "POST");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification matching with multiple configuration elements where the first configuration matches.
     */
    @Test
    public void multipleConfigMatchFirst() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001", "CLASS002", "CLASS003", "CLASS004" };
        String[] host = new String[] { "127.0.0.1", "*", "127.0.0.1", "*" };
        String[] port = new String[] { "9080", "*", "*", "*" };
        String[] method = new String[] { "GET", "POST", "*", "*" };
        String[] resource = new String[] { "/testResource", "/testResource", "*", "*" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "multipleConfigMatchFirst");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification matching with multiple configuration elements where the first configuration doesn't match.
     */
    @Test
    public void multipleConfigMatchNotFirst() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001", "CLASS002", "CLASS003", "CLASS004" };
        String[] host = new String[] { "127.0.0.1", "*", "127.0.0.1", "*" };
        String[] port = new String[] { "9080", "*", "*", "*" };
        String[] method = new String[] { "GET", "POST", "*", "*" };
        String[] resource = new String[] { "/testResource", "/testResource", "*", "*" };

        String expectedResult = "CLASS003";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "multipleConfigMatchNotFirst");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9085, "/newTestResource", "PUT");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification matching with multiple configuration elements where the last configuration matches.
     */
    @Test
    public void multipleConfigMatchLast() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001", "CLASS002", "CLASS003", "CLASS004" };
        String[] host = new String[] { "127.0.0.1", "*", "127.0.0.1", "*" };
        String[] port = new String[] { "9080", "*", "*", "*" };
        String[] method = new String[] { "GET", "POST", "*", "*" };
        String[] resource = new String[] { "/testResource", "/testResource", "*", "*" };

        String expectedResult = "CLASS004";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "multipleConfigMatchLast");

        // Call our classify method and store it's return
        byte[] returnedClassification = classifier.classifyHTTP("192.168.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the port is a comma separated list.
     */
    @Test
    public void singleConfigMutiplePortMatchMiddle() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080,9043,9070" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMultiplePortMatchMiddle");

        // Call our classify method and store its return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9043, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the port is a comma separated list.
     */
    @Test
    public void singleConfigMutiplePortMatchFirst() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080,9043,9070" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMultiplePortMatchFirst");

        // Call our classify method and store its return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the port is a comma separated list.
     */
    @Test
    public void singleConfigMutiplePortMatchLast() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080,9043,9070" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMultiplePortMatchLast");

        // Call our classify method and store its return
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9070, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification not matching with a single configuration element where the port is a comma separated list.
     */
    @Test
    public void singleConfigMultiplePortNoMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080,9043,9070" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMultiplePortNoMatch");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9081, "/testResource", "GET");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the method is a comma separated list
     */
    @Test
    public void singleConfigMultipleMethodMatchMiddle() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET,POST,PUT" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMultipleMethodMatchMiddle");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "POST");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the method is a comma separated list
     */
    @Test
    public void singleConfigMultipleMethodMatchFirst() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET,POST,PUT" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMultipleMethodMatchFirst");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the method is a comma separated list
     */
    @Test
    public void singleConfigMultipleMethodMatchLast() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET,POST,PUT" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMultipleMethodMatchLast");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "PUT");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification not matching with a single configuration element where the method is a comma separated list
     */
    @Test
    public void singleConfigMultipleMethodNoMatch() {
        // Setup the values to be used when calling the updated() method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET,POST" };
        String[] resource = new String[] { "/testResource" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMultipleMethodNoMatch");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "PUT");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification matching with a single configuration element where the port is a comma separated list of ranges
     */
    @Test
    public void singleConfigMultiplePortRangesMatch() {
        // Setup the values to be used when calling the udpated() method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9040-9045,9078-9082,9068-9072" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResource" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMultiplePortRangeMatch");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification not matching with a single configuration element where the port is a comma separated list of ranges
     */
    @Test
    public void singleConfigMultiplePortRangesNoMatch() {
        // Setup the values to be used when calling the udpated() method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9040-9045,9078-9082,9068-9072" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/testResoruce" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigMultiplePortRangeNoMatch");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 1337, "/testResource", "GET");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification using double wildcarding within the resource.
     */
    @Test
    public void singleConfigResourceDoubleWildcard() {
        // Setup the values to be used when calling the updated method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/**/testResource" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigResourceDoubleWildcard");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/this/is/a/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));

        // Should not find
        returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/this/is/a/testedResource", "GET");
        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));

        returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/this/testResource", "GET");
        assertTrue(compareTranClassResults(expectedResult, returnedClassification));

        returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");
        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test a classification that uses an illegal wildcarding pattern to replace the resource.
     */
    @Test
    public void singleConfigResourceIllegalWildcard() {
        // Setup the values to be used when calling the updated method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/***/testResource" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigResourceIllegalWildcard");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification that uses both single and double asterisk wildcarding within the resource name
     */
    @Test
    public void singleConfigResourceCombinedWildcarding() {
        // Setup the values to be used when calling the updated method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/**/test*" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigResourceCombinedWildcarding");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/this/is/a/testResource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));

        returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/this/is/a/resource", "GET");
        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    @Test
    public void singleConfigResourceDoubleWildcardMiddle() {
        // Setup the values to be used when calling the updated method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/this/**/resource" };

        String expectedResult = "CLASS001";

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigResourceDoubleWildcardMiddle");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/this/is/a/resource", "GET");

        assertTrue(compareTranClassResults(expectedResult, returnedClassification));

        returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/this/is/a/testResource", "GET");
        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));

        returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/this/resource", "GET");
        assertTrue(compareTranClassResults(expectedResult, returnedClassification));
    }

    /**
     * Test that the updated method works as expected when no configuration is specified
     */
    @Test
    public void noConfig() {
        String[] tclass = new String[0];
        String[] host = new String[0];
        String[] port = new String[0];
        String[] method = new String[0];
        String[] resource = new String[0];

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "noConfig");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/testResource", "GET");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }

    /**
     * Test a classification with a bad use of wildcarding (where a "**" is used illegaly)
     */
    @Test
    public void singleConfigResourceBadWildcard() {
        // Setup the values to be used when calling the updated method
        String[] tclass = new String[] { "CLASS001" };
        String[] host = new String[] { "127.0.0.1" };
        String[] port = new String[] { "9080" };
        String[] method = new String[] { "GET" };
        String[] resource = new String[] { "/this**/is/a/testResource" };

        // Setup the expectations and create the classifier to work with
        WlmClassification classifier = setupExpectations(tclass, host, port, method, resource, "singleConfigResourceBadWildcard");

        // Call our classify method and store its result
        byte[] returnedClassification = classifier.classifyHTTP("127.0.0.1", 9080, "/this/is/a/test/resource", "GET");

        assertFalse(WlmClassification.isValidTransactionClass(returnedClassification));
    }
}
