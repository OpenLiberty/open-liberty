/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Hashtable;

import org.junit.Test;

import com.ibm.wsspi.webcontainer.util.RequestUtils;

import junit.framework.Assert;

public class RequestUtilsTest {

    private static String qs = "a=b&c==d&e&f=&=g&=&h+i=j+k&a=c";
    private static String[] expectedKeys = {"a", "c", "f", "", "h i"};
    private static String[][] expectedValues = {{"b", "c"}, {"=d"}, {""}, {"g", ""}, {"j k"}};
    
    private static String qs2 = "key=value&key2=value=2&key3=&key4&key+5=value5&key=value2";
    private static String[] expectedKeys2 = {"key", "key2", "key3", "key 5"};
    private static String[][] expectedValues2 = {{"value", "value2"}, {"value=2"}, {""}, {"value5"}};

    private static String qs3 = "key=И вдаль глядел. Пред ним широко";
    private static String[] expectedKeys3 = {"key"};
    private static String[][] expectedValues3 = {{"И вдаль глядел. Пред ним широко"}};

    private static String qs4;
    static {
        try {
            qs4 = "key=" + URLEncoder.encode("И вдаль глядел. Пред ним широко", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateParams(Hashtable params, String[] expectedKeys, String[][] expectedValues) {
        Assert.assertEquals(expectedKeys.length, params.size());
        for (int i = 0; i < expectedKeys.length; ++i) {
            String[] val = (String[]) params.get(expectedKeys[i]);
            Assert.assertNotNull(val);
            Assert.assertEquals("Wrong number of values for key " + expectedKeys[i], expectedValues[i].length, val.length);
            for (int j = 0; j < expectedValues[i].length; ++j) {
                Assert.assertEquals(expectedValues[i][j], val[j]);
            }
        }
    }

    private void testString(String queryString, String encoding, String[] keys, String[][] values) throws Exception {
        Hashtable params = RequestUtils.parseQueryString(queryString, encoding);
        validateParams(params, keys, values);
    }
    
    @Test
    public void parseStringQueryStringUTF8() throws Exception {
        testString(qs, "UTF-8", expectedKeys, expectedValues);
        testString(qs2, "UTF-8", expectedKeys2, expectedValues2);
        testString(qs4, "UTF-8", expectedKeys3, expectedValues3);
    }

    @Test
    public void parseStringQueryStringISO8859_1() throws Exception {
        testString(qs, "8859_1", expectedKeys, expectedValues);
        testString(qs2, "8859_1", expectedKeys2, expectedValues2);
        testString(qs3, "8859_1", expectedKeys3, expectedValues3);
    }

    private void testCharArray(String queryString, String encoding, String[] keys, String[][] values) {
        char[][] worstCase = new char[queryString.length()][1];
        char[][] optimizedCase = new char[1][queryString.length()];
        for (int i = 0, length = queryString.length(); i < length; ++i) {
            worstCase[i][0] = queryString.charAt(i);
            optimizedCase[0][i] = queryString.charAt(i);
        }

        Hashtable params = RequestUtils.parseQueryString(optimizedCase, encoding);
        validateParams(params, keys, values);

        Assert.assertNull(optimizedCase[0]);
        
        params = RequestUtils.parseQueryString(worstCase, encoding);
        validateParams(params, keys, values);

        for (int i = 0; i < queryString.length(); ++i) {
            Assert.assertNull("index " + i + " is not null", worstCase[i]);
        }

        char[][] simple2ArrayCase = new char[2][];
        for (int i = 0; i <= queryString.length(); ++i) {
            int size2 = queryString.length() - i;
            simple2ArrayCase[0] = new char[i];
            simple2ArrayCase[1] = new char[size2];
            for (int j = 0; j < i; ++j) {
                simple2ArrayCase[0][j] = queryString.charAt(j);
            }
            for (int j = 0; j < size2; ++j) {
                simple2ArrayCase[1][j] = queryString.charAt(i + j);
            }

            params = RequestUtils.parseQueryString(simple2ArrayCase, encoding);
            validateParams(params, keys, values);

            for (int j = 0; j < simple2ArrayCase.length; ++j) {
                Assert.assertNull("index " + j + " is not null", simple2ArrayCase[j]);
            }
        }
    }

    @Test
    public void parseCharArrayQueryStringUTF8() {
        testCharArray(qs, "UTF-8", expectedKeys, expectedValues);
        testCharArray(qs2, "UTF-8", expectedKeys2, expectedValues2);
        testCharArray(qs4, "UTF-8", expectedKeys3, expectedValues3);
    }

    @Test
    public void parseCharArrayQueryStringISO8859_1() {
        testCharArray(qs, "8859_1", expectedKeys, expectedValues);
        testCharArray(qs2, "8859_1", expectedKeys2, expectedValues2);
        testCharArray(qs3, "8859_1", expectedKeys3, expectedValues3);
    }
}
