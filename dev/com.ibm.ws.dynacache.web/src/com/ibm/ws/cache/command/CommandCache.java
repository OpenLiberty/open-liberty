/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.command;

import java.io.Serializable;
import java.rmi.RemoteException;

import com.ibm.websphere.command.CacheableCommand;
import com.ibm.websphere.command.CommandException;
import com.ibm.websphere.command.CommandTarget;
import com.ibm.websphere.command.TargetPolicy;
import com.ibm.websphere.command.TargetableCommandImpl;
import com.ibm.ws.cache.BatchUpdateDaemon;
import com.ibm.ws.cache.DynaCacheConstants;
import com.ibm.ws.cache.EntryInfo;
import com.ibm.ws.cache.RemoteServices;
import com.ibm.ws.cache.intf.DCache;

/**
 * This class implements the unique features of command caching prior to
 * delegating to the underlying Cache.
 * It is used by the CacheableCommandImpl class.
 * The Dynacache is for use by command writers.
 */
public class CommandCache implements com.ibm.ws.cache.intf.CommandCache {
    private DCache cache = null;
    private RemoteServices remoteServices = null;
    private BatchUpdateDaemon batchUpdateDaemon = null;
    private CommandStoragePolicy commandStoragePolicy = null;

    /**
     * This sets the Cache for this JVM.
     * It is called by the CacheUnit when things get started.
     * 
     * @param cache The Cache.
     */
    @Override
    public void setCache(DCache cache) {
        this.cache = cache;
    }

    /**
     * This sets the BatchUpdateDaemon for this JVM.
     * It is called by the CacheUnit when things get started.
     * 
     * @param batchUpdateDaemon The BatchUpdateDaemon.
     */
    public void setBatchUpdateDaemon(BatchUpdateDaemon batchUpdateDaemon) {
        this.batchUpdateDaemon = batchUpdateDaemon;
    }

    /**
     * This sets the RemoteServices for this JVM.
     * It is called by the CacheUnit when things get started.
     * 
     * @param remoteServices The RemoteServices.
     */
    public void setRemoteServices(RemoteServices remoteServices) {
        this.remoteServices = remoteServices;
    }

    /**
     * This gets the name of the CommandStoragePolicy class.
     * 
     * @return The CommandStoragePolicy class name.
     */
    public CommandStoragePolicy getCommandStoragePolicy() {
        return commandStoragePolicy;
    }

    /**
     * This sets the name of the CommandStoragePolicy class.
     * 
     * @param commandStoragePolicy The CommandStoragePolicy class name.
     */
    public void setCommandStoragePolicy(Object commandStoragePolicy) {
        this.commandStoragePolicy = (CommandStoragePolicy) commandStoragePolicy;
    }

    /**
     * This is called by the CacheUnitImpl class when everything gets started.
     */
    public void start() {
        if ((cache == null) || (commandStoragePolicy == null) || (batchUpdateDaemon == null) || (remoteServices == null)) {
            throw new IllegalStateException("cache, batchUpdateDaemon, and remoteServices " + "must all be set before start()");
        }
    }

    /**
     * This looks for a command in the local cache with the same id
     * as the input command.
     * All commands in the cache are in the executed state
     * (i.e., input and output properties are set).
     * It delegates to the getCommand method with the shared parameter.
     * 
     * @param inputCommand The command in the initialized state
     *            (i.e., input properties are set but output properties are not).
     * @return The executed command or null if not cached.
     */
    public CacheableCommand getCommandLocally(CacheableCommand inputCommand, boolean execute) throws CommandException {
        if (inputCommand == null) {
            throw new IllegalArgumentException("inputCommand was null");
        }
        CacheableCommand outputCommand;
        com.ibm.websphere.cache.CacheEntry cacheEntry = null;
        Object value = null;

        //Object mutex = cache.getMutex(inputCommand.getId());
        try {
            // synchronized (mutex) {
            EntryInfo ei = (EntryInfo) inputCommand.getEntryInfo();
            //Object value = cache.getValue(ei, false);
            cacheEntry = cache.getEntry(ei, false);
            if (cacheEntry != null)
                value = cacheEntry.getValue();
            outputCommand = commandStoragePolicy.prepareForCacheAccess((Serializable) value, cache, ei);
            //}
        } finally {
            if (cacheEntry != null)
                cacheEntry.finish();
        }
        if (outputCommand == null && execute) {
            outputCommand = executeAndCacheCommand(inputCommand);
        }
        return outputCommand;
    }

