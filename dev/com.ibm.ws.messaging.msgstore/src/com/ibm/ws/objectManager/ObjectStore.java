package com.ibm.ws.objectManager;

import com.ibm.ws.objectManager.utils.Printable;
import com.ibm.ws.objectManager.utils.Trace;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * <p>ObjectStores store ManagedObjects some time other than
 * the lifetime of the virtual machine. ManagedObjects are
 * represented by a Token which is used to retrieve them from
 * the store. There can only be one version of each Token and
 * one version of the ManagedObject in virtual memory. This
 * ensures that all references to a Token or ManagedObject
 * actually refer to the same Object.
 * 
 * @version @(#) 1/25/13
 * @author IBM Corporation
 */

public abstract class ObjectStore
                implements Printable
{
    private static final Class cclass = ObjectStore.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_STORE);

    //Reserve a well known identifier for ObjectManagerState in case we decide to store it in here.
    protected static final Long objectManagerStateIdentifier = new Long(0);
    // Reserve a well known identifier for namedObjecTree in case we decide to store it in here.
    protected static final Long namedObjectTreeIdentifier = new Long(1);
    // Reserve sequence numbers 2-100 for future use.
    protected static final long initialSequenceNumber = 100;

    // Define the storageStrategy.
    /**
     * <code>STRATEGY_KEEP_ALWAYS</code>
     * Always harden to disk, never clear the store. Implies chanmges will be logged.
     */
    public static final int STRATEGY_KEEP_ALWAYS = 0;
    /**
     * <code>STRATEGY_KEEP_UNTIL_NEXT_OPEN</code>
     * On restart empty the store. Implies changes will not be logged.
     */
    public static final int STRATEGY_KEEP_UNTIL_NEXT_OPEN = 1;
    /**
     * <code>STRATEGY_SAVE_ONLY_ON_SHUTDOWN</code>
     * Do not flush the store until normal shutdown of the ObjectManager.
     * If a failure occurs before normal shutdown is completed no
     * changes are saved. If a failure occurs during shutdown some stores may be
     * updated and others not updated, either all changes to a single store are
     * saved or none of them are. It is unwise to involve ManagedObjects in this type
     * of store with a Global Transaction. If the transaction is in prepared state
     * normal shutdown occurs then ManagedObjects Added under the scope of
     * the transaction may have been added but other changes will not.
     * Implies changes will not be logged.
     */
    public static final int STRATEGY_SAVE_ONLY_ON_SHUTDOWN = 2;

    public static final String[] strategyNames = { "KEEP_ALWAYS",
                                                  "KEEP_UNTIL_NEXT_OPEN",
                                                  "SAVE_ONLY_ON_SHUTDOWN" };

    // Reserved value for the storeIdentifier until it has not been set.
    protected static final int IDENTIFIER_NOT_SET = -1;

    public static final boolean gatherStatistics = true; // Built for statistics if true.

    private transient boolean _storeFullWaitForCheckPoint = false;

    /**
     * Initialise the transient state of the ObjectStore.
     * 
     * @param objectManagerState with which the ObjectStore is registered.
     * @throws ObjectManagerException
     */
    protected abstract void open(ObjectManagerState objectManagerState)
                    throws ObjectManagerException;

    /**
     * Retrieve and reinstantiate an object in the store.
     * 
     * @param storedObject representing the object to be retrieved.
     * @return ManagedObject the object from the store, or null if there is none.
     * @throws ObjectManagerException
     */
    protected abstract ManagedObject get(Token storedObject)
                    throws ObjectManagerException;

    /**
     * References a ManagedObject at the same location in the store.
     * 
     * @param likeObject prototype Token for the definitive Token in the store.
     * @return Token in the store.
     */
    protected abstract Token like(Token likeObject);

    /**
     * Allocate a Token for the ManagedObject.
     * 
     * @param objectToStore to be allocated a token.
     * @return Token allocated.
     * @throws ObjectManagerException
     */
    public abstract Token allocate(ManagedObject objectToStore)
                    throws ObjectManagerException;

    /**
     * Reserve or release space for a serialized ManagedObject when it is eventually added and written to the store.
     * Used to determine if the store has sufficient space to fulfil its obligations to write ManagedObjects.
     * 
     * @param deltaSize change in size of the serialized managedObject since last reserve call.
     *            may be negative or positive.
     * @param paced true if the call may be blocked while the store completes its backlog of work.
     * @throws ObjectManagerException
     * @throws ObjectStoreFullException if the store is too full to honour the request.
     * 
     */
    protected abstract void reserve(int deltaSize, boolean paced)
                    throws ObjectManagerException;

    /**
     * Adds an Object to the store, but may not write it until flush() is called.
     * This operation may be repeated, with the effect that the object is replaced
     * in the store.
     * 
     * @param managedObject to add.
     * @param requiresCurrentCheckpoint true if the managed object must be updated as part of the current checkpoint.
     * @throws ObjectManagerException
     */
    protected abstract void add(ManagedObject managedObject,
                                boolean requiresCurrentCheckpoint)
                    throws ObjectManagerException;

    /**
     * Permanently deletes an object in the store, used by the Transaction after commit of a transaction
     * to delete objects from the object store.
     * 
     * @param objectToRemove representing the ManagedObject to be removed from the store.
     * @param requiresCurrentCheckpoint true if the managed object must be updated as part of the current checkpoint.
     * @throws ObjectManagerException
     */
    protected abstract void remove(Token objectToRemove,
                                   boolean requiresCurrentCheckpoint)
                    throws ObjectManagerException;

    /**
     * Removes knowledge of the ObjectStore from the ObjectManager.
     * Does not destroy the contents of the store, it can be re attached to the,
     * ObjectManager, or another ObjectManager by using the constructor.
     * 
     * @throws ObjectManagerException
     */
    // TODO reregistering using the constructor assigns a new ObjectStore Identifier
    // so sadly this does not currently work. Need a utility to rewrite tokens 
    // to contain the new Identifier.
    // Need also to check the Object manager identity.
    public abstract void deRegister()
                    throws ObjectManagerException;

    /**
     * Empty the contents of the ObjectStore.
     * 
     * @throws ObjectManagerException
     */
    protected abstract void clear()
                    throws ObjectManagerException;

    /**
     * Prohibits further operations on the ObjectStore.
     * 
     * @throws ObjectManagerException
     */
    protected abstract void close()
                    throws ObjectManagerException;

    /**
     * Writes buffered output to hardened storage.
     * 
     * @throws ObjectManagerException
     */
    protected abstract void flush()
                    throws ObjectManagerException;

    /**
     * The identifier of the ObjectStore, unique within this ObjectManager.
     * 
     * @return int the objectStore identifier
     */
    protected abstract int getIdentifier();

    /**
     * Set the identifier of the Object Store, unique within this ObjectManager.
     * 
     * @param identifier the objectStore identifier
     * @throws ObjectManagerException
     */
    protected abstract void setIdentifier(int identifier)
                    throws ObjectManagerException;

    /**
     * The name of the ObjectStore.
     * 
     * @return String the objectStore name
     */
    public abstract String getName();

    /**
     * @param newName The new logical name by which the store will be known to the ObjectManager.
     *            If the store has a physical location, like a file, it is not changed by this call.
     *            The store remains open and any physical contents remain unchanged.
     *            When the ObjectManager next restarts after successful completion of this call, the new
     *            logical name will be used. If the store has a physical location then one of the
     *            following must happen after shutdown and before startup of the ObjectManager.
     *            <ol>
     *            <li> Move the physical file to a new location to match the new logical name.
     *            <li> Start the object manager with an entry in the objectStoreLocations Map passed
     *            to the constructor which matches the new name and maps it to the physical location.
     *            </ol>
     * 
     *            If this method fails to complete, because the objectManager crashes, the next start
     *            of the ObjectManager may expect to find the store using either the old or the new
     *            logical name. If an exception is thrown no change is made.
     * 
     * @throws ObjectManagerException
     */
    public abstract void setName(String newName)
                    throws ObjectManagerException;

    /**
     * Set the new logical name in no non transient state so that the next checkpoint will
     * store this name.
     * 
     * @param newName to save the new logical in the ObjectStore non transient state.
     */
    abstract void setLogicalName(String newName);

    /**
     * @return ObjectManagerState the objectManagerState with which thes store is registered.
     */
    protected abstract ObjectManagerState getObjectManagerState();

    /**
     * @return Returns the storeStrategy.
     */
    public abstract int getStoreStrategy();

    /**
     * Whether the object store recovered at ObjectManager warm start.
     * Transaction logging is only necessary if the ManagesObject is stored in an ObjectStore
     * that is persistent.
     * 
     * @return true if the ObjectStore is recovered on a warm start of the ObjectManager.
     *         false if the ObjectStore is not restored intact on restart.
     */
    public abstract boolean getPersistence();

    /**
     * Whether the object store contains restart data, ObjectManagerState and the nameObjectsTree.
     * 
     * @return true if the ObjectStore contains restart data.
     *         false if the ObjectStore does not contain restart data.
     */
    public abstract boolean getContainsRestartData();

    // TODO Could allow set of ContainsRestartData.

    /**
     * Whether the object store uses the serialized form of the ManagedObjects its stores.
     * 
     * @return true if the ObjectStore uses the serialized form of managedObjects its stores.
     */
    public abstract boolean getUsesSerializedForm();

    /**
     * Allows users of the store to reserve space for additions to the store. By default
     * ManagedObject does this automatically. There is no need to reserve space for deletions
     * because the store retains enough space after a flush to guarantee that all of its
     * contents can be deleted.
     * 
     * @return int the bytes required over and above the serialized ManagedObject data
     *         to perform an addition to the store. This also includes enough space to guarantee
     *         the eventual removal of the ManagedObject.
     */
    public abstract int getAddSpaceOverhead();

    /**
     * @return Set of tokens in the store.
     */
    public abstract Set tokens();

    /**
     * Builds a set of properties containing the current statistics.
     * 
     * @return java.util.Map the statistics.
     * @throws ObjectManagerException
     */
    public abstract java.util.Map captureStatistics()
                    throws ObjectManagerException;

    /**
     * Validates that the store space is not corrupt.
     * 
     * @param printStream to be written to.
     * @return boolean true if the store is not corrupt.
     * @throws ObjectManagerException
     */
    public boolean validate(java.io.PrintStream printStream)
                    throws ObjectManagerException
    {
        return true;
    }

    /**
     * Print a dump of the state.
     * 
     * @param printWriter to be written to.
     */
    public abstract void print(java.io.PrintWriter printWriter);

    /**
     * Get the storeFullWaitForCheckPoint property
     * 
     * @return true == wait for checkpoint, false == simply request a checkpoint and exception with StoreFull
     */
    public final boolean getStoreFullWaitForCheckPoint() {
        return _storeFullWaitForCheckPoint;
    }

    /**
     * Set the storeFullWaitForCheckPoint property.
     * 
     * @param storeFullWaitForCheckPoint, true == wait for checkpoint, false == simply request a checkpoint and
     *            exception with StoreFull
     */
    public final void setStoreFullWaitForCheckPoint(
                                                    boolean storeFullWaitForCheckPoint) {
        _storeFullWaitForCheckPoint = storeFullWaitForCheckPoint;
    }

} // class ObjectStore.