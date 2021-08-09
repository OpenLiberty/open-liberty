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
package com.ibm.ws.webcontainer.security.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.webcontainer.security.metadata.CollectionMatch;
import com.ibm.ws.webcontainer.security.metadata.WebResourceCollection;

import test.common.SharedOutputManager;

/**
 *
 */
public class WebResourceCollectionTest {

    private static SharedOutputManager outputMgr;

    private static List<String> oneExactURL = null;
    private static List<String> onePathURL = null;
    private static List<String> nestedPathURL = null;
    private static List<String> oneExtensionURL = null;
    private static List<String> multipleAllURL = null;

    private static String ONE_EXACT_URL = "/oneExactURL/oneExactURL.oneExactURL";
    private static String ONE_EXACT_URL_NO_MATCH = "/oneExactURL/oneExactURL.oneExact";
    private static String ONE_PATH_URL = "/onePathURL/*";
    private static String ONE_PATH_URL_MATCH = "/onePathURL/match";
    private static String NESTED_PATH_URL_MATCH = "/twoPathURL/nested/";
    private static String NESTED_PATH_URL_MATCH2 = "/twoPathURL/nested";
    private static String NESTED_PATH_URL_MATCH3 = "/twoPathURL/nested/match";
    private static String NESTED_PATH_URL_MATCH4 = "/twoPathURL/nested/morenested/match";
    private static String ONE_PATH_URL_NO_MATCH1 = "/notMatch/notMatch";
    private static String ONE_PATH_URL_NO_MATCH2 = "/notMatch/onePathURL/prefix";
    private static String ONE_PATH_URL_NO_MATCH3 = "/notMatchonePathURL/test";
    private static String ONE_PATH_URL_NO_MATCH4 = "a/notMatchonePathURL/test";
    private static String ONE_EXT_URL = "*.oneExtension";
    private static String ONE_EXT_URL_MATCH = "matches.oneExtension";
    private static String ONE_EXT_URL_NO_MATCH1 = "nomatches.oneExtension1";
    private static String ONE_EXT_URL_NO_MATCH2 = "oneExtension";

    private static String PATH_NESTED_URL = "/twoPathURL/nested/*";
    private static String PATH_LONGEST_NESTED_URL = "/twoPathURL/nested/morenested/*";

    private static String MIX_URL_1 = "/allExactURL/allExactURL1.allExactURL";
    private static String MIX_URL_2 = "/allPathURL/allPathURL1/*";
    private static String MIX_URL_3 = "*.allExtension1";

    private static String MIX_URL_1_MATCH = "/allExactURL/allExactURL1.allExactURL";
    private static String MIX_URL_2_MATCH = "/allPathURL/allPathURL1/abc.differentExt";
    private static String MIX_URL_3_MATCH = "/allExtURL/match.allExtension1";
    private static String MIX_URL_NO_MATCH = "/all/Ext/URL/No.match";

    private final String[] oneStandardMethod = { "GET" };
    private final String oneStandardMethodNoMatch[] = { "POST" };
    private final String multipleStandardMethods[] = { "GET", "POST", "PUT" };
    private final String multipleBothMethods[] = { "CUSTOM", "GET", "TRACE" };
    private final String multipleBothMethodsNoMatch[] = { "PUT", "HEAD", "OPTIONS" };
    private final String multipleBothMethodsNoMatchCustomMethod[] = { "CUSTOMNOMATCH" };

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor: 
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        oneExactURL = new ArrayList<String>();
        oneExactURL.add(ONE_EXACT_URL);

        onePathURL = new ArrayList<String>();
        onePathURL.add(ONE_PATH_URL);

        nestedPathURL = new ArrayList<String>();
        nestedPathURL.add(PATH_LONGEST_NESTED_URL);
        nestedPathURL.add(PATH_NESTED_URL);

        oneExtensionURL = new ArrayList<String>();
        oneExtensionURL.add(ONE_EXT_URL);

