/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.sib.msgstore.cache.statemodel;

import com.ibm.ws.sib.transactions.PersistentTranId;

public final class TransactionIdMismatch extends StateException
{
    private static final long serialVersionUID = -4901938327580282065L;

    public TransactionIdMismatch(PersistentTranId got, PersistentTranId expected)
    {
        super("{"+got+"/"+expected+"}");
    }
}
