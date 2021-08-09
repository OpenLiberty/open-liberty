/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.xa.Xid;

/**
 *
 */
public class FATXID implements Xid {
    private final byte branchQualifier[];
    private final AtomicLong counter = new AtomicLong();
    private final byte globalTranID[];

    FATXID() {
        this.globalTranID = new byte[Xid.MAXGTRIDSIZE];
        this.branchQualifier = new byte[Xid.MAXBQUALSIZE];
        byte[] bytes = Long.toString(counter.incrementAndGet()).getBytes();
        System.arraycopy(bytes, 0, globalTranID, 0, bytes.length);
    }

    @Override
    public int getFormatId() {
        return 0xABC;
    }

    @Override
    public byte[] getBranchQualifier() {
        return branchQualifier;
    }

    @Override
    public byte[] getGlobalTransactionId() {
        return globalTranID;
    }
}