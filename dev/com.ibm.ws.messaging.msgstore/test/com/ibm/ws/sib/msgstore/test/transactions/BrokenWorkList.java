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
 * Reason          Date    Origin    Description
 * ------------- -------- --------  -------------------------------------------
 * 184788        09/12/03  gareth   Add transaction state model
 * 186657.1      24/05/04  gareth   Per-work-item error checking.
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.WorkItem;
import com.ibm.ws.sib.msgstore.transactions.impl.WorkList;

public class BrokenWorkList implements WorkList {
    public static final int BREAK_AT_PRE_COMMIT = 1;
    public static final int BREAK_AT_COMMIT = 2;
    public static final int BREAK_AT_ROLLBACK = 3;

    private final int _breakPoint;
    private final MessageStoreTestCase _test;

    public BrokenWorkList(MessageStoreTestCase test, int breakPoint) {
        _test = test;
        _breakPoint = breakPoint;
    }

    public void addWork(WorkItem item) {}

    public void preCommit(PersistentTransaction transaction) throws MessageStoreException {
        if (_breakPoint == BREAK_AT_PRE_COMMIT) {
            _test.print("* - Exception thrown in WorkList.PreCommit - SUCCESS *");
            throw new MessageStoreException();
        } else {
            _test.print("* - WorkList called to PreCommit           - SUCCESS *");
        }
    }

    public void commit(PersistentTransaction transaction) throws MessageStoreException {
        if (_breakPoint == BREAK_AT_COMMIT) {
            _test.print("* - Exception thrown in WorkList.Commit    - SUCCESS *");
            throw new MessageStoreException();
        } else {
            _test.print("* - WorkList called to Commit              - SUCCESS *");
        }
    }

    public void rollback(PersistentTransaction transaction) throws MessageStoreException {
        if (_breakPoint == BREAK_AT_ROLLBACK) {
            _test.print("* - Exception thrown in WorkList.Rollback  - SUCCESS *");
            throw new MessageStoreException();
        } else {
            _test.print("* - WorkList called to Rollback            - SUCCESS *");
        }
    }

    public void postComplete(PersistentTransaction transaction, boolean committed) {
        _test.print("* - WorkList called to PostComplete        - SUCCESS *");
    }

    public String toXmlString() {
        StringBuffer retval = new StringBuffer();

        retval.append("<work-list>\n");
        retval.append("<work-item></work-item>\n");
        retval.append("</work-list>\n");

        return retval.toString();
    }
}