    private CacheableCommand executeAndCacheCommand(CacheableCommand inputCommand) throws CommandException {
        TargetPolicy targetPolicy = TargetableCommandImpl.getTargetPolicy();
        CommandTarget commandTarget = targetPolicy.getCommandTarget(inputCommand);
        CacheableCommand outputCommand = executeCommand(inputCommand, commandTarget);
        if (outputCommand == null) {
            throw new IllegalStateException("outputCommand was returned from proxy as null");
        }
        setCommand(outputCommand);
        return outputCommand;
    }

    /**
     * This either finds the command in a cache or executes it and puts
     * it in the cache.
     * 
     * @param inputCommand The command in the initialized state
     *            (i.e., input properties are set but output properties are not).
     * @param shared False indicates that it should look only in the
     *            local cache. True indicates that it should also look in the
     *            coordinating CacheUnit.
     * @return The executed command.
     */
    /*
     * public CacheableCommand getCommand(CacheableCommand inputCommand, boolean execute) throws CommandException {
     * String id = inputCommand.getId();
     * //Object mutex = cache.getMutex(id);
     * com.ibm.websphere.cache.CacheEntry cacheEntry = null;
     * Object value = null;
     * try {
     * // synchronized (mutex) { //DCS 8/1
     * if (inputCommand == null) {
     * throw new IllegalArgumentException("inputCommand was null");
     * }
     * EntryInfo ei = (EntryInfo) inputCommand.getEntryInfo();
     * //Object value = cache.getValue(ei, false);
     * cacheEntry = cache.getEntry(ei, false);
     * if(cacheEntry != null)
     * value = cacheEntry.getValue();
     * CacheableCommand outputCommand = commandStoragePolicy.prepareForCacheAccess((Serializable) value,cache,ei);
     * 
     * if (outputCommand == null) {
     * //outputCommand = remoteServices.getCommand(inputCommand,execute);
     * int cmdSharing = inputCommand.getSharingPolicy();
     * boolean pulled = false;
     * if (remoteServices.shouldPull(cmdSharing, id)) {
     * CacheEntry ce = remoteServices.getEntry(id);
     * if (ce != null) {
     * value = ce.getValue();
     * outputCommand = commandStoragePolicy.prepareForCacheAccess((Serializable) value,cache,ei);
     * pulled = true;
     * }
     * }
     * 
     * if (outputCommand == null) {
     * // NOTE - results in setCommandLocal, which calls prepareForCacheAccess
     * if (execute)
     * outputCommand = executeAndCacheCommand(inputCommand);
     * } else {
     * if (pulled) {
     * // use cache value to just set local cache
     * // NOTE - prepareForCacheAccess called here
     * setCommandLocal(outputCommand);
     * }
     * }
     * }
     * return outputCommand;
     * // }
     * } finally {
     * if(cacheEntry != null)
     * cache.finish((com.ibm.ws.cache.CacheEntry)cacheEntry);
     * //cache.releaseMutex(mutex);
     * }
     * }
     */
    public CacheableCommand getCommand(CacheableCommand inputCommand, boolean execute) throws CommandException {
        if (inputCommand == null) {
            throw new IllegalArgumentException("inputCommand was null");
        }

        String id = inputCommand.getId();
        //Object mutex = cache.getMutex(id);
        com.ibm.websphere.cache.CacheEntry cacheEntry = null;
        Object value = null;
        try {
            // synchronized (mutex) { //DCS 8/1
            EntryInfo ei = (EntryInfo) inputCommand.getEntryInfo();
            //Object value = cache.getValue(ei, false);
            int cmdSharing = inputCommand.getSharingPolicy();
            boolean askPermission = remoteServices.shouldPull(cmdSharing, id);
            cacheEntry = cache.getEntry(ei, askPermission);
            if (cacheEntry != null)
                value = cacheEntry.getValue();
            CacheableCommand outputCommand = commandStoragePolicy.prepareForCacheAccess((Serializable) value, cache, ei);

            if (outputCommand == null) {
                if (execute)
                    outputCommand = executeAndCacheCommand(inputCommand);
            }
            return outputCommand;
            // }
        } finally {
            if (cacheEntry != null) {
                //cache.finish((com.ibm.ws.cache.CacheEntry)cacheEntry);
                cacheEntry.finish();
            }

            //cache.releaseMutex(mutex);
        }
    }

