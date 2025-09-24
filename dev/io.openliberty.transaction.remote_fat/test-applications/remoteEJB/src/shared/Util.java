/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package shared;

import javax.transaction.SystemException;
import javax.transaction.Transaction;

import com.ibm.tx.jta.TransactionManagerFactory;

public class Util {

    public static String tranID() throws SystemException {
        final Transaction t = TransactionManagerFactory.getTransactionManager().getTransaction();

        if (t instanceof Transaction) {
            final String strID = t.toString();
            final int start = strID.indexOf("#tid=") + 5;
            final int end = strID.indexOf(",");
            return strID.substring(start, end);
        }

        return null;
    }
}
