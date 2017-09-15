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
package com.ibm.ws.javaee.dd.ejb;

import java.util.List;

import com.ibm.ws.javaee.dd.common.DescriptionGroup;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefsGroup;
import com.ibm.ws.javaee.dd.common.SecurityRoleRef;

/**
 * Represents common subelements of the elements in &lt;enterprise-beans>.
 */
public interface EnterpriseBean
                extends JNDIEnvironmentRefsGroup,
                DescriptionGroup
{
    /**
     * Represents &lt;session> for {@link #getKindValue}. Objects of this kind
     * also implement {@link Session}.
     */
    int KIND_SESSION = 0;

    /**
     * Represents &lt;entity> for {@link #getKindValue}. Objects of this kind
     * also implement {@link Entity}.
     */
    int KIND_ENTITY = 1;

    /**
     * Represents @lt;message-driven for {@link #getKindValue}. Objects of this
     * kind also implement {@link MessageDriven}.
     */
    int KIND_MESSAGE_DRIVEN = 2;

    /**
     * @return the kind of enterprise bean represented by this object
     *         <ul>
     *         <li>{@link #KIND_SESSION} - &lt;session>
     *         <li>{@link #KIND_ENTITY} - &lt;entity>
     *         <li>{@link #KIND_MESSAGE_DRIVEN} - &lt;message-driven>
     *         </ul>
     */
    int getKindValue();

    /**
     * @return &lt;ejb-name>
     */
    String getName();

    /**
     * @return &lt;ejb-class>, or null if unspecified
     */
    String getEjbClassName();

    /**
     * @return &lt;mapped-name>, or null if unspecified
     */
    String getMappedName();

    /**
     * @return &lt;security-role-ref> as a read-only list
     */
    List<SecurityRoleRef> getSecurityRoleRefs();

    /**
     * @return &lt;security-identity>, or null if unspecified
     */
    SecurityIdentity getSecurityIdentity();
}
