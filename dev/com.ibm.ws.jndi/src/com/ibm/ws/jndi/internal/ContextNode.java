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
package com.ibm.ws.jndi.internal;

import static com.ibm.ws.jndi.WSNameUtil.normalize;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NotContextException;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jndi.WSName;

/**
 * This class represents a node in a naming context tree.
 * It only provides the basic function needed to create
 * and destroy subnodes; to bind, unbind, and rebind objects;
 * and to look up and list bindings.
 */
@SuppressWarnings("serial")
public final class ContextNode {
    final ContextNode root;
    final ContextNode parent;
    final WSName fullName;
    final boolean autoDelete;

    // Except for unittests, the values are all either:
    // - ContextNode - via autoBind/createSubcontext
    // - AutoBindNode - containing ServiceReference, via JNDIServiceBinder
    // - ServiceRegistration - via Context.bind
    // - other - via unittest autoBind
    final ConcurrentMap<String, Object> children = new ConcurrentHashMap<String, Object>(1) {
        // hook into the remove calls to trigger auto-delete
        @Override
        public Object remove(Object key) {
            Object result = super.remove(key);
            if (autoDelete && isEmpty())
                ContextNode.this.autoDelete();
            return result;
        }

        @Override
        public boolean remove(Object key, Object value) {
            boolean result = super.remove(key, value);
            if (autoDelete && isEmpty())
                ContextNode.this.autoDelete();
            return result;
        }

    };

    /** Create a root node. */
    public ContextNode() {
        this.root = this;
        this.parent = null;
        this.fullName = new WSName();
        this.autoDelete = false; // obviously a root node can't be autodeleted
    }

    private void autoDelete() {
        if (autoDelete && children.isEmpty()) {
            try {
                String key = fullName.getLast();
                parent.children.remove(key, this);
            } catch (InvalidNameException e) {
                // Automatic FFDC - this should never happen:
                // if fullname is empty this is a root node
                // and autoDelete should not be true.
            }
        }
    }

    /** Create a child node. */
    private ContextNode(ContextNode parent, String name, boolean autoDelete) throws InvalidNameException {
        this.root = parent.root;
        this.parent = parent;
        this.fullName = parent.fullName.plus(name);
        this.autoDelete = autoDelete;
    }

    /** Create a child node. */
    private ContextNode(ContextNode parent, String name) throws InvalidNameException {
        this(parent, name, false);
    }

    /**
     * Navigate to the penultimate node referred to by <code>subname</code>.
     * This is the node within which <code>subname</code> itself can be found or bound.
     * 
     * @param subname
     * @return the target node
     * @throws InvalidNameException if the target or any intermediate name is bound to something other than a context
     * @throws NameNotFoundException if the target or any intermediate context does not exist
     */
    @FFDCIgnore({ ClassCastException.class, NullPointerException.class })
    private ContextNode getTargetNode(WSName subname) throws InvalidNameException, NotContextException, NameNotFoundException {
        ContextNode target = this;
        int i = 0;
        try {
            for (/* int i = 0 */; i < subname.size() - 1; i++)
                target = (ContextNode) target.children.get(subname.get(i));
            if (target == null)
                throw new NullPointerException();
        } catch (ClassCastException e) {
            throw new NotContextException("Intermediate context name is bound to something other than a context: " + fullName.plus(subname.getPrefix(i + 1)));
        } catch (NullPointerException e) {
            throw new NameNotFoundException("Intermediate context does not exist: " + fullName.plus(subname.getPrefix(i + 1)));
        }
        return target;
    }

    /** @see Context#createSubcontext(Name) */
    ContextNode createSubcontext(WSName subname) throws InvalidNameException, NameNotFoundException, NameAlreadyBoundException, NotContextException {
        ContextNode target = getTargetNode(subname);
        String branchName = subname.getLast();
        ContextNode branch = new ContextNode(target, branchName);
        if (target.children.putIfAbsent(branchName, branch) != null)
            throw new NameAlreadyBoundException("" + fullName.plus(subname));
        return branch;
    }

    /** @see Context#bind(Name, Object) */
    void bind(WSName relativeName, Object o) throws InvalidNameException, NameAlreadyBoundException, NameNotFoundException, NotContextException {
        WSName subname = normalize(relativeName);
        ContextNode target = getTargetNode(subname);
        String lastName = subname.getLast();
        target.bindImmediate(lastName, o);
    }

    private void bindImmediate(String lastName, Object obj) throws NameAlreadyBoundException, InvalidNameException {
        if (children.putIfAbsent(lastName, obj) != null)
            throw new NameAlreadyBoundException("" + fullName.plus(lastName));
    }

