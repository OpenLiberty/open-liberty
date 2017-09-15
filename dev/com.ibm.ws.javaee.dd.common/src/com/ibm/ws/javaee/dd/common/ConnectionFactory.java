/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
 * Represents &lt;connection-factory>.
 */
public interface ConnectionFactory extends JNDIEnvironmentRef, Describable {
    /**
     * Represents an unspecified value for {@link #getTransactionSupportValue}.
     */
    int TRANSACTION_SUPPORT_UNSPECIFIED = -1;

    /**
     * Represents "NoTransaction" for {@link #getTransactionSupportValue}.
     */
    int TRANSACTION_SUPPORT_NO_TRANSACTION = 0;

    /**
     * Represents "LocalTransaction" for {@link #getTransactionSupportValue}.
     */
    int TRANSACTION_SUPPORT_LOCAL_TRANSACTION = 1;

    /**
     * Represents "XATransaction" for {@link #getTransactionSupportValue}.
     */
    int TRANSACTION_SUPPORT_XA_TRANSACTION = 2;

    /**
     * @return &lt;interface-name>
     */
    String getInterfaceNameValue();

    /**
     * @return &lt;resource-adapter>
     */
    String getResourceAdapter();

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
     * @return &lt;transaction-support>
     *         <li>{@link #TRANSACTION_SUPPORT_UNSPECIFIED} if unspecified
     *         <li>{@link #TRANSACTION_SUPPORT_NO_TRANSACTION} - NoTransaction
     *         <li>{@link #TRANSACTION_SUPPORT_LOCAL_TRANSACTION} - LocalTransaction
     *         <li>{@link #TRANSACTION_SUPPORT_XA_TRANSACTION} - XATransaction
     */
    int getTransactionSupportValue();

    /**
     * @return &lt;property> as a read-only list
     */
    List<Property> getProperties();
}
