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
package com.ibm.ws.kernel.instrument.serialfilter.serverconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.kernel.instrument.serialfilter.config.SimpleConfig;
import com.ibm.ws.kernel.instrument.serialfilter.config.ValidationMode;
import com.ibm.ws.kernel.instrument.serialfilter.config.PermissionMode;

public class FilterConfigFactoryTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private boolean isEnabled;
    private SimpleConfig simpleConfig, mockSimpleConfig;
    
    private FilterConfigFactory fcf = new FilterConfigFactory() {
    	@Override
        protected boolean isEnabled() {
            return isEnabled;
        }
        @Override
        protected SimpleConfig getSystemConfigProxy() {
            return simpleConfig;
        }
        
    };

    
    @Before
    public void setUp() {
        mockSimpleConfig = mockery.mock(SimpleConfig.class);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testPropagateConfigMapValidEntry1() throws Exception {
        Map<String, String> modeMap = new HashMap<String, String>();
        Map<String, String> policyMap = new HashMap<String, String>();
        DummySimpleConfig dummySimpleConfig = new DummySimpleConfig();
        simpleConfig = dummySimpleConfig;
        modeMap.put("*", "Enforce");
        policyMap.put("*", "Deny");
        fcf.propagateConfigMap(modeMap, policyMap);
        Properties output = dummySimpleConfig.getProperties();
        assertEquals("number of properties should be one", output.size(), 1);
        assertEquals("properties should contain a valid data ", output.getProperty("*"), "ENFORCE,DENY");
    }

    @Test
    public void testPropagateConfigMapValidEntry2() throws Exception {
        Map<String, String> modeMap = new HashMap<String, String>();
        Map<String, String> policyMap = new HashMap<String, String>();
        DummySimpleConfig dummySimpleConfig = new DummySimpleConfig();
        simpleConfig = dummySimpleConfig;
        modeMap.put("com.ibm.test.Dummy#Method", "Enforce");
        modeMap.put("com.ibm.test.Dummy", "Discover");
        policyMap.put("com.ibm.test.Dummy", "Allow");
        fcf.propagateConfigMap(modeMap, policyMap);
        Properties output = dummySimpleConfig.getProperties();
        assertEquals("number of properties should be two", output.size(), 2);
        assertEquals("properties should contain a valid data com.ibm.test.Dummy#Method", output.getProperty("com.ibm.test.Dummy#Method"), "ENFORCE");
        assertEquals("properties should contain a valid data com.ibm.test.Dummy", output.getProperty("com.ibm.test.Dummy"), "DISCOVER,ALLOW");
    }

    @Test
    public void testPropagateConfigMapValidEntries() throws Exception {
        Map<String, String> modeMap = new HashMap<String, String>();
        Map<String, String> policyMap = new HashMap<String, String>();
        DummySimpleConfig dummySimpleConfig = new DummySimpleConfig();
        simpleConfig = dummySimpleConfig;
        modeMap.put("*", "Enforce");
        policyMap.put("*", "Deny");
        modeMap.put("com.ibm.test.Dummy#Method", "Discover");
        modeMap.put("com.ibm.test.Second", "Reject");
        policyMap.put("com.ibm.test.Second", "Allow");
        fcf.propagateConfigMap(modeMap, policyMap);
        Properties output = dummySimpleConfig.getProperties();
        assertEquals("number of properties should be two", output.size(), 3);
        assertEquals("properties should contain a valid data for *", output.getProperty("*"), "ENFORCE,DENY");
        assertEquals("properties should contain a valid data for com.ibm.test.Dummy#Method", output.getProperty("com.ibm.test.Dummy#Method"), "DISCOVER");
        assertEquals("properties should contain a valid data for com.ibm.test.Second", output.getProperty("com.ibm.test.Second"), "REJECT,ALLOW");
    }

    class DummySimpleConfig implements SimpleConfig {
        Properties props;
        @Override
        public void reset(){
        }

        @Override
        public ValidationMode getDefaultMode(){
            return null;
        }

        @Override
        public ValidationMode getValidationMode(String specifier) {
            return null;
        }

        @Override
        public boolean setValidationMode(ValidationMode mode, String specifier) {
            return true;
        }

        @Override
        public boolean setPermission(PermissionMode perm, String s) {
            return true;
        }

        @Override
        public void load(Properties props) {
            this.props = props;
        }

        public Properties getProperties() {
            return props;
        }
    }
}