    //Is this the only place I need to set DefaultPriority on EntryInfo
    // for the CommandCache? mdm
    /**
     * This puts a command in the cache.
     * 
     * @param command The command to be cached.
     */
    /*
     * public void setCommand(CacheableCommand command) {
     * if (command == null) {
     * throw new IllegalArgumentException("command was null");
     * }
     * 
     * String id = command.getId();
     * EntryInfo entryInfo = (EntryInfo) command.getEntryInfo();
     * if (entryInfo.isSharedPull()) { // true if SHARED_PULL or SHARED_PUSH_PULL
     * // want immediate inval
     * batchUpdateDaemon.invalidateById(id, true,cache);
     * }
     * 
     * setCommandLocal(command);
     * 
     * if (entryInfo.isSharedPush()) {
     * CacheEntry cacheEntry = cache.getEntry(command.getId());
     * if (cacheEntry == null) {
     * return;
     * }
     * if (cacheEntry.isBatchEnabled())
     * batchUpdateDaemon.pushCacheEntry(cacheEntry,cache);
     * else
     * remoteServices.setEntry(cacheEntry);
     * }
     * }
     */
    public void setCommand(CacheableCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command was null");
        }

        String id = command.getId();
        EntryInfo entryInfo = (EntryInfo) command.getEntryInfo();
        if (entryInfo.isSharedPull()) {
            // want immediate inval
            batchUpdateDaemon.invalidateById(id, true, cache);
        }
        Serializable value = commandStoragePolicy.prepareForCache(command);

