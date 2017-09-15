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

import java.util.Dictionary;
import java.util.Vector;

import org.osgi.service.cm.Configuration;
import org.osgi.service.component.ComponentContext;

import test.server.BaseTestRunner;
import test.server.config.dynamic.ConfigWriter;
import test.server.config.dynamic.FactoryTest;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;

public class NestedDefaultsTestRunner extends BaseTestRunner {

    private final String parentName = "test.config.nested.defaults";
    private FactoryTest parent;

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/nested-defaults-test", new TestDynamicConfigServlet(), null, null);

        this.parent = new FactoryTest(parentName);
        addTest(parent);
    }

    public void testRemoveAttribute() throws Exception {
        testNestedDefaults("cardPos");
    }

    public void testRemoveAttributeNegativeCardinality() throws Exception {
        testNestedDefaults("cardNeg");
    }

    public void testRemoveAttributeZeroCardinality() throws Exception {
        testNestedDefaults("cardZero");
    }

    private void testNestedDefaults(String childName) throws Exception {

        Dictionary<String, Object> dictionary;
        ConfigWriter writer;
        String[] pids = new String[] {};

        try {
            parent.reset();

            // *** STEP 1 ***
            // Write initial configuration
            writer = readConfiguration();
            writer.addConfig("<{0} id=\"parent\" someAttribute=\"empty attributes test\">" +
                             "  <{1} hello=\"hello\" enabled=\"true\"/>" +
                             "</{0}>", parentName, childName);
            writeConfiguration(writer);
            // wait for the initial configuration to be injected.
            dictionary = parent.waitForUpdate("parent");
            assertEquals("someAttribute should be \"empty attributes test\"",
                         "empty attributes test", dictionary.get("someAttribute"));

            // *** STEP 2 ***
            // Remove attribute from child
            writer = readConfiguration();
            writer.deleteConfig(parentName, "parent", false);
            writer.addConfig("<{0} id=\"parent\" someAttribute=\"empty attributes test\">" +
                             "  <{1}/>" +
                             "</{0}>", parentName, childName);
            writeConfiguration(writer);

            dictionary = parent.waitForUpdate("parent");
            assertEquals("someAttribute should be \"empty attributes test\"",
                         "empty attributes test", dictionary.get("someAttribute"));

            Object children = dictionary.get(childName);
            if (children instanceof String) {
                pids = new String[] { (String) children };
            } else if (children instanceof Vector) {
                Vector<String> v = (Vector<String>) children;
                pids = v.toArray(pids);
            } else {
                pids = (String[]) dictionary.get(childName);
            }

            assertNotNull("There should be a child element", pids);
            assertEquals("There should be only one child element", 1, pids.length);

            Configuration childConfig = configAdmin.getConfiguration(pids[0]);

            assertEquals("Enabled attribute should be true", Boolean.TRUE, childConfig.getProperties().get("enabled"));
            assertNull("The attribute hello should be null", childConfig.getProperties().get("hello"));
            assertEquals("The parent's child pid should equal the child's service.pid value", pids[0], childConfig.getProperties().get(XMLConfigConstants.CFG_SERVICE_PID));
        } finally {
            // Clean up
            writer = readConfiguration();
            writer.deleteConfig(parentName, "parent", false);
            writeConfiguration(writer);
        }

    }

}
