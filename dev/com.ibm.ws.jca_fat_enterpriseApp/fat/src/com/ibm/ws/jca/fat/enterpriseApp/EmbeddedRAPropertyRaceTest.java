/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.enterpriseApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

/**
 * Behavioral tests for the embedded RA property race condition fix.
 *
 * These tests verify the property-setting logic that BootstrapContextImpl.configureResourceAdapter()
 * and applyProperties() use: Java Beans introspection with null-skip behavior.
 *
 * The bug condition test verifies that incomplete properties cause the RA to retain ra.xml defaults
 * and that re-applying complete properties (as modified() does) corrects the RA state.
 *
 * The preservation tests verify that the non-buggy path (complete properties at activation time)
 * continues to work correctly.
 *
 * Integration-level validation is done by testEmbeddedRAConfigPropsApplied in EnterpriseAppTest,
 * which starts a Liberty server with an embedded RA and verifies the property is applied.
 */
public class EmbeddedRAPropertyRaceTest {

    /**
     * Minimal RA class that mimics an embedded resource adapter with multiple configurable
     * properties of different types. The default values simulate what ra.xml would set.
     */
    public static class FakeResourceAdapter {
        private String serverUrl = "tcp://localhost:61616";
        private String userName = "defaultUser";
        private String password = "defaultPass";
        private boolean enableLogging = false;
        private int maxConnections = 10;
        private String generalConfigProp = "unset";

        public String getServerUrl() { return serverUrl; }
        public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isEnableLogging() { return enableLogging; }
        public void setEnableLogging(boolean enableLogging) { this.enableLogging = enableLogging; }
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        public String getGeneralConfigProp() { return generalConfigProp; }
        public void setGeneralConfigProp(String generalConfigProp) { this.generalConfigProp = generalConfigProp; }
    }

    private static final Map<String, Object> SERVER_XML_PROPS = new HashMap<String, Object>();
    static {
        SERVER_XML_PROPS.put("serverUrl", "tcp://broker.prod:61616");
        SERVER_XML_PROPS.put("userName", "prodUser");
        SERVER_XML_PROPS.put("password", "prodPass");
        SERVER_XML_PROPS.put("enableLogging", Boolean.TRUE);
        SERVER_XML_PROPS.put("maxConnections", Integer.valueOf(50));
        SERVER_XML_PROPS.put("generalConfigProp", "PROP_SET");
    }

    private static final Map<String, Object> RA_XML_DEFAULTS = new HashMap<String, Object>();
    static {
        RA_XML_DEFAULTS.put("serverUrl", "tcp://localhost:61616");
        RA_XML_DEFAULTS.put("userName", "defaultUser");
        RA_XML_DEFAULTS.put("password", "defaultPass");
        RA_XML_DEFAULTS.put("enableLogging", Boolean.FALSE);
        RA_XML_DEFAULTS.put("maxConnections", Integer.valueOf(10));
        RA_XML_DEFAULTS.put("generalConfigProp", "unset");
    }

    private static final String[] CONFIGURABLE_PROPS = {
        "serverUrl", "userName", "password", "enableLogging", "maxConnections", "generalConfigProp"
    };

    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-:/";

