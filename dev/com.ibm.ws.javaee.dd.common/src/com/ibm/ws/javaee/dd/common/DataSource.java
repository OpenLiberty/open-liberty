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

import java.util.List;

/**
 * Represents &lt;data-source>.
 */
public interface DataSource
                extends JNDIEnvironmentRef
{
    /**
     * @return &lt;description>, or null if unspecified
     */
    String getDescription();

    /**
     * @return &lt;class-name>, or null if unspecified
     */
    String getClassNameValue();

    /**
     * @return &lt;server-name>, or null if unspecified
     */
    String getServerName();

    /**
     * @return true if &lt;port-number> is specified
     * @see #getPortNumber
     */
    boolean isSetPortNumber();

    /**
     * @return &lt;port-number> if specified
     * @see #isSetPortNumber
     */
    int getPortNumber();

    /**
     * @return &lt;database-name>, or null if unspecified
     */
    String getDatabaseName();

    /**
     * @return &lt;url>, or null if unspecified
     */
    String getUrl();

    /**
     * @return &lt;user>, or null if unspecified
     */
    String getUser();

    /**
     * @return &lt;password>, or null if unspecified
     */
    String getPassword();

    /**
     * @return &lt;property> as a read-only list
     */
    List<Property> getProperties();

    /**
     * @return true if &lt;login-timeout> is specified
     * @see #getLoginTimeout
     */
    boolean isSetLoginTimeout();

    /**
     * @return &lt;login-timeout> if specified
     * @see #isSetLoginTimeout
     */
    int getLoginTimeout();

    /**
     * @return true if &lt;transactional> is specified
     * @see #isTransactional
     */
    boolean isSetTransactional();

    /**
     * @return &lt;transactional> if specified
     * @see #isSetTransactional
     */
    boolean isTransactional();

    /**
     * @return &lt;isolation-level>
     *         <ul>
     *         <li>{@link java.sql.Connection#TRANSACTION_NONE} if unspecified
     *         <li>{@link java.sql.Connection#TRANSACTION_READ_UNCOMMITTED} - TRANSACTION_READ_UNCOMMITTED
     *         <li>{@link java.sql.Connection#TRANSACTION_READ_COMMITTED} - TRANSACTION_READ_COMMITTED
     *         <li>{@link java.sql.Connection#TRANSACTION_REPEATABLE_READ} - TRANSACTION_REPEATABLE_READ
     *         <li>{@link java.sql.Connection#TRANSACTION_SERIALIZABLE} - TRANSACTION_SERIALIZABLE
     *         </ul>
     */
    int getIsolationLevelValue();

    /**
     * @return true if &lt;initial-pool-size> is specified
     * @see #getInitialPoolSize
     */
    boolean isSetInitialPoolSize();

    /**
     * @return &lt;initial-pool-size> if specified
     * @see #isSetInitialPoolSize
     */
    int getInitialPoolSize();

    /**
     * @return true if &lt;max-pool-size> is specified
     * @see #getMaxPoolSize
     */
    boolean isSetMaxPoolSize();

    /**
     * @return &lt;max-pool-size> if specified
     * @see #isSetMaxPoolSize
     */
    int getMaxPoolSize();

    /**
     * @return true if &lt;min-pool-size> is specified
     * @see #getMinPoolSize
     */
    boolean isSetMinPoolSize();

    /**
     * @return &lt;min-pool-size> if specified
     * @see #isSetMinPoolSize
     */
    int getMinPoolSize();

    /**
     * @return true if &lt;max-idle-time> is specified
     * @see #getMaxIdleTime
     */
    boolean isSetMaxIdleTime();

    /**
     * @return &lt;max-idle-time> if specified
     * @see #isSetMaxIdleTime
     */
    int getMaxIdleTime();

    /**
     * @return true if &lt;max-statements> is specified
     * @see #getMaxStatements
     */
    boolean isSetMaxStatements();

    /**
     * @return &lt;max-statements> if specified
     * @see #isSetMaxStatements
     */
    int getMaxStatements();
}
