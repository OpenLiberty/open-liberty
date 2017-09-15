/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi;

import static com.ibm.ws.jndi.WSNameUtil.normalize;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 *
 */
public abstract class WSContextBase implements Context {

    protected abstract Object lookup(WSName name) throws NamingException;

    protected abstract void bind(WSName name, Object obj) throws NamingException;

    protected abstract void rebind(WSName name, Object obj) throws NamingException;

    protected abstract void rename(WSName oldName, WSName newName) throws NamingException;

    protected abstract void unbind(WSName name) throws NamingException;

    protected abstract NamingEnumeration<NameClassPair> list(WSName name) throws NamingException;

    protected abstract NamingEnumeration<Binding> listBindings(WSName name) throws NamingException;

    protected abstract void destroySubcontext(WSName name) throws NamingException;

    protected abstract Context createSubcontext(WSName name) throws NamingException;

    protected abstract Object lookupLink(WSName name) throws NamingException;

    protected abstract NameParser getNameParser(WSName name) throws NamingException;

    @Override
    @Sensitive
    public final Object lookup(Name name) throws NamingException {
        return lookup(normalize(name));
    }

    @Override
    @Sensitive
    public final Object lookup(String name) throws NamingException {
        return lookup(normalize(name));
    }

    @Override
    public final void bind(Name name, Object obj) throws NamingException {
        bind(normalize(name), obj);
    }

    @Override
    public final void bind(String name, Object obj) throws NamingException {
        bind(normalize(name), obj);
    }

    @Override
    public final void rebind(Name name, Object obj) throws NamingException {
        rebind(normalize(name), obj);
    }

    @Override
    public final void rebind(String name, Object obj) throws NamingException {
        rebind(normalize(name), obj);
    }

    @Override
    public final void unbind(Name name) throws NamingException {
        unbind(normalize(name));
    }

    @Override
    public final void unbind(String name) throws NamingException {
        unbind(normalize(name));
    }

    @Override
    public final void rename(Name oldName, Name newName) throws NamingException {
        rename(normalize(oldName), normalize(newName));
    }

    @Override
    public final void rename(String oldName, String newName) throws NamingException {
        rename(normalize(oldName), normalize(newName));
    }

    @Override
    public final NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return list(normalize(name));
    }

    @Override
    public final NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return list(normalize(name));
    }

    @Override
    public final NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return listBindings(normalize(name));
    }

    @Override
    public final NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return listBindings(normalize(name));
    }

    @Override
    public final void destroySubcontext(Name name) throws NamingException {
        destroySubcontext(normalize(name));
    }

    @Override
    public final void destroySubcontext(String name) throws NamingException {
        destroySubcontext(normalize(name));
    }

    @Override
    public final Context createSubcontext(Name name) throws NamingException {
        return createSubcontext(normalize(name));
    }

    @Override
    public final Context createSubcontext(String name) throws NamingException {
        return createSubcontext(normalize(name));
    }

    @Override
    public final Object lookupLink(Name name) throws NamingException {
        return lookupLink(normalize(name));
    }

    @Override
    public final Object lookupLink(String name) throws NamingException {
        return lookupLink(normalize(name));
    }

    @Override
    public final NameParser getNameParser(Name name) throws NamingException {
        return getNameParser(normalize(name));
    }

    @Override
    public final NameParser getNameParser(String name) throws NamingException {
        return getNameParser(normalize(name));
    }

    @Override
    public final Name composeName(Name name, Name prefix) throws NamingException {
        return WSNameUtil.compose(prefix, name);
    }

    @Override
    public final String composeName(String name, String prefix) throws NamingException {
        return WSNameUtil.compose(prefix, name);
    }
}
