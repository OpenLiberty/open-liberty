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
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import javax.naming.NameClassPair;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.container.service.naming.NamingConstants;

public class JavaColonNameServiceTest {
    @Test
    public void testGetObjectInstance() throws Exception {
        TestJavaColonNameService service = new TestJavaColonNameService();
        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.MODULE, "ModuleName"));
        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.MODULE, "AppName"));
        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.MODULE, "env"));
        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.APP, "ModuleName"));
        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.APP, "AppName"));
        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.APP, "env"));

        service.setAppName("app");
        service.setModuleName("module");
        Assert.assertEquals("module", service.getObjectInstance(NamingConstants.JavaColonNamespace.MODULE, "ModuleName"));
        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.MODULE, "AppName"));
        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.MODULE, "env"));
        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.APP, "ModuleName"));
        Assert.assertEquals("app", service.getObjectInstance(NamingConstants.JavaColonNamespace.APP, "AppName"));
        Assert.assertEquals(null, service.getObjectInstance(NamingConstants.JavaColonNamespace.APP, "env"));
    }

    @Test
    public void testHasObjectWithPrefix() throws Exception {
        TestJavaColonNameService service = new TestJavaColonNameService();
        Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.MODULE, ""));
        Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.MODULE, "env"));
        Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.APP, ""));
        Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.APP, "env"));

        service.setAppName("app");
        service.setModuleName("module");
        Assert.assertTrue(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.MODULE, ""));
        Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.MODULE, "env"));
        Assert.assertTrue(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.APP, ""));
        Assert.assertFalse(service.hasObjectWithPrefix(NamingConstants.JavaColonNamespace.APP, "env"));
    }

    @Test
    public void testListInstances() throws Exception {
        TestJavaColonNameService service = new TestJavaColonNameService();
        Assert.assertEquals(NameClassPairTestHelper.newSet(),
                            NameClassPairTestHelper.newSet(service.listInstances(NamingConstants.JavaColonNamespace.MODULE, "")));
        Assert.assertEquals(NameClassPairTestHelper.newSet(),
                            NameClassPairTestHelper.newSet(service.listInstances(NamingConstants.JavaColonNamespace.MODULE, "env")));
        Assert.assertEquals(NameClassPairTestHelper.newSet(),
                            NameClassPairTestHelper.newSet(service.listInstances(NamingConstants.JavaColonNamespace.APP, "")));
        Assert.assertEquals(NameClassPairTestHelper.newSet(),
                            NameClassPairTestHelper.newSet(service.listInstances(NamingConstants.JavaColonNamespace.APP, "env")));

        service.setAppName("app");
        service.setModuleName("module");
        Assert.assertEquals(NameClassPairTestHelper.newSet(new NameClassPair("ModuleName", String.class.getName())),
                            NameClassPairTestHelper.newSet(service.listInstances(NamingConstants.JavaColonNamespace.MODULE, "")));
        Assert.assertEquals(NameClassPairTestHelper.newSet(),
                            NameClassPairTestHelper.newSet(service.listInstances(NamingConstants.JavaColonNamespace.MODULE, "env")));
        Assert.assertEquals(NameClassPairTestHelper.newSet(new NameClassPair("AppName", String.class.getName())),
                            NameClassPairTestHelper.newSet(service.listInstances(NamingConstants.JavaColonNamespace.APP, "")));
        Assert.assertEquals(NameClassPairTestHelper.newSet(),
                            NameClassPairTestHelper.newSet(service.listInstances(NamingConstants.JavaColonNamespace.APP, "env")));
    }
}