        multipleAllURL = new ArrayList<String>();
        multipleAllURL.add(MIX_URL_1);
        multipleAllURL.add(MIX_URL_2);
        multipleAllURL.add(MIX_URL_3);
    }

    private static List<String> createListFromArray(String[] array) {
        List<String> list = new ArrayList<String>();
        for (String value : array) {
            list.add(value);
        }
        return list;
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

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            List<String> emtpyMethods = new ArrayList<String>();
            List<String> emptyUrlPatterns = new ArrayList<String>();
            WebResourceCollection webResourceCollection = new WebResourceCollection(emptyUrlPatterns, emtpyMethods);
            assertNotNull("There must be a web resource collection.", webResourceCollection);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testConstructorTwoParam() {
        final String methodName = "testConstructorTwoParam";
        try {
            List<String> emtpyMethods = new ArrayList<String>();
            List<String> emptyUrlPatterns = new ArrayList<String>();
            // construct simple one
            List<String> methods = createListFromArray(oneStandardMethod);
            WebResourceCollection webResourceCollection = new WebResourceCollection(oneExactURL, methods);
            assertNotNull(webResourceCollection);

            // construct null one
            webResourceCollection = new WebResourceCollection(emptyUrlPatterns, emtpyMethods);
            assertNotNull(webResourceCollection);

            // construct mixed one
            methods = createListFromArray(multipleBothMethods);
            webResourceCollection = new WebResourceCollection(multipleAllURL, methods);
            assertNotNull(webResourceCollection);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testConstructorThreeParam() {
        final String methodName = "testConstructorThreeParam";
        try {
            List<String> emtpyMethods = new ArrayList<String>();
            List<String> emptyUrlPatterns = new ArrayList<String>();
            // construct null one
            WebResourceCollection webResourceCollection = new WebResourceCollection(emptyUrlPatterns, emtpyMethods, emtpyMethods);
            assertNotNull(webResourceCollection);

            // construct mixed one 1
            List<String> omissionMethods = createListFromArray(multipleBothMethods);
            webResourceCollection = new WebResourceCollection(multipleAllURL, emtpyMethods, omissionMethods);
            assertNotNull(webResourceCollection);

            // construct mixed one 2
            List<String> methods = createListFromArray(multipleStandardMethods);
            webResourceCollection = new WebResourceCollection(multipleAllURL, methods, omissionMethods);
            assertNotNull(webResourceCollection);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMatchesExactNormalOne() {
        List<String> method = createListFromArray(oneStandardMethod);
        WebResourceCollection webResourceCollection = new WebResourceCollection(oneExactURL, method);
        CollectionMatch match = webResourceCollection.performUrlMatch(ONE_EXACT_URL);
        assertNotNull("There must be a match.", match);
        assertEquals("The URIs must be the same.", ONE_EXACT_URL, match.getUrlPattern());
        assertTrue("The method must match.", webResourceCollection.isMethodMatched(oneStandardMethod[0]));
        assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_EXACT_URL_NO_MATCH));
        assertFalse("The method must not match.", webResourceCollection.isMethodMatched(oneStandardMethodNoMatch[0]));
    }

    @Test
    public void testMatchesPathNormalOne() {
        final String methodName = "testMatchesPathNormalOne";
        try {
            List<String> method = createListFromArray(oneStandardMethod);
            WebResourceCollection webResourceCollection = new WebResourceCollection(onePathURL, method);
            CollectionMatch match = webResourceCollection.performUrlMatch(ONE_PATH_URL_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", ONE_PATH_URL, match.getUrlPattern());
            assertTrue(webResourceCollection.isMethodMatched(oneStandardMethod[0]));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_PATH_URL_NO_MATCH1));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_PATH_URL_NO_MATCH2));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_PATH_URL_NO_MATCH3));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_PATH_URL_NO_MATCH4));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMatchesPathNormalNested() {
        final String methodName = "testMatchesPathNormalNested";
        try {
            List<String> method = createListFromArray(oneStandardMethod);
            WebResourceCollection webResourceCollection = new WebResourceCollection(nestedPathURL, method);

            // Resource ends with /
            CollectionMatch match = webResourceCollection.performUrlMatch(NESTED_PATH_URL_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", PATH_NESTED_URL, match.getUrlPattern());

            // Resource does not end with /
            match = webResourceCollection.performUrlMatch(NESTED_PATH_URL_MATCH2);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", PATH_NESTED_URL, match.getUrlPattern());

            // Resource in nested path
            match = webResourceCollection.performUrlMatch(NESTED_PATH_URL_MATCH3);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", PATH_NESTED_URL, match.getUrlPattern());

            // Resource in longest nested path
            match = webResourceCollection.performUrlMatch(NESTED_PATH_URL_MATCH4);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", PATH_LONGEST_NESTED_URL, match.getUrlPattern());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMatchesExtensionNormalOne() {
        final String methodName = "testMatchesExtensionNormalOne";
        try {
            List<String> method = createListFromArray(oneStandardMethod);
            WebResourceCollection webResourceCollection = new WebResourceCollection(oneExtensionURL, method);
            CollectionMatch match = webResourceCollection.performUrlMatch(ONE_EXT_URL_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", ONE_EXT_URL, match.getUrlPattern());
            assertTrue(webResourceCollection.isMethodMatched(oneStandardMethod[0]));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_EXT_URL_NO_MATCH1));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_EXT_URL_NO_MATCH2));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMatchesExactOmissionOne() {
        final String methodName = "testMatchesExactOmissionOne";
        try {
            List<String> emtpyMethods = new ArrayList<String>();
            List<String> method = createListFromArray(oneStandardMethod);
            WebResourceCollection webResourceCollection = new WebResourceCollection(oneExactURL, emtpyMethods, method);
            CollectionMatch match = webResourceCollection.performUrlMatch(ONE_EXACT_URL);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", ONE_EXACT_URL, match.getUrlPattern());
            assertTrue("The method must match.", webResourceCollection.isMethodMatched(oneStandardMethodNoMatch[0]));
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(oneStandardMethod[0]));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_EXACT_URL_NO_MATCH));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMatchesPathOmissionOne() {
        final String methodName = "testMatchesPathOmissionOne";
        try {
            List<String> emtpyMethods = new ArrayList<String>();
            List<String> method = createListFromArray(oneStandardMethod);
            WebResourceCollection webResourceCollection = new WebResourceCollection(onePathURL, emtpyMethods, method);

            // normal case
            CollectionMatch match = webResourceCollection.performUrlMatch(ONE_PATH_URL_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", ONE_PATH_URL, match.getUrlPattern());
            assertTrue("The method must match.", webResourceCollection.isMethodMatched(oneStandardMethodNoMatch[0]));
            // error case
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(oneStandardMethod[0]));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_PATH_URL_NO_MATCH1));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_PATH_URL_NO_MATCH2));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_PATH_URL_NO_MATCH3));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_PATH_URL_NO_MATCH4));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMatchesExtensionOmissionOne() {
        final String methodName = "testMatchesExtensionOmissionOne";
        try {
            List<String> emtpyMethods = new ArrayList<String>();
            List<String> method = createListFromArray(oneStandardMethod);
            WebResourceCollection webResourceCollection = new WebResourceCollection(oneExtensionURL, emtpyMethods, method);

            // normal case
            CollectionMatch match = webResourceCollection.performUrlMatch(ONE_EXT_URL_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", ONE_EXT_URL, match.getUrlPattern());
            assertTrue(webResourceCollection.isMethodMatched(oneStandardMethodNoMatch[0]));
            // error case
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(oneStandardMethod[0]));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_EXT_URL_NO_MATCH1));
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(ONE_EXT_URL_NO_MATCH2));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMatchesMulipleNormal() {
        final String methodName = "testMatchesMulipleNormal";
        try {
            List<String> methods = createListFromArray(multipleBothMethods);
            WebResourceCollection webResourceCollection = new WebResourceCollection(multipleAllURL, methods);

            // normal case 1
            CollectionMatch match = webResourceCollection.performUrlMatch(MIX_URL_1_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", MIX_URL_1, match.getUrlPattern());
            assertTrue(webResourceCollection.isMethodMatched(multipleBothMethods[0]));
            // normal case 2
            match = webResourceCollection.performUrlMatch(MIX_URL_2_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", MIX_URL_2, match.getUrlPattern());
            assertTrue(webResourceCollection.isMethodMatched(multipleBothMethods[1]));
            // normal case 3
            match = webResourceCollection.performUrlMatch(MIX_URL_3_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", MIX_URL_3, match.getUrlPattern());
            assertTrue(webResourceCollection.isMethodMatched(multipleBothMethods[2]));
            // error cases
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(MIX_URL_NO_MATCH));
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(multipleBothMethodsNoMatch[0]));
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(multipleBothMethodsNoMatch[1]));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMatchesMulipleOmission() {
        final String methodName = "testMatchesMulipleOmission";
        try {
            List<String> emtpyMethods = new ArrayList<String>();
            List<String> omissionMethods = createListFromArray(multipleBothMethods);
            WebResourceCollection webResourceCollection = new WebResourceCollection(multipleAllURL, emtpyMethods, omissionMethods);

            // normal case 1
            CollectionMatch match = webResourceCollection.performUrlMatch(MIX_URL_1_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", MIX_URL_1, match.getUrlPattern());
            assertTrue(webResourceCollection.isMethodMatched(multipleBothMethodsNoMatch[0]));
            // normal case 2
            match = webResourceCollection.performUrlMatch(MIX_URL_2_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", MIX_URL_2, match.getUrlPattern());
            assertTrue(webResourceCollection.isMethodMatched(multipleBothMethodsNoMatch[1]));
            // normal case 3
            match = webResourceCollection.performUrlMatch(MIX_URL_3_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", MIX_URL_3, match.getUrlPattern());
            assertTrue(webResourceCollection.isMethodMatched(multipleBothMethodsNoMatch[2]));
            // error cases
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(MIX_URL_NO_MATCH));
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(multipleBothMethods[2]));
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(multipleBothMethods[1]));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCustomPropertyNormal() {
        final String methodName = "testCustomPropertyNormal";
        try {
            List<String> methods = createListFromArray(multipleBothMethods);
            WebResourceCollection webResourceCollection = new WebResourceCollection(multipleAllURL, methods);

            // normal case 1 (method name doesn't match, but uri matches)
            CollectionMatch match = webResourceCollection.performUrlMatch(MIX_URL_1_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", MIX_URL_1, match.getUrlPattern());
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(multipleBothMethodsNoMatchCustomMethod[0]));

            // normal case 2 ( normal one works)
            assertTrue("The method must match.", webResourceCollection.isMethodMatched(multipleBothMethods[1]));

            // no match case 1 ( URL matches, but http method doesn't match)
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(multipleBothMethodsNoMatch[1]));

            // no match case 2 ( URL doesn'tmatch, but http method matches)
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(MIX_URL_NO_MATCH));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCustomPropertyOmission() {
        final String methodName = "testCustomPropertyOmission";
        try {
            List<String> emtpyMethods = new ArrayList<String>();
            List<String> omissionMethods = createListFromArray(multipleBothMethods);
            WebResourceCollection webResourceCollection = new WebResourceCollection(multipleAllURL, emtpyMethods, omissionMethods);

            // normal case 1 (method name doesn't match, but uri matches)
            CollectionMatch match = webResourceCollection.performUrlMatch(MIX_URL_1_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", MIX_URL_1, match.getUrlPattern());
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(multipleBothMethods[0]));

            // normal case 2 ( normal one works)
            assertTrue("The method must match.", webResourceCollection.isMethodMatched(multipleBothMethodsNoMatch[1]));

            // no match case 1 ( URL matches, but http method doesn't match)
            assertFalse("The method must not match.", webResourceCollection.isMethodMatched(multipleBothMethods[1]));

            // no match case 2 ( URL doesn'tmatch, but http method matches)
            assertNull("There must not be a match.", webResourceCollection.performUrlMatch(MIX_URL_NO_MATCH));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /*
     * check validate all methods
     */
    @Test
    public void testMatchesAllMethods() {
        final String methodName = "testCustomPropertyOmission";
        try {
            List<String> emtpyMethods = new ArrayList<String>();
            WebResourceCollection webResourceCollection = new WebResourceCollection(multipleAllURL, emtpyMethods, emtpyMethods);

            // since all methods match, as long as URL pattern is matched it should return URL.
            CollectionMatch match = webResourceCollection.performUrlMatch(MIX_URL_NO_MATCH);
            assertNull("There must not be a match.", match);
            assertTrue("The method must match.", webResourceCollection.isMethodMatched(multipleBothMethods[0]));

            // normal case 1
            match = webResourceCollection.performUrlMatch(MIX_URL_1_MATCH);
            assertNotNull("There must be a match.", match);
            assertEquals("The URIs must be the same.", MIX_URL_1, match.getUrlPattern());

            // normal case 2
            assertTrue("The method must match.", webResourceCollection.isMethodMatched(multipleBothMethods[1]));

            // normal case 3
            assertTrue("The method must match.", webResourceCollection.isMethodMatched(multipleBothMethods[2]));

            // normal case 4
            assertTrue("The method must match.", webResourceCollection.isMethodMatched(multipleBothMethodsNoMatch[0]));

            // normal case 5
            assertTrue("The method must match.", webResourceCollection.isMethodMatched(multipleBothMethodsNoMatch[1]));

            // normal case 6
            assertTrue("The method must match.", webResourceCollection.isMethodMatched(multipleBothMethodsNoMatch[2]));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void isCustomMethodListedNormal() {
        final String methodName = "isCustomMethodListedNormal";
        try {
            List<String> methods = createListFromArray(multipleBothMethods);
            WebResourceCollection webResourceCollection = new WebResourceCollection(multipleAllURL, methods);
            assertTrue("The method must be listed.", webResourceCollection.isMethodListed(multipleBothMethods[0]));
            assertFalse("The method must not be listed.", webResourceCollection.isMethodListed("FOO"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void isCustomMethodListedOmission() {
        final String methodName = "isCustomMethodListedOmission";
        try {
            List<String> emtpyMethods = new ArrayList<String>();
            List<String> omissionMethods = createListFromArray(multipleBothMethods);
            WebResourceCollection webResourceCollection = new WebResourceCollection(multipleAllURL, emtpyMethods, omissionMethods);
            assertTrue("The method must be listed.", webResourceCollection.isMethodListed(multipleBothMethods[0]));
            assertFalse("The method must not be listed.", webResourceCollection.isMethodListed("FOO"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
