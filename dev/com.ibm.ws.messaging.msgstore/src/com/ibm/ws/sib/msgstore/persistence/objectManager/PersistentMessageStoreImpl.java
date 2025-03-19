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
package com.ibm.ws.sib.msgstore.persistence.objectManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.objectManager.ConcurrentLinkedList;
import com.ibm.ws.objectManager.LogFileInUseException;
import com.ibm.ws.objectManager.LogFileSizeTooSmallException;
import com.ibm.ws.objectManager.ManagedObject;
import com.ibm.ws.objectManager.NonExistentLogFileException;
import com.ibm.ws.objectManager.ObjectManager;
import com.ibm.ws.objectManager.ObjectManagerEventCallback;
import com.ibm.ws.objectManager.ObjectManagerException;
import com.ibm.ws.objectManager.ObjectStore;
import com.ibm.ws.objectManager.PermanentIOException;
import com.ibm.ws.objectManager.SingleFileObjectStore;
import com.ibm.ws.objectManager.StoreFileSizeTooSmallException;
import com.ibm.ws.objectManager.Token;
import com.ibm.ws.objectManager.Transaction;
import com.ibm.ws.objectManager.UnknownObjectStoreException;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Configuration;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreUnavailableException;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.SeverePersistenceException;
import com.ibm.ws.sib.msgstore.cache.links.RootMembership;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.BatchingContextFactory;
import com.ibm.ws.sib.msgstore.persistence.MELockOwner;
import com.ibm.ws.sib.msgstore.persistence.Operation;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.PersistentMessageStore;
import com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum;
import com.ibm.ws.sib.msgstore.persistence.UniqueKeyGenerator;
import com.ibm.ws.sib.msgstore.persistence.dispatcher.SpillDispatcher;
import com.ibm.ws.sib.msgstore.persistence.impl.Tuple;
import com.ibm.ws.sib.msgstore.task.TaskList;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistenceManager;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;
import com.ibm.ws.sib.msgstore.transactions.impl.XidManager;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implements the PersistentMessageStore interface, which is the primary interface between
 * the cache and persistence.
 */
public final class PersistentMessageStoreImpl implements PersistentMessageStore, PersistenceManager, ObjectManagerEventCallback
{
    private static TraceComponent tc = SibTr.register(PersistentMessageStoreImpl.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private static long MAXIMUM_STORE_FILE_SIZE = Long.MAX_VALUE;
    private static String PERMANENT_STORE_NAME = "PermanentStore";
    private static String TEMPORARY_STORE_NAME = "TemporaryStore";

 // private JsMessagingEngine _me;
    private MELockOwner       _meOwner;

    private MessageStoreImpl  _ms;
    private Configuration     _configuration;
    private MEStartupTimeouts _timeouts;

    private ObjectManager _objectManager;
    private ObjectStore   _permanentStore;   // Used for STORE_ALWAYS/EVENTUALLY items
    private ObjectStore   _temporaryStore;   // Used for STORE_MAYBE items

    private Token  _anchorToken;
    private Anchor _anchor;

    private UniqueKeyManager _uniqueKeyManager;

    // Defect 530772
    private SpillDispatcher        _spillDispatcher;
    private BatchingContextFactory _batchingContextFactory;

    // Defect 363755
    private volatile boolean _available = false;
    // Defect 549131
    private volatile boolean _starting = false;

    // Defect 496893
    private boolean _shutdownRequested = false;

    // Indicates whether the ObjectManager has finished starting up - d533632
    private boolean _omgrStarted = false;

    // Map of ObjectStoreLocations passed to the OMgr - d 502275
    private Map<String,String> storeLocations = new HashMap<String,String>();

    // Parking place for old location of permanent & temporary stores - d502275
    private String oldPermanentStore = null;
    private String oldTemporaryStore = null;

    private String fileSepPattern = (File.separatorChar == '/') ? File.separator : "\\" + File.separator;

    /**
     * Public no-args constructor.
     *
     * The initialize method must be called before starting the PersistentMessageStore.
     */
    public PersistentMessageStoreImpl()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

        _uniqueKeyManager = new UniqueKeyManager();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }


    /*************************************************************************/
    /*                   PersistentMessageStore Implementation               */
    /*************************************************************************/

    /**
     * Initialises the PersistentMessageStore
     *
     * @param ms Reference to the owning MessageStoreImpl which offers a variety of utility methods
     * @param xidManager The transaction layer's XidManager
     * @param configuration The configuration for the persistence layer
     */
    public void initialize(MessageStoreImpl ms, XidManager xidManager, Configuration configuration)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initialize", new Object[]{"MS="+ms, "XidManager="+xidManager, "Config="+configuration});

        _ms            = ms;
        _configuration = configuration;

       

