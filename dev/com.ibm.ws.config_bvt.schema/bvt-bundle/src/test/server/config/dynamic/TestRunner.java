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
package test.server.config.dynamic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;

import test.server.BaseTestRunner;

public class TestRunner extends BaseTestRunner {

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/dynamic-config-test", new TestDynamicConfigServlet(), null, null);
    }

    public void testSingleton() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        SingletonTest singletonTest = new SingletonTest("test.config.dynamic.singleton");
        addTest(singletonTest);

        // step 1. wait for the initial configuration to be injected.
        dictionary = singletonTest.waitForUpdate();
        assertEquals("testValue", "10", dictionary.get("testValue"));

        // step 2a. modify the configuration
        writer = readConfiguration();
        writer.setValue("test.config.dynamic.singleton", null, "testValue", "15");
        writeConfiguration(writer);
        // step 2b: wait for update
        dictionary = singletonTest.waitForUpdate();
        assertEquals("testValue", "15", dictionary.get("testValue"));

        // step 3a. delete configuration 
        writer = readConfiguration();
        writer.deleteConfig("test.config.dynamic.singleton", null, false);
        writeConfiguration(writer);
        // step 3b. wait for update
        dictionary = singletonTest.waitForUpdate();
        assertEquals("Test deleted", null, dictionary);

        // step 4a. add configuration back in
        writer = readConfiguration();
        writer.addConfig("<test.config.dynamic.singleton message=\"hello\"/>");
        writeConfiguration(writer);
        // step 4b. wait for update
        dictionary = singletonTest.waitForUpdate();
        assertEquals("Test added", "hello", dictionary.get("message"));
    }

    public void testFactory() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        FactoryTest factoryTest = new FactoryTest("test.config.dynamic.factory");
        addTest(factoryTest);

        // step 1. wait for the initial configuration to be injected.
        dictionary = factoryTest.waitForUpdate("1");
        assertEquals("instance 1 testValue", "10", dictionary.get("testValue"));

        // step 2a. modify instance 1 configuration & add instance 2 configuration
        writer = readConfiguration();
        writer.setValue("test.config.dynamic.factory", "1", "testValue", "15");
        writer.addConfig("<test.config.dynamic.factory id=\"2\" message=\"bye\"/>");
        writeConfiguration(writer);
        // step 2b: wait for updates
        dictionary = factoryTest.waitForUpdate("1");
        assertEquals("instance 1 testValue", "15", dictionary.get("testValue"));
        dictionary = factoryTest.waitForUpdate("2");
        assertEquals("instance 2 message", "bye", dictionary.get("message"));

        // step 3a. delete instance 1 configuration & modify instance 2 configuration
        writer = readConfiguration();
        writer.deleteConfig("test.config.dynamic.factory", "1", false);
        writer.setValue("test.config.dynamic.factory", "2", "message", "what's up");
        writeConfiguration(writer);
        // step 3b. wait for updates
        dictionary = factoryTest.waitForUpdate("1");
        assertEquals("instance 1 deleted", null, dictionary);
        dictionary = factoryTest.waitForUpdate("2");
        assertEquals("instance 2 message", "what's up", dictionary.get("message"));

        // step 4a. add instance 1 configuration back in & delete instance 2 
        writer = readConfiguration();
        writer.addConfig("<test.config.dynamic.factory id=\"1\" message=\"hello\"/>");
        writer.deleteConfig("test.config.dynamic.factory", "2", false);
        writeConfiguration(writer);
        // step 4b. wait for updates
        dictionary = factoryTest.waitForUpdate("1");
        assertEquals("instance 1 added", "hello", dictionary.get("message"));
        dictionary = factoryTest.waitForUpdate("2");
        assertEquals("instance 2 deleted", null, dictionary);
    }

    public void testSingletonMetatype() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        SingletonTest singletonMetatypeTest = new SingletonTest("test.config.dynamic.singleton.metatype");
        addTest(singletonMetatypeTest);

        // step 1. wait for the initial configuration to be injected. 
        // This singleton was not configured in server.xml -- It's created because all of its required
        // fields have defaults
        dictionary = singletonMetatypeTest.waitForUpdate();
        // Make sure auto-added singletons use the alias for the display id
        assertEquals("singletonMetatype", dictionary.get("config.displayId"));

        // step 2a. modify the configuration using an alias
        writer = readConfiguration();
        writer.addConfig("<singletonMetatype version=\"2.0\"/>");
        writeConfiguration(writer);
        // step 2b: wait for update
        dictionary = singletonMetatypeTest.waitForUpdate();
        assertEquals("version", "2.0", dictionary.get("version"));
        assertEquals("threads", new Integer(100), dictionary.get("threads"));

        // step 3a. delete configuration using an alias
        writer = readConfiguration();
        writer.deleteConfig("singletonMetatype", null, false);
        writeConfiguration(writer);
        // step 3b. wait for update - defaults should be injected
        dictionary = singletonMetatypeTest.waitForUpdate();
        assertEquals("version", "1.0", dictionary.get("version"));
        assertEquals("threads", new Integer(100), dictionary.get("threads"));

        // step 4a. add configuration back in
        writer = readConfiguration();
        writer.addConfig("<singletonMetatype threads=\"500\"/>");
        writeConfiguration(writer);
        // step 4b. wait for update
        dictionary = singletonMetatypeTest.waitForUpdate();
        assertEquals("version", "1.0", dictionary.get("version"));
        assertEquals("threads", new Integer(500), dictionary.get("threads"));
    }

    public void testFactoryMetatype() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        FactoryTest factoryTest = new FactoryTest("test.config.dynamic.factory.metatype");
        addTest(factoryTest);

        // step 1. wait for the initial configuration to be injected.
        dictionary = factoryTest.waitForUpdate("one");

        // step 2a. modify instance 1 configuration & add instance 2 configuration
        // The timeout value is of type duration(s), so the value is in seconds instead of milliseconds
        writer = readConfiguration();
        writer.setValue("test.config.dynamic.factory.metatype", "one", "timeout", "123m");
        writer.addConfig("<factoryMetatype id=\"two\" minSize=\"456\"/>");
        writeConfiguration(writer);
        // step 2b: wait for updates
        dictionary = factoryTest.waitForUpdate("one");
        assertEquals("instance one timeout", new Long(123 * 60), dictionary.get("timeout"));
        assertEquals("instance one minSize", new Integer(10), dictionary.get("minSize"));
        dictionary = factoryTest.waitForUpdate("two");
        assertEquals("instance two timeout", new Long(100), dictionary.get("timeout"));
        assertEquals("instance two minSize", new Integer(456), dictionary.get("minSize"));

        // step 3a. delete instance 1 configuration & modify instance 2 configuration
        writer = readConfiguration();
        writer.deleteConfig("test.config.dynamic.factory.metatype", "one", false);
        writer.setValue("factoryMetatype", "two", "timeout", "7890ms");
        writeConfiguration(writer);
        // step 3b. wait for updates
        dictionary = factoryTest.waitForUpdate("one");
        assertEquals("instance one deleted", null, dictionary);
        dictionary = factoryTest.waitForUpdate("two");
        assertEquals("instance two timeout", new Long(7), dictionary.get("timeout"));
        assertEquals("instance two minSize", new Integer(456), dictionary.get("minSize"));

        // step 4a. add instance 1 configuration back in & delete instance 2 
        writer = readConfiguration();
        writer.addConfig("<factoryMetatype id=\"one\" minSize=\"91011\"/>");
        writer.deleteConfig("factoryMetatype", "two", false);
        writeConfiguration(writer);
        // step 4b. wait for updates
        dictionary = factoryTest.waitForUpdate("one");
        assertEquals("instance one timeout", new Long(100), dictionary.get("timeout"));
        assertEquals("instance one minSize", new Integer(91011), dictionary.get("minSize"));
        dictionary = factoryTest.waitForUpdate("two");
        assertEquals("instance 2 deleted", null, dictionary);
    }

    public void testFactoryOptionalId() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        String idAttribute = "name";

        FactoryTest factoryTest = new FactoryTest("test.config.dynamic.factory.optional.metatype", idAttribute);
        addTest(factoryTest);

        // step 1a. wait for initial configuration of first instance
        dictionary = factoryTest.waitForUpdate("one");
        assertEquals("instance one timeout", new Long(100), dictionary.get("timeout"));
        assertEquals("instance one minSize", new Integer(10), dictionary.get("minSize"));
        assertNull(dictionary.get("id"));

        // step 1b. wait for initial configuration of second instance
        dictionary = factoryTest.waitForUpdate("four");
        assertEquals("instance one timeout", new Long(5000), dictionary.get("timeout"));
        assertEquals("instance one minSize", new Integer(10), dictionary.get("minSize"));
        assertNull(dictionary.get("id"));

        assertEquals(2, factoryTest.getPids().size());

        // step 2a. modify default instance & add another instance
        writer = readConfiguration();
        writer.setValue("factoryOptionalMetatype", "one", idAttribute, "timeout", "123");
        writer.addConfig("<factoryOptionalMetatype id=\"two\" name=\"two\" minSize=\"456\"/>");
        writeConfiguration(writer);

        // step 2b. wait for updates on default instance
        dictionary = factoryTest.waitForUpdate("one");
        assertEquals("instance one timeout", new Long(123), dictionary.get("timeout"));
        assertEquals("instance one minSize", new Integer(10), dictionary.get("minSize"));
        assertNull(dictionary.get("id"));

        // step 2c. wait for updates on second instance
        dictionary = factoryTest.waitForUpdate("two");
        assertEquals("instance one timeout", new Long(100), dictionary.get("timeout"));
        assertEquals("instance one minSize", new Integer(456), dictionary.get("minSize"));
        assertEquals("two", dictionary.get("id"));

        assertEquals(3, factoryTest.getPids().size());

        // step 3a. delete the default instance
        writer = readConfiguration();
        writer.deleteConfig("factoryOptionalMetatype", "one", idAttribute, false);
        writeConfiguration(writer);

        // step 3a. wait for the update
        dictionary = factoryTest.waitForUpdate("one");
        assertNull("default instance deleted", dictionary);

        assertEquals(3, factoryTest.getPids().size());

        // step 4a. add a new default instance & update the second instance
        writer = readConfiguration();
        writer.addConfig("<factoryOptionalMetatype name=\"three\" minSize=\"111\" timeout=\"222\"/>");
        writer.setValue("factoryOptionalMetatype", "two", idAttribute, "timeout", "789");
        writeConfiguration(writer);

        // step 4b. wait for updates on the default instance
        dictionary = factoryTest.waitForUpdate("three");
        assertEquals("instance one timeout", new Long(222), dictionary.get("timeout"));
        assertEquals("instance one minSize", new Integer(111), dictionary.get("minSize"));
        assertNull(dictionary.get("id"));

        // step 4c. wait for updates on the two instance
        dictionary = factoryTest.waitForUpdate("two");
        assertEquals("instance one timeout", new Long(789), dictionary.get("timeout"));
        assertEquals("instance one minSize", new Integer(456), dictionary.get("minSize"));
        assertEquals("two", dictionary.get("id"));

        assertEquals(4, factoryTest.getPids().size());

        // step 5a. delete two & three, update four
        writer = readConfiguration();
        writer.deleteConfig("factoryOptionalMetatype", "two", idAttribute, false);
        writer.deleteConfig("factoryOptionalMetatype", "three", idAttribute, false);
        writer.setValue("factoryOptionalMetatype", "four", idAttribute, "timeout", "6000");
        writeConfiguration(writer);

        // step 5b. wait for updates
        dictionary = factoryTest.waitForUpdate("two");
        assertEquals("instance 2 deleted", null, dictionary);

        // step 5c. wait for updates
        dictionary = factoryTest.waitForUpdate("three");
        assertEquals("instance 2 deleted", null, dictionary);

        // step 5d. wait for updates
        dictionary = factoryTest.waitForUpdate("four");
        assertEquals("instance one timeout", new Long(6000), dictionary.get("timeout"));
        assertEquals("instance one minSize", new Integer(10), dictionary.get("minSize"));
        assertNull(dictionary.get("id"));

        assertEquals(4, factoryTest.getPids().size());
    }
}
