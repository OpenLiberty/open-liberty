/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

public class JNDIEnvironmentRefTypeTest {
    @Test
    public void testGetType() {
        Set<Class<?>> types = new HashSet<Class<?>>();
        for (JNDIEnvironmentRefType refType : JNDIEnvironmentRefType.VALUES) {
            Class<? extends JNDIEnvironmentRef> type = refType.getType();
            Assert.assertNotNull(refType.name(), type);
            Assert.assertTrue(refType.name(), types.add(type));
        }
    }

    @Test
    public void testGetXMLNames() {
        Set<String> names = new HashSet<String>();
        for (JNDIEnvironmentRefType refType : JNDIEnvironmentRefType.VALUES) {
            String name = refType.getXMLElementName();
            Assert.assertNotNull(refType.name(), name);
            Assert.assertTrue(refType.name(), names.add(name));
            Assert.assertNotNull(refType.getNameXMLElementName());
        }
    }

    @Test
    public void testGetAnnotationNames() {
        Set<String> names = new HashSet<String>();
        Set<JNDIEnvironmentRefType> resourceTypes = new HashSet<JNDIEnvironmentRefType>(Arrays.asList(JNDIEnvironmentRefType.EnvEntry,
                                                                                                      JNDIEnvironmentRefType.ResourceRef,
                                                                                                      JNDIEnvironmentRefType.ResourceEnvRef,
                                                                                                      JNDIEnvironmentRefType.MessageDestinationRef));
        for (JNDIEnvironmentRefType refType : JNDIEnvironmentRefType.VALUES) {
            String name = refType.getAnnotationShortName();
            Assert.assertNotNull(refType.name(), name);
            Assert.assertTrue(refType.name(), names.add(name) || resourceTypes.contains(refType));
            Assert.assertNotNull(refType.getNameAnnotationElementName());
        }
    }

    @Test
    public void testGetBindingAttributeName() {
        for (JNDIEnvironmentRefType refType : JNDIEnvironmentRefType.VALUES) {
            if (refType.getBindingElementName() == null) {
                Assert.assertNull(refType.name(), refType.getBindingAttributeName());
            } else {
                Assert.assertNotNull(refType.name(), refType.getBindingAttributeName());
            }
        }
    }

    @Test
    public void testAddRef() {
        TestHelper.EnvEntryImpl envEntry = new TestHelper.EnvEntryImpl(null, null, null, null);

        Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs = new EnumMap<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>>(JNDIEnvironmentRefType.class);
        JNDIEnvironmentRefType.EnvEntry.addRef(allRefs, envEntry);

        Assert.assertEquals(Arrays.asList(envEntry), allRefs.get(JNDIEnvironmentRefType.EnvEntry));
        for (JNDIEnvironmentRefType refType : JNDIEnvironmentRefType.VALUES) {
            if (refType != JNDIEnvironmentRefType.EnvEntry) {
                Assert.assertNull(refType.name(), allRefs.get(refType));
            }
        }
    }

    @Test
    public void testAddAllRefs() {
        TestHelper.EnvEntryImpl envEntry = new TestHelper.EnvEntryImpl(null, null, null, null);
        ComponentNameSpaceConfiguration compNSConfig = new TestHelper();
        compNSConfig.setEnvEntries(Arrays.asList(envEntry));

        Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs = new EnumMap<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>>(JNDIEnvironmentRefType.class);
        JNDIEnvironmentRefType.addAllRefs(allRefs, compNSConfig);
        JNDIEnvironmentRefType.addAllRefs(allRefs, compNSConfig);

        Assert.assertEquals(Arrays.asList(envEntry, envEntry), allRefs.get(JNDIEnvironmentRefType.EnvEntry));
        for (JNDIEnvironmentRefType refType : JNDIEnvironmentRefType.VALUES) {
            if (refType != JNDIEnvironmentRefType.EnvEntry) {
                Assert.assertNull(refType.name(), allRefs.get(refType));
            }
        }
    }

    @Test
    public void testSetAllRefs() {
        TestHelper.EnvEntryImpl envEntry = new TestHelper.EnvEntryImpl(null, null, null, null);

        Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs = new EnumMap<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>>(JNDIEnvironmentRefType.class);
        JNDIEnvironmentRefType.EnvEntry.addRef(allRefs, envEntry);

        ComponentNameSpaceConfiguration compNSConfig = new TestHelper();
        JNDIEnvironmentRefType.setAllRefs(compNSConfig, allRefs);

        Assert.assertEquals(Arrays.asList(envEntry), compNSConfig.getEnvEntries());
        for (JNDIEnvironmentRefType refType : JNDIEnvironmentRefType.VALUES) {
            if (refType != JNDIEnvironmentRefType.EnvEntry) {
                Assert.assertNull(refType.name(), compNSConfig.getJNDIEnvironmentRefs(refType.getType()));
            }
        }
    }
}
