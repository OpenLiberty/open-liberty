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
package com.ibm.ws.container.service.naming;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NotContextException;

import org.junit.Assert;
import org.junit.Test;

public class JavaColonNamespaceBindingsTest implements JavaColonNamespaceBindings.ClassNameProvider<String> {
    @Override
    public String getBindingClassName(String value) {
        return "type:" + value;
    }

    @Test
    public void testEmpty() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);

        Assert.assertFalse(bindings.hasObjectWithPrefix(""));
        Assert.assertFalse(bindings.hasObjectWithPrefix("x"));
        Assert.assertEquals(NameClassPairTestHelper.newSet(),
                            NameClassPairTestHelper.newSet(bindings.listInstances("")));
    }

    @Test
    public void testBasic() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);
        bindings.bind("a", "1");
        bindings.bind("x/b", "2");
        bindings.bind("x/y/c", "3");

        Assert.assertEquals("1", bindings.lookup("a"));
        Assert.assertNull(bindings.lookup("x"));
        Assert.assertEquals("2", bindings.lookup("x/b"));
        Assert.assertNull(bindings.lookup("x/y"));
        Assert.assertEquals("3", bindings.lookup("x/y/c"));

        Assert.assertTrue(bindings.hasObjectWithPrefix(""));
        Assert.assertFalse(bindings.hasObjectWithPrefix("a"));
        Assert.assertTrue(bindings.hasObjectWithPrefix("x"));
        Assert.assertFalse(bindings.hasObjectWithPrefix("x/b"));
        Assert.assertTrue(bindings.hasObjectWithPrefix("x/y"));
        Assert.assertFalse(bindings.hasObjectWithPrefix("x/y/c"));

        Assert.assertEquals(NameClassPairTestHelper.newSet(new NameClassPair("a", "type:1"), new NameClassPair("x", Context.class.getName())),
                            NameClassPairTestHelper.newSet(bindings.listInstances("")));
        Assert.assertEquals(NameClassPairTestHelper.newSet(new NameClassPair("b", "type:2"), new NameClassPair("y", Context.class.getName())),
                            NameClassPairTestHelper.newSet(bindings.listInstances("x")));
        Assert.assertEquals(NameClassPairTestHelper.newSet(new NameClassPair("c", "type:3")),
                            NameClassPairTestHelper.newSet(bindings.listInstances("x/y")));
    }

    @Test(expected = NotContextException.class)
    public void testGetBindingAsContext() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);
        bindings.bind("x", "1");
        bindings.lookup("x/a");
    }

    @Test(expected = NotContextException.class)
    public void testGetBindingAsSubcontext() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);
        bindings.bind("x", "1");
        bindings.lookup("x/y/a");
    }

    @Test(expected = NotContextException.class)
    public void testListBindingAsContext() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);
        bindings.bind("a", "1");
        bindings.listInstances("a");
    }

    @Test(expected = NotContextException.class)
    public void testListBindingAsSubcontext() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);
        bindings.bind("a", "1");
        bindings.listInstances("a/x");
    }

    @Test(expected = NotContextException.class)
    public void testListBindingAsSubSubcontext() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);
        bindings.bind("a", "1");
        bindings.listInstances("a/x/y");
    }

    @Test(expected = NotContextException.class)
    public void testListSubcontextBindingAsContext() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);
        bindings.bind("x/a", "1");
        bindings.listInstances("x/a");
    }

    @Test(expected = NotContextException.class)
    public void testListSubcontextBindingAsSubcontext() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);
        bindings.bind("x/a", "1");
        bindings.listInstances("x/a/y");
    }

    @Test(expected = NotContextException.class)
    public void testListSubcontextBindingAsSubSubcontext() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);
        bindings.bind("x/a", "1");
        bindings.listInstances("x/a/y/z");
    }

    @Test
    public void testImplicitContext() throws Exception {
        JavaColonNamespaceBindings<String> bindings = new JavaColonNamespaceBindings<String>(NamingConstants.JavaColonNamespace.COMP, this);
        bindings.bind("x/y/c", "3");

        Assert.assertTrue(bindings.hasObjectWithPrefix(""));
        Assert.assertTrue(bindings.hasObjectWithPrefix("x"));
        Assert.assertTrue(bindings.hasObjectWithPrefix("x/y"));

        Assert.assertEquals(NameClassPairTestHelper.newSet(new NameClassPair("x", Context.class.getName())),
                            NameClassPairTestHelper.newSet(bindings.listInstances("")));
        Assert.assertEquals(NameClassPairTestHelper.newSet(new NameClassPair("y", Context.class.getName())),
                            NameClassPairTestHelper.newSet(bindings.listInstances("x")));
        Assert.assertEquals(NameClassPairTestHelper.newSet(new NameClassPair("c", "type:3")),
                            NameClassPairTestHelper.newSet(bindings.listInstances("x/y")));
    }
}
