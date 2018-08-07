package com.ibm.ws.sib.msgstore.persistence.objectManager;

/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.IOException;
import java.util.ArrayList;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.objectManager.ConcurrentLinkedList;
import com.ibm.ws.objectManager.List;
import com.ibm.ws.objectManager.ManagedObject;
import com.ibm.ws.objectManager.ObjectManagerException;
import com.ibm.ws.objectManager.ObjectStore;
import com.ibm.ws.objectManager.Token;
import com.ibm.ws.objectManager.Transaction;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.PersistentDataEncodingException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum;
import com.ibm.ws.sib.msgstore.persistence.impl.Tuple;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

public class PersistableImpl implements Persistable, Tuple, XmlConstants
{
    private static TraceNLS nls = TraceNLS.getTraceNLS(MessageStoreConstants.MSG_BUNDLE);
    private static TraceComponent tc = SibTr.register(PersistableImpl.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    // Defect 585163
    // The Item meta data
    private final long _uniqueID;
    private final long _streamID;
    private long _lockID = AbstractItem.NO_LOCK_ID;
    private long _referredID = AbstractItem.NO_ID;
    private long _sequence;
    private long _expiryTime;
    private int _storageStrategy = AbstractItem.STORE_NEVER;
    private int _priority;
    private int _persistentSize;
    private boolean _canExpireSilently;
    private final TupleTypeEnum _tupleType;
    private String _className;
    private PersistentTranId _persistentTranID;
    private boolean _logicallyDeleted;
    private int _redeliveredCount;
    private long _deliveryDelayTime;
    private boolean _deliveryDelayTimeIsSuspect = false;

    // Our link to the byte data
    private AbstractItemLink _link;

    // Container, i.e. item stream or reference stream, for this object
    private Persistable _containingStream;

    // Used to track async work    
    private int _persistableOperationBegunCounter;
    private int _persistableOperationCompletedCounter;

    private Token _metaDataToken;

    // Defect 463642
    // This flag is used to track whether this persistable needs to make it 
    // to disk. If it is true then the parent stream of this Item was over
    // its spill limits at add time and therefore we need to write this to
    // disk.
    private boolean _wasSpillingAtAddition = false;

    // Save the approx In Memory Size of the Item, if we know it
    private int _inMemoryByteSize;

    public PersistableImpl(long uniqueID, long containingStreamID, TupleTypeEnum tupleType)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { "UniqueID=" + uniqueID, "StreamID=" + containingStreamID, "Type=" + tupleType });

