/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

import javax.transaction.InvalidTransactionException;
import javax.transaction.TransactionRequiredException;
import javax.transaction.TransactionRolledbackException;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INVALID_TRANSACTION;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TRANSACTION_REQUIRED;
import org.omg.CORBA.TRANSACTION_ROLLEDBACK;
import org.omg.CORBA.portable.UnknownException;

import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.OrbUtils;

public class OrbUtilsImpl implements OrbUtils {
    @Override
    public void connectToOrb(Object stub) throws CSIException {
        throw new UnsupportedOperationException(String.valueOf(stub));
    }

    @Override
    public Exception mapException(RemoteException ex) {
        String detail = ex.toString();
        SystemException sysex;

        // If minor code is specified, completion status must be also.
        // the default completion status for the exception type is used.
        if (ex instanceof NoSuchObjectException) {
            sysex = new OBJECT_NOT_EXIST(detail);
        } else if (ex instanceof TransactionRequiredException) {
            sysex = new TRANSACTION_REQUIRED(detail);
        } else if (ex instanceof TransactionRolledbackException) {
            sysex = new TRANSACTION_ROLLEDBACK(detail);
        } else if (ex instanceof InvalidTransactionException) {
            sysex = new INVALID_TRANSACTION(detail);
        } else if (ex instanceof AccessException) {
            sysex = new NO_PERMISSION(detail);
        } else {
            return new UnknownException(ex);
        }

        sysex.initCause(ex.detail);
        return sysex;
    }

    @Override
    public Exception mapException(RemoteException ex, int minorCode) {
        String detail = ex.toString();
        SystemException sysex;

        // If minor code is specified, completion status must be also.
        // the default completion status for the exception type is used.
        if (ex instanceof NoSuchObjectException) {
            sysex = new OBJECT_NOT_EXIST(detail, minorCode, CompletionStatus.COMPLETED_NO);
        } else if (ex instanceof TransactionRequiredException) {
            sysex = new TRANSACTION_REQUIRED(detail, minorCode, CompletionStatus.COMPLETED_NO);
        } else if (ex instanceof TransactionRolledbackException) {
            sysex = new TRANSACTION_ROLLEDBACK(detail, minorCode, CompletionStatus.COMPLETED_NO);
        } else if (ex instanceof InvalidTransactionException) {
            sysex = new INVALID_TRANSACTION(detail, minorCode, CompletionStatus.COMPLETED_MAYBE);
        } else if (ex instanceof AccessException) {
            sysex = new NO_PERMISSION(detail, minorCode, CompletionStatus.COMPLETED_NO);
        } else {
            return new UnknownException(ex);
        }

        sysex.initCause(ex.detail);
        return sysex;
    }
}
