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
import com.ibm.ws.javaee.dd.common.MailSession;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.DDParser;

public class MailSessionTest extends JNDIEnvironmentRefsTestBase {
    @Test
    public void testDefault() throws Exception {
        String xml = "<mail-session>" +
                     "  <name>name0</name>" +
                     "</mail-session>";
        MailSession o = parse(xml).getMailSessions().get(0);
        Assert.assertEquals(Arrays.asList(), o.getDescriptions());
        Assert.assertEquals("name0", o.getName());
        Assert.assertNull(o.getStoreProtocol());
        Assert.assertNull(o.getStoreProtocolClassName());
        Assert.assertNull(o.getTransportProtocol());
        Assert.assertNull(o.getTransportProtocolClassName());
        Assert.assertNull(o.getHost());
        Assert.assertNull(o.getUser());
        Assert.assertNull(o.getPassword());
        Assert.assertNull(o.getFrom());
        Assert.assertEquals(Arrays.asList(), o.getProperties());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testDefaultEJB31() throws Exception {
        parseEJB31("<mail-session>" +
                   "  <name>name0</name>" +
                   "</mail-session>");
    }

    @Test
    public void testAll() throws Exception {
        String xml = "<mail-session>" +
                     "  <description>desc0</description>" +
                     "  <description>desc1</description>" +
                     "  <name>name0</name>" +
                     "  <store-protocol>sp0</store-protocol>" +
                     "  <store-protocol-class>spc0</store-protocol-class>" +
                     "  <transport-protocol>tp0</transport-protocol>" +
                     "  <transport-protocol-class>tpc0</transport-protocol-class>" +
                     "  <host>host0</host>" +
                     "  <user>user0</user>" +
                     "  <password>pass0</password>" +
                     "  <from>from0</from>" +
                     "  <property>" +
                     "    <name>prop0</name>" +
                     "    <value>value0</value>" +
                     "  </property>" +
                     "  <property>" +
                     "    <name>prop1</name>" +
                     "    <value>value1</value>" +
                     "  </property>" +
                     "</mail-session>";
        MailSession o = parse(xml).getMailSessions().get(0);
        List<Description> descs = o.getDescriptions();
        Assert.assertEquals(descs.toString(), 2, descs.size());
        Assert.assertEquals("desc0", descs.get(0).getValue());
        Assert.assertEquals("desc1", descs.get(1).getValue());
        Assert.assertEquals("name0", o.getName());
        Assert.assertEquals("sp0", o.getStoreProtocol());
        Assert.assertEquals("spc0", o.getStoreProtocolClassName());
        Assert.assertEquals("tp0", o.getTransportProtocol());
        Assert.assertEquals("tpc0", o.getTransportProtocolClassName());
        Assert.assertEquals("host0", o.getHost());
        Assert.assertEquals("user0", o.getUser());
        Assert.assertEquals("pass0", o.getPassword());
        Assert.assertEquals("from0", o.getFrom());
        List<Property> props = o.getProperties();
        Assert.assertEquals(props.toString(), 2, props.size());
        Assert.assertEquals("prop0", props.get(0).getName());
        Assert.assertEquals("value0", props.get(0).getValue());
        Assert.assertEquals("prop1", props.get(1).getName());
        Assert.assertEquals("value1", props.get(1).getValue());
    }
}
