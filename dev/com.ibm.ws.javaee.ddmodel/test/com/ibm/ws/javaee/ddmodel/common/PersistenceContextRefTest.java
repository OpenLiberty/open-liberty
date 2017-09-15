/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.DDParser;

public class PersistenceContextRefTest extends JNDIEnvironmentRefsTestBase {
    @Test
    public void testDefault() throws Exception {
        String xml = "<persistence-context-ref>" +
                     "  <persistence-context-ref-name>name0</persistence-context-ref-name>" +
                     "</persistence-context-ref>";
        PersistenceContextRef o = parse(xml).getPersistenceContextRefs().get(0);
        Assert.assertEquals(Arrays.asList(), o.getDescriptions());
        Assert.assertEquals("name0", o.getName());
        Assert.assertNull(o.getPersistenceUnitName());
        Assert.assertEquals(PersistenceContextRef.TYPE_UNSPECIFIED, o.getTypeValue());
        Assert.assertEquals(PersistenceContextRef.SYNCHRONIZATION_UNSPECIFIED, o.getSynchronizationValue());
        Assert.assertEquals(Arrays.asList(), o.getProperties());
        Assert.assertNull(o.getMappedName());
        Assert.assertEquals(Arrays.asList(), o.getInjectionTargets());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testSynchronizationEJB31() throws Exception {
        parseEJB31("<persistence-context-ref>" +
                   "  <persistence-context-ref-name>name0</persistence-context-ref-name>" +
                   "  <persistence-context-synchronization>Synchronized</persistence-context-synchronization>" +
                   "</persistence-context-ref>");
    }

    @Test
    public void testAll() throws Exception {
        String xml = "<persistence-context-ref>" +
                     "  <description>desc0</description>" +
                     "  <description>desc1</description>" +
                     "  <persistence-context-ref-name>name0</persistence-context-ref-name>" +
                     "  <persistence-context-type>Transaction</persistence-context-type>" +
                     "  <persistence-context-synchronization>Synchronized</persistence-context-synchronization>" +
                     "  <persistence-property>" +
                     "    <name>prop0</name>" +
                     "    <value>value0</value>" +
                     "  </persistence-property>" +
                     "  <persistence-property>" +
                     "    <name>prop1</name>" +
                     "    <value>value1</value>" +
                     "  </persistence-property>" +
                     "  <mapped-name>mn0</mapped-name>" +
                     "  <injection-target>" +
                     "    <injection-target-class>itc0</injection-target-class>" +
                     "    <injection-target-name>itn0</injection-target-name>" +
                     "  </injection-target>" +
                     "  <injection-target>" +
                     "    <injection-target-class>itc1</injection-target-class>" +
                     "    <injection-target-name>itn1</injection-target-name>" +
                     "  </injection-target>" +
                     "</persistence-context-ref>" +
                     "<persistence-context-ref>" +
                     "  <persistence-context-ref-name>name0</persistence-context-ref-name>" +
                     "  <persistence-context-type>Extended</persistence-context-type>" +
                     "  <persistence-context-synchronization>Unsynchronized</persistence-context-synchronization>" +
                     "</persistence-context-ref>";
        List<PersistenceContextRef> os = parse(xml).getPersistenceContextRefs();
        PersistenceContextRef o = os.get(0);
        List<Description> descs = o.getDescriptions();
        Assert.assertEquals(descs.toString(), 2, descs.size());
        Assert.assertEquals("desc0", descs.get(0).getValue());
        Assert.assertEquals("desc1", descs.get(1).getValue());
        Assert.assertEquals("name0", o.getName());
        Assert.assertNull(o.getPersistenceUnitName());
        Assert.assertEquals(PersistenceContextRef.TYPE_TRANSACTION, o.getTypeValue());
        Assert.assertEquals(PersistenceContextRef.TYPE_EXTENDED, os.get(1).getTypeValue());
        Assert.assertEquals(PersistenceContextRef.SYNCHRONIZATION_SYNCHRONIZED, o.getSynchronizationValue());
        Assert.assertEquals(PersistenceContextRef.SYNCHRONIZATION_UNSYNCHRONIZED, os.get(1).getSynchronizationValue());
        List<Property> props = o.getProperties();
        Assert.assertEquals(props.toString(), 2, props.size());
        Assert.assertEquals("prop0", props.get(0).getName());
        Assert.assertEquals("value0", props.get(0).getValue());
        Assert.assertEquals("prop1", props.get(1).getName());
        Assert.assertEquals("value1", props.get(1).getValue());
        Assert.assertEquals("mn0", o.getMappedName());
        List<InjectionTarget> its = o.getInjectionTargets();
        Assert.assertEquals(its.toString(), 2, its.size());
        Assert.assertEquals("itc0", its.get(0).getInjectionTargetClassName());
        Assert.assertEquals("itn0", its.get(0).getInjectionTargetName());
        Assert.assertEquals("itc1", its.get(1).getInjectionTargetClassName());
        Assert.assertEquals("itn1", its.get(1).getInjectionTargetName());
    }
}
