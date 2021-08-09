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
package com.ibm.ws.sib.msgstore.test.persistence.dispatcher;
/*
 * Change activity:
 *
 * Reason     Date       Origin    Description
 * ---------- ---------- --------  --------------------------------------------
 * 184390.1.1 26/01/04   schofiel  Revised Reliability Qualities of Service - MS - Tests for spill
 * 191800     24/02/04   pradine   Add NLS support to the persistence layer
 * 184390.1.3 27/02/04   schofiel  Revised Reliability Qualities of Service - MS - PersistentDispatcher
 * 188052.2   16/02/04   pradine   Changes to the garbage collector (continued)
 * 188050.4   06/04/04   pradine   SpecJAppServer2003 optimization
 * 213328     30/06/04   pradine   Perform synchronous delete during 2PC processing
 * 214937     08/07/04   pradine   Avoid resizing of lists if batching contexts are cached
 * 327709     14/12/05   gareth    Output NLS messages when OM files are full
 * F1332-51592 13/10/11  vmadhuka  Persist redelivery count
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.PersistenceFullException;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.transactions.PersistentTranId;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

public class BatchingContextImpl implements BatchingContext
{
    private static TraceComponent tc = SibTr.register(BatchingContextImpl.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    public static final int EXECUTE_DELAY_MILLIS = 50;
    public static final int EXECUTE_FAIL_DELAY_MILLIS = 150;
    
    private int _numTasksInBatch;
    private boolean _executeWillFail;
    private boolean _executeWillFailAsFull;
    private PersistableEventDispatchListener _listener;
    private int _identity;
    private int _batchNumber;

    public BatchingContextImpl(PersistableEventDispatchListener listener)
    {
        _listener = listener;
        _identity = System.identityHashCode(this);
    }
    
    public void setCapacity(int capacity) {}
    
    public int getCapacity() 
    {
        return 0;
    }
    
    public void setUseEnlistedConnections(boolean useEnlistedConnections) {}

    public void insert(Persistable tuple)
    {
        _numTasksInBatch++;
    }

    public void updateDataAndSize(Persistable tuple)
    {
        _numTasksInBatch++;
    }

    public void updateLockIDOnly(Persistable tuple)
    {
        _numTasksInBatch++;
    }

    public void updateRedeliveredCountOnly(Persistable tuple)
    {
    	_numTasksInBatch++;
    }

    public void updateLogicalDeleteAndXID(Persistable tuple)
    {
        _numTasksInBatch++;
    }

    public void delete(Persistable tuple)
    {
        _numTasksInBatch++;
    }

    public void addIndoubtXID(PersistentTranId xid)
    {
        _numTasksInBatch++;
    }

    public void updateXIDToCommitted(PersistentTranId xid)
    {
        _numTasksInBatch++;
    }
    
    public void updateXIDToRolledback(PersistentTranId xid)
    {
        _numTasksInBatch++;
    }
    
    public void deleteXID(PersistentTranId xid)
    {
        _numTasksInBatch++;
    }
    
    public void executeBatch() throws PersistenceException
    {
        try
        {
            if (_executeWillFail || _executeWillFailAsFull)
            {
                Thread.sleep(EXECUTE_FAIL_DELAY_MILLIS);
            }
            else
            {
                Thread.sleep(EXECUTE_DELAY_MILLIS);
            }
        }
        catch (InterruptedException iexc) {}
        
        if (_executeWillFailAsFull)
        {
            throw new PersistenceFullException("Write failed in batching context due to PersistenceFullException.");
        }

        if (_executeWillFail)
        {
            throw new PersistenceException("Write failed in batching context due to PersistenceException.");
        }
        
        if (_listener != null)
        {
            _listener.eventExecuteBatch(_identity, _batchNumber, _numTasksInBatch);
        }
        
        _batchNumber++;
    }

    public void clear()
    {
        _numTasksInBatch       = 0;
        _executeWillFail       = false;
        _executeWillFailAsFull = false;
    }

    public void setExecuteFail(boolean fail)
    {
        if (tc.isEntryEnabled()) SibTr.entry(tc, "setExecuteFail", "Fail="+fail);

        _executeWillFail = fail;

        if (tc.isEntryEnabled()) SibTr.exit(tc, "setExecuteFail");
    }

    public void setExecuteFailAsFull(boolean fail)
    {
        if (tc.isEntryEnabled()) SibTr.entry(tc, "setExecuteFailAsFull", "Fail="+fail);

        _executeWillFailAsFull = fail;                                               

        if (tc.isEntryEnabled()) SibTr.exit(tc, "setExecuteFailAsFull");
    }

    public void setPersistableEventDispatchListener(PersistableEventDispatchListener listener)
    {
        _listener = listener;
    }
}
