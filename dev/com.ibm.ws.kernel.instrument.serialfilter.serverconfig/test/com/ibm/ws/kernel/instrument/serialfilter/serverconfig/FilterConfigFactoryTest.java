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

import org.osgi.service.cm.ConfigurationException;
import com.ibm.ws.kernel.instrument.serialfilter.config.SimpleConfig;
import com.ibm.ws.kernel.instrument.serialfilter.config.ValidationMode;
import com.ibm.ws.kernel.instrument.serialfilter.config.PermissionMode;

public class FilterConfigFactoryTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private boolean isStopping;
    private boolean isEnabled;
    private SimpleConfig simpleConfig, mockSimpleConfig;
    
    private FilterConfigFactory fcf = new FilterConfigFactory() {
    	@Override
        protected boolean isStopping() {
            return isStopping;
        }
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
    public void testUpdatedStopping() throws Exception {
        Map<String, Dictionary> configMap = fcf.getConfigMap();
        configMap.clear();
        isStopping = true;
        // since isStopping is set to true, updated method does nothing.
        fcf.updated(null, null);
        assertTrue("configMap should be empty.", configMap.isEmpty());
    }

    @Test
    public void testUpdatedAddingEmptyEntry() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(mockSimpleConfig).reset();
                never(mockSimpleConfig).load(with(any(Properties.class)));
            }
        });

        Map<String, Dictionary> configMap = fcf.getConfigMap();
        configMap.clear();
        isStopping = false;
        isEnabled = true;
        simpleConfig = mockSimpleConfig;
        String pid = "pid1";
        Properties props = new Properties();
        fcf.updated(pid, props);
        assertEquals("configMap should be one", configMap.size(), 1);
        assertEquals("configMap should contain a property", configMap.get(pid), props);
    }

    @Test
    public void testUpdatedAddingValidEntry() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(mockSimpleConfig).reset();
                one(mockSimpleConfig).load(with(any(Properties.class)));
            }
        });

        Map<String, Dictionary> configMap = fcf.getConfigMap();
        configMap.clear();
        isStopping = false;
        isEnabled = true;
        simpleConfig = mockSimpleConfig;
        String pid = "pid1";
        Properties props = new Properties();
        props.setProperty("class", "*");
        props.setProperty("mode", "enforce");
        props.setProperty("permission", "deny");
        fcf.updated(pid, props);
        assertEquals("configMap should be one", configMap.size(), 1);
        assertEquals("configMap should contain a property", configMap.get(pid), props);
    }

    @Test
    public void testDeleted() {
        mockery.checking(new Expectations() {
            {
                one(mockSimpleConfig).reset();
                never(mockSimpleConfig).load(with(any(Properties.class)));
            }
        });

        Map<String, Dictionary> configMap = fcf.getConfigMap();
        configMap.clear();
        isStopping = false;
        isEnabled = true;
        simpleConfig = mockSimpleConfig;
        String pid = "pid1";
        Properties props = new Properties();
        props.setProperty("class", "*");
        props.setProperty("mode", "enforce");
        props.setProperty("permission", "deny");
        configMap.put(pid, props);
        fcf.deleted(pid);
        assertTrue("configMap should be zero", configMap.isEmpty());
    }

    @Test
    public void testPropagateConfigMapValidEntry1() throws Exception {
        Map<String, Dictionary> configMap = new HashMap<String, Dictionary>();
        isStopping = false;
        DummySimpleConfig dummySimpleConfig = new DummySimpleConfig();
        simpleConfig = dummySimpleConfig;
        String pid = "pid1";
        Properties props = new Properties();
        props.setProperty("class", "*");
        props.setProperty("mode", "enforce");
        props.setProperty("permission", "deny");
        configMap.put(pid, props);
        fcf.propagateConfigMap(configMap);
        Properties output = dummySimpleConfig.getProperties();
        assertEquals("number of properties should be one", output.size(), 1);
        assertEquals("properties should contain a valid data ", output.getProperty("*"), "ENFORCE,DENY");
    }

    @Test
    public void testPropagateConfigMapValidEntry2() throws Exception {
        Map<String, Dictionary> configMap = new HashMap<String, Dictionary>();
        isStopping = false;
        DummySimpleConfig dummySimpleConfig = new DummySimpleConfig();
        simpleConfig = dummySimpleConfig;
        String pid = "pid1";
        Properties props = new Properties();
        props.setProperty("class", "com.ibm.test.Dummy");
        props.setProperty("method", "Method");
        props.setProperty("mode", "discover");
        props.setProperty("permission", "allow");
        configMap.put(pid, props);
        fcf.propagateConfigMap(configMap);
        Properties output = dummySimpleConfig.getProperties();
        assertEquals("number of properties should be one", output.size(), 1);
        assertEquals("properties should contain a valid data ", output.getProperty("com.ibm.test.Dummy#Method"), "DISCOVER,ALLOW");
    }

    @Test
    public void testPropagateConfigMapValidEntries() throws Exception {
        Map<String, Dictionary> configMap = new HashMap<String, Dictionary>();
        isStopping = false;
        DummySimpleConfig dummySimpleConfig = new DummySimpleConfig();
        simpleConfig = dummySimpleConfig;
        String pid1 = "pid1";
        String pid2 = "pid2";
        Properties props1 = new Properties();
        props1.setProperty("class", "com.ibm.test.Dummy");
        props1.setProperty("method", "Method");
        props1.setProperty("mode", "discover");
        props1.setProperty("permission", "allow");
        configMap.put(pid1, props1);
        Properties props2 = new Properties();
        props2.setProperty("class", "com.ibm.test.Second");
        props2.setProperty("mode", "reject");
        configMap.put(pid2, props2);
        fcf.propagateConfigMap(configMap);
        Properties output = dummySimpleConfig.getProperties();
        assertEquals("number of properties should be two", output.size(), 2);
        assertEquals("properties should contain a valid data ", output.getProperty("com.ibm.test.Dummy#Method"), "DISCOVER,ALLOW");
        assertEquals("properties should contain a valid data ", output.getProperty("com.ibm.test.Second"), "REJECT");
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
