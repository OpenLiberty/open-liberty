/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.naming;

import java.util.Collection;

import javax.ejb.EJBContext;
import javax.naming.NameClassPair;

import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.ejbcontainer.mock.TestContextJavaColonNamingHelper;

public class ContextJavaColonNamingTest {

    private final Mockery mockery = new Mockery();

    @Test
    public void testGetObjectInstance() throws Exception {
        EJBContext context = mockery.mock(EJBContext.class);
        TestContextJavaColonNamingHelper service = new TestContextJavaColonNamingHelper(context);

        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.COMP, "EJBContext"));

        for (boolean ejbContextActive : new boolean[] { false, true }) {
            service.setEjbContextActive(ejbContextActive);
            Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.COMP_ENV, "EJBContext"));
            Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.COMP_WS, "EJBContext"));
            Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.GLOBAL, "EJBContext"));
            Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.APP, "EJBContext"));
            Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.MODULE, "EJBContext"));
        }

        Assert.assertSame(context, service.getObjectInstance(NamingConstants.JavaColonNamespace.COMP, "EJBContext"));
    }

    @Test
    public void testHasObjectWithPrefix() throws Exception {
        EJBContext context = mockery.mock(EJBContext.class);
        TestContextJavaColonNamingHelper service = new TestContextJavaColonNamingHelper(context);

        Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.COMP, ""));

        for (boolean ejbContextActive : new boolean[] { false, true }) {
            service.setEjbContextActive(ejbContextActive);

            Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.MODULE, ""));
            Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.MODULE, "env"));
            Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.APP, ""));
            Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.APP, "env"));
            Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.COMP, "env"));
        }

        Assert.assertTrue(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.COMP, ""));
    }

    @Test
    public void testListInstances() throws Exception {
        EJBContext context = mockery.mock(EJBContext.class);
        TestContextJavaColonNamingHelper service = new TestContextJavaColonNamingHelper(context);

        Assert.assertEquals(0, service.listInstances(NamingConstants.JavaColonNamespace.COMP, "").size());

        for (boolean ejbContextActive : new boolean[] { false, true }) {
            service.setEjbContextActive(ejbContextActive);

            Assert.assertEquals(0, service.listInstances(NamingConstants.JavaColonNamespace.MODULE, "").size());
            Assert.assertEquals(0, service.listInstances(NamingConstants.JavaColonNamespace.MODULE, "env").size());
            Assert.assertEquals(0, service.listInstances(NamingConstants.JavaColonNamespace.APP, "").size());
            Assert.assertEquals(0, service.listInstances(NamingConstants.JavaColonNamespace.APP, "env").size());
            Assert.assertEquals(0, service.listInstances(NamingConstants.JavaColonNamespace.COMP, "env").size());
        }

        Collection<? extends NameClassPair> listedNamespace = service.listInstances(NamingConstants.JavaColonNamespace.COMP, "");
        Assert.assertEquals(1, listedNamespace.size());

        for (NameClassPair pair : listedNamespace) {
            Assert.assertEquals("EJBContext", pair.getName());
            Assert.assertEquals(EJBContext.class.getName(), pair.getClassName());
        }
    }
}
