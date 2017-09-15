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

import java.util.HashMap;

import com.ibm.ws.objectManager.*;

import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.persistence.*;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.utils.ras.SibTr;


public class UniqueKeyRangeManager implements RangeManager, Runnable
{
    private static TraceComponent tc = SibTr.register(UniqueKeyRangeManager.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private final static long UNIQUE_KEY_INITIAL_VALUE = 0L;

    // Keep going flag for the async work.
    private boolean _running = true;

    // Map of generator Tokens
    private HashMap _generators;

    // Anchor for the list of unique key generators.
    private Token   _uniqueKeyRootToken;

    private ObjectStore   _objectStore;
    private ObjectManager _objectManager;

    private java.util.LinkedList _asyncQ;


    public UniqueKeyRangeManager(Token uniqueKeyRootToken, ObjectManager objectManager, ObjectStore objectStore)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", "Token="+uniqueKeyRootToken);

        _uniqueKeyRootToken = uniqueKeyRootToken;
        _objectStore        = objectStore;
        _objectManager      = objectManager;

        _generators = new HashMap(5);
        _asyncQ     = new java.util.LinkedList();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
    }


    void start() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "start");

        try
        {
            // Defect 528563
            // We don't need to use a transaction to 
            // pass on the iterator as we can use null
            // which saves us having to start and backout
            // an uneccessary tran.
            
            LinkedList list = (LinkedList)_uniqueKeyRootToken.getManagedObject();

            // We need to iterate through the existing list of known 
            // generators and build up our hash map. We will then be 
            // ready to answer queries about the existence of generators.
            com.ibm.ws.objectManager.Iterator iterator = list.iterator();
            while (iterator.hasNext(null))
            {
                Token token = (Token)iterator.next(null);

                UniqueKeyGeneratorManagedObject mo = (UniqueKeyGeneratorManagedObject)token.getManagedObject();

                String name = mo.getGeneratorName();
                _generators.put(name, mo);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Generator loaded from persistent store: "+name);
            }
        }
        catch (ClassCastException cce)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(cce, "com.ibm.ws.sib.msgstore.persistence.objectManager.UniqueKeyRangeManager.start", "1:109:1.8", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(tc, "Class cast exception caught during start of RangeManager!", cce);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "start");
            throw new PersistenceException("Class cast exception caught during start of RangeManager!", cce);
        }
        catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.UniqueKeyRangeManager.start", "1:116:1.8", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(tc, "Unexpected exception caught during start of RangeManager!", ome);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "start");
            throw new PersistenceException("Unexpected exception caught during start of RangeManager!", ome);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "start");
    }

    public void stop()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "stop");

        _running = false;

        synchronized (_asyncQ)
        {
            _asyncQ.notify();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "stop");
    }


    /**
     * Request an asynchronous update of the persistent state
     *
     * @param generator to be updated
     */
    public void scheduleUpdate(UniqueKeyGenerator generator)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "scheduleUpdate", "GeneratorName="+generator.getName());

        synchronized (_asyncQ)
        {
            _asyncQ.add(generator);
            _asyncQ.notify();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "scheduleUpdate");
    }


    /**
     * Test if the generator is known to the persistence layer
     *
     * @param generator to check for
     * @return true if generator exists, false otherwise
     */
    public boolean entryExists(UniqueKeyGenerator generator)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "entryExists", "GeneratorName="+generator.getName());

        boolean retval = false;

        if (_generators.containsKey(generator.getName()))
        {
            retval = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "entryExists", "retrun="+retval);
        return retval;
    }


    /**
     * Tell the persistence layer about a new generator
     *
     * @param generator
     * @return initial key to be used by the generator
     * @throws PersistenceException
     */
    public long addEntry(UniqueKeyGenerator generator) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "addEntry", "GeneratorName="+generator.getName());

        if (!_generators.containsKey(generator.getName()))
        {
            Transaction transaction = null;

            try
            {
                transaction = _objectManager.getTransaction();

                // Add the ManagedObject for this generator
                UniqueKeyGeneratorManagedObject uniqueKey = new UniqueKeyGeneratorManagedObject(generator);
                Token uniqueKeyToken = _objectStore.allocate(uniqueKey);
                transaction.add(uniqueKey);

                // Add token to the list of generators
                LinkedList list = (LinkedList)_uniqueKeyRootToken.getManagedObject();
                list.add(uniqueKeyToken, transaction);

                // Commit all of the work
                transaction.commit(false);

                // Store the token in our list to access at runtime.
                _generators.put(generator.getName(), uniqueKey);
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.UniqueKeyRangeManager.addEntry", "1:217:1.8", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(tc, "Exception caught creating new unique key generator!", ome);

                if (transaction != null)
                {
                    try
                    {
                        // Clean up our ObjectManager work.
                        transaction.backout(false);
                    }
                    catch (ObjectManagerException e)
                    {
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.persistence.objectManager.UniqueKeyRangeManager.addEntry", "1:229:1.8", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(tc, "Exception caught backing out unique key generator creation!", e);
                    }
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addEntry");
                throw new PersistenceException("Exception caught creating new unique key generator!", ome);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addEntry", "return="+UNIQUE_KEY_INITIAL_VALUE);
        return UNIQUE_KEY_INITIAL_VALUE;
    }


    /**
     * Request an immediate update to the persistent state
     *
     * @param generator to be updated
     * @return the value that was stored prior to the update
     * @throws PersistenceException
     */
    public long updateEntry(UniqueKeyGenerator generator) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "updateEntry", "GeneratorName="+generator.getName());

        long currentLimit = 0L;

        // Do we know of this generator?
        if (_generators.containsKey(generator.getName()))
        {
            Transaction transaction = null;

            try
            {
                transaction = _objectManager.getTransaction();

                // Replace the ManagedObject for this generator
                UniqueKeyGeneratorManagedObject mo = (UniqueKeyGeneratorManagedObject)_generators.get(generator.getName());

                // Lock the token so we can make our changes.
                transaction.lock(mo);

                // Update the value in the managed object to the 
                // new increased limit.
                synchronized (mo)
                {
                    currentLimit = mo.getGeneratorKeyLimit();
                    mo.setGeneratorKeyLimit(currentLimit + generator.getRange());
                }

                // Use replace to update the contents.
                transaction.replace(mo);
                transaction.commit(false);
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.UniqueKeyRangeManager.updateEntry", "1:286:1.8", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(tc, "Exception caught increasing range of unique key generator!", ome);

                if (transaction != null)
                {
                    try
                    {
                        // Clean up our ObjectManager work.
                        transaction.backout(false);
                    }
                    catch (ObjectManagerException e)
                    {
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.persistence.objectManager.UniqueKeyRangeManager.updateEntry", "1:298:1.8", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(tc, "Exception caught backing out unique key generator update!", e);
                    }
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "updateEntry");
                throw new PersistenceException("Exception caught increasing range of unique key generator!", ome);
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(tc, "No UniqueKeyGenerator matching: "+generator.getName()+" found to update!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "updateEntry");
            throw new PersistenceException("No UniqueKeyGenerator matching: "+generator.getName()+" found to update!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "updateEntry", "return="+currentLimit);
        return currentLimit;
    }


    /**
     * This method is where the asynchronous reads and writes are done 
     * to the filesystem for persistent updates to the currently active 
     * range of keys.
     */
    public void run()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "run");

        UniqueKeyGenerator generator = null;

        while (_running)
        {
            synchronized (_asyncQ)
            {
                // Do we have anything to do? 
                while (_asyncQ.isEmpty())
                {
                    // If not we need to wait until told
                    // that we do.
                    try
                    {       
                        _asyncQ.wait();
                    }
                    catch (InterruptedException ie)
                    {
                        // No FFDC Code Needed.
                    }

                    // If we have been woken up because we are 
                    // stopping then we can simply return here
                    if (!_running)
                    {
                        return;
                    }
                }

                generator = (UniqueKeyGenerator)_asyncQ.removeFirst();
            }

            // Carry out the requested updates.
            try
            {
                updateEntry(generator);
            }
            catch (PersistenceException pe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.UniqueKeyRangeManager.run", "1:366:1.8", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(tc, "Exception caught asynchronously increasing range of unique key generator!", pe);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "run");
    }
}
