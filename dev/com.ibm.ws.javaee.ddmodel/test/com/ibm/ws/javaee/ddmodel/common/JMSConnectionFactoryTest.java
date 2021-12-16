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
import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.DDParser;

public class JMSConnectionFactoryTest extends JNDIEnvironmentRefsTestBase {
    @Test
    public void testDefault() throws Exception {
        String xml = "<jms-connection-factory>" +
                     "  <name>name0</name>" +
                     "</jms-connection-factory>";
        JMSConnectionFactory o = parse(xml).getJMSConnectionFactories().get(0);
        Assert.assertEquals(Arrays.asList(), o.getDescriptions());
        Assert.assertEquals("name0", o.getName());
        Assert.assertNull(o.getInterfaceNameValue());
        Assert.assertNull(o.getClassNameValue());
        Assert.assertNull(o.getResourceAdapter());
        Assert.assertNull(o.getUser());
        Assert.assertNull(o.getPassword());
        Assert.assertNull(o.getClientId());
        Assert.assertEquals(Arrays.asList(), o.getProperties());
        Assert.assertFalse(o.isSetTransactional());
        Assert.assertFalse(o.isSetMaxPoolSize());
        Assert.assertFalse(o.isSetMinPoolSize());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testDefaultEJB31() throws Exception {
        parseEJB31("<jms-connection-factory>" +
                   "  <name>name0</name>" +
                   "</jms-connection-factory>");
    }

    @Test
    public void testAll() throws Exception {
        String xml = "<jms-connection-factory>" +
                     "  <description>desc0</description>" +
                     "  <description>desc1</description>" +
                     "  <name>name0</name>" +
                     "  <interface-name>intf0</interface-name>" +
                     "  <class-name>class0</class-name>" +
                     "  <resource-adapter>ra0</resource-adapter>" +
                     "  <user>user0</user>" +
                     "  <password>pass0</password>" +
                     "  <client-id>cid0</client-id>" +
                     "  <property>" +
                     "    <name>prop0</name>" +
                     "    <value>value0</value>" +
                     "  </property>" +
                     "  <property>" +
                     "    <name>prop1</name>" +
                     "    <value>value1</value>" +
                     "  </property>" +
                     "  <transactional>true</transactional>" +
                     "  <max-pool-size>100</max-pool-size>" +
                     "  <min-pool-size>10</min-pool-size>" +
                     "</jms-connection-factory>";
        JMSConnectionFactory o = parse(xml).getJMSConnectionFactories().get(0);
        List<Description> descs = o.getDescriptions();
        Assert.assertEquals(descs.toString(), 2, descs.size());
        Assert.assertEquals("desc0", descs.get(0).getValue());
        Assert.assertEquals("desc1", descs.get(1).getValue());
        Assert.assertEquals("name0", o.getName());
        Assert.assertEquals("intf0", o.getInterfaceNameValue());
        Assert.assertEquals("class0", o.getClassNameValue());
        Assert.assertEquals("ra0", o.getResourceAdapter());
        Assert.assertEquals("user0", o.getUser());
        Assert.assertEquals("pass0", o.getPassword());
        Assert.assertEquals("cid0", o.getClientId());
        List<Property> props = o.getProperties();
        Assert.assertEquals(props.toString(), 2, props.size());
        Assert.assertEquals("prop0", props.get(0).getName());
        Assert.assertEquals("value0", props.get(0).getValue());
        Assert.assertEquals("prop1", props.get(1).getName());
        Assert.assertEquals("value1", props.get(1).getValue());
        Assert.assertTrue(o.isSetTransactional());
        Assert.assertTrue(o.isTransactional());
        Assert.assertTrue(o.isSetMaxPoolSize());
        Assert.assertEquals(100, o.getMaxPoolSize());
        Assert.assertTrue(o.isSetMinPoolSize());
        Assert.assertEquals(10, o.getMinPoolSize());
    }
}
