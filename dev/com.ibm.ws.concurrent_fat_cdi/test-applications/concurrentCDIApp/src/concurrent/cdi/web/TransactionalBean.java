/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import java.io.Serializable;

import jakarta.inject.Singleton;
import jakarta.transaction.TransactionSynchronizationRegistry;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@Singleton
public class TransactionalBean implements Serializable {
    private static final long serialVersionUID = 8518443344930037109L;

    private static Object getTransactionKey() throws NamingException {
        TransactionSynchronizationRegistry tranSyncRegistry = (TransactionSynchronizationRegistry) new InitialContext().lookup("java:comp/TransactionSynchronizationRegistry");
        return tranSyncRegistry.getTransactionKey();
    }

    public Object runAsNever() throws Exception {
        return getTransactionKey();
    }

    public Object runAsSupports() throws Exception {
        return getTransactionKey();
    }
}