    /**
     * Works like bind but automatically creates intermediate contexts if
     * they do not exist. Any automatically created contexts will be
     * cleaned up automatically when their last child is unbound.
     */
    @FFDCIgnore(ClassCastException.class)
    void autoBind(WSName subname, Object obj) throws InvalidNameException, NotContextException, NameAlreadyBoundException {
        ContextNode target = this, parent = this;
        int i = 0;
        try {
            for (/* int i = 0 */; i < subname.size() - 1; i++) {
                String elem = subname.get(i);
                target = (ContextNode) target.children.get(elem);
                // create an auto-deleteable subcontext
                if (target == null) {
                    target = new ContextNode(parent, elem, true);
                    Object existingEntry = parent.children.putIfAbsent(elem, target);
                    if (existingEntry != null) {
                        // another thread just beat me to it creating this entry
                        // assume it is a context and continue
                        target = (ContextNode) existingEntry;
                    }
                }
                parent = target;
            }
        } catch (ClassCastException e) {
            throw new NotContextException("" + fullName.plus(subname.getPrefix(i + 1)));
        }
        // allow multiple entries during invocation by JndiServiceBinder
        boolean replaced = false;
        String lastName = subname.getLast();
        AutoBindNode abNode = new AutoBindNode(obj);
        while (!replaced) {
            Object oldObj = target.children.putIfAbsent(lastName, abNode);
            if (oldObj != null) {
                if (oldObj instanceof AutoBindNode) {
                    abNode = (AutoBindNode) oldObj;
                    // This synch ensures that addition of entries to an 
                    // AutoBindNode does not take place during removal of 
                    // the same AutoBindNode.
                    synchronized (abNode) {
                        abNode.addLastEntry(obj);
                        // This is a no-op in the normal case, but if we're racing an unbind, 
                        // we might need to modify a different AutoBindNode
                        replaced = target.children.replace(lastName, oldObj, abNode);
                    }
                } else {
                    throw new NameAlreadyBoundException("" + fullName.plus(lastName));
                }
            } else {
                replaced = true;
            }
        }
    }

    Object lookup(WSName subname) throws InvalidNameException, NotContextException, NameNotFoundException {
        if (subname.isEmpty())
            return this;
        Object result = getTargetNode(subname).children.get(subname.getLast());
        if (result instanceof AutoBindNode) {
            AutoBindNode abNode = (AutoBindNode) result;
            result = abNode.getLastEntry();
        }
        if (result == null) {
            throw new NameNotFoundException(subname.toString());
        }
        return result;
    }

    /** @see Context#bind(Name, Object) */
    Object rebind(WSName relativeName, Object o) throws InvalidNameException, NameNotFoundException, NotContextException {
        WSName subname = normalize(relativeName);
        ContextNode target = getTargetNode(subname);
        String lastName = subname.getLast();
        return target.children.put(lastName, o);
    }

    /** @see Context#destroySubcontext(Name) */
    @FFDCIgnore(ClassCastException.class)
    ContextNode destroySubcontext(WSName subname) throws InvalidNameException, NameNotFoundException, ContextNotEmptyException, NotContextException {
        ContextNode target = getTargetNode(subname);
        String lastName = subname.getLast();
        Object entry = target.children.get(lastName);
        // if the object was null, our job is done
        if (entry == null)
            return null;
        // check that this is an empty branch
        try {
            ContextNode branch = (ContextNode) entry;
            if (!!!branch.children.isEmpty())
                throw new ContextNotEmptyException("" + fullName.plus(subname));
            // Now we know it satisfies the constraints, it is ok to remove it.
            // If the remove fails, return null - the object has already been
            // removed / replaced and we do not want to trigger an additional 
            // clean up operation by returning it again.
            return target.children.remove(lastName, branch) ? branch : null;
        } catch (ClassCastException e) {
            // this isn't a context!
            throw new NotContextException("" + fullName.plus(subname));
        }
    }

    /** @see Context#unbind(Name) */
    Object unbind(WSName subname) throws InvalidNameException, NameNotFoundException, NotContextException {
        ContextNode target = getTargetNode(subname);
        String lastName = subname.getLast();
        return target.children.remove(lastName);
    }

    @FFDCIgnore(NamingException.class)
    void ensureNotBound(WSName subname, Object obj) {
        ContextNode target;
        try {
            target = getTargetNode(subname);
            String lastName = subname.getLast();
            // multiple entries can be present only during invocation by JndiServiceBinder
            if (!target.children.remove(lastName, obj)) {
                Object currObj = target.children.get(lastName);
                if (currObj instanceof AutoBindNode) {
                    AutoBindNode abNode = (AutoBindNode) currObj;
                    // This synch ensures that addition of entries to an 
                    // AutoBindNode does not take place during removal 
                    // of the same AutoBindNode.
                    synchronized (abNode) {
                        boolean remove = abNode.removeEntry(obj);
                        if (remove) {
                            target.children.remove(lastName, abNode);
                        }
                    }
                }
            }
        } catch (NamingException ignored) {
        }
    }

    Map<String, Object> getChildren(WSName subname) throws InvalidNameException, NotContextException, NameNotFoundException {
        Object o = lookup(subname);
        if (!!!(o instanceof ContextNode))
            throw new NotContextException("" + fullName.plus(subname));
        return Collections.unmodifiableMap(((ContextNode) o).children);
    }

    @Override
    public String toString() {
        return "" + fullName;
    }
}