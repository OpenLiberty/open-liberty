/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bytebuffer.internal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

/**
 * Implementation of a WsByteBuffer pool manager.
 */
public class WsByteBufferPoolManagerImpl implements WsByteBufferPoolManager {

    private static final TraceComponent tc = Tr.register(WsByteBufferPoolManagerImpl.class,
                                                         MessageConstants.WSBB_TRACE_NAME,
                                                         MessageConstants.WSBB_BUNDLE);

    private static class DefaultPoolHolder {
        static final WsByteBufferPoolManager defaultInstance;
        static {
            WsByteBufferPoolManager newInstance;
            try {
                newInstance = new WsByteBufferPoolManagerImpl(new AtomicReference<DirectByteBufferHelper>());
            } catch (WsBBConfigException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Error constructing default instance", e);
                }
                // not possible w/ default config
                newInstance = null;
            }
            defaultInstance = newInstance;
        }
    }

    private static int CONFIG_DEFAULT = -1;
    private static int MEMORY_LEAK_INTERVAL_MIN = 5;
    private static int MEMORY_LEAK_INTERVAL_MAX = 3600; // in seconds
    private static int VALIDATE_OK = 0;
    private static int VALIDATE_ERROR = 1;

    /** Reference to single instance of this class */
    private static WsByteBufferPoolManager instanceRef = null;

    private final AtomicReference<DirectByteBufferHelper> directByteBufferHelper;

    // For Speed and Simplicity, the pools will be kept in an array, and
    // another array for their entry sizes, and the number of Pool will be tracked locally.
    // We will assume pool entry sizes run from small to large in the pool array
    // If not, the algorithms below will break
    private WsByteBufferPool[] pools = null;
    private WsByteBufferPool[] poolsDirect = null;
    private int[] poolSizes = null;

    // these are the local pool size, global pools will be 10 times larger in depth
    private static int[] defaultPoolSizes = { 32, 1024, 8192, 16384, 24576, 32768, 49152, 65536 };
    private static int[] defaultPoolDepths = { 30, 30, 30, 20, 20, 20, 10, 10 };

    private static final String MEM_LEAK_INTERVAL = "memoryLeakDetectionInterval";
    private static final String MEM_LEAK_FILE = "memoryLeakOutputFile";
    private static final String TRUSTED_USERS = "trustedUsers";
    private static final String POOL_SIZES = "poolSizes";
    private static final String POOL_DEPTHS = "poolDepths";
    private static final String CLEAN_UP = "cleanUp";
    private static final String CONFIG_ALIAS = "bytebuffer";

    private boolean trustedUsers = false;
    private long lastTimeCheck = 0L;
    private int leakDetectionInterval = -1;
    private String leakDetectionOutput = null;
    private final Object leakDetectionSyncObject = new Object() {};

    /**
     * Create the one WsByteBufferPool Manager that is to be used.
     *
     * @param directByteBufferHelper
     *
     * @param properties
     * @throws WsBBConfigException
     */
    public WsByteBufferPoolManagerImpl(AtomicReference<DirectByteBufferHelper> directByteBufferHelper, Map<String, Object> properties) throws WsBBConfigException {
        this.directByteBufferHelper = directByteBufferHelper;

        String key = null;
        Object value = null;

        int result = VALIDATE_OK;
        int leakInterval = -1;
        String leakFile = "";
        int[] sizes = null;
        int[] depths = null;

        try {
            for (Entry<String, Object> prop : properties.entrySet()) {
                if (result != VALIDATE_OK) {
                    break;
                }
                key = prop.getKey();
                value = prop.getValue();

                if (key.startsWith("service.") ||
                    key.startsWith("component.") ||
                    key.startsWith("config.") ||
                    key.startsWith("parentPid") ||
                    key.startsWith("id")) {
                    // skip osgi standard properties
                    continue;
                }

                if (null == value) {
                    result = VALIDATE_ERROR;
                    continue;
                }

                if (key.equalsIgnoreCase(MEM_LEAK_INTERVAL)) {
                    // convert and check
                    leakInterval = MetatypeUtils.parseInteger(CONFIG_ALIAS, MEM_LEAK_INTERVAL, value, leakInterval);
                    result = testMemoryLeakDetectionInterval(leakInterval);
                    if (result == VALIDATE_OK) {
                        // convert to milliseconds
                        leakInterval = leakInterval * 1000;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(MEM_LEAK_FILE)) {
                    // convert and check
                    leakFile = (String) value;
                    continue;
                }

                if (key.equalsIgnoreCase(TRUSTED_USERS)) {
                    // convert and check
                    this.trustedUsers = MetatypeUtils.parseBoolean(CONFIG_ALIAS, MEM_LEAK_INTERVAL, value, this.trustedUsers);
                    continue;
                }

                if (key.equalsIgnoreCase(POOL_SIZES)) {
                    sizes = MetatypeUtils.parseIntegerArray(CONFIG_ALIAS, POOL_SIZES, value, sizes);
                    continue;
                }

                if (key.equalsIgnoreCase(POOL_DEPTHS)) {
                    depths = MetatypeUtils.parseIntegerArray(CONFIG_ALIAS, POOL_DEPTHS, value, depths);
                    continue;
                }

                if (key.equalsIgnoreCase(CLEAN_UP)) {
                    continue;
                }

                Tr.warning(tc, MessageConstants.UNRECOGNIZED_CUSTOM_PROPERTY, new Object[] { key });
            }
        } catch (NumberFormatException x) {
            Tr.error(tc, MessageConstants.CONFIG_VALUE_NUMBER_EXCEPTION,
                     new Object[] { key, value });
            WsBBConfigException e = new WsBBConfigException("NumberFormatException processing name: "
                                                            + key + " value: " + value, x);
            FFDCFilter.processException(e, getClass().getName(), "102", this);
            throw e;
        }

        if (result != VALIDATE_OK) {
            Tr.error(tc, MessageConstants.NOT_VALID_CUSTOM_PROPERTY, new Object[] { key, value });
            WsBBConfigException e = new WsBBConfigException("Property has a value that is not valid, name: " + key + " value: [" + value + "]");
            FFDCFilter.processException(e, getClass().getName(), "103", this);
            throw e;
        }

        if (leakInterval != -1) {
            if (leakFile != null && 0 != leakFile.length()) {
                try {
                    setLeakDetectionSettings(leakInterval, leakFile);
                } catch (IOException x) {
                    Tr.error(tc, MessageConstants.NOT_VALID_CUSTOM_PROPERTY,
                             new Object[] { MEM_LEAK_FILE, leakFile });
                    WsBBConfigException e = new WsBBConfigException("Error with leak detection file, " + MEM_LEAK_FILE + "=[" + leakFile + "]", x);
                    FFDCFilter.processException(e,
                                                getClass().getName(), "104", this);
                    throw e;
                }
            } else {
                Tr.error(tc, MessageConstants.NOT_VALID_CUSTOM_PROPERTY,
                         new Object[] { MEM_LEAK_FILE, "null" });
                WsBBConfigException e = new WsBBConfigException("Leak interval set without an output file");
                FFDCFilter.processException(e, getClass().getName(), "105", this);
                throw e;
            }
        }

        if ((sizes == null) || (depths == null)) {
            // use default sizes
            initialize(defaultPoolSizes, defaultPoolDepths);
        } else if (sizes.length != depths.length) {
            Tr.error(tc, MessageConstants.POOL_MISMATCH, new Object[] { sizes, depths });
            WsBBConfigException e = new WsBBConfigException("Mismatch in pool sizes (" + sizes.length + ") and depths(" + depths.length + ")");
            FFDCFilter.processException(e, getClass().getName(), "106", this);
            throw e;
        } else {
            initialize(sizes, depths);
        }

        instanceRef = this;
    }

    /**
     * Create a pool manager with the default configuration.
     *
     * @throws WsBBConfigException
     */
    public WsByteBufferPoolManagerImpl(AtomicReference<DirectByteBufferHelper> directByteBufferHelper) throws WsBBConfigException {
        this(directByteBufferHelper, new HashMap<String, Object>());
    }

    /**
     * This class implements the singleton pattern. This method is provided
     * to return a reference to the single instance of this class in existence.
     *
     * @return WsByteBufferPoolManager
     */
    public static WsByteBufferPoolManager getRef() {
        if (instanceRef == null) {
            return DefaultPoolHolder.defaultInstance;
        }

        return instanceRef;
    }

    /**
     * Initialize the pool manager with the number of pools, the entry sizes for each
     * pool, and the maximum depth of the free pool.
     *
     * @param bufferEntrySizes the memory sizes of each entry in the pools
     * @param bufferEntryDepths the maximum number of entries in the free pool
     */
    public void initialize(int[] bufferEntrySizes, int[] bufferEntryDepths) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "initialize");
        }

        // order both lists from smallest to largest, based only on Entry Sizes
        int len = bufferEntrySizes.length;
        int[] bSizes = new int[len];
        int[] bDepths = new int[len];
        int sizeCompare;
        int depth;
        int sizeSort;
        int j;

        for (int i = 0; i < len; i++) {
            sizeCompare = bufferEntrySizes[i];
            depth = bufferEntryDepths[i];
            // go backwards, for speed, since first Array List is
            // probably already ordered small to large
            for (j = i - 1; j >= 0; j--) {
                sizeSort = bSizes[j];
                if (sizeCompare > sizeSort) {
                    // add the bigger one after the smaller one
                    bSizes[j + 1] = sizeCompare;
                    bDepths[j + 1] = depth;
                    break;
                }
                // move current one down, since it is bigger
                bSizes[j + 1] = sizeSort;
                bDepths[j + 1] = bDepths[j];
            }
            if (j < 0) {
                // smallest so far, add it at the front of the list
                bSizes[0] = sizeCompare;
                bDepths[0] = depth;
            }
        }

        boolean tracking = trackingBuffers();
        this.pools = new WsByteBufferPool[len];
        this.poolsDirect = new WsByteBufferPool[len];
        this.poolSizes = new int[len];
        for (int i = 0; i < len; i++) {
            // make backing pool 10 times larger than local pools
            this.pools[i] = new WsByteBufferPool(bSizes[i], bDepths[i] * 10, tracking, false);
            this.poolsDirect[i] = new WsByteBufferPool(bSizes[i], bDepths[i] * 10, tracking, true);
            this.poolSizes[i] = bSizes[i];
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Number of pools created: " + this.poolSizes.length);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "initialize");
        }
    }

    /**
     * Set the memory leak detection parameters. If the interval is 0 or
     * greater, then the detection code will enabled.
     *
     * @param interval the minimum amount of time between leak detection code
     *            will be ran. Also the minimum amount of time that a buffer can go without
     *            being accessed before it will be flagged as a possible memory leak.
     * @param output The name of the output file where the memory leak information
     *            will be written.
     * @throws IOException
     */
    public void setLeakDetectionSettings(int interval, String output) throws IOException {
        this.leakDetectionInterval = interval;
        this.leakDetectionOutput = TrConfigurator.getLogLocation() + File.separator + output;

        if ((interval > -1) && (output != null)) {
            // clear file
            FileWriter outFile = new FileWriter(this.leakDetectionOutput, false);
            outFile.close();
        }
    }

    private int testMemoryLeakDetectionInterval(int value) {

        if (value == CONFIG_DEFAULT) {
            return VALIDATE_OK;
        }

        if ((value < MEMORY_LEAK_INTERVAL_MIN) || (value > MEMORY_LEAK_INTERVAL_MAX)) {
            return VALIDATE_ERROR;
        }
        return VALIDATE_OK;
    }

    /**
     * Check whether the buffer leak detection code is enabled or not.
     *
     * @return boolean
     */
    private boolean trackingBuffers() {
        return this.leakDetectionInterval > -1;
    }

    /**
     * Query the interval used for leak detection.
     *
     * @return int
     */
    public int getLeakDetectionInterval() {
        return this.leakDetectionInterval;
    }

    /**
     * Access the sync lock object for the leak detection.
     *
     * @return Object
     */
    public Object getLeakDetectionSyncObject() {
        return this.leakDetectionSyncObject;
    }

    @Override
    public WsByteBuffer allocate(int entrySize) {
        return allocateCommon(entrySize, false);
    }

    @Override
    public WsByteBuffer allocateDirect(int entrySize) {
        return allocateCommon(entrySize, true);
    }

    /**
     * Allocate a buffer which will use tha FileChannel until the buffer needs
     * to be used in a "non-FileChannel" way.
     *
     * @param fc FileChannel to use for this buffer
     * @return FCWsByteBuffer buffer which can now be used.
     */
    @Override
    public WsByteBuffer allocateFileChannelBuffer(FileChannel fc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "allocateFileChannelBuffer");
        }
        return new FCWsByteBufferImpl(fc);
    }

    /**
     * Allocate a buffer from a buffer pool. Choose the buffer pool which
     * is closest to the desired size, but not less than the desired size.
     *
     * @param entrySize the amount of memory be requested for this allocation
     * @param direct
     * @return WsByteBuffer buffer which can now be used.
     */
    public WsByteBuffer allocateCommon(int entrySize, boolean direct) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "allocateCommon: " + entrySize);
        }

        // see if we should look for leaks
        if (trackingBuffers()) {
            lookForLeaks(false);
        }

        for (int i = 0; i < this.poolSizes.length; i++) {
            ByteBuffer bytebufferFromPool = null;
            int intPoolSize = this.poolSizes[i];

            // find the correct pool from which to get the buffer
            if (entrySize <= intPoolSize) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "found a pool of size: " + intPoolSize);
                }

                WsByteBufferPool oWsByteBufferPool = null;
                // get the pool that should be used
                if (direct) {
                    oWsByteBufferPool = this.poolsDirect[i];
                } else {
                    oWsByteBufferPool = this.pools[i];
                }

                // allocate an entry from the pool
                PooledWsByteBufferImpl pooledWSBB = oWsByteBufferPool.getEntry();
                // reset released flag so this buffer can be used again
                pooledWSBB.resetReleaseCalled();

                bytebufferFromPool = pooledWSBB.getWrappedByteBufferNonSafe();
                if (bytebufferFromPool == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "no ByteBuffer so alloc a new one");
                    }
                    // allocate the ByteBuffer and store it in the PoolEntry
                    if (direct) {
                        allocateBufferDirect(pooledWSBB, intPoolSize, false);
                        pooledWSBB.setIsDirectPool(true);
                    } else {
                        bytebufferFromPool = ByteBuffer.allocate(intPoolSize);
                        pooledWSBB.setIsDirectPool(false);
                        pooledWSBB.setByteBufferNonSafe(bytebufferFromPool);
                    }
                    // keep the pool index for this entry,to quickly access on the release
                    pooledWSBB.pool = oWsByteBufferPool;

                    // WsByteBuffer needs to call back to us for duplicates/release/slice
                    pooledWSBB.setPoolManagerRef(this);
                } else {

                    // reset the limit, position and mark to the "beginning"
                    bytebufferFromPool.clear();

                    // reset the byte order back to the default
                    bytebufferFromPool.order(java.nio.ByteOrder.BIG_ENDIAN);

                    // reset the refCount
                    pooledWSBB.intReferenceCount = 1;

                    // reset buffer optomization algorithmic variables.
                    pooledWSBB.getMin = -1;
                    pooledWSBB.getMax = -1;
                    pooledWSBB.putMin = -1;
                    pooledWSBB.putMax = -1;
                    pooledWSBB.readMin = -1;
                    pooledWSBB.readMax = -1;
                    pooledWSBB.actionState = PooledWsByteBufferImpl.COPY_ALL_INIT;
                    pooledWSBB.quickBufferAction = WsByteBufferImpl.NOT_ACTIVATED;
                }

                // set the limit to the size, the user can set limit to
                // capacity if they want to use any possible extra space
                pooledWSBB.limit(entrySize);

                if ((TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    || trackingBuffers()) {

                    Throwable t = new Throwable();
                    StackTraceElement[] ste = t.getStackTrace();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        if (ste.length >= 3) {
                            Tr.debug(tc,
                                     "BUFFER OBTAINED: Allocate: Calling Element: " + ste[2] + " Main ID: " + pooledWSBB.getID());
                        }
                        Tr.debug(tc, "Buffer allocated: " + pooledWSBB);
                    }

                    if (trackingBuffers()) {
                        String sEntry = fillOutStackTrace(" (Allocate) ", ste);
                        pooledWSBB.setOwnerID(sEntry);

                        pooledWSBB.addWsByteBuffer(pooledWSBB);
                        pooledWSBB.addOwner(pooledWSBB.getOwnerID());
                    }
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "allocateCommon");
                }
                return pooledWSBB;
            }
        } // end-pool loop

        // Either we have no pools or the allocate size is bigger
        // than our biggest pool, so allocate "as is"
        WsByteBufferImpl buffer = new WsByteBufferImpl();

        if (direct) {
            buffer = allocateBufferDirect(buffer, entrySize, true);
        } else {
            buffer.setByteBufferNonSafe(ByteBuffer.allocate(entrySize));
        }

        // WsByteBuffer needs to call back to us for duplicates/release/slice
        buffer.setPoolManagerRef(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Buffer allocated: " + buffer);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "allocateCommon");
        }
        return buffer;
    }

    /**
     * Allocate the direct ByteBuffer that will be wrapped by the
     * input WsByteBuffer object.
     *
     * @param buffer
     * @param size
     * @param overrideRefCount
     */
    protected WsByteBufferImpl allocateBufferDirect(WsByteBufferImpl buffer,
                                                    int size, boolean overrideRefCount) {
        DirectByteBufferHelper directByteBufferHelper = this.directByteBufferHelper.get();
        ByteBuffer byteBuffer;
        if (directByteBufferHelper != null) {
            byteBuffer = directByteBufferHelper.allocateDirectByteBuffer(size);
        } else {
            byteBuffer = ByteBuffer.allocateDirect(size);
        }
        buffer.setByteBufferNonSafe(byteBuffer);
        return buffer;
    }

    /*
     * @see WsByteBufferPoolManager#wrap(byte[])
     */
    @Override
    public WsByteBuffer wrap(byte[] array) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "wrap");
        }

        WsByteBufferImpl buffer = new WsByteBufferImpl();
        buffer.setByteBuffer(ByteBuffer.wrap(array));

        buffer.setPoolManagerRef(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "wrap");
        }
        return (buffer);
    }

    /*
     * @see WsByteBufferPoolManager#wrap(byte[], int, int)
     */
    @Override
    public WsByteBuffer wrap(byte[] array, int offset, int length) throws IndexOutOfBoundsException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "wrap(byte[], int, int): offset= " + offset + ", length= " + length);
        }

        WsByteBufferImpl buffer = new WsByteBufferImpl();
        buffer.setByteBuffer(ByteBuffer.wrap(array, offset, length));

        // WsByteBuffer needs to call back to us for duplicates/slice
        buffer.setPoolManagerRef(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "wrap(byte[], int, int)");
        }
        return (buffer);
    }

    /**
     * Release a buffer from being in use.
     *
     * @param buffer
     * @param isDirectPool
     * @param pool
     */
    public void release(PooledWsByteBufferImpl buffer, boolean isDirectPool, WsByteBufferPool pool) {
        pool.release(buffer);
    }

    /**
     * Method called when a buffer is being destroyed to allow any
     * additional cleanup that might be required.
     *
     * @param buffer
     */
    protected void releasing(ByteBuffer buffer) {
        if (buffer != null && buffer.isDirect()) {
            DirectByteBufferHelper directByteBufferHelper = this.directByteBufferHelper.get();
            if (directByteBufferHelper != null) {
                directByteBufferHelper.releaseDirectByteBuffer(buffer);
            }
        }
    }

    /**
     * Duplicate a WsByteBuffer. This will mainly consist of
     * 1. Creating a new WsByteBuffer
     * 2. wrapping into this WsByteBuffer the ByteBuffer returned on
     * the duplicate() method of the ByteBuffer wrapped by the passed
     * WsByteBuffer
     * 3. Updating variables of the new WsByteBuffer
     *
     * @param buffer to duplicate
     * @return WsByteBuffer duplicated buffer
     */
    @Override
    public WsByteBuffer duplicate(WsByteBuffer buffer) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "duplicate");
        }

        // see if we should look for leaks
        if (trackingBuffers()) {
            lookForLeaks(false);
        }

        WsByteBufferImpl newBuffer = new WsByteBufferImpl();
        WsByteBufferImpl srcBuffer = (WsByteBufferImpl) buffer;

        // update the new object with pool manager specific data
        newBuffer.setPoolManagerRef(this);

        PooledWsByteBufferImpl bRoot = srcBuffer.getWsBBRoot();
        if (bRoot != null) {
            newBuffer.setWsBBRoot(bRoot);
            synchronized (bRoot) {
                bRoot.intReferenceCount++;
            }
        } else {
            RefCountWsByteBufferImpl rRoot = srcBuffer.getWsBBRefRoot();
            if (rRoot != null) {
                newBuffer.setWsBBRefRoot(rRoot);
                synchronized (rRoot) {
                    rRoot.intReferenceCount++;
                }
            }
        }

        // have the old object update the new object with object specific data
        srcBuffer.updateDuplicate(newBuffer);

        if ((TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            || (trackingBuffers())) {

            Throwable t = new Throwable();
            StackTraceElement[] ste = t.getStackTrace();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && ste.length >= 3) {
                if ((srcBuffer.getWsBBRoot() == null) || (srcBuffer.getWsBBRoot().pool == null)) {
                    Tr.debug(tc, "BUFFER OBTAINED: Duplicate: Calling Element: " + ste[2] + " Main ID: none");
                } else {
                    Tr.debug(tc, "BUFFER OBTAINED: Duplicate: Calling Element: " + ste[2] + " Main ID: " + srcBuffer.getWsBBRoot().getID());
                }
            }

            if (trackingBuffers()) {
                String sEntry = fillOutStackTrace(" (Duplicate) ", ste);
                newBuffer.setOwnerID(sEntry);
            }

            if ((newBuffer.getWsBBRoot() != null) && (newBuffer.getWsBBRoot().pool != null)) {
                if (trackingBuffers()) {
                    newBuffer.getWsBBRoot().addWsByteBuffer(newBuffer);
                    newBuffer.getWsBBRoot().owners.put(newBuffer.getOwnerID(), newBuffer.getOwnerID());
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "duplicate");
        }
        return newBuffer;
    }

    /**
     * Slice a WsByteBuffer. This will mainly consist of
     * 1. Creating a new WsByteBuffer
     * 2. wrapping into this WsByteBuffer the ByteBuffer returned on
     * the duplicate() method of the ByteBuffer wrapped by the passed
     * WsByteBuffer
     * 3. Updating the control variables of the new WsByteBuffer (such
     * setting the ID to be the same as that of the passed WsByteBuffer)
     *
     * @param buffer to slice
     * @return WsByteBuffer sliced buffer
     */
    @Override
    public WsByteBuffer slice(WsByteBuffer buffer) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "slice");
        }

        // see if we should look for leaks
        if (trackingBuffers()) {
            lookForLeaks(false);
        }

        WsByteBufferImpl newBuffer = new WsByteBufferImpl();
        WsByteBufferImpl srcBuffer = (WsByteBufferImpl) buffer;

        // update the new object with pool manager specific data
        newBuffer.setPoolManagerRef(this);

        PooledWsByteBufferImpl bRoot = srcBuffer.getWsBBRoot();
        if (bRoot != null) {
            newBuffer.setWsBBRoot(bRoot);
            synchronized (bRoot) {
                bRoot.intReferenceCount++;
            }
        } else {
            RefCountWsByteBufferImpl rRoot = srcBuffer.getWsBBRefRoot();
            if (rRoot != null) {
                newBuffer.setWsBBRefRoot(rRoot);
                synchronized (rRoot) {
                    rRoot.intReferenceCount++;
                }
            }
        }

        // have the old object update the new object with object specific data
        srcBuffer.updateSlice(newBuffer);

        if ((TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            || (trackingBuffers())) {

            Throwable t = new Throwable();
            StackTraceElement[] ste = t.getStackTrace();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && ste.length >= 3) {
                if ((srcBuffer.getWsBBRoot() == null) || (srcBuffer.getWsBBRoot().pool == null)) {
                    Tr.debug(tc, "BUFFER OBTAINED: Slice: Calling Element: " + ste[2] + " Main ID: none");
                } else {
                    Tr.debug(tc, "BUFFER OBTAINED: Slice: Calling Element: " + ste[2] + " Main ID: " + srcBuffer.getWsBBRoot().getID());
                }
            }

            if (trackingBuffers()) {
                String sEntry = fillOutStackTrace(" (Slice) ", ste);
                newBuffer.setOwnerID(sEntry);
            }

            if ((newBuffer.getWsBBRoot() != null) && (newBuffer.getWsBBRoot().pool != null)) {
                if (trackingBuffers()) {
                    newBuffer.getWsBBRoot().addWsByteBuffer(newBuffer);
                    newBuffer.getWsBBRoot().owners.put(newBuffer.getOwnerID(), newBuffer.getOwnerID());
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "slice");
        }
        return newBuffer;
    }

    /**
     * Look for entries in the inUse pool which have not been accessed for
     * a time interval. This time interval is configurable. Also only look
     * for leak if the amount of time specified by this time interval has
     * elapsed since the last time leaks were looked for. Print results
     * out to a file. The name of this file is also configurable. If the
     * force parameter is set, then look for leaks even if less time has
     * elapsed than is specified by the time interval.
     *
     * @param force if this method is to look for leaks without regard to
     *            the last time leaks were looked for, false if leaks are only to be
     *            looked for if a specified (configuable) amount of time has elapsed.
     *
     */
    public void lookForLeaks(boolean force) {
        if (this.leakDetectionInterval < -1) {
            // should never get here, return
            return;
        }

        synchronized (this.leakDetectionSyncObject) {
            try {
                // get current time
                long now = System.currentTimeMillis();

                if (force == false) {
                    if (this.lastTimeCheck == 0L) {
                        this.lastTimeCheck = now;
                        return;
                    } else if ((now - this.lastTimeCheck) < getLeakDetectionInterval()) {
                        return;
                    }
                }

                this.lastTimeCheck = now;
                int iterIndex = 1;

                for (int i = 0; i < this.poolSizes.length; i++) {
                    WsByteBufferImpl buffer = null;
                    Object[] buffers = this.pools[i].getInUse();
                    if (buffers != null) {
                        for (int j = 0; j < buffers.length; j++) {
                            PooledWsByteBufferImpl x = (PooledWsByteBufferImpl) buffers[j];

                            if (x.getallBuffers() == null) {
                                // This can happen because a new buffer has just been
                                // allocated, but our initialization of it is not
                                // complete. Don't sync the init, since we don't want
                                // to add needless logic for the leak code.
                                continue;
                            }

                            Hashtable<?, ?> possibleLeakers = (Hashtable<?, ?>) (x.getallBuffers().clone());
                            Iterator<?> it = possibleLeakers.values().iterator();

                            while (it.hasNext()) {
                                buffer = (WsByteBufferImpl) it.next();

                                if ((now - buffer.getLastAccessTime()) > getLeakDetectionInterval()) {
                                    String sOutput = iterIndex + " Possible Leak Entry: Buffer ID - " + x.getID() +
                                                     "\nNon-Direct Buffer Pool" +
                                                     "\n" + x.toString(buffer.getOwnerID());
                                    try {
                                        FileWriter outFile = new FileWriter(this.leakDetectionOutput, true);
                                        if (iterIndex == 1) {
                                            outFile.write("\n\n\n****  " + (new Date()) + "  ***\n");
                                        } else {
                                            outFile.write("\n----------\n");
                                        }
                                        outFile.write(sOutput);

                                        outFile.close();

                                    } catch (IOException e) {
                                        // do nothing if we can't print out leak debug info.
                                    }
                                    iterIndex++;
                                }
                            }
                        }
                    }
                } // end-non-direct-pool loop

                // check direct pools in the same way
                for (int i = 0; i < this.poolSizes.length; i++) {
                    WsByteBufferImpl buffer = null;
                    Object[] buffers = this.poolsDirect[i].getInUse();

                    if (buffers != null) {
                        for (int j = 0; j < buffers.length; j++) {
                            PooledWsByteBufferImpl x = (PooledWsByteBufferImpl) buffers[j];

                            if (x.getallBuffers() == null) {
                                // This can happen because a new buffer has just been
                                // allocated, but the our initialization of it is not
                                // complete. Don't sync the init, since we don't want
                                // to add needless logic for the leak code.
                                continue;
                            }
                            Hashtable<?, ?> possibleLeakers = (Hashtable<?, ?>) (x.getallBuffers().clone());
                            Iterator<?> oIterator = possibleLeakers.values().iterator();

                            while (oIterator.hasNext()) {
                                buffer = (WsByteBufferImpl) oIterator.next();

                                if ((now - buffer.getLastAccessTime()) > getLeakDetectionInterval()) {
                                    String sOutput = iterIndex + " Possible Leak Entry: Buffer ID - " + x.getID() +
                                                     "\nDirect Buffer Pool" +
                                                     "\n" + x.toString(buffer.getOwnerID());
                                    try {
                                        FileWriter outFile = new FileWriter(this.leakDetectionOutput, true);
                                        if (iterIndex == 1) {
                                            outFile.write("\n\n\n****  " + (new Date()) + "  ***\n");
                                        } else {
                                            outFile.write("\n----------\n");
                                        }
                                        outFile.write(sOutput);
                                        outFile.close();
                                    } catch (IOException e) {
                                        // do nothing if we can't print out leak debug info.
                                    }
                                    iterIndex++;
                                }
                            }
                        }
                    }
                } // end-direct-pool loop
            } catch (NullPointerException xe) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "WsByteBuffer Leak Detection Caught an NPE looking through the inUse tables");
                }
                RuntimeException re = new RuntimeException("WsByteBuffer Leak Detection Caught an NPE looking through the inUse table", xe);
                FFDCFilter.processException(re, getClass().getName(), "932", this);
                throw re;
            }
        }
    }

    private String fillOutStackTrace(String starter, StackTraceElement[] _ste) {
        StringBuilder sb = new StringBuilder(starter);

        // first two entries in the stack are usually internal to
        // our allocation routines, so we don't want to include them.
        if (_ste.length >= 3) {
            // stop a 10 deep, so we don't take up too much
            for (int i = 2; (i < _ste.length) && (i < 12); i++) {
                sb.append("\n").append(_ste[i].toString());
            }
            return sb.toString();
        }
        // internal allocation only was done
        for (int i = 0; i < _ste.length; i++) {
            sb.append("\n").append(_ste[i].toString());
        }
        return sb.toString();
    }

    /**
     * Check whether the pool-user trust flag is enabled or not.
     *
     * @return boolean
     */
    protected boolean isTrustedUsers() {
        return this.trustedUsers;
    }

    /*
     * @see WsByteBufferPoolManager#wrap(java.nio.ByteBuffer)
     */
    @Override
    public WsByteBuffer wrap(ByteBuffer buffer) {
        return wrap(buffer, false);
    }

    /**
     * @param buffer
     * @param doRefCount
     * @return WsByteBuffer
     */
    public WsByteBuffer wrap(ByteBuffer buffer, boolean doRefCount) {
        final String methodName = "wrap(ByteBuffer, boolean)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { buffer, "doRefCount=" + doRefCount });
        }

        WsByteBufferImpl oWsByteBuffer = null;
        if (doRefCount) {
            oWsByteBuffer = new RefCountWsByteBufferImpl();
        } else {
            oWsByteBuffer = new WsByteBufferImpl();
        }

        oWsByteBuffer.setByteBuffer(buffer);
        oWsByteBuffer.setPoolManagerRef(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, oWsByteBuffer);
        }
        return (oWsByteBuffer);
    }
}
