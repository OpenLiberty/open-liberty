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
 * Reason          Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * 186657.4        11/06/04 gareth   Add isAlive() method
 * 341158          13/03/06 gareth   Make better use of LoggingTestCase
 * 410652          12/04/07 gareth   Check Transactions ME at add time
 * ============================================================================
 */

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.ProtocolException;
import com.ibm.ws.sib.msgstore.RollbackException;
import com.ibm.ws.sib.msgstore.SeverePersistenceException;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionParticipant;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;
import com.ibm.ws.sib.msgstore.transactions.impl.WorkItem;
import com.ibm.ws.sib.msgstore.transactions.impl.WorkList;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.transactions.TransactionCallback;

public class NullParticipant implements TransactionParticipant {
    private final String _name;
    private TransactionState _state;
    private final MessageStoreTestCase _test;

    public NullParticipant(MessageStoreTestCase test, String name) {
        _test = test;
        _name = name;
    }

    public void addWork(WorkItem item) throws ProtocolException, TransactionException {}

    public WorkList getWorkList() {
        return null;
    }

    public void registerCallback(TransactionCallback callback) {}

    public boolean isAutoCommit() {
        return false;
    }

    public int getId() {
        return 0;
    }

    public int prepare() throws ProtocolException, RollbackException, SeverePersistenceException {
        _test.print(" - Participant(" + _name + ") called to prepare()    - SUCCESS *");
        return 0;
    }

    public void commit(boolean onePhase) throws ProtocolException, RollbackException, SeverePersistenceException,
                                                TransactionException, PersistenceException {
        _test.print(" - Participant(" + _name + ") called to commit()     - SUCCESS *");
    }

    public void rollback() throws ProtocolException, SeverePersistenceException, TransactionException, PersistenceException {
        _test.print(" - Participant(" + _name + ") called to rollback()   - SUCCESS *");
    }

    public void setRollbackOnly() {}

    public int getTransactionType() {
        return 0;
    }

    public PersistentTranId getPersistentTranId() {
        return null;
    }

    public void setTransactionState(TransactionState state) {
        _state = state;
    }

    public TransactionState getTransactionState() {
        return _state;
    }

    public void incrementCurrentSize() throws SIResourceException {}

    public boolean isAlive() {
        return true;
    }

    public BatchingContext getBatchingContext() {
        return null;
    }

    public void setBatchingContext(BatchingContext bc) {}

    public String toXmlString() {
        StringBuffer retval = new StringBuffer();

        retval.append("<transaction>\n");
        retval.append("<type>GLOBAL</type>\n");
        retval.append("<state>STATE_ACTIVE</state>\n");
        retval.append("<size>-1</size>\n");
        retval.append("<max-size>-1</max-size>\n");
        retval.append("<work-list></work-list>\n");
        retval.append("</transaction>\n");
        return retval.toString();
    }

    public boolean hasSubordinates() {
        return false;
    }

    public MessageStore getOwningMessageStore() {
        return null;
    }
}
