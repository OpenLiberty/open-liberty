package com.ibm.tx.remote;

/*******************************************************************************
 * Copyright (c) 2002, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * This class simply contains minor code values for standard exceptions thrown
 * by the JTS.
 */

public interface MinorCode
{
    // 171049 - ras has 16 minor codes defined for transactions
    public static final int TRANSACTION_MINORCODE_BASE = 0x494210d0;// com.ibm.ejs.ras.WsCorbaMinor.transactionBase;  // 0x494210d0

    // minor codes for org.omg.CORBA.INTERNAL();
    public static final int NO_COORDINATOR = TRANSACTION_MINORCODE_BASE + 0x0;
    public static final int NO_GLOBAL_TID = TRANSACTION_MINORCODE_BASE + 0x1;
    public static final int LOGIC_ERROR = TRANSACTION_MINORCODE_BASE + 0x2;
    public static final int SERVER_BUSY = TRANSACTION_MINORCODE_BASE + 0x3;
}