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

import com.ibm.ws.javaee.dd.common.ConnectionFactory;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.DDParser;

public class ConnectionFactoryTest extends JNDIEnvironmentRefsTestBase {
    @Test
    public void testDefault() throws Exception {
        String xml = "<connection-factory>" +
                     "  <name>name0</name>" +
                     "  <interface-name>intf0</interface-name>" +
                     "  <resource-adapter>ra0</resource-adapter>" +
                     "</connection-factory>";
        ConnectionFactory o = parse(xml).getConnectionFactories().get(0);
        Assert.assertEquals(Arrays.asList(), o.getDescriptions());
        Assert.assertEquals("name0", o.getName());
        Assert.assertEquals("intf0", o.getInterfaceNameValue());
        Assert.assertEquals("ra0", o.getResourceAdapter());
        Assert.assertFalse(o.isSetMaxPoolSize());
        Assert.assertFalse(o.isSetMinPoolSize());
        Assert.assertEquals(ConnectionFactory.TRANSACTION_SUPPORT_UNSPECIFIED, o.getTransactionSupportValue());
        Assert.assertEquals(Arrays.asList(), o.getProperties());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testDefaultEJB31() throws Exception {
        parseEJB31("<connection-factory>" +
                   "  <name>name0</name>" +
                   "  <interface-name>intf0</interface-name>" +
                   "  <resource-adapter>ra0</resource-adapter>" +
                   "</connection-factory>");
    }

    @Test
    public void testAll() throws Exception {
        String xml = "<connection-factory>" +
                     "  <description>desc0</description>" +
                     "  <description>desc1</description>" +
                     "  <name>name0</name>" +
                     "  <interface-name>intf0</interface-name>" +
                     "  <resource-adapter>ra0</resource-adapter>" +
                     "  <max-pool-size>100</max-pool-size>" +
                     "  <min-pool-size>10</min-pool-size>" +
                     "  <transaction-support>NoTransaction</transaction-support>" +
                     "  <property>" +
                     "    <name>prop0</name>" +
                     "    <value>value0</value>" +
                     "  </property>" +
                     "  <property>" +
                     "    <name>prop1</name>" +
                     "    <value>value1</value>" +
                     "  </property>" +
                     "</connection-factory>" +
                     "<connection-factory>" +
                     "  <name>name0</name>" +
                     "  <interface-name>intf0</interface-name>" +
                     "  <resource-adapter>ra0</resource-adapter>" +
                     "  <transaction-support>LocalTransaction</transaction-support>" +
                     "</connection-factory>" +
                     "<connection-factory>" +
                     "  <name>name0</name>" +
                     "  <interface-name>intf0</interface-name>" +
                     "  <resource-adapter>ra0</resource-adapter>" +
                     "  <transaction-support>XATransaction</transaction-support>" +
                     "</connection-factory>";
        List<ConnectionFactory> os = parse(xml).getConnectionFactories();
        ConnectionFactory o = os.get(0);
        List<Description> descs = o.getDescriptions();
        Assert.assertEquals(descs.toString(), 2, descs.size());
        Assert.assertEquals("desc0", descs.get(0).getValue());
        Assert.assertEquals("desc1", descs.get(1).getValue());
        Assert.assertEquals("name0", o.getName());
        Assert.assertEquals("intf0", o.getInterfaceNameValue());
        Assert.assertEquals("ra0", o.getResourceAdapter());
        Assert.assertTrue(o.isSetMaxPoolSize());
        Assert.assertEquals(100, o.getMaxPoolSize());
        Assert.assertTrue(o.isSetMinPoolSize());
        Assert.assertEquals(10, o.getMinPoolSize());
        Assert.assertEquals(ConnectionFactory.TRANSACTION_SUPPORT_NO_TRANSACTION, o.getTransactionSupportValue());
        Assert.assertEquals(ConnectionFactory.TRANSACTION_SUPPORT_LOCAL_TRANSACTION, os.get(1).getTransactionSupportValue());
        Assert.assertEquals(ConnectionFactory.TRANSACTION_SUPPORT_XA_TRANSACTION, os.get(2).getTransactionSupportValue());
        List<Property> props = o.getProperties();
        Assert.assertEquals(props.toString(), 2, props.size());
        Assert.assertEquals("prop0", props.get(0).getName());
        Assert.assertEquals("value0", props.get(0).getValue());
        Assert.assertEquals("prop1", props.get(1).getName());
        Assert.assertEquals("value1", props.get(1).getValue());
    }
}
