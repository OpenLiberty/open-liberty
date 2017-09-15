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

/**
 * Represents the group of elements common to bean types that support
 * declarative transactions.
 */
public interface TransactionalBean
                extends EnterpriseBean
{
    /**
     * Represents an unspecified value for {@link #getTransactionTypeValue}.
     */
    int TRANSACTION_TYPE_UNSPECIFIED = -1;

    /**
     * Represents "Bean" for {@link #getTransactionTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.TransactionType#BEAN
     */
    int TRANSACTION_TYPE_BEAN = 0;

    /**
     * Represents "Container" for {@link #getTransactionTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.TransactionType#CONTAINER
     */
    int TRANSACTION_TYPE_CONTAINER = 1;

    /**
     * @return &lt;transaction-type>
     *         <ul>
     *         <li>{@link #TRANSACTION_TYPE_UNSPECIFIED} if unspecified
     *         <li>{@link #TRANSACTION_TYPE_BEAN} - Bean
     *         <li>{@link #TRANSACTION_TYPE_CONTAINER} - Container
     *         </ul>
     */
    int getTransactionTypeValue();
}