        cache.setValue(entryInfo, value, entryInfo.isSharedPush(), DynaCacheConstants.VBC_CACHE_NEW_CONTENT);

    }

    /*
     * private void setCommandLocal(CacheableCommand command) {
     * EntryInfo entryInfo = (EntryInfo) command.getEntryInfo();
     * if (!entryInfo.wasPrioritySet()) {
     * entryInfo.setPriority(defaultPriority);
     * }
     * Serializable value = commandStoragePolicy.prepareForCache(command);
     * 
     * cache.setValue(entryInfo, value, !Cache.COORDINATE, DynaCacheConstants.VBC_CACHE_NEW_CONTENT);
     * 
     * }
     */

    /**
     * This runs the command without caching it.
     * 
     * @param command The command in the initialized state.
     * @param commandTarget This command's target.
     * @return The command in the executed state.
     */
    public CacheableCommand executeCommand(CacheableCommand command, CommandTarget commandTarget) throws CommandException {
        CacheableCommand returnCommand = command;
        try {
            boolean done = command.preExecute();
            if (done) {
                return command;
            }
            returnCommand = (CacheableCommand) commandTarget.executeCommand(command);
            returnCommand.postExecute();
            return returnCommand;
        } catch (RemoteException rex) {
            throw new CommandException(rex);
        } finally {
            EntryInfo entryInfo = (EntryInfo) returnCommand.getEntryInfo();
            entryInfo.addTemplate(returnCommand.getClass().getName());
        }
    }

    /**
     * This method forces the entry to stay in the cache.
     * If an entry with this id does not exist, it creates one.
     * The LRU algorithm ingores a pinned entry.
     * For a pinned entry, an invalidation only
     * marks the entry to be removed when it is not pinned.
     * A pin count is maintained.
     * This delegates to the Cache.
     * 
     * @param id The String identifier for the cache entry.
     *            The id cannot be null.
     * @return True implies the entry was in the cache prior to this method.
     */
    /*
     * public boolean pin(String id) {
     * return cache.pin(id);
     * }
     */

    /**
     * This method subjects the entry to LRU replacement,
     * time limit and invalidation processing.
     * If an entry with this id does not exist, this does nothing.
     * A pin count is maintained.
     * The pinCount is never allowed to go negative.
     * If this method tries to make the pinCount go negative, it is set to 0.
     * This delegates to the Cache.
     * 
     * @param id The String identifier for the entry.
     *            The id cannot be null.
     * @return True implies the entry was in the cache prior to this method.
     */
    /*
     * public boolean unpin(String id) {
     * return cache.unpin(id);
     * }
     */

    /**
     * This gets the mutex for a cache entry that can
     * be used by a client in a synchronized block to lock
     * an entry across multiple Cache method calls.
     * This delegates to the Cache.
     * 
     * @param id The cache id for the entry. The id cannot be null.
     * @return The mutex for the entry with the specified cache id.
     */
    /*
     * public Object getMutex(String id) {
     * return cache.getMutex(id);
     * }
     * 
     * public void releaseMutex(Object mutex) {
     * cache.releaseMutex(mutex);
     * }
     */

    /**
     * This is used by the CacheHook to determine if an entry has
     * been either removed or invalidated while it is being rendered.
     * This delegates to the Cache.
     * 
     * @param id The cache id for the entry being tested.
     * @return True if id is in cache and !removeWhenUnpinned.
     */
    public boolean isValid(String id) {
        return cache.isValid(id);
    }

    /**
     * This invalidates in all caches all entries dependent on the specified
     * id.
     * This delegates to the Cache.
     * 
     * @param id The cache id or data id.
     * @param waitOnInvalidation True indicates that this method should
     *            not return until the invalidations have taken effect on all caches.
     *            False indicates that the invalidations will be queued for later
     *            batch processing.
     */
    public void invalidateById(String id, boolean waitOnInvalidation) {
        cache.invalidateById(id, waitOnInvalidation);
    }

    /**
     * This invalidates in all caches all entries dependent on the specified
     * id.
     * This delegates to the Cache.
     * 
     * @param id The cache id or data id.
     * @param causeOfInvalidation see com.ibm.websphere.pmi.CachePerf
     *            for the different causes.
     * @param waitOnInvalidation True indicates that this method should
     *            not return until the invalidations have taken effect on all caches.
     *            False indicates that the invalidations will be queued for later
     *            batch processing.
     */

    public void invalidateById(String id, int causeOfInvalidation, boolean waitOnInvalidation) {
        cache.invalidateById(id, causeOfInvalidation, waitOnInvalidation);
    }

    /**
     * This invalidates in all caches all entries dependent on the specified
     * template.
     * This delegates to the Cache.
     * 
     * @param template The template name.
     * @param waitOnInvalidation True indicates that this method should
     *            not return until the invalidations have taken effect on all caches.
     *            False indicates that the invalidations will be queued for later
     *            batch processing.
     */
    public void invalidateByTemplate(String template, boolean waitOnInvalidation) {
        cache.invalidateByTemplate(template, waitOnInvalidation);
    }

    public DCache getCache() {
        return cache;
    }
}
