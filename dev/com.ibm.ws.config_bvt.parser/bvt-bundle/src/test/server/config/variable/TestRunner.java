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
package test.server.config.variable;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;

import test.server.BaseTestRunner;
import test.server.config.dynamic.ConfigWriter;
import test.server.config.dynamic.FactoryTest;

public class TestRunner extends BaseTestRunner {

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/variable-config-test", new TestDynamicConfigServlet(), null, null);
    }

    public void testVariableChange() throws Exception {

        FactoryTest parent = new FactoryTest("test.config.variable.factory");
        addTest(parent);

        // wait for the initial configurations to be injected.
        Dictionary<String, Object> oneDictionary = parent.waitForUpdate("one");
        assertEquals("port", Integer.valueOf(1234), oneDictionary.get("port"));
        assertEquals("host", "localhost", oneDictionary.get("host"));
        assertEquals("ipAddress", "${localIpAddress}", oneDictionary.get("ipAddress"));

        Dictionary<String, Object> twoDictionary = parent.waitForUpdate("two");
        assertEquals("port", Integer.valueOf(5678), twoDictionary.get("port"));
        assertEquals("host", "127.0.0.1", twoDictionary.get("host"));
        assertEquals("ipAddress", "${localIpAddress}", twoDictionary.get("ipAddress"));

        ConfigWriter writer;

        // STEP 1: change hostname variable
        writer = readConfiguration();
        writer.setValue("variable", "hostname", "name", "value", "ibm.com");
        writeConfiguration(writer);

        // wait for instance one to update
        oneDictionary = parent.waitForUpdate("one");
        assertEquals("port", Integer.valueOf(1234), oneDictionary.get("port"));
        assertEquals("host", "ibm.com", oneDictionary.get("host"));
        assertEquals("ipAddress", "${localIpAddress}", oneDictionary.get("ipAddress"));

        // STEP 2: add localIpAdress variable
        writer = readConfiguration();
        writer.addConfig("<variable name=\"localIpAddress\" value=\"192.168.1.1\"/>");
        writeConfiguration(writer);

        // wait for instance one update
        oneDictionary = parent.waitForUpdate("one");
        assertEquals("port", Integer.valueOf(1234), oneDictionary.get("port"));
        assertEquals("host", "ibm.com", oneDictionary.get("host"));
        assertEquals("ipAddress", "192.168.1.1", oneDictionary.get("ipAddress"));

        // wait for instance two update
        twoDictionary = parent.waitForUpdate("two");
        assertEquals("port", Integer.valueOf(5678), twoDictionary.get("port"));
        assertEquals("host", "127.0.0.1", twoDictionary.get("host"));
        assertEquals("ipAddress", "192.168.1.1", twoDictionary.get("ipAddress"));

        // STEP 3: set localHostNames variable
        writer = readConfiguration();
        writer.addConfig("<variable name=\"localHostName\" value=\"w3.ibm.com\"/>");
        writeConfiguration(writer);

        // wait for instance two update
        twoDictionary = parent.waitForUpdate("two");
        assertEquals("port", Integer.valueOf(5678), twoDictionary.get("port"));
        assertEquals("host", "w3.ibm.com", twoDictionary.get("host"));
        assertEquals("ipAddress", "192.168.1.1", twoDictionary.get("ipAddress"));
    }

}
