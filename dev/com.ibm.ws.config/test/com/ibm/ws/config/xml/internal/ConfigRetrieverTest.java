/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.framework.ServiceReference;

import test.common.SharedOutputManager;

import com.ibm.websphere.config.ConfigRetrieverException;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ExtendedConfiguration;

/**
 *
 */
public class ConfigRetrieverTest {

    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @Test
    public void testFindAllNestedConfigurations() throws ConfigRetrieverException {
        TestConfigurationAdmin configAdmin = new TestConfigurationAdmin();
        ConfigRetriever cr = new ConfigRetriever(null, configAdmin, null);

        // config.id = abc[someId]
        ConfigID parent = new ConfigID("abc", "someId");

        // config.id = abc[someId]/a(a)[a1]
        ConfigID child1 = new ConfigID(parent, "a", "a1", "a");

        // config.id = abc[someId]/a(a)[a2]
        ConfigID child2 = new ConfigID(parent, "a", "a2", "a");

        // config.id = abc[someId]/a(a)[a3]
        ConfigID child3 = new ConfigID(parent, "a", "a3", "a");

        // config.id = abc[someId]/ab(ab)[ab1]
        ConfigID child4 = new ConfigID(parent, "ab", "ab1", "ab");

        // config.id = abc[someId]/ab(ab)[ab1]/a(a)[abChild]
        ConfigID child5 = new ConfigID(child4, "a", "abChild", "a");

        configAdmin.addConfigurationsForConfigIDs(child1, child2, child3, child4, child5);
        Configuration[] matching = cr.findAllNestedConfigurations(child1);
        assertEquals("There should be four child configurations matching 'a'", 4, matching.length);

        matching = cr.findAllNestedConfigurations(parent);
        assertEquals("The parent should not be found as a nested configuration", 0, matching.length);

        matching = cr.findAllNestedConfigurations(child4);
        assertEquals("There should be two child configurations matching 'ab'", 2, matching.length);
    }

    private class TestConfigurationAdmin implements ConfigurationAdmin {

        private final List<Configuration> factoryConfigs = new ArrayList<Configuration>();

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.ConfigurationAdmin#createFactoryConfiguration(java.lang.String)
         */
        @Override
        public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * @param child1
         * @param child2
         * @param child3
         * @param child4
         */
        public void addConfigurationsForConfigIDs(ConfigID... configs) {
            for (ConfigID cfg : configs) {
                Dictionary<String, Object> properties = new Hashtable<String, Object>();
                properties.put("config.id", cfg.toString());
                Configuration c = new TestConfiguration(cfg.getPid(), properties);
                factoryConfigs.add(c);
            }

        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.ConfigurationAdmin#createFactoryConfiguration(java.lang.String, java.lang.String)
         */
        @Override
        public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.ConfigurationAdmin#getConfiguration(java.lang.String, java.lang.String)
         */
        @Override
        public Configuration getConfiguration(String pid, String location) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.ConfigurationAdmin#getConfiguration(java.lang.String)
         */
        @Override
        public Configuration getConfiguration(String pid) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.ConfigurationAdmin#listConfigurations(java.lang.String)
         */
        @Override
        public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
            ArrayList<Configuration> configs = new ArrayList<Configuration>();
            Filter f = FrameworkUtil.createFilter(filter);
            for (Configuration c : factoryConfigs) {
                if (f.match(c.getProperties()))
                    configs.add(c);
            }
            return configs.toArray(new TestConfiguration[configs.size()]);
        }

        @Override
        public Configuration getFactoryConfiguration(String factoryPid, String name, String location) throws IOException {
			return null;
		}

		@Override
		public Configuration getFactoryConfiguration(String factoryPid, String name) throws IOException {
			return null;
		}
    }

    private class TestConfiguration implements ExtendedConfiguration {

        private final Dictionary<String, Object> properties;
        private final String factoryPid;

        /**
         * @param pid
         * @param properties
         */
        public TestConfiguration(String pid, Dictionary<String, Object> properties) {
            this.factoryPid = pid;
            this.properties = properties;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.Configuration#getPid()
         */
        @Override
        public String getPid() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.Configuration#getProperties()
         */
        @Override
        public Dictionary<String, Object> getProperties() {
            return properties;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.Configuration#update(java.util.Dictionary)
         */
        @Override
        public void update(Dictionary<String, ?> properties) throws IOException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.Configuration#delete()
         */
        @Override
        public void delete() throws IOException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.Configuration#getFactoryPid()
         */
        @Override
        public String getFactoryPid() {
            return factoryPid;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.Configuration#update()
         */
        @Override
        public void update() throws IOException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.Configuration#setBundleLocation(java.lang.String)
         */
        @Override
        public void setBundleLocation(String location) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.Configuration#getBundleLocation()
         */
        @Override
        public String getBundleLocation() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.service.cm.Configuration#getChangeCount()
         */
        @Override
        public long getChangeCount() {
            // TODO Auto-generated method stub
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#lock()
         */
        @Override
        public void lock() {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#unlock()
         */
        @Override
        public void unlock() {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#fireConfigurationDeleted(java.util.Collection)
         */
        @Override
        public void fireConfigurationDeleted(Collection<Future<?>> futureList) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#fireConfigurationUpdated(java.util.Collection)
         */
        @Override
        public void fireConfigurationUpdated(Collection<Future<?>> futureList) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#delete(boolean)
         */
        @Override
        public void delete(boolean fireNotifications) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#getProperty(java.lang.String)
         */
        @Override
        public Object getProperty(String key) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#getReadOnlyProperties()
         */
        @Override
        public Dictionary<String, Object> getReadOnlyProperties() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#updateCache(java.util.Dictionary, java.util.Set, java.util.Set)
         */
        @Override
        public void updateCache(Dictionary<String, Object> properties, Set<ConfigID> references, Set<String> newUniques) throws IOException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#updateProperties(java.util.Dictionary)
         */
        @Override
        public void updateProperties(Dictionary<String, Object> properties) throws IOException {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#getReferences()
         */
        @Override
        public Set<ConfigID> getReferences() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#setInOverridesFile(boolean)
         */
        @Override
        public void setInOverridesFile(boolean inOverridesFile) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#isInOverridesFile()
         */
        @Override
        public boolean isInOverridesFile() {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#getUniqueVariables()
         */
        @Override
        public Set<String> getUniqueVariables() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#setFullId(com.ibm.ws.config.admin.ConfigID)
         */
        @Override
        public void setFullId(ConfigID id) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#getFullId()
         */
        @Override
        public ConfigID getFullId() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.admin.ExtendedConfiguration#isDeleted()
         */
        @Override
        public boolean isDeleted() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
		public java.util.Dictionary<java.lang.String,java.lang.Object> getProcessedProperties(ServiceReference<?> reference) {
			return null;
		}

		@Override
		public Set<ConfigurationAttribute> getAttributes() {
			return null;
		}

		@Override
		public void addAttributes(Configuration.ConfigurationAttribute... attrs) throws IOException {

		}

		@Override
		public void removeAttributes(Configuration.ConfigurationAttribute... attrs) throws IOException {

		}

		@Override
		public boolean updateIfDifferent(java.util.Dictionary<java.lang.String,?> properties) throws java.io.IOException {
			return false;

		}

    }

}
