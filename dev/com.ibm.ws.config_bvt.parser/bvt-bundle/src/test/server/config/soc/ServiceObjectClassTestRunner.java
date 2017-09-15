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
package test.server.config.soc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import test.server.BaseTestRunner;
import test.server.config.dynamic.ConfigWriter;
import test.server.config.dynamic.FactoryTest;

/**
 *
 */
public class ServiceObjectClassTestRunner extends BaseTestRunner {

    String simpleSOCPID = "com.ibm.ws.config.soc.simple";
    String applePID = "com.ibm.ws.config.soc.apple";
    String orangePID = "com.ibm.ws.config.soc.orange";

    private FactoryTest simpleSOCTest;

    private ConfigurationAdmin configAdmin;

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/serviceObjectClass-test", new TestDynamicConfigServlet(), null, null);

        this.simpleSOCTest = new FactoryTest(simpleSOCPID);

        addTest(simpleSOCTest);

        BundleContext bundleContext = context.getBundleContext();
        ServiceReference<?> ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        this.configAdmin = (ConfigurationAdmin) bundleContext.getService(ref);
    }

    public void testSimpleServiceObjectClass() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        try {
            // Reset to avoid contamination
            resetFactoryTests();

            /*
             * <filtertest id="a" fruitRef="apple"/>
             * <apple appleType="macintosh"/>
             * <orange orangeType="clementine"/>
             */

            // *** STEP 1 ***
            // Write initial configuration
            writer = readConfiguration();
            writer.addConfig("<serviceObjectClassTest id=\"a\" fruitRef=\"apple\"/>");
            writer.addConfig("<apple id=\"apple\" appleType=\"macintosh\"/>");
            writer.addConfig("<orange id=\"orange\" orangeType=\"clementine\"/>");
            writeConfiguration(writer);

            // wait for the initial configuration to be injected.
            dictionary = simpleSOCTest.waitForUpdate("a");
            assertEquals("id should be \"a\"",
                         "a", dictionary.get("id"));

            String referencePid = (String) dictionary.get("fruitRef");

            Configuration[] references = configAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + referencePid + ")");
            assertNotNull(references);
            assertEquals("There should be one reference", 1, references.length);

            Dictionary<String, Object> appleDictionary = references[0].getProperties();
            assertEquals("id should be apple", "apple", appleDictionary.get("id"));
            assertNull("apple should not contain properties from orange metatype", appleDictionary.get("orangeType"));
            assertEquals("apple should have properties from apple metatype", "macintosh", appleDictionary.get("appleType"));

            // *** STEP 2 ***
            // Update apple->orange
            writer = readConfiguration();
            writer.setValue("serviceObjectClassTest", "a", "fruitRef", "orange");
            writeConfiguration(writer);

            // wait for the dictionary update
            dictionary = simpleSOCTest.waitForUpdate("a");
            assertEquals("id should be \"a\"",
                         "a", dictionary.get("id"));
            referencePid = (String) dictionary.get("fruitRef");

            references = configAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + referencePid + ")");
            assertNotNull(references);
            assertEquals("There should be one reference", 1, references.length);

            Dictionary<String, Object> orangeDictionary = references[0].getProperties();
            assertEquals("id should be orange", "orange", orangeDictionary.get("id"));
            assertNull("orange should not contain properties from apple metatype", orangeDictionary.get("appleType"));
            assertEquals("orange should have properties from orange metatype", "clementine", orangeDictionary.get("orangeType"));

        } finally {
            // Clean up
            writer = readConfiguration();
            writer.deleteConfig("serviceObjectClassTest", "a", true);
            writer.deleteConfig("apple", "apple", true);
            writer.deleteConfig("orange", "orange", true);
            writeConfiguration(writer);
        }
    }

    public void testServiceObjectClassConflict() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        try {
            // Reset to avoid contamination
            resetFactoryTests();

            /*
             * <filtertest id="a" fruitRef="apple"/>
             * <apple appleType="macintosh"/>
             * <orange orangeType="macintosh"/>
             */

            // *** STEP 1 ***
            // Write initial configuration
            writer = readConfiguration();
            writer.addConfig("<serviceObjectClassTest id=\"a\" fruitRef=\"apple\"/>");
            writer.addConfig("<apple id=\"apple\" appleType=\"macintosh\"/>");
            writer.addConfig("<orange id=\"apple\" orangeType=\"clementine\"/>");
            writeConfiguration(writer);

            // wait for the initial configuration to be injected.
            dictionary = simpleSOCTest.waitForUpdate("a");
            assertEquals("id should be \"a\"",
                         "a", dictionary.get("id"));

            // fruitRef should be null because we can't distinguish between apple/orange
            assertNull(dictionary.get("fruitRef"));

            // *** STEP 2 ***
            // Update apple->orange to eliminate conflict
            writer = readConfiguration();
            writer.deleteConfig("orange", "apple", true);
            writer.addConfig("<orange id=\"orange\" orangeType=\"clementine\"/>");
            writeConfiguration(writer);

            // wait for the dictionary update
            dictionary = simpleSOCTest.waitForUpdate("a");
            assertEquals("id should be \"a\"",
                         "a", dictionary.get("id"));

            String referencePid = (String) dictionary.get("fruitRef");

            Configuration[] references = configAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + referencePid + ")");
            assertNotNull(references);
            assertEquals("There should be one reference", 1, references.length);

            Dictionary<String, Object> appleDictionary = references[0].getProperties();
            assertEquals("id should be apple", "apple", appleDictionary.get("id"));
            assertNull("apple should not contain properties from orange metatype", appleDictionary.get("orangeType"));
            assertEquals("apple should have properties from apple metatype", "macintosh", appleDictionary.get("appleType"));

        } finally {
            // Clean up
            writer = readConfiguration();
            writer.deleteConfig("serviceObjectClassTest", "a", false);
            writer.deleteConfig("apple", "apple", true);
            writer.deleteConfig("orange", "orange", true);
            writer.deleteConfig("orange", "apple", true);
            writeConfiguration(writer);
        }
    }

    /**
     * Reset factories to avoid test contamination
     */
    private void resetFactoryTests() {
        simpleSOCTest.reset();

    }
}
