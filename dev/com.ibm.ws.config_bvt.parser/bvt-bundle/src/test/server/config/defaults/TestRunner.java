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
package test.server.config.defaults;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;

import test.server.BaseTestRunner;
import test.server.config.dynamic.ConfigWriter;
import test.server.config.dynamic.FactoryTest;
import test.server.config.dynamic.SingletonTest;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;

public class TestRunner extends BaseTestRunner {

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/default-config-test", new TestDynamicConfigServlet(), null, null);
    }

    public void testDefaultConfigSingleton() throws Exception {
        SingletonTest parent = new SingletonTest("test.config.default.singleton");
        addTest(parent);

        Dictionary<String, Object> dictionary;

        // wait for the initial configuration to be injected.
        dictionary = parent.waitForUpdate();
        assertEquals("port", Integer.valueOf(5678), dictionary.get("port"));
        assertArrayEquals("hostname", new String[] { "192.168.1.1", "192.168.1.2" }, (String[]) dictionary.get("hostname"));

        ConfigWriter writer;

        // STEP 1: update singleton to disable default configuration
        writer = readConfiguration();
        writer.setValue("defaultSingleton", null, XMLConfigConstants.CONFIG_ENABLED_ATTRIBUTE, "false");
        writeConfiguration(writer);

        // wait for deleted configuration
        dictionary = parent.waitForUpdate();
        assertNull(dictionary);

        // STEP 2: update singleton to enable default configuration
        writer = readConfiguration();
        writer.setValue("defaultSingleton", null, XMLConfigConstants.CONFIG_ENABLED_ATTRIBUTE, "true");
        writeConfiguration(writer);

        // wait for updated configuration
        dictionary = parent.waitForUpdate();
        assertEquals("port", Integer.valueOf(5678), dictionary.get("port"));
        assertArrayEquals("hostname", new String[] { "192.168.1.1", "192.168.1.2" }, (String[]) dictionary.get("hostname"));
    }

    public void testDefaultConfigFactory() throws Exception {

        FactoryTest parent = new FactoryTest("test.config.default.factory");
        addTest(parent);

        // wait for the initial configurations to be injected.
        Dictionary<String, Object> oneDictionary = parent.waitForUpdate("one");
        assertEquals("port", Integer.valueOf(1234), oneDictionary.get("port"));
        assertArrayEquals("hostname", new String[] { "192.168.1.1", "192.168.1.2" }, (String[]) oneDictionary.get("hostname"));

        Dictionary<String, Object> twoDictionary = parent.waitForUpdate("two");
        assertEquals("port", Integer.valueOf(9999), twoDictionary.get("port"));
        assertArrayEquals("hostname", new String[] { "localhost", "ibm.com" }, (String[]) twoDictionary.get("hostname"));

        ConfigWriter writer;

        // STEP 1: update instance two to disable default configuration
        writer = readConfiguration();
        writer.setValue("defaultFactory", "two", XMLConfigConstants.CONFIG_ENABLED_ATTRIBUTE, "false");
        writeConfiguration(writer);

        // wait for instance two delete
        twoDictionary = parent.waitForUpdate("two");
        assertNull(twoDictionary);

        // STEP 2: update instance two to enable default configuration
        writer = readConfiguration();
        writer.setValue("defaultFactory", "two", XMLConfigConstants.CONFIG_ENABLED_ATTRIBUTE, "true");
        writeConfiguration(writer);

        // wait for instance two update
        twoDictionary = parent.waitForUpdate("two");
        assertEquals("port", Integer.valueOf(9999), twoDictionary.get("port"));
        assertArrayEquals("hostname", new String[] { "localhost", "ibm.com" }, (String[]) twoDictionary.get("hostname"));
    }

}
