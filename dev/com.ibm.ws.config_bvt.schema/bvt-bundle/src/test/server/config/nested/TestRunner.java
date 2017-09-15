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
package test.server.config.nested;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import test.server.BaseTestRunner;
import test.server.config.dynamic.ConfigWriter;
import test.server.config.dynamic.FactoryNestedTest;
import test.server.config.dynamic.FactoryTest;
import test.server.config.dynamic.SingletonTest;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;

public class TestRunner extends BaseTestRunner {

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/nested-config-test", new TestDynamicConfigServlet(), null, null);
    }

    public void testNested() throws Exception {
        testNested("test.config.nested.singleton", "test.config.nested.singleton",
                   "test.config.nested.child", "test.config.nested.child");
    }

    public void testNestedMetatype() throws Exception {
        testNested("nestedSingleton", "test.config.nested.singleton.metatype",
                   "nestedChildOne", "test.config.nested.child.one.metatype");
    }

    @SuppressWarnings("unchecked")
    private void testNested(String parentName, String parentPid, String childName, String childPid) throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;
        String[] pids;

        SingletonTest parent = new SingletonTest(parentPid);
        addTest(parent);

        FactoryNestedTest child = new FactoryNestedTest(childPid);
        addTest(child);

        // *** STEP 1 ***
        // wait for the initial configuration to be injected.
        dictionary = parent.waitForUpdate();

        // *** STEP 2 ***
        // add first nested child
        writer = readConfiguration();
        writer.deleteConfig(parentName, null, false);
        writer.addConfig("<{0} name=\"myNestedSingleton\">" +
                         "  <{1} name=\"myNestedChildOne\" a=\"b\" value=\"10\"/>" +
                         "</{0}>", parentName, childName);
        writeConfiguration(writer);

        // parent gets injected with the first child pid
        dictionary = parent.waitForUpdate();
        assertEquals("name property", "myNestedSingleton", dictionary.get("name"));
        String parentServicePid = (String) dictionary.get(XMLConfigConstants.CFG_SERVICE_PID);
        pids = (String[]) dictionary.get(childName);
        assertNotNull("child pids", pids);
        assertEquals("child pids", 1, pids.length);
        String firstChildPid = pids[0];

        // first child instance is created       
        dictionary = child.waitForUpdate(firstChildPid);
        assertEquals("name property", "myNestedChildOne", dictionary.get("name"));
        assertEquals("value property", "10", dictionary.get("value"));
        assertEquals("a property", "b", dictionary.get("a"));

        assertEquals("Nested Child config.parentPID should be equal to parent's service.pid", parentServicePid,
                     dictionary.get(XMLConfigConstants.CFG_PARENT_PID));

        // *** STEP 3 ***
        // add second nested child
        writer = readConfiguration();
        writer.deleteConfig(parentName, null, false);
        writer.addConfig("<{0} name=\"myNestedSingleton\">" +
                         "  <{1} name=\"myNestedChildOne\" a=\"b\" value=\"10\"/>" +
                         "  <{1} name=\"myNestedChildTwo\" b=\"c\" value=\"20\"/>" +
                         "</{0}>", parentName, childName);
        writeConfiguration(writer);

        // parent gets injected with the second child pid
        dictionary = parent.waitForUpdate();
        assertEquals("name property", "myNestedSingleton", dictionary.get("name"));
        pids = (String[]) dictionary.get(childName);
        assertNotNull("child pids", pids);
        assertEquals("child pids", 2, pids.length);
        Set<String> pidSet = toSet(pids);
        assertTrue("first child pid", pidSet.contains(firstChildPid));
        pidSet.remove(firstChildPid);
        String secondChildPid = pidSet.iterator().next();

        // second child instance is created
        dictionary = child.waitForUpdate(secondChildPid);
        assertEquals("name property", "myNestedChildTwo", dictionary.get("name"));
        assertEquals("value property", "20", dictionary.get("value"));
        assertEquals("b property", "c", dictionary.get("b"));

        // *** STEP 4 ***
        // update second child and remove first
        writer = readConfiguration();
        writer.deleteConfig(parentName, null, false);
        writer.addConfig("<{0} name=\"myNestedSingleton\">" +
                         "  <{1} name=\"myNestedChildTwo\" b=\"c\" value=\"40\"/>" +
                         "</{0}>", parentName, childName);
        writeConfiguration(writer);

        // parent gets updated with a single pid
        dictionary = parent.waitForUpdate();
        assertEquals("name property", "myNestedSingleton", dictionary.get("name"));
        pids = (String[]) dictionary.get(childName);
        assertNotNull("child pids", pids);
        assertEquals("child pids", 1, pids.length);

        String deletedPid = null;
        String updatedPid = null;

        if (firstChildPid.equals(pids[0])) {
            updatedPid = firstChildPid;
            deletedPid = secondChildPid;
        } else if (secondChildPid.equals(pids[0])) {
            updatedPid = secondChildPid;
            deletedPid = firstChildPid;
        } else {
            fail("Unexpected child pid: " + pids[0]);
        }

        // one child instance is removed
        dictionary = child.waitForUpdate(deletedPid);
        assertNull("child removed", dictionary);

        // one child instance is updated
        dictionary = child.waitForUpdate(updatedPid);
        assertEquals("name property", "myNestedChildTwo", dictionary.get("name"));
        assertEquals("value property", "40", dictionary.get("value"));
        assertNull("a property", dictionary.get("a"));
        assertEquals("b property", "c", dictionary.get("b"));

        // *** STEP 5 ***
        // delete parent configuration
        writer = readConfiguration();
        writer.deleteConfig(parentName, null, false);
        writeConfiguration(writer);

        // remaining child instance is removed
        dictionary = child.waitForUpdate(updatedPid);
        assertNull("child was not removed", dictionary);

        // parent is removed
        dictionary = parent.waitForUpdate();
        assertNull("parent was not removed", dictionary);
    }

    @SuppressWarnings("unchecked")
    public void testNestedReferences() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;
        String[] pids;

        String parentName = "test.config.nested.singleton.ref";
        SingletonTest parent = new SingletonTest(parentName);
        addTest(parent);

        String childName = "test.config.nested.child.ref.1";
        FactoryNestedTest child = new FactoryNestedTest(childName);
        addTest(child);

        // *** STEP 1 ***
        // wait for the initial configuration to be injected.
        dictionary = parent.waitForUpdate();

        // *** STEP 2 ***
        // add first nested child
        writer = readConfiguration();
        writer.addConfig("<{0} id=\"1\" name=\"myNestedChildOne\" a=\"b\" value=\"10\"/>", childName);
        writer.deleteConfig(parentName, null, false);
        writer.addConfig("<{0} name=\"myNestedSingleton\">" +
                         "  <{1} ref=\"1\" />" +
                         "</{0}>", parentName, childName);
        writeConfiguration(writer);

        // parent gets injected with the first child pid
        dictionary = parent.waitForUpdate();
        assertEquals("name property", "myNestedSingleton", dictionary.get("name"));
        pids = (String[]) dictionary.get(childName);
        assertNotNull("child pids", pids);
        assertEquals("child pids", 1, pids.length);
        String firstChildPid = pids[0];

        // first child instance is created       
        dictionary = child.waitForUpdate(firstChildPid);
        assertEquals("name property", "myNestedChildOne", dictionary.get("name"));
        assertEquals("value property", "10", dictionary.get("value"));
        assertEquals("a property", "b", dictionary.get("a"));

        // *** STEP 3 ***
        // add second nested child
        writer = readConfiguration();
        writer.addConfig("<{0} id=\"2\" name=\"myNestedChildTwo\" b=\"c\" value=\"20\"/>", childName);
        writer.deleteConfig(parentName, null, false);
        writer.addConfig("<{0} name=\"myNestedSingleton\">" +
                         "  <{1} ref=\"1\" />" +
                         "  <{1} ref=\"2\" />" +
                         "</{0}>", parentName, childName);
        writeConfiguration(writer);

        // parent gets injected with the second child pid
        dictionary = parent.waitForUpdate();
        assertEquals("name property", "myNestedSingleton", dictionary.get("name"));
        pids = (String[]) dictionary.get(childName);
        assertNotNull("child pids", pids);
        assertEquals("child pids", 2, pids.length);
        Set<String> pidSet = toSet(pids);
        assertTrue("first child pid", pidSet.contains(firstChildPid));
        pidSet.remove(firstChildPid);
        String secondChildPid = pidSet.iterator().next();

        // second child instance is created
        dictionary = child.waitForUpdate(secondChildPid);
        assertEquals("name property", "myNestedChildTwo", dictionary.get("name"));
        assertEquals("value property", "20", dictionary.get("value"));
        assertEquals("b property", "c", dictionary.get("b"));

        // *** STEP 4 ***
        // update second child and remove first
        writer = readConfiguration();
        writer.deleteConfig(childName, "1", false);
        writer.setValue(childName, "2", "value", "40");
        writeConfiguration(writer);

        // parent gets updated with a single pid
        dictionary = parent.waitForUpdate();
        assertEquals("name property", "myNestedSingleton", dictionary.get("name"));
        pids = (String[]) dictionary.get(childName);
        assertNotNull("child pids", pids);
        assertEquals("child pids", 1, pids.length);

        String deletedPid = null;
        String updatedPid = null;

        if (firstChildPid.equals(pids[0])) {
            updatedPid = firstChildPid;
            deletedPid = secondChildPid;
        } else if (secondChildPid.equals(pids[0])) {
            updatedPid = secondChildPid;
            deletedPid = firstChildPid;
        } else {
            fail("Unexpected child pid: " + pids[0]);
        }

        // one child instance is removed
        dictionary = child.waitForUpdate(deletedPid);
        assertNull("child removed", dictionary);

        // one child instance is updated
        dictionary = child.waitForUpdate(updatedPid);
        assertEquals("name property", "myNestedChildTwo", dictionary.get("name"));
        assertEquals("value property", "40", dictionary.get("value"));
        assertNull("a property", dictionary.get("a"));
        assertEquals("b property", "c", dictionary.get("b"));

        // *** STEP 5 ***
        // delete parent configuration
        writer = readConfiguration();
        writer.deleteConfig(parentName, null, false);
        writeConfiguration(writer);

        // parent is removed
        dictionary = parent.waitForUpdate();
        assertNull("parent was not removed", dictionary);

        // remaining child is still there
        Configuration[] configs = configAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + updatedPid + ")");
        assertNotNull(configs);
        assertEquals(1, configs.length);
    }

    public void testNestedFactoryReferences() throws Exception {
        testNestedFactoryReferences("test.config.nested.factory.ref", "test.config.nested.factory.ref",
                                    "test.config.nested.child.ref.2", "test.config.nested.child.ref.2");
    }

    public void testNestedMetatypeFactoryReferences() throws Exception {
        testNestedFactoryReferences("nestedFactory", "test.config.nested.factory.metatype",
                                    "nestedChildTwo", "test.config.nested.child.two.metatype");
    }

    @SuppressWarnings("unchecked")
    private void testNestedFactoryReferences(String parentName, String parentPid, String childName, String childPid) throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;
        String[] pids;

        FactoryTest parent = new FactoryTest(parentPid);
        addTest(parent);

        FactoryNestedTest child = new FactoryNestedTest(childPid);
        addTest(child);

        // *** STEP 1 ***
        // add initial configurations
        writer = readConfiguration();
        writer.addConfig("<{0} id=\"1\" name=\"myNestedChildOne\" a=\"b\" value=\"10\"/>", childName);
        writer.deleteConfig(parentName, null, true);
        writer.addConfig("<{0} id=\"one\" name=\"myNestedFactoryOne\">" +
                         "  <{1} ref=\"1\" />" +
                         "</{0}>", parentName, childName);
        writer.addConfig("<{0} id=\"two\" name=\"myNestedFactoryTwo\">" +
                         "  <{1} ref=\"1\" />" +
                         "</{0}>", parentName, childName);
        writeConfiguration(writer);

        // parents get injected with the first child pid
        Dictionary<String, Object> dictionaryOne = parent.waitForUpdate("one");
        assertEquals("name property", "myNestedFactoryOne", dictionaryOne.get("name"));
        String[] onePids = (String[]) dictionaryOne.get(childName);
        assertNotNull("child pids", onePids);
        assertEquals("child pids", 1, onePids.length);

        Dictionary<String, Object> dictionaryTwo = parent.waitForUpdate("two");
        assertEquals("name property", "myNestedFactoryTwo", dictionaryTwo.get("name"));
        String[] twoPids = (String[]) dictionaryTwo.get(childName);
        assertNotNull("child pids", twoPids);
        assertEquals("child pids", 1, twoPids.length);

        // both pids should be the same
        assertEquals("child pids are not the same", onePids[0], twoPids[0]);

        // child "1" is created       
        dictionary = child.waitForUpdate(onePids[0]);
        assertEquals("name property", "myNestedChildOne", dictionary.get("name"));
        assertEquals("value property", "10", dictionary.get("value"));
        assertEquals("a property", "b", dictionary.get("a"));

        // *** STEP 2 ***
        // add "2" child, modify "1" child, and add reference to "2" child to "one" parent
        writer = readConfiguration();
        writer.addConfig("<{0} id=\"2\" name=\"myNestedChildTwo\" b=\"c\" value=\"20\"/>", childName);
        writer.setValue(childName, "1", "value", "40");
        writer.deleteConfig(parentName, "one", false);
        writer.addConfig("<{0} id=\"one\" name=\"myNestedFactoryOne\">" +
                         "  <{1} ref=\"1\" />" +
                         "  <{1} ref=\"2\" />" +
                         "</{0}>", parentName, childName);
        writeConfiguration(writer);

        // "two" parent should be notified and have no changes
        dictionary = parent.waitForUpdate("two");
        dictionaryEquals(dictionaryTwo, dictionary);

        // "one" parent should be notified and have 2 pids
        dictionary = parent.waitForUpdate("one");
        assertEquals("name property", "myNestedFactoryOne", dictionary.get("name"));
        pids = (String[]) dictionary.get(childName);
        assertNotNull("child pids", pids);
        assertEquals("child pids", 2, pids.length);
        Set<String> pidSet = toSet(pids);
        assertTrue(pidSet.contains(onePids[0]));
        pidSet.remove(onePids[0]);
        String child2Pid = pidSet.iterator().next();

        // child "1" is updated       
        dictionary = child.waitForUpdate(onePids[0]);
        assertEquals("name property", "myNestedChildOne", dictionary.get("name"));
        assertEquals("value property", "40", dictionary.get("value"));
        assertEquals("a property", "b", dictionary.get("a"));

        // child "2" is created       
        dictionary = child.waitForUpdate(child2Pid);
        assertEquals("name property", "myNestedChildTwo", dictionary.get("name"));
        assertEquals("value property", "20", dictionary.get("value"));
        assertEquals("b property", "c", dictionary.get("b"));

        // *** STEP 3 ****
        // delete "1" child
        writer = readConfiguration();
        writer.deleteConfig(childName, "1", false);
        writeConfiguration(writer);

        // child "1" is deleted       
        dictionary = child.waitForUpdate(onePids[0]);
        assertNull(dictionary);

        // parent "two" has no pids
        dictionary = parent.waitForUpdate("two");
        assertEquals("name property", "myNestedFactoryTwo", dictionary.get("name"));
        assertEquals(Arrays.asList(), Arrays.asList((String[]) dictionary.get(childName)));

        // parent "one" has one pid
        dictionary = parent.waitForUpdate("one");
        assertEquals("name property", "myNestedFactoryOne", dictionary.get("name"));
        pids = (String[]) dictionary.get(childName);
        assertNotNull("child pids", pids);
        assertEquals("child pids", 1, pids.length);
        assertEquals(child2Pid, pids[0]);
    }

    @SuppressWarnings("unchecked")
    public void testNestedNonUniqueReferences() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;
        String[] pids;

        String parentName = "test.config.nested.singleton.1";
        SingletonTest parent = new SingletonTest(parentName);
        addTest(parent);

        String childOneName = "test.config.nested.child.ref.3";
        FactoryNestedTest childOne = new FactoryNestedTest(childOneName);
        addTest(childOne);

        String childTwoName = "test.config.nested.child.ref.4";
        FactoryNestedTest childTwo = new FactoryNestedTest(childTwoName);
        addTest(childTwo);

        dictionary = parent.waitForUpdate();

        // *** STEP 1 ***
        // add initial configurations - the references use the same id
        writer = readConfiguration();
        writer.addConfig("<{0} id=\"1\" name=\"myChildOne\" a=\"b\" value=\"10\"/>", childOneName);
        writer.addConfig("<{0} id=\"1\" name=\"myChildTwo\" a=\"b\" strValue=\"bob\"/>", childTwoName);
        writer.deleteConfig(parentName, null, true);
        writer.addConfig("<{0} name=\"myNestedSingleton\">" +
                         "  <{1} ref=\"1\" />" +
                         "  <{2} ref=\"1\" />" +
                         "</{0}>", parentName, childOneName, childTwoName);
        writeConfiguration(writer);

        // parent gets injected with the children pids
        dictionary = parent.waitForUpdate();
        assertEquals("name property", "myNestedSingleton", dictionary.get("name"));

        pids = (String[]) dictionary.get(childOneName);
        assertNotNull("child pids", pids);
        assertEquals("child pids", 1, pids.length);
        String childOnePid = pids[0];

        pids = (String[]) dictionary.get(childTwoName);
        assertNotNull("child pids", pids);
        assertEquals("child pids", 1, pids.length);
        String childTwoPid = pids[0];

        assertFalse("pids are the same: " + childOnePid, childOnePid.equals(childTwoPid));
    }

    @SuppressWarnings("unchecked")
    public void testFactoryOptionalId() throws Exception {
        ConfigWriter writer;

        FactoryTest parent = new FactoryTest("test.config.nested.factory.optional.metatype", "name");
        addTest(parent);

        FactoryNestedTest child = new FactoryNestedTest("test.config.nested.factory.optional.child.1");
        addTest(child);

        FactoryNestedTest childChild = new FactoryNestedTest("test.config.nested.factory.optional.child.2");
        addTest(childChild);

        String parentName = "nestedOptionalFactory";
        String firstChildName = "nestedOptionalChildOne";
        String secondChildName = "nestedOptionalChildTwo";

        // write initial configuration
        writer = readConfiguration();
        writer.addConfig("<{0} name=\"myOptionalFactory\">" +
                         "  <{1} name=\"myNestedChild\" a=\"b\" value=\"10\">" +
                         "      <{2} name=\"myDoubleNestedChild\" threads=\"15\"/>" +
                         "  </{1}>" +
                         "</{0}>", parentName, firstChildName, secondChildName);
        writeConfiguration(writer);

        // parent get injected with the child
        Dictionary<String, Object> dictionaryOne = parent.waitForUpdate("myOptionalFactory");
        assertEquals(Short.valueOf("5"), dictionaryOne.get("poolSize"));
        String[] onePids = (String[]) dictionaryOne.get(firstChildName);
        assertNotNull("child pids", onePids);
        assertEquals("child pids", 1, onePids.length);

        // first child is updated       
        Dictionary<String, Object> dictionaryTwo = child.waitForUpdate(onePids[0]);
        assertEquals("name property", "myNestedChild", dictionaryTwo.get("name"));
        assertEquals("value property", "10", dictionaryTwo.get("value"));
        assertEquals("a property", "b", dictionaryTwo.get("a"));
        assertEquals(Long.valueOf("5000"), dictionaryTwo.get("idleTime"));
        String[] twoPids = (String[]) dictionaryTwo.get(secondChildName);
        assertNotNull("child pids", twoPids);
        assertEquals("child pids", 1, twoPids.length);

        // second child is updated       
        Dictionary<String, Object> dictionaryThree = childChild.waitForUpdate(twoPids[0]);
        assertEquals("name property", "myDoubleNestedChild", dictionaryThree.get("name"));
        assertEquals("value property", "15", dictionaryThree.get("threads"));
        assertEquals(Integer.valueOf("1000"), dictionaryThree.get("timeout"));

        // update double nested configuration
        writer = readConfiguration();
        writer.deleteConfig(parentName, "myOptionalFactory", "name", false);
        writer.addConfig("<{0} name=\"myOptionalFactory\">" +
                         "  <{1} name=\"myNestedChild\" a=\"b\" value=\"10\">" +
                         "      <{2} name=\"myDoubleNestedChild\" threads=\"20\"/>" +
                         "  </{1}>" +
                         "</{0}>", parentName, firstChildName, secondChildName);
        writeConfiguration(writer);

        Dictionary<String, Object> dictionary = null;

        // top parent should be notified of the changes but have no differences
        dictionary = parent.waitForUpdate("myOptionalFactory");
        dictionaryEquals(dictionaryOne, dictionary);

        // first child should be notified of the changes but have no differences
        dictionary = child.waitForUpdate(onePids[0]);
        dictionaryEquals(dictionaryTwo, dictionary);

        // second child should be notified of the changes
        dictionary = childChild.waitForUpdate(twoPids[0]);
        assertEquals("name property", "myDoubleNestedChild", dictionary.get("name"));
        assertEquals("value property", "20", dictionary.get("threads"));
        assertEquals(Integer.valueOf("1000"), dictionary.get("timeout"));
    }

    public static class TimestampFactoryTest extends FactoryTest {

        private long sleep = -1;

        public TimestampFactoryTest(String name) {
            super(name);
        }

        public void setSleep(long sleep) {
            this.sleep = sleep;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public void updated(String pid, Dictionary properties) throws ConfigurationException {
            properties.put("timestamp", System.currentTimeMillis());
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            super.updated(pid, properties);
        }
    }

    @SuppressWarnings("unchecked")
    public void testUpdateOrder() throws Exception {
        Dictionary<String, Object> dictionary;
        ConfigWriter writer;

        TimestampFactoryTest a = new TimestampFactoryTest("test.config.order.a");
        TimestampFactoryTest b = new TimestampFactoryTest("test.config.order.b");
        TimestampFactoryTest c = new TimestampFactoryTest("test.config.order.c");
        TimestampFactoryTest d = new TimestampFactoryTest("test.config.order.d");
        TimestampFactoryTest e = new TimestampFactoryTest("test.config.order.e");

        addTest(a);
        addTest(b);
        addTest(c);
        addTest(d);
        addTest(e);

        // *** STEP 1 ***
        // wait for the initial configuration to be injected.
        dictionary = a.waitForUpdate("a");
        assertEquals("a", dictionary.get("name"));
        dictionary = b.waitForUpdate("b");
        assertEquals("b", dictionary.get("name"));
        dictionary = c.waitForUpdate("c");
        assertEquals("c", dictionary.get("name"));
        dictionary = d.waitForUpdate("d");
        assertEquals("d", dictionary.get("name"));
        dictionary = e.waitForUpdate("e");
        assertEquals("e", dictionary.get("name"));

        // *** STEP 2 ***
        // update e and check for updates in right order
        long timeout = 1000;
        a.setSleep(timeout);
        b.setSleep(timeout);
        c.setSleep(timeout);
        d.setSleep(timeout);
        e.setSleep(timeout);

        writer = readConfiguration();
        writer.setValue("test.config.order.e", "e", "name", "ee");
        writeConfiguration(writer);

        long aTimeout, bTimeout, cTimeout, dTimeout, eTimeout;

        dictionary = a.waitForUpdate("a");
        assertEquals("a", dictionary.get("name"));
        aTimeout = (Long) dictionary.get("timestamp");
        dictionary = b.waitForUpdate("b");
        assertEquals("b", dictionary.get("name"));
        bTimeout = (Long) dictionary.get("timestamp");
        dictionary = c.waitForUpdate("c");
        assertEquals("c", dictionary.get("name"));
        cTimeout = (Long) dictionary.get("timestamp");
        dictionary = d.waitForUpdate("d");
        assertEquals("d", dictionary.get("name"));
        dTimeout = (Long) dictionary.get("timestamp");
        dictionary = e.waitForUpdate("e");
        assertEquals("ee", dictionary.get("name"));
        eTimeout = (Long) dictionary.get("timestamp");

        // expected update order, e, d, c, b, a
        assertTrue(dTimeout > eTimeout);
        assertTrue(cTimeout > dTimeout);
        assertTrue(bTimeout > cTimeout);
        assertTrue(aTimeout > bTimeout);
    }

    private static Set<String> toSet(String[] array) {
        return new HashSet<String>(Arrays.asList(array));
    }

}
