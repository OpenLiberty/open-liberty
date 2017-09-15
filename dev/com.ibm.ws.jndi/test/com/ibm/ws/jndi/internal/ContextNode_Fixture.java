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

import static com.ibm.ws.jndi.internal.Assert.assertEquals;

import javax.naming.ContextNotEmptyException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.NotContextException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.ibm.ws.jndi.WSName;

public abstract class ContextNode_Fixture {
    enum BindType {
        CONTEXT, BIND, REBIND
    }

    private final BindType bindType;
    protected ContextNode root, a, ab, abc;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    ContextNode_Fixture() {
        bindType = null;
    }

    ContextNode_Fixture(BindType bindType) {
        this.bindType = bindType;
    }

    ////////////
    // SETUP //
    //////////

    @Before
    public void createStructure() throws Exception {
        root = new ContextNode();
        a = root.createSubcontext(new WSName("a"));
        ab = a.createSubcontext(new WSName("b"));
        abc = ab.createSubcontext(new WSName("c"));
        a.bind(new WSName("o"), "AN OBJECT");
        ab.bind(new WSName("o"), "ANOTHER OBJECT");
    }

    ///////////////////////////////
    // DISPATCH                 //
    // choose how to perform an \\
    // operation based on params \\
    ///////////////////////////////

    ContextNode lookupContext(ContextNode node, String name) throws NamingException {
        return (ContextNode) lookup(node, name);
    }

    Object lookup(ContextNode node, String name) throws NamingException {
        return node.lookup(new WSName(name));
    }

    ///////////////////////////////
    // DISPATCH UTILITY METHODS //
    // choose how to perform an \\
    // operation based on params \\
    ///////////////////////////////
    final Object bind(ContextNode target, String name) throws Exception {
        switch (bindType) {
            case CONTEXT:
                return target.createSubcontext(new WSName(name));
            case BIND:
                target.bind(new WSName(name), "HELLO");
                return null;
            case REBIND:
                return target.rebind(new WSName(name), "HELLO");
        }
        throw null;
    }

    final void bindAndCheckNew(ContextNode target, String name) throws Exception {
        Object result = bind(target, name);
        String expectedName = target.fullName.plus(new WSName(name)).toString();
        switch (bindType) {
            case CONTEXT:
                ContextNode node = (ContextNode) result;
                assertEquals("New node should have correct full name.", expectedName, node);
                break;
            default:
                Assert.assertNull("Nothing should have been replaced: " + result, result);
        }
    }

    final Object bindExisting(ContextNode target, String name) throws Exception {
        // Set up what exceptions to expect
        switch (bindType) {
            default:
                thrown.expect(NameAlreadyBoundException.class);
            case REBIND:
                // do nothing
        }
        return bind(target, name);
    }

    final Object unbind(ContextNode target, String name) throws Exception {
        switch (bindType) {
            case CONTEXT:
                return target.destroySubcontext(new WSName(name));
            case BIND:
                return target.unbind(new WSName(name));
            case REBIND: // No special unbind for this, so just overwrite existing entry
                return target.rebind(new WSName(name), "");
        }
        throw null;
    }

    final Object unbindObject(ContextNode target, String name) throws Exception {
        // This should only fail if we are using destroySubcontext() to perform the unbind
        switch (bindType) {
            case CONTEXT:
                thrown.expect(NotContextException.class);
        }
        return unbind(target, name);

    }

    final Object unbindNonEmptyContext(ContextNode target, String name) throws Exception {
        // Set up what exceptions to expect
        switch (bindType) {
            case CONTEXT:
                thrown.expect(ContextNotEmptyException.class);
        }
        return unbind(target, name);
    }
}
