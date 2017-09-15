/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.jmock.Mockery;
import org.junit.Test;

public class WASInitialContextFactoryBuilderTest {
    WASInitialContextFactoryBuilder sut = new WASInitialContextFactoryBuilder();
    private static Context ctx = new Mockery().mock(Context.class);

    @Test
    public void testNoICFProperty() throws Exception {
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        assertNull(sut.createInitialContextFactory(env));

    }

    @Test
    public void testNonExistentICF() throws Exception {
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        env.put(InitialContext.INITIAL_CONTEXT_FACTORY, "does.not.exist");
        assertNull(sut.createInitialContextFactory(env));
    }

    public static class MyICF implements InitialContextFactory {
        public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
            return ctx;
        }
    }

    @Test
    public void testUserICF() throws Exception {
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        env.put(InitialContext.INITIAL_CONTEXT_FACTORY, MyICF.class.getName());
        assertSame(ctx, sut.createInitialContextFactory(env).getInitialContext(env));
    }
}
