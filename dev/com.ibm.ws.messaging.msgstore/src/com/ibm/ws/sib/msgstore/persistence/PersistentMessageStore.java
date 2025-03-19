/* ==============================================================================
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ==============================================================================
 */
package com.ibm.ws.sib.msgstore.persistence;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.ibm.ws.sib.msgstore.Configuration;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.UniqueKeyGenerator;
import com.ibm.ws.sib.msgstore.transactions.impl.XidManager;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

/** 
 * This interface is used to retrieve items from the persistent store.
 * Writing to the persistent store is done via the
 * {@link com.ibm.ws.sib.msgstore.transactions.PersistenceManager} interface.
 *
 * @see com.ibm.ws.sib.msgstore.transactions.PersistenceManager
 */
public interface PersistentMessageStore 
{
    /**
     * Returns the serialized binary data associated with a {@link Persistable}
     * item.
     * 
     * @param item the {@link Persistable}
     * @return a byte array containing the binary data for the {@link Persistable}
     * @throws PersistenceException
     */
    // FSIB0112b.ms.1
    public List<DataSlice> readDataOnly(Persistable item) throws PersistenceException;

    /**
     * Returns the items and item references contain in a stream.
     * 
     * @param containingStream the stream to search.
     * @return a {@link java.util.List} containing {@link Persistable} objects
     * @throws PersistenceException
     */
    public List readNonStreamItems(Persistable containingStream) throws PersistenceException;

    /**
     * Read all item streams and reference streams.
     * 
     * @return a {@link java.util.List} containing {@link Persistable} objects
     * @throws PersistenceException
     */
    public List readAllStreams() throws PersistenceException;

    /**
     * Identify all streams that contain items that can expire
     *
     * @return a {@link java.util.Set} containing {@link Long} objects
     * @throws PersistenceException
     */
    public Set identifyStreamsWithExpirableItems() throws PersistenceException;

    /**
     * Read all in-doubt transaction ids.
     * 
     * @return a {@link java.util.List} containing
     * {@link com.ibm.ws.sib.msgstore.transactions.PersistentTranId} objects
     * @throws PersistenceException
     */
    public List readIndoubtXIDs() throws PersistenceException;

    /**
     * Identify all streams that contain in-doubt items
     * 
     * @return a {@link java.util.Set} containing {@link Long} objects
     * @throws PersistenceException
     */
    public Set identifyStreamsWithIndoubtItems() throws PersistenceException;

    /**
     * Return the root persistable.
     * 
     * @return a {@link Persistable}
     * @throws PersistenceException
     */
    public Persistable readRootPersistable() throws PersistenceException, SevereMessageStoreException;

    /**
     * Initialises the Persistent Message Store.
     * 
     * @param msi Reference to the owning MessageStoreImpl which offers a variety of utility methods
     * @param xidManager The transaction layer's XidManager
     * @param configuration The configuration for the persistence layer 
     */
    public void initialize(MessageStoreImpl msi, XidManager xidManager, Configuration configuration);

    /**
     * Starts the Persistent Message Store.
     */
    public void start() throws PersistenceException;

    /**
     * Stops the Persistent Message Store.
     * 
     * @param mode specifies the type of stop operation which is to be performed.
     */
    public void stop(int mode, Throwable reason);

    /**
     * Creates a UniqueKeyGenerator object with a persistent range counter.
     * 
     * @param name   The unique name of the generator
     * @param range  The batch size for allocation of unique id's
     * 
     * @return A new UniqueKeyGenerator
     */
    public UniqueKeyGenerator getUniqueKeyGenerator(String name, int range);

    /** Request that the receiver prints its xml representation
     * (recursively) onto writer.
     * @param writer
     * @throws IOException
     */
    public abstract void xmlWriteOn(FormattedWriter writer) throws IOException;
}
