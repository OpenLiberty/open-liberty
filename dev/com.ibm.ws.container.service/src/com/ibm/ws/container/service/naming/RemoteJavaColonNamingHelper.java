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
package com.ibm.ws.container.service.naming;

import java.util.Collection;

import javax.naming.NameClassPair;
import javax.naming.NamingException;

/**
 * This interface is intended for implementation by any container wishing to
 * register on the JNDI NamingHelper whiteboard for the <b>java:</b> namespace.
 * 
 * All services registered on the whiteboard are consulted during object lookup
 * in the appropriate namespace. This replaces the traditional JNDI bind for
 * populating the namespace.
 * 
 * In order to support very lazy object creation the <b>java:</b> lookup
 * consults the whiteboard services looking for a non-null Object to
 * be returned when a registered helper knows about the object.
 * 
 */
public interface RemoteJavaColonNamingHelper {

    /**
     * This method is called by JNDI during lookups. If an implementer of this
     * service does not know about the JNDI resource in question it should
     * return null.
     * 
     * If the implementer knows about the JNDI resource requested it should
     * return the object instance to be returned on the lookup.
     * 
     * @param namespace
     *            enum representing the particular java: namespace being
     *            queried, see {@link NamingConstants.JavaColonNamespace}
     * @param name
     *            String form of the jndi name, excluding the namespace prefix
     *            e.g. for the resource "java:comp/env/jdbc/test" this name
     *            would be "jdbc/test"
     * @return the object instance to be returned on the lookup.
     * @throws NamingException
     *             is thrown when an implementation knows about the the JNDI
     *             resource, but encounters a problem obtaining an instance
     *             to return.
     */
    public RemoteObjectInstance getRemoteObjectInstance(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException;

    /**
     * 
     * @param namespace
     *            enum representing the particular java: namespace being
     *            queried, see {@link NamingConstants.JavaColonNamespace}
     * @param name
     *            String form of the jndi name prefix, excluding the namespace prefix
     *            e.g. for the resource prefix "java:comp/env/jdbc" this name
     *            would be "jdbc"
     * @return true if the implementer of this service knows that there is JNDI resource
     *         whose name is started with the jndi name prefix specified
     */
    public boolean hasRemoteObjectWithPrefix(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException;

    /**
     * @param namespace
     *            enum representing the particular java: namespace being
     *            queried, see {@link NamingConstants.JavaColonNamespace}
     * @param nameInContext
     *            String form of the jndi name, excluding the namespace prefix
     *            e.g. for the resource "java:comp/env/jdbc/test" this name
     *            would be "jdbc/test". "" corresponds to the namespace root,
     *            and null is not valid.
     * @return a Collection of NameClassPair objects
     * 
     * @throws NamingException
     */
    public Collection<? extends NameClassPair> listRemoteInstances(NamingConstants.JavaColonNamespace namespace, String nameInContext) throws NamingException;

}
