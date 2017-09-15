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

import java.util.Dictionary;

import org.osgi.framework.BundleContext;
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
public class ChildFirstExtendsTestRunner extends BaseTestRunner {

    String parentName = "com.ibm.example.topLevelElement";
    String childName = "com.ibm.example.child.a";
    String child2Name = "com.ibm.example.child.b";
    String child3Name = "com.ibm.example.child.c";
    String topName = "com.ibm.example.top.d";
    String abstractName = "com.ibm.example.supertype";

    private FactoryTest parent;
    private FactoryTest child;
    private FactoryTest child2;
    private FactoryTest child3;
    private FactoryTest top;

    private ConfigurationAdmin configAdmin;

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/child-first-extends", new TestDynamicConfigServlet(), null, null);

        this.parent = new FactoryTest(parentName);
        this.child = new FactoryTest(childName);
        this.child2 = new FactoryTest(child2Name);
        this.child3 = new FactoryTest(child3Name);
        this.top = new FactoryTest(topName);

        addTest(parent);
        addTest(child);
        addTest(child2);
        addTest(child3);
        addTest(top);

        BundleContext bundleContext = context.getBundleContext();
        ServiceReference<?> ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        this.configAdmin = (ConfigurationAdmin) bundleContext.getService(ref);
    }

    public void testChildFirstExtends() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        try {
            // Reset to avoid contamination
            resetFactoryTests();

            /*
             * <topLevelElement id="a">
             * <child.a attrA1="a1"/>
             * </topLevelElement>
             * 
             * <topLevelElement id="a">
             * <child.a attrA2="a2"/>
             * </topLevelElement>
             * 
             * <topLevelElement id="b">
             * <child.b attrB1="b1"/>
             * <child.b attrB2="b2"/>
             * </topLevelElement>
             * 
             * <topLevelElement id="c"/>
             * 
             * <top.d attrD1="d1"/>
             * <top.d attrD1="d1copy"/>
             */

            // *** STEP 1 ***
            // Write initial configuration
            writer = readConfiguration();
            writer.addConfig("<topLevelElement id=\"a\">" +
                             "<child.a attrA1=\"a1\"/>" +
                             "</topLevelElement>");
            writeConfiguration(writer);

            // wait for the initial configuration to be injected.
            dictionary = parent.waitForUpdate("a");
            assertEquals("id should be \"a\"",
                         "a", dictionary.get("id"));

            dictionary = child.waitForUpdate("child.a");
            assertEquals("a1", dictionary.get("attrA1"));
            assertEquals("The child should inherit from the supertype", "value", dictionary.get("commonAttribute"));

            Configuration[] superTypes = configAdmin.listConfigurations("(service.factoryPid=" + abstractName + ")");
            assertNotNull(superTypes);
            assertEquals(1, superTypes.length);
            dictionary = superTypes[0].getProperties();
            assertEquals("value", dictionary.get("commonAttribute"));

        } finally {
            // Clean up
            writer = readConfiguration();
            writer.deleteConfig("topLevelElement", "a", false);
            writeConfiguration(writer);
        }
    }

    /**
     * Reset factories to avoid test contamination
     */
    private void resetFactoryTests() {
        parent.reset();
        child.reset();
        child2.reset();
        child3.reset();
        top.reset();
    }
}
