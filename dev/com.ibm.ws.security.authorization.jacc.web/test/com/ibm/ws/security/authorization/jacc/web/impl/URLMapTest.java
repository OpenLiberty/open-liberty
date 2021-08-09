/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.web.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class URLMapTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule outputRule = outputMgr;

    static final String URL_PATH_1 = "/test/test/test/*";

    /**
     * This is not a general implementation you can copy! It does specific things
     * to factor in the fact that expected/actual string may start with an !.
     *
     * @param expected
     * @param actual
     */
    private void assertEqualsOrderIndependent(String expected, String actual) {
        String failMsgCommon = " Expected=[" + expected + "] Actual=[" + actual + "]";
        assertEquals("The expected and actual Strings were not of the same length." + failMsgCommon,
                     expected.length(), actual.length());

        int notOffset = 0;
        int qualifierOffset = expected.length();
        if (expected.startsWith("!")) {
            assertTrue("Expected string starts with ! but actual string does not." + failMsgCommon,
                       actual.startsWith("!"));
            notOffset = 1;
        }
        if (expected.contains(":")) {
            assertTrue("Expected string has :QUALIFIER but actual string does not." + failMsgCommon,
                       expected.contains(":"));
            qualifierOffset = expected.indexOf(":");
            String expectedQualifier = expected.substring(qualifierOffset);
            String actualQualifier = actual.substring(actual.indexOf(":"));
            assertEquals("The qualifiers did not match." + failMsgCommon,
                         expectedQualifier, actualQualifier);
        }

        String[] expectedCmps = expected.substring(notOffset, qualifierOffset).split(",");
        String[] actualCmps = actual.substring(notOffset, qualifierOffset).split(",");
        assertEquals("The number of expected and actual comma-separated components were not of the same length." + failMsgCommon,
                     expectedCmps.length, actualCmps.length);
        for (String expectedCmp : expectedCmps) {
            boolean matched = false;
            for (String actualCmp : actualCmps) {
                matched |= expectedCmp.equals(actualCmp);
            }
            assertTrue("Expected element [" + expectedCmp + "] was not found in " + actual, matched);
        }
    }

    /**
     * Tests constructor
     * Expected result: make sure that the object is constructed properly
     * and all fields are set as default value.
     */
    @Test
    public void ctorNull() {
        URLMap urlmap = new URLMap((String) null);
        assertNotNull(urlmap);
        assertEquals("", urlmap.getURLPattern());
    }

    /**
     * Tests constructor
     * Expected result: make sure that the object is constructed properly
     * and all fields are set as default value.
     */
    @Test
    public void ctorValidParam() {
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        assertEquals(URL_PATH_1, urlmap.getURLPattern());
    }

    /**
     * Tests appendURLPattern
     * Expected result: make sure that the object is unchanged.
     */
    @Test
    public void appendURLPatternNull() {
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.appendURLPattern(null);
        assertEquals(URL_PATH_1, urlmap.getURLPattern());
    }

    /**
     * Tests appendURLPattern
     * Expected result: make sure that the object is unchanged.
     */
    @Test
    public void appendURLPatternEmpty() {
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.appendURLPattern("");
        assertEquals(URL_PATH_1, urlmap.getURLPattern());
    }

    /**
     * Tests appendURLPattern
     * Expected result: make sure that the urlspec string has changed.
     */
    @Test
    public void appendURLPatternNormal() {
        String append = "append";
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.appendURLPattern(append);
        assertEquals(URL_PATH_1 + ":" + append, urlmap.getURLPattern());
    }

    /**
     * Tests setExcludedSet
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setExcludedSetWithMethodsToNormal() {
        final String methodName = "setExcludedSetWithMethodsToNormal";
        String METHOD[] = { "GET", "PUT" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setExcludedSet(mList);
        try {
            // first, check the value is set
            List<String> output = urlmap.getExcludedSet(URLMap.METHODS_NORMAL);
            assertNotNull(output);
            String outputResult[] = getArray(output);
            String expectedResult[] = METHOD.clone();
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));
            // second, check the value isn't set to other attributes:
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_ALL));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setExcludedSet
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setExcludedSetWithMethodsToOmission() {
        final String methodName = "setExcludedSetWithMethodsToOmission";
        String METHOD[] = { "POST", "OPTIONS", "CUSTOMMETHOD" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setExcludedSet(mList, true);
        try {
            // first, check the value is set
            List<String> output = urlmap.getExcludedSet(URLMap.METHODS_OMISSION);
            assertNotNull(output);
            String outputResult[] = getArray(output);
            String expectedResult[] = METHOD.clone();
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));
            // second, check the value isn't set to other attributes:
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_ALL));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setExcludedSet
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setExcludedSetWithAllMethodsToNormal() {
        final String methodName = "setExcludedSetWithAllMethodsToNormal";
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setExcludedSet(null);
        try {
            // first, check the value is set
            List<String> output = urlmap.getExcludedSet(URLMap.METHODS_ALL);
            assertNotNull(output);
            String outputResult[] = getArray(output);
            String expectedResult[] = { "ALLMETHODS" };
            assertTrue(Arrays.equals(expectedResult, outputResult));
            // second, check the value isn't set to other attributes:
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_ALL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setExcludedSet
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setExcludedSetWithAllMethodsToOmission() {
        final String methodName = "setExcludedSetWithAllMethodsToOmission";
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setExcludedSet(null, true);
        try {
            // first, check the value is set
            List<String> output = urlmap.getExcludedSet(URLMap.METHODS_ALL);
            assertNotNull(output);
            String outputResult[] = getArray(output);
            String expectedResult[] = { "ALLMETHODS" };
            assertTrue(Arrays.equals(expectedResult, outputResult));
            // second, check the value isn't set to other attributes:
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_ALL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setUncheckedSet
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUncheckedSetWithMethodsToNormal() {
        final String methodName = "setUncheckedSetWithMethodsToNormal";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUncheckedSet(mList);
        try {
            // first, check the value is set
            List<String> output = urlmap.getUncheckedSet(URLMap.METHODS_NORMAL);
            assertNotNull(output);
            String outputResult[] = getArray(output);
            String expectedResult[] = METHOD.clone();
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));
            // second, check the value isn't set to other attributes:
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_ALL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setUncheckedSet
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUncheckedSetOmission() {
        final String methodName = "setUncheckedSetOmission";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUncheckedSet(mList, true);
        try {
            // first, check the value is set
            List<String> output = urlmap.getUncheckedSet(URLMap.METHODS_OMISSION);
            assertNotNull(output);
            String outputResult[] = getArray(output);
            String expectedResult[] = METHOD.clone();
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));
            // second, check the value isn't set to other attributes:
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_ALL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setUncheckedSet
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUncheckedSetWithAllMethodsToNormal() {
        final String methodName = "setUncheckedSetWithAllMethodsToNormal";
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUncheckedSet(null);
        try {
            // first, check the value is set
            List<String> output = urlmap.getUncheckedSet(URLMap.METHODS_ALL);
            assertNotNull(output);
            String outputResult[] = getArray(output);
            String expectedResult[] = { "ALLMETHODS" };
            assertTrue(Arrays.equals(expectedResult, outputResult));
            // second, check the value isn't set to other attributes:
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_ALL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setUncheckedSet
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUncheckedSetWithAllMethodsToOmission() {
        final String methodName = "setUncheckedSetWithAllMethodsToOmission";
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUncheckedSet(null, true);
        try {
            // first, check the value is set
            List<String> output = urlmap.getUncheckedSet(URLMap.METHODS_ALL);
            assertNotNull(output);
            String outputResult[] = getArray(output);
            String expectedResult[] = { "ALLMETHODS" };
            assertTrue(Arrays.equals(expectedResult, outputResult));
            // second, check the value isn't set to other attributes:
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_ALL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setRoleMapWithMethodsToNormal() {
        final String methodName = "setRoleMapWithMethodsToNormal";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        String ROLE = "RoleRole";
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE, mList);
        try {
            // first, check the methods are correct
            Map<String, MethodConstraint> output = urlmap.getRoleMap(URLMap.METHODS_NORMAL);
            assertNotNull(output);
            String outputResult[] = getArray(new ArrayList<String>(output.keySet()));
            String expectedResult[] = METHOD.clone();
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));

            // then check the role
            MethodConstraint outputMC[] = new MethodConstraint[output.size()];
            for (int i = 0; i < output.size(); i++) {
                outputMC[i] = output.get(outputResult[i]);
            }
            for (int i = 0; i < outputMC.length; i++) {
                List<String> role = outputMC[i].getRoleList();
                assertEquals(1, role.size()); // check number of role is one
                assertEquals(ROLE, role.get(0));
            }

            // after that, check the value isn't set to other attributes:
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_ALL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setRoleMapWithMethodsToOmission() {
        final String methodName = "setRoleMapWithMethodsToOmission";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        String ROLE = "RoleRole";
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE, mList, true);
        try {
            // first, check the methods are correct
            Map<String, MethodConstraint> output = urlmap.getRoleMap(URLMap.METHODS_OMISSION);
            assertNotNull(output);
            String outputResult[] = getArray(new ArrayList<String>(output.keySet()));
            String expectedResult[] = METHOD.clone();
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));

            // then check the role
            MethodConstraint outputMC[] = new MethodConstraint[output.size()];
            for (int i = 0; i < output.size(); i++) {
                outputMC[i] = output.get(outputResult[i]);
            }

            for (int i = 0; i < outputMC.length; i++) {
                List<String> role = outputMC[i].getRoleList();
                assertEquals(1, role.size()); // check number of role is one
                assertEquals(ROLE, role.get(0));
            }

            // after that, check the value isn't set to other attributes:
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_ALL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setRoleMapWithAllMethodsToNormal() {
        final String methodName = "setRoleMapWithAllMethodsToNormal";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String ROLE = "RoleRole";
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE, null);
        try {
            // first, check the methods are corect
            Map<String, MethodConstraint> output = urlmap.getRoleMap(URLMap.METHODS_ALL);
            assertNotNull(output);
            String outputResult[] = getArray(new ArrayList<String>(output.keySet()));
            String expectedResult[] = { "ALLMETHODS" };
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));

            // then check the role
            MethodConstraint outputMC[] = new MethodConstraint[output.size()];
            for (int i = 0; i < output.size(); i++) {
                outputMC[i] = output.get(outputResult[i]);
            }
            for (int i = 0; i < outputMC.length; i++) {
                List<String> role = outputMC[i].getRoleList();
                assertEquals(1, role.size()); // check number of role is one
                assertEquals(ROLE, role.get(0));
            }

            // after that, check the value isn't set to other attributes:
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setRoleMapWithAllMethodsToOmission() {
        final String methodName = "setRoleMapWithAllMethodsToOmission";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String ROLE = "RoleRole";
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE, null, true);
        try {
            // first, check the methods are corect
            Map<String, MethodConstraint> output = urlmap.getRoleMap(URLMap.METHODS_ALL);
            assertNotNull(output);
            String outputResult[] = getArray(new ArrayList<String>(output.keySet()));
            String expectedResult[] = { "ALLMETHODS" };
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));

            // then check the role
            MethodConstraint outputMC[] = new MethodConstraint[output.size()];
            for (int i = 0; i < output.size(); i++) {
                outputMC[i] = output.get(outputResult[i]);
            }
            for (int i = 0; i < outputMC.length; i++) {
                List<String> role = outputMC[i].getRoleList();
                assertEquals(1, role.size()); // check number of role is one
                assertEquals(ROLE, role.get(0));
            }

            // after that, check the value isn't set to other attributes:
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_ALL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_NORMAL));
            assertNull(urlmap.getExcludedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getUncheckedSet(URLMap.METHODS_OMISSION));
            assertNull(urlmap.getRoleMap(URLMap.METHODS_OMISSION));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setUserDataMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUserDataMapAsConfidentialWithMethodsToNormal() {
        final String methodName = "setUserDataMapAsConfidentialWithMethodsToNormal";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        String USERDATA = "CONFIDENTIAL"; //the value should be CONFIDENTIAL, INTEGRAL, or NONE. No value check in MethodConstraint class though
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUserDataMap(USERDATA, mList);
        try {
            // first, check the methods are correct
            Map<String, MethodConstraint> output = urlmap.getUserDataMap(URLMap.METHODS_NORMAL);
            assertNotNull(output);
            String outputResult[] = getArray(new ArrayList<String>(output.keySet()));
            String expectedResult[] = METHOD.clone();
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));

            // then check the userdata value
            MethodConstraint outputMC[] = new MethodConstraint[output.size()];
            for (int i = 0; i < output.size(); i++) {
                outputMC[i] = output.get(outputResult[i]);
            }
            for (int i = 0; i < outputMC.length; i++) {
                String userData = outputMC[i].getUserData();
                assertEquals(USERDATA, userData);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setUserDataMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUserDataMapAsIntegralWithMethodsToOmission() {
        final String methodName = "setUserDataMapAsIntegralWithMethodsToOmission";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT", "TRACE" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        String USERDATA = "INTEGRAL"; //the value should be CONFIDENTIAL, INTEGRAL, or NONE. No value check in MethodConstraint class though
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUserDataMap(USERDATA, mList, true);
        try {
            // first, check the methods are correct
            Map<String, MethodConstraint> output = urlmap.getUserDataMap(URLMap.METHODS_OMISSION);
            assertNotNull(output);
            String outputResult[] = getArray(new ArrayList<String>(output.keySet()));
            String expectedResult[] = METHOD.clone();
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));

            // then check the userdata value
            MethodConstraint outputMC[] = new MethodConstraint[output.size()];
            for (int i = 0; i < output.size(); i++) {
                outputMC[i] = output.get(outputResult[i]);
            }
            for (int i = 0; i < outputMC.length; i++) {
                String userData = outputMC[i].getUserData();
                assertEquals(USERDATA, userData);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setUserDataMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUserDataMapAsNoneWithMethodsToNormal() {
        final String methodName = "setUserDataMapAsNoneWithMethodsToNormal";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        String USERDATA = "NONE"; //the value should be CONFIDENTIAL, INTEGRAL, or NONE. No value check in MethodConstraint class though
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUserDataMap(USERDATA, mList);
        try {
            // first, check the methods are correct
            Map<String, MethodConstraint> output = urlmap.getUserDataMapNone(URLMap.METHODS_NORMAL);
            assertNotNull(output);
            String outputResult[] = getArray(new ArrayList<String>(output.keySet()));
            String expectedResult[] = METHOD.clone();
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));

            // then check the userdata value
            MethodConstraint outputMC[] = new MethodConstraint[output.size()];
            for (int i = 0; i < output.size(); i++) {
                outputMC[i] = output.get(outputResult[i]);
            }
            for (int i = 0; i < outputMC.length; i++) {
                assertTrue(outputMC[i].isUserDataNone());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setUserDataMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUserDataMapAsNoneWithMethodsToOmission() {
        final String methodName = "setUserDataMapAsNoneWithMethodsToOmission";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT", "TRACE" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        String USERDATA = "NONE"; //the value should be CONFIDENTIAL, INTEGRAL, or NONE. No value check in MethodConstraint class though
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUserDataMap(USERDATA, mList, true);
        try {
            // first, check the methods are correct
            Map<String, MethodConstraint> output = urlmap.getUserDataMapNone(URLMap.METHODS_OMISSION);
            assertNotNull(output);
            String outputResult[] = getArray(new ArrayList<String>(output.keySet()));
            String expectedResult[] = METHOD.clone();
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));

            // then check the userdata value
            MethodConstraint outputMC[] = new MethodConstraint[output.size()];
            for (int i = 0; i < output.size(); i++) {
                outputMC[i] = output.get(outputResult[i]);
            }
            for (int i = 0; i < outputMC.length; i++) {
                assertTrue(outputMC[i].isUserDataNone());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setUserDataMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUserDataMapAsConfidentialWithAllMethodsToNormal() {
        final String methodName = "setUserDataMapAsConfidentialWithAllMethodsToNormal";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String USERDATA = "CONFIDENTIAL"; //the value should be CONFIDENTIAL, INTEGRAL, or NONE. No value check in MethodConstraint class though
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUserDataMap(USERDATA, null);
        try {
            // first, check the methods are correct
            Map<String, MethodConstraint> output = urlmap.getUserDataMap(URLMap.METHODS_ALL);
            assertNotNull(output);
            String outputResult[] = getArray(new ArrayList<String>(output.keySet()));
            String expectedResult[] = { "ALLMETHODS" };
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));

            // then check the userdata value
            MethodConstraint outputMC[] = new MethodConstraint[output.size()];
            for (int i = 0; i < output.size(); i++) {
                outputMC[i] = output.get(outputResult[i]);
            }
            for (int i = 0; i < outputMC.length; i++) {
                String userData = outputMC[i].getUserData();
                assertEquals(USERDATA, userData);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests setUserDataMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUserDataMapAsIntegralWithAllMethodsToOmission() {
        final String methodName = "setUserDataMapAsIntegralWithAllMethodsToOmission";
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String USERDATA = "INTEGRAL"; //the value should be CONFIDENTIAL, INTEGRAL, or NONE. No value check in MethodConstraint class though
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUserDataMap(USERDATA, null, true);
        try {
            // first, check the methods are correct
            Map<String, MethodConstraint> output = urlmap.getUserDataMap(URLMap.METHODS_ALL);
            assertNotNull(output);
            String outputResult[] = getArray(new ArrayList<String>(output.keySet()));
            String expectedResult[] = { "ALLMETHODS" };
            Arrays.sort(expectedResult);
            assertTrue(Arrays.equals(expectedResult, outputResult));

            // then check the userdata value
            MethodConstraint outputMC[] = new MethodConstraint[output.size()];
            for (int i = 0; i < output.size(); i++) {
                outputMC[i] = output.get(outputResult[i]);
            }
            for (int i = 0; i < outputMC.length; i++) {
                String userData = outputMC[i].getUserData();
                assertEquals(USERDATA, userData);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        return;
    }

    /**
     * Tests getUserDataStringFromAllMap method
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void getUserDataStringFromAllMapNull() {
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        assertNull(urlmap.getUserDataStringFromAllMap(null, "REST"));
        assertNull(urlmap.getUserDataStringFromAllMap(new HashMap<String, MethodConstraint>(), null));
    }

    /**
     * Tests getUserDataStringFromAllMap method
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void getUserDataStringFromAllMapRest() {
        String METHOD[] = { "REST" };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        MethodConstraint mc = new MethodConstraint();
        mc.setExcluded();
        Map<String, MethodConstraint> allMap = new HashMap<String, MethodConstraint>();
        allMap.put("REST", mc);
        assertNotNull(urlmap);
        urlmap.setExcludedSet(mList);
        assertNull(urlmap.getUserDataStringFromAllMap(allMap, "REST"));
    }

    /**
     * Tests getUserDataStringFromAllMap method
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void getUserDataStringFromAllMapNone() {
        String method = "GET";
        String METHOD[] = { method };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        MethodConstraint mc = new MethodConstraint();
        mc.setUnchecked();
        mc.setUserData("NONE");
        Map<String, MethodConstraint> allMap = new HashMap<String, MethodConstraint>();
        allMap.put(method, mc);
        assertNotNull(urlmap);
        urlmap.setExcludedSet(mList);
        assertNull(urlmap.getUserDataStringFromAllMap(allMap, method));
    }

    /**
     * Tests getUserDataStringFromAllMap method
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void getUserDataStringFromAllMapConfidential() {
        String method = "GET";
        String ud = "INTEGRAL";
        String METHOD[] = { method };
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        MethodConstraint mc = new MethodConstraint();
        mc.setUnchecked();
        mc.setUserData(ud);
        Map<String, MethodConstraint> allMap = new HashMap<String, MethodConstraint>();
        allMap.put(method, mc);
        assertNotNull(urlmap);
        urlmap.setExcludedSet(mList);
        assertEquals(":CONFIDENTIAL", urlmap.getUserDataStringFromAllMap(allMap, "CONFIDENTIAL_OR_INTEGRAL").getActions());
        assertEquals(":INTEGRAL", urlmap.getUserDataStringFromAllMap(allMap, ud).getActions());
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void getExcludedStringWithMethodsToNormal() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        String EXCLUDED_RESULT = "GET,PUT";
        String UNCHECKED_RESULT = "!GET,PUT";
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setExcludedSet(mList);
        // first, check the output is correct
        ActionString output = urlmap.getExcludedString();
        assertNotNull(output);
        assertEquals(EXCLUDED_RESULT, output.getActions());
        // then checks whether other outputs are fine.
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        Map<String, String> role = urlmap.getRoleMap();
        // no role is defined.
        assertNull(role);
        return;
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void getExcludedStringWithMethodsToOmission() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        String EXCLUDED_RESULT = "!GET,PUT";
        String UNCHECKED_RESULT = "GET,PUT";
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setExcludedSet(mList, true);
        // first, check the output is correct
        ActionString output = urlmap.getExcludedString();
        assertNotNull(output);
        assertEquals(EXCLUDED_RESULT, output.getActions());
        // then checks whether other outputs are fine.
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());
        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        Map<String, String> role = urlmap.getRoleMap();
        // no role is defined.
        assertNull(role);
        return;
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void roleStringWithMethodsToNormal() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        String ROLE = "ROLE1";
        String UNCHECKED_RESULT = "!GET,PUT";
        String ROLE_METHODS_RESULT = "GET,PUT";
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE, mList);
        // first, check the output is correct
        ActionString output = urlmap.getExcludedString();
        assertNull(output);
        // then checks whether other outputs are fine.
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(1, role.size());
        assertTrue(role.containsKey(ROLE));
        assertEquals(ROLE_METHODS_RESULT, role.get(ROLE));

        return;
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void roleStringWithMethodsToOmission() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        String ROLE = "ROLE1";
        String UNCHECKED_RESULT = "GET,PUT";
        String ROLE_METHODS_RESULT = "!GET,PUT";
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE, mList, true);
        // first, check the output is correct
        ActionString output = urlmap.getExcludedString();
        assertNull(output);
        // then checks whether other outputs are fine.
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(1, role.size());
        assertTrue(role.containsKey(ROLE));
        assertEquals(ROLE_METHODS_RESULT, role.get(ROLE));

        return;
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void roleStringWithAllMethodsToNormal() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String ROLE = "ROLEROLE1";
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE, null);
        // first, check the output is correct
        ActionString output = urlmap.getExcludedString();
        assertNull(output);

        // then checks whether other outputs are fine.
        output = urlmap.getUncheckedString();
        assertNull(output);

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(1, role.size());
        assertTrue(role.containsKey(ROLE));
        assertNull(role.get(ROLE));

        return;
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void userDataStringWithMethodsToNormal() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        String USERDATA = "CONFIDENTIAL";
        String USERDATA_RESULT = "GET,PUT:CONFIDENTIAL";
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUserDataMap(USERDATA, mList);
        // first, check the output is correct
        ActionString output = urlmap.getExcludedString();
        assertNull(output);
        // then checks whether other outputs are fine.
        output = urlmap.getUncheckedString();
        assertNull(output);

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(USERDATA_RESULT, output.getActions());

        Map<String, String> role = urlmap.getRoleMap();
        assertNull(role);

        return;
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void userDataStringWithMethodsToOmission() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "GET", "PUT" };
        String USERDATA = "CONFIDENTIAL";
        String USERDATA_RESULT = "!GET,PUT:CONFIDENTIAL";
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUserDataMap(USERDATA, mList, true);
        // first, check the output is correct
        ActionString output = urlmap.getExcludedString();
        assertNull(output);
        // then checks whether other outputs are fine.
        output = urlmap.getUncheckedString();
        assertNull(output);

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(USERDATA_RESULT, output.getActions());

        Map<String, String> role = urlmap.getRoleMap();
        assertNull(role);

        return;
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void roleStringWithMethodsComplex() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD_NORMAL[] = { "GET", "PUT" };
        String METHOD_OMISSION[] = { "CUSTOM", "TRACE" };
        String METHOD_EXCLUDED_NORMAL[] = { "POST" };
        String ROLE_NORMAL = "NORMAL_ROLE";
        String ROLE_OMISSION = "OMISSION_ROLE";
        String EXCLUDED_RESULT = "POST";
        String UNCHECKED_RESULT = "CUSTOM,TRACE";
        String ROLE_METHODS_NORMAL_RESULT = "GET,PUT";
        String ROLE_METHODS_OMISSION_RESULT = "!CUSTOM,TRACE";

        List<String> mList_normal = new ArrayList<String>(Arrays.asList(METHOD_NORMAL));
        List<String> mList_omission = new ArrayList<String>(Arrays.asList(METHOD_OMISSION));
        List<String> mList_excluded_normal = new ArrayList<String>(Arrays.asList(METHOD_EXCLUDED_NORMAL));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_NORMAL, mList_normal);
        urlmap.setRoleMap(ROLE_OMISSION, mList_omission, true);
        urlmap.setExcludedSet(mList_excluded_normal);
        // first, check the output is correct
        ActionString output = urlmap.getExcludedString();
        assertNotNull(output);
        assertEquals(EXCLUDED_RESULT, output.getActions());
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEqualsOrderIndependent(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(2, role.size());
        assertTrue(role.containsKey(ROLE_NORMAL));
        assertTrue(role.containsKey(ROLE_OMISSION));
        assertEqualsOrderIndependent(ROLE_METHODS_NORMAL_RESULT, role.get(ROLE_NORMAL));
        assertEqualsOrderIndependent(ROLE_METHODS_OMISSION_RESULT, role.get(ROLE_OMISSION));
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void excludedStringWithMethodsComplex() {
        // this UT is for defect 644529
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_OMISSION[] = { "POST" };
        String METHOD_EXCLUDED_NORMAL[] = { "POST" };
        String ROLE_OMISSION = "OMISSION_ROLE";
        String EXCLUDED_RESULT = "POST";
        String ROLE_METHODS_OMISSION_RESULT = "!POST";

        List<String> mList_omission = new ArrayList<String>(Arrays.asList(METHOD_OMISSION));
        List<String> mList_excluded_normal = new ArrayList<String>(Arrays.asList(METHOD_EXCLUDED_NORMAL));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_OMISSION, mList_omission, true);
        urlmap.setExcludedSet(mList_excluded_normal);
        // first, check the output is correct
        ActionString output = urlmap.getExcludedString();
        assertNotNull(output);
        assertEquals(EXCLUDED_RESULT, output.getActions());
        output = urlmap.getUncheckedString();
        assertNull(output);

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(1, role.size());
        assertTrue(role.containsKey(ROLE_OMISSION));
        assertEquals(ROLE_METHODS_OMISSION_RESULT, role.get(ROLE_OMISSION));
        return;
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void userDataStringWithMethodsToOmissionIntegral() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD[] = { "POST" };
        String USERDATA = "INTEGRAL";
        String USERDATA_RESULT = "!POST:CONFIDENTIAL";
        String UNCHECKED_RESULT = "POST";
        List<String> mList = new ArrayList<String>(Arrays.asList(METHOD));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUserDataMap(USERDATA, mList, true);
        // first, check the output is correct
        ActionString output = urlmap.getExcludedString();
        assertNull(output);
        // then checks whether other outputs are fine.
        output = urlmap.getUncheckedString();
        assertNull(output);
        // check confidential user data
        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(USERDATA_RESULT, output.getActions());
        // check unchecked user data
        output = urlmap.getUserDataString("REST");
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());
        return;
    }

    /**
     * Tests getExcludedString, getUncheckedString, getUserDataString, getRoleMap
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void userDataStringWithMethodsComplex() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role
        String METHOD_NORMAL[] = { "GET" };
        String METHOD_OMISSION[] = { "GET", "POST" };
        String METHOD_EXCLUDED_NORMAL[] = { "POST" };
        String ROLE_NORMAL = "NORMAL_ROLE";
        String EXCLUDED_RESULT = "POST";
        String REST_RESULT = "!POST";

        List<String> mList_normal = new ArrayList<String>(Arrays.asList(METHOD_NORMAL));
        List<String> mList_omission = new ArrayList<String>(Arrays.asList(METHOD_OMISSION));
        List<String> mList_excluded_normal = new ArrayList<String>(Arrays.asList(METHOD_EXCLUDED_NORMAL));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setUncheckedSet(mList_omission, true);
        urlmap.setRoleMap(ROLE_NORMAL, mList_normal);
        urlmap.setExcludedSet(mList_excluded_normal);

        String USERDATA = "NONE";
        urlmap.setUserDataMap(USERDATA, mList_omission, true);

        ActionString output = urlmap.getExcludedString();
        assertNotNull(output);
        assertEquals(EXCLUDED_RESULT, output.getActions());

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(REST_RESULT, output.getActions());

        return;
    }

    /***************************************************************************
     * Test: testComplex1a()
     * << INPUT >>
     * <security-constraint id="SecurityConstraint_1">
     * <web-resource-collection id="WebResourceCollection_1">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>POST</http-method-omission>
     * <http-method-omission>GET</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>CONTRACTOR</role-name>
     * </auth-constraint>
     * <user-data-constraint>
     * <transport-guarantee>CONFIDENTIAL</transport-guarantee>
     * </user-data-constraint>
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_2">
     * <web-resource-collection id="WebResourceCollection_2">
     * <web-resource-name>wholesale</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method>POST</http-method>
     * <http-method>GET</http-method>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>ROLE1</role-name>
     * </auth-constraint>
     * <user-data-constraint>
     * <transport-guarantee>CONFIDENTIAL</transport-guarantee>
     * </user-data-constraint>
     * </security-constraint>
     *
     *
     * << OUTPUT >>
     * Role:
     * [2/19/10 19:52:42:109 EST] 0000000c AppInstallNot 3 Added the role: CONTRACTOR
     * URL: /c/*
     * method: !POST,GET to the permission (javax.security.jacc.WebResourcePermission /c/* !GET,POST)
     * [2/19/10 19:52:42:109 EST] 0000000c AppInstallNot 3 Added the role: ROLE1
     * URL: /c/*
     * method: POST,GET to the permission (javax.security.jacc.WebResourcePermission /c/* GET,POST)
     *
     * Excluded: None
     *
     * Unchecked:
     * (javax.security.jacc.WebResourcePermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /c/* CONFIDENTIAL)
     * (javax.security.jacc.WebUserDataPermission /:/c/*)
     ******************************************************************************/
    @Test
    public void testComplex1a() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_NORMAL[] = { "POST", "GET" };
        String METHOD_OMISSION[] = { "POST", "GET" };
        String ROLE_NORMAL = "NORMAL_ROLE";
        String ROLE_OMISSION = "OMISSION_ROLE";
        String ROLE_METHODS_NORMAL_RESULT = "POST,GET";
        String ROLE_METHODS_OMISSION_RESULT = "!POST,GET";
        String CONF_RESULT = ":CONFIDENTIAL";

        List<String> mList_normal = new ArrayList<String>(Arrays.asList(METHOD_NORMAL));
        List<String> mList_omission = new ArrayList<String>(Arrays.asList(METHOD_OMISSION));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_OMISSION, mList_omission, true);
        urlmap.setRoleMap(ROLE_NORMAL, mList_normal);

        String USERDATA = "CONFIDENTIAL";
        urlmap.setUserDataMap(USERDATA, mList_omission, true);
        urlmap.setUserDataMap(USERDATA, mList_normal);

        // role
        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(2, role.size());
        assertTrue(role.containsKey(ROLE_NORMAL));
        assertEquals(ROLE_METHODS_NORMAL_RESULT, role.get(ROLE_NORMAL));
        assertTrue(role.containsKey(ROLE_OMISSION));
        assertEquals(ROLE_METHODS_OMISSION_RESULT, role.get(ROLE_OMISSION));

        // excluded - null;
        ActionString output = urlmap.getExcludedString();
        assertNull(output);

        // unchecked
        output = urlmap.getUncheckedString();
        assertNull(output);

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(CONF_RESULT, output.getActions());

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        return;
    }

    /***************************************************************************
     * testComplexb1
     * << input >>
     * <security-constraint id="SecurityConstraint_4">
     * <web-resource-collection id="WebResourceCollection_4">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>POST</http-method-omission>
     * <http-method-omission>GET</http-method-omission>
     * </web-resource-collection>
     *
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_3">
     * <web-resource-collection id="WebResourceCollection_3">
     * <web-resource-name>wholesale</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method>POST</http-method>
     * <http-method>GET</http-method>
     * </web-resource-collection>
     * </security-constraint>
     *
     * <<OUTPUT>>
     * only unchecked:
     *
     * (javax.security.jacc.WebResourcePermission /c/*)
     * (javax.security.jacc.WebResourcePermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /c/*)
     ******************************************************************************/
    @Test
    public void testComplex1b() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_NORMAL[] = { "POST", "GET" };
        String METHOD_OMISSION[] = { "POST", "GET" };
        String REST_RESULT = ":NONE";

        List<String> mList_normal = new ArrayList<String>(Arrays.asList(METHOD_NORMAL));
        List<String> mList_omission = new ArrayList<String>(Arrays.asList(METHOD_OMISSION));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);

        String USERDATA = "NONE";
        urlmap.setUserDataMap(USERDATA, mList_omission, true);
        urlmap.setUserDataMap(USERDATA, mList_normal);

        // role
        Map<String, String> role = urlmap.getRoleMap();
        assertNull(role);

        // excluded - null;
        ActionString output = urlmap.getExcludedString();
        assertNull(output);

        // unchecked
        output = urlmap.getUncheckedString();
        assertNull(output);

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(REST_RESULT, output.getActions());

        return;
    }

    /***************************************************************************
     * <<INPUT>>
     * <security-constraint id="SecurityConstraint_4">
     * <web-resource-collection id="WebResourceCollection_4">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>CONTRACTOR</role-name>
     * </auth-constraint>
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_3">
     * <web-resource-collection id="WebResourceCollection_3">
     * <web-resource-name>wholesale</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     *
     * <auth-constraint>
     * <role-name>ROLE1</role-name>
     * </auth-constraint>
     * <user-data-constraint>
     * <transport-guarantee>CONFIDENTIAL</transport-guarantee>
     * </user-data-constraint>
     * </security-constraint>
     *
     * <<OUTPUT>>
     * [2/19/10 17:43:23:421 EST] 0000000b AppInstallNot 3 Added the role: CONTRACTOR
     * URL: /c/*
     * method: !POST to the permission (javax.security.jacc.WebResourcePermission /c/* !POST)
     * [2/19/10 17:43:23:421 EST] 0000000b AppInstallNot 3 Added the role: ROLE1
     * URL: /c/*
     * method: !POST to the permission (javax.security.jacc.WebResourcePermission /c/* !POST)
     * [2/19/10 17:43:23:437 EST] 0000000b AppInstallNot 3 Added the following ExcludedPerms:java.security.Permissions@8ea08ea (
     * )
     *
     * [2/19/10 17:43:23:437 EST] 0000000b AppInstallNot 3 Added the following UncheckedPerms:java.security.Permissions@8d708d7 (
     * (javax.security.jacc.WebUserDataPermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /c/*)
     * (javax.security.jacc.WebResourcePermission /:/c/*)
     * (javax.security.jacc.WebResourcePermission /c/* POST)
     ******************************************************************************/
    @Test
    public void testComplex9() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_OMISSION[] = { "POST" };
        String ROLE_OMISSION1 = "OMISSION_ROLE1";
        String ROLE_OMISSION2 = "OMISSION_ROLE2";
        String ROLE_METHODS_OMISSION_RESULT = "!POST";
        String UNCHECKED_RESULT = "POST";
        String REST_RESULT = ":NONE";

        List<String> mList_omission = new ArrayList<String>(Arrays.asList(METHOD_OMISSION));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_OMISSION1, mList_omission, true);
        urlmap.setRoleMap(ROLE_OMISSION2, mList_omission, true);

        String USERDATA = "NONE";
        urlmap.setUserDataMap(USERDATA, mList_omission, true);
        urlmap.setUserDataMap("CONFIDENTIAL", mList_omission, true);

        // role
        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(2, role.size());
        assertTrue(role.containsKey(ROLE_OMISSION1));
        assertEquals(ROLE_METHODS_OMISSION_RESULT, role.get(ROLE_OMISSION1));
        assertTrue(role.containsKey(ROLE_OMISSION2));
        assertEquals(ROLE_METHODS_OMISSION_RESULT, role.get(ROLE_OMISSION2));

        // excluded - null;
        ActionString output = urlmap.getExcludedString();
        assertNull(output);

        // unchecked
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(REST_RESULT, output.getActions());

        return;
    }

    /***************************************************************************
     * <<INPUT>>
     * <security-constraint id="SecurityConstraint_4">
     * <web-resource-collection id="WebResourceCollection_4">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>POST</http-method-omission>
     * <http-method-omission>GET</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>CONTRACTOR</role-name>
     * </auth-constraint>
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_3">
     * <web-resource-collection id="WebResourceCollection_3">
     * <web-resource-name>wholesale</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>ROLE1</role-name>
     * </auth-constraint>
     * <user-data-constraint>
     * <transport-guarantee>CONFIDENTIAL</transport-guarantee>
     * </user-data-constraint>
     * </security-constraint>
     *
     * <<OUTPUT>>
     * [2/19/10 17:32:26:359 EST] 0000000e AppInstallNot 3 Added the role: CONTRACTOR
     * URL: /c/*
     * method: !POST,GET to the permission (javax.security.jacc.WebResourcePermission /c/* !GET,POST)
     * [2/19/10 17:32:26:359 EST] 0000000e AppInstallNot 3 role is ROLE1
     * [2/19/10 17:32:26:359 EST] 0000000e AppInstallNot 3 Added the role: ROLE1
     * URL: /c/*
     * method: !POST to the permission (javax.security.jacc.WebResourcePermission /c/* !POST)
     * [2/19/10 17:32:26:375 EST] 0000000e AppInstallNot 3 Added the following ExcludedPerms:java.security.Permissions@42684268 (
     * )
     *
     * [2/19/10 17:32:26:375 EST] 0000000e AppInstallNot 3 Added the following UncheckedPerms:java.security.Permissions@42554255 (
     * (javax.security.jacc.WebUserDataPermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /c/* !GET)
     * (javax.security.jacc.WebUserDataPermission /c/* GET:CONFIDENTIAL)
     * (javax.security.jacc.WebResourcePermission /:/c/*)
     * (javax.security.jacc.WebResourcePermission /c/* POST)
     ******************************************************************************/
    @Test
    public void testComplex9a() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_OMISSION1[] = { "POST", "GET" };
        String METHOD_OMISSION2[] = { "POST" };
        String ROLE_OMISSION1 = "OMISSION_ROLE1";
        String ROLE_OMISSION2 = "OMISSION_ROLE2";
        String ROLE_METHODS_OMISSION_RESULT1 = "!POST,GET";
        String ROLE_METHODS_OMISSION_RESULT2 = "!POST";
        String UNCHECKED_RESULT = "POST";
        String CONF_RESULT = "GET:CONFIDENTIAL";
        String REST_RESULT = "!GET";

        List<String> mList_omission1 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION1));
        List<String> mList_omission2 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION2));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_OMISSION1, mList_omission1, true);
        urlmap.setRoleMap(ROLE_OMISSION2, mList_omission2, true);

        String USERDATA1 = "NONE";
        String USERDATA2 = "CONFIDENTIAL";
        urlmap.setUserDataMap(USERDATA1, mList_omission1, true);
        urlmap.setUserDataMap(USERDATA2, mList_omission2, true);

        // role
        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(2, role.size());
        assertTrue(role.containsKey(ROLE_OMISSION1));
        assertEqualsOrderIndependent(ROLE_METHODS_OMISSION_RESULT1, role.get(ROLE_OMISSION1));
        assertTrue(role.containsKey(ROLE_OMISSION2));
        assertEquals(ROLE_METHODS_OMISSION_RESULT2, role.get(ROLE_OMISSION2));

        // excluded - null;
        ActionString output = urlmap.getExcludedString();
        assertNull(output);

        // unchecked
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(CONF_RESULT, output.getActions());

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(REST_RESULT, output.getActions());

        return;
    }

    /***************************************************************************
     * <<INPUT>>
     * <security-constraint id="SecurityConstraint_3">
     * <web-resource-collection id="WebResourceCollection_3">
     * <web-resource-name>wholesale</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>GET</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * <http-method-omission>PUT</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>ROLE1</role-name>
     * </auth-constraint>
     *
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_4">
     * <web-resource-collection id="WebResourceCollection_4">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>GET</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint/>
     *
     * </security-constraint>
     *
     * <<OUTPUT>>
     * [2/18/10 10:18:57:140 EST] 0000000c AppInstallNot 3 Added the following ExcludedPerms:java.security.Permissions@77257725 (
     * (javax.security.jacc.WebResourcePermission /c/* !GET,POST)
     * (javax.security.jacc.WebUserDataPermission /c/* !GET,POST)
     * )
     * [2/18/10 10:18:57:140 EST] 0000000c AppInstallNot 3 Added the following UncheckedPerms:java.security.Permissions@77127712 (
     * (javax.security.jacc.WebResourcePermission /c/* GET,POST)
     * (javax.security.jacc.WebResourcePermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /c/* GET,POST)
     * )
     ******************************************************************************/
    @Test
    public void testComplex17() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_OMISSION1[] = { "GET", "POST", "PUT" };
        String METHOD_OMISSION2[] = { "GET", "POST" };
        String ROLE_OMISSION1 = "OMISSION_ROLE1";
        String EXCLUDED_RESULT = "!POST,GET";
        String UNCHECKED_RESULT = "POST,GET";
        String REST_RESULT = "POST,GET";

        List<String> mList_omission1 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION1));
        List<String> mList_omission2 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION2));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_OMISSION1, mList_omission1, true);
        urlmap.setExcludedSet(mList_omission2, true);

        String USERDATA1 = "NONE";
        urlmap.setUserDataMap(USERDATA1, mList_omission1, true);
        urlmap.setUserDataMap(USERDATA1, mList_omission2, true);

        // role
        Map<String, String> role = urlmap.getRoleMap();
        assertNull(role);

        // excluded - null;
        ActionString output = urlmap.getExcludedString();
        assertNotNull(output);
        assertEquals(EXCLUDED_RESULT, output.getActions());

        // unchecked
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(REST_RESULT, output.getActions());

        return;
    }

    /***************************************************************************
     * <<INPUT>>
     * <security-constraint id="SecurityConstraint_3">
     * <web-resource-collection id="WebResourceCollection_3">
     * <web-resource-name>wholesale</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>GET</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * <http-method-omission>PUT</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>ROLE1</role-name>
     * </auth-constraint>
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_4">
     * <web-resource-collection id="WebResourceCollection_4">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>GET</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint/>
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_5">
     * <web-resource-collection id="WebResourceCollection_5">
     * <web-resource-name>wholesale3</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method>POST</http-method>
     * </web-resource-collection>
     * <auth-constraint/>
     *
     * </security-constraint>
     *
     * <<OUTPUT>>
     * [2/18/10 10:18:57:140 EST] 0000000c AppInstallNot 3 Added the following ExcludedPerms:java.security.Permissions@77257725 (
     * (javax.security.jacc.WebResourcePermission /c/* !GET)
     * (javax.security.jacc.WebUserDataPermission /c/* !GET)
     * )
     * [2/18/10 10:18:57:140 EST] 0000000c AppInstallNot 3 Added the following UncheckedPerms:java.security.Permissions@77127712 (
     * (javax.security.jacc.WebResourcePermission /c/* GET)
     * (javax.security.jacc.WebResourcePermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /c/* GET)
     * )
     ******************************************************************************/
    @Test
    public void testComplex17a() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_OMISSION1[] = { "GET", "POST", "PUT" };
        String METHOD_OMISSION2[] = { "GET", "POST" };
        String METHOD_EXCLUDED_NORMAL[] = { "POST" };
        String ROLE_OMISSION1 = "OMISSION_ROLE1";
        String EXCLUDED_RESULT = "!GET";
        String UNCHECKED_RESULT = "GET";
        String REST_RESULT = "GET";

        List<String> mList_omission1 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION1));
        List<String> mList_omission2 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION2));
        List<String> mList_excluded_normal = new ArrayList<String>(Arrays.asList(METHOD_EXCLUDED_NORMAL));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_OMISSION1, mList_omission1, true);
        urlmap.setExcludedSet(mList_omission2, true);
        urlmap.setExcludedSet(mList_excluded_normal);

        String USERDATA1 = "NONE";
        urlmap.setUserDataMap(USERDATA1, mList_omission1, true);
        urlmap.setUserDataMap(USERDATA1, mList_omission2, true);

        // role
        Map<String, String> role = urlmap.getRoleMap();
        assertNull(role);

        // excluded - null;
        ActionString output = urlmap.getExcludedString();
        assertNotNull(output);
        assertEquals(EXCLUDED_RESULT, output.getActions());

        // unchecked
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(REST_RESULT, output.getActions());

        return;
    }

    /***************************************************************************
     * <<INPUT>>
     * <security-constraint id="SecurityConstraint_3">
     * <web-resource-collection id="WebResourceCollection_3">
     * <web-resource-name>wholesale</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>GET</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * <http-method-omission>PUT</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>ROLE1</role-name>
     * </auth-constraint>
     * <user-data-constraint>
     * <transport-guarantee>CONFIDENTIAL</transport-guarantee>
     * </user-data-constraint>
     *
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_4">
     * <web-resource-collection id="WebResourceCollection_4">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>GET</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint/>
     *
     * </security-constraint>
     *
     * <<OUTPUT>>
     * [2/18/10 10:18:57:140 EST] 0000000c AppInstallNot 3 Added the following ExcludedPerms:java.security.Permissions@77257725 (
     * (javax.security.jacc.WebResourcePermission /c/* !GET,POST)
     * (javax.security.jacc.WebUserDataPermission /c/* !GET,POST)
     * )
     * [2/18/10 10:18:57:140 EST] 0000000c AppInstallNot 3 Added the following UncheckedPerms:java.security.Permissions@77127712 (
     * (javax.security.jacc.WebResourcePermission /c/* GET,POST)
     * (javax.security.jacc.WebResourcePermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /c/* GET,POST)
     * )
     ******************************************************************************/
    @Test
    public void testComplex17d() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_OMISSION1[] = { "GET", "POST", "PUT" };
        String METHOD_OMISSION2[] = { "GET", "POST" };
        String ROLE_OMISSION1 = "OMISSION_ROLE1";
        String EXCLUDED_RESULT = "!POST,GET";
        String UNCHECKED_RESULT = "POST,GET";
        String REST_RESULT = "POST,GET";

        List<String> mList_omission1 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION1));
        List<String> mList_omission2 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION2));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_OMISSION1, mList_omission1, true);
        urlmap.setExcludedSet(mList_omission2, true);

        String USERDATA1 = "CONFIDENTIAL";
        String USERDATA2 = "NONE";
        urlmap.setUserDataMap(USERDATA1, mList_omission1, true);
        urlmap.setUserDataMap(USERDATA2, mList_omission2, true);

        // role
        Map<String, String> role = urlmap.getRoleMap();
        assertNull(role);

        // excluded - null;
        ActionString output = urlmap.getExcludedString();
        assertNotNull(output);
        assertEquals(EXCLUDED_RESULT, output.getActions());

        // unchecked
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(REST_RESULT, output.getActions());

        return;
    }

    /***************************************************************************
     * <<INPUT>>
     * <security-constraint id="SecurityConstraint_3">
     * <web-resource-collection id="WebResourceCollection_3">
     * <web-resource-name>wholesale</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>GET</http-method-omission>
     * <http-method-omission>PUT</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>ROLE1</role-name>
     * </auth-constraint>
     * <user-data-constraint>
     * <transport-guarantee>CONFIDENTIAL</transport-guarantee>
     * </user-data-constraint>
     *
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_4">
     * <web-resource-collection id="WebResourceCollection_4">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>CUSTOM</http-method-omission>
     * <http-method-omission>TRACE</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint/>
     * </security-constraint>
     *
     * <<OUTPUT>>
     * [4/7/10 13:00:06:984 EDT] 0000000e AppInstallNot 3 Added the role: ROLE1
     * URL: /c/*
     * method: !POST,GET,PUT to the permission (javax.security.jacc.WebResourcePermission /c/* !GET,POST,PUT)
     * )
     * [4/7/10 13:00:06:984 EDT] 0000000e AppInstallNot 3 Added the following ExcludedPerms:java.security.Permissions@71cf71cf (
     * (javax.security.jacc.WebUserDataPermission /c/* !POST,TRACE,CUSTOM)
     * (javax.security.jacc.WebResourcePermission /c/* !POST,TRACE,CUSTOM)
     * )
     * [4/7/10 13:00:06:984 EDT] 0000000e AppInstallNot 3 Added the following UncheckedPerms:java.security.Permissions@71bc71bc (
     * (javax.security.jacc.WebUserDataPermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /c/* POST,TRACE,CUSTOM)
     * (javax.security.jacc.WebResourcePermission /:/c/*)
     * (javax.security.jacc.WebResourcePermission /c/* POST)
     ******************************************************************************/
    @Test
    public void testComplex17e() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_OMISSION1[] = { "GET", "PUT", "POST" };
        String METHOD_OMISSION2[] = { "CUSTOM", "TRACE", "POST" };
        String ROLE_OMISSION1 = "OMISSION_ROLE1";
        String ROLE_METHODS_OMISSION_RESULT1 = "!POST,GET,PUT";
        String EXCLUDED_RESULT = "!POST,CUSTOM,TRACE";
        String UNCHECKED_RESULT = "POST";
        String REST_RESULT = "POST,CUSTOM,TRACE";

        List<String> mList_omission1 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION1));
        List<String> mList_omission2 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION2));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_OMISSION1, mList_omission1, true);
        urlmap.setExcludedSet(mList_omission2, true);

        String USERDATA1 = "CONFIDENTIAL";
        urlmap.setUserDataMap(USERDATA1, mList_omission1, true);

        // role
        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(1, role.size());
        assertTrue(role.containsKey(ROLE_OMISSION1));
        assertEqualsOrderIndependent(ROLE_METHODS_OMISSION_RESULT1, role.get(ROLE_OMISSION1));

        // excluded - null;
        ActionString output = urlmap.getExcludedString();
        assertNotNull(output);
        assertEqualsOrderIndependent(EXCLUDED_RESULT, output.getActions());

        // unchecked
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEqualsOrderIndependent(REST_RESULT, output.getActions());

        return;
    }

    /***************************************************************************
     * <<INPUT>>
     * <security-constraint id="SecurityConstraint_3">
     * <web-resource-collection id="WebResourceCollection_3">
     * <web-resource-name>wholesale</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>GET</http-method-omission>
     * <http-method-omission>PUT</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>ROLE1</role-name>
     * </auth-constraint>
     * <user-data-constraint>
     * <transport-guarantee>CONFIDENTIAL</transport-guarantee>
     * </user-data-constraint>
     *
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_4">
     * <web-resource-collection id="WebResourceCollection_4">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>CUSTOM</http-method-omission>
     * <http-method-omission>TRACE</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * </security-constraint>
     *
     * <<OUTPUT>>
     * [4/7/10 13:00:06:984 EDT] 0000000e AppInstallNot 3 Added the role: ROLE1
     * URL: /c/*
     * method: !POST,GET,PUT to the permission (javax.security.jacc.WebResourcePermission /c/* !GET,POST,PUT)
     * )
     * [4/7/10 13:00:06:984 EDT] 0000000e AppInstallNot 3 Added the following ExcludedPerms:java.security.Permissions@71cf71cf (
     * )
     * [4/7/10 13:00:06:984 EDT] 0000000e AppInstallNot 3 Added the following UncheckedPerms:java.security.Permissions@71bc71bc (
     * (javax.security.jacc.WebUserDataPermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /c/* !TRACE,CUSTOM)
     * (javax.security.jacc.WebUserDataPermission /c/* TRACE,CUSTOM:CONFIDENTIAL)
     * (javax.security.jacc.WebResourcePermission /:/c/*)
     * (javax.security.jacc.WebResourcePermission /c/* GET,POST,PUT) <-- should have this
     ******************************************************************************/
    @Test
    public void testComplex17f() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_OMISSION1[] = { "GET", "PUT", "POST" };
        String METHOD_OMISSION2[] = { "CUSTOM", "TRACE", "POST" };
        String ROLE_OMISSION1 = "OMISSION_ROLE1";
        String ROLE_METHODS_OMISSION_RESULT1 = "!POST,GET,PUT";
        String UNCHECKED_RESULT = "POST,GET,PUT";
        String CONF_RESULT = "CUSTOM,TRACE:CONFIDENTIAL";
        String REST_RESULT = "!CUSTOM,TRACE";

        List<String> mList_omission1 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION1));
        List<String> mList_omission2 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION2));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_OMISSION1, mList_omission1, true);

        String USERDATA1 = "CONFIDENTIAL";
        String USERDATA2 = "NONE";
        urlmap.setUserDataMap(USERDATA1, mList_omission1, true);
        urlmap.setUserDataMap(USERDATA2, mList_omission2, true);

        // role
        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(1, role.size());
        assertTrue(role.containsKey(ROLE_OMISSION1));
        assertEqualsOrderIndependent(ROLE_METHODS_OMISSION_RESULT1, role.get(ROLE_OMISSION1));

        // excluded - null;
        ActionString output = urlmap.getExcludedString();
        assertNull(output);

        // unchecked
        output = urlmap.getUncheckedString();
        assertNotNull(output);
        assertEquals(UNCHECKED_RESULT, output.getActions());

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEqualsOrderIndependent(CONF_RESULT, output.getActions());

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEqualsOrderIndependent(REST_RESULT, output.getActions());

        return;
    }

    /***************************************************************************
     * <<INPUT>>
     * <security-constraint id="SecurityConstraint_3">
     * <web-resource-collection id="WebResourceCollection_3">
     * <web-resource-name>wholesale</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>GET</http-method-omission>
     * <http-method-omission>PUT</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint>
     * <role-name>ROLE1</role-name>
     * </auth-constraint>
     * <user-data-constraint>
     * <transport-guarantee>CONFIDENTIAL</transport-guarantee>
     * </user-data-constraint>
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_4">
     * <web-resource-collection id="WebResourceCollection_4">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>CUSTOM</http-method-omission>
     * <http-method-omission>TRACE</http-method-omission>
     * <http-method-omission>POST</http-method-omission>
     * </web-resource-collection>
     * </security-constraint>
     *
     * <security-constraint id="SecurityConstraint_5">
     * <web-resource-collection id="WebResourceCollection_5">
     * <web-resource-name>wholesale2</web-resource-name>
     * <url-pattern>/c/*</url-pattern>
     * <http-method-omission>OPTIONS</http-method-omission>
     * </web-resource-collection>
     * <auth-constraint/>
     * </security-constraint>
     *
     * <<OUTPUT>>
     * [4/7/10 13:00:06:984 EDT] 0000000e AppInstallNot 3 Added the role: ROLE1
     * URL: /c/*
     * method: !POST,GET,PUT to the permission (javax.security.jacc.WebResourcePermission /c/* !GET,POST,PUT)
     * )
     * [4/7/10 13:59:16:390 EDT] 0000000d WSPolicyConfi > addToExcludedPolicy Entry
     * java.security.Permissions@297c297c (
     * (javax.security.jacc.WebUserDataPermission /c/* !OPTIONS)
     * (javax.security.jacc.WebResourcePermission /c/* !OPTIONS)
     * )
     * [4/7/10 13:59:16:390 EDT] 0000000d AppInstallNot 3 Added the following UncheckedPerms:java.security.Permissions@29692969 (
     * (javax.security.jacc.WebUserDataPermission /:/c/*)
     * (javax.security.jacc.WebUserDataPermission /c/*)
     * (javax.security.jacc.WebResourcePermission /:/c/*)
     * )
     ******************************************************************************/
    @Test
    public void testComplex17g() {
        //since there is no getter methods to validate whether setUncheckedSet works property, use following way:
        // set some role

        String METHOD_OMISSION1[] = { "GET", "PUT", "POST" };
        String METHOD_OMISSION2[] = { "CUSTOM", "TRACE", "POST" };
        String METHOD_OMISSION3[] = { "OPTIONS" };
        String ROLE_OMISSION1 = "OMISSION_ROLE1";
        String ROLE_METHODS_OMISSION_RESULT1 = "!POST,GET,PUT";
        String EXCLUDED_RESULT = "!OPTIONS";
        String REST_RESULT = ":NONE";

        List<String> mList_omission1 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION1));
        List<String> mList_omission2 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION2));
        List<String> mList_omission3 = new ArrayList<String>(Arrays.asList(METHOD_OMISSION3));
        URLMap urlmap = new URLMap(URL_PATH_1);
        assertNotNull(urlmap);
        urlmap.setRoleMap(ROLE_OMISSION1, mList_omission1, true);
        urlmap.setExcludedSet(mList_omission3, true);

        String USERDATA1 = "CONFIDENTIAL";
        String USERDATA2 = "NONE";
        urlmap.setUserDataMap(USERDATA1, mList_omission1, true);
        urlmap.setUserDataMap(USERDATA2, mList_omission2, true);

        // role
        Map<String, String> role = urlmap.getRoleMap();
        assertEquals(1, role.size());
        assertTrue(role.containsKey(ROLE_OMISSION1));
        assertEqualsOrderIndependent(ROLE_METHODS_OMISSION_RESULT1, role.get(ROLE_OMISSION1));

        // excluded - null;
        ActionString output = urlmap.getExcludedString();
        assertNotNull(output);
        assertEquals(EXCLUDED_RESULT, output.getActions());

        // unchecked
        output = urlmap.getUncheckedString();
        assertNull(output);

        output = urlmap.getUserDataString("CONFIDENTIAL"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNull(output);

        output = urlmap.getUserDataString("REST"); // for the output, INTEGRAL will be treated as CONFIDENTIAL
        assertNotNull(output);
        assertEquals(REST_RESULT, output.getActions());

        return;
    }

    // converts and sorts the contents of ArrayList to String array.
    private String[] getArray(List<String> input) {
        String output[] = null;
        if (input != null) {
            output = new String[input.size()];
            for (int i = 0; i < input.size(); i++) {
                output[i] = input.get(i);
            }
            Arrays.sort(output);
        }
        return output;
    }

}
