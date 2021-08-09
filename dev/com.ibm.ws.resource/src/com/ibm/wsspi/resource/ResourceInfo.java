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
package com.ibm.wsspi.resource;

import java.util.List;

/**
 * Information about a resource.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface ResourceInfo
{
    /**
     * @see #getAuth
     */
    // see com.ibm.ws.javaee.dd.common.ResourceRef#AUTH_CONTAINER
    int AUTH_CONTAINER = 0;

    /**
     * @see #getAuth
     */
    // see com.ibm.ws.javaee.dd.common.ResourceRef#AUTH_APPLICATION
    int AUTH_APPLICATION = 1;

    /**
     * @see #getSharingScope
     */
    // see com.ibm.ws.javaee.dd.common.ResourceRef#SHARING_SCOPE_SHAREABLE
    int SHARING_SCOPE_SHAREABLE = 0;

    /**
     * @see #getSharingScope
     */
    // see com.ibm.ws.javaee.dd.common.ResourceRef#SHARING_SCOPE_UNSHAREABLE
    int SHARING_SCOPE_UNSHAREABLE = 1;

    /**
     * Represents an unset value for {@link #getBranchCoupling}.
     */
    int BRANCH_COUPLING_UNSET = -1;

    /**
     * @see #getBranchCoupling
     */
    // see com.ibm.ejs.models.base.extensions.commonext.BranchCouplingKind#LOOSE
    int BRANCH_COUPLING_LOOSE = 0;

    /**
     * @see #getBranchCoupling
     */
    // see com.ibm.ejs.models.base.extensions.commonext.BranchCouplingKind#TIGHT
    int BRANCH_COUPLING_TIGHT = 1;

    /**
     * Returns the name, which is either null (for a direct lookup), relative to
     * java:comp/env (e.g., jdbc/myDS), or is fully-qualified with a scope (e.g.,
     * java:global/env/myDS or java:comp/jdbc/myDS).
     */
    String getName();

    /**
     * @return the description, or null if unset
     */
    String getDescription();

    /**
     * @return the class type name (e.g., javax.sql.DataSource)
     */
    String getType();

    /**
     * @return the authentication type
     *         <ul>
     *         <li>{@link #AUTH_CONTAINER} - Container
     *         <li>{@link #AUTH_APPLICATION} - Application
     *         </ul>
     */
    int getAuth();

    /**
     * @return the sharing scope
     *         <ul>
     *         <li>{@link #SHARING_SCOPE_SHAREABLE} - Shareable
     *         <li>{@link #SHARING_SCOPE_UNSHAREABLE} - Unshareable
     *         </ul>
     */
    int getSharingScope();

    /**
     * @return the LoginConfigurationName for this object
     */
    String getLoginConfigurationName();

    /**
     * @return the non-null login properties
     */
    List<? extends Property> getLoginPropertyList();

    /**
     * @return the configured isolation level
     *         <ul>
     *         <li>{@link java.sql.Connection#TRANSACTION_NONE} <li>{@link java.sql.Connection#TRANSACTION_READ_UNCOMMITTED} <li>
     *         {@link java.sql.Connection#TRANSACTION_READ_COMMITTED} <li>{@link java.sql.Connection#TRANSACTION_REPEATABLE_READ} <li>
     *         {@link java.sql.Connection#TRANSACTION_SERIALIZABLE} </ul>
     */
    int getIsolationLevel();

    /**
     * @return the commit priority, or 0 if unspecified
     */
    int getCommitPriority();

    /**
     * @return the branch coupling
     *         <ul>
     *         <li>{@link #BRANCH_COUPLING_UNSET} <li>{@link #BRANCH_COUPLING_LOOSE} <li>{@link #BRANCH_COUPLING_TIGHT} </li>
     */
    public int getBranchCoupling(); // F923-4350

    interface Property
    {
        String getName();

        String getValue();
    }
}
