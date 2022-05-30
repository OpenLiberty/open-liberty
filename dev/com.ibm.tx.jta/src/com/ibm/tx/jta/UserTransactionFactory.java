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
package com.ibm.tx.jta;

import javax.transaction.UserTransaction;

/**
 *
 */
public class UserTransactionFactory {

    private static UserTransaction _ut;

    public static UserTransaction getUserTransaction() {
        return _ut;
    }

    /**
     * DS method
     */
    public void setUT(UserTransaction ut) {
        _ut = ut;
    }

    /**
     * DS method
     */
    public void unsetUT(UserTransaction ut) {
        _ut = null;
    }
}