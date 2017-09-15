/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config.nested;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.component.ComponentContext;

import test.server.BaseTestRunner;
import test.server.config.dynamic.ConfigWriter;
import test.server.config.dynamic.SingletonTest;

/**
 *
 */
public class NestedSingletonTestRunner extends BaseTestRunner {

    private SingletonTest child;
    private final String parentName = "test.config.nested.managed.metatype";
    private final String parentAlias = "singletonNested";
    private final String childName = "test.singleton.under.singleton";
    private final String nestedElementTwo = "resultTwo";
    private final String singletonElement = "singletonResult";
    private final String nestedElement = "testResult";
    private SingletonTest parent;

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/nested-singleton-test", new TestDynamicConfigServlet(), null, null);

        this.child = new SingletonTest(childName);
        this.parent = new SingletonTest(parentName);
        addTest(child);
        addTest(parent);
    }

    public static Map<String, Map<String, Object>> getMetatypeExpectedProperties() {
        Map<String, Object> map1 = new HashMap<String, Object>();
        map1.put("pass", Integer.valueOf(9999));
        map1.put("fail", Integer.valueOf(7777));

        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("pass", Integer.valueOf(100));
        map2.put("fail", Integer.valueOf(0));

        Map<String, Map<String, Object>> expectedProperties = new Hashtable<String, Map<String, Object>>();
        expectedProperties.put("one", map1);
        expectedProperties.put("two", map2);

        return expectedProperties;
    }

    /*
     * Test that the initial configuration is parsed and transformed correctly. Moved from TestRunner
     * because it can be affected by testUpdateReceived(). Both should be called from the same test.
     */
    public void testInitialConfiguration() throws Exception {
        Dictionary<String, Object> properties = parent.waitForUpdate();

        String[] singleton = (String[]) properties.get(singletonElement);
        if (singleton != null) {
            Configuration singletonConfig = configAdmin.getConfiguration(singleton[0]);
            Dictionary singletonProps = singletonConfig.getProperties();
            String singletonString = (String) singletonProps.get("someString");
            String[] singletonArray = (String[]) singletonProps.get("someArray");
            assertEquals("aSingleton", singletonString);
            assertEquals(3, singletonArray.length);
        }
        String[] resultsTwo = (String[]) properties.get(nestedElementTwo);
        assertNotNull(resultsTwo);
        assertEquals("There should be three nested results", 3, resultsTwo.length);
        for (int i = 0; i < resultsTwo.length; i++) {
            Configuration config = configAdmin.getConfiguration(resultsTwo[i]);
            Dictionary prop = config.getProperties();
            String someString = (String) prop.get("someString");
            String[] someArray = (String[]) prop.get("someArray");
            if ("notDefault".equals(someString)) {
                assertEquals(3, someArray.length);
            } else if ("zzz".equals(someString)) {
                if (someArray.length == 2) {
                    assertEquals("Incorrect defined value for array", "ten", someArray[0]);
                    assertEquals("Incorrect defined value for array", "eleven", someArray[1]);
                } else if (someArray.length == 3) {
                    assertEquals("Incorrect default value for array", "four", someArray[0]);
                    assertEquals("Incorrect default value for array", "five", someArray[1]);
                    assertEquals("Incorrect default value for array", "six", someArray[2]);
                } else {
                    fail("Incorrect number of entries for someArray attribute: " + someArray.length);
                }

            } else {
                fail("Incorrect value for attribute someString: " + someString);
            }
        }

        String[] testResults = (String[]) properties.get(nestedElement);
        assertNotNull(testResults);
        assertEquals(2, testResults.length);

        Map<String, Map<String, Object>> expectedProperties = getMetatypeExpectedProperties();
        for (int i = 0; i < testResults.length; i++) {
            Configuration config = configAdmin.getConfiguration(testResults[i]);
            Dictionary prop = config.getProperties();
            String innerName = (String) prop.get("name");
            Map<String, Object> expectedProps = expectedProperties.remove(innerName);
            assertNotNull("Unexpected name: " + innerName, expectedProps);
            for (Map.Entry<String, Object> entry : expectedProps.entrySet()) {
                assertEquals("property " + entry.getKey() + " mismatch", entry.getValue(), prop.get(entry.getKey()));
            }
        }

        assertEquals(0, expectedProperties.size());
    }

    /**
     * Make sure a cross bundle nested singleton update causes the ManagedService to be updated
     *
     * @throws Exception
     */
    public void testUpdateReceived() throws Exception {

        ConfigWriter writer;
        Dictionary<String, Object> dictionary;

        // *** STEP 1 ***
        // Clear any previous updates
        child.reset();

        // Delete any existing elements
        writer = readConfiguration();
        boolean updated = writer.deleteConfig(parentName, null, true);
        updated |= writer.deleteConfig(parentAlias, null, true);
        if (updated) {
            // The update check here is just a convenience for debugging. When the test is run, there should
            // always be an element to delete.
            writeConfiguration(writer);
            assertNotNull("Singleton deletion should result in a configuration with all default properties", child.waitForUpdate());
        }

        // Write initial configuration
        writer = readConfiguration();
        writer.addConfig("<{0} name=\"Jane Doe\">" +
                         "  <{1} someString=\"hello\"/>" +
                         "</{0}>", parentName, childName);
        writeConfiguration(writer);

        // wait for the initial configuration to be injected.

        dictionary = child.waitForUpdate();
        assertEquals("someString should be \"hello\"", "hello", dictionary.get("someString"));

        // Clean up
        writer = readConfiguration();
        writer.deleteConfig(parentName, null, true);
        writer.deleteConfig(parentAlias, null, true);
        writeConfiguration(writer);
        assertNull("Singleton deletion should result in a null dictionary", child.waitForUpdate());

    }
}
