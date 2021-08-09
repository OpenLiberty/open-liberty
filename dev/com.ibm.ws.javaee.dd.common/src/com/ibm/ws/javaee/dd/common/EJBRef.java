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
package com.ibm.ws.javaee.dd.common;

/**
 * Represents &lt;ejb-ref> and &lt;ejb-local-ref>.
 */
public interface EJBRef
                extends ResourceGroup, Describable
{
    /**
     * Represents an unknown kind for {@link #getKindValue}.
     */
    int KIND_UNKNOWN = -1;

    /**
     * Represents &lt;ejb-ref> for {@link #getKindValue}.
     */
    int KIND_REMOTE = 0;

    /**
     * Represents &lt;ejb-local-ref> for {@link #getKindValue}.
     */
    int KIND_LOCAL = 1;

    /**
     * Represents an unspecified value for {@link #getTypeValue}.
     */
    int TYPE_UNSPECIFIED = -1;

    /**
     * Represents "Session" for {@link #getTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.common.EjbRefType#SESSION
     */
    int TYPE_SESSION = 0;

    /**
     * Represents "Entity" for {@link #getTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.common.EjbRefType#ENTITY
     */
    int TYPE_ENTITY = 1;

    /**
     * @return the kind of EJB reference represented by this object
     *         <ul>
     *         <li>{@link #KIND_UNKNOWN} for programmatically created references for
     *         which the kind cannot be determined
     *         <li>{@link #KIND_REMOTE} - &lt;ejb-ref>
     *         <li>{@link #KIND_LOCAL} - &lt;ejb-local-ref>
     *         </ul>
     */
    int getKindValue();

    /**
     * @return &lt;ejb-type>
     *         <ul>
     *         <li>{@link #TYPE_UNSPECIFIED} if unspecified
     *         <li>{@link #TYPE_SESSION} - Session
     *         <li>{@link #TYPE_ENTITY} - Entity
     *         </ul>
     */
    int getTypeValue();

    /**
     * @return the home interface (&lt;home> or &lt;local-home>), or null if
     *         unspecified
     */
    String getHome();

    /**
     * @return the client interface (&lt;remote> or &lt;local>), or null if
     *         unspecified
     */
    String getInterface();

    /**
     * @return &lt;ejb-link>, or null if unspecified
     */
    String getLink();
}
