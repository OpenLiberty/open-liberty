/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.equinox.metatype.EquinoxObjectClassDefinition;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

public class MetaTypeRegistryTest {

    static WsLocationAdmin libertyLocation;
    static XMLConfigParser configParser;
    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();

        // Restore back to old kernel and let next test case set to new kernel
        // as needed
        SharedLocationManager.resetWsLocationAdmin();
    }

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, this keeps
        // things sane
        outputMgr.resetStreams();
    }

    @Test
    public void testEmptyMetaType() throws Exception {
        MockMetaTypeInformation metatype = new MockMetaTypeInformation();

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertTrue("No metatypes should be added", registry.addMetaType(metatype).isEmpty());
    }

    @Test
    public void testMetaType() throws Exception {
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition("myoid");
        objectClass.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "IL" }));

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add("singleton", false, objectClass);
        metatype.add("factory", true, objectClass);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        // test lookup by bundle
        assertEquals(metatype, registry.getMetaTypeInformation(bundle));

        assertContains(registry, "singleton", objectClass);
        assertContains(registry, "factory", objectClass);

        registry.removeMetaType(bundle);

        // test lookup by bundle
        assertNull(registry.getMetaTypeInformation(bundle));

        assertEmpty(registry, "singleton");
        assertEmpty(registry, "factory");
    }

    @Test
    public void testMetaTypeWithAlias() throws Exception {
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition("myoid");
        objectClass.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "IL" }));
        objectClass.setAlias("myalias");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add("singleton", false, objectClass);
        metatype.add("factory", true, objectClass);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        // test lookup by bundle
        assertEquals(metatype, registry.getMetaTypeInformation(bundle));

        assertContains(registry, "singleton", objectClass);
        assertContains(registry, "factory", objectClass);
        assertContains(registry, "myalias", objectClass);

        registry.removeMetaType(bundle);

        // test lookup by bundle
        assertNull(registry.getMetaTypeInformation(bundle));

        assertEmpty(registry, "singleton");
        assertEmpty(registry, "factory");
        assertEmpty(registry, "myalias");
    }

    @Test
    public void testMetaTypeWithSharedAlias() throws Exception {
        MockObjectClassDefinition objectClass1 = new MockObjectClassDefinition("myoid1");
        objectClass1.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "IL" }));
        objectClass1.setAlias("myalias");

        MockObjectClassDefinition objectClass2 = new MockObjectClassDefinition("myoid2");
        objectClass2.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "NC" }));
        objectClass2.setAlias("myalias");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add("singleton1", false, objectClass1);
        metatype.add("singleton2", false, objectClass2);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype).isEmpty());

        // test lookup by bundle
        assertEquals(metatype, registry.getMetaTypeInformation(bundle));

        assertContains(registry, "singleton1", objectClass1);
        assertContains(registry, "singleton2", objectClass2);
        assertContains(registry, "myalias", new EquinoxObjectClassDefinition[] { objectClass1, objectClass2 });

        registry.removeMetaType(bundle);

        // test lookup by bundle
        assertNull(registry.getMetaTypeInformation(bundle));

        assertEmpty(registry, "singleton1");
        assertEmpty(registry, "singleton2");
        assertEmpty(registry, "myalias");
    }

    @Test
    public void testSeparateMetaTypeWithSharedAlias() throws Exception {
        MockObjectClassDefinition objectClass1 = new MockObjectClassDefinition("myoid1");
        objectClass1.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "IL" }));
        objectClass1.setAlias("myalias");

        MockBundle bundle1 = new MockBundle();
        MockMetaTypeInformation metatype1 = new MockMetaTypeInformation(bundle1);
        metatype1.add("singleton1", false, objectClass1);

        MockObjectClassDefinition objectClass2 = new MockObjectClassDefinition("myoid2");
        objectClass2.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "NC" }));
        objectClass2.setAlias("myalias");

        MockBundle bundle2 = new MockBundle();
        MockMetaTypeInformation metatype2 = new MockMetaTypeInformation(bundle2);
        metatype2.add("singleton2", false, objectClass2);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype1).isEmpty());
        assertFalse("The registry should be updated", registry.addMetaType(metatype2).isEmpty());

        // test lookup by bundle
        assertEquals(metatype1, registry.getMetaTypeInformation(bundle1));
        assertEquals(metatype2, registry.getMetaTypeInformation(bundle2));

        // add metatype2 again - should not make any difference
        assertTrue(registry.addMetaType(metatype2).isEmpty());

        assertContains(registry, "singleton1", objectClass1);
        assertContains(registry, "singleton2", objectClass2);
        assertContains(registry, "myalias", new EquinoxObjectClassDefinition[] { objectClass1, objectClass2 });

        registry.removeMetaType(bundle2);

        // test lookup by bundle
        assertEquals(metatype1, registry.getMetaTypeInformation(bundle1));
        assertNull(registry.getMetaTypeInformation(bundle2));

        assertContains(registry, "singleton1", objectClass1);
        assertEmpty(registry, "singleton2");
        assertContains(registry, "myalias", objectClass1);

        registry.removeMetaType(bundle1);

        // test lookup by bundle
        assertNull(registry.getMetaTypeInformation(bundle1));
        assertNull(registry.getMetaTypeInformation(bundle2));

        assertEmpty(registry, "singleton1");
        assertEmpty(registry, "singleton2");
        assertEmpty(registry, "myalias");
    }

    @Test
    public void testMetaTypeByBundle() throws Exception {
        MockObjectClassDefinition objectClass1 = new MockObjectClassDefinition("myoid1");
        objectClass1.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "IL" }));
        objectClass1.setAlias("myalias");

        MockBundle bundle1 = new MockBundle();
        MockMetaTypeInformation metatype1 = new MockMetaTypeInformation(bundle1);
        metatype1.add("singleton1", false, objectClass1);

        MockObjectClassDefinition objectClass2 = new MockObjectClassDefinition("myoid2");
        objectClass2.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "NC" }));
        objectClass2.setAlias("myalias");

        MockBundle bundle2 = new MockBundle();
        MockMetaTypeInformation metatype2 = new MockMetaTypeInformation(bundle2);
        metatype2.add("singleton2", false, objectClass2);

        MetaTypeRegistry registry = new MetaTypeRegistry();
        assertFalse("The registry should be updated", registry.addMetaType(metatype1).isEmpty());
        assertFalse("The registry should be updated", registry.addMetaType(metatype2).isEmpty());

        // test lookup by bundle
        assertEquals(metatype1, registry.getMetaTypeInformation(bundle1));
        assertEquals(metatype2, registry.getMetaTypeInformation(bundle2));

        // add metatype2 again - should not make any difference
        assertTrue(registry.addMetaType(metatype2).isEmpty());

        assertContains(registry, "singleton1", objectClass1);
        assertContains(registry, "singleton2", objectClass2);
        assertContains(registry, "myalias", new EquinoxObjectClassDefinition[] { objectClass1, objectClass2 });

        assertEquals(metatype2, registry.removeMetaType(bundle2));

        // test lookup by bundle
        assertEquals(metatype1, registry.getMetaTypeInformation(bundle1));
        assertNull(registry.getMetaTypeInformation(bundle2));

        assertContains(registry, "singleton1", objectClass1);
        assertEmpty(registry, "singleton2");
        assertContains(registry, "myalias", objectClass1);

        assertEquals(metatype1, registry.removeMetaType(bundle1));

        // test lookup by bundle
        assertNull(registry.getMetaTypeInformation(bundle1));
        assertNull(registry.getMetaTypeInformation(bundle2));

        assertEmpty(registry, "singleton1");
        assertEmpty(registry, "singleton2");
        assertEmpty(registry, "myalias");
    }

    private void assertEmpty(MetaTypeRegistry registry, String pid) {
        RegistryEntry entry = registry.getRegistryEntryByPidOrAlias(pid);
        assertNull(entry);
    }

    private void assertContains(MetaTypeRegistry registry, String pid, EquinoxObjectClassDefinition ocd) {
        assertContains(registry, pid, new EquinoxObjectClassDefinition[] { ocd });
    }

    private void assertContains(MetaTypeRegistry registry, String pid, EquinoxObjectClassDefinition[] ocds) {
        RegistryEntry entry = registry.getRegistryEntryByPidOrAlias(pid);
        assertNotNull(entry);
        assertEquals(ocds[0], entry.getObjectClassDefinition().getDelegate());
    }
}
