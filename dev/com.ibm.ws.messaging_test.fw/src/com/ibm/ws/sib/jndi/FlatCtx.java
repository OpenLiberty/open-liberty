/*
 * @start_no_prolog@
 * 
 * 
 * No IBM proprietary protection statement required
 * @end_no_prolog@
 */

/*******************************************************************************
 * Copyright (c) 1997, 1999 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

//package examples;

package com.ibm.ws.sib.jndi;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;

/**
 * A sample service provider that implements a flat namespace in memory.
 */

public class FlatCtx implements Context {
    protected Hashtable myEnv;
    protected Hashtable bindings = new Hashtable(11);
    protected static final NameParser myParser = new FlatNameParser();

    FlatCtx(Hashtable inEnv) {
        myEnv = (inEnv != null)
            ? (Hashtable)(inEnv.clone())
            : null;
    }

    private FlatCtx(Hashtable inEnv, Hashtable bindings) {
    this(inEnv);
    this.bindings = bindings;
    }

    private FlatCtx cloneCtx() {
    return new FlatCtx(myEnv, bindings);
    }

    /**
     * Utility method for processing composite/compound name.
     * @param name The non-null composite or compound name to process.
     * @return The non-null string name in this namespace to be processed.
     */
    protected String getMyComponents(Name name) throws NamingException {
    if (name instanceof CompositeName) {
        if (name.size() > 1) {
        throw new InvalidNameException(name.toString() +
            " has more components than namespace can handle");
        }
        return name.get(0);
    } else {
        // compound name
        return name.toString();
    }
    }