        // Defect 496893
        _timeouts = new MEStartupTimeouts(_ms);

        

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initialize");
    }

    /**
     * Starts the Persistent Message Store.
     *
     * @exception PersistenceException
     */
    public void start() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start");

        String logFileName        = null;
        String permanentStoreFileName = null;
        String temporaryStoreFileName = null;

        String logFileProperty = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_NAME,
                                                 MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_NAME_DEFAULT);
        String logPrefix       = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_PREFIX,
                                                 _configuration.getObjectManagerLogDirectory());
        String logSizeStr      = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_SIZE, null);

        long logSize = _configuration.getObjectManagerLogSize();
        if (logSizeStr != null) logSize = Long.parseLong(logSizeStr);

        // Permanent Object Store properties
        String permanentStoreProperty       = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_NAME,
                                                              MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_NAME_DEFAULT);
        String permanentStorePrefix         = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_PREFIX,
                                                              _configuration.getObjectManagerPermanentStoreDirectory());

        // Temporary Object Store properties
        String temporaryStoreProperty       = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_NAME,
                                                              MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_NAME_DEFAULT);
        String temporaryStorePrefix         = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_PREFIX,
                                                              _configuration.getObjectManagerTemporaryStoreDirectory());
        // 727994
        // Set _hasRedeliveryCountColumn to true because unlike datastore where we
        // check if redelivery count column exist or not we dont have to do the
        // same check for filestore as that column will be automatically available
        // depending upon the version of WAS
        // Persisting RedeliveryCount feature was introduced from WAS v8.5 onwards
        _ms.setRedeliveryCountColumn(true);

        // Defect 496893
        // First we need to start up our ObjectManager instance. We will retry this
        // for a pre-determined time period in order to allow for connectivity problems
        // to the file store files.
        boolean connected = false;
        long   retryLimit = System.currentTimeMillis() + _timeouts.getRetryTimeLimit();
        // Defect 549131
        // Set our starting flag so that we can detect if we are asked to 
        // stop while attempting to start the ObjectManager.
        _starting = true;
        do
        {
            // The directory creation needs to be in the retry loop as if we can't connect
            // once then the directory would never get created and may cause us to fail to
            // find our path even if the disc is available.
            logPrefix = createDirectoryPath(logPrefix);

            if (logPrefix.endsWith(File.separator))
            {
                logFileName = logPrefix+logFileProperty;
            }
            else
            {
                logFileName = logPrefix+File.separator+logFileProperty;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
            	SibTr.debug(this, tc, "User directory for ObjectManager log file defined: "+logPrefix);
            	SibTr.debug(this, tc, "The file store is attempting to initalise its log file:" + logFileName);
            }

            // As we do not know earlier whether we have access to our file system we need to do
            // our path name creation at this point. It still does not save us if our store
            // files and log file are on completely seperately network accessible locations
            // but it is slightly safer than doing it before the log starts up.
            permanentStorePrefix = createDirectoryPath(permanentStorePrefix);
            temporaryStorePrefix = createDirectoryPath(temporaryStorePrefix);

            // Figure out the real file names where we expect to find the permanent and temporary stores.
            // We use them to build the ObjectStoreLocations map to pass into the ObjectManager.
            if (permanentStorePrefix.endsWith(File.separator))
            {
                permanentStoreFileName = permanentStorePrefix+permanentStoreProperty;
            }
            else
            {
                permanentStoreFileName = permanentStorePrefix+File.separator+permanentStoreProperty;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Filename for ObjectManager permanent store is: "+permanentStoreFileName);

            if (temporaryStorePrefix.endsWith(File.separator))
            {
                temporaryStoreFileName = temporaryStorePrefix+temporaryStoreProperty;
            }
            else
            {
                temporaryStoreFileName = temporaryStorePrefix+File.separator+temporaryStoreProperty;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Filename for ObjectManager temporary store is: "+temporaryStoreFileName);

            // Populate the Map of ObjectStoreLocations
            storeLocations.put(PERMANENT_STORE_NAME, permanentStoreFileName);
            storeLocations.put(TEMPORARY_STORE_NAME, temporaryStoreFileName);

            try
            {
                // Defect 549131
                // Synchronize with the stop thread at this point so that one 
                // of two things happen:
                //  1) The stop thread gets in first. We need to avoid starting an object
                //     manager altogether.
                //  2) The start thread gets in first. The stop thread needs to wait so
                //     that its stop processing has the handle to the objectmanager to 
                //     call stop on.
                synchronized(this)
                {
                    if (_starting)
                    {
                        // Do we need to start from an empty file store or can
                        // we startup using an existing set of data?
                        // Either way, we need to register ourselves for any event callbacks.
                        if (_configuration.isCleanPersistenceOnStart())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Clean starting the Persistence Layer.");
        
                            _objectManager = new ObjectManager(logFileName, ObjectManager.LOG_FILE_TYPE_CLEAR, storeLocations, new ObjectManagerEventCallback[]{this});
                        }
                        else
                        {
                            _objectManager = new ObjectManager(logFileName, ObjectManager.LOG_FILE_TYPE_FILE, storeLocations, new ObjectManagerEventCallback[]{this});
                        }

                        connected = true;
                    }
                }

            }
            catch (PermanentIOException pioe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(pioe, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.start", "1:397:1.81.1.6", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unable to start ObjectManager instance!", pioe);
                SibTr.error(tc, "FILE_STORE_UNEXPECTED_IO_EXCEPTION_SIMS1592");
                // This will drop through to the retry wait below.
            }
            catch (LogFileInUseException lfiue)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(lfiue, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.start", "1:404:1.81.1.6", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unable to start ObjectManager instance!", lfiue);
                SibTr.error(tc, "FILE_STORE_LOG_FILE_IN_USE_SIMS1579");
                // This will drop through to the retry wait below.
            }
            catch (NonExistentLogFileException nelfe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(nelfe, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.start", "1:411:1.81.1.6", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unable to start ObjectManager instance!", nelfe);
                SibTr.error(tc, "FILE_STORE_CANNOT_FIND_LOG_FILE_SIMS1580", logFileName);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
                throw new SeverePersistenceException("Unable to locate the file path: " + nelfe.getMessage(), nelfe);
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.start", "1:418:1.81.1.6", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught initializing file store!", ome);
                SibTr.error(tc, "FILE_STORE_UNEXPECTED_INITIALISATION_EXCEPTION_SIMS1591");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
                throw new SeverePersistenceException("Unexpected exception caught initializing file store: "+ome.getMessage(), ome);
            }

            // Defect 549131
            // Only go into the retry wait if we are still starting
            if (!connected && _starting)
            {
                // Have we taken longer to create the ObjectManager than our alloted
                // retry period?
                long currentTime = System.currentTimeMillis();
                if (currentTime < retryLimit)
                {
                    SibTr.info(tc, "FILE_STORE_PROBLEM_INITIALISING_LOG_SIMS1582");

                    // If we still have time to retry then we need to wait a while to
                    // avoid a hot loop and then try again.
                    try
                    {
                        synchronized(this)
                        {
                            wait(_timeouts.getRetryWaitTime());
                        }
                    }
                    catch (InterruptedException ie)
                    {
                        // No FFDC Code Needed.
                    }
                }
                else
                {
                    SibTr.error(tc, "FILE_STORE_INITIALISATION_RETRY_LIMIT_EXCEEDED_SIMS1583");

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
                    throw new SeverePersistenceException("Retry time limit reached when starting file store!");
                }
            }
        // Defect 549131
        // If we were already waiting when the stop request came in our
        // notify should pop us out at which point this check will drop
        // us out of the while loop.
        } while (!connected && _starting); 

        // Defect 549131
        // Carry on starting up only if we have not been asked to stop.
        if (_starting)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "The file store has initialised successfully.");
    
            // Note that the the OMgr is now up and running, so we do care if it stops
            _omgrStarted = true;
    
            // Now we need to open our ObjectStores and find our Anchor object.
            // Figure out the size properties
            // Permanent Object Store properties
            String minimumPermanentStoreSizeStr = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MINIMUM_SIZE, null);
            String maximumPermanentStoreSizeStr = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MAXIMUM_SIZE, null);
    
            long minimumPermanentStoreSize = _configuration.getObjectManagerMinimumPermanentStoreSize();
            if (minimumPermanentStoreSizeStr != null) minimumPermanentStoreSize = Long.parseLong(minimumPermanentStoreSizeStr);
            long maximumPermanentStoreSize = _configuration.getObjectManagerMaximumPermanentStoreSize();
            if (maximumPermanentStoreSizeStr != null) maximumPermanentStoreSize = Long.parseLong(maximumPermanentStoreSizeStr);
    
            boolean isPermanentStoreSizeUnlimited = _configuration.isObjectManagerPermanentStoreSizeUnlimited();
    
            // Temporary Object Store properties
            String minimumTemporaryStoreSizeStr = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MINIMUM_SIZE, null);
            String maximumTemporaryStoreSizeStr = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MAXIMUM_SIZE, null);
    
            long minimumTemporaryStoreSize = _configuration.getObjectManagerMinimumTemporaryStoreSize();
            if (minimumTemporaryStoreSizeStr != null) minimumTemporaryStoreSize = Long.parseLong(minimumTemporaryStoreSizeStr);
            long maximumTemporaryStoreSize = _configuration.getObjectManagerMaximumTemporaryStoreSize();
            if (maximumTemporaryStoreSizeStr != null) maximumTemporaryStoreSize = Long.parseLong(maximumTemporaryStoreSizeStr);
    
            boolean isTemporaryStoreSizeUnlimited = _configuration.isObjectManagerTemporaryStoreSizeUnlimited();

            // common properties
            String storeFullWaitForCheckPointStr = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_STORE_FULL_WAIT_FOR_CHECKPOINT,
                                                                   MessageStoreConstants.PROP_OBJECT_MANAGER_STORE_FULL_WAIT_FOR_CHECKPOINT_DEFAULT);
            boolean storeFullWaitForCheckPoint = Boolean.parseBoolean(storeFullWaitForCheckPointStr);


            // Defect 533310
            // Read the object store object cache size properties
            String permanentStoreObjectCacheSizeStr = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_CACHE_SIZE, 
                                                                      MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_CACHE_SIZE_DEFAULT);
    
            int permanentStoreObjectCacheSize = Integer.parseInt(permanentStoreObjectCacheSizeStr);
    
            String temporaryStoreObjectCacheSizeStr = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_CACHE_SIZE, 
                                                                      MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_CACHE_SIZE_DEFAULT);
    
            int temporaryStoreObjectCacheSize = Integer.parseInt(temporaryStoreObjectCacheSizeStr);
    
            Transaction transaction = null;
    
            try
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "The file store is attempting to start its permanent store (" + PERMANENT_STORE_NAME + ") and temporary store (" + TEMPORARY_STORE_NAME + ")");
    
                // Defect 298902
                // If we are warm started then we should find an existing
                // set of object stores. If we are cold started then we
                // need to create them.
                if (_objectManager.warmStarted())
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Object Manager Warm Started");
    
                    try
                    {
                        // If we have an 'oldPermanentStoreName' we'll need to open that
                        // and rename it, otherwise we just open the 'proper' name.
                        if (oldPermanentStore == null)
                        {
                            _permanentStore = _objectManager.getObjectStore(PERMANENT_STORE_NAME);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Permanent Store Found");
                        }
                        else
                        {
                            _permanentStore = _objectManager.getObjectStore(oldPermanentStore);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Old Permanent Store Found");
                            _permanentStore.setName(PERMANENT_STORE_NAME);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Old Permanent Store Renamed");
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "The file stores permanent store has been started successfully.");
    
                        // If we have an 'oldTemporaryStoreName' we'll need to open that
                        // and rename it, otherwise we just open the 'proper' name.
                        if (oldTemporaryStore == null)
                        {
                            _temporaryStore = _objectManager.getObjectStore(TEMPORARY_STORE_NAME);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Temporary Store Found");
                        }
                        else
                        {
                            _temporaryStore = _objectManager.getObjectStore(oldTemporaryStore);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Old Temporary Store Found");
                            _temporaryStore.setName(TEMPORARY_STORE_NAME);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Old Temporary Store Renamed");
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "The file stores temporary store has been started successfully.");
    
                        // Set the size of our files.
                        setFileSizesAfterRestart(false,
                                                 minimumPermanentStoreSize, maximumPermanentStoreSize, isPermanentStoreSizeUnlimited,
                                                 minimumTemporaryStoreSize, maximumTemporaryStoreSize, isTemporaryStoreSizeUnlimited,
                                                 logSize);
                    }
                    catch (UnknownObjectStoreException uose)
                    {
                        com.ibm.ws.ffdc.FFDCFilter.processException(uose, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.start", "1:572:1.81.1.6", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "ObjectStore not found after warm start!", uose);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
                        throw new SeverePersistenceException("ObjectStore not found after warm start!");
                    }
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Object Manager Cold Started");
    
                    // Create the permanent store.
                    _permanentStore = new SingleFileObjectStore(PERMANENT_STORE_NAME, _objectManager, ObjectStore.STRATEGY_KEEP_ALWAYS);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    	SibTr.debug(this, tc, "The file stores permanent store has been started successfully.");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Permanent Store Created");
    
                    // Create the temporary store.                                        // Defect 291558, empty this store on restart
                    _temporaryStore = new SingleFileObjectStore(TEMPORARY_STORE_NAME, _objectManager, ObjectStore.STRATEGY_KEEP_UNTIL_NEXT_OPEN);
                    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    	SibTr.debug(this, tc, "The file stores temporary store has been started successfully.");
                    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Temporary Store Created");
    
                    // Set the size of our files.
                    setFileSizesAfterRestart(true,
                                             minimumPermanentStoreSize, maximumPermanentStoreSize, isPermanentStoreSizeUnlimited,
                                             minimumTemporaryStoreSize, maximumTemporaryStoreSize, isTemporaryStoreSizeUnlimited,
                                             logSize);
                }

                // set the storeFullWaitForCheckpoint property
                _permanentStore.setStoreFullWaitForCheckPoint(storeFullWaitForCheckPoint);
                _temporaryStore.setStoreFullWaitForCheckPoint(storeFullWaitForCheckPoint);

                // Defect 533310
                // Set the cache sizes on the object stores
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Permanent store object cache size set to: "+permanentStoreObjectCacheSize);
                ((SingleFileObjectStore)_permanentStore).setCachedManagedObjectsSize(permanentStoreObjectCacheSize);
    
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Temporary store object cache size set to: "+temporaryStoreObjectCacheSize);
                ((SingleFileObjectStore)_temporaryStore).setCachedManagedObjectsSize(temporaryStoreObjectCacheSize);
    
    
                // Once we have our ObjectStore references we now need our
                // Anchor object. If we have cold started or done a clean warm
                // start then we need to create the Anchor. If not we should
                // find it in the ObjectManager.
                _anchorToken = _objectManager.getNamedObject("Anchor", null);
    
                if (_anchorToken != null)
                {
                    _anchor = (Anchor)_anchorToken.getManagedObject();
    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Stored Anchor Found");
                }
                else
                {
                    // If we didn't manage to create the Anchor last time
                    // then we need to do it now.
                    transaction = _objectManager.getTransaction();
    
                    // Create and allocate a new Anchor.
                    _anchor      = new Anchor();
                    _anchorToken = _permanentStore.allocate(_anchor);
    
                    // Add Anchor to transaction.
                    transaction.add(_anchor);
    
                    // Add Anchor's token to the ObjectManager as a named object.
                    _objectManager.putNamedObject("Anchor", _anchorToken, transaction);
    
                    transaction.commit(false);
    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Stored Anchor Created");
                }
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.start", "1:647:1.81.1.6", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught starting permanent and temporary stores!", ome);
    
                if (transaction != null)
                {
                    try
                    {
                        // Clean up our ObjectManager work.
                        transaction.backout(false);
                    }
                    catch (ObjectManagerException e)
                    {
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.start", "1:659:1.81.1.6", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught backing out persistent message store startup work!", e);
                    }
                }
    
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
                throw new SeverePersistenceException("Unexpected exception caught starting permanent and temporary stores: "+ome.getMessage(), ome);
            }
    
            // Check the ME_UUID and update our INC_UUID.
            // If we are successful then carry on with startup.
            if (checkAndUpdateMEOwner(_meOwner))
            {
                // Start the unique key generator manager.
                _uniqueKeyManager.start(_anchor, _objectManager, _permanentStore);
    
                // Defect 530772
                // Create the batching context factory.
                _batchingContextFactory = new BatchingContextFactoryImpl(_objectManager, _temporaryStore);
    
                // Create and start the spill dispatcher.
                _spillDispatcher = new SpillDispatcher(_ms, _batchingContextFactory);
                _spillDispatcher.start();
    
                // Defect 363755
                // Only become available once we have finished starting.
                _available = true;
                
                if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "The file store has started successfully.");
            }
        } // if (_starting)
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Startup processing halted as a stop was requested!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
            throw new PersistenceException("Startup processing halted as a stop was requested!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
    }

    @Deprecated
    public void stop(int mode) {
        stop(mode, null);
    }

    /**
     * Stops the Persistent Message Store.
     *
     * @param mode specifies the type of stop operation which is to
     *             be performed.
     */
    public void stop(int mode, Throwable reason)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stop", "Mode="+mode);

        // Defect 363755
        // Stop any new work coming in as soon as we start stopping.
        // This way we make sure nothing is attempted during the
        // (possibly long) time it takes for the OM to shutdown.
        _available = false;

  

        // Defect 496893
        // Set the shutdown flag so we can tell if the callback from
        // the object manager was requested or not.
        _shutdownRequested = true;

        // Defect 549131
        // Check to see if we have an active startup thread
        if (_starting)
        {
            // Synchronize with the startup thread
            synchronized(this)
            {
                // We have an active startup thread so we need 
                // to inform it that it should give up.
                _starting = false;
    
                // We may need to wake up the startup thread for
                // it to check the flag so notify.
                notify();
            }
        }

        // Defect 530772
        // Stop spill dispatcher first so that it
        // can flush it's buffers before we stop the
        // object manager.
        if (_spillDispatcher != null)
        {
            _spillDispatcher.stop(mode, reason);

            _spillDispatcher = null;
        }

        _batchingContextFactory = null;

        if (_objectManager != null)
        {
            try
            {
                _objectManager.shutdown();
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.stop", "1:764:1.81.1.6", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught stopping persistent message store!", ome);
            }

            _objectManager  = null;
            _permanentStore = null;
            _temporaryStore = null;
        }

        if (_uniqueKeyManager != null)
        {
            _uniqueKeyManager.stop();

            _uniqueKeyManager = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stop");
    }

    // This method implements ObjectManagerEventCallback and will be called
    // by the ObjectManager when anything 'interesting' occurs.
    public void notification(int event, Object[] args)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "notification", new Object[]{event, args});

      switch (event) {

        // This event will be notified by the ObjectManager when it shuts down.
        // Do any clearing up and closing down required if it had previously started.
        // If it is still in the process of starting, we are not interested.
        case ObjectManagerEventCallback.objectManagerStopped:
          if (_omgrStarted) {
              objectManagerStopped();
          }
          break;

        // The ObjectManager has found an object store to open.
        // We need to ensure it exists & patch up the map if not.
        case ObjectManagerEventCallback.objectStoreOpened:
          ensureStoreCanBeFound((ObjectStore)args[0]);
          break;

        // No other events that we know or care about.
        default:
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "notification");
    }

    // Defect 496893
    // This method will be called when ObjectManager shuts down.
    // If the shut down was not triggered by us then
    // we need to make sure that the MS and ME are stopped aswell
    // as we can not function once we have lost the ObjectManager.
    private void objectManagerStopped()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "objectManagerStopped");

        // Stop any new work coming in as we do not have a working
        // ObjectManager to service any requests.
        _available = false;

        // Only treat this shutdown as an error if we haven't
        // asked for it. In the case of a normal ME stop we
        // will still get the callback but don't have to worry
        // about it.
        if (!_shutdownRequested)
        {
            // Report a local error so that we can begin failover to a
            // new ME instance. This will allow the new instance to
            // enter it's startup retry loop and try to re-connect to
            // a working ObjectManager.
            _ms.reportLocalError();

            SibTr.error(tc, "FILE_STORE_STOP_UNEXPECTED_SIMS1590");
        }
        else
        {
            SibTr.info(tc, "FILE_STORE_STOP_EXPECTED_SIMS1589");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "objectManagerStopped");
    }


    /**                                ]
     * ensureStoreCanBeFound
     * This is called when the ObjectManager notifies us that it is about to open
     * a Store. We need to ensure the store it is looking for exists. If it doesn't,
     * we need to patch up the locations map (which we have already passed into
     * the OMgr) so that the OMGr can get from the old name to where (we think) it
     * is now.
     * This is necessary to support the migration of v61 to v7, where the Stores
     * are located in the WAS install tree.
     *
     * @param store        The ObjectStore about to be opened.
     */
    private void ensureStoreCanBeFound(ObjectStore store) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "ensureStoreCanBeFound", store);

        // We only care it it is a 'real' store (i.e not OMgr's 'default' store).
        if (store instanceof SingleFileObjectStore) {
            // This is the store name from the logFile, so it could be a real
            // name or a mapped name.
            String storeName = store.getName();
            // Find the real filename of the file we're trying to open
            String storeFileName = storeLocations.get(storeName);

            // If we get back a 'storeFileName', then the storeName was in the Map
            // in which case we are operating on Logical Names so everything should
            // work without any patching up.
            if (storeFileName != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, storeName +  " already maps to " + storeFileName);
            }

            // However, if it is not in the map, we are not in the brave new world of
            // Logical Names so some patching up is needed.
            // We should only need to patch up on the first startup after a migration
            // from a pre-WAS70 install.
            else {
                // We need to figure out which of our current stores represents
                // the store the OMgr wansts to open.
                // It must be one of our 'logical' stores, and at least the end-part
                // should match the end-part of one of our current store file names.
                String[] nameParts = storeName.split(fileSepPattern);
                String lastPart = nameParts[nameParts.length-1];

                // Does it appear to match the expected file name of the permanent store?
                String permStoreName = storeLocations.get(PERMANENT_STORE_NAME);
                if (permStoreName.endsWith(lastPart)) {
                    // If so, map the name the OMgr will look for to where it is now
                    // and stash away the old name.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Mappping old PermanentStore: " + storeName + " to " + permStoreName);
                    storeLocations.put(storeName, permStoreName);
                    oldPermanentStore = storeName;
                }

                // Otherise, does it appear to match the expected file name of the temporary store?
                else {
                    String tempStoreName = storeLocations.get(TEMPORARY_STORE_NAME);
                    if (tempStoreName.endsWith(lastPart)) {
                         // If so, map the name the OMgr will look for to where it is now
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Mappping old Temporary Store:" + storeName + " to " + tempStoreName);
                        storeLocations.put(storeName, tempStoreName);
                        oldTemporaryStore = storeName;
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "ensureStoreCanBeFound");
    }


    // Feature SIB0112b.ms.1
    public List<DataSlice> readDataOnly(Persistable item) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readDataOnly", "Item="+item);

        // Defect 363755
        if (!_available)
        {
            MessageStoreUnavailableException msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readDataOnly");
            throw msue;
        }

        List<DataSlice> dataSlices = ((PersistableImpl)item).getPersistedData();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readDataOnly", "return="+dataSlices);
        return dataSlices;
    }

    public Persistable readRootPersistable() throws PersistenceException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readRootPersistable");

        // Defect 363755
        if (!_available)
        {
            MessageStoreUnavailableException msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readRootPersistable");
            throw msue;
        }

        PersistableImpl root = null;
        Transaction transaction = null;

        try
        {
            // Do we have an existing persisted anchor?
            if (_anchor.getRootStreamToken() != null)
            {
                root = new PersistableImpl(_anchor.getRootStreamToken());
            }
            else
            {
                // No existing anchor so we need to create one.
                transaction = _objectManager.getTransaction();

                root = new PersistableImpl(RootMembership.MEMBER_KEY, RootMembership.MEMBERSHIP_KEY, TupleTypeEnum.ROOT);
                root.setStorageStrategy(AbstractItem.STORE_ALWAYS);
                root.setItemClassName(RootMembership.class.getName());

                // Ask the persistable to add itself to the store.
                root.addToStore(transaction, _permanentStore);

                // Update the Anchor to point to our new root.
                transaction.lock(_anchor);
                _anchor.setRootStreamToken(root.getMetaDataToken());
                transaction.replace(_anchor);
                transaction.commit(false);
            }
        }
        catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.readRootPersistable", "1:982:1.81.1.6", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught reading root persistable!", ome);

            if (transaction != null)
            {
                try
                {
                    // Clean up our ObjectManager work.
                    transaction.backout(false);
                }
                catch (ObjectManagerException e)
                {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.readRootPersistable", "1:994:1.81.1.6", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught backing out addition of root persistable!", e);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readRootPersistable");
            throw new PersistenceException("Unexpected exception caught reading root persistable: "+ome.getMessage(), ome);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readRootPersistable", "return="+root);
        return root;
    }

    public List readNonStreamItems(Persistable containingStream) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readNonStreamItems", "Stream="+containingStream);

        // Defect 363755
        if (!_available)
        {
            MessageStoreUnavailableException msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readNonStreamItems");
            throw msue;
        }

        Transaction storeMaybetransaction = null;
        List items = new java.util.LinkedList();

        try
        {
            storeMaybetransaction = _objectManager.getTransaction();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Item Stream: "+containingStream);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "{");

            Token           itemListToken = ((PersistableImpl)containingStream).getMetaData().getItemListToken();
            ConcurrentLinkedList itemList = (ConcurrentLinkedList)itemListToken.getManagedObject();

            com.ibm.ws.objectManager.Iterator listIterator = itemList.entryIterator();

            int expirableCounter = 0;
            int storeMaybeCounter = 0;

            while (listIterator.hasNext())
            {
                com.ibm.ws.objectManager.List.Entry entry = (com.ibm.ws.objectManager.List.Entry)listIterator.next();
                Token token = entry.getValue();

                // APAR PM43456
                // If we have amassed enough STORE_MAYBE deletions then
                // lets commit the transaction and start a new one so
                // we don't end up filling the log with one big tran.
                // NOTE: We need to do this between iterator.next()
                // and entry.remove() so that the cursor on the list
                // is valid for the following call to next().
                // This scenario is applicable only when migration
                // happens from v6.x to higher versions due to the  
                // difference in implementation for STORE_MAYBE items
                if (storeMaybeCounter > 100)
                {
                    storeMaybetransaction.commit(false);
                    storeMaybeCounter = 0;

                    storeMaybetransaction = _objectManager.getTransaction();
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Token: "+token);

                // APAR PM43456
                // If we cannot get to our managed object then
                // we have found a token that points to an
                // Item that was in the STORE_MAYBE object
                // store. If this is the case then we need to
                // remove the reference.
                // This scenario is applicable only when migration
                // happens from v6.x to higher versions due to the 
                // difference in implementation for STORE_MAYBE items
                if (token.getManagedObject() == null)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Item Found: null");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " STORE_MAYBE Item cleaned-up at restart.");

                    entry.remove(storeMaybetransaction);

                    storeMaybeCounter++;
                }
                else
                {
                    PersistableImpl item = new PersistableImpl(token);

                    if ((item.getTupleType() == TupleTypeEnum.ITEM) || (item.getTupleType() == TupleTypeEnum.ITEM_REFERENCE))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Item Found: "+item);

                        item.setContainingStream(containingStream);

                        items.add(item);
                    }

                    // Keep track of any expirable items on this stream.
                    // If we need to update the stream after reading in
                    // all the items to reset the flag we can do so after
                    // reading the entire contents.
                    if (item.getExpiryTime() > 0)
                    {
                        expirableCounter++;
                    }
                }
            }

            // Do we need to reset our containsExpirables flag?
            PersistableImpl stream = (PersistableImpl)containingStream;
            if (stream.getMetaData().containsExpirables() && (expirableCounter == 0))
            {
                // We no longer have any expirables on the stream so we
                // we can reset the flag.
                Transaction transaction = _objectManager.getTransaction();

                transaction.lock(stream.getMetaData());
                stream.getMetaData().setContainsExpirables(false);
                transaction.replace(stream.getMetaData());

                transaction.commit(false);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "}");
        }
        catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.readNonStreamItems", "1:1122:1.81.1.6", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught reading in items!", ome);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readNonStreamItems");
            throw new PersistenceException("Unexpected exception caught reading in items: "+ome.getMessage(), ome);
        }
        finally
        {
            // Complete our cleanup transaction even
            // if we have hit an error.
            if (storeMaybetransaction != null)
            {
                try
                {
                    storeMaybetransaction.commit(false);
                }
                catch (ObjectManagerException e)
                {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.readNonStreamItems", "1:1077:1.74.1.4", this);
                    if (tc.isEventEnabled()) SibTr.event(tc, "Unexpected exception caught cleaning up STORE_MAYBE Items!", e);
                    if (tc.isEntryEnabled()) SibTr.exit(tc, "readNonStreamItems");
                    throw new PersistenceException("Unexpected exception caught cleaning up STORE_MAYBE Items!", e);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readNonStreamItems", "return="+items);
        return items;
    }

    public List readAllStreams() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readAllStreams");

        // Defect 363755
        if (!_available)
        {
            MessageStoreUnavailableException msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readAllStreams");
            throw msue;
        }

        List streams = new java.util.LinkedList();

        // Want all information about all streams except the root stream
        Token rootToken = _anchor.getRootStreamToken();

        Transaction transaction = null;

        try
        {
            transaction = _objectManager.getTransaction();

            PersistableImpl root = new PersistableImpl(rootToken);
            addStreamsToList(root, streams, transaction);

            transaction.commit(false);
        }
        catch (Exception e)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.readAllStreams", "1:1182:1.81.1.6", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught reading in all streams!", e);

            if (transaction != null)
            {
                try
                {
                    // Clean up our ObjectManager work.
                    transaction.backout(false);
                }
                catch (ObjectManagerException ome)
                {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.readAllStreams", "1:1194:1.81.1.6", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught backing out read of existing streams!", ome);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readAllStreams");
            throw new PersistenceException("Unexpected exception caught reading in all streams: "+e.getMessage(), e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readAllStreams", streams);
        return streams;
    }

    /**
     * Adds PersistableImpls for streams contained within the supplied stream to the
     * list. Does not add the stream itself (the caller does this if required)
     *
     * @param stream     The root stream to find all sublists from.
     * @param allStreams The list to add any found streams to.
     * @param transaction
     *                   The transaction to use for traversing the lists.
     *
     * @exception ObjectManagerException
     */
    private void addStreamsToList(PersistableImpl stream, List allStreams, Transaction transaction) throws PersistenceException, ObjectManagerException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addStreamsToList", new Object[]{"Stream="+stream, "Transaction="+transaction});

        try
        {
            Token streamListToken = stream.getMetaData().getStreamListToken();
            ConcurrentLinkedList streamsList = (ConcurrentLinkedList)streamListToken.getManagedObject();

            com.ibm.ws.objectManager.Iterator listIterator = streamsList.entryIterator();
            while (listIterator.hasNext(transaction))
            {
                com.ibm.ws.objectManager.List.Entry entry = (com.ibm.ws.objectManager.List.Entry)listIterator.next(transaction);
                Token token = entry.getValue();

                // Defect 358126
                // If the storage strategy for an ItemStream is STORE_MAYBE
                // then we need to clean it up at startup as it's contents
                // will have been removed from the Temporary object store.
                PersistableImpl item = new PersistableImpl(token);

                if (item.getStorageStrategy() == AbstractItem.STORE_MAYBE)
                {
                    entry.remove(transaction);

                    item.removeFromStore(transaction);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "STORE_MAYBE Stream cleaned-up at restart.");
                }
                else
                {
                    allStreams.add(item);

                    if (item.getTupleType() == TupleTypeEnum.ITEM_STREAM)
                    {
                        addStreamsToList(item, allStreams, transaction);
                    }
                }
            }
        }
        catch (PersistenceException pe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.addStreamsToList", "1:1260:1.81.1.6", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "PersistenceException caught cleaning up STORE_MAYBE ItemStream!", pe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addStreamsToList");
            throw pe;
        }
        catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.addStreamsToList", "1:1267:1.81.1.6", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught building stream list from object store!", ome);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addStreamsToList");
            throw ome;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addStreamsToList");
    }

    public List readIndoubtXIDs() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readIndoubtXIDs");

        // Defect 363755
        if (!_available)
        {
            MessageStoreUnavailableException msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readIndoubtXIDs");
            throw msue;
        }

        // TODO: can we cache this list? Are we going to be
        // called at any time other than startup? If not we
        // can read this list once and use it for all queries
        // during recovery.
        List indoubtList = new java.util.LinkedList();

        try
        {
            Iterator iterator = _objectManager.getTransactionIterator();

            while (iterator.hasNext())
            {
                Transaction tran = (Transaction)iterator.next();
                if (tran.getState() == Transaction.statePreparedPersistent)
                {
                    byte[] xid = tran.getXID();
                    if (xid != null)
                    {
                        indoubtList.add(new PersistentTranId(xid));
                    }
                    else
                    {
                        // What's going on? We should have an XID!
                        PersistenceException pe = new PersistenceException("No XID found in prepared transaction!");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "No XID found in prepared transaction!");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readIndoubtXIDs");
                        throw pe;
                    }
                }
            }
        }
        catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.readIndoubtXids", "1:1322:1.81.1.6", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught reading indoubt XIDs!", ome);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readIndoubtXIDs");
            throw new PersistenceException("Unexpected exception caught reading indoubt XIDs: "+ome.getMessage(), ome);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readIndoubtXIDs", "return="+indoubtList);
        return indoubtList;
    }

    /**
     * Not only do we identify any streams with indoubt Items on in this
     * method but we also iterate through the indoubt Items to update
     * them with any state they need after restart. This mainly affects
     * deleted Items as they need to rebuild their logically deleted
     * state as the ObjectManager does not persist their final state
     * during prepare so we get back an Item that is not yet marked
     * logically deleted.
     *
     * @return
     * @exception PersistenceException
     */
    public Set identifyStreamsWithIndoubtItems() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "identifyStreamsWithIndoubtItems");

        // Defect 363755
        if (!_available)
        {
            MessageStoreUnavailableException msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "identifyStreamsWithIndoubtItems");
            throw msue;
        }

        Set set = new java.util.HashSet();

        try
        {
            // Read set of indoubt transactions
            Iterator iterator = _objectManager.getTransactionIterator();

            while (iterator.hasNext())
            {
                Transaction tran = (Transaction)iterator.next();

                if (tran.getState() == Transaction.statePreparedPersistent)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Indoubt Transaction: "+tran);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "{");

                    // Get the list of tokens involved in this tran
                    Iterator items = tran.getIncludedManagedObjects().iterator();
                    while (items.hasNext())
                    {
                        ManagedObject mo = (ManagedObject)items.next();

                        if ((mo != null) && (mo instanceof PersistableMetaData))
                        {
                            PersistableMetaData metaData = (PersistableMetaData)mo;

                            if (metaData.getTupleType() == TupleTypeEnum.ITEM || metaData.getTupleType() == TupleTypeEnum.ITEM_REFERENCE)
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Item Found: "+metaData);

                                // What stream does this Item belong to?
                                // Add it to our list.
                                set.add(Long.valueOf(metaData.getStreamId()));
                            }
                            else
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Item Stream Found: "+metaData);
                            }

                            // Is this a delete?
                            if (metaData.getState() == ManagedObject.stateToBeDeleted)
                            {
                                // If so we need to tell it which XID it is
                                // associated with.
                                // NOTE: We are playing a bit fast and loose with the
                                // ObjectStore here as this change in the persistent
                                // data hasn't been surrounded with a lock/replce. this
                                // shouldn't be a problem though as when this transaction
                                // is committed the item will be gone.
                                metaData.setTransactionId(tran.getXID());
                                metaData.setLogicallyDeleted(true);
                            }
                        }
                        else
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Managed Object Found: "+mo);
                        }
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "}");
                }
            }
        }
        catch (ObjectManagerException ome)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.identifyStreamsWithIndoubtItems", "1:1422:1.81.1.6", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught identifying indoubt streams!", ome);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "identifyStreamsWithIndoubtItems");
            throw new PersistenceException("Unexpected exception caught identifying indoubt streams: "+ome.getMessage(), ome);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "identifyStreamsWithIndoubtItems", "return="+set);
        return set;
    }

    public Set identifyStreamsWithExpirableItems() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "identifyStreamsWithExpirableItems");

        Set set = new java.util.HashSet();

        List      streams = readAllStreams();
        Iterator iterator = streams.iterator();
        while (iterator.hasNext())
        {
            PersistableImpl stream = (PersistableImpl)iterator.next();

            if (stream.getMetaData().containsExpirables())
            {
                set.add(Long.valueOf(stream.getUniqueId()));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "identifyStreamsWithExpirableItems", "return="+set);
        return set;
    }

    /*************************************************************************/
    /*                   PersistentMessageStore Implementation               */
    /*************************************************************************/


    /*************************************************************************/
    /*                     PersistenceManager Implementation                 */
    /*************************************************************************/

    /**
     * Perform the persistent message store operations required to prepare
     * a transaction.
     *
     * @param transaction The transaction to prepare
     * @throws SevereMessageStoreException 
     */
    public void prepare(PersistentTransaction transaction) throws PersistenceException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "prepare", "Transaction="+transaction);

        // Defect 363755
        if (!_available)
        {
            MessageStoreUnavailableException msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
            throw msue;
        }

        //Get the task list from the transaction
        TaskList taskList = (TaskList) transaction.getWorkList();

        PersistentTranId       xid = transaction.getPersistentTranId();
        TransactionState tranState = transaction.getTransactionState();
        int          numberOfTasks = taskList.countLinks();

        boolean requiresSyncPersistence = false;

        // Defect 530772
        boolean requiresSpillPersistence = false;

        // Defect 298584
        // Batching context will only be created if we
        // have synchronous work to do.
        BatchingContext syncBatch = null;
        List             syncList = null;

        if (taskList.hasStoreAlways() || taskList.hasStoreEventually() || taskList.hasStoreMaybe())
        {
            // We have synchronous work to do so
            // we need to build up a list of tasks.
            Iterator iterator = taskList.iterator();
            while (iterator.hasNext())
            {
                Operation          task = (Operation) iterator.next();
                Persistable persistable = task.getPersistable();

                // Defect 451518
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Preparing Task: "+task);

                if (persistable != null && persistable.requiresPersistence())
                {
                    int     storageStrategy = persistable.getStorageStrategy();
                    TupleTypeEnum tupleType = persistable.getTupleType();

                    // Feature SIB0112i.ms.1
                    // We only need to persist our synchronous work at this point. If our
                    // transaction prepares successfully then at commit time we can make
                    // sure our STORE_MAYBE work is persisted.

                    // Defect 530772
                    // Are we a STORE_MAYBE Item? If so then we can be dispatched to the
                    // spill dispatcher. If not then we need to be persisted synchronously.
                    // This is to avoid adding complication to the spill dispatcher as it
                    // does not worry about the order in which things are persisted so an
                    // Item on a stream can be hardened to disk before the stream. This will
                    // not work in the filestore case.
                    if (tupleType != TupleTypeEnum.ITEM_STREAM &&
                        tupleType != TupleTypeEnum.REFERENCE_STREAM &&
                        storageStrategy == AbstractItem.STORE_MAYBE)
                    {
                        if (requiresSpillPersistence == false)
                        {
                            requiresSpillPersistence = true;
                        }
                    }
                    // If not we are synchronous.
                    else
                    {
                        // Defect 298584
                        // Only create our batching context once we know for sure
                        // that we have some synchronous work to do.
                        if (requiresSyncPersistence == false)
                        {
                            syncList  = new ArrayList(numberOfTasks);
                            syncBatch = new BatchingContextImpl(_objectManager, _permanentStore, transaction);
                            transaction.setBatchingContext(syncBatch);
                            requiresSyncPersistence = true;
                        }

                        //Associate the global tran id with the persistable
                        persistable.setPersistentTranId(xid);

                        task.persist(syncBatch, tranState);
                        syncList.add(persistable);
                    }
                }
            }
        }


        // Defect 530772
        // Check the health of the spill dispatcher to minimise the risk
        // of splitting a transaction.
        if (requiresSpillPersistence)
        {
            if (!_spillDispatcher.isHealthy())
            {
                // Defect 345250
                // The dispatcher is not currently accepting new work as it has
                // hit a problem. Details of the problem should have been output
                // to the logs by the dispatcher thread at the point where it
                // occurred.
                PersistenceException pe = new PersistenceException("SPILL_DISPATCHER_CANNOT_ACCEPT_WORK_SIMS1578");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "The spill dispatcher cannot accept work!", pe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
                throw pe;
            }
        }

        // Feature SIB0112i.ms.1
        // We only have to do some work in prepare if we have
        // synchronous work to add to the file store.
        if (requiresSyncPersistence)
        {
            try
            {
                // As this is a prepare we use the addIndoubtXid()
                // method to inform the batching context that we only
                // wish to prepare during this executeUpdate() call.
                syncBatch.addIndoubtXID(xid);

                // Submit the batch and trigger the prepare in the
                // object manager.
                syncBatch.executeBatch();

                // If we get to this point then the prepare was successful, so we need to
                // notify the cache layer
                Iterator iterator = syncList.iterator();
                while (iterator.hasNext())
                {
                    Tuple tuple = (Tuple)iterator.next();

                    // Both these methods need to be called to indicate to the cache
                    // that it can soften its in-memory reference to the persistable
                    tuple.persistableOperationBegun();
                    tuple.persistableOperationCompleted();
                }
            }
            catch (PersistenceException pe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.prepare", "1:1615:1.81.1.6", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught preparing transaction in object store!", pe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
                throw pe;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
    }

    /**
     * Perform the persistent message store operations required to commit
     * a transaction, either one-phase or two-phase. The latter will of
     * course be a prepared transaction.
     *
     * @param transaction The transaction to commit
     * @param onePhase <tt>true</tt> for one-phase, <tt>false</tt> for the
     *                 second phase of a two-phase commit
     * @throws SevereMessageStoreException 
     */
    public void commit(PersistentTransaction transaction, boolean onePhase) throws PersistenceException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commit", new Object[]{"Transaction="+transaction, "OnePhase="+onePhase});

        // Defect 363755
        if (!_available)
        {
            MessageStoreUnavailableException msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            throw msue;
        }

        // Get the task list from the transaction
        TaskList taskList = (TaskList) transaction.getWorkList();

        PersistentTranId       xid = transaction.getPersistentTranId();
        TransactionState tranState = transaction.getTransactionState();
        int          numberOfTasks = taskList.countLinks();

        boolean requiresSyncPersistence  = false;
        boolean requiresSpillPersistence = false;

        BatchingContext  syncBatch = null;

        List         syncList = null;
        List        spillList = null;

        if (onePhase)
        {
            // In a one-phase commit we need to check for
            // STORE_ALWAYS,STORE_EVENTUALLY and STORE_MAYBE
            // all at the same time as everything is handled
            // in a single phase.
            if (taskList.hasStoreAlways() || taskList.hasStoreEventually() || taskList.hasStoreMaybe())
            {
                // Scan through the list finding out what kind of persistence we need.
                // This pass will build up lists of our synchronous work and possible
                // spill work.
                Iterator iterator = taskList.iterator();
                while (iterator.hasNext())
                {
                    Operation   task        = (Operation) iterator.next();
                    Persistable persistable = task.getPersistable();

                    // Defect 451518
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Committing Task(1PC): "+task);

                    if (persistable != null && persistable.requiresPersistence())
                    {
                        int     storageStrategy = persistable.getStorageStrategy();
                        TupleTypeEnum tupleType = persistable.getTupleType();

                        // Feature SIB0112i.ms.1
                        // Are we a STORE_MAYBE Item? If so then we can be directly added to the
                        // temporary store. This allows the ObjectManager to manage the time at
                        // which the transaction data is written to the temporary store (usually
                        // at the next available checkpoint) and removes the need for the spill
                        // dispatcher.
                        if (tupleType != TupleTypeEnum.ITEM_STREAM &&
                            tupleType != TupleTypeEnum.REFERENCE_STREAM &&
                            storageStrategy == AbstractItem.STORE_MAYBE)
                        {
                            // Feature SIB0112i.ms.1
                            if (requiresSpillPersistence == false)
                            {
                                spillList  = new ArrayList(numberOfTasks);
                                requiresSpillPersistence = true;
                            }

                            // Defect 530772
                            // Add the task to the list we will pass
                            // to the dispatcher.
                            spillList.add(task);
                        }
                        // If not we are synchronous.
                        else
                        {
                            // Defect 298584
                            // Only create our batching context once we know for sure
                            // that we have some synchronous work to do.
                            if (requiresSyncPersistence == false)
                            {
                                syncList  = new ArrayList(numberOfTasks);
                                syncBatch = new BatchingContextImpl(_objectManager, _permanentStore);
                                requiresSyncPersistence = true;
                            }

                            task.persist(syncBatch, tranState);
                            syncList.add(persistable);
                        }
                    }
                }
            }
        }
        //two-phase commit
        else
        {
            if (taskList.hasStoreAlways() || taskList.hasStoreEventually() || taskList.hasStoreMaybe())
            {
                Iterator iterator = taskList.iterator();
                while (iterator.hasNext())
                {
                    Operation   task        = (Operation) iterator.next();
                    Persistable persistable = task.getPersistable();

                    // Defect 451518
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Committing Task(2PC): "+task);

                    if (persistable != null && persistable.requiresPersistence())
                    {
                        int     storageStrategy = persistable.getStorageStrategy();
                        TupleTypeEnum tupleType = persistable.getTupleType();

                        // Feature SIB0112i.ms.1
                        // Are we a STORE_MAYBE Item? If so then we can be directly added to the
                        // temporary store. This allows the ObjectManager to manage the time at
                        // which the tramnsaction data is written to the temporary store (usually
                        // at the next available checkpoint) and removes the need for the spill
                        // dispatcher.
                        if (tupleType != TupleTypeEnum.ITEM_STREAM &&
                            tupleType != TupleTypeEnum.REFERENCE_STREAM &&
                            storageStrategy == AbstractItem.STORE_MAYBE)
                        {
                            // Feature SIB0112i.ms.1
                            // If we have some STORE_MAYBE work to do then
                            // we can now add it to the file store's
                            // temporary store.
                            if (requiresSpillPersistence == false)
                            {
                                spillList  = new ArrayList(numberOfTasks);
                                requiresSpillPersistence = true;
                            }

                            // Defect 530772
                            // Add the task to the list we will pass
                            // to the dispatcher.
                            spillList.add(task);
                        }
                        // If not we are synchronous.
                        else
                        {
                            // Defect 298584
                            // We only need to check on our batching context once
                            // we know that we have some synchronous work from
                            // the first phase that needs completing.
                            if (requiresSyncPersistence == false)
                            {
                                // Get the batching context for this two-phase operation.
                                // One must exist from the prepare phase if it does not
                                // then we need to check if this transaction has been
                                // recovered or not.
                                syncBatch = transaction.getBatchingContext();
                                if (syncBatch == null)
                                {
                                    // We need to see if this is the second phase of
                                    // a recovered indoubt transaction if not we can
                                    // throw an exception.
                                    Transaction tran = null;
                                    try
                                    {
                                        tran = _objectManager.getTransactionByXID(xid.toByteArray());
                                    }
                                    catch (ObjectManagerException ome)
                                    {
                                        com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.commit", "1:1800:1.81.1.6", this);
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught requesting recovered transaction from object manager!", ome);
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
                                        throw new PersistenceException("Exception caught requesting recovered transaction from object manager: "+ome.getMessage(), ome);
                                    }

                                    if (tran != null)
                                    {
                                        // We are completing an indoubt so we need to
                                        // create a batching context to use.
                                        syncBatch = new BatchingContextImpl(_objectManager, _permanentStore, tran);
                                        transaction.setBatchingContext(syncBatch);
                                    }
                                    else
                                    {
                                        PersistenceException pe = new PersistenceException("No existing batching context found during two-phase commit!");
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "No existing batching context found during two-phase commit!", pe);
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
                                        throw pe;
                                    }
                                }
                                requiresSyncPersistence = true;
                            }
                        }
                    }
                }
            }
        }

        // Defect 530772
        // Check the health of the spill dispatcher to minimise the risk
        // of splitting a transaction.
        if (requiresSpillPersistence)
        {
            if (!_spillDispatcher.isHealthy())
            {
                // Defect 345250
                // The dispatcher is not currently accepting new work as it has
                // hit a problem. Details of the problem should have been output
                // to the logs by the dispatcher thread at the point where it
                // occurred.
                PersistenceException pe = new PersistenceException("SPILL_DISPATCHER_CANNOT_ACCEPT_WORK_SIMS1578");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "The spill dispatcher cannot accept work!", pe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
                throw pe;
            }
        }

        if (requiresSyncPersistence)
        {
            try
            {
                // Tell the batch that we need to commit during the
                // next executeBatch call.
                syncBatch.updateXIDToCommitted(null);

                //Submit the batch
                syncBatch.executeBatch();

                // If we are one-phase then we need to inform the cache
                // layer of the successful writes. If we are two-phase
                // this will have been done at prepare time.
                if (onePhase)
                {
                    //If we get to this point then the commit was successful, so we need to
                    //notify the cache layer
                    Iterator iterator = syncList.iterator();
                    while (iterator.hasNext())
                    {
                        Tuple tuple = (Tuple)iterator.next();

                        //Both these methods need to be called to indicate to the cache
                        //that it can soften its in-memory reference to the persistable
                        tuple.persistableOperationBegun();
                        tuple.persistableOperationCompleted();
                    }
                }
            }
            catch (PersistenceException pe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.commit", "1:1880:1.81.1.6", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught committing transaction work in permanent store!", pe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
                throw pe;
            }
        }

        // Defect 530772
        // Always submit any spill updates to the dispatcher for processing.
        if (requiresSpillPersistence)
        {
            _spillDispatcher.dispatch(spillList, transaction, false);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
    }

    /**
     * Perform the persistent message store operations required to rollback a
     * <b>prepared</b> transaction. The operations performed undo the effects
     * of a prepared transaction, rather than recording the rollback of an
     * active transaction which will not have written anything to the
     * persistent message store anyway.
     *
     * @param transaction The prepared transaction to roll back
     */
    public void rollback(PersistentTransaction transaction) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "rollback", "Transaction="+transaction);

        // Defect 363755
        if (!_available)
        {
            MessageStoreUnavailableException msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
            throw msue;
        }

        TaskList    taskList = (TaskList) transaction.getWorkList();
        PersistentTranId xid = transaction.getPersistentTranId();

        // Defect 292594
        // We only need to do any work if we have STORE_ALWAYS or
        // STORE_EVENTUALLY. STORE_MAYBE work will not have been done
        // in prepare so it doesn't need rolling back.
        if (taskList.hasStoreAlways() || taskList.hasStoreEventually())
        {
            // Defect 298584
            // We only need to get a bacthing context if did some
            // synchronous work during prepare.

            // Get the batching context for this two-phase operation.
            // One must exist from the prepare phase. If it does not
            // exist in the MS transaction then it must exist in the
            // object manager indoubt list. 
            // PM31431 if one does not exist then we can only assume 
            // we did not get as far as doing the prepare and so just
            // accept the rollback
            
            BatchingContext batch = transaction.getBatchingContext();
            if (batch == null)
            {
                Transaction tran = null;
                try
                {
                    tran = _objectManager.getTransactionByXID(xid.toByteArray());
                }
                catch (ObjectManagerException ome)
                {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.rollback", "1:1950:1.81.1.6", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught requesting recovered transaction from object manager!", ome);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
                    throw new PersistenceException("Exception caught requesting recovered transaction from object manager: "+ome.getMessage(), ome);
                }

                if (tran != null)
                {
                    batch = new BatchingContextImpl(_objectManager, _permanentStore, tran);
                }
                else
                {
                    // no transaction could be found which must mean we did not manage to get to the prepare.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Transaction could not be found");
                }
            }

            if (batch != null)
            {
                try
                {
                    // As this is a rollback we use the updateXIDToRolledBack()
                    // method to inform the batching context that we now
                    // wish to backout all existing work during the next
                    // executeBatch() call.
                    batch.updateXIDToRolledback(xid);

                    // Submit the batch and trigger the rollback in the
                    // object manager.
                    batch.executeBatch();

                    //If we get to this point then the rollback was successful.
                    //However, we don't need to invoke the OperationBegun/Completed
                    //callbacks on the Persistable. The rollback can only be for
                    //addition and removal of Persistables, not for updates. In
                    //the case of a rollback of an addition, the Persistable will
                    //be removed from the cache which will no longer be interested
                    //in its persistent representation. In the case of a rollback
                    //of a removal, the Persistable will be re-instated in the cache
                    //and the disk representation is exactly the same as it was
                    //before the rollback.
                }
                catch (PersistenceException pe)
                {
                    com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.rollback", "1:1994:1.81.1.6", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught rolling back transction in object store!", pe);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
                    throw pe;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
    }

    public boolean supports1PCOptimisation()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "supports1PCOptimisation");
            SibTr.exit(this, tc, "supports1PCOptimisation", "return=false");
        }
        return false;
    }

    public void beforeCompletion(PersistentTransaction transaction) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "beforeCompletion", "Transaction="+transaction);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Connection sharing with JDBC not supported by FileStore!");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "beforeCompletion");
        throw new PersistenceException("Connection sharing with JDBC not supported by FileStore!");
    }

    public void afterCompletion(PersistentTransaction transaction, boolean committed)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "afterCompletion", new Object[]{"Transaction="+transaction, "DidCommit="+committed});
            SibTr.exit(this, tc, "afterCompletion");
        }
    }

    /*************************************************************************/
    /*                     PersistenceManager Implementation                 */
    /*************************************************************************/

    public UniqueKeyGenerator getUniqueKeyGenerator(String name, int range)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getUniqueKeyGenerator", new Object[]{"Name="+name, "Range="+range});

        UniqueKeyGenerator retval = null;

        try
        {
            retval = _uniqueKeyManager.createUniqueKeyGenerator(name, range);
        }
        catch (PersistenceException pe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.getUniqueKeyGenerator", "1:2089:1.81.1.6", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught creating new unique key generator!", pe);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getUniqueKeyGenerator", "return="+retval);
        return retval;
    }

    /**
     * This method checks the ME_UUID stored in the permanent object store for this messaging engine
     * to see if it matches that which we retrieve from the admin object. If the ME_UUID matches it
     * then updates the INC_UUID to one that is associated with this current incarnation.
     *
     * @return <UL>
     *         <LI>true - If all checks and updates are successful</LI>
     *         <LI>false - If a check or update fails</LI>
     *         </UL>
     */
    public boolean checkAndUpdateMEOwner(MELockOwner owner)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "checkAndUpdateMEOwner", "Owner="+owner);

        // Venu mock mock disableUUIDCheck for now in this version of Liberty profile.
        boolean disableUUIDCheck = true;
        /*
        // set always true to 
        // Defect 601995
        // Check to see if UUID checking has been disabled.
        String disableUUIDCheckStr = _ms.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_DISABLE_UUID_CHECK, 
                                                     MessageStoreConstants.PROP_OBJECT_MANAGER_DISABLE_UUID_CHECK_DEFAULT);

        boolean disableUUIDCheck = Boolean.parseBoolean(disableUUIDCheckStr); */


        boolean success = true;

        if (disableUUIDCheck)
        {
            // Start regardless of the contents of our owner objects
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Owner checking skipped as "+MessageStoreConstants.PROP_OBJECT_MANAGER_DISABLE_UUID_CHECK+"=true");
        }
        else
        {
            MEStoredOwner storedOwner = null;
            Transaction   transaction = null;
    
            SibTr.info(tc, "FILE_STORE_LOCK_ATTEMPTING_SIMS1564", new Object[] {owner.getMeUUID(), owner.getIncUUID()});
    
            try
            {
                transaction = _objectManager.getTransaction();
                Token storedOwnerToken = _anchor.getMEOwnerToken();
    
                if (storedOwnerToken != null)
                {
                    storedOwner = (MEStoredOwner)storedOwnerToken.getManagedObject();
    
                    SibTr.info(tc, "FILE_STORE_LOCK_ONE_OWNER_SIMS1566", new Object[] {storedOwner.getMeUUID(), storedOwner.getIncUUID()});
                    // F008622 -start
                    if(_ms.getProperty(MessageStoreConstants.START_MODE, MessageStoreConstants.DEFAULT_START_MODE).equalsIgnoreCase("RECOVERY"))
                    {
                        owner=new MELockOwner(storedOwner.getMeUUID(),storedOwner.getIncUUID(),storedOwner.getVersion(),storedOwner.getMigrationVersion(),"default");
                    }
                    // F008622 end
                    // If our ME_UUID matches that found then we need
                    // to update our incarnation id.
                    if (owner.getMeUUID().equals(storedOwner.getMeUUID()))
                    {
                        if (owner.getVersion() == storedOwner.getVersion())
                        {
                            transaction.lock(storedOwner);
    
                            storedOwner.setIncUUID(owner.getIncUUID());
    
                            transaction.replace(storedOwner);
                        }
                        else
                        {
                            // Different Version
                            SibTr.error(tc, "FILE_STORE_LOCK_VERSION_DOESNT_MATCH_SIMS1562", new Object[]{Integer.valueOf(owner.getVersion()), Integer.valueOf(storedOwner.getVersion())});
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Different ME version found in file store!", "ME Version="+owner.getVersion()+", ME Version(FS)="+storedOwner.getVersion());
    
                            // Defect 496893
                            // We know that no instance of this ME will ever work with these
                            // files so we report a global error to stop us from failing over
                            // to a new instance.
                            _ms.reportGlobalError();
    
                            success = false;
                        }
                    }
                    else
                    {
                        // Different ME_UUID
                        SibTr.error(tc, "FILE_STORE_LOCK_MEUUID_DOESNT_MATCH_SIMS1561", new Object[]{owner.getMeUUID(), storedOwner.getMeUUID()});
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Different ME unique id found in file store!", "ME_UUID="+owner.getMeUUID()+", ME_UUID(FS)="+storedOwner.getMeUUID());
    
                        // Defect 496893
                        // We know that no instance of this ME will ever work with these
                        // files so we report a global error to stop us from failing over
                        // to a new instance.
                        _ms.reportGlobalError();
    
                        success = false;
                    }
                }
                else
                {
                    // We are starting with an empty object store
                    // so we need to add our ME owner information
                    // to the anchor.
    
                    SibTr.info(tc, "FILE_STORE_LOCK_NO_OWNER_SIMS1565");
    
                    storedOwner      = new MEStoredOwner(owner.getMeUUID(), owner.getIncUUID(), 1, 0);
                    storedOwnerToken = _permanentStore.allocate(storedOwner);
    
                    transaction.lock(_anchor);
    
                    _anchor.setMEOwnerToken(storedOwnerToken);
    
                    transaction.replace(_anchor);
    
                    transaction.add(storedOwner);
                }
    
                transaction.commit(false);
            }
            catch (ObjectManagerException ome)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(ome, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.checkAndUpdateMEOwner", "1:2214:1.81.1.6", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught updating ME Owner information!", ome);
    
                if (transaction != null)
                {
                    try
                    {
                        // Clean up our ObjectManager work.
                        transaction.backout(false);
                    }
                    catch (ObjectManagerException e)
                    {
                        com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.checkAndUpdateMEOwner", "1:2226:1.81.1.6", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled ()) SibTr.event(this, tc, "Exception caught backing out update of ME Owner information!", e);
                    }
                }
    
                // Defect 496893
                // We don't know what problem we've had but we can't guarantee that we have
                // successfully checked our owner information so we should ask to be failed
                // over to a new instance to see if we have more luck next time.
                _ms.reportLocalError();
    
                success = false;
            }
    
            if (success)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ME Owner information in ObjectStore updated successfully: "+storedOwner);
    
                SibTr.info(tc, "FILE_STORE_LOCK_ACQUIRED_SIMS1563", new Object[] {storedOwner.getMeUUID(), storedOwner.getIncUUID()});
            }
            else
            {
                SibTr.error(tc, "CANNOT_OBTAIN_FILE_STORE_LOCK_SIMS1567", new Object[]{owner.getMeName()});
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "checkAndUpdateMEOwner", "return="+success);
        return success;
    }

    /**
     * Checks to see if the supplied path exists and if not creates the directory
     * structure required to store the file store files.
     *
     * @param path   The file store file path as provided from the
     *               admin console
     *
     * @return
     */
    private String createDirectoryPath(String path)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createDirectoryPath", "Path="+path);

        StringBuilder directoryPath = new StringBuilder("");

        if ((path != null) && (path.length() > 0))
        {
            // Defect 287722
            // Are we on unix? If so then we need
            // to make sure we replace the first
            // slash if present as it will be
            // removed by the tokenizer.
            if (path.charAt(0) == '/')
            {
                directoryPath.append("/");
            }

            // Are we UNC format? If so then we need
            // to make sure we replace the first
            // two backslashes as they will be
            // removed by the tokenizer.
            if (path.charAt(0) == '\\' && path.charAt(1) == '\\')
            {
                directoryPath.append("\\\\");
            }

            final StringTokenizer tokenizer = new StringTokenizer(path, "\\/");

            while (tokenizer.hasMoreTokens())
            {
                final String pathChunk = tokenizer.nextToken();

                directoryPath.append(pathChunk);

                // Check to see if the directory exists.
                File test = new File(directoryPath.toString());
                if (!test.exists())
                {
                    // Create the directory.
                    test.mkdir();
                }

                if (tokenizer.hasMoreTokens())
                {
                    directoryPath.append(File.separator);
                }
            }
        }

        String retval = directoryPath.toString();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createDirectoryPath", retval);
        return retval;
    }

    /**
     * Sets the sizes of the object manager's log and object stores following a restart.
     * Reports any changes and warnings using console messages.
     *
     * @param coldStart
     * @param minimumPermanentStoreSize
     * @param maximumPermanentStoreSize
     * @param isPermanentStoreSizeUnlimited
     * @param minimumTemporaryStoreSize
     * @param maximumTemporaryStoreSize
     * @param isTemporaryStoreSizeUnlimited
     * @param logSize
     *
     * @exception ObjectManagerException
     */
    private void setFileSizesAfterRestart(boolean coldStart,
                                          long minimumPermanentStoreSize, long maximumPermanentStoreSize, boolean isPermanentStoreSizeUnlimited,
                                          long minimumTemporaryStoreSize, long maximumTemporaryStoreSize, boolean isTemporaryStoreSizeUnlimited,
                                          long logSize) throws ObjectManagerException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setFileSizesAfterRestart", new Object[]{"ColdStart="+coldStart, "LogSize="+logSize,
            "MinimumPermanentStoreSize="+minimumPermanentStoreSize, "MaximumPermanentStoreSize="+maximumPermanentStoreSize, "IsPermanentStoreSizeUnlimited="+isPermanentStoreSizeUnlimited,
            "MinimumTemporaryStoreSize="+minimumTemporaryStoreSize, "MaximumTemporaryStoreSize="+maximumTemporaryStoreSize, "IsTemporaryStoreSizeUnlimited="+isTemporaryStoreSizeUnlimited});

        try
        {
            // The recommendation is to set the log file size
            // before the store file sizes when cold-starting
            if (coldStart)
            {
                long currentLogSize = _objectManager.getLogFileSize();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "The current size of the log file is " + Long.valueOf(currentLogSize) + " bytes. The size in the configuration information of the log file is " + Long.valueOf(logSize) + " bytes.");

                if (currentLogSize != logSize)
                {
                    // Defect 326589
                    // Check the values provided to us to make sure we are attempting
                    // sensible modifications. Each check is only worth doing if the matching
                    // store size unlimited flag is set to false. If this is true then we are
                    // going to ignore the maximum store size parameters anyway.
                    if ((!isPermanentStoreSizeUnlimited && (logSize > maximumPermanentStoreSize)) || // Log larger than permanent store
                        (!isTemporaryStoreSizeUnlimited && (logSize > maximumTemporaryStoreSize)))   // Log larger than temporary store
                    {
                        SibTr.warning(tc, "FILE_STORE_LOG_SIZE_CHANGE_PREVENTED_SIMS1548");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Log size not changed!");
                    }
                    else
                    {
                        _objectManager.setLogFileSize(logSize);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Log size changed");
                    }
                }
            }

            // Change the permanent store file sizes.
            SingleFileObjectStore store = (SingleFileObjectStore)_permanentStore;

            long currentPermanentStoreUsed        = store.getStoreFileUsed();
            long currentPermanentStoreSize        = store.getStoreFileSize();
            long currentMinimumPermanentStoreSize = store.getMinimumStoreFileSize();
            long currentMaximumPermanentStoreSize = store.getMaximumStoreFileSize();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(this, tc, "currentPermanentStoreUsed        = " + currentPermanentStoreUsed);
                SibTr.debug(this, tc, "currentPermanentStoreSize        = " + currentPermanentStoreSize);
                SibTr.debug(this, tc, "currentMinimumPermanentStoreSize = " + currentMinimumPermanentStoreSize);
                SibTr.debug(this, tc, "currentMaximumPermanentStoreSize = " + currentMaximumPermanentStoreSize);
                // Defect 342044
                // Output the current size of the data in the permanent store
                SibTr.debug(this, tc, "The data in the permanent store file occupies " + Long.valueOf(currentPermanentStoreUsed) + " bytes.");
                // Output the current file size limits.
                if (currentMaximumPermanentStoreSize != MAXIMUM_STORE_FILE_SIZE)
                {
                    SibTr.debug(this, tc, "The current minimum reserved size of the permanent store file is " + Long.valueOf(currentMinimumPermanentStoreSize) + " bytes. The current maximum size is " + Long.valueOf(currentMaximumPermanentStoreSize) + " bytes.");
                }
                else
                {
                    SibTr.debug(this, tc, "The current minimum reserved size of the permanent store file is " + Long.valueOf(currentMinimumPermanentStoreSize) + " bytes. The current maximum size is unlimited");
                }

            }

            // Defect 326589
            // Check the values provided to us to make sure we are attempting
            // sensible modifications. Each check is only worth doing if the matching
            // store size unlimited flag is set to false. If this is true then we are
            // going to ignore the maximum store size parameters anyway.
            if ((!isPermanentStoreSizeUnlimited && (minimumPermanentStoreSize > maximumPermanentStoreSize)))  // Permanent store minimum larger than maximum
            {
                SibTr.info(tc, "FILE_STORE_PERMANENT_STORE_SIZE_CONFIGURATION_INFO_SIMS1553", new Object[] {Long.valueOf(minimumPermanentStoreSize), Long.valueOf(maximumPermanentStoreSize)});
                SibTr.warning(tc, "FILE_STORE_STORE_SIZE_CHANGE_PREVENTED_SIMS1549");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Permanent store size not changed!");
            }
            else
            {
                if ((currentMinimumPermanentStoreSize != minimumPermanentStoreSize) ||                                     /* Minimum is not the same as the current minimum.     */
                    (!isPermanentStoreSizeUnlimited && (currentMaximumPermanentStoreSize != maximumPermanentStoreSize)) || /* Maximum is not the same as current limited maximum. */
                    (isPermanentStoreSizeUnlimited && (currentMaximumPermanentStoreSize != MAXIMUM_STORE_FILE_SIZE)))      /* Maximum is not already set to unlimited.            */
                {
                    if (!isPermanentStoreSizeUnlimited)
                    {
                        SibTr.info(tc, "FILE_STORE_PERMANENT_STORE_SIZE_CONFIGURATION_INFO_SIMS1553", new Object[] {Long.valueOf(minimumPermanentStoreSize), Long.valueOf(maximumPermanentStoreSize)});
                        store.setStoreFileSize(minimumPermanentStoreSize, maximumPermanentStoreSize);
                    }
                    else
                    {
                        SibTr.info(tc, "FILE_STORE_PERMANENT_STORE_SIZE_CONFIGURATION_INFO_UNLIMITED_SIMS1554", new Object[] {Long.valueOf(minimumPermanentStoreSize)});
                        store.setStoreFileSize(minimumPermanentStoreSize, MAXIMUM_STORE_FILE_SIZE);
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Permanent Store size changed");
                }
            }

            // Change the temporary store file sizes.
            store = (SingleFileObjectStore)_temporaryStore;

            long currentTemporaryStoreUsed        = store.getStoreFileUsed();
            long currentTemporaryStoreSize        = store.getStoreFileSize();
            long currentMinimumTemporaryStoreSize = store.getMinimumStoreFileSize();
            long currentMaximumTemporaryStoreSize = store.getMaximumStoreFileSize();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(this, tc, "currentTemporaryStoreUsed        = " + currentTemporaryStoreUsed);
                SibTr.debug(this, tc, "currentTemporaryStoreSize        = " + currentTemporaryStoreSize);
                SibTr.debug(this, tc, "currentMinimumTemporaryStoreSize = " + currentMinimumTemporaryStoreSize);
                SibTr.debug(this, tc, "currentMaximumTemporaryStoreSize = " + currentMaximumTemporaryStoreSize);
                // Defect 342044
                // Output the current size of the data in the temporary store
                SibTr.debug(this, tc, "The data in the temporary store file occupies " + Long.valueOf(currentTemporaryStoreUsed) + " bytes.");

                // Output the current file size limits.
                if (currentMaximumTemporaryStoreSize != MAXIMUM_STORE_FILE_SIZE)
                {
                    SibTr.debug(this, tc, "The current minimum reserved size of the temporary store file is " + Long.valueOf(currentMinimumTemporaryStoreSize) + " bytes. The current maximum size is " + Long.valueOf(currentMaximumTemporaryStoreSize) +" bytes.");
                }
                else
                {
                    SibTr.debug(this, tc, "The current minimum reserved size of the temporary store file is " + Long.valueOf(currentMinimumTemporaryStoreSize) + " bytes. The current maximum size is unlimited.");
                }

            }

            // Defect 326589
            // Check the values provided to us to make sure we are attempting
            // sensible modifications. Each check is only worth doing if the matching
            // store size unlimited flag is set to false. If this is true then we are
            // going to ignore the maximum store size parameters anyway.
            if ((!isTemporaryStoreSizeUnlimited && (minimumTemporaryStoreSize > maximumTemporaryStoreSize)))  // Temporary store minimum larger than maximum
            {
                SibTr.info(tc, "FILE_STORE_TEMPORARY_STORE_SIZE_CONFIGURATION_INFO_SIMS1557", new Object[] {Long.valueOf(minimumTemporaryStoreSize), Long.valueOf(maximumTemporaryStoreSize)});
                SibTr.warning(tc, "FILE_STORE_STORE_SIZE_CHANGE_PREVENTED_SIMS1549");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Temporary store size not changed!");
            }
            else
            {
                if ((currentMinimumTemporaryStoreSize != minimumTemporaryStoreSize) ||                                     /* Minimum is not the same as the current minimum.     */
                    (!isTemporaryStoreSizeUnlimited && (currentMaximumTemporaryStoreSize != maximumTemporaryStoreSize)) || /* Maximum is not the same as current limited maximum. */
                    (isTemporaryStoreSizeUnlimited && (currentMaximumTemporaryStoreSize != MAXIMUM_STORE_FILE_SIZE)))      /* Maximum is not already set to unlimited.            */
                {
                    if (!isTemporaryStoreSizeUnlimited)
                    {
                        SibTr.info(tc, "FILE_STORE_TEMPORARY_STORE_SIZE_CONFIGURATION_INFO_SIMS1557", new Object[] {Long.valueOf(minimumTemporaryStoreSize), Long.valueOf(maximumTemporaryStoreSize)});
                        store.setStoreFileSize(minimumTemporaryStoreSize, maximumTemporaryStoreSize);
                    }
                    else
                    {
                        SibTr.info(tc, "FILE_STORE_TEMPORARY_STORE_SIZE_CONFIGURATION_INFO_UNLIMITED_SIMS1558", new Object[] {Long.valueOf(minimumTemporaryStoreSize)});
                        store.setStoreFileSize(minimumTemporaryStoreSize, MAXIMUM_STORE_FILE_SIZE);
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Temporary Store size changed");
                }
            }

            // The recommendation is to set the log file size
            // after the store file sizes when warm-starting
            if (!coldStart)
            {
                long currentLogSize = _objectManager.getLogFileSize();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "The current size of the log file is " + Long.valueOf(currentLogSize) + " bytes. The size in the configuration information of the log file is " + Long.valueOf(logSize) + " bytes.");

                if (currentLogSize != logSize)
                {
                    // Defect 326589
                    // Check the values provided to us to make sure we are attempting
                    // sensible modifications. Each check is only worth doing if the matching
                    // store size unlimited flag is set to false. If this is true then we are
                    // going to ignore the maximum store size parameters anyway.
                    if ((!isPermanentStoreSizeUnlimited && (logSize > maximumPermanentStoreSize)) || // Log larger than permanent store
                        (!isTemporaryStoreSizeUnlimited && (logSize > maximumTemporaryStoreSize)))   // Log larger than temporary store
                    {
                        SibTr.warning(tc, "FILE_STORE_LOG_SIZE_CHANGE_PREVENTED_SIMS1548");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Log size not changed!");
                    }
                    else
                    {
                        _objectManager.setLogFileSize(logSize);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Log size changed");
                    }
                }
            }
        }
        catch (LogFileSizeTooSmallException lfse)   // This is due to the minimum log size not being big
        {                                           // enough for existing data.
            com.ibm.ws.ffdc.FFDCFilter.processException(lfse, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.setFileSizesAfterRestart", "1:2531:1.81.1.6", this);
            SibTr.warning(tc, "FILE_STORE_LOG_SIZE_CHANGE_PREVENTED_SIMS1548");
        }
        catch (IllegalArgumentException iae)        // This is due to invalid file sizes i.e. min > max
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(iae, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.setFileSizesAfterRestart", "1:2536:1.81.1.6", this);
            SibTr.warning(tc, "FILE_STORE_STORE_SIZE_CHANGE_PREVENTED_SIMS1549");
        }
        catch (StoreFileSizeTooSmallException sfse) // This is due to the minimum store size not being big
        {                                           // enough for existing data.
            com.ibm.ws.ffdc.FFDCFilter.processException(sfse, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.setFileSizesAfterRestart", "1:2541:1.81.1.6", this);
            SibTr.warning(tc, "FILE_STORE_STORE_SIZE_CHANGE_PREVENTED_SIMS1549");
        }
        catch (PermanentIOException pie)            // This is due to an error setting the minimum store size
        {                                           // on the local file system.
            com.ibm.ws.ffdc.FFDCFilter.processException(pie, "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableMessageStoreImpl.setFileSizesAfterRestart", "1:2546:1.81.1.6", this);
            SibTr.warning(tc, "FILE_STORE_STORE_SIZE_CHANGE_PREVENTED_SIMS1549");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setFileSizesAfterRestart");
    }


    /**
     * Request that the receiver prints its xml representation
     * (recursively) onto writer.
     *
     * @param writer
     *
     * @throws IOException
     */
    public void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        // This is the minimal implementation of this
        //        writer.newLine();
        //        writer.emptyTag(com.ibm.ws.sib.msgstore.XmlConstants.XML_FILE_STORE);
    }
}
