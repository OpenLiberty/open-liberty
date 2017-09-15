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
package test.server.config.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import test.server.BaseTestRunner;
import test.server.config.dynamic.ConfigWriter;
import test.server.config.dynamic.FactoryNestedTest;
import test.server.config.dynamic.FactoryTest;
import test.server.config.dynamic.SingletonTest;

public class TestRunner extends BaseTestRunner {

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/reference-config-test", new TestDynamicConfigServlet(), null, null);

        BundleContext bundleContext = context.getBundleContext();
        ServiceReference<?> ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        this.configAdmin = (ConfigurationAdmin) bundleContext.getService(ref);

    }

    public void testTwoReferencesOnePid() throws Exception {

        String id = "testTwoReferencesOnePid";

        FactoryTest parent = new FactoryTest("test.config.reference.parent");
        addTest(parent);

        ConfigWriter writer = readConfiguration();

        writer.addConfig("<test.config.reference.parent id=\"" + id + "\">" +
                         "  <widget someProperty=\"widget\"/>" +
                         "  <part someProperty=\"widget\"/>" +
                         "</test.config.reference.parent>");
        writeConfiguration(writer);

        Dictionary<String, Object> dictionary = parent.waitForUpdate(id);
        assertNotNull(dictionary.get("widget"));
        assertNotNull(dictionary.get("part"));

        // Update a widget

        writer = readConfiguration();
        writer.deleteConfig("test.config.reference.parent", id, true);
        writer.addConfig("<test.config.reference.parent id=\"" + id + "\">" +
                         "  <widget someProperty=\"widget 2\"/>" +
                         "  <part someProperty=\"widget\"/>" +
                         "</test.config.reference.parent>");
        writeConfiguration(writer);

        dictionary = parent.waitForUpdate(id);
        assertNotNull(dictionary.get("widget"));
        assertNotNull(dictionary.get("part"));

        // Update a part
        writer = readConfiguration();
        writer.deleteConfig("test.config.reference.parent", id, true);
        writer.addConfig("<test.config.reference.parent id=\"" + id + "\">" +
                         "  <widget someProperty=\"widget 2\"/>" +
                         "  <part someProperty=\"widget 2\"/>" +
                         "</test.config.reference.parent>");
        writeConfiguration(writer);

        // Check that the final configuration is ok
        dictionary = parent.waitForUpdate(id);
        String[] widgets = (String[]) dictionary.get("widget");
        String[] parts = (String[]) dictionary.get("part");

        assertNotNull(widgets);
        assertNotNull(parts);

        assertEquals(1, widgets.length);
        assertEquals(1, parts.length);

        String widgetPid = widgets[0];
        String partPid = parts[0];

        Configuration[] widgetConfigs = configAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + widgetPid + ")");
        Configuration[] partConfigs = configAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + partPid + ")");
        assertNotNull(widgetConfigs);
        assertNotNull(partConfigs);

        assertEquals("There should be one widget in CA", 1, widgetConfigs.length);
        assertEquals("There should be one part in CA", 1, partConfigs.length);

        Dictionary<String, Object> widgetDictionary = widgetConfigs[0].getProperties();
        Dictionary<String, Object> partDictionary = partConfigs[0].getProperties();
        assertEquals("widget 2", widgetDictionary.get("someProperty"));
        assertEquals("widget 2", partDictionary.get("someProperty"));

    }

    public void testReferenceAttribute() throws Exception {
        SingletonTest parent = new SingletonTest("test.config.reference.attribute");
        addTest(parent);

        FactoryNestedTest child = new FactoryNestedTest("test.config.reference.attribute.child");
        addTest(child);

        // wait for the initial configuration to be injected.
        Dictionary<String, Object> parentDictionary = parent.waitForUpdate();
        assertEquals("version property", "3.0", parentDictionary.get("version"));
        assertEquals("maxThreads property", Integer.valueOf(100), parentDictionary.get("maxThreads"));
        String pid = (String) parentDictionary.get("reference");
        assertNotNull("reference pid", pid);

        // check the child info
        Dictionary<String, Object> childDictionary = child.waitForUpdate(pid);
        assertEquals("name property", "one", childDictionary.get("name"));
        assertEquals("threads property", Integer.valueOf(100), childDictionary.get("threads"));

        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        // STEP 1: update child and check if parent is updated
        writer = readConfiguration();
        writer.setValue("childAttributeReference", "ref1", "threads", "500");
        writeConfiguration(writer);

        // check updated child info
        dictionary = child.waitForUpdate(pid);
        assertEquals("name property", "one", dictionary.get("name"));
        assertEquals("threads property", Integer.valueOf(500), dictionary.get("threads"));

        // check parent got updated
        dictionary = parent.waitForUpdate();
        dictionaryEquals(parentDictionary, dictionary);

        // STEP 2: delete child and check if parent is updated
        writer = readConfiguration();
        writer.deleteConfig("childAttributeReference", "ref1", false);
        writeConfiguration(writer);

        // check updated child info
        dictionary = child.waitForUpdate(pid);
        assertNull(dictionary);

        // check parent got updated
        dictionary = parent.waitForUpdate();
        assertEquals("version property", "3.0", dictionary.get("version"));
        assertEquals("maxThreads property", Integer.valueOf(100), dictionary.get("maxThreads"));
        assertNull(dictionary.get("reference"));

        // STEP 3: add child back up and check if parent is updated        
        writer = readConfiguration();
        writer.addConfig("<childAttributeReference id=\"ref1\" name=\"noname\"/>");
        writeConfiguration(writer);

        parentDictionary = parent.waitForUpdate();
        assertEquals("version property", "3.0", parentDictionary.get("version"));
        assertEquals("maxThreads property", Integer.valueOf(100), parentDictionary.get("maxThreads"));
        pid = (String) parentDictionary.get("reference");
        assertNotNull("reference pid", pid);

        // check updated child info
        dictionary = child.waitForUpdate(pid);
        assertEquals("name property", "noname", dictionary.get("name"));
        assertEquals("threads property", Integer.valueOf(300), dictionary.get("threads"));
    }

    public void testReferenceElement() throws Exception {

        SingletonTest parent = new SingletonTest("test.config.reference.element");
        addTest(parent);

        FactoryNestedTest child = new FactoryNestedTest("test.config.reference.element.child");
        addTest(child);

        // wait for the initial configuration to be injected.
        Dictionary<String, Object> parentDictionary = parent.waitForUpdate();
        assertEquals("version property", "4.0", parentDictionary.get("version"));
        assertEquals("maxThreads property", Integer.valueOf(200), parentDictionary.get("maxThreads"));
        // check pids in references
        String[] pids = (String[]) parentDictionary.get("references");
        assertNotNull("reference pids", pids);
        assertEquals("child pids", 4, pids.length);
        // check pids in referenceList
        Vector<String> pidList = (Vector) parentDictionary.get("referenceList");
        assertNotNull("reference pids", pidList);
        assertEquals("child pids", 4, pidList.size());
        assertEquals(pids[0], pidList.get(0));
        assertEquals(pids[1], pidList.get(2));
        assertEquals(pids[2], pidList.get(1));
        // check pids in childRef
        String[] pids2 = (String[]) parentDictionary.get("childRef");
        assertNotNull(pids2);
        assertEquals(2, pids2.length);
        assertEquals(pids[0], pids2[1]);
        assertEquals(pids[1], pids2[0]);
        // these elements should not be in the dictionary
        assertNull(parentDictionary.get("referencesRef"));
        assertNull(parentDictionary.get("referenceListRef"));
        assertNull(parentDictionary.get("child"));

        // check the first reference info
        Dictionary<String, Object> childOneDictionary = child.waitForUpdate(pids[0]);
        assertEquals("name property", "five", childOneDictionary.get("name"));
        assertEquals("threads property", Integer.valueOf(5), childOneDictionary.get("threads"));

        // check the first reference info
        Dictionary<String, Object> childTwoDictionary = child.waitForUpdate(pids[1]);
        assertEquals("name property", "unavailable", childTwoDictionary.get("name"));
        assertEquals("threads property", Integer.valueOf(300), childTwoDictionary.get("threads"));

        // check the second reference info
        Dictionary<String, Object> childThreeDictionary = child.waitForUpdate(pids[2]);
        assertEquals("name property", "two", childThreeDictionary.get("name"));
        assertEquals("threads property", Integer.valueOf(200), childThreeDictionary.get("threads"));

        // check the nested reference from "references"
        Dictionary<String, Object> childFourDictionary = child.waitForUpdate(pids[3]);
        assertEquals("name property", "nested-one", childFourDictionary.get("name"));
        assertEquals("threads property", Integer.valueOf(5000), childFourDictionary.get("threads"));

        // check the nested reference from "referenceList"
        Dictionary<String, Object> childFiveictionary = child.waitForUpdate(pidList.get(3));
        assertEquals("name property", "nested-two", childFiveictionary.get("name"));
        assertEquals("threads property", Integer.valueOf(6000), childFiveictionary.get("threads"));

        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        // STEP 1: update child and check if parent is updated
        writer = readConfiguration();
        writer.setValue("childElementReference", "ref2", "threads", "444");
        writeConfiguration(writer);

        // check updated child info
        dictionary = child.waitForUpdate(pids[2]);
        assertEquals("name property", "two", dictionary.get("name"));
        assertEquals("threads property", Integer.valueOf(444), dictionary.get("threads"));

        // check parent got updated
        dictionary = parent.waitForUpdate();
        dictionaryEquals(parentDictionary, dictionary);

        // STEP 2: delete child two and check if parent is updated
        writer = readConfiguration();
        writer.deleteConfig("childElementReference", "ref3", false);
        writeConfiguration(writer);

        // check updated child info
        dictionary = child.waitForUpdate(pids[1]);
        assertNull(dictionary);

        // check parent got updated
        parentDictionary = parent.waitForUpdate();
        assertEquals("version property", "4.0", parentDictionary.get("version"));
        assertEquals("maxThreads property", Integer.valueOf(200), parentDictionary.get("maxThreads"));
        // check pids in references
        String[] newPids = (String[]) parentDictionary.get("references");
        assertNotNull("reference pids", newPids);
        assertEquals("child pids", 3, newPids.length);
        assertEquals(pids[0], newPids[0]);
        assertEquals(pids[2], newPids[1]);
        assertEquals(pids[3], newPids[2]);
        // check pids in referenceList
        Vector<String> newPidList = (Vector) parentDictionary.get("referenceList");
        assertNotNull("reference pids", newPidList);
        assertEquals("child pids", 3, newPidList.size());
        assertEquals(pidList.get(0), newPidList.get(0));
        assertEquals(pids[2], newPidList.get(1));
        assertEquals(pidList.get(3), newPidList.get(2));
        // check pids in childRef
        String[] newPids2 = (String[]) parentDictionary.get("childRef");
        assertNotNull(newPids2);
        assertEquals(1, newPids2.length);
        assertEquals(pids[0], newPids2[0]);
        // these elements should not be in the dictionary
        assertNull(parentDictionary.get("referencesRef"));
        assertNull(parentDictionary.get("referenceListRef"));
        assertNull(parentDictionary.get("child"));

        // STEP 3: add child two back up and check if parent is updated        
        writer = readConfiguration();
        writer.addConfig("<childElementReference id=\"ref3\" name=\"noname\" threads=\"666\"/>");
        writeConfiguration(writer);

        // check parent got updated
        parentDictionary = parent.waitForUpdate();
        assertEquals("version property", "4.0", parentDictionary.get("version"));
        assertEquals("maxThreads property", Integer.valueOf(200), parentDictionary.get("maxThreads"));
        // check pids in references
        pids = (String[]) parentDictionary.get("references");
        assertNotNull("reference pids", pids);
        assertEquals("child pids", 4, pids.length);
        assertEquals(newPids[0], pids[0]);
        assertEquals(newPids[1], pids[2]);
        assertEquals(newPids[2], pids[3]);
        // check pids in referenceList
        pidList = (Vector) parentDictionary.get("referenceList");
        assertNotNull("reference pids", pidList);
        assertEquals("child pids", 4, pidList.size());
        assertEquals(pids[0], pidList.get(0));
        assertEquals(pids[1], pidList.get(2));
        assertEquals(pids[2], pidList.get(1));
        assertEquals(newPidList.get(2), pidList.get(3));
        // check pids in childRef
        pids2 = (String[]) parentDictionary.get("childRef");
        assertNotNull(pids2);
        assertEquals(2, pids2.length);
        assertEquals(pids[0], pids2[1]);
        assertEquals(pids[1], pids2[0]);
        // these elements should not be in the dictionary
        assertNull(parentDictionary.get("referencesRef"));
        assertNull(parentDictionary.get("referenceListRef"));
        assertNull(parentDictionary.get("child"));

        // check updated child info
        dictionary = child.waitForUpdate(pids[1]);
        assertEquals("name property", "noname", dictionary.get("name"));
        assertEquals("threads property", Integer.valueOf(666), dictionary.get("threads"));
    }

    public void testReferenceUpdates() throws Exception {

        FactoryTest nodeA = new FactoryTest("test.config.reference.node.a", "uniqueName");
        addTest(nodeA);

        FactoryTest nodeB = new FactoryTest("test.config.reference.node.b");
        addTest(nodeB);

        FactoryTest nodeC = new FactoryTest("test.config.reference.node.c");
        addTest(nodeC);

        Dictionary<String, Object> nodeADictionary = nodeA.waitForUpdate("foo");
        String nodeBOnePid = (String) nodeADictionary.get("nodeRef");
        assertNotNull(nodeBOnePid);

        Dictionary<String, Object> nodeCDictionary = nodeC.waitForUpdate("three");
        assertEquals("ford", nodeCDictionary.get("name"));
        String nodeCPid = nodeC.getPid("three");

        Dictionary<String, Object> nodeBOneDictionary = nodeB.waitForUpdate("one");
        assertEquals(Integer.valueOf(100), nodeBOneDictionary.get("value"));
        assertEquals(nodeBOnePid, nodeB.getPid("one"));
        assertEquals(nodeCPid, nodeBOneDictionary.get("nodeRef"));

        Dictionary<String, Object> nodeBTwoDictionary = nodeB.waitForUpdate("two");
        assertEquals(Integer.valueOf(200), nodeBTwoDictionary.get("value"));
        assertEquals(nodeBOnePid, nodeB.getPid("one"));
        assertEquals(nodeCPid, nodeBTwoDictionary.get("nodeRef"));

        ConfigWriter writer;
        Dictionary<String, Object> dictionary;

        // Test 1: Update nodeB one - that should update nodeA
        writer = readConfiguration();
        writer.setValue("nodeB", "one", "value", "5555");
        writeConfiguration(writer);

        nodeBOneDictionary = nodeB.waitForUpdate("one");
        assertEquals(Integer.valueOf(5555), nodeBOneDictionary.get("value"));
        assertEquals(nodeBOnePid, nodeB.getPid("one"));
        assertEquals(nodeCPid, nodeBOneDictionary.get("nodeRef"));

        dictionary = nodeA.waitForUpdate("foo");
        dictionaryEquals(nodeADictionary, dictionary);

        // Test 2: Update nodeC - that should update everything!
        writer = readConfiguration();
        writer.setValue("nodeC", "three", "name", "honda");
        writeConfiguration(writer);

        nodeCDictionary = nodeC.waitForUpdate("three");
        assertEquals("honda", nodeCDictionary.get("name"));

        dictionary = nodeB.waitForUpdate("one");
        dictionaryEquals(nodeBOneDictionary, dictionary);

        dictionary = nodeB.waitForUpdate("two");
        dictionaryEquals(nodeBTwoDictionary, dictionary);

        dictionary = nodeA.waitForUpdate("foo");
        dictionaryEquals(nodeADictionary, dictionary);
    }

}