        // Defect 585163
        _uniqueID = uniqueID;
        _streamID = containingStreamID;
        _tupleType = tupleType;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    public PersistableImpl(long uniqueID, PersistableImpl containingStream, TupleTypeEnum tupleType)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { "UniqueID=" + uniqueID, "Type=" + tupleType, "Stream=" + containingStream });

        // Defect 585163
        _uniqueID = uniqueID;
        _streamID = containingStream.getUniqueId();
        _tupleType = tupleType;

        _containingStream = containingStream;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    public PersistableImpl(Token metaDataToken) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", "MetaDataToken=" + metaDataToken);

        _metaDataToken = metaDataToken;

        // Defect 585163
        // Get the meta data object off the disk.
        PersistableMetaData metaData = getMetaData();

        // We need to restore our state from the copy
        // on disk so that we can release our meta data
        // from memory once we are consistent.
        _uniqueID = metaData.getUniqueId();
        _streamID = metaData.getStreamId();
        _lockID = metaData.getLockID();
        _referredID = metaData.getReferredID();
        _sequence = metaData.getSequence();
        _expiryTime = metaData.getExpiryTime();
        _storageStrategy = metaData.getStorageStrategy();
        _priority = metaData.getPriority();
        _persistentSize = metaData.getPersistentSize();
        _canExpireSilently = metaData.canExpireSilently();
        _tupleType = metaData.getTupleType();
        _className = metaData.getClassName();
        _redeliveredCount = metaData.getRedeliveredCount();
        _deliveryDelayTime = metaData.getDeliveryDelayTime();
        _deliveryDelayTimeIsSuspect = metaData.getDeliveryDelayTimeIsSuspect();

        byte[] tranId = metaData.getTransactionId();
        if (tranId != null)
        {
            _persistentTranID = new PersistentTranId(tranId);
        }

        // Defect 585163
        // If we are in the process of deleting this persistable
        // we need to set our logicallyDeleted flag to ensure that
        // we restore our Item to the correct state.
        try
        {
            if (metaData.getState() == ManagedObject.stateToBeDeleted)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Setting logicallyDeleted=true");

                _logicallyDeleted = true;
            }
        } catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl.<init>", "1:188:1.36", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Exception caught checking state of meta data!", ome);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "<init>");
            throw new PersistenceException("Exception caught checking state of meta data!", ome);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    // FSIB0112b.ms.1
    public java.util.List<DataSlice> getPersistedData()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPersistedData");

        java.util.List<DataSlice> retval = null;

        try
        {
            Token rawDataToken = null;

            if (_metaDataToken != null)
            {
                // If we haven't accessed our metadata since restart we may 
                // need to get our managed object from the token.
                PersistableMetaData metaData = (PersistableMetaData) _metaDataToken.getManagedObject();

                rawDataToken = metaData.getRawDataToken();
            }

            // If we have a token then we can try
            // to get our persistent data from the 
            // managed object.
            if (rawDataToken != null)
            {
                // FSIB0112b.ms.2
                // We need to check what we get returned. If we get a v61 
                // object then we need to convert to List<DataSlice>. If
                // not then we can ask the object directly.
                Object object = rawDataToken.getManagedObject();

                if (object != null)
                {
                    if (object instanceof PersistableSlicedData)
                    {
                        PersistableSlicedData slicedData = (PersistableSlicedData) object;

                        retval = slicedData.getData();
                    }
                    else if (object instanceof PersistableRawData)
                    {
                        PersistableRawData rawData = (PersistableRawData) object;

                        byte[] data = rawData.getData();

                        retval = new ArrayList<DataSlice>(1);
                        retval.add(new DataSlice(data));
                    }
                }
            }
        } catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl.getPersistedData", "1:250:1.36", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Unexpected exception caught retrieving persistent data!", ome);
        }

        // TODO: This is a hack as some of the cache layer further
        // up seems to think that if the data is null then the item
        // is a rolled back in-doubt!
        if (retval == null)
        {
            retval = new ArrayList<DataSlice>(0);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPersistedData", "return=" + retval);
        return retval;
    }

    public PersistableMetaData getMetaData() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMetaData");

        PersistableMetaData metaData = null;

        // Defect 585163
        // Get the meta data object from the token. This may result
        // in the object being read from disk if it has dropped out
        // of memory.
        try
        {
            metaData = (PersistableMetaData) _metaDataToken.getManagedObject();
        } catch (ClassCastException cce)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(cce, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl.getMetaData", "1:282:1.36", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "ClassCaseException caught retrieving meta data!", cce);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getMetaData");
            throw new PersistenceException("ClassCastException caught retrieving meta data!", cce);
        } catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl.getMetaData", "1:289:1.36", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Unexpected exception caught retrieving meta data!", ome);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getMetaData");
            throw new PersistenceException("Exception caught retrieving meta data!", ome);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMetaData", "return=" + metaData);
        return metaData;
    }

    // Defect 585163
    // Used to get the root stream token during initial
    // startup of the message store
    public Token getMetaDataToken()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getMetaDataToken");
            SibTr.exit(this, tc, "getMetaDataToken", "return=" + _metaDataToken);
        }
        return _metaDataToken;
    }

    /**
     * Add this Persistable and it's raw data managed object
     * to the provided transaction.
     * 
     * @param tran The transaction under which the add of this Persistable to
     *            the ObjectStore takes place.
     * @param store The ObjectStore to add this Persistable to.
     * 
     * @exception ObjectManagerException
     * @exception PersistenceException
     * @exception SevereMessageStoreException
     */
    public void addToStore(Transaction tran, ObjectStore store) throws PersistenceException, ObjectManagerException, SevereMessageStoreException
    {
        addToStore(tran, store, this);
    }

    /**
     * Add this Persistable and it's raw data managed object
     * to the provided transaction.
     * 
     * @param tran
     * @param store
     * @param persistable
     *            This is a handle to the persistable to use to retrieve our data from. It
     *            will normally be "this" but can sometimes be a cached version if the
     *            storage strategy is STORE_MAYBE.
     * 
     * @exception ObjectManagerException
     * @exception PersistenceException
     * @exception SevereMessageStoreException
     */
    public void addToStore(Transaction tran, ObjectStore store, Persistable persistable) throws PersistenceException, ObjectManagerException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addToStore", new Object[] { "Tran=" + tran, "ObjectStore=" + store, "Persistable=" + persistable });

        // Defect 585163
        // Create our meta data object so that we can populate it 
        // ready for adding to the file store.
        PersistableMetaData metaData = new PersistableMetaData(_uniqueID, _streamID, _tupleType.toString());

        metaData.setLockID(persistable.getLockID());
        metaData.setReferredID(persistable.getReferredID());
        metaData.setSequence(persistable.getSequence());
        metaData.setExpiryTime(persistable.getExpiryTime());
        metaData.setStorageStrategy(persistable.getStorageStrategy());
        metaData.setPriority(persistable.getPriority());
        metaData.setPersistentSize(persistable.getPersistentSize());
        metaData.setCanExpireSilently(persistable.getCanExpireSilently());
        metaData.setClassName(persistable.getItemClassName());
        metaData.setRedeliveredCount(persistable.getRedeliveredCount());
        metaData.setDeliveryDelayTime(persistable.getDeliveryDelayTime());

        PersistentTranId tranId = persistable.getPersistentTranId();
        if (tranId != null)
        {
            metaData.setTransactionId(tranId.toByteArray());
        }

        // Defect 542362
        // We need to catch any object manager exceptions so that
        // we can reset our state if we know that our object
        // manager transaction is going to be rolled back.
        // 
        // This is to handle the case where this persistables
        // addToStore() hits a problem.
        try
        {
            // If we are a Stream then we need to create our persistent
            // lists to store our child Items and Streams in.
            if ((_tupleType == TupleTypeEnum.ITEM_STREAM) || (_tupleType == TupleTypeEnum.REFERENCE_STREAM) || (_tupleType == TupleTypeEnum.ROOT))
            {
                // Defect 297550
                // If we are a STORE_MAYBE ItemStream then we don't
                // need a persistent structure for an ItemStream. 
                // Otherwise we need to create our persistent lists.
                if (_storageStrategy != AbstractItem.STORE_MAYBE)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Persistent item is a stream. Building lists.");

                    // Defect 585163
                    ConcurrentLinkedList streamList = new ConcurrentLinkedList(tran, store, 64);
                    metaData.setStreamListToken(streamList.getToken());

                    ConcurrentLinkedList itemList = new ConcurrentLinkedList(tran, store, 64);
                    metaData.setItemListToken(itemList.getToken());
                }
            }

            // Create our raw data managed object. At this
            // point we will update our copy of the persistent
            // data from the ItemLink.
            // FSIB0112b.ms.2
            // Make sure all new objects use the new DataSlice
            // based binary data object.
            PersistableSlicedData slicedData = new PersistableSlicedData();

            try
            {
                // Get our data from the passed persistable. This will
                // either be this object or a cached version.
                slicedData.setData(persistable.getData());
            } catch (SevereMessageStoreException smse)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(smse, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl.addToStore", "1:416:1.36", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(tc, "Severe exception caught retrieving latest copy of binary data from Item!", smse);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "addToStore");
                throw smse;
            } catch (PersistentDataEncodingException pdee)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(pdee, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl.addToStore", "1:423:1.36", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Encoding exception caught retrieving latest copy of binary data from Item!", pdee);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "addToStore");
                throw pdee;
            }

            // Allocate in ObjectStore
            Token rawDataToken = store.allocate(slicedData);

            // Add MO to transaction
            tran.add(slicedData);

            // Add token to metadata
            metaData.setRawDataToken(rawDataToken);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Persistent data token added to transaction: " + rawDataToken);

            // Then we need to allocate the item in the object store
            _metaDataToken = store.allocate(metaData);

            // Defect 441208
            // If we are STORE_MAYBE then we do not need a persistent
            // representation in our containing stream as at start-up
            // all STORE_MAYBE Items/Streams will be deleted.
            if ((_containingStream != null) && (_storageStrategy != AbstractItem.STORE_MAYBE))
            {
                // If we aren't the root then we need to keep our
                // parents pointers up to date.
                PersistableImpl parentStream = (PersistableImpl) _containingStream;

                PersistableMetaData parentMetaData = parentStream.getMetaData();

                // Are we expirable? If so then we need to mark 
                // our stream so it can be pre-loaded at startup.
                if ((getExpiryTime() > 0) && !parentMetaData.containsExpirables())
                {
                    // Only need to update if our streams flag
                    // isn't already set. This flag will be reset 
                    // at startup time when the stream is read in 
                    // if it no longer contains any expirables.
                    tran.lock(parentMetaData);
                    parentMetaData.setContainsExpirables(true);
                    tran.replace(parentMetaData);
                }

                // Defect 297550
                // We only need to update our persistent lists
                // if we have any so only STORE_ALWAYS and
                // STORE_EVENTUALLY lists are updated.
                if (parentStream.getStorageStrategy() != AbstractItem.STORE_MAYBE)
                {
                    // We have a persistent parent so we can 
                    // update it's item list
                    ConcurrentLinkedList list = (ConcurrentLinkedList) parentMetaData.getItemListToken().getManagedObject();
                    List.Entry itemListEntry = list.addEntry(_metaDataToken, tran);

                    // Add the item list entry token to the metadata
                    metaData.setItemListEntryToken(((ManagedObject) itemListEntry).getToken());

                    // Finally, if this is a stream, we need to add 
                    // this item to it's parent's list of streams
                    if ((_tupleType == TupleTypeEnum.ITEM_STREAM) || (_tupleType == TupleTypeEnum.REFERENCE_STREAM))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Persistent item is a stream. Adding to parent.");

                        list = (ConcurrentLinkedList) parentMetaData.getStreamListToken().getManagedObject();
                        List.Entry streamListEntry = list.addEntry(_metaDataToken, tran);

                        // Add the stream list entry token to the metadata
                        metaData.setStreamListEntryToken(((ManagedObject) streamListEntry).getToken());
                    }
                }
            }

            // Finally add it to the transaction
            tran.add(metaData);
        } catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl.addToStore", "1:502:1.36", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Exception caught trying to add persistable to object manager!", ome);

            // Defect 542362
            // This exception is going to trigger a rollback
            // of our object manager transaction so we need to
            // rollback the state of our object manager objects.
            _metaDataToken = null; // Defect 585163

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "addToStore");
            throw ome;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addToStore");
    }

    /**
     * Only update the persistent copy of the binary data held within this Persistable.
     * 
     * @param tran The ObjectManager transaction under which the update of the data is carried out.
     * 
     * @exception ObjectManagerException
     * @throws SevereMessageStoreException
     */
    public void updateDataOnly(Transaction tran, ObjectStore store) throws PersistenceException, ObjectManagerException, SevereMessageStoreException
    {
        updateDataOnly(tran, store, this);
    }

    /**
     * Only update the persistent copy of the binary data held within this Persistable.
     * 
     * @param tran The ObjectManager transaction under which the update of the data is carried out.
     * @param persistable
     *            This is a handle to the persistable to use to retrieve our data from. It
     *            will normally be "this" but can sometimes be a cached version if the
     *            storage strategy is STORE_MAYBE.
     * 
     * @exception ObjectManagerException
     * @throws SevereMessageStoreException
     */
    public void updateDataOnly(Transaction tran, ObjectStore store, Persistable persistable) throws PersistenceException, ObjectManagerException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "updateDataOnly", new Object[] { "Tran=" + tran, "Persistable=" + persistable });

        // Defect 585163
        // Get the meta data object to work with. This may involve 
        // pulling it off disk if it has fallen out of memory.
        PersistableMetaData metaData = getMetaData();

        Token rawDataToken = metaData.getRawDataToken();

        if (rawDataToken != null)
        {
            Object object = rawDataToken.getManagedObject();

            if (object != null)
            {
                // FSIB0112b.ms.2
                // We need to check the type of our binary data object
                // to make sure it's in the correct format
                if (object instanceof PersistableSlicedData)
                {
                    PersistableSlicedData slicedData = (PersistableSlicedData) object;

                    tran.lock(slicedData);

                    try
                    {
                        // Get our data from the passed persistable. This will
                        // either be this object or a cached version.
                        slicedData.setData(persistable.getData());
                    } catch (SevereMessageStoreException smse)
                    {
                        com.ibm.ws.ffdc.FFDCFilter.processException(smse, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl.updateDataOnly", "1:577:1.36", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            SibTr.event(tc, "Severe exception caught retrieving latest copy of binary data from Item!", smse);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "updateDataOnly");
                        throw smse;
                    } catch (PersistentDataEncodingException pdee)
                    {
                        com.ibm.ws.ffdc.FFDCFilter.processException(pdee, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl.updateDataOnly", "1:584:1.36", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            SibTr.event(this, tc, "Encoding exception caught retrieving latest copy of binary data from Item!", pdee);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(this, tc, "updateDataOnly");
                        throw pdee;
                    }

                    tran.replace(slicedData);
                }
                else if (object instanceof PersistableRawData)
                {
                    // FSIB0112b.ms.2 
                    // In this case we can replace the old data object
                    // with a new one created from the list of DataSlice
                    // object we get from the passed Persistable
                    PersistableRawData rawData = (PersistableRawData) object;

                    // Delete the old object
                    tran.delete(rawData);

                    // Create the new object.
                    PersistableSlicedData slicedData = new PersistableSlicedData();

                    try
                    {
                        // Get our data from the passed persistable. This will
                        // either be this object or a cached version.
                        slicedData.setData(persistable.getData());
                    } catch (PersistentDataEncodingException pdee)
                    {
                        com.ibm.ws.ffdc.FFDCFilter.processException(pdee, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl.updateDataOnly", "1:614:1.36", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            SibTr.event(this, tc, "Encoding exception caught retrieving latest copy of binary data from Item!", pdee);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(this, tc, "updateDataOnly");
                        throw pdee;
                    }

                    // Allocate in ObjectStore
                    rawDataToken = store.allocate(slicedData);

                    // Add MO to transaction
                    tran.add(slicedData);

                    // Update token in metadata
                    tran.lock(metaData);
                    metaData.setRawDataToken(rawDataToken);
                    tran.replace(metaData);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Successfully converted PersistableRawData to PersistableSlicedData.");
                }
            }
        }
        else
        {
            PersistenceException pe = new PersistenceException("No raw data Token found for this Persistable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "No raw data Token found for this Persistable!", pe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "updateDataOnly");
            throw pe;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "updateDataOnly");
    }

    /**
     * Only update the persistent copy of the meta data associated with this Persistable.
     * 
     * @param tran The ObjectManager transaction under which the update of the data is carried out.
     * 
     * @exception ObjectManagerException
     */
    public void updateMetaDataOnly(Transaction tran) throws PersistenceException, ObjectManagerException
    {
        // Defect 585163 
        // Can now delegate down onto the other implementation 
        // passing 'this' as it will no longer retrieve the 
        // lock id from the metadata object on a call to getLockID()
        updateMetaDataOnly(tran, this);
    }

    /**
     * Only update the persistent copy of the meta data associated with this Persistable.
     * This variant is for a cached persistable in which the lock ID has been cached by the task.
     * 
     * @param tran The ObjectManager transaction under which the update of the data is carried out.
     * 
     * @exception ObjectManagerException
     */
    public void updateMetaDataOnly(Transaction tran, Persistable persistable) throws PersistenceException, ObjectManagerException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "updateMetaDataOnly", new Object[] { "Tran=" + tran, "Persistable=" + persistable });

        // Defect 585163
        // Get the meta data object to work with. This may involve 
        // pulling it off disk if it has fallen out of memory.
        PersistableMetaData metaData = getMetaData();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "MetaData=" + metaData);

        tran.lock(metaData);

        // Update the MetaData with the cached values
        metaData.setLockID(persistable.getLockID());
        metaData.setRedeliveredCount(persistable.getRedeliveredCount());

        tran.replace(metaData);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "updateMetaDataOnly", "MetaData=" + metaData);
    }

    public void removeFromStore(Transaction tran) throws PersistenceException, ObjectManagerException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeFromStore", "Tran=" + tran);

        // Defect 585163
        // Get the meta data object to work with. This may involve 
        // pulling it off disk if it has fallen out of memory.
        PersistableMetaData metaData = getMetaData();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "MetaData=" + metaData);

        // If we have stream lists then we need to delete
        // them from the object store.
        if ((_tupleType == TupleTypeEnum.ITEM_STREAM) || (_tupleType == TupleTypeEnum.REFERENCE_STREAM) || (_tupleType == TupleTypeEnum.ROOT))
        {
            // Defect 297550
            // If we aren't STORE_MAYBE then we need to delete the 
            // lists that we store our children in. If we are 
            // STORE_MAYBE then we don't have any.
            if (_storageStrategy != AbstractItem.STORE_MAYBE)
            {
                tran.delete(metaData.getStreamListToken().getManagedObject());
                tran.delete(metaData.getItemListToken().getManagedObject());
            }
        }

        // If we have a data token then we need to make 
        // sure that is deleted in the transaction aswell.
        Token rawDataToken = metaData.getRawDataToken();
        if (rawDataToken != null)
        {
            // FSIB0112b.ms.2
            // This object could be one of two types but we 
            // can avoid a cast as we don't need to know in 
            // order to delete it.
            ManagedObject rawData = rawDataToken.getManagedObject();

            tran.delete(rawData);
        }

        // Defect 441208
        // If we are STORE_MAYBE then we won't have added ourselves
        // to any persistent lists at add time so we do not need to 
        // do any cleanup now.
        if ((_containingStream != null) && (_storageStrategy != AbstractItem.STORE_MAYBE))
        {
            // If we aren't the root then we need to keep our
            // parents pointers up to date.
            PersistableImpl parentStream = (PersistableImpl) _containingStream;

            // Defect 297550
            if (parentStream.getStorageStrategy() != AbstractItem.STORE_MAYBE)
            {
                // Use the Entry to delete directly from the list
                // without the need to traverse it. 
                ((List.Entry) metaData.getItemListEntryToken().getManagedObject()).remove(tran);

                // Then, if this is a stream, we need to delete this item from its parent's list of streams
                if (_tupleType == TupleTypeEnum.ITEM_STREAM || _tupleType == TupleTypeEnum.REFERENCE_STREAM)
                {
                    // Use the Entry to delete directly from the list
                    // without the need to traverse it. 
                    ((List.Entry) metaData.getStreamListEntryToken().getManagedObject()).remove(tran);
                }
            }
        }

        // Finally, we need to delete the item in the object store
        tran.delete(metaData);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeFromStore");
    }

    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer("Persistable[metaDataToken: ");

        buffer.append(_metaDataToken);
        buffer.append(", link: ");
        buffer.append(_link);
        buffer.append(", uniqueID: ");
        buffer.append(_uniqueID);
        buffer.append(", streamID: ");
        buffer.append(_streamID);
        buffer.append(", lockID: ");
        buffer.append(_lockID);
        buffer.append(", referredID: ");
        buffer.append(_referredID);
        buffer.append(", sequence: ");
        buffer.append(_sequence);
        buffer.append(", expiryTime: ");
        buffer.append(_expiryTime);
        buffer.append(", storageStrategy: ");
        buffer.append(_storageStrategy);
        buffer.append(", priority: ");
        buffer.append(_priority);
        buffer.append(", persistentSize: ");
        buffer.append(_persistentSize);
        buffer.append(", canExpireSilently: ");
        buffer.append(_canExpireSilently);
        buffer.append(", type: ");
        buffer.append(_tupleType);
        buffer.append(", className: ");
        buffer.append(_className);
        buffer.append(", persistentTranID: ");
        buffer.append(_persistentTranID);
        buffer.append(", logicallyDeleted: ");
        buffer.append(_logicallyDeleted);
        buffer.append(", wasSpillingAtAddition: ");
        buffer.append(_wasSpillingAtAddition);
        buffer.append(", inMemoryByteSize: ");
        buffer.append(_inMemoryByteSize);
        buffer.append(", redeliveredCount: ");
        buffer.append(_redeliveredCount);
        buffer.append(", deliveryDelayTime: ");
        buffer.append(_deliveryDelayTime);
        buffer.append("]");

        return buffer.toString();
    }

    /*************************************************************************/
    /* Persistable Interface implementation */
    /*************************************************************************/

    // Defect 463642 
    // As we are now using spill limits to determine if we pass down to
    // the dispatchers we need to correctly determine if we require 
    // persistence.
    @Override
    public boolean requiresPersistence()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "requiresPersistence");

        boolean requiresPersistence = false;

        // Defect 585163
        if ((_storageStrategy == AbstractItem.STORE_ALWAYS) || (_storageStrategy == AbstractItem.STORE_EVENTUALLY))
        {
            requiresPersistence = true;
        }
        else if (AbstractItem.STORE_MAYBE == _storageStrategy)
        {
            requiresPersistence = _wasSpillingAtAddition;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "requiresPersistence", Boolean.valueOf(requiresPersistence));
        return requiresPersistence;
    }

    /**
     * This method is used by the task layer to get hold of data from the
     * cache layer before it is hardened to disk. It should therefore return
     * the data from the Item and not that from the ManagedObject.
     * 
     * @return
     * @throws SevereMessageStoreException
     */
    @Override
    public java.util.List<DataSlice> getData() throws PersistentDataEncodingException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getData");

        java.util.List<DataSlice> retval = null;

        synchronized (this)
        {
            if (_link != null)
            {
                retval = _link.getMemberData();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getData", "return=" + retval);
        return retval;
    }

    @Override
    public Persistable createPersistable(long uniqueID, TupleTypeEnum tupleType)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createPersistable", new Object[] { "UniqueID=" + uniqueID, "Type=" + tupleType });

        PersistableImpl persistable = new PersistableImpl(uniqueID, this, tupleType);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createPersistable", "return=" + persistable);
        return persistable;
    }

    @Override
    public synchronized void setAbstractItemLink(AbstractItemLink link)
    {
        _link = link;
    }

    @Override
    public void setContainingStream(Persistable containingStream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setContainingStream", "ContainingStream=" + containingStream);

        _containingStream = containingStream;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setContainingStream");
    }

    @Override
    public Persistable getContainingStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getContainingStream");
            SibTr.exit(this, tc, "getContainingStream", "return=" + _containingStream);
        }
        return _containingStream;
    }

    @Override
    public void setCanExpireSilently(boolean canExpireSilently)
    {
        _canExpireSilently = canExpireSilently;
    }

    @Override
    public boolean getCanExpireSilently()
    {
        return _canExpireSilently;
    }

    @Override
    public long getContainingStreamId()
    {
        return _streamID;
    }

    @Override
    public void setExpiryTime(long expiryTime)
    {
        _expiryTime = expiryTime;
    }

    @Override
    public long getExpiryTime()
    {
        return _expiryTime;
    }

    @Override
    public void setItemClassName(String className)
    {
        _className = className;
    }

    @Override
    public String getItemClassName()
    {
        return _className;
    }

    @Override
    public void setLockID(long lockID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setLockID", "LockID=" + lockID);

        _lockID = lockID;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setLockID");
    }

    @Override
    public long getLockID()
    {
        return _lockID;
    }

    @Override
    public void setLogicallyDeleted(boolean logicallyDeleted)
    {
        _logicallyDeleted = logicallyDeleted;
    }

    @Override
    public boolean isLogicallyDeleted()
    {
        return _logicallyDeleted;
    }

    @Override
    public void setPersistentSize(int persistentSize)
    {
        _persistentSize = persistentSize;
    }

    @Override
    public int getPersistentSize()
    {
        return _persistentSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setInMemoryByteSize(int)
     */
    @Override
    public void setInMemoryByteSize(int byteSize)
    {
        _inMemoryByteSize = byteSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getInMemoryByteSize()
     */
    @Override
    public int getInMemoryByteSize()
    {
        return _inMemoryByteSize;
    }

    @Override
    public void setPersistentTranId(PersistentTranId xid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setPersistentTranId", "XID=" + xid);

        _persistentTranID = xid;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setPersistentTranId");
    }

    @Override
    public PersistentTranId getPersistentTranId()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getPersistentTranId");
            SibTr.exit(this, tc, "getPersistentTranId", "return=" + _persistentTranID);
        }
        return _persistentTranID;
    }

    @Override
    public void setPriority(int priority)
    {
        _priority = priority;
    }

    @Override
    public int getPriority()
    {
        return _priority;
    }

    @Override
    public void setRedeliveredCount(int redeliveredCount)
    {
        _redeliveredCount = redeliveredCount;
    }

    @Override
    public int getRedeliveredCount()
    {
        return _redeliveredCount;
    }

    @Override
    public void setReferredID(long referredID)
    {
        _referredID = referredID;
    }

    @Override
    public long getReferredID()
    {
        return _referredID;
    }

    @Override
    public void setSequence(long sequence)
    {
        _sequence = sequence;
    }

    @Override
    public long getSequence()
    {
        return _sequence;
    }

    @Override
    public void setStorageStrategy(int storageStrategy)
    {
        _storageStrategy = storageStrategy;
    }

    @Override
    public int getStorageStrategy()
    {
        return _storageStrategy;
    }

    @Override
    public TupleTypeEnum getTupleType()
    {
        return _tupleType;
    }

    @Override
    public long getUniqueId()
    {
        return _uniqueID;
    }

    // Defect 463642
    // As we are now making use of the spill limits within the filestore
    // we need to correctly track the value of this flag.
    @Override
    public void setWasSpillingAtAddition(boolean wasSpillingAtAddition)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setWasSpillingAtAddition", Boolean.valueOf(wasSpillingAtAddition));

        _wasSpillingAtAddition = wasSpillingAtAddition;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setWasSpillingAtAddition");
    }

    @Override
    public void xmlWrite(FormattedWriter writer) throws IOException
    {
        writer.newLine();
        writer.taggedValue(XML_CLASS, getItemClassName());
        writer.newLine();
        writer.taggedValue(XML_PRIORITY, getPriority());
        if (isLogicallyDeleted())
        {
            writer.newLine();
            writer.emptyTag(XML_LOGICALLY_DELETED);
        }
        if (getCanExpireSilently())
        {
            writer.newLine();
            writer.emptyTag(XML_CAN_EXPIRE_SILENTLY);
        }

        writer.newLine();
        switch (getStorageStrategy())
        {
            case AbstractItem.STORE_ALWAYS:
                writer.taggedValue(XML_STORAGE_STRATEGY, XML_STORE_ALWAYS);
                break;
            case AbstractItem.STORE_MAYBE:
                writer.taggedValue(XML_STORAGE_STRATEGY, XML_STORE_MAYBE);
                break;
            case AbstractItem.STORE_EVENTUALLY:
                writer.taggedValue(XML_STORAGE_STRATEGY, XML_STORE_EVENTUALLY);
                break;
            case AbstractItem.STORE_NEVER:
                writer.taggedValue(XML_STORAGE_STRATEGY, XML_STORE_NEVER);
                break;
            default:
                writer.taggedValue(XML_STORAGE_STRATEGY, getStorageStrategy());
                break;
        }

        writer.newLine();
        writer.taggedValue(XML_EXPIRY_TIME, getExpiryTime());

        writer.newLine();
        writer.taggedValue(XML_SEQUENCE, getSequence());

        writer.newLine();
        // Defect 571947
        PersistentTranId tranId = getPersistentTranId();
        if (tranId != null)
        {
            writer.taggedValue(XML_TRANID, tranId.toTMString());
        }
        else
        {
            writer.taggedValue(XML_TRANID, "");
        }

        if (AbstractItem.NO_LOCK_ID != getLockID())
        {
            writer.newLine();
            writer.taggedValue(XML_LOCKID, getLockID());
        }

        long referredID = getReferredID();
        if (AbstractItem.NO_ID != referredID)
        {
            writer.newLine();
            writer.taggedValue(XML_REFERRED_ID, referredID);
        }
    }

    /*************************************************************************/
    /* Persistable Interface implementation */
    /*************************************************************************/

    /*************************************************************************/
    /* Tuple Interface implementation */
    /*************************************************************************/

    @Override
    public synchronized void persistableOperationBegun() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistableOperationBegun");

        _persistableOperationBegunCounter++;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            SibTr.event(this, tc, "persistableOperationBegunCounter: " + _persistableOperationBegunCounter);

        if (_link != null)
        {
            if (_persistableOperationBegunCounter - _persistableOperationCompletedCounter == 1)
            {
                _link.persistentRepresentationIsUnstable();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "persistableOperationBegun");
    }

    @Override
    public synchronized void persistableOperationCompleted() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistableOperationCompleted");

        _persistableOperationCompletedCounter++;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            SibTr.event(this, tc, "persistableOperationCompletedCounter: " + _persistableOperationCompletedCounter);

        if (_link != null)
        {
            if (_persistableOperationBegunCounter == _persistableOperationCompletedCounter)
            {
                _link.persistentRepresentationIsStable();
            }
            else if (_persistableOperationBegunCounter < _persistableOperationCompletedCounter)
            {
                throw new IllegalStateException(nls.getFormattedMessage("INVALID_PERSISTABLE_STATE_SIMS1527",
                                                                        new Object[] { Integer.valueOf(_persistableOperationBegunCounter),
                                                                                      Integer.valueOf(_persistableOperationCompletedCounter) },
                                                                        null));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "persistableOperationCompleted");
    }

    @Override
    public synchronized void persistableOperationCancelled()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistableOperationCancelled");

        _persistableOperationBegunCounter--;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            SibTr.event(this, tc, "persistableOperationBegunCounter: " + _persistableOperationBegunCounter);

        // Although we're cancelling an operation, and this operation may be the first,
        // the persistence layer no longer has a need for the link to be held in memory.
        // Therefore, we declare the link stable regardless of the value of the
        // counter of the number of operations begun. This effectively means that if
        // the cancellation results in no persistable operations being completed, there
        // was never any need for a persistent representation and one will never be required.

        // Defect 359654
        // The above comment is incorrect for the filestore implementation as I believe 
        // it is talking about the possibility of a dispatched items persistence being 
        // cancelled due to a corresponding get also being dispatched. In the filestore
        // there is another case in the SpillDispatcher where we could be cancelled but 
        // it is not safe to release the reference from memory. If the filestore is full
        // and a spill dispatch is cancelled then we cannot throw the reference away as
        // when the item attempts to restore from the message store it will not have a 
        // copy on disc to retrieve it's data from. In this case we will not be able to
        // declare ourselves stable and must stay in memory until the item is removed via 
        // a get request.
        if (_persistableOperationBegunCounter < _persistableOperationCompletedCounter)
        {
            throw new IllegalStateException(nls.getFormattedMessage("INVALID_PERSISTABLE_STATE_SIMS1527",
                                                                    new Object[] { Integer.valueOf(_persistableOperationBegunCounter),
                                                                                  Integer.valueOf(_persistableOperationCompletedCounter) },
                                                                    null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "persistableOperationCancelled");
    }

    /*
     * @see com.ibm.ws.sib.msgstore.persistence.impl.Tuple#persistableRepresentationWasCreated()
     * Queries whether a persistent representation of the object was created.
     * This can be used to determine whether there's a persistent representation
     * to be deleted.
     * <p>This method returns true when {@link #persistableOperationCompleted()}
     * has been called at least once.
     */
    @Override
    public boolean persistableRepresentationWasCreated()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistableRepresentationWasCreated");

        boolean result = (_persistableOperationCompletedCounter > 0);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "persistableRepresentationWasCreated", "return=" + result);
        return result;
    }

    /**
     * Queries whether an operation to change the persistent representation
     * of the object is in progress. This can be used to determine whether
     * there's a risk that the persistent representation is being updated
     * asynchronously.
     * <p>When {@link #persistableOperationBegun} has been called at least once and the number of calls to {@link #persistableOperationBegun()} matches the number of calls to
     * {@link #persistableOperationCompleted()}, the current persistent representation
     * of the object is consistent with the object's current data.
     * <p>This method returns true when the number of calls to {@link #persistableOperationBegun()} does not equal the number of calls to {@link #persistableOperationCompleted()}.
     */
    public synchronized boolean persistableRepresentationIsUnstable()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistableRepresentationIsUnstable");

        boolean result = (_persistableOperationBegunCounter != _persistableOperationCompletedCounter);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "persistableRepresentationIsUnstable", "return=" + result);
        return result;
    }

    // Defect 496154
    @Override
    public synchronized int persistableOperationsOutstanding()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistableOperationsOutstanding");

        int result = _persistableOperationBegunCounter - _persistableOperationCompletedCounter;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "persistableOperationsOutstanding", Integer.valueOf(result));
        return result;
    }

    @Override
    public void setPermanentTableId(int permanentTableId) {/* NO-OP */}

    @Override
    public int getPermanentTableId() {
        return 0;
    }

    @Override
    public void setTemporaryTableId(int temporaryTableId) {/* NO-OP */}

    @Override
    public int getTemporaryTableId() {
        return 0;
    }

    @Override
    public void setItemClassId(int itemClassId) {/* NO-OP */}

    @Override
    public int getItemClassId() {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setDeliveryDelayTime(long)
     */
    @Override
    public void setDeliveryDelayTime(long deliveryDelayTime) {
        this._deliveryDelayTime = deliveryDelayTime;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getDeliveryDelayTime()
     */
    @Override
    public long getDeliveryDelayTime() {
        return this._deliveryDelayTime;
    }

	@Override
	public boolean getDeliveryDelayTimeIsSuspect() {
		return _deliveryDelayTimeIsSuspect;
	}

    /*************************************************************************/
    /* Tuple Interface implementation */
    /*************************************************************************/
}
