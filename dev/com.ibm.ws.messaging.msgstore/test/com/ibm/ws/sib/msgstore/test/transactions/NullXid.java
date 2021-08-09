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
package com.ibm.ws.sib.msgstore.test.transactions;
/*
 * Change activity:
 *
 * Reason            Date     Origin    Description
 * --------------- --------  --------  ----------------------------------------
 * 184788          09/12/03   gareth    Add transaction state model
 * ============================================================================
 */

import javax.transaction.xa.Xid;

public class NullXid implements Xid
{
    String _id;

    public NullXid(String id)
    {
        _id = id;
    }

    public int getFormatId()
    {
        return 0;
    }

    public byte[] getGlobalTransactionId()
    {
        return _id.getBytes();
    }

    public byte[] getBranchQualifier()
    {
        return _id.getBytes();
    }
}
