/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.embeddable;

import javax.naming.NamingException;
import javax.transaction.UserTransaction;

/**
 *
 */
public interface UserTransactionDecorator {
    /**
     *
     * @param ut the actual UserTransaction object
     * @param injection true if the object is being injected
     * @param injectionContext the injection target context if injection is true, or null if unspecified
     * @return the actual UserTransaction object or a wrapper
     * @throws NamingException
     */
    public UserTransaction decorateUserTransaction(UserTransaction ut, boolean injection, Object injectionContext) throws NamingException;
}