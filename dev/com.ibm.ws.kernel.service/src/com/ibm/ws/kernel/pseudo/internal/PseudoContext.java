/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.pseudo.internal;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A pseudo context object that controls when to fail with indicating
 * that the jndi-1.0 feature is not enabled. If the name passed in
 * with a url scheme (ex. rmi:), a failure is not thrown as this ensures
 * the default behavior of what is available in the javax.naming package.
 */
public class PseudoContext implements Context {

    private static final TraceComponent tc = Tr.register(PseudoContext.class);

    private final Hashtable<Object, Object> env = new Hashtable<Object, Object>();

    public PseudoContext(Hashtable<?, ?> env) {
        this.env.putAll(env);
    }

    /**
     * Get the URL context for the given name. The URL context is only possible
     * if the name contains a ":", indicating that the name is actually a URL with
     * a scheme. If the URL context isn't available, an exception is thrown indicating
     * that the jndi feature needs to be enabled to do the specified operation by
     * the user.
     *
     * @param name The name to get the URL context for or fail if one isn't available
     * @return The URL context if available
     * @throws NamingException
     */
    private Context getURLContextOrFail(String name) throws NamingException {
        Context urlContext = null;

        if (name.contains(":")) {
            urlContext = getURLContext(name);
            if (urlContext == null) {
                fail(name);
            }
        } else {
            fail(name);
        }

        return urlContext;
    }

    private Context getURLContextOrFail(Name name) throws NamingException {
        return getURLContextOrFail(name.toString());
    }

    /**
     * Get the URL context given a name based on the scheme of the URL.
     *
     * @param name The name to get the URL context for
     * @return The URL context for the name
     * @throws NamingException
     */
    private Context getURLContext(String name) throws NamingException {
        int schemeIndex = name.indexOf(":");
        if (schemeIndex != -1) {
            String scheme = name.substring(0, schemeIndex);
            return NamingManager.getURLContext(scheme, env);
        }

        return null;
    }

    private void fail(String name) throws NamingException {
        String errorMessage = Tr.formatMessage(tc, "jndi.not.installed", name);
        throw new NamingException(errorMessage);
    }

    @Override
    public Object addToEnvironment(String s, Object o) throws NamingException {
        return env.put(s, o);
    }

    @Override
    public void bind(Name n, Object o) throws NamingException {
        getURLContextOrFail(n).bind(n, o);
    }

    @Override
    public void bind(String s, Object o) throws NamingException {
        getURLContextOrFail(s).bind(s, o);
    }

    @Override
    public void close() throws NamingException {
        env.clear();
    }

    @Override
    public Name composeName(Name n, Name pfx) throws NamingException {
        return getURLContextOrFail(n).composeName(n, pfx);
    }

    @Override
    public String composeName(String s, String pfx) throws NamingException {
        return getURLContextOrFail(s).composeName(s, pfx);
    }

    @Override
    public Context createSubcontext(Name n) throws NamingException {
        return getURLContextOrFail(n).createSubcontext(n);
    }

    @Override
    public Context createSubcontext(String s) throws NamingException {
        return getURLContextOrFail(s).createSubcontext(s);
    }

    @Override
    public void destroySubcontext(Name n) throws NamingException {
        getURLContextOrFail(n).destroySubcontext(n);
    }

    @Override
    public void destroySubcontext(String s) throws NamingException {
        getURLContextOrFail(s).destroySubcontext(s);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return new Hashtable<Object, Object>(env);
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        fail("null");
        return null;
    }

    @Override
    public NameParser getNameParser(Name n) throws NamingException {
        return getURLContextOrFail(n).getNameParser(n);
    }

    @Override
    public NameParser getNameParser(String s) throws NamingException {
        return getURLContextOrFail(s).getNameParser(s);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name n) throws NamingException {
        return getURLContextOrFail(n).list(n);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
        return getURLContextOrFail(s).list(s);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name n) throws NamingException {
        return getURLContextOrFail(n).listBindings(n);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
        return getURLContextOrFail(s).listBindings(s);
    }

    @Override
    public Object lookup(Name n) throws NamingException {
        return getURLContextOrFail(n).lookup(n);
    }

    @Override
    public Object lookup(String s) throws NamingException {
        return getURLContextOrFail(s).lookup(s);
    }

    @Override
    public Object lookupLink(Name n) throws NamingException {
        return getURLContextOrFail(n).lookupLink(n);
    }

    @Override
    public Object lookupLink(String s) throws NamingException {
        return getURLContextOrFail(s).lookup(s);
    }

    @Override
    public void rebind(Name n, Object o) throws NamingException {
        getURLContextOrFail(n).rebind(n, o);
    }

    @Override
    public void rebind(String s, Object o) throws NamingException {
        getURLContextOrFail(s).rebind(s, o);
    }

    @Override
    public Object removeFromEnvironment(String s) throws NamingException {
        return env.remove(s);
    }

    @Override
    public void rename(Name nOld, Name nNew) throws NamingException {
        getURLContextOrFail(nOld).rename(nOld, nNew);
    }

    @Override
    public void rename(String sOld, String sNew) throws NamingException {
        getURLContextOrFail(sOld).rename(sOld, sNew);
    }

    @Override
    public void unbind(Name n) throws NamingException {
        getURLContextOrFail(n).unbind(n);
    }

    @Override
    public void unbind(String s) throws NamingException {
        getURLContextOrFail(s).unbind(s);
    }
}
