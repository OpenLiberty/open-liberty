/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal;

import java.io.Serializable;

import com.ibm.ws.jpa.fvt.txsync.testlogic.TargetEntityManager;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TestWorkRequest;

public interface TxSyncCMTBuddyLocal extends java.io.Serializable {
    public Serializable doWorkRequestWithTxMandatory(TestWorkRequest work, TargetEntityManager targetEm);

    public Serializable doWorkRequestWithTxNever(TestWorkRequest work, TargetEntityManager targetEm);

    public Serializable doWorkRequestWithTxNotSupported(TestWorkRequest work, TargetEntityManager targetEm);

    public Serializable doWorkRequestWithTxRequired(TestWorkRequest work, TargetEntityManager targetEm);

    public Serializable doWorkRequestWithTxRequiresNew(TestWorkRequest work, TargetEntityManager targetEm);

    public Serializable doWorkRequestWithTxSupports(TestWorkRequest work, TargetEntityManager targetEm);

}
