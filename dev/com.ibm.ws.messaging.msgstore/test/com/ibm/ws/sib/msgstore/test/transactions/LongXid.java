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
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 *  SIB0048c.ms.2  05/02/07 gareth   Resolve wide XID problem
 * ============================================================================
 */

import java.util.Random;

import javax.transaction.xa.Xid;

import com.ibm.ws.sib.utils.Base64Utils;

public class LongXid implements Xid
{
    private int    _formatId;
    private byte[] _gtrid;
    private byte[] _bqual;

    public LongXid()
    {
        Random generator = new Random(System.currentTimeMillis());

        _formatId = generator.nextInt();

        // Create max length GTRID and assign random 
        // values to the bytes.
        _gtrid = new byte[Xid.MAXGTRIDSIZE];
        generator.nextBytes(_gtrid);

        // Create max length BQUAL and assign random 
        // values to the bytes.
        _bqual = new byte[Xid.MAXBQUALSIZE];
        generator.nextBytes(_bqual);
    }

    public byte[] getBranchQualifier()
    {
        return _bqual;
    }

    public int getFormatId()
    {
        return _formatId;
    }

    public byte[] getGlobalTransactionId()
    {
        return _gtrid;
    }

    public String toString()
    {
        return "FormatID: "+_formatId+", GTRID: "+Base64Utils.encodeBase64(_gtrid)+", BQUAL: "+Base64Utils.encodeBase64(_bqual);
    }
}

