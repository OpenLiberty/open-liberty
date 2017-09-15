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
package test.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;

/**
 *
 */
public class NestedConfigElementTest extends ManagedFactoryTest {

    private final ConfigurationAdmin configAdmin;
    private final Map apps, hostNames, ports;

    /**
     * @param name
     */
    public NestedConfigElementTest(String name, ConfigurationAdmin configAdmin) {
        super(name);
        this.configAdmin = configAdmin;
        apps = new HashMap();
        hostNames = new HashMap();
        ports = new HashMap();
        init();
    }

    /**
     * results data to compare against
     */
    private void init() {
        apps.put("fred", null);
        apps.put("bob", null);
        apps.put("app", null);
        hostNames.put("w3.ibm.com", "fred");
        hostNames.put("w3.hursley.ibm.com", "fred");
        hostNames.put("butterfly.torolab.ibm.com", "bob");
        hostNames.put("mallet.torolab.ibm.com", "bob");
        hostNames.put("*", "app");
        hostNames.put("nonexistent", "app");
        ports.put("9084", "app");
        ports.put("9085", "app");
        ports.put("9080", "fred");
        ports.put("9081", "fred");
        ports.put("9082", "bob");
        ports.put("9083", "bob");
    }

    /** {@inheritDoc} */
    @Override
    public void configurationUpdated(String pid, Dictionary properties) throws Exception {
        String parentPID = (String) properties.get("service.pid");
        assertEquals("Expected top level pid", "test.config.nested.elements", properties.get("service.factoryPid"));
        String[] virtualHosts = (String[]) properties.get("testVirtualHost");
        assertNotNull(virtualHosts);
        assertEquals("There should be two virtual hosts", 2, virtualHosts.length);
        String name = (String) properties.get("name");
        assertTrue(apps.containsKey(name));

        if ("fred".equals(name)) {
            String[] nestedSingleton = (String[]) properties.get("aNestedSingleton");
            Configuration singletonConfig = configAdmin.getConfiguration(nestedSingleton[0]);
            assertEquals("Expected pid", "bundletwo.singleton", singletonConfig.getPid());
            Dictionary singletonProps = singletonConfig.getProperties();
            assertEquals("The config.parentPID value should be equal to the parent's service.pid value", parentPID,
                         singletonProps.get(XMLConfigConstants.CFG_PARENT_PID));
            assertEquals("The displayID should be application[fred]/aNestedSingleton", "application[fred]/aNestedSingleton",
                         singletonProps.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID));
            assertEquals(1, ((String[]) singletonProps.get("anArray")).length);

            String[] libraries = (String[]) properties.get("library");
            assertNotNull(libraries);
            assertEquals(2, libraries.length);

            for (int i = 0; i < libraries.length; i++) {
                Configuration config = configAdmin.getConfiguration(libraries[i]);
                assertEquals("Expected factoryPid", "test.config.nested.bundletwo", config.getFactoryPid());
                Dictionary prop = config.getProperties();
                assertEquals("The config.parentPID value should be equal to the parent's service.pid value", parentPID,
                             prop.get(XMLConfigConstants.CFG_PARENT_PID));
                String aString = (String) prop.get("aString");
                String[] anArray = (String[]) prop.get("anArray");
                if ("defaultString".equals(aString)) {
                    assertEquals(4, anArray.length);
                    assertEquals("The displayID should be application[fred]/library[default-1]", "application[fred]/library[default-1]",
                                 prop.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID));
                } else if ("notADefault".equals(aString)) {
                    assertEquals(3, anArray.length);
                    assertEquals("The displayID should be application[fred]/library[default-0]", "application[fred]/library[default-0]",
                                 prop.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID));
                } else {
                    fail("Invalid value for attribute aString: " + aString);
                }

            }

            for (int i = 0; i < virtualHosts.length; i++) {
                Configuration config = configAdmin.getConfiguration(virtualHosts[i]);
                assertEquals("Expected factoryPid", "test.config.nested.virtualHost", config.getFactoryPid());
                Dictionary prop = config.getProperties();
                assertEquals("The config.parentPID value should be equal to the parent's service.pid value", parentPID,
                             prop.get(XMLConfigConstants.CFG_PARENT_PID));
                String[] hostAliases = (String[]) prop.get("testHostAlias");
                assertEquals(1, hostAliases.length);
                Configuration aliasConfig = configAdmin.getConfiguration(hostAliases[0]);
                assertEquals("Expected factoryPid", "test.host.alias", aliasConfig.getFactoryPid());
                Dictionary aliasProps = aliasConfig.getProperties();
                String[] aliases = (String[]) aliasProps.get("names");
                assertEquals(2, aliases.length);
            }
        }
        for (int i = 0; i < virtualHosts.length; i++) {
            Configuration config = configAdmin.getConfiguration(virtualHosts[i]);
            Dictionary prop = config.getProperties();
            String host = (String) prop.get("host");
            assertTrue(hostNames.containsKey(host));
            assertEquals(hostNames.get(host), name);
            String port = String.valueOf(prop.get("port"));
            assertTrue(ports.containsKey(port));
            assertEquals(ports.get(port), name);
        }

    }
}
