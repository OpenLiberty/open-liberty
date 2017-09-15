/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.cache.DistributedNioMapObject;
import com.ibm.websphere.cache.Sizeable;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.util.ObjectSizer;
import com.ibm.ws.cache.util.SerializationUtility;
import com.ibm.ws.util.ObjectPool;
import com.ibm.wsspi.cache.GenerateContents;

/**
 * A CacheEntry is a struct object that holds the
 * cache id and value, as well as metadata for caching.
 * The information in these variables is obtained from the
 * EntryInfo object used when the entry was cached.
 */
public class CacheEntry implements com.ibm.websphere.cache.CacheEntry, InvalidationEvent, Sizeable, Externalizable {

    private static final long serialVersionUID = -3194658354293955553L;
    private static TraceComponent tc = Tr.register(CacheEntry.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private static final String EMPTY_STRING = "";
    private static boolean errorAlreadyDisplayed = false;

    /** cache name is needed in Liberty to get hold of the shared library associated with the Cache */
    protected String cacheName = null;

    /**
     * This is the identifier of the entry.
     * It must be unique within the scope of the group of Cache instances.
     * Having this in the CacheEntry allows an entry obtained
     * via the lruArray to know how to find it in the entryHashtable.
     */
    public Object id = null;

    protected byte[] serializedId = null;

    /**
     * This is the entry's value (i.e., the object that is cached).
     */
    protected Object value = null;

    /**
     * This is the serialized value for the object
     * Required when the cacheEntry will be serialized/deserialized
     */
    protected byte[] serializedValue = null;

    /**
     * This is the hashcode for the value object
     */
    protected int valueHashcode = 0;

    /**
     * This is the entry's user meta data.
     */
    protected Object userMetaData = null;

    /**
     * This is the serialized user meta data for the object
     * Required when the cacheEntry will be serialized/deserialized
     */
    protected byte[] serializedUserMetaData = null;

    /**
     * A larger priority gives an entry a longer time in the
     * cache when it is not being used.
     * The value of priority should be based on the ratio of
     * the cost of computing the entry to the cost of
     * the memory in the cache (the size of the entry).
     * The default is 1.
     */
    protected int priority = CacheConfig.DEFAULT_PRIORITY;

    /**
     * This is the maximum interval of time in seconds
     * that the entry is allowed to stay in the cache.
     * The entry may be discarded via LRU replacement prior to this time.
     * A negative value indicates no time limit.
     */
    protected int timeLimit = -1;

    /**
     * This is the minimum interval of time in seconds
     * that the entry is allowed to stay in the cache.
     * The entry may be discarded via LRU replacement prior to this time.
     * A negative value indicates no inactivity limit.
     * The inactivity timer is reset on a read operation.
     */
    protected int inactivity = -1; // CPF-Inactivity

    /**
     * This is the absolute time (RET) when the entry should expire.
     * The entry may be discarded via LRU replacement prior to this time.
     * A negative value indicates no expiration time.
     */
    public long expirationTime = -1;

    /**
     * This is the time (VET) when the entry becomes invalid.
     * CE becomes invalid at the time t if VET < t < RET.
     * CE becomes valid at the time t < VET.
     * value set to -1 means not used.
     */
    public long validatorExpirationTime = -1;

    /**
     * This is the absolute time when the entry leaves
     * the local machine heading for a remote machine.
     * It is used by the receiving machine to adjust
     * expirationTime & timeStamp.
     */
    protected long drsClock = -1;

    /**
     * This is the time when the entry was first created.
     * It is used to help synchronize sets and invalidations.
     */
    public long timeStamp = -1;

    /**
     * For a command, this is the class name of the command;
     * for a JSP/servlet, this is the URI for this entry.
     * <ul>
     * <li>For a top-level entry (eg, a JSP/Servlet that is
     * externally requested), this is obtained from the HTTP
     * request object's URL. It can be set either by
     * the server or by the top-level JSP/Servlet.
     * <li>For a contained entry, this is the JSP/Servlet
     * file name URL (the parameter that would be used
     * in the callPage method). It can be set either by
     * the JSP/Servlet it names or its containing JSP/Servlet,
     * plus other information from the request object.
     * </ul>
     */
    protected String _templates[] = EMPTY_STRING_ARRAY;

    /**
     * This is the list of entry or data ids that when invalidated cause
     * this entry to become invalid.
     * It is a ValueSet whose elements are Strings.
     * They are the identifiers used in the invalidateById methods
     * to invalidate all cache entries having a dependency on these ids.
     * Data ids must be unique within the same scope as cache ids.
     */
    protected Object _dataIds[] = EMPTY_OBJECT_ARRAY;

    protected Object _serializedDataIds[] = null;
    /**
     * This is used by the Cache to mark a entry as pendingRemoval
     * without removing it from the cache.
     */
    protected boolean pendingRemoval = false;

    /**
     * In a multi-JVM environment, this indicates whether
     * the cache entry
     * should be EntryInfo.NOT_SHARED, EntryInfo.SHARED_PUSH_PULL or
     * EntryInfo.SHARED_PUSH. The default is NOT_SHARED.
     */
    protected int sharingPolicy = EntryInfo.NOT_SHARED;

    /**
     * The persist to disk property determines if the entry
     * gets sent to the disk when overflow, replication or
     * server stopping occur. The default value is true which
     * means that the entry stays in the memory only.
     */
    protected boolean persistToDisk = true;

    public int cacheType = CACHE_TYPE_DEFAULT;

    /**
     * This indicates the value object implements the DistributedNioMapObject or not
     */
    protected boolean useByteBuffer = false;

    protected boolean skipMemoryAndWriteToDisk = false;

    /**
     * Primarily added for the cachemonitor display of FRCA caches
     * The ExternalCacheGroup this CacheEntry belongs to ..
     */
    protected String externalCacheGroupId = null;

    /**
     * Holds a reference to the pool to which
     * this CE belongs.
     */
    transient protected CacheEntryPool cacheEntryPool = null;

    /**
     * This is the entry's clock value.
     * It starts with the value in the priority variable.
     * Each clock cycle, it is decremented.
     * Whenever the entry is used, it is reset to the value
     * in the priority variable.
     * If it reaches 0, the entry is a victim candidate.
     */
    transient protected int clock = CacheConfig.DEFAULT_PRIORITY;

    /**
     * When this <= 0, the entry is subject to LRU replacement.
     * When this is > 0, it is immune to LRU replacement
     * (the clock algorithm simply bypasses it),
     * and remove (remove sets its removeWhenUnpinned to true).
     */
    transient private AtomicInteger refCount = new AtomicInteger(0);

    /**
     * This indicates that the entry should be removed
     * when the pinCount goes to zero.
     * It is set by the remove method when an entry is removed
     * but its pinCount > 0.
     */
    transient protected boolean removeWhenUnpinned = false;

    // flag that indicates that this cacheEntry was created by the
    // LRU overflow
    transient protected boolean isOverflowEntry = false;

    transient protected boolean lruEvicted = false;

    protected Object aliasList[] = EMPTY_OBJECT_ARRAY;

    protected Object serializedAliasList[] = null;

    //LRU processing variables
    transient protected LRUHead lruHead;
    transient protected CacheEntry _previous = null;
    transient protected CacheEntry _next = null;

    transient protected boolean loadedFromDisk = false;
    transient protected boolean skipValueSerialized = false;
    transient protected int skipMemoryAndWriteToDiskErrorCode = HTODDynacache.NO_EXCEPTION;

    // This variable is used when CE is found but is invalid (VET < t < RET). 
    // It indicates where the CE comes from local memory, disk or remote.
    transient protected int vbcSource = DynaCacheConstants.VBC_INVALID_NOT_USED;

    /**
     * This method will attempt to serialize
     * the cached value. A failure will result
     * in an error message being logged so the
     * user can correct his configuration.
     * 
     * Callers of this method should not log
     * additional messages.
     * 
     * @return true - The value was serialized
     *         false - The value can not be serialized and msg DYNA0052E is logged.
     */
    @Override
    public synchronized boolean prepareForSerialization() {

        // PK29388 Get Concurrent Modification Exception if method is not synchronized
        // Some one else might be iterating over this object when we are serializing it

        boolean success = true;
        boolean serialized = false; //used to determine if any serialization took place
        String exceptionMessage = null;
        long oldSize = 0, newSize = 0;
        if (cacheEntryPool != null) {
            if (cacheEntryPool.cache.isCacheSizeInMBEnabled()) {
                oldSize = getObjectSize();
            }
        }

        if (value != null) {
            try {
                serializedValue = SerializationUtility.serialize((Serializable) value);
                if (useByteBuffer) {
                    if (this.value instanceof DistributedNioMapObject) {
                        ((DistributedNioMapObject) this.value).release();
                    }
                }
                value = null;
                serialized = true;
            } catch (Exception e) {
                exceptionMessage = e.toString();
                // com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.CacheEntry.prepareForSerialization", "206", this);
            }
            if (value != null) {
                Tr.error(tc, "DYNA0052E", new Object[] { id, value.getClass().getName(), "cache-value", exceptionMessage }); // LI4337-17
                success = false;
            }
        }

        if ((serializedId == null) && success) {
            try {
                serializedId = SerializationUtility.serialize((Serializable) id);
                serialized = true;
            } catch (Exception e) {
                exceptionMessage = e.toString();
                // com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.CacheEntry.prepareForSerialization", "244", this);
            }
            if (serializedId == null) {
                Tr.error(tc, "DYNA0052E", new Object[] { id, (id == null ? "null" : id.getClass().getName()), "cache-id", exceptionMessage }); // LI4337-17
                success = false;
            }
        }

        if ((_serializedDataIds == null) && _dataIds.length > 0 && success) {
            _serializedDataIds = new Object[_dataIds.length];
            int i = 0;
            try {
                for (i = 0; i < _dataIds.length; i++) {
                    _serializedDataIds[i] = SerializationUtility.serialize((Serializable) _dataIds[i]);
                }
                serialized = true;
            } catch (Exception e) {
                exceptionMessage = e.toString();
                // com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.CacheEntry.prepareForSerialization", "264", this);
            }
            if (exceptionMessage != null) {
                if (_dataIds[i] != null) {
                    Tr.error(tc, "DYNA0052E", new Object[] { id, (_dataIds[i] == null ? "null" : _dataIds[i].getClass().getName()), "dep-id", exceptionMessage }); // LI4337-17
                }
                success = false;
                _serializedDataIds = null;
            }
        }

        if ((serializedAliasList == null) && aliasList.length > 0 && success) { //CCC-O
            serializedAliasList = new Object[aliasList.length];
            int i = 0;
            try {
                for (i = 0; i < aliasList.length; i++) {
                    serializedAliasList[i] = SerializationUtility.serialize((Serializable) aliasList[i]);
                }
                serialized = true;
            } catch (Exception e) {
                exceptionMessage = e.toString();
            }
            if (exceptionMessage != null) {
                if (aliasList[i] != null) {
                    Tr.error(tc, "DYNA0052E", new Object[] { id, (aliasList[i] == null ? "null" : aliasList[i].getClass().getName()), "alias-id", exceptionMessage }); // LI4337-17
                }
                success = false;
                serializedAliasList = null;
            }
        }

        if (userMetaData != null && success) {
            try {
                serializedUserMetaData = SerializationUtility.serialize((Serializable) userMetaData);
                userMetaData = null;
                serialized = true;
            } catch (Exception e) {
                exceptionMessage = e.toString();
            }
            if (userMetaData != null) {
                Tr.error(tc, "DYNA0052E", new Object[] { id, userMetaData.getClass().getName(), "metadata", exceptionMessage }); // LI4337-17
                success = false;
            }
        }

        //adjusting the size is necessary here because after serialization the size of the CE changes in memory
        //therefore it becomes necessary to recompute the size and adjust the difference in the overall cache size in MB.		
        if (success && serialized && cacheEntryPool != null) {
            if (cacheEntryPool.cache.isCacheSizeInMBEnabled()) {
                newSize = getObjectSize();
                if (oldSize != -1 && newSize != -1) {
                    cacheEntryPool.cache.increaseCacheSizeInBytes(newSize - oldSize, "PREPARE_SER");
                } else {
                    cacheEntryPool.cache.disableCacheSizeInMB();
                }
            }
        }

        return success;
    }

    /**
     * Handle needed processing after receiving a CE from
     * a remote mahine via DRS.
     * 
     * (1) Adjust the timestamps on an incomming
     * CE to try and account for clock delta
     * between machines.
     */
    public void processDrsInbound(long localClock) {
        // Is dsrClock timestamp from the remote machine missing
        // or was the CE's timestampls already altered?
        if (drsClock <= 0) {
            return;
        }
        // Adjust timestamps for this machines clock.
        long clockDifference = localClock - drsClock;
        if (expirationTime > 0)
            expirationTime += clockDifference;
        if (timeStamp > 0)
            timeStamp += clockDifference;
        // Don't allow >1 time alter on this CE.
        drsClock = -1;
    }

    /**
     * Handle needed processing before sending a CE to
     * a remote machine via DRS.
     * 
     * (1) Add a timestamp ( see processDrsInbound() )
     */
    public void processDrsOutbound() {
        // Timestamp this CE - ( If addition actions
        // are added to this method, always mnake this
        // the last action )
        drsClock = System.currentTimeMillis();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        if (!prepareForSerialization()) {
            // Should never see this. If you do it is because
            // some previos code is missing a call to
            // prepareForSerialization().
            throw new IOException("Object not serializable: " + (value == null ? "null object" : value.getClass().getName()));
        }

        out.writeUTF(cacheName);

        if (serializedId != null) {
            out.writeInt(serializedId.length);
            out.write(serializedId);
        } else {
            out.writeInt(-1);
        }
        if (skipValueSerialized) {
            out.writeInt(-1);
        } else {
            if (serializedValue != null) {
                out.writeInt(serializedValue.length);
                out.write(serializedValue);
            } else {
                out.writeInt(-1);
            }
        }
        out.writeInt(priority);
        out.writeInt(timeLimit);
        out.writeInt(inactivity); // CPF-Inactivity
        out.writeLong(expirationTime);
        out.writeLong(timeStamp);
        out.writeLong(drsClock);
        out.writeInt(cacheType);
        out.writeObject(_templates);

        if (_serializedDataIds != null) {
            out.writeInt(_serializedDataIds.length);
            for (int i = 0; i < _serializedDataIds.length; i++) {
                byte[] did = (byte[]) _serializedDataIds[i];
                out.writeInt(did.length);
                out.write(did);
            }
        } else {
            out.writeInt(-1);
        }
        out.writeBoolean(pendingRemoval);
        out.writeInt(sharingPolicy);
        out.writeBoolean(persistToDisk);
        // Write boolean is required for backward compatibility. 
        // This boolean is previously used to determine batch mode. However, cache replication is always runnning in an asychronous batch mode.
        // Therefore, batch mode variable is removed.
        out.writeBoolean(true);
        out.writeBoolean(useByteBuffer);
        if (serializedAliasList != null) {
            out.writeInt(serializedAliasList.length);
            for (int i = 0; i < serializedAliasList.length; i++) {
                byte[] aid = (byte[]) serializedAliasList[i];
                out.writeInt(aid.length);
                out.write(aid);
            }
        } else {
            out.writeInt(-1);
        }
        if (serializedUserMetaData != null) {
            out.writeInt(serializedUserMetaData.length);
            out.write(serializedUserMetaData);
        } else {
            out.writeInt(-1);
        }
        out.writeBoolean(skipMemoryAndWriteToDisk);
        out.writeInt(valueHashcode);
        out.writeLong(validatorExpirationTime);

        if (externalCacheGroupId != null) {
            out.writeUTF(externalCacheGroupId);
        } else {
            out.writeUTF(EMPTY_STRING); //write out an empty string
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        try {

            cacheName = in.readUTF();

            int keyLength = in.readInt();
            if (keyLength > 0) {
                serializedId = new byte[keyLength];
                in.readFully(serializedId);
            } else {
                serializedId = null;
            }

            if (serializedId != null) {
                try {
                    id = SerializationUtility.deserialize(serializedId, cacheName);
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheEntry.readExternal", "366", this);
                }
            } else {
                id = null;
            }

            int len = in.readInt();
            if (len > 0) {
                serializedValue = new byte[len];
                in.readFully(serializedValue);
            } else {
                serializedValue = null;
            }

            value = null;
            priority = in.readInt();
            timeLimit = in.readInt();
            inactivity = in.readInt();
            expirationTime = in.readLong();
            timeStamp = in.readLong();
            drsClock = in.readLong();
            cacheType = in.readInt();
            _templates = (String[]) in.readObject();
            int size = in.readInt();

            if (size > 0) {
                _serializedDataIds = new Object[size];
                for (int i = 0; i < size; i++) {
                    len = in.readInt();
                    _serializedDataIds[i] = new byte[len];
                    in.readFully((byte[]) _serializedDataIds[i]);
                }
            } else {
                _serializedDataIds = null;
            }

            if (_serializedDataIds != null) {
                _dataIds = new Object[_serializedDataIds.length];
                try {
                    for (int i = 0; i < _serializedDataIds.length; i++) {
                        _dataIds[i] = SerializationUtility.deserialize((byte[]) _serializedDataIds[i], cacheName);
                    }
                    _serializedDataIds = null;
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheEntry.readExternal", "388", this);
                    _dataIds = EMPTY_OBJECT_ARRAY;
                }
            } else {
                _dataIds = EMPTY_OBJECT_ARRAY;
            }
            pendingRemoval = in.readBoolean();
            sharingPolicy = in.readInt();
            persistToDisk = in.readBoolean();
            // Read boolean is required for backward compatibility. 
            // This boolean is previously used to determine batch mode. However, cache replication is always runnning in an asychronous batch mode.
            // Therefore, batch mode variable is removed.
            in.readBoolean();
            useByteBuffer = in.readBoolean();
            size = in.readInt();
            if (size > 0) {
                serializedAliasList = new Object[size];
                for (int i = 0; i < size; i++) {
                    len = in.readInt();
                    serializedAliasList[i] = new byte[len];
                    in.readFully((byte[]) serializedAliasList[i]);
                }
            } else {
                serializedAliasList = null;
            }
            if (serializedAliasList != null) {
                aliasList = new Object[serializedAliasList.length];
                try {
                    for (int i = 0; i < serializedAliasList.length; i++) {
                        aliasList[i] = SerializationUtility.deserialize((byte[]) serializedAliasList[i], cacheName);
                    }
                    serializedAliasList = null;
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheEntry.readExternal", "428", this);
                    aliasList = EMPTY_OBJECT_ARRAY;
                }
            } else {
                aliasList = EMPTY_OBJECT_ARRAY;
            }
            len = in.readInt();
            if (len > 0) {
                serializedUserMetaData = new byte[len];
                in.readFully(serializedUserMetaData);
            } else {
                serializedUserMetaData = null;
            }
            userMetaData = null;
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheEntry.readExternal", "468", this);
            return;
        }

        try { // ignore the exception when try to get skipMemoryAndWriteToDisk boolean
            skipMemoryAndWriteToDisk = in.readBoolean();
        } catch (Exception ex) {
            skipMemoryAndWriteToDisk = false;
            if (!CacheEntry.errorAlreadyDisplayed && tc.isDebugEnabled()) {
                Tr.debug(tc, "Old format: missing skipMemoryAndWriteToDisk boolean during deserialization of cache entry.");
                CacheEntry.errorAlreadyDisplayed = true;
            }
        }

        try { // ignore the exception when try to get value hashcode. If exception happens, find the hashcode for the value.
            valueHashcode = in.readInt();
        } catch (Exception ex) {
            Object val = getValue();
            if (val != null) {
                valueHashcode = val.hashCode();
            } else {
                valueHashcode = 0;
            }
            if (!CacheEntry.errorAlreadyDisplayed && tc.isDebugEnabled()) {
                Tr.debug(tc, "Old format: missing hashcode for value during deserialization of cache entry.");
                CacheEntry.errorAlreadyDisplayed = true;
            }
        }

        try { // read in validator expiration time. If exception happens, set it to -1 (not used).
            validatorExpirationTime = in.readLong();
        } catch (Exception ex) {
            validatorExpirationTime = -1;
            if (!CacheEntry.errorAlreadyDisplayed && tc.isDebugEnabled()) {
                Tr.debug(tc, "Old format: missing validator expiration time during deserialization of cache entry.");
                CacheEntry.errorAlreadyDisplayed = true;
            }
        }

        try { //read in the externalCacheGroupId
            String externalCacheGroupId = in.readUTF();
            if (externalCacheGroupId.equals(EMPTY_STRING)) {
                externalCacheGroupId = null;
            }
        } catch (IOException ioe) {
            if (!CacheEntry.errorAlreadyDisplayed && tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not read externalCacheGroupId: " + ioe.getMessage());
                CacheEntry.errorAlreadyDisplayed = true;
            }
        }
    }

    /**
     * Get's the entry's value
     */
    @Override
    public synchronized Object getValue() {
        if (id != null) {
            if (serializedValue != null) {
                long oldSize = -1;
                if (cacheEntryPool != null) {
                    if (cacheEntryPool.cache.isCacheSizeInMBEnabled()) {
                        oldSize = getObjectSize();
                    }
                }

                try {
                    value = SerializationUtility.deserialize(serializedValue, cacheName);
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheEntry.getValue", "200", this);
                }
                if (value != null) {
                    serializedValue = null;
                    if (cacheEntryPool != null) {
                        if (cacheEntryPool.cache.isCacheSizeInMBEnabled()) {
                            long newSize = getObjectSize();
                            if (oldSize != -1 && newSize != -1) {
                                cacheEntryPool.cache.increaseCacheSizeInBytes(newSize - oldSize, "GET_VALUE");
                            } else {
                                cacheEntryPool.cache.disableCacheSizeInMB();
                            }
                        }
                    }
                }
            }
        }
        return value;
    }

    /**
     * Get's the entry's serialized value
     */
    public byte[] getSerializedValue() {
        return this.serializedValue;
    }

    /**
     * Set's the entry's value
     */
    protected void setValue(Object value) {
        this.value = value;
        serializedValue = null;
        timeStamp = System.currentTimeMillis();
        this.valueHashcode = 0;
    }

    /**
     * This gets all templates that this entry depends on for invalidation.
     * 
     * @return An Enumeration of the templates.
     */
    @Override
    public Enumeration getTemplates() {
        return new ArrayEnumerator(_templates);
    }

    //convenience method
    public String getTemplate() {
        if (_templates.length > 0)
            return _templates[0];
        return null;
    }

    /**
     * This gets all ids (cache ids and data ids) that this entry
     * depends on for invalidation.
     * 
     * @return An Enumeration of the ids.
     */
    @Override
    public Enumeration getDataIds() {
        return new ArrayEnumerator(_dataIds);
    }

    /**
     * Mimics a cache Hit, refreshing an entries spot in the LRU algorithm.
     * TODO: this function seems does not work
     * 
     * @return The creation timestamp.
     */
    @Override
    public void refreshEntry() {
        clock = priority;
    }

    /**
     * This implements the method in the InvalidationEvent interface.
     * 
     * @return The creation timestamp.
     */
    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * This brings this CacheEntry back to the same state it had when it
     * was first created. It does not change its lruArray index.
     * It is called by the Cache when one of the preallocated
     * CacheEntry instances is about to be reused for another logical
     * entry.
     */
    public void reset() {

        if (refCount.get() > 0 && isRefCountingEnabled()) {
            Tr.warning(tc, "reset called on " + id + " with a refCount of " + refCount);
            Thread.dumpStack();
        }

        cacheName = null;
        drsClock = -1;
        timeStamp = -1;
        serializedId = null;
        id = null;
        if (useByteBuffer && this.value != null) {
            if (this.value instanceof DistributedNioMapObject) {
                ((DistributedNioMapObject) this.value).release();
            }
        }
        serializedValue = null;
        value = null;
        clock = CacheConfig.DEFAULT_PRIORITY;
        refCount.set(0);
        priority = CacheConfig.DEFAULT_PRIORITY;
        timeLimit = -1;
        inactivity = -1;
        expirationTime = -1;
        validatorExpirationTime = -1;
        removeWhenUnpinned = false;
        _templates = EMPTY_STRING_ARRAY;
        _dataIds = EMPTY_OBJECT_ARRAY;
        _serializedDataIds = null;
        pendingRemoval = false;
        sharingPolicy = EntryInfo.NOT_SHARED;
        persistToDisk = true;
        isOverflowEntry = false;
        lruEvicted = false;
        lruHead = null;
        useByteBuffer = false;
        aliasList = EMPTY_OBJECT_ARRAY;
        serializedAliasList = null;
        userMetaData = null;
        serializedUserMetaData = null;
        loadedFromDisk = false;
        cacheType = CACHE_TYPE_DEFAULT;
        skipValueSerialized = false;
        skipMemoryAndWriteToDisk = false;
        skipMemoryAndWriteToDiskErrorCode = HTODDynacache.NO_EXCEPTION;
        valueHashcode = 0;
        externalCacheGroupId = null;
    }

    /**
     * This method copies the state of another CacheEntry into this CacheEntry.
     * It is called by the Cache when a CacheEntry is imported
     * from another JVM.
     * 
     * @param cacheEntry The CacheEntry that this CacheEntry is copied into.
     */
    public void copy(CacheEntry cacheEntry) {

        if (cacheEntry == this)
            return;

        if (useByteBuffer && this.value != null) {
            if (this.value instanceof DistributedNioMapObject) {
                ((DistributedNioMapObject) this.value).release();
            }
        }

        this.value = cacheEntry.value;
        this.valueHashcode = cacheEntry.valueHashcode;
        this.serializedValue = cacheEntry.serializedValue;
        this.serializedId = cacheEntry.serializedId;
        timeStamp = cacheEntry.timeStamp;
        expirationTime = cacheEntry.expirationTime;
        validatorExpirationTime = cacheEntry.validatorExpirationTime;
        timeLimit = cacheEntry.timeLimit;
        inactivity = cacheEntry.inactivity;
        drsClock = cacheEntry.drsClock;
        id = cacheEntry.id;
        priority = cacheEntry.priority;
        if (priority < 0)
            priority = 0;
        if (priority > CacheConfig.MAX_PRIORITY)
            priority = CacheConfig.MAX_PRIORITY;
        _templates = cacheEntry._templates;
        _dataIds = cacheEntry._dataIds;
        _serializedDataIds = cacheEntry._serializedDataIds;
        sharingPolicy = cacheEntry.sharingPolicy;
        persistToDisk = cacheEntry.persistToDisk;
        refCount = new AtomicInteger(cacheEntry.refCount.get());
        aliasList = cacheEntry.aliasList;
        serializedAliasList = cacheEntry.serializedAliasList;
        useByteBuffer = cacheEntry.useByteBuffer;
        userMetaData = cacheEntry.userMetaData;
        serializedUserMetaData = cacheEntry.serializedUserMetaData;
        loadedFromDisk = cacheEntry.loadedFromDisk;
        cacheType = cacheEntry.cacheType;
        skipValueSerialized = cacheEntry.skipValueSerialized;
        skipMemoryAndWriteToDisk = cacheEntry.skipMemoryAndWriteToDisk;
        skipMemoryAndWriteToDiskErrorCode = cacheEntry.skipMemoryAndWriteToDiskErrorCode;
        vbcSource = cacheEntry.vbcSource;
        externalCacheGroupId = cacheEntry.externalCacheGroupId;
        //Note: clock is not set here because it's really owned
        //by the cache.  Only the cache should say when the clock
        //changes.  It makes the cache code more readable.
    }

    /**
     * This method is called by the Cache to copy caching metadata
     * provided in a EntryInfo into this CacheEntry. It copies: <ul>
     * <li>id<li>timeout<li>expiration time<li>priority<li>template(s)
     * <li>data id(s)<li>sharing policy (included for forward compatibility</ul>
     * 
     * @param entryInfo The EntryInfo that contains the caching
     *            metadata to be copied from.
     */
    public void copyMetaData(EntryInfo entryInfo) {

        entryInfo.lock();

        if (!entryInfo.wasIdSet()) {
            throw new IllegalStateException("id was not set on entryInfo");
        }

        id = entryInfo.id;
        timeLimit = entryInfo.timeLimit;
        inactivity = entryInfo.inactivity;
        expirationTime = entryInfo.expirationTime;
        validatorExpirationTime = entryInfo.validatorExpirationTime;
        priority = entryInfo.priority;

        if (priority < 0)
            priority = 0;

        if (priority > CacheConfig.MAX_PRIORITY)
            priority = CacheConfig.MAX_PRIORITY;

        if (entryInfo.templates.size() > 0) {
            _templates = (String[]) entryInfo.templates.toArray(new String[entryInfo.templates.size()]);
        } else {
            _templates = EMPTY_STRING_ARRAY;
        }

        if (entryInfo.dataIds.size() > 0) {
            _dataIds = entryInfo.dataIds.toArray(new Object[entryInfo.dataIds.size()]);
        } else {
            _dataIds = EMPTY_OBJECT_ARRAY;
        }

        if (entryInfo.aliasList.size() > 0) {
            aliasList = entryInfo.aliasList.toArray(new Object[entryInfo.aliasList.size()]);
        } else {
            aliasList = EMPTY_OBJECT_ARRAY;
        }

        sharingPolicy = entryInfo.sharingPolicy;
        persistToDisk = entryInfo.persistToDisk;
        cacheType = entryInfo.cacheType;
        userMetaData = entryInfo.userMetaData;
        externalCacheGroupId = entryInfo.externalCacheGroupId;

        _serializedDataIds = null;
        serializedAliasList = null;
        serializedUserMetaData = null;
        loadedFromDisk = false;
        skipValueSerialized = false;
        skipMemoryAndWriteToDisk = false;
        skipMemoryAndWriteToDiskErrorCode = HTODDynacache.NO_EXCEPTION;

    }

    /**
     * This overrides the method in Object.
     * It compares cache ids.
     * 
     * @return A true indicates they are equal.
     */
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof CacheEntry)) {
            return false;
        }
        CacheEntry cacheEntry = (CacheEntry) object;
        if (id == null || cacheEntry.id == null) {
            return super.equals(object);
        }
        return id.equals(cacheEntry.id);
    }

    /**
     * This overrides the method in Object.
     * It returns the hashCode of the cache id.
     * 
     * @return The hashCode.
     */
    @Override
    public int hashCode() {
        if (id == null) {
            return CacheEntry.class.hashCode();
        }
        return id.hashCode();
    }

    /**
     * This gets the time limit on this cache entry.
     * 
     * @param The time limit.
     */
    @Override
    public int getTimeLimit() {
        return timeLimit;
    }

    /**
     * This gets the time limit on this cache entry.
     * 
     * @param The time limit.
     */
    public int getInactivity() { // CPF-Inactivity
        return inactivity;
    }

    /**
     * This gets the expiration time from the expirationTime variable.
     * 
     * @return The expiration time.
     */
    @Override
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * This sets the expiration time to the expirationTime variable.
     */
    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
        this.timeLimit = (int) ((this.expirationTime - System.currentTimeMillis()) / 1000L);
    }

    /**
     * This gets the priority in the priority variable.
     * 
     * @return The priority.
     */
    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * This gets the sharing policy in the sharingPolicy variable.
     * Included for forward compatibility with distributed caches.
     * 
     * @return The sharing policy.
     */
    @Override
    public int getSharingPolicy() {
        return sharingPolicy;
    }

    @Override
    public boolean isBatchEnabled() {
        return true;
    }

    /**
     * The unique identifier of this cached entry
     * 
     * @return The String id of this Cache Entry.
     */
    @Override
    public String getId() {
        if (id != null) { //CCC-O
            return id.toString(); //CCC-O
        }
        return null;
    }

    @Override
    public Object getIdObject() { //SKS-O
        return id;
    }

    public byte[] getSerializedId() { //SKS-O
        return serializedId;
    }

    /**
     * gets this entry's value without expanding any subfragments.
     * called only for debug/admin purposes!
     */
    @Override
    public byte[] getDisplayValue() {
        getValue();

        if (value == null)
            return new byte[0];

        if (value instanceof GenerateContents) {
            try {
                return ((GenerateContents) value).generateContents();
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheEntry.getDisplayValue", "887", this);
            }
        }

        Object dValue = value;
        if (value instanceof byte[]) {
            try {
                dValue = SerializationUtility.deserialize((byte[]) value, cacheName);
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheEntry.getDisplayValue", "896", this);
            }
        }

        byte[] returnme = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos, "UTF8"));
            pw.print(dValue);
            pw.flush();
            returnme = baos.toByteArray();
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.CacheEntry.getDisplayValue", "908", this);
            Tr.error(tc, "dynacache.error", e.getMessage());
            if (tc.isDebugEnabled())
                Tr.debug(tc, "error getting value for CacheEntry " + id + ": " + e.getMessage());
        }
        return returnme;

    }

    @Override
    public Enumeration getAliasList() { //CCC
        return new ArrayEnumerator(aliasList);
    }

    public void addAlias(Object alias) { //CCC
        ValueSet vs = new ValueSet(aliasList.length + 1);
        if (alias != null) {
            for (int i = 0; i < aliasList.length; i++)
                vs.add(aliasList[i]);
        }
        vs.add(alias);
        aliasList = vs.toArray(new Object[vs.size()]);
        serializedAliasList = null;
    }

    public void removeAlias(Object alias) { //CCC
        ValueSet vs = new ValueSet(aliasList.length);
        if (alias != null) {
            for (int i = 0; i < aliasList.length; i++)
                vs.add(aliasList[i]);
        }
        vs.remove(alias);
        aliasList = vs.toArray(new Object[vs.size()]);
        serializedAliasList = null;
    }

    /**
     * Get's the userMetaData
     */
    @Override
    public Object getUserMetaData() {
        if (serializedUserMetaData != null) {
            try {
                userMetaData = SerializationUtility.deserialize(serializedUserMetaData, cacheName);
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheEntry.getUserMetaData", "600", this);
            }
            serializedUserMetaData = null;
        }
        return userMetaData;
    }

    /**
     * Set's the userMetaData
     */
    protected void setUserMetaData(Object userMetaData) { //CCC
        this.userMetaData = userMetaData;
        serializedUserMetaData = null;
    }

    /**
     * Set the skipMemoryWriteToDisk boolean
     */
    protected void setSkipMemoryAndWriteToDisk(boolean skipMemoryAndWriteToDisk) {
        this.skipMemoryAndWriteToDisk = skipMemoryAndWriteToDisk;
    }

    /**
     * Get validator expiration time
     * 
     * @return validator expiration time in long
     */
    @Override
    public long getValidatorExpirationTime() {
        return this.validatorExpirationTime;
    }

    /**
     * Set validator expiration time
     * 
     * @param validator expiration time
     */
    public void setValidatorExpirationTime(long validatorExpirationTime) {
        this.validatorExpirationTime = validatorExpirationTime;
    }

    /**
     * Get's the vbcSource - INVALID_NOT_USED, INVALID_MEMORY_HIT, INVALID_DISK_HIT or INVALID_REMOTE_HIT.
     */
    public int getVBCSource() {
        return this.vbcSource;
    }

    /**
     * Set's the vbcSource
     */
    protected void setVBCSource(int vbcSource) {
        this.vbcSource = vbcSource;
    }

    /**
     * Check whether the entry is invalid VBC or not. Return true if VBC < t < RET. If validatorExpirationTime is not used, it will
     * return false.
     * 
     * @return boolean to indicate the entry invalid VBC or not.
     */
    @Override
    public boolean isInvalid() {
        if (this.validatorExpirationTime != -1) {
            return (System.currentTimeMillis() - this.validatorExpirationTime) >= 0 ? true : false;
        }
        return false;
    }

    @Override
    public int getCacheType() {
        return cacheType;
    }

    /**
     * Returns the time of the entry was first created.
     * 
     * @Return the time of the entry was first created.
     */
    @Override
    public long getCreationTime() {
        return this.timeStamp;
    }

    /*
     * debug code for cacheentry problems
     * LinkedList history = new LinkedList();
     * 
     * public void addHistory(String msg) {
     * history.addLast(msg+" id="+id+" templates="+templates+" invalid="+invalid+" lruEvicted="+lruEvicted+" pinCount="+pinCount);
     * if (history.size()>20)
     * history.removeFirst();
     * }
     * 
     * public void dumpHistory() {
     * System.out.println("history for: "+this);
     * while (!history.isEmpty())
     * System.out.println(history.removeFirst());
     * }
     */

    static public class LRUHead implements Iterator {
        public int priority = 0;
        private CacheEntry head;
        private CacheEntry tail;

        private CacheEntry _iterator = null;

        public void addFirst(CacheEntry cacheEntry) {
            cacheEntry._previous = null;
            cacheEntry._next = head;
            if (head != null)
                head._previous = cacheEntry;
            head = cacheEntry;
            if (tail == null)
                tail = cacheEntry;
        }

        public void addLast(CacheEntry cacheEntry) {
            cacheEntry._previous = tail;
            cacheEntry._next = null;
            if (head == null) {
                head = cacheEntry;
            } else {
                tail._next = cacheEntry;
            }
            tail = cacheEntry;

        }

        public boolean isEmpty() {
            return head == null;
        }

        public boolean isLast(CacheEntry cacheEntry) {
            return tail == cacheEntry;
        }

        // iterator is not thread safe! - done for high efficiency
        public Iterator iterator() {
            _iterator = head;
            return this;
        }

        @Override
        public Object next() {
            CacheEntry ret = _iterator;
            _iterator = _iterator._next;
            return ret;
        }

        @Override
        public boolean hasNext() {
            return _iterator != null;
        }

        @Override
        public void remove() {
            throw new IllegalStateException("remove not implemented");
        }

        public void remove(CacheEntry cacheEntry) {

            if (cacheEntry.lruHead != this && tc.isDebugEnabled()) {
                Tr.debug(tc, "internal error for cacheEntry=" + cacheEntry);
                Tr.debug(tc, "_next=" + cacheEntry._next);
                Tr.debug(tc, "_previous=" + cacheEntry._previous);
                Tr.debug(tc, "lruHead=" + cacheEntry.lruHead);
                Tr.debug(tc, "this=" + this);
            }
            if (head == cacheEntry) {
                head = cacheEntry._next;
                if (head != null)
                    head._previous = null;
            } else {
                cacheEntry._previous._next = cacheEntry._next;
                if (cacheEntry._next != null)
                    cacheEntry._next._previous = cacheEntry._previous;
            }
            if (tail == cacheEntry) {
                tail = cacheEntry._previous;
            }
            cacheEntry._next = cacheEntry._previous = null;
            cacheEntry.lruHead = null;
        }

        public CacheEntry removeFirst() {
            CacheEntry ret = head;
            if (ret != null) {
                head = ret._next;
                if (head != null)
                    head._previous = null;
                else
                    tail = null;
            }
            return ret;
        }
    }

    static class ArrayEnumerator implements Enumeration {
        int pos = 0;
        Object array[];

        public ArrayEnumerator(Object[] array) {
            this.array = array;
        }

        @Override
        public boolean hasMoreElements() {
            return array.length > pos;
        }

        @Override
        public Object nextElement() {
            return array[pos++];
        }
    }

    //--------------------------------------------------------------------
    // CacheEntry pooling
    //--------------------------------------------------------------------
    static class CacheEntryPool extends ObjectPool {

        DCache cache = null;
        String cacheName = null;

        public CacheEntryPool(int size, DCache cache) {
            super("CacheEntry.Pool", size);
            this.cache = cache;
            this.cacheName = cache.getCacheName();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "CacheEntryPool created for cache " + cacheName);
        }

        @Override
        public Object createObject() {
            CacheEntry ce = new CacheEntry();
            ce.reset();
            ce.cacheName = this.cacheName;
            return ce;
        }

        //---------------------------------------------
        // Allocate ( or create/allocate ) a CacheEntry
        //---------------------------------------------
        public CacheEntry allocate() {
            CacheEntry ce = (CacheEntry) remove();
            ce.cacheName = this.cacheName;
            ce.cacheEntryPool = this;
            return ce;
        }
        //---------------------------------------------
    }

    //--------------------------------------------------------------------

    //---------------------------------
    // Allocate a CacheEntry pool
    //---------------------------------
    static public CacheEntryPool createCacheEntryPool(DCache cache, int size) {
        CacheEntryPool pool = new CacheEntryPool(size, cache);
        return pool;
    }

    //---------------------------------

    //---------------------------------
    // Release an allocated CacheEntry
    //---------------------------------
    @Override
    public void finish() {
        // An assertion failure here means this CE
        // did not come from a CE pool and this
        // should never be the case;
        //assert cacheEntryPool != null;
        // if cacheEntryPool is null, the cacheEntry is not allocated from the pool
        if (cacheEntryPool != null) {
            synchronized (cacheEntryPool.cache) {
                decRefCount();
                if (refCount.get() <= 0) {
                    if (refCount.get() < 0)
                        refCount.set(0);
                    if (removeWhenUnpinned && refCount.get() == 0) {
                        returnToPool();
                    }
                }
            }
        } else {
            synchronized (this.id) {
                decRefCount();
                if (refCount.get() <= 0) {
                    reset();
                }
            }
        }
    }

    //---------------------------------

    //---------------------------------
    // Release an allocated CacheEntry.
    // This is only called while synch'd on the Cache object.
    //---------------------------------
    protected void returnToPool() {
        CacheEntryPool cep = cacheEntryPool;
        // An assertion failure here means this CE is being
        // returned to the pool while in use and this
        // should never be the case;
        assert refCount.get() == 0;
        if (lruHead != null)
            lruHead.remove(this);
        reset();
        cep.add(this);
    }

    //---------------------------------

    /**
     * Computes the best-effort size of the cache entry's value. Returns -1 if
     * value could not be computed.
     * 
     */
    @Override
    public long getCacheValueSize() {
        long valuesize = -1;
        if (this.value != null) {
            Object localValue = this.value;
            valuesize = ObjectSizer.getSize(localValue);
        } else {
            if (this.serializedValue != null) {
                byte[] localSerializedValue = this.serializedValue;
                valuesize = ObjectSizer.getSize(localSerializedValue);
            }
        }

        // System.out.println("Returning cacheValueSize = " + valuesize);
        return valuesize;
    }

    // ---------------------------------
    /**
     * Converts the cacheEntry to a string
     */

    @Override
    public String toString() {

        StringBuffer sb = new StringBuffer();

        sb.append("\n[CacheEntry]").
                        append("\nCacheId: ").
                        append(id).
                        append("\nuserMetaData: ").
                        append(userMetaData).
                        append("\npriority: ").
                        append(priority).
                        append("\ntimeLimit: ").
                        append(timeLimit).
                        append("\ninactivity: ").
                        append(inactivity).
                        append("\nexpirationTime: ").
                        append(expirationTime);
        if (validatorExpirationTime != -1) {
            sb.append("\nvalidatorExpirationTime: ");
            sb.append(validatorExpirationTime);
            sb.append("\nvalid: ");
            sb.append(!isInvalid());
        }
        sb.append("\nloadedFromDisk: ").
                        append(loadedFromDisk).
                        append("\ncacheValueSize: ").
                        append(getCacheValueSize()).
                        append("\ncacheValueHashcode: ").
                        append(this.valueHashcode).

                        append("\nskipMemoryAndWriteToDisk: ").
                        append(skipMemoryAndWriteToDisk);

        if (externalCacheGroupId != null) {
            sb.append("\nexternalCacheGroupId: ").
                            append(externalCacheGroupId);
        }

        if (tc.isDebugEnabled()) {
            sb.append("\nrefCount=");
            sb.append(refCount);
        }

        for (int i = 0; _templates != null && i < _templates.length; i++) {
            sb.append("\n[template " + i + "]");
            sb.append(_templates[i]);
        }

        for (int i = 0; _dataIds != null && i < _dataIds.length; i++) {
            sb.append("\n[Dependency " + i + "]");
            sb.append(_dataIds[i]);
        }

        for (int i = 0; aliasList != null && i < aliasList.length; i++) {
            sb.append("\n[Aliases " + i + "]");
            sb.append(aliasList[i]);
        }

        return sb.toString();
    }

    /**
     * 
     * @return an estimated size of and CacheEntry in memory.
     *         Please note that getObjectSize() is an estimate and NOT the exact memory footprint in the heap.
     */
    @Override
    public long getObjectSize() {

        long totalSize = ObjectSizer.CACHEENTRY_INITIAL_PER_ENTRY_OVERHEAD; // overhead of CacheEntry
        long size = 0;
        if (this.id != null) {
            Object localId = this.id;
            size = ObjectSizer.getSize(localId);
            if (size == -1) {
                //DYNA1067E=DYNA1067E: The cache size in MB feature is disabled because the cached object is not sizable.  ClassName={0}   Type={1}
                Tr.error(tc, "DYNA1067E", new Object[] { this.id.getClass().getName(), "cache-id" });
                return -1;
            } else {
                totalSize += size;
            }
        }

        if (this.serializedId != null) {
            byte[] localSerializedId = this.serializedId;
            totalSize += ObjectSizer.getSize(localSerializedId);
        }

        if (this.value != null) {
            Object localValue = this.value;
            size = ObjectSizer.getSize(localValue);
            if (size == -1) {
                //DYNA1067E=DYNA1067E: The cache size in MB feature is disabled because the cached object is not sizable.  ClassName={0}   Type={1}
                Tr.error(tc, "DYNA1067E", new Object[] { this.value.getClass().getName(), "cache-value" });
                return -1;
            } else {
                totalSize += size;
            }
        } else {
            if (this.serializedValue != null) {
                byte[] localSerializedValue = this.serializedValue;
                totalSize += ObjectSizer.getSize(localSerializedValue);
            }
        }

        if (this.userMetaData != null) {
            Object localUserMetaData = this.userMetaData;
            size = ObjectSizer.getSize(localUserMetaData);
            if (size == -1) {
                //DYNA1067E=DYNA1067E: The cache size in MB feature is disabled because the cached object is not sizable.  ClassName={0}   Type={1}
                Tr.error(tc, "DYNA1067E", new Object[] { this.userMetaData.getClass().getName(), "metadata" });
                return -1;
            } else {
                totalSize += size;
            }
        }

        if (this.serializedUserMetaData != null) {
            byte[] localSerializedUserMetaData = this.serializedUserMetaData;
            totalSize += ObjectSizer.getSize(localSerializedUserMetaData);
        }

        if (this._dataIds != null) {
            if (this._dataIds.length > 0) {
                totalSize = totalSize + ObjectSizer.OBJECT_ARRAY_OVERHEAD + this._dataIds.length * 16;
                for (int i = 0; i < this._dataIds.length; i++) {
                    Object localDataIds = this._dataIds[i];
                    size = ObjectSizer.getSize(localDataIds);
                    if (size == -1) {
                        //DYNA1067E=DYNA1067E: The cache size in MB feature is disabled because the cached object is not sizable.  ClassName={0}   Type={1}
                        Tr.error(tc, "DYNA1067E", new Object[] { localDataIds.getClass().getName(), "dep-id" });
                        return -1;
                    } else {
                        totalSize += size;
                    }
                }
            }
        }
        if (this._serializedDataIds != null) {
            if (this._serializedDataIds.length > 0) {
                totalSize = totalSize + ObjectSizer.BYTE_ARRAY_OVERHEAD + this._serializedDataIds.length * 16;
                for (int i = 0; i < this._serializedDataIds.length; i++) {
                    Object localSerializedDataIds = this._serializedDataIds[i];
                    totalSize += ObjectSizer.getSize(localSerializedDataIds);
                }
            }
        }

        if (this._templates != null) {
            if (this._templates.length > 0) {
                totalSize = totalSize + ObjectSizer.OBJECT_ARRAY_OVERHEAD + this._templates.length * 16;
                for (int i = 0; i < this._templates.length; i++) {
                    String localTemplates = this._templates[i];
                    totalSize += ObjectSizer.getSize(localTemplates);
                }
            }
        }

        if (this.aliasList != null) {
            if (this.aliasList.length > 0) {
                totalSize = totalSize + ObjectSizer.OBJECT_ARRAY_OVERHEAD + this.aliasList.length * 16;
                for (int i = 0; i < this.aliasList.length; i++) {
                    Object localAliasList = this.aliasList[i];
                    size = ObjectSizer.getSize(localAliasList);
                    if (size == -1) {
                        //DYNA1067E=DYNA1067E: The cache size in MB feature is disabled because the cached object is not sizable.  ClassName={0}   Type={1}
                        Tr.error(tc, "DYNA1067E", new Object[] { localAliasList.getClass().getName(), "alias-id" });
                        return -1;
                    } else {
                        totalSize += size;
                    }
                }
            }
        }

        if (this.serializedAliasList != null) {
            if (this.serializedAliasList.length > 0) {
                totalSize = totalSize + ObjectSizer.BYTE_ARRAY_OVERHEAD + this.serializedAliasList.length * 16;
                for (int i = 0; i < this.serializedAliasList.length; i++) {
                    Object localSerializedAliasList = this.serializedAliasList[i];
                    totalSize += ObjectSizer.getSize(localSerializedAliasList);
                }
            }
        }

        if (null != externalCacheGroupId) {
            String localExternalCacheGroupId = externalCacheGroupId;
            totalSize += ObjectSizer.getSize(localExternalCacheGroupId);
        }

        return totalSize;
    }

    /**
     * @return estimate (serialized) size of CacheEntry. It is called by DRS to calculate the payload.
     */
    public long getSerializedSize() {
        long totalSize = 0;
        if (this.serializedId != null) {
            byte[] localSerializedId = this.serializedId;
            totalSize += ObjectSizer.getSize(localSerializedId);
        }
        if (this.serializedValue != null) {
            byte[] localSerializedValue = this.serializedValue;
            totalSize += ObjectSizer.getSize(localSerializedValue);
        }
        if (this.serializedUserMetaData != null) {
            byte[] localSerializedUserMetaData = this.serializedUserMetaData;
            totalSize += ObjectSizer.getSize(localSerializedUserMetaData);
        }
        if (this._serializedDataIds != null) {
            if (this._serializedDataIds.length > 0) {
                for (int i = 0; i < this._serializedDataIds.length; i++) {
                    Object localSerializedDataIds = this._serializedDataIds[i];
                    totalSize += ObjectSizer.getSize(localSerializedDataIds);
                }
            }
        }
        if (this._templates != null) {
            if (this._templates.length > 0) {
                for (int i = 0; i < this._templates.length; i++) {
                    String localTemplates = this._templates[i];
                    totalSize += ObjectSizer.getSize(localTemplates);
                }
            }
        }
        if (this.serializedAliasList != null) {
            if (this.serializedAliasList.length > 0) {
                for (int i = 0; i < this.serializedAliasList.length; i++) {
                    Object localSerializedAliasList = this.serializedAliasList[i];
                    totalSize += ObjectSizer.getSize(localSerializedAliasList);
                }
            }
        }
        if (this.externalCacheGroupId != null) {
            String localExternalCacheGroupId = externalCacheGroupId;
            totalSize += ObjectSizer.getSize(localExternalCacheGroupId);
        }

        //System.out.println("CacheEntry.getSerializedSize(): id=" + id + " size=" + totalSize);
        return totalSize;
    }

    @Override
    public String getExternalCacheGroupId() {
        return externalCacheGroupId;
    }

    public int getRefCount() {
        return refCount.get();
    }

    public void incRefCount() {
        refCount.incrementAndGet();
        if (isRefCountingEnabled()) {
            getCache().getRefCountLeakMap().put(id, Cache.extractStackTrace(this));
        }

    }

    public void decRefCount() {

        refCount.decrementAndGet();

        if (isRefCountingEnabled()) {
            if (refCount.get() <= 0) {
                getCache().getRefCountLeakMap().remove(id);
            } else {
                getCache().getRefCountLeakMap().put(id, Cache.extractStackTrace(this));
            }
        }
    }

    public Cache getCache() { //Caller needs to check  for null because Cache is a transient field		
        Cache c = null;
        if (cacheEntryPool != null && cacheEntryPool.cache != null) {
            c = (Cache) cacheEntryPool.cache;
        } else {
            c = (Cache) ServerCache.getCache(cacheName);
        }
        return c;
    }

    public boolean isRefCountingEnabled() { //returns false if the Cache is null 
        boolean rc = false;
        if (getCache() != null) {
            rc = ((CacheConfig) getCache().getCacheConfig()).isRefCountTrackingEnabled();
        }

        return rc;
    }

}