    public Object lookup(String name) throws NamingException {
    return lookup(new CompositeName(name));
    }

    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty()) {
            // Asking to look up this context itself.  Create and return
            // a new instance with its own independent environment.
            return (cloneCtx());
        }

    // Extract components that belong to this namespace
    String nm = getMyComponents(name);

    // Find object in internal hash table
        Object answer = bindings.get(nm);
        if (answer == null) {
            throw new NameNotFoundException(name + " not found");
        }
        return answer;
    }

    public void bind(String name, Object obj) throws NamingException {
    bind(new CompositeName(name), obj);
    }

    public void bind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot bind empty name");
        }

    // Extract components that belong to this namespace
    String nm = getMyComponents(name);

    // Find object in internal hash table
        if (bindings.get(nm) != null) {
            throw new NameAlreadyBoundException(
                    "Use rebind to override");
        }

    // Add object to internal hash table
        bindings.put(nm, obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
    rebind(new CompositeName(name), obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot bind empty name");
        }

    // Extract components that belong to this namespace
    String nm = getMyComponents(name);

    // Add object to internal hash table
        bindings.put(nm, obj);
    }

    public void unbind(String name) throws NamingException {
    unbind(new CompositeName(name));
    }

    public void unbind(Name name) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot unbind empty name");
        }

    // Extract components that belong to this namespace
    String nm = getMyComponents(name);

    // Remove object from internal hash table
        bindings.remove(nm);
    }

    public void rename(String oldname, String newname) throws NamingException {
    rename(new CompositeName(oldname), new CompositeName(newname));
    }

    public void rename(Name oldname, Name newname) throws NamingException {
        if (oldname.isEmpty() || newname.isEmpty()) {
            throw new InvalidNameException("Cannot rename empty name");
        }

    // Extract components that belong to this namespace
    String oldnm = getMyComponents(oldname);
    String newnm = getMyComponents(newname);

        // Check if new name exists
        if (bindings.get(newnm) != null) {
            throw new NameAlreadyBoundException(newname.toString() +
                                                " is already bound");
        }

        // Check if old name is bound
        Object oldBinding = bindings.remove(oldnm);
        if (oldBinding == null) {
            throw new NameNotFoundException(oldname.toString() + " not bound");
        }

        bindings.put(newnm, oldBinding);
    }

    public NamingEnumeration list(String name) throws NamingException {
    return list(new CompositeName(name));
    }

    public NamingEnumeration list(Name name) throws NamingException {
        if (name.isEmpty()) {
            // listing this context
            return new ListOfNames(bindings.keys());
        }

        // Perhaps 'name' names a context
        Object target = lookup(name);
        if (target instanceof Context) {
        try {
        return ((Context)target).list("");
        } finally {
        ((Context)target).close();
        }
        }
        throw new NotContextException(name + " cannot be listed");
    }

    public NamingEnumeration listBindings(String name) throws NamingException {
    return listBindings(name);
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        if (name.isEmpty()) {
            // listing this context
            return new ListOfBindings(bindings.keys());
        }

        // Perhaps 'name' names a context
        Object target = lookup(name);
        if (target instanceof Context) {
        try {
        return ((Context)target).listBindings("");
        } finally {
        ((Context)target).close();
        }
        }
        throw new NotContextException(name + " cannot be listed");
    }

    public void destroySubcontext(String name) throws NamingException {
    destroySubcontext(new CompositeName(name));
    }

    public void destroySubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException(
                "FlatCtx does not support subcontexts");
    }

    public Context createSubcontext(String name) throws NamingException {
    return createSubcontext(new CompositeName(name));
    }

    public Context createSubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException(
                "FlatCtx does not support subcontexts");
    }

    public Object lookupLink(String name) throws NamingException {
    return lookupLink(new CompositeName(name));
    }

    public Object lookupLink(Name name) throws NamingException {
        // This flat context does not treat links specially
        return lookup(name);
    }

    public NameParser getNameParser(String name) throws NamingException {
    return getNameParser(new CompositeName(name));
    }

    public NameParser getNameParser(Name name) throws NamingException {
    // Do lookup to verify name exists
    Object obj = lookup(name);
    if (obj instanceof Context) {
        ((Context)obj).close();
    }
    return myParser;
    }

    public String composeName(String name, String prefix)
            throws NamingException {
        Name result = composeName(new CompositeName(name),
                                  new CompositeName(prefix));
        return result.toString();
    }

    public Name composeName(Name name, Name prefix)
            throws NamingException {
        Name result = (Name)(prefix.clone());
        result.addAll(name);
        return result;
    }

    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        if (myEnv == null) {
            myEnv = new Hashtable(5, 0.75f);
        }
        return myEnv.put(propName, propVal);
    }

    public Object removeFromEnvironment(String propName)
            throws NamingException {
        if (myEnv == null)
            return null;

        return myEnv.remove(propName);
    }

    public Hashtable getEnvironment() throws NamingException {
        if (myEnv == null) {
            // Must return non-null
            return new Hashtable(3, 0.75f);
        } else {
            return (Hashtable)myEnv.clone();
        }
    }

    public String getNameInNamespace() throws NamingException {
        return "";
    }

    public void close() throws NamingException {
    }

    // Class for enumerating name/class pairs
    class ListOfNames implements NamingEnumeration {
        protected Enumeration names;

        ListOfNames (Enumeration names) {
            this.names = names;
        }

        public boolean hasMoreElements() {
        try {
        return hasMore();
        } catch (NamingException e) {
        return false;
        }
        }

        public boolean hasMore() throws NamingException {
            return names.hasMoreElements();
        }

        public Object next() throws NamingException {
            String name = (String)names.nextElement();
            String className = bindings.get(name).getClass().getName();
            return new NameClassPair(name, className);
        }

        public Object nextElement() {
        try {
        return next();
        } catch (NamingException e) {
        throw new NoSuchElementException(e.toString());
        }
        }

        public void close() {
        }
    }

    // Class for enumerating bindings
    class ListOfBindings extends ListOfNames {

        ListOfBindings(Enumeration names) {
        super(names);
        }

        public Object next() throws NamingException {
            String name = (String)names.nextElement();
            return new Binding(name, bindings.get(name));
        }
    }
};
