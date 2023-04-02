/*
 * =============================================================================
 * Copyright (c) 2012, 2023 IBM Corporation and others.
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

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 *
 */
public class MockICF implements InitialContextFactory {

    /** {@inheritDoc} */
    @Override
    public Context getInitialContext(Hashtable<?, ?> envmt) throws NamingException {
        return new MockIC();
    }

    public static final class MockIC implements Context {

        @Override
        public void unbind(String s) throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public void unbind(Name n) throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public void rename(String sOld, String sNew) throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public void rename(Name nOld, Name nNew) throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public Object removeFromEnvironment(String s) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void rebind(String s, Object o) throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public void rebind(Name n, Object o) throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public Object lookupLink(String s) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object lookupLink(Name n) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object lookup(String s) throws NamingException {
            if ("test".equals(s))
                return "MockIC";
            else
                return null;
        }

        @Override
        public Object lookup(Name n) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NamingEnumeration<Binding> listBindings(Name n) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NamingEnumeration<NameClassPair> list(Name n) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NameParser getNameParser(String s) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NameParser getNameParser(Name n) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getNameInNamespace() throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Hashtable<?, ?> getEnvironment() throws NamingException {
            // TODO Auto-generated method stub
            return new Hashtable<Object, Object>();
        }

        @Override
        public void destroySubcontext(String s) throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public void destroySubcontext(Name n) throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public Context createSubcontext(String s) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Context createSubcontext(Name n) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String composeName(String s, String pfx) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Name composeName(Name n, Name pfx) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void close() throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public void bind(String s, Object o) throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public void bind(Name n, Object o) throws NamingException {
            // TODO Auto-generated method stub

        }

        @Override
        public Object addToEnvironment(String s, Object o) throws NamingException {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
