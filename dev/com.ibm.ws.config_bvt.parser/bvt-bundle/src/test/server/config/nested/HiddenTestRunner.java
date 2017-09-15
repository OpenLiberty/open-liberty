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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;

import test.server.BaseTestRunner;
import test.server.config.dynamic.ConfigWriter;
import test.server.config.dynamic.FactoryTest;

/**
 *
 */
public class HiddenTestRunner extends BaseTestRunner {
    private final String grandparentName = "test.config.hidden.grandparent";
    private final String parentName = "test.config.hidden.parent";
    private final String childName = "test.config.hidden.child";
    private final String childExtended = "test.config.hidden.child.extended";

    private FactoryTest grandparent;
    private FactoryTest parent;
    private FactoryTest child;
    private FactoryTest extendedChild;

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/hidden-test", new TestDynamicConfigServlet(), null, null);

        this.grandparent = new FactoryTest(grandparentName);
        this.parent = new FactoryTest(parentName);
        this.child = new FactoryTest(childName);
        this.extendedChild = new FactoryTest(childExtended);

        addTest(grandparent);
        addTest(parent);
        addTest(child);
        addTest(extendedChild);
    }

    public void testHiddenChildWithExtends() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        try {
            // Reset to avoid contamination
            grandparent.reset();
            parent.reset();
            extendedChild.reset();

            // *** STEP 1 ***
            // Write initial configuration
            writer = readConfiguration();
            writer.addConfig("<grandparentOfHiddenElement id=\"grandparent\" name=\"hidden grandparent\">" +
                             " <parentOfHiddenElement id=\"parent\" name=\"hidden parent\">" +
                             "  <hiddenExtended id=\"child\" name=\"I am an extended element\"/>" +
                             " </parentOfHiddenElement>" +
                             "</grandparentOfHiddenElement>");
            writeConfiguration(writer);

            // wait for the initial configuration to be injected.
            dictionary = grandparent.waitForUpdate("grandparent");
            assertEquals("name should be \"hidden grandparent\"",
                         "hidden grandparent", dictionary.get("name"));

            dictionary = parent.waitForUpdate("parent");
            assertEquals("name should be \"hidden parent\"",
                         "hidden parent", dictionary.get("name"));

            dictionary = extendedChild.waitForUpdate("child");
            assertEquals("The name for the child should be I am an extended element", "I am an extended element", dictionary.get("name"));
            assertEquals("The child should inherit from the supertype", "inheritedProperty", dictionary.get("abstractProperty"));

            // *** STEP 2 ***
            // Update child
            writer = readConfiguration();
            writer.deleteConfig("grandparentOfHiddenElement", "grandparent", false);
            writer.addConfig("<grandparentOfHiddenElement id=\"grandparent\" name=\"hidden grandparent\">" +
                             " <parentOfHiddenElement id=\"parent\" name=\"hidden parent\">" +
                             "  <hiddenExtended id=\"child\" name=\"Updated extended element\"/>" +
                             " </parentOfHiddenElement>" +
                             "</grandparentOfHiddenElement>");
            writeConfiguration(writer);

            dictionary = extendedChild.waitForUpdate("child");
            assertEquals("The child name should be Updated extended element", "Updated extended element", dictionary.get("name"));

            // Check that the parent dictionary isn't updated
            assertFalse("There should not be an update to the parent", parent.hasDictionary("parent"));

            // Check that the grandparent dictionary isn't updated
            assertFalse("There should not be an update to the grandparent", grandparent.hasDictionary("grandparent"));

            // *** STEP 3 ***
            // Update parent
            writer = readConfiguration();
            writer.deleteConfig("grandparentOfHiddenElement", "grandparent", false);
            writer.addConfig("<grandparentOfHiddenElement id=\"grandparent\" name=\"hidden grandparent\">" +
                             " <parentOfHiddenElement id=\"parent\" name=\"hidden parent updated\">" +
                             "  <hiddenExtended id=\"child\" name=\"Updated extended element\"/>" +
                             " </parentOfHiddenElement>" +
                             "</grandparentOfHiddenElement>");
            writeConfiguration(writer);

            // Check that the child wasn't updated
            assertFalse("The child should not be updated", extendedChild.hasDictionary("child"));

            // Check parent update
            dictionary = parent.waitForUpdate("parent");
            assertEquals("name should be \"hidden parent updated\"",
                         "hidden parent updated", dictionary.get("name"));

            // Check grandparent update
            dictionary = grandparent.waitForUpdate("grandparent");
            assertEquals("name should be \"hidden grandparent\"",
                         "hidden grandparent", dictionary.get("name"));

            // *** STEP 4 ***
            // Delete child -- This should not result in an update to the parent
            writer = readConfiguration();
            writer.deleteConfig("grandparentOfHiddenElement", "grandparent", false);
            writer.addConfig("<grandparentOfHiddenElement id=\"grandparent\" name=\"hidden grandparent\">" +
                             " <parentOfHiddenElement id=\"parent\" name=\"hidden parent updated\">" +
                             " </parentOfHiddenElement>" +
                             "</grandparentOfHiddenElement>");
            writeConfiguration(writer);

            // Check that the child was deleted
            assertNull("The child dictionary should be null", extendedChild.waitForUpdate("child"));

            // Check that the parent dictionary isn't updated
            assertFalse("There should not be an update to the parent", parent.hasDictionary("parent"));

            // Check that the grandparent dictionary isn't updated
            assertFalse("There should not be an update to the grandparent", grandparent.hasDictionary("grandparent"));

            // *** STEP 5 ***
            // Put the child back -- This should not result in an update to the parent
            writer = readConfiguration();
            writer.deleteConfig("grandparentOfHiddenElement", "grandparent", false);
            writer.addConfig("<grandparentOfHiddenElement id=\"grandparent\" name=\"hidden grandparent\">" +
                             " <parentOfHiddenElement id=\"parent\" name=\"hidden parent updated\">" +
                             "  <hiddenExtended id=\"child\" name=\"Returned extended element\"/>" +
                             " </parentOfHiddenElement>" +
                             "</grandparentOfHiddenElement>");
            writeConfiguration(writer);

            // Check that the child was updated
            dictionary = extendedChild.waitForUpdate("child");
            assertEquals("The name for the child should be Returned extended element", "Returned extended element", dictionary.get("name"));
            assertEquals("The child should inherit from the supertype", "inheritedProperty", dictionary.get("abstractProperty"));

            // Check that the parent dictionary isn't updated
            assertFalse("There should not be an update to the parent", parent.hasDictionary("parent"));

            // Check that the grandparent dictionary isn't updated
            assertFalse("There should not be an update to the grandparent", grandparent.hasDictionary("grandparent"));

        } finally {
            // Clean up
            writer = readConfiguration();
            writer.deleteConfig("grandparentOfHiddenElement", "grandparent", false);
            writeConfiguration(writer);
            try {
                grandparent.waitForUpdate("grandparent");
            } catch (Exception ex) {
                ex.printStackTrace();
                // We don't care. Just waiting to make sure server update has gone through. If something went wrong earlier, that might not happen. 
            }
        }
    }

    public void testHiddenChild() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        try {

            // Reset to avoid contamination
            grandparent.reset();
            parent.reset();
            child.reset();

            // *** STEP 1 ***
            // Write initial configuration
            writer = readConfiguration();
            writer.addConfig("<grandparentOfHiddenElement id=\"grandparent\" name=\"hidden grandparent\">" +
                             " <parentOfHiddenElement id=\"parent\" name=\"hidden parent\">" +
                             "  <hidden id=\"child\" name=\"I am stealthy!\"/>" +
                             " </parentOfHiddenElement>" +
                             "</grandparentOfHiddenElement>");
            writeConfiguration(writer);

            // wait for the initial configuration to be injected.
            dictionary = grandparent.waitForUpdate("grandparent");
            assertEquals("name should be \"hidden grandparent\"",
                         "hidden grandparent", dictionary.get("name"));

            dictionary = parent.waitForUpdate("parent");
            assertEquals("name should be \"hidden parent\"",
                         "hidden parent", dictionary.get("name"));

            dictionary = child.waitForUpdate("child");
            assertEquals("The name for the child should be I am stealthy!", "I am stealthy!", dictionary.get("name"));

            // *** STEP 2 ***
            // Update child
            writer = readConfiguration();
            writer.deleteConfig("grandparentOfHiddenElement", "grandparent", false);
            writer.addConfig("<grandparentOfHiddenElement id=\"grandparent\" name=\"hidden grandparent\">" +
                             " <parentOfHiddenElement id=\"parent\" name=\"hidden parent\">" +
                             "  <hidden id=\"child\" name=\"You can't see me.\"/>" +
                             " </parentOfHiddenElement>" +
                             "</grandparentOfHiddenElement>");
            writeConfiguration(writer);

            dictionary = child.waitForUpdate("child");
            assertEquals("The child name should be You can't see me.", "You can't see me.", dictionary.get("name"));

            // Check that the parent dictionary isn't updated
            assertFalse("There should not be an update to the parent", parent.hasDictionary("parent"));

            // Check that the grandparent dictionary isn't updated
            assertFalse("There should not be an update to the grandparent", grandparent.hasDictionary("grandparent"));

            // *** STEP 3 ***
            // Update parent
            writer = readConfiguration();
            writer.deleteConfig("grandparentOfHiddenElement", "grandparent", false);
            writer.addConfig("<grandparentOfHiddenElement id=\"grandparent\" name=\"hidden grandparent\">" +
                             " <parentOfHiddenElement id=\"parent\" name=\"hidden parent updated\">" +
                             "  <hidden id=\"child\" name=\"You can't see me.\"/>" +
                             " </parentOfHiddenElement>" +
                             "</grandparentOfHiddenElement>");
            writeConfiguration(writer);

            // Check that the child wasn't updated
            assertFalse("The child should not be updated", child.hasDictionary("child"));

            // Check parent update
            dictionary = parent.waitForUpdate("parent");
            assertEquals("name should be \"hidden parent updated\"",
                         "hidden parent updated", dictionary.get("name"));

            // Check grandparent update
            dictionary = grandparent.waitForUpdate("grandparent");
            assertEquals("name should be \"hidden grandparent\"",
                         "hidden grandparent", dictionary.get("name"));

            // *** STEP 4 ***
            // Delete child -- This should not result in an update to the parent
            writer = readConfiguration();
            writer.deleteConfig("grandparentOfHiddenElement", "grandparent", false);
            writer.addConfig("<grandparentOfHiddenElement id=\"grandparent\" name=\"hidden grandparent\">" +
                             " <parentOfHiddenElement id=\"parent\" name=\"hidden parent updated\">" +
                             " </parentOfHiddenElement>" +
                             "</grandparentOfHiddenElement>");
            writeConfiguration(writer);

            // Check that the child wasn't updated
            assertNull("The child dictionary should be null", child.waitForUpdate("child"));

            // Check that the parent dictionary isn't updated
            assertFalse("There should not be an update to the parent", parent.hasDictionary("parent"));

            // Check that the grandparent dictionary isn't updated
            assertFalse("There should not be an update to the grandparent", grandparent.hasDictionary("grandparent"));

            // *** STEP 5 ***
            // Put the child back -- This should not result in an update to the parent
            writer = readConfiguration();
            writer.deleteConfig("grandparentOfHiddenElement", "grandparent", false);
            writer.addConfig("<grandparentOfHiddenElement id=\"grandparent\" name=\"hidden grandparent\">" +
                             " <parentOfHiddenElement id=\"parent\" name=\"hidden parent updated\">" +
                             "  <hidden id=\"child\" name=\"I'm back!\"/>" +
                             " </parentOfHiddenElement>" +
                             "</grandparentOfHiddenElement>");
            writeConfiguration(writer);

            // Check that the child was updated
            dictionary = child.waitForUpdate("child");
            assertEquals("The name for the child should be I'm back!", "I'm back!", dictionary.get("name"));

            // Check that the parent dictionary isn't updated
            assertFalse("There should not be an update to the parent", parent.hasDictionary("parent"));

            // Check that the grandparent dictionary isn't updated
            assertFalse("There should not be an update to the grandparent", grandparent.hasDictionary("grandparent"));

        } finally {
            // Clean up
            writer = readConfiguration();
            writer.deleteConfig("grandparentOfHiddenElement", "grandparent", false);
            writeConfiguration(writer);
            try {
                grandparent.waitForUpdate("grandparent");
                parent.waitForUpdate("parent");
            } catch (Exception ex) {
                ex.printStackTrace();
                // We don't care. Just waiting to make sure server update has gone through. If something went wrong earlier, that might not happen. 
            }
        }
    }

}