    /**
     * Simulates the property-setting logic from BootstrapContextImpl.configureResourceAdapter():
     * iterate PropertyDescriptors, look up value in the properties dictionary, invoke setter
     * only if value != null.
     */
    private static List<String> applyProperties(FakeResourceAdapter instance, Dictionary<String, Object> props) throws Exception {
        List<String> appliedProps = new ArrayList<String>();
        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(instance.getClass()).getPropertyDescriptors()) {
            String name = descriptor.getName();
            Method writeMethod = descriptor.getWriteMethod();
            if (writeMethod == null)
                continue;
            Object value = props.get(name);
            if (value != null) {
                writeMethod.invoke(instance, value);
                appliedProps.add(name);
            }
        }
        return appliedProps;
    }

    private static Object getPropertyValue(FakeResourceAdapter instance, String propName) throws Exception {
        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(instance.getClass()).getPropertyDescriptors()) {
            if (descriptor.getName().equals(propName)) {
                Method readMethod = descriptor.getReadMethod();
                if (readMethod != null)
                    return readMethod.invoke(instance);
            }
        }
        return null;
    }

    private static String randomString(Random rng, int minLen, int maxLen) {
        int len = minLen + rng.nextInt(maxLen - minLen + 1);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(ALPHA.charAt(rng.nextInt(ALPHA.length())));
        return sb.toString();
    }

    private static Dictionary<String, Object> generateCompleteProps(long seed) {
        Random rng = new Random(seed);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("serverUrl", "tcp://" + randomString(rng, 5, 20) + ":" + (1024 + rng.nextInt(64000)));
        props.put("userName", randomString(rng, 3, 15));
        props.put("password", randomString(rng, 6, 20));
        props.put("enableLogging", Boolean.valueOf(rng.nextBoolean()));
        props.put("maxConnections", Integer.valueOf(1 + rng.nextInt(500)));
        props.put("generalConfigProp", randomString(rng, 1, 30));
        return props;
    }

    // =========================================================================
    // Bug Condition Tests — behavioral verification
    // =========================================================================

    /**
     * Verifies the bug condition behavior: when the properties dictionary is missing
     * user-configured values (simulating config admin race), the null-skip logic
     * causes the RA to retain ra.xml defaults for those properties.
     *
     * Then verifies the fix behavior: re-applying complete properties to the same
     * RA instance (as modified() does) correctly overrides the defaults.
     */
    @Test
    public void testBugCondition_IncompletePropertiesThenReapply() throws Exception {
        // Phase 1: Activate with incomplete properties (missing generalConfigProp)
        Hashtable<String, Object> incompleteProps = new Hashtable<String, Object>();
        incompleteProps.put("serverUrl", SERVER_XML_PROPS.get("serverUrl"));
        incompleteProps.put("userName", SERVER_XML_PROPS.get("userName"));
        // password, enableLogging, maxConnections, generalConfigProp are MISSING

        FakeResourceAdapter ra = new FakeResourceAdapter();
        applyProperties(ra, incompleteProps);

        // Present properties were applied
        assertEquals("tcp://broker.prod:61616", ra.getServerUrl());
        assertEquals("prodUser", ra.getUserName());

        // Missing properties retain ra.xml defaults
        assertEquals("defaultPass", ra.getPassword());
        assertEquals(false, ra.isEnableLogging());
        assertEquals(10, ra.getMaxConnections());
        assertEquals("unset", ra.getGeneralConfigProp());

        // Phase 2: Re-apply with complete properties (simulates modified() behavior)
        Hashtable<String, Object> completeProps = new Hashtable<String, Object>();
        for (String propName : CONFIGURABLE_PROPS)
            completeProps.put(propName, SERVER_XML_PROPS.get(propName));

        applyProperties(ra, completeProps);

        // All properties now have server.xml values
        assertEquals("tcp://broker.prod:61616", ra.getServerUrl());
        assertEquals("prodUser", ra.getUserName());
        assertEquals("prodPass", ra.getPassword());
        assertEquals(true, ra.isEnableLogging());
        assertEquals(50, ra.getMaxConnections());
        assertEquals("PROP_SET", ra.getGeneralConfigProp());
    }

    /**
     * Verifies that with various random subsets of missing properties, the RA
     * retains defaults for missing ones and gets correct values for present ones.
     * After re-applying complete properties, all values are correct.
     */
    @Test
    public void testBugCondition_RandomIncompleteSubsetsThenReapply() throws Exception {
        for (int trial = 0; trial < 10; trial++) {
            Random rng = new Random(42L + trial);
            Hashtable<String, Object> incompleteProps = new Hashtable<String, Object>();
            List<String> omitted = new ArrayList<String>();

            // Randomly include/exclude each property, ensuring at least one is omitted
            for (String propName : CONFIGURABLE_PROPS) {
                if (rng.nextBoolean() && omitted.size() == 0 || !rng.nextBoolean()) {
                    omitted.add(propName);
                } else {
                    incompleteProps.put(propName, SERVER_XML_PROPS.get(propName));
                }
            }
            if (omitted.isEmpty()) {
                omitted.add(CONFIGURABLE_PROPS[0]);
                incompleteProps.remove(CONFIGURABLE_PROPS[0]);
            }

            FakeResourceAdapter ra = new FakeResourceAdapter();
            applyProperties(ra, incompleteProps);

            // Omitted properties retain ra.xml defaults
            for (String propName : omitted) {
                assertEquals("Trial " + trial + ": " + propName + " should retain default",
                             RA_XML_DEFAULTS.get(propName), getPropertyValue(ra, propName));
            }

            // Re-apply complete properties
            Hashtable<String, Object> completeProps = new Hashtable<String, Object>();
            for (String propName : CONFIGURABLE_PROPS)
                completeProps.put(propName, SERVER_XML_PROPS.get(propName));
            applyProperties(ra, completeProps);

            // All properties now correct
            for (String propName : CONFIGURABLE_PROPS) {
                assertEquals("Trial " + trial + ": " + propName + " should have server.xml value after re-apply",
                             SERVER_XML_PROPS.get(propName), getPropertyValue(ra, propName));
            }
        }
    }

    // =========================================================================
    // Preservation Tests — non-buggy path unchanged
    // =========================================================================

    /**
     * For complete property dictionaries, all properties are applied correctly.
     */
    @Test
    public void testPreservation_CompletePropertiesAllApplied() throws Exception {
        for (int trial = 0; trial < 10; trial++) {
            Dictionary<String, Object> completeProps = generateCompleteProps(1000L + trial);
            FakeResourceAdapter ra = new FakeResourceAdapter();
            List<String> applied = applyProperties(ra, completeProps);

            for (String propName : CONFIGURABLE_PROPS) {
                assertTrue("Trial " + trial + ": " + propName + " should have been applied",
                           applied.contains(propName));
                assertEquals("Trial " + trial + ": " + propName + " value mismatch",
                             completeProps.get(propName), getPropertyValue(ra, propName));
            }
        }
    }

    /**
     * Applying complete properties twice produces the same result (idempotent).
     */
    @Test
    public void testPreservation_IdempotentReapplication() throws Exception {
        for (int trial = 0; trial < 10; trial++) {
            Dictionary<String, Object> props = generateCompleteProps(4000L + trial);
            FakeResourceAdapter ra = new FakeResourceAdapter();
            applyProperties(ra, props);
            applyProperties(ra, props);

            for (String propName : CONFIGURABLE_PROPS)
                assertEquals("Trial " + trial + ": " + propName,
                             props.get(propName), getPropertyValue(ra, propName));
        }
    }

    /**
     * Sequential config updates with different complete properties — RA has latest values.
     */
    @Test
    public void testPreservation_SequentialConfigUpdates() throws Exception {
        for (int trial = 0; trial < 10; trial++) {
            Dictionary<String, Object> first = generateCompleteProps(5000L + trial);
            Dictionary<String, Object> second = generateCompleteProps(6000L + trial);
            FakeResourceAdapter ra = new FakeResourceAdapter();

            applyProperties(ra, first);
            for (String propName : CONFIGURABLE_PROPS)
                assertEquals("Trial " + trial + ": " + propName + " after first",
                             first.get(propName), getPropertyValue(ra, propName));

            applyProperties(ra, second);
            for (String propName : CONFIGURABLE_PROPS)
                assertEquals("Trial " + trial + ": " + propName + " after second",
                             second.get(propName), getPropertyValue(ra, propName));
        }
    }
}
