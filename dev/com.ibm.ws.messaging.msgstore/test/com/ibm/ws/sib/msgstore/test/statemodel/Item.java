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
package com.ibm.ws.sib.msgstore.test.statemodel;

/*
 * Change activity:
 *
 * Reason          Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 *                 27/10/03 drphill  Original
 * 295531          07/11/05 schofiel Redundant return value for eventPrecommitAdd
 * 515543.2        08/07/08 gareth   Change runtime exceptions to caught exception
 * ============================================================================
 */

import com.ibm.js.test.LoggingTestCase;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

public class Item extends com.ibm.ws.sib.msgstore.Item {

    private int _becomeAvailableCount = 0;
    private int _postcommitAddCount = 0;
    private int _postcommitRemoveCount = 0;
    private int _postcommitUpdateCount = 0;
    private int _postRollbackAddCount = 0;
    private int _postRollbackRemoveCount = 0;
    private int _postRollbackUpdateCount = 0;
    private int _precommitAddCount = 0;
    private int _precommitRemoveCount = 0;
    private int _precommitUpdateCount = 0;
    private int _unlockCount = 0;

    long _expiryInterval = NEVER_EXPIRES;
    String _name = "NONE";
    int _expired = 0;

    /**
     * Default constructor
     */
    public Item() {
        super();
    }

    /**
     * Constructor for expirable items
     * 
     * @param name
     * @param interval
     */
    Item(String name, long interval) {
        super();
        _expiryInterval = interval;
        _name = name;
    }

    @Override
    public long getMaximumTimeInStore() {
        return _expiryInterval;
    }

    @Override
    public long getExpiryStartTime() {
        return 0; // ie. now
    }

    @Override
    public boolean canExpireSilently() {
        return false;
    }

    @Override
    public void eventExpiryNotification(Transaction transaction) throws SevereMessageStoreException {
        super.eventExpiryNotification(transaction);
        _expired++;
        LoggingTestCase.print("Item <" + _name + "> expired");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.AbstractItem#eventItemIsAvailable()
     */
    @Override
    public final void eventUnlocked() throws SevereMessageStoreException {
        super.eventUnlocked();
        _becomeAvailableCount++;
        _unlockCount++;
    }

    @Override
    public final void eventPostCommitAdd(Transaction transaction) throws SevereMessageStoreException {
        super.eventPostCommitAdd(transaction);
        _postcommitAddCount++;
    }

    @Override
    public final void eventPostCommitRemove(Transaction transaction) throws SevereMessageStoreException {
        super.eventPostCommitRemove(transaction);
        _postcommitRemoveCount++;
    }

    @Override
    public final void eventPostCommitUpdate(Transaction transaction) throws SevereMessageStoreException {
        super.eventPostCommitUpdate(transaction);
        _postcommitUpdateCount++;
    }

    @Override
    public final void eventPostRollbackAdd(Transaction transaction) throws SevereMessageStoreException {
        super.eventPostRollbackAdd(transaction);
        _postRollbackAddCount++;
    }

    @Override
    public final void eventPostRollbackRemove(Transaction transaction) throws SevereMessageStoreException {
        super.eventPostRollbackRemove(transaction);
        _postRollbackRemoveCount++;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.AbstractItem#eventPostRollbackUpdate(com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public final void eventPostRollbackUpdate(Transaction transaction) throws SevereMessageStoreException {
        super.eventPostRollbackUpdate(transaction);
        _postRollbackUpdateCount++;
    }

    @Override
    public final void eventPrecommitAdd(Transaction transaction) throws SevereMessageStoreException {
        super.eventPrecommitAdd(transaction);
        _precommitAddCount++;
    }

    @Override
    public final void eventPrecommitRemove(Transaction transaction) throws SevereMessageStoreException {
        super.eventPrecommitRemove(transaction);
        _precommitRemoveCount++;
    }

    @Override
    public final void eventPrecommitUpdate(Transaction transaction) throws SevereMessageStoreException {
        super.eventPrecommitUpdate(transaction);
        _precommitUpdateCount++;
    }

    /**
     * @return
     */
    public final int getBecomeAvailableCount() {
        return _becomeAvailableCount;
    }

    /**
     * @return
     */
    public final int getPostcommitAddCount() {
        return _postcommitAddCount;
    }

    /**
     * @return
     */
    public final int getPostcommitRemoveCount() {
        return _postcommitRemoveCount;
    }

    /**
     * @return
     */
    public final int getPostcommitUpdateCount() {
        return _postcommitUpdateCount;
    }

    /**
     * @return
     */
    public final int getPostRollbackAddCount() {
        return _postRollbackAddCount;
    }

    /**
     * @return
     */
    public final int getPostRollbackRemoveCount() {
        return _postRollbackRemoveCount;
    }

    /**
     * @return
     */
    public final int getPostRollbackUpdateCount() {
        return _postRollbackUpdateCount;
    }

    /**
     * @return
     */
    public final int getPrecommitAddCount() {
        return _precommitAddCount;
    }

    /**
     * @return
     */
    public final int getPrecommitRemoveCount() {
        return _precommitRemoveCount;
    }

    /**
     * @return
     */
    public final int getPrecommitUpdateCount() {
        return _precommitUpdateCount;
    }

    public final int getUnlockCount() {
        return _unlockCount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getStorageStrategy()
     */
    @Override
    public final int getStorageStrategy() {
        return STORE_ALWAYS;
    }

    public final void resetCounts() {
        _becomeAvailableCount = 0;
        _precommitAddCount = 0;
        _postcommitAddCount = 0;
        _postRollbackAddCount = 0;
        _precommitRemoveCount = 0;
        _postcommitRemoveCount = 0;
        _postRollbackRemoveCount = 0;
        _precommitUpdateCount = 0;
        _postcommitUpdateCount = 0;
        _postRollbackUpdateCount = 0;
        _unlockCount = 0;
    }
}
