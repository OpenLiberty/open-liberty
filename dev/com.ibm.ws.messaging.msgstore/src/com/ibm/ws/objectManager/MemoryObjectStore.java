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
 * Lightweight Objectstore where managedObjects are never written to disk and always remain in memory.
 * 
 * @author IBM Corporation
 */
public class MemoryObjectStore extends AbstractObjectStore
{
    private static final Class cclass = MemoryObjectStore.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_STORE);

    private static final long serialVersionUID = 219689509007536977L;

    // Map all ManagedObjects to hold them in memory.
    private transient java.util.Map inMemoryManagedObjects;

    /**
     * Constructor
     * 
     * @param storeName Identifies the ObjecStore, not relevant to processing.
     * @param objectManager The ObjectManager that manages this store.
     * @throws ObjectManagerException
     */
    public MemoryObjectStore(String storeName,
                             ObjectManager objectManager)
        throws ObjectManagerException
    {
        super(storeName,
              objectManager,
              STRATEGY_KEEP_UNTIL_NEXT_OPEN); // Invoke the SuperClass constructor.
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "<init>",
                        "StoreName=" + storeName + ", ObjectManager=" + objectManager);
            trace.exit(this,
                       cclass,
                       "<init>");
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).
    } // End of constructor (String storeName).

    // --------------------------------------------------------------------------
    // extends AbstractObjectStore.
    // --------------------------------------------------------------------------
    /**
     * Establish the transient state of this ObjectStore.
     * 
     * @param objectManagerState with which the ObjectStore is registered.
     * @throws ObjectManagerException
     */
    public synchronized void open(ObjectManagerState objectManagerState)
                    throws ObjectManagerException {
        final String methodName = "open";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { objectManagerState });

        super.open(objectManagerState);
        usesSerializedForm = false;

        // Lock all ManagedObjects into memory.
        inMemoryManagedObjects = new ConcurrentHashMap(concurrency);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // open().

    /**
     * Retrieve an object in the store. For MemoryObjectStores this should only be called for a ManagedObject that was
     * lost over a restart so null is returned.
     * 
     * @param token representing the object to be retrieved.
     * @return ManagedObject the object from the store, always null.
     */
    public ManagedObject get(Token token)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "get",
                        token);
            trace.exit(this,
                       cclass,
                       "get returns null");
        }
        return null;

        // TODO If the memory object store has been restarted after a restart of the ObjectManager,
        // TODO we may have reused an ObjectSequenceNumber, in which case rathet than findingin null
        // TODO getManagedObject will find a newly created MAnagedObject instead of null.
        // TODO Need some kind of ObjectManagerCycle number in the Token!
        // TODO Alternatively we know that any Token trying to find the currentToken after restart
        // TODO of the ObjectMAnager must have been restored from previous run so it must return
        // TODO null and leave the get method the to throw an exception.
        // TODO Alternatively we could occasionally save a safe staer sequencenumber, perhaps on e
        // TODO checkpoint when the ObjectStopre is serialized anyway.

    } // End of method get.

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#add(com.ibm.ws.objectManager.ManagedObject, boolean)
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

        super.add(managedObject, requiresCurrentCheckpoint);

        inMemoryManagedObjects.put(new Long(managedObject.owningToken.storedObjectIdentifier),
                                   managedObject);

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
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "remove"
                        , new Object[] { token, new Boolean(requiresCurrentCheckpoint) });

        super.remove(token, requiresCurrentCheckpoint);
        // Free up the reference to the memory.
        inMemoryManagedObjects.remove(new Long(token.storedObjectIdentifier));

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "remove");
    } // remove().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#clear()
     */
    protected synchronized void clear()
                    throws ObjectManagerException
    {
        final String methodName = "clear";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName);

        super.clear();
        inMemoryManagedObjects = new ConcurrentHashMap(concurrency);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, "clear");
    } // clear().

    /**
     * For MemoryObjectstore there is nothing to do.
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#flush()
     */
    public synchronized void flush()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "flush");
            trace.exit(this,
                       cclass,
                       "flush");
        }
    } // flush().

    private transient Set tokenSet; // Initialised if used.

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#tokens()
     */
    public Set tokens() {
        if (tokenSet == null) {
            tokenSet = new AbstractSetView() {
                public long size() {
                    return inMemoryManagedObjects.size();
                }

                public Iterator iterator() {
                    final java.util.Iterator tokenIterator = inMemoryTokens.values().iterator();
                    return new Iterator() {

                        public boolean hasNext()
                        {
                            return tokenIterator.hasNext();
                        } // hasNext().

                        public Object next()
                        {
                            return tokenIterator.next();
                        } // next().

                        public boolean hasNext(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            throw new UnsupportedOperationException();
                        } // hasNext().

                        public Object next(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            throw new UnsupportedOperationException();
                        } // next().

                        public Object remove(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            throw new UnsupportedOperationException();
                        } // remove().               
                    }; // new Iterator().
                } // iterator().  
            }; // new AbstractSetView().
        } // if (tokenSet == null). 
        return tokenSet;
    } // tokens().

} // class MemoryObjectStore.
