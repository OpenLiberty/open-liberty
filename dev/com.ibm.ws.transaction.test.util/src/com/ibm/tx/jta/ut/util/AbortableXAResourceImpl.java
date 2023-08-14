/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.tx.jta.ut.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.transaction.xa.Xid;

import com.ibm.tx.jta.AbortableXAResource;

/**
 *
 */
public class AbortableXAResourceImpl extends com.ibm.tx.jta.ut.util.XAResourceImpl implements AbortableXAResource {

    /**  */
    private static final long serialVersionUID = -1945889317488521383L;

    public AbortableXAResourceImpl(String string) {
        super(string);
    }

    public static AbortableXAResourceImpl getAbortableXAResourceImpl(String string) {
        return new AbortableXAResourceImpl(string);
    }

    @Override
    public void abort(Xid xid)
    {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yy:HH:mm:ss");
        System.out.println("abort(" + _key + ") on Xid " + xid + "called at: " + df.format(date));

        // Mark the query as having been aborted
        setQueryAborted();

        // Setting this flag allows the "busy thread" to complete
        setAmBusyInLongRunningQuery(false);
    }

}
