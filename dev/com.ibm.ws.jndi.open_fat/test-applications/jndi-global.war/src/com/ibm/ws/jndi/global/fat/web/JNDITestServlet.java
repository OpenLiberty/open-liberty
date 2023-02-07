/*
 * =============================================================================
 * Copyright (c) 2011, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.jndi.global.fat.web;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.PartialResultException;
import javax.naming.spi.NamingManager;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/JNDITestServlet")
@SuppressWarnings("serial")
public class JNDITestServlet extends FATServlet {
    public void testServletIsReachable() {
        System.out.println("Hello, world.");
    }

    @Test
    public void testCreateInitialContext() throws NamingException {
        Context ctx = new InitialContext();
        Assert.assertNotNull(ctx);
        System.out.println(ctx.getClass());
        System.out.println(ctx);
    }

    @Test
    public void testCreateSubcontext() throws NamingException {
        Context ctx = new InitialContext();
        ctx.createSubcontext("testCreateSubContext");
        try {
            ctx.createSubcontext("testCreateSubContext");
        } catch (NameAlreadyBoundException e) {
            System.out.println("As expected, caught " + e);
        }
    }

    @Test
    public void testBindUnbind() throws NamingException {
        Context ctx = new InitialContext();
        ctx = ctx.createSubcontext("testBindUnbind");
        HashMap<Object, Object> map = new HashMap<Object, Object>();
        map.put("hello", "world");
        ctx.bind("A", map);
        checkBound(ctx, "A", map);
        ctx.unbind("A");
        checkNotBound(ctx, "A");
    }

    @Test
    public void testRename() throws NamingException {
        Context ctx = new InitialContext();
        ctx = ctx.createSubcontext("testRename");
        HashMap<Object, Object> map = new HashMap<Object, Object>();
        map.put("hello", "world");
        ctx.bind("A", map);
        checkBound(ctx, "A", map);
        ctx.rename("A", "B");
        checkNotBound(ctx, "A");
        checkBound(ctx, "B", map);
    }

    @Test
    public void testMultipleRebinds() throws NamingException {
        Context ctx = new InitialContext();
        ctx = ctx.createSubcontext("testMultipleRebinds");
        ctx.bind("someUniqueName", "value1");
        checkBound(ctx, "someUniqueName", "value1");
        ctx.rebind("someUniqueName", "value2");
        checkBound(ctx, "someUniqueName", "value2");
        ctx.rebind("someUniqueName", "value3");
        checkBound(ctx, "someUniqueName", "value3");
    }

    @Test
    public void testRebindNewValue() throws NamingException {
        Context ctx = new InitialContext();
        ctx = ctx.createSubcontext("testRebindNewValue");
        ctx.rebind("someUniqueName", "value1");
        checkBound(ctx, "someUniqueName", "value1");
    }

    @Test
    public void testListExternal() throws Exception {
        Context ctx = new InitialContext();
        NamingEnumeration<NameClassPair> list = ctx.list("database");
        Set<String> names = new HashSet<String>();

        while (list.hasMore()) {
            NameClassPair next = list.next();
            Assert.assertEquals(Context.class.getName(), next.getClassName());
            names.add(next.getName());
        }

        Set<String> expected = new HashSet<String>(Arrays.asList("user,admin".split(",")));
        Assert.assertEquals(expected, names);
    }

    @Test
    public void testListExternalBindings() throws Exception {
        Context ctx = new InitialContext();
        NamingEnumeration<Binding> list = ctx.listBindings("database/user");
        Set<String> expected = new HashSet<String>(Arrays.asList("name,password,retryCount".split(",")));
        Set<String> names = new HashSet<String>();
        while (list.hasMore()) {
            Binding binding = list.next();
            String name = binding.getName();
            Object value = binding.getObject();
            names.add(name);
            if (name.equals("name"))
                Assert.assertEquals(value, "sam");
            if (name.equals("password"))
                Assert.assertEquals(value, "beckett");
            if (name.equals("retryCount"))
                Assert.assertEquals(value, 7);
        }

        Assert.assertEquals(expected, names);
    }

    @Test
    public void testDeleteExternalBindings() throws Exception {
        Context ctx = new InitialContext();
        for (DeleteOperation op : DeleteOperation.values()) {
            // try to delete the parent contexts of entries
            op.expectDeleteToFailWith(ctx, "database", PartialResultException.class);
            op.expectDeleteToFailWith(ctx, "database/user", PartialResultException.class);
            // try to delete the entries themselves
            op.expectDeleteToFailWith(ctx, "database/user/name", OperationNotSupportedException.class);
        }
    }

    @Test
    public void testCustomInitialContextFactory() throws Exception {
        try {
            Hashtable<String, Object> env = new Hashtable<String, Object>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.ws.jndi.global.fat.web.MockICF");
            Context ctx = new InitialContext(env);
            Assert.assertNotNull("A null InitialContext was returned, should have got a MockIC", ctx);
            Assert.assertEquals("The InitialContext did not function as expected when performing test lookup", ctx.lookup("test"), "MockIC");
        } catch (NoInitialContextException e) {
            StringWriter w = new StringWriter();
            PrintWriter p = new PrintWriter(w);
            e.printStackTrace(p);
            if (p != null)
                p.close();
            Assert.fail("Caught a NoInitialContextException, the test InitialContextFactory class was probably not accessable\nStack was " + w.toString());
        }
    }

    @Test
    public void testRebind() throws Exception {
        Context ctx = new InitialContext();
        ctx.rebind("lookupTest", "lookupTest");
        checkBound(ctx, "lookupTest", "lookupTest");
    }

    @Test
    public void testNamingManager() throws Exception {
        Object refInfo = new MockICF();
        Name name = new CompositeName("test");
        Context nameCtx = new InitialContext();
        Hashtable<?, ?> environment = new Hashtable<Object, Object>();
        NamingManager.getObjectInstance(refInfo, name, nameCtx, environment);
    }

    enum DeleteOperation {
        UNBIND {
            @Override
            void delete(Context ctx, String name) throws NamingException {
                ctx.unbind(name);
            }
        },
        REBIND {
            @Override
            void delete(Context ctx, String name) throws NamingException {
                ctx.rebind(name, "dummy" + counter++);
            }
        },
        RENAME {
            @Override
            void delete(Context ctx, String name) throws NamingException {
                ctx.rename(name, "dummy" + counter++);
            }
        };

        abstract void delete(Context ctx, String name) throws NamingException;

        static int counter;

        void expectDeleteToFailWith(Context ctx, String name, Class<? extends Exception> exceptionClass) throws Exception {
            try {
                delete(ctx, name);
                Assert.fail("Should not have been allowed to delete '" + name + "'");
            } catch (Exception e) {
                if (exceptionClass.isInstance(e))
                    return;
                throw e;
            }
        }
    }

    private void checkBound(Context ctx, String name, Object expected) throws NamingException {
        Object o = ctx.lookup(name);
        Assert.assertEquals("retrieved value should be the same as inserted", expected, o);
    }

    private static void checkNotBound(Context ctx, String name1) throws NamingException {
        try {
            ctx.lookup(name1);
            Assert.fail("Should have thrown a NameNotFoundException");
        } catch (NameNotFoundException e) {
        }
    }
}
