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

package com.ibm.ws.sib.jndi;

import javax.naming.Context;

/* ************************************************************************** */
/**
 * Initial context factory for the hierarchical context tutorial factory
 * 
 * @author David Vines
 * 
 */
/* ************************************************************************** */
public class InitCtxFactory implements javax.naming.spi.InitialContextFactory {

    private static javax.naming.Context _ctx = new HierCtx(new java.util.Hashtable());

    public javax.naming.Context getInitialContext(java.util.Hashtable env) {
        return _ctx;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setUp method
     * /* --------------------------------------------------------------------------
     */
    /**
     * This method sets the environment so that the naming service will use this
     * factory if does not already have an initial context and to use our
     * own javaURLContextFactory in the event that someone else (e.g. was.naming
     * in a full build) overrides the standard behaviour.
     */
    public static void setUp() {
        System.setProperty(
                           Context.INITIAL_CONTEXT_FACTORY,
                           InitCtxFactory.class.getName());

        System.setProperty(
                           Context.URL_PKG_PREFIXES,
                           "com.ibm.ws.sib.jndi");

    }
}
