package com.ibm.ws.sib.msgstore.persistence.objectManager;
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

import com.ibm.ws.objectManager.LogFileFullException;
import com.ibm.ws.objectManager.ObjectManager;
import com.ibm.ws.objectManager.ObjectManagerException;
import com.ibm.ws.objectManager.ObjectStore;
import com.ibm.ws.objectManager.ObjectStoreFullException;
import com.ibm.ws.objectManager.Transaction;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.PersistenceFullException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.impl.CachedPersistableImpl;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.transactions.PersistentTranId;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

public class BatchingContextImpl implements BatchingContext
{
    private static TraceComponent tc = SibTr.register(BatchingContextImpl.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private ObjectManager _objectManager;
    private ObjectStore _objectStore;
    private Transaction _tran;
    private PersistenceException _deferredException;
    private int _capacity = 10;

    // Flags used to determine if this batching context 
    // is being used to service two-phase work. These 
    // flags will be dependant on the calling of the 
    // addIndoubtXid(), updateXIDXXX() and deleteXID()
    // methods during the lifetime of this object.
    private static final int STATE_ACTIVE            = 0;
    private static final int STATE_PREPARING         = 1;
    private static final int STATE_PREPARED          = 2;
    private static final int STATE_COMMITTING        = 3;
    private static final int STATE_COMMITTED         = 4;
    private static final int STATE_ROLLINGBACK       = 5;
    private static final int STATE_ROLLEDBACK        = 6;

    private static final String[] _stateToString = {"STATE_ACTIVE",
                                                    "STATE_PREPARING",
                                                    "STATE_PREPARED",
                                                    "STATE_COMMITTING",
                                                    "STATE_COMMITTED",
                                                    "STATE_ROLLINGBACK",
                                                    "STATE_ROLLEDBACK"};

    private int  _state = STATE_ACTIVE;
    private byte[] _xid;


    /**
     * one-phase constructor
     * 
     * @param objectManager
     * @param objectStore
     */
    public BatchingContextImpl(ObjectManager objectManager, ObjectStore objectStore)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", new Object[] {"ObjectManager="+objectManager,"ObjectStore="+objectStore});

        _objectManager = objectManager;
        _objectStore   = objectStore;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
    }

    /**
     * two-phase constructor
     * 
     * @param objectManager
     * @param objectStore
     * @param transaction
     * 
     * @exception PersistenceException
     */
    public BatchingContextImpl(ObjectManager objectManager, ObjectStore objectStore, PersistentTransaction transaction) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", new Object[] {"ObjectManager="+objectManager,"ObjectStore="+objectStore,"Transaction="+transaction});

        _objectManager = objectManager;
        _objectStore   = objectStore;

