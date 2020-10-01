/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.naming;

import java.util.List;

import javax.naming.NamingException;

/**
 * This interface is intended for implementation by any container wishing to
 * register on the JNDI NamingHelper whiteboard for the <b>ejblocal:</b> namespace.
 *
 * All services registered on the whiteboard are consulted during object lookup
 * in the appropriate namespace. This replaces the traditional JNDI bind for
 * populating the namespace.
 *
 * In order to support very lazy object creation the <b>ejblocal:</b> lookup
 * consults the whiteboard services looking for a non-null Object to
 * be returned when a registered helper knows about the object.
 *
 */
public interface EJBLocalNamingHelper<EJBBinding> {

    /**
     * This method is called by JNDI during lookups. If an implementer of this
     * service does not know about the JNDI resource in question it should
     * return null.
     *
     * If the implementer knows about the JNDI resource requested it should
     * return the object instance to be returned on the lookup.
     *
     * @param name
     *            String form of the jndi name, excluding the namespace prefix
     *            e.g. for the resource "ejblocal:<app>/<module>.jar/<bean>#<interface>" this name
     *            would be "<app>/<module>.jar/<bean>#<interface>"
     * @return the object instance to be returned on the lookup.
     * @throws NamingException
     *            is thrown when an implementation knows about the the JNDI
     *            resource, but encounters a problem obtaining an instance
     *            to return.
     */
    public Object getObjectInstance(String name) throws NamingException;

    /**
     * This method adds a JNDI lookup string binding reference.
     *
     * @param binding
     *            The EJBBinding Object that represents a reference to a bean
     * @param name
     *            String form of the jndi name, excluding the namespace prefix
     *            e.g. for the resource "ejblocal:<app>/<module>.jar/<bean>#<interface>" this name
     *            would be "<app>/<module>.jar/<bean>#<interface>"
     * @param isSimpleName
     *            Flag used to force creation of an AmbiguousEJBReference if an
     *            ambiguous simple name binding is detected
     * @return false if the binding is an AmbiguousEJBReference
     * @throws NamingException
     *            is thrown if the binding is ambiguous and customBindingsOnErr is FAIL.
     */
    boolean bind(EJBBinding binding, String name, boolean isSimpleName) throws NamingException;

    /**
     * Unbind the names from the ejblocal: name space.
     *
     * @param names List of names to remove from the
     *            application name space.
     */
    public void removeBindings(List<String> names);
}
