package com.ibm.ws.objectManager;

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

import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * <p>ObjectStore a generic state of objects that may persist for some time other than
 * the lifetime of the virtual machine. ManagedObjects are represented by a Token which is used
 * to retrieve them from the store. There can only be one version of each Token in virtual
 * memory, this ensures that there is only one copy of each ManagedObject.
 * 
 * @version @(#) 1/25/13
 * @author IBM Corporation
 */
public abstract class AbstractObjectStore
                extends ObjectStore
                implements java.io.Serializable
{
    private static final Class cclass = AbstractObjectStore.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_STORE);

    private static final long serialVersionUID = 9004399024189054153L;

    // Concurrent threads in ConcurrentHashMap.   
    protected static final int concurrency = 64;

    // Non transient state of the ObjectStore persisted by the ObjectManager as 
    // part of ObjectManagerState.
    protected String storeName;
    // Identifies the Object Store within the ObjectManager.
    protected int objectStoreIdentifier = IDENTIFIER_NOT_SET;
    // Keep the store strategy.
    protected int storeStrategy;

    // Monotonically increasing number.
    protected transient long sequenceNumber;
    private transient SequenceNumberLock sequenceNumberLock;

    private class SequenceNumberLock {}

    // Set to false if the store is too full to allow allocation of new ManagedObjects.
    protected transient boolean allocationAllowed = true;

    // A table of the primary copies of the Queue Manager objects currently known
    // to be in virtual machine memory, indexed by storedObjectIdentifier.
    // We can only drop them from this table if no other reference to them is made bacuse we might be asked for the
    // definitive copy of the token. If they are dropped from this table they are lost.
    transient WeakValueConcurrentHashMap inMemoryTokens;

    transient ObjectManagerState objectManagerState;
    // Store name, uniquely identifies the ObjectStore within the ObjectManager.

    // Derived from the storeStrategy.
    transient boolean persistent;
    transient boolean containsRestartData;
    transient boolean usesSerializedForm;

    // The space needed to add a new ManagedObject into the store, and ultimately remove it.
    // This is in addition to the space needed for the serialized ManagedObject itself.
    transient int addSpaceOverhead = 0;

    /**
     * Constructor.
     * 
     * @param storeName the logical name that identifies the ObjectStore to the ObjectManager.
     * @param objectManager The ObjectManager that manages this store.
     *            Assumes STRATEGY_KEEP_UNTIL_NEXT_OPEN.
     * 
     *            ObjectStores cannot be created as part of a transaction
     *            becase they have to be stored in an object store themselves.
     * @throws ObjectManagerException
     */
    AbstractObjectStore(String storeName,
                        ObjectManager objectManager)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { storeName, objectManager });

        // Check validity of the storeName.
        if (storeName == null) {
            throw new InvalidObjectStoreNameException(this
                                                      , storeName);
        } // if (storeName == null).

        this.storeName = storeName; // Save the name for any future use.
        this.objectManagerState = objectManager.objectManagerState;
        storeStrategy = STRATEGY_KEEP_UNTIL_NEXT_OPEN;

        // Register this ObjectStore with the ObjectManager, 
        // after all constructor processing is complete, this causes the store to be opened.
        objectManager.objectManagerState.registerObjectStore(this);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // AbstractObjectStore().

    /**
     * Constructor, used during ObjectManagerState construction.
     * 
     * @param objectManagerState
     *            The ObjectManagerState that manages this store.
     * @throws ObjectManagerException
     */
    protected AbstractObjectStore(ObjectManagerState objectManagerState)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { objectManagerState });

        this.storeName = "ObjectManagerState.default";
        this.objectManagerState = objectManagerState;
        this.storeStrategy = STRATEGY_KEEP_ALWAYS;
        persistent = true;
        // We do declare that we do not contain restart data even though the ObjectManager stores 
        // it in here bacause we are unable to give it back at restart.
        containsRestartData = false;

        // Do not register this ObjectStore with the ObjectManager, it is not ready yet. 
        // and we don't want this object store saved as part of the checkpoint.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // AbstractObjectStore().

    /**
     * Constructor
     * 
     * @param storeName Identifies the ObjecStore and the file directory.
     * @param objectManager The ObjectManager that manages this store.
     * @param storeStrategy one of STRATEGY_XXX:
     * @throws ObjectManagerException
     */
    public AbstractObjectStore(String storeName,
                               ObjectManager objectManager,
                               int storeStrategy)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { storeName, objectManager, new Integer(storeStrategy) });

        // Check validity of the storeName.
        if (storeName == null) {
            throw new InvalidObjectStoreNameException(this,
                                                      storeName);
        } // if (storeName == null).

        this.storeName = storeName; // Save the name for any future use.
        this.objectManagerState = objectManager.objectManagerState;
        this.storeStrategy = storeStrategy;

        // Register this ObjectStore with the ObjectManager, 
        // after all construtor processing is complete.
        objectManager.objectManagerState.registerObjectStore(this);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // AbstractObjectStore().

    /**
     * Establish the transient state of this ObjectStore.
     * 
     * @param objectManagerState with which the ObjectStore is registered.
     * @throws ObjectManagerException
     */
    public synchronized void open(ObjectManagerState objectManagerState)
                    throws ObjectManagerException
    {
        final String methodName = "open";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { objectManagerState, new Integer(storeStrategy), strategyNames[storeStrategy], storeName });

        this.objectManagerState = objectManagerState;
        // All ManagedObjects are new, sequence number zero through initialSequenceNumbers are reserved.
        sequenceNumber = initialSequenceNumber;
        sequenceNumberLock = new SequenceNumberLock();
        allocationAllowed = true;

        // Tokens can drop out of memory, once they are unreferenced.
        // Other classes need to hold ManagedObjects in memory as long as they need their transient state,
        // for example Transactions hold included ManagedObjects in memory via a strong 
        // reference from a HashTable.
        // This can be a weakValueHashMap because MemoryObjectStore holds a reference to all of its managed
        // objects which in turn hold a reference to their owningToken.
        inMemoryTokens = new WeakValueConcurrentHashMap(concurrency);

        // Set switches depending on the flush strategy.
        switch (storeStrategy) {
            case (STRATEGY_KEEP_ALWAYS):
                persistent = true;
                containsRestartData = true;
                usesSerializedForm = true;
                break;

            case (STRATEGY_KEEP_UNTIL_NEXT_OPEN):
                persistent = false;
                containsRestartData = false;
                usesSerializedForm = true;
                break;

            case (STRATEGY_SAVE_ONLY_ON_SHUTDOWN):
                persistent = false;
                containsRestartData = true;
                usesSerializedForm = false;
                break;

            default:
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Integer(storeStrategy));
                throw new InvalidStoreStrategyException(this, storeStrategy);
        } // switch (storeStrategy).

        objectManagerState.notifyCallbacks(ObjectManagerEventCallback.objectStoreOpened,
                                           new Object[] { this });

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { new Boolean(persistent), new Boolean(containsRestartData), new Boolean(usesSerializedForm) });
    } // open().

    /**
     * Retrieve and reinstantiate an object in the store.
     * 
     * @param storedObject the Token referencing an Object in this store.
     * @return ManagedObject referenced by the Token.
     * @throws ObjectManagerException
     */
    public abstract ManagedObject get(Token storedObject)
                    throws ObjectManagerException;

    /**
     * References a ManagedObject at the same location in the store.
     * Used to make sure that the caller is refering to the same object as
     * all other users of the ManagedObject.
     * 
     * @param likeToken which is a prototype of the Token we are looking for.
     * @return the definitive version of the Token.
     */
    public Token like(Token likeToken)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "like",
                        new Object[] { likeToken });

        // See if we already have a copy of the object in memory. If so we must reuse
        // it so that all references to the object via the object store point to the
        // same object. Otherwise multiple instances of the object might get created by 
        // multiple Get calls against multiple instances of the the ManagedObject. 
        Token inMemoryToken = (Token) inMemoryTokens.putIfAbsent(new Long(likeToken.storedObjectIdentifier), likeToken);
        if (inMemoryToken == null) {
            // We just established the definitive version of the Token.
            inMemoryToken = likeToken;
            synchronized (sequenceNumberLock) {

                // We should not have to do this check if the application has already allocated the Object
                // but during recovery we will be restablishing the largest sequence number used
                // for non persistenmt ObjectStores.
                if (likeToken.storedObjectIdentifier > sequenceNumber) {
                    sequenceNumber = Math.max(likeToken.storedObjectIdentifier,
                                              sequenceNumber);
                } // if (likeToken.storedObjectIdentifier > sequenceNumber).

            } // synchronized (sequenceNumberLock).
        } // if (inMemoryToken == null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "like",
                       new Object[] { inMemoryToken });
        return inMemoryToken;
    } // like().

    /**
     * Allocate a Token for the ManagedObject.
     * 
     * @param objectToStore to be allocated a token.
     * @return Token allocated.
     * @throws ObjectManagerException
     */

    public Token allocate(ManagedObject objectToStore)
                    throws ObjectManagerException
    {

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "allocate",
                        objectToStore);

        // We cannot store null, it won't serialize and we have no way to manage the transaction state.
        if (objectToStore == null) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "allocate",
                           objectToStore);
            throw new InvalidObjectToStoreException(this,
                                                    objectToStore);
        } // If asked to store null.

        // Is the store full?
        if (!allocationAllowed) {
            // request or wait for a checkpoint and see if that helps
            if (getStoreFullWaitForCheckPoint())
            {
                if (trace.isDebugEnabled())
                    trace.debug(this, cclass, "allocate", "Store is currently full, waiting for checkpoint");
                objectManagerState.waitForCheckpoint(true);
            }
            else
            {
                if (trace.isDebugEnabled())
                    trace.debug(this, cclass, "allocate", "Store is currently full, requesting checkpoint");
                objectManagerState.requestCheckpoint(true);
            }

            if (!allocationAllowed)
            {
                if (trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "allocate"
                                    );

                throw new ObjectStoreFullException(this
                                                   , objectToStore);
            }
        } // If (!allocationAllowed). 

        Token tokenToStore;
        long usableSequenceNumber;

        synchronized (sequenceNumberLock) {
            // Establish the sequence number we will use. 
            usableSequenceNumber = ++sequenceNumber;
        } // synchronized (sequenceNumberLock).

        // Create the container for the object.    
        tokenToStore = new Token(objectToStore, this, usableSequenceNumber);

        // Keep a note of the in memory copy of the object so that we give back the same one
        // whenever it is asked for.
        // If the application never adds the object, we would clean this up when its reference to the Object
        // is lost. 
        Token inMemoryToken = (Token) inMemoryTokens.putIfAbsent(new Long(usableSequenceNumber),
                                                                 tokenToStore);

        // Check that this genuinely a new storedObjectIdentifier.
        if (inMemoryToken != null) {
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            "allocate",
                            new Object[] { "via StoreSequenceException",
                                          inMemoryToken,
                                          new Long(sequenceNumber) });
            StoreSequenceException storeSequenceException = new StoreSequenceException(this,
                                                                                       sequenceNumber,
                                                                                       inMemoryToken);
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                "add",
                                                storeSequenceException,
                                                "1:402:1.31");
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "allocate",
                           storeSequenceException);
            throw storeSequenceException;
        } // if (inMemoryToken != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "allocate"
                       , new Object[] { tokenToStore }
                            );
        return tokenToStore;
    } // allocate(). 

    /**
     * No space accounting implemented. This should be done in a subclass if required.
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#reserve(int,boolean)
     */
    public void reserve(int deltaSize, boolean pacing)
                    throws ObjectManagerException
    {
        // Does nothing, no space tracking.
    } // reserve().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#add(com.ibm.ws.objectManager.ManagedObject, boolean, int)
     */
    public void add(ManagedObject managedObject,
                    boolean requiresCurrentCheckpoint)
                    throws ObjectManagerException
    {
        final String methodName = "add";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { managedObject, new Boolean(requiresCurrentCheckpoint) });

        // At recovery make sure the Token is in memory, if the Token was newly allocated it will already be in memory.   
        Token inMemoryToken = (Token) inMemoryTokens.putIfAbsent(new Long(managedObject.owningToken.storedObjectIdentifier)
                                                                 , managedObject.owningToken);
        if (inMemoryToken == null) {
            synchronized (sequenceNumberLock) {
                // During recovery processing we may be given a sequence number to replace an object which never completed
                // an add operation in the previous run so make sure we do not reuse such a number again.
                if (managedObject.owningToken.storedObjectIdentifier > sequenceNumber) {
                    sequenceNumber = Math.max(managedObject.owningToken.storedObjectIdentifier, sequenceNumber);
                    if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                        trace.debug(this,
                                    cclass,
                                    methodName,
                                    new Object[] { "sequenceNumber now", new Long(sequenceNumber) });

                } // if (tokenToStore.storedObjectIdentifier > sequenceNumber).
            } // synchronized (sequenceNumberLock).

        } else {
            // The inMemoryToken must be the same Token.
            if (inMemoryToken != managedObject.owningToken) {

                ReplacementException replacementException = new ReplacementException(this,
                                                                                     managedObject,
                                                                                     managedObject.owningToken,
                                                                                     inMemoryToken);
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    replacementException,
                                                    "1:473:1.31");
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "add",
                               new Object[] { replacementException,
                                             managedObject,
                                             managedObject.owningToken,
                                             inMemoryToken });

                throw replacementException;
            } // if (inMemoryToken != managedObject.owningToken).
        } // if (inMemoryToken == null). 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // add().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#remove(com.ibm.ws.objectManager.Token, boolean)
     */
    public void remove(Token token,
                       boolean requiresCurrentCheckpoint)
                    throws ObjectManagerException
    {
        final String methodName = "remove";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { token, new Boolean(requiresCurrentCheckpoint) });

        // The token is no longer visible.
        inMemoryTokens.remove(new Long(token.storedObjectIdentifier));

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // remove().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#deRegister()
     */
    public final void deRegister()
                    throws ObjectManagerException
    {
        // Deregistering the store causes the ObjectManager to close() it. This then 
        // clears the inMemoryTokens.
        objectManagerState.deRegisterObjectStore(this);
    } // deRegister(). 

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#clear()
     */
    protected synchronized void clear()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "clear"
                            );
        // All ManagedObjects are new sequence number up to initialSequenceNumber are reserved.
        sequenceNumber = initialSequenceNumber;
        allocationAllowed = true;

        inMemoryTokens = new WeakValueConcurrentHashMap(concurrency);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "clear"
                            );
    } // clear(). 

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#close()
     */
    public synchronized void close()
                    throws ObjectManagerException
    {
        // Invalidate out all ManagedObjects in inMemory Tokens to prevent them
        // from being used after shutdown.
        for (java.util.Iterator tokenIterator = inMemoryTokens.values().iterator(); tokenIterator.hasNext();) {
            java.lang.ref.Reference reference = (java.lang.ref.Reference) tokenIterator.next();
            if (reference != null) {
                Token token = (Token) reference.get();
                if (token != null)
                    token.invalidate();
            } // if (reference != null).
        } // for inMemoryTokens.

        // Help garbage collection. 
        inMemoryTokens.clear();
        inMemoryTokens = null;
    } // close(). 

    /**
     * Writes buffered output to hardened storage.
     * 
     * @throws ObjectManagerException
     */
    public abstract void flush()
                    throws ObjectManagerException;

    /**
     * The identifier of the ObjectStore, unique within this ObjectManager.
     * 
     * @return int the objectStore identifier
     */
    public int getIdentifier()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
        {
            trace.entry(this, cclass
                        , "getIdentifier"
                            );
            trace.exit(this, cclass
                       , "getIdentifier"
                       , "returns objectStoreIdentifier=" + objectStoreIdentifier + "(int)"
                            );
        }

        return objectStoreIdentifier;
    } // End of getIdentifier.

    /**
     * Set the identifier of the Object Store, unique within this ObjectManager.
     * 
     * @param identifier the assigned objectStore identifier
     * @throws ObjectManagerException
     */
    public void setIdentifier(int identifier)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "setIdentifier",
                        new Object[] { new Integer(identifier) });

        // We can only set this once. 
        if (objectStoreIdentifier != IDENTIFIER_NOT_SET) {
            InvalidConditionException invalidConditionException = new InvalidConditionException(this
                                                                                                , "objectStoreIdentifier"
                                                                                                , Integer.toString(objectStoreIdentifier) + "not equal IDENTIFIER_NOT_SET"
                            );
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                "setIdentifier",
                                                invalidConditionException,
                                                "1:623:1.31",
                                                new Object[] { new Integer(objectStoreIdentifier) });
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "setIdentifier"
                           , invalidConditionException
                                );
            throw invalidConditionException;
        } // if (objectStoreIdentifier != IDENTIFIER_NOT_SET).
        objectStoreIdentifier = identifier;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "setIdentifier"
                            );
    } // setIdentifier().

    /**
     * The name of the ObjectStore.
     * 
     * @return String the objectStore name
     */
    public final String getName() {
        final String methodName = "getName";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this, cclass, methodName);
            trace.exit(this, cclass, methodName, new Object[] { storeName });
        }
        return storeName;
    } // getName().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#setName(java.lang.String)
     */
    public final void setName(String newName)
                    throws ObjectManagerException {
        final String methodName = "setName";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { newName, storeName });

        objectManagerState.renameObjectStore(this, newName);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // setName().

    final void setLogicalName(String newName) {
        storeName = newName;
    } // setLogicalName().

    /**
     * @return ObjectManagerState the objectManagerState with which thes store is registered.
     */
    protected final ObjectManagerState getObjectManagerState()
    {
        return objectManagerState;
    } // getObjectManagerState().

    /**
     * @return Returns the storeStrategy.
     */
    public final int getStoreStrategy()
    {
        return storeStrategy;
    } // getStoreStrategy().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#getPersistence()
     */
    public final boolean getPersistence()
    {
        return persistent;
    } // getPersistence().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#getContainsRestartData()
     */
    public final boolean getContainsRestartData()
    {
        return containsRestartData;
    } // getContainsRestartData().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#getUsesSerializedForm()
     */
    public final boolean getUsesSerializedForm()
    {
        return usesSerializedForm;
    } // getUsesSerializedForm().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#getAddSpaceOverhead()
     */
    public int getAddSpaceOverhead()
    {
        return addSpaceOverhead;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#captureStatistics()
     */
    public java.util.Map captureStatistics()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "captureStatistics"
                            );

        java.util.Map statistics = new java.util.HashMap();
        // inMemoryTokens set to null after close().
        if (inMemoryTokens != null)
            statistics.put("inMemoryTokens.size()", Integer.toString(inMemoryTokens.size()));

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "captureStatistics"
                       , statistics
                            );
        return statistics;
    } // captureStatistics().

    /**
     * Print a dump of the state.
     * 
     * @param printWriter to be written to.
     */
    public void print(java.io.PrintWriter printWriter)
    {
        printWriter.println("State Dump for:" + cclass.getName()
                            + " sequenceNumber=" + sequenceNumber + "(int)"
                            + " allocationAllowed=" + allocationAllowed + "(boolean)"
                            + " storeName=" + storeName + "(String)"
                            + "\n objectStoreIdentifier=" + objectStoreIdentifier + "(int)"
                            + " storeStrategy=" + storeStrategy + "(int)" + strategyNames[storeStrategy] + "(String)"
                            + " persistent=" + persistent + "(boolean)"
                            + " objectManagerState=" + objectManagerState + "(ObjectManagerState)"
                        );
        printWriter.println();

        printWriter.println("inMemoryTokens...");
        for (java.util.Iterator tokenIterator = inMemoryTokens.values().iterator(); tokenIterator.hasNext();) {
            java.lang.ref.Reference reference = (java.lang.ref.Reference) tokenIterator.next();
            if (reference != null) {
                Token token = (Token) reference.get();
                if (token != null)
                    printWriter.println(token.toString());
            }
        } // for ... inMemoryTokens.

    } // print().

    // --------------------------------------------------------------------------
    // extends Object.
    // --------------------------------------------------------------------------  

    /**
     * Short description of the object.
     * 
     * @return String describing this ObjectStore.
     */
    public String toString()
    {
        return new String("AbstractObjectStore"
                          + "(" + storeName + ")"
                          + "/" + Integer.toHexString(hashCode()));
    } // toString().
} // class AbstractObjectStore.