        if (transaction != null)
        {
            _xid = transaction.getPersistentTranId().toByteArray();

            try
            {
                _tran = _objectManager.getTransaction();
                _tran.setXID(_xid);
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.<init>", "1:132:1.18", this);
                PersistenceException pe = new PersistenceException("Exception caught starting object manager transaction: "+ome.getMessage(), ome);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Exception caught starting object manager transaction!");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
                throw pe;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
    }
                        
    /**
     * recovery constructor
     * 
     * @param objectManager
     * @param objectStore
     * @param transaction
     * 
     * @exception PersistenceException
     */
    public BatchingContextImpl(ObjectManager objectManager, ObjectStore objectStore, Transaction transaction) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", new Object[] {"ObjectManager="+objectManager,"ObjectStore="+objectStore,"Transaction="+transaction});

        _objectManager = objectManager;
        _objectStore   = objectStore;

        if (transaction != null)
        {
            _tran  = transaction;
            _state = STATE_PREPARED;
        }
        else
        {
            PersistenceException pe = new PersistenceException("Recovered transaction is null!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Recovered transaction is null!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
            throw pe;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
    }

    public void insert(Persistable persistable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "insert", "Persistable="+persistable);

        if (_deferredException == null)
        {
            // Creation of a new item
            try
            {
                _startTransaction();
    
                PersistableImpl       thePersistable    = null;
                CachedPersistableImpl cachedPersistable = null;
    
                if (persistable instanceof PersistableImpl)
                {
                    // We are working directly on the persistable 
                    // so we can just call it directly.
                    thePersistable = (PersistableImpl)persistable;
    
                    thePersistable.addToStore(_tran, _objectStore);
                }
                else if (persistable instanceof CachedPersistableImpl)
                {
                    // We are using a cached version of the persistables
                    // data so we need to pass that to the original
                    // persistable object.
                    cachedPersistable = (CachedPersistableImpl)persistable;
                    thePersistable    = (PersistableImpl)cachedPersistable.getPersistable();
    
                    thePersistable.addToStore(_tran, _objectStore, persistable);
                }
            }
            catch (LogFileFullException lffe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(lffe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.insert", "1:210:1.18", this);
                SibTr.error(tc, "FILE_STORE_LOG_FULL_SIMS1573E");
                _deferredException = new PersistenceFullException("FILE_STORE_LOG_FULL_SIMS1573E", lffe);
            }
            catch (ObjectStoreFullException osfe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(osfe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.insert", "1:216:1.18", this);
                if (_objectStore.getStoreStrategy() == ObjectStore.STRATEGY_KEEP_ALWAYS)
                {
                    SibTr.error(tc, "FILE_STORE_PERMANENT_STORE_FULL_SIMS1574E");
                    _deferredException = new PersistenceFullException("FILE_STORE_PERMANENT_STORE_FULL_SIMS1574E", osfe);
                }
                else
                {
                    SibTr.error(tc, "FILE_STORE_TEMPORARY_STORE_FULL_SIMS1575E");
                    _deferredException = new PersistenceFullException("FILE_STORE_TEMPORARY_STORE_FULL_SIMS1575E", osfe);
                }
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.insert", "1:230:1.18", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Exception caught inserting item into object store!", ome);
                _deferredException = new PersistenceException("Exception caught inserting item into object store: "+ome.getMessage(), ome);
            }
            catch (PersistenceException pe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.insert", "1:236:1.18", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "PersistenceException caught inserting item into object store!", pe);
                _deferredException = pe;
            }
            catch (SevereMessageStoreException smse)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(smse, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.insert", "1:242:1.18", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Severe exception caught inserting item into object store!", smse);
                _deferredException = new PersistenceException("Exception caught inserting item into object store: "+smse.getMessage(), smse);;
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No work attempted as an exception has already been thrown during this batch!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "insert");
    }

    public void updateDataAndSize(Persistable persistable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "updateDataAndSize", "Persistable="+persistable);

        if (_deferredException == null)
        {
            try
            {
                _startTransaction();
    
                PersistableImpl       thePersistable    = null;
                CachedPersistableImpl cachedPersistable = null;
    
                if (persistable instanceof PersistableImpl)
                {
                    // We are working directly on the persistable 
                    // so we can just call it directly.
                    thePersistable = (PersistableImpl)persistable;
    
                    thePersistable.updateDataOnly(_tran, _objectStore);
                }
                else if (persistable instanceof CachedPersistableImpl)
                {
                    // We are using a cached version of the persistables
                    // data so we need to pass that to the original
                    // persistable object.
                    cachedPersistable = (CachedPersistableImpl)persistable;
                    thePersistable    = (PersistableImpl)cachedPersistable.getPersistable();
    
                    thePersistable.updateDataOnly(_tran, _objectStore, persistable);
                }
            }
            catch (LogFileFullException lffe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(lffe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateDataAndSize", "1:289:1.18", this);
                SibTr.error(tc, "FILE_STORE_LOG_FULL_SIMS1573E");
                _deferredException = new PersistenceFullException("FILE_STORE_LOG_FULL_SIMS1573E", lffe);
            }
            catch (ObjectStoreFullException osfe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(osfe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateDataAndSize", "1:295:1.18", this);
                if (_objectStore.getStoreStrategy() == ObjectStore.STRATEGY_KEEP_ALWAYS)
                {
                    SibTr.error(tc, "FILE_STORE_PERMANENT_STORE_FULL_SIMS1574E");
                    _deferredException = new PersistenceFullException("FILE_STORE_PERMANENT_STORE_FULL_SIMS1574E", osfe);
                }
                else
                {
                    SibTr.error(tc, "FILE_STORE_TEMPORARY_STORE_FULL_SIMS1575E");
                    _deferredException = new PersistenceFullException("FILE_STORE_TEMPORARY_STORE_FULL_SIMS1575E", osfe);
                }
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateDataAndSize", "1:309:1.18", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Exception caught updating data in object store!", ome);
                _deferredException = new PersistenceException("Exception caught updating data in object store: "+ome.getMessage(), ome);
            }
            catch (PersistenceException pe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateDataAndSize", "1:315:1.18", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Persistence exception caught updating data in object store!", pe);
                _deferredException = pe;
            }
            catch (SevereMessageStoreException smse)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(smse, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateDataAndSize", "1:321:1.18", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Severe exception caught updating data in object store!", smse);
                _deferredException = new PersistenceException("Exception caught inserting item into object store: "+smse.getMessage(), smse);;
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No work attempted as an exception has already been thrown during this batch!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "updateDataAndSize");
    }

    public void updateLockIDOnly(Persistable persistable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "updateLockIDOnly", "Persistable="+persistable);

        if (_deferredException == null)
        {
            try
            {
                _startTransaction();
                
                PersistableImpl       thePersistable    = null;
                CachedPersistableImpl cachedPersistable = null;
    
                if (persistable instanceof PersistableImpl)
                {
                    // We are working directly on the persistable 
                    // so we can just call it directly.
                    thePersistable = (PersistableImpl)persistable;
    
                    thePersistable.updateMetaDataOnly(_tran);
                }
                else if (persistable instanceof CachedPersistableImpl)
                {
                    // We are using a cached version of the persistables
                    // data so we need to pass that to the original
                    // persistable object.
                    cachedPersistable = (CachedPersistableImpl)persistable;
                    thePersistable    = (PersistableImpl)cachedPersistable.getPersistable();
    
                    thePersistable.updateMetaDataOnly(_tran, persistable);
                }
            }
            catch (LogFileFullException lffe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(lffe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateLockIDOnly", "1:368:1.18", this);
                SibTr.error(tc, "FILE_STORE_LOG_FULL_SIMS1573E");
                _deferredException = new PersistenceFullException("FILE_STORE_LOG_FULL_SIMS1573E", lffe);
            }
            catch (ObjectStoreFullException osfe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(osfe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateLockIDOnly", "1:374:1.18", this);
                if (_objectStore.getStoreStrategy() == ObjectStore.STRATEGY_KEEP_ALWAYS)
                {
                    SibTr.error(tc, "FILE_STORE_PERMANENT_STORE_FULL_SIMS1574E");
                    _deferredException = new PersistenceFullException("FILE_STORE_PERMANENT_STORE_FULL_SIMS1574E", osfe);
                }
                else
                {
                    SibTr.error(tc, "FILE_STORE_TEMPORARY_STORE_FULL_SIMS1575E");
                    _deferredException = new PersistenceFullException("FILE_STORE_TEMPORARY_STORE_FULL_SIMS1575E", osfe);
                }
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateLockIDOnly", "1:388:1.18", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Exception caught updating metadata in object store!", ome);
                _deferredException = new PersistenceException("Exception caught updating metadata in object store: "+ome.getMessage(), ome);
            }
            catch (PersistenceException pe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateLockIDOnly", "1:394:1.18", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Persistence exception caught updating metadata in object store!", pe);
                _deferredException = pe;
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No work attempted as an exception has already been thrown during this batch!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "updateLockIDOnly");
    }

    public void updateRedeliveredCountOnly(Persistable persistable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "updateRedeliveredCountOnly", "Persistable="+persistable);

        if (_deferredException == null)
        {
            try
            {
                _startTransaction();
                
                PersistableImpl       thePersistable    = null;
                CachedPersistableImpl cachedPersistable = null;
    
                if (persistable instanceof PersistableImpl)
                {
                    // We are working directly on the persistable 
                    // so we can just call it directly.
                    thePersistable = (PersistableImpl)persistable;
    
                    thePersistable.updateMetaDataOnly(_tran);
                }
                else if (persistable instanceof CachedPersistableImpl)
                {
                    // We are using a cached version of the persistables
                    // data so we need to pass that to the original
                    // persistable object.
                    cachedPersistable = (CachedPersistableImpl)persistable;
                    thePersistable    = (PersistableImpl)cachedPersistable.getPersistable();
    
                    thePersistable.updateMetaDataOnly(_tran, persistable);
                }
            }
            catch (LogFileFullException lffe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(lffe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateRedeliveredCountOnly", "1:368:1.17", this);
                SibTr.error(tc, "FILE_STORE_LOG_FULL_SIMS1573E");
                _deferredException = new PersistenceFullException("FILE_STORE_LOG_FULL_SIMS1573E", lffe);
            }
            catch (ObjectStoreFullException osfe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(osfe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateRedeliveredCountOnly", "1:374:1.17", this);
                if (_objectStore.getStoreStrategy() == ObjectStore.STRATEGY_KEEP_ALWAYS)
                {
                    SibTr.error(tc, "FILE_STORE_PERMANENT_STORE_FULL_SIMS1574E");
                    _deferredException = new PersistenceFullException("FILE_STORE_PERMANENT_STORE_FULL_SIMS1574E", osfe);
                }
                else
                {
                    SibTr.error(tc, "FILE_STORE_TEMPORARY_STORE_FULL_SIMS1575E");
                    _deferredException = new PersistenceFullException("FILE_STORE_TEMPORARY_STORE_FULL_SIMS1575E", osfe);
                }
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateRedeliveredCountOnly", "1:388:1.17", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Exception caught updating metadata in object store!", ome);
                _deferredException = new PersistenceException("Exception caught updating metadata in object store: "+ome.getMessage(), ome);
            }
            catch (PersistenceException pe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.updateRedeliveredCountOnly", "1:394:1.17", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Persistence exception caught updating metadata in object store!", pe);
                _deferredException = pe;
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No work attempted as an exception has already been thrown during this batch!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "updateRedeliveredCountOnly");
    }

    public void updateLogicalDeleteAndXID(Persistable persistable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "updateLogicalDeleteAndXID", "Persistable="+persistable);

        if (_deferredException == null)
        {
            PersistableImpl thePersistable = (PersistableImpl)persistable;
    
            if (thePersistable.isLogicallyDeleted())
            {
                // Task level prepare time remove processing.
                internalDelete(persistable);
            }
            else
            {
                // Task level rollback time remove processing. At 
                // the moment we don't have anything to do here as 
                // we simply backout the changes in the object 
                // store at executeBatch time.
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No work attempted as an exception has already been thrown during this batch!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "updateLogicalDeleteAndXID");
    }


    public void delete(Persistable persistable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "delete", "Persistable="+persistable);

        if (_deferredException == null)
        {
            // Task level rollback time add processing. At the moment 
            // we don't need to do anything as we simply backout the
            // change in the object store at executeBatch time.
    
            // Task level two-phase commit time remove processing. 
            // At the moment we don't need to do anything as we 
            // simply commit the first phase changes in the object 
            // store at executeBatch time.
    
            // Task level one-phase commit time remove processing.
            // If our state is not STATE_ACTIVE then we have carried
            // out first phase processing previously.
            if (_state == STATE_ACTIVE)
            {
                internalDelete(persistable);
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No work attempted as an exception has already been thrown during this batch!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "delete");
    }


    private void internalDelete(Persistable persistable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "internalDelete", "Persistable="+persistable);

        PersistableImpl thePersistable = (PersistableImpl)persistable;

        // Deletion of an item
        try
        {
            _startTransaction();

            thePersistable.removeFromStore(_tran);
        }
        catch (LogFileFullException lffe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(lffe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.internalDelete", "1:557:1.18", this);
            SibTr.error(tc, "FILE_STORE_LOG_FULL_SIMS1573E");
            _deferredException = new PersistenceFullException("FILE_STORE_LOG_FULL_SIMS1573E", lffe);
        }
        catch (ObjectStoreFullException osfe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(osfe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.internalDelete", "1:563:1.18", this);
            if (_objectStore.getStoreStrategy() == ObjectStore.STRATEGY_KEEP_ALWAYS)
            {
                SibTr.error(tc, "FILE_STORE_PERMANENT_STORE_FULL_SIMS1574E");
                _deferredException = new PersistenceFullException("FILE_STORE_PERMANENT_STORE_FULL_SIMS1574E", osfe);
            }
            else
            {
                SibTr.error(tc, "FILE_STORE_TEMPORARY_STORE_FULL_SIMS1575E");
                _deferredException = new PersistenceFullException("FILE_STORE_TEMPORARY_STORE_FULL_SIMS1575E", osfe);
            }
        }
        catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.internalDelete", "1:577:1.18", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Exception caught deleting from object store!", ome);
            _deferredException = new PersistenceException("Exception caught deleting from object store: "+ome.getMessage(), ome);
        }
        catch (PersistenceException pe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.internalDelete", "1:583:1.18", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Persistence exception caught deleting from object store!", pe);
            _deferredException = pe;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "internalDelete");
    }


    public void addIndoubtXID(PersistentTranId xid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "addIndoubtXID", "XID="+xid);

        if (_deferredException == null)
        {
            // We are going indoubt which basically translates
            // to preparing the object manager transaction so
            // we can just update our state here and carry out 
            // the proper action at executeBatch() time.
            if (_state == STATE_ACTIVE)
            {
                _state = STATE_PREPARING;
            }
            else
            {
                _deferredException = new PersistenceException("Cannot PREPARE batch as it not in the correct state! State="+_stateToString[_state]);
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No work attempted as an exception has already been thrown during this batch!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addIndoubtXID");
    }


    /**
     * In the OM implementation this method is used to flag the batching
     * context so that upon the next call to executeBatch the OM transaction
     * being used is committed.
     * 
     * @param xid
     */
    public void updateXIDToCommitted(PersistentTranId xid) 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "updateXIDToCommitted", "XID="+xid);

        if (_deferredException == null)
        {
            // We are committing a transaction. The transaction can
            // be either one-phase or two-phase so we need to check 
            // our state and then update it so that commit is called 
            // at executeBatch() time.
            if (_state == STATE_ACTIVE || _state == STATE_PREPARED)
            {
                _state = STATE_COMMITTING;
            }
            else
            {
                _deferredException = new PersistenceException("Cannot COMMIT batch as it not in the correct state! State="+_stateToString[_state]);
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No work attempted as an exception has already been thrown during this batch!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "updateXIDToCommitted");
    }


    /**
     * In the OM implementation this method is used to flag the batching
     * context so that upon the next call to executeBatch the OM transaction
     * being used is rolled back. This call is only valid after an 
     * addIndoubtXid/executeBatch pair have been called i.e. a prepare
     * has occurred.
     * 
     * @param xid
     */
    public void updateXIDToRolledback(PersistentTranId xid) 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "updateXIDToRolledback", "XID="+xid);

        if (_deferredException == null)
        {
            // We are rolling back a transaction. This should only be 
            // applied to prepared transactions as single-phase rollbacks
            // should not get as far as the persistence layer.
            if (_state == STATE_PREPARED)
            {
                _state = STATE_ROLLINGBACK;
            }
            else
            {
                _deferredException = new PersistenceException("Cannot ROLLBACK batch as it not in the correct state! State="+_stateToString[_state]);
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No work attempted as an exception has already been thrown during this batch!");
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "updateXIDToRolledback");
    }


    public void executeBatch() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "executeBatch");

        // If we have a deferred exception, throw it now
        if (_deferredException != null)
        {
            // If we have an existing OM tran then we need
            // to roll it back before we throw any exception.
            if (_tran != null)
            {
                try
                {
                    Transaction t = _tran;

                    _xid  = null;
                    _tran = null;

                    t.backout(false);
                    _state = STATE_ROLLEDBACK;
                }
                catch (ObjectManagerException ome)
                {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.executeBatch", "1:714:1.18", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Unexpected exception caught cleaning up transaction!", ome);
                    // Don't throw here as we are going to throw a deferred
                    // exception anyway and that may have more information 
                    // about the root cause of our problems.
                }
            }

            PersistenceException e = _deferredException;
            _deferredException = null;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Deferred exception being thrown during executeBatch!", e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "executeBatch");
            throw e;
        }
        
        // Complete the work in this batch. Dependant 
        // upon the state of this batch object we have 
        // to choose to prepare, commit or rollback
        // the object manager transaction.
        try
        {
            if (_tran != null)
            {
                Transaction t = _tran;

                switch (_state)
                {
                case STATE_PREPARING:
                    if (t.getXID() == null)
                    {
                        PersistenceException pe = new PersistenceException("No XID associated at prepare time!");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "No XID associated at prepare time!");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "executeBatch");
                        throw pe;
                    }
                    t.prepare();
                    _state = STATE_PREPARED;
                    break;
                case STATE_ACTIVE:     // Spill 1PC
                case STATE_COMMITTING:
                    _xid  = null;
                    _tran = null;
                    t.commit(false);
                    _state = STATE_COMMITTED;
                    break;
                case STATE_ROLLINGBACK:
                    _xid  = null;
                    _tran = null;
                    t.backout(false);
                    _state = STATE_ROLLEDBACK;
                    break;
                default:
                    PersistenceException pe = new PersistenceException("BatchingContext not in correct state to executeUpdate! State="+_stateToString[_state]);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "BatchingContext not in correct state to executeUpdate! State="+_stateToString[_state]);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "executeBatch");
                    throw pe;
                }
            }
        }
        catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.BatchingContextImpl.executeBatch", "1:775:1.18", this);
            PersistenceException pe = new PersistenceException("Unexpected exception caught completing transaction: "+ome.getMessage(), ome);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "Exception caught completing transaction!", ome);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "executeBatch");
            throw pe;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "executeBatch");
    }

    public void clear() 
    {
        // We are probably going to be re-used so we need 
        // to reset all of our current state variables.
        _state             = STATE_ACTIVE;
        _deferredException = null;
    }
    
    private void _startTransaction() throws ObjectManagerException
    {
        if (_tran == null)
        {
            _tran = _objectManager.getTransaction();
        }
    }

    public void setCapacity(int capacity)
    {
        _capacity = capacity;
    }

    public int getCapacity() 
    {
        return _capacity;
    }


    public void updateTickValueOnly(Persistable persistable) {/* NO-OP within file store implementation.*/}
    public void deleteXID(PersistentTranId xid) {/* NO-OP within file store implementation.*/}
    public void setUseEnlistedConnections(boolean useEnlistedConnections) {/* NO-OP within file store implementation.*/}
}
