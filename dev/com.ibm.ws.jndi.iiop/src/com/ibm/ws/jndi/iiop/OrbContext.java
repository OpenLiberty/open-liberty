/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.iiop;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class OrbContext implements Context {
static final TraceComponent tc = Tr.register(OrbContext.class);
    private final ORB orb;
    private final Hashtable<Object, Object> environment;

    public OrbContext(ORB orb, Hashtable<?,?> environment) {
        this.orb = orb;
        this.environment = new Hashtable<Object, Object>(environment);
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    @FFDCIgnore(NO_IMPLEMENT.class)
    public Object lookup(String name) throws NamingException {
        final String methodName = "lookup(): ";
        name = CorbanameUrlContextFactory.Escaper.escapeCorbanameUrlIfNecessary(name);
        if (tc.isDebugEnabled()) Tr.debug(tc, methodName + "");
        org.omg.CORBA.Object result = orb.string_to_object(name);
        if (tc.isDebugEnabled()) Tr.debug(tc, methodName + ": orb.string_to_object() returned "+result);
        try {
            if ((result instanceof NamingContext) || result._is_a(NamingContextHelper.id())) {
                if (tc.isDebugEnabled()) Tr.debug(tc, methodName + "result seems to be a naming context - narrowing");
                NamingContext cosCtx = NamingContextHelper.unchecked_narrow(result);
                return new JndiCosNamingContext(name, cosCtx, environment);
            }
        } catch (NO_IMPLEMENT e) {
            if (tc.isDebugEnabled()) Tr.debug(tc, methodName + ": " + e);
        }
        return result;
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unbind(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unbind(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        throw new UnsupportedOperationException();
    }

}
