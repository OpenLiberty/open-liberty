/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.persistent.htod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.HTODDynacache;
import com.ibm.ws.cache.HTODDynacache.EvictionTableEntry;
import com.ibm.ws.cache.PrimitiveArrayPool;
import com.ibm.ws.cache.persistent.filemgr.FileManager;
import com.ibm.ws.cache.persistent.filemgr.FileManagerException;
import com.ibm.ws.cache.persistent.filemgr.FileManagerImpl;
import com.ibm.ws.cache.persistent.util.ByteArrayPlusOutputStream;
import com.ibm.ws.cache.persistent.util.ProfTimer;
import com.ibm.ws.cache.util.SerializationUtility;
import com.ibm.ws.ffdc.FFDCFilter;

public class HashtableOnDisk {
    private static final boolean IS_UNIT_TEST = false;
    private static final int RETRIEVE_KEY = 1;
    private static final int RETRIEVE_KEY_VALUE = 2;
    private static final int RETRIEVE_ALL = 3;
    public static final boolean HAS_CACHE_VALUE = true;
    public static final boolean CHECK_EXPIRED = true;
    public static final boolean ALIAS_ID = true;

    static TraceComponent tc = Tr.register(HashtableOnDisk.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    HashHeader header = null; // current hashtable header
    HTODDynacache htoddc = null;

    //
    // Default values
    //
    int defaulttablesize = 477551; // default initial value - updated 6/4/04
                                   // to actual value as structure grows

    long[] htindex = null; // cached hashtable index
    long[] new_htindex = null; // alternate index to use when doubling

    int threshold; // integer 1 to 100

    Semaphore iterationLock;
    FileManager filemgr = null; // disk allocator 
    long currentTable = 0; // active table
    String filename = null;

    boolean debug = false;

    public void setDebug(boolean d) {
        debug = d;
    }

    ByteArrayPlusOutputStream dataout = null; // reusable output stream
    byte[] databuf = null; // reusable output buffer

    ByteArrayPlusOutputStream keyout = null; // reusable output stream
    byte[] keybuf = null; // reusable input buffer

    HashtableInitInterface item_initialize = null;
    boolean readonly = false;

    //
    // Use these three to buffer object headers in updateEntry
    //
    byte[] headeroutbuf = null;
    ByteArrayOutputStream headeroutbytestream = null;
    DataOutputStream headerout = null;
    byte[] headerinbuf = null;
    ByteArrayInputStream headerinbytestream = null;
    DataInputStream headerin = null;

    ArrayList<Integer> rangeIndexList = new ArrayList<Integer>();

    public int rangeExpiredIndex = 0;

    //
    // Statics
    //
    static final int DWORDSIZE = 8;
    static final int SWORDSIZE = 4;

    //
    // Magic number: first four digits specify type of structure
    //               7175 = htod
    //               second four digits specify structure version.
    // The magic string MUST be the first four bytes in the header on
    // disk of any file containing persistent structures.
    //
    static final int magic = 71750002;

    static final long HTENTRY_MAGIC = 0x1268000000000000L;
    static final long HTENTRY_VERSION = 0x0000010000000000L;

    static final long HTENTRY_MAGIC_MASK = 0xFFFF000000000000L;
    static final long HTENTRY_VERSION_MASK = 0x0000FF0000000000L;
    static final long HTENTRY_FLAGS_MASK = 0x000000FF00000000L;
    static final long HTENTRY_DATA_SIZE_MASK = 0x00000000FFFFFFFFL;

    static final long HTENTRY_HASHCODE_MASK = 0x00000000FFFFFFFFL; // LI4337-17

    static final long HTENTRY_ALIAS_ID = 0x0000000100000000L;

    public static final int HTENTRY_OVERHEAD_SIZE = DWORDSIZE + // room for "next"
                                                    SWORDSIZE + // room for fastpath hash key
                                                    DWORDSIZE + // room for last-updated
                                                    DWORDSIZE + // room for last-referenced
                                                    DWORDSIZE + // room for first_created
                                                    DWORDSIZE + // room for expiration
                                                    DWORDSIZE + // room for flag + object size
                                                    SWORDSIZE + // room for size of the key
                                                    SWORDSIZE + // room for bytes flag
                                                    SWORDSIZE + // room for size of the value
                                                    SWORDSIZE; // room for size of the cacheValue

    public static final int HT_INITIAL_OVERHEAD_SIZE = 16900;

    //
    // Instrumentation
    //
    int collisions = 0;
    int read_requests = 0;
    int read_hits = 0;

    int write_replacements = 0;
    int write_requests = 0;
    long bytes_deserialized = 0;
    long bytes_serialized = 0;

    int removes = 0;
    int clears = 0;

    boolean auto_rehash = false; // if true, automatically rehash
                                 // when number of entries >
                                 // load * tablesize, otherwise
                                 // don't

    ProfTimer serializeTimer = null;
    ProfTimer deserializeTimer = null;
    long serialize_time = 0;
    long deserialize_time = 0;

    public int tempTableSize = 0;
    public boolean bHasCacheValue = false;

    /****************************************************************************
     * Constructor. This initializes a new file with default geometry, or opens an
     * existing file in read-write mode.
     * 
     * @param fn The FileManager instance over which this HTOD is implemented.
     * @param auto_rehash If "true", the HTOD will automatically double in
     *            capacity when its occupancy exceeds its threshold. If "false"
     *            the HTOD will increase only if the startRehash() method is
     *            invoked.
     * @param instanceid The HTOD instance id to use for this HTOD. See
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException It was necessary to count the objects in
     *                the hashtable on init, and at least one of those objects cannot be
     *                deserialized.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     ****************************************************************************/
    protected HashtableOnDisk(FileManager fm, boolean auto_rehash, long instanceid, boolean hasCacheValue, HTODDynacache htoddc)
        throws FileManagerException,
        ClassNotFoundException,
        IOException,
        HashtableOnDiskException {
        init(fm, auto_rehash, instanceid, null, hasCacheValue, htoddc);
    }

    /****************************************************************************
     * 
     * Constructor. This initializes a new file with default geometry, or opens an
     * existing file in read-write mode.
     * 
     * @param fn The FileManager instance over which this HTOD is implemented.
     * @param auto_rehash If "true", the HTOD will automatically double in
     *            capacity when its occupancy exceeds its threshold. If "false"
     *            the HTOD will increase only if the startRehash() method is
     *            invoked.
     * @param instanceid The HTOD instance id to use for this HTOD
     * @param initfn This class's "initialze" function is passed each key and
     *            each object during initialization of the hashtable, ONLY
     *            if the hashtable determines that it was not closed properly.
     *            If the hashtable was closed properly, this interface
     *            is never invoked.
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException It was necessary to count the objects in
     *                the hashtable on init, and at least one of those objects cannot be
     *                deserialized.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     ****************************************************************************/
    protected HashtableOnDisk(FileManager fm,
                              boolean auto_rehash,
                              long instanceid,
                              HashtableInitInterface initfn,
                              boolean hasCacheValue,
                              HTODDynacache htoddc)
        throws FileManagerException,
        ClassNotFoundException,
        IOException,
        HashtableOnDiskException {
        init(fm, auto_rehash, instanceid, initfn, hasCacheValue, htoddc);
    }

    /*************************************************************************
     * Init()
     * 
     * Init local variables.
     * Initialize the header fo the hashtable and verify that the instance
     * points to a valid HTOD on the disk. If a rehash was in progress and we
     * crashed, finish the rehash. If "item_initialize" is specified, iterate
     * the hashtable passing each object to the initialize interface so other
     * apps can set up if needed. Finally flush any potential writes to disk and
     * we're off and running.
     * 
     * @param fn The FileManager instance over which this HTOD is implemented.
     * @param auto_rehash If "true", the HTOD will automatically double in
     *            capacity when its occupancy exceeds its threshold. If "false"
     *            the HTOD will increase only if the startRehash() method is
     *            invoked.
     * @param instanceid The HTOD instance id to use for this HTOD
     * @param initfn This class's "initialze" function is passed each key and
     *            each object during initialization of the hashtable, ONLY
     *            if the hashtable determines that it was not closed properly.
     *            If the hashtable was closed properly, this interface
     *            is never invoked.
     * 
     *************************************************************************/
    protected void init(FileManager filemgr,
                        boolean auto_rehash,
                        long instanceid, // persistent object instance
                        HashtableInitInterface item_initialize, // force init of all items
                        boolean hasCacheValue,
                        HTODDynacache htoddc)
                    throws FileManagerException,
                    ClassNotFoundException,
                    IOException,
                    HashtableOnDiskException {
        this.filemgr = filemgr;
        this.filename = filemgr.filename();
        this.item_initialize = item_initialize;
        readonly = filemgr.isReadOnly();
        this.auto_rehash = auto_rehash;
        this.bHasCacheValue = hasCacheValue;
        this.htoddc = htoddc;

        // 
        // Open the file manager.  If this throws a FileManagerException or
        // ClassNotFoundException the file is probably not a valid file.
        // If we get an I/O exception it could be anything.
        //

        header = new HashHeader(filemgr, instanceid);
        if (header.magic != magic) {
            throw new HashtableOnDiskException("Invalid magic string. Expected " +
                                               magic + " received " + header.magic);
        }

        currentTable = header.currentTable();

        threshold = (header.loadFactor * header.tablesize()) / 100; // when num_objects 
        // exceeds this we rehash
        cacheHTIndex();
        serializeTimer = new ProfTimer();
        deserializeTimer = new ProfTimer();

        iterationLock = new Semaphore();

        if (header.rehashInProgress != 0) { // recover hashtable if needed
            recover();
            cacheHTIndex(); // reread, to insure it is correct
                            // after recovery.
            iterationLock.p(); // wait for recovery to complete
            iterationLock.v();
        }

        if (header.num_objects() == 0) { // get updated count
            countObjects();
        }

        filemgr.flush();
    }

    /*************************************************************************
     * cacheHTIndex
     *************************************************************************/
    void cacheHTIndex()
                    throws IOException {
        //htindex = new long[header.tablesize()];
        PrimitiveArrayPool.PoolEntry longPoolEntry = this.htoddc.longArrayPool.allocate(header.tablesize());
        htindex = (long[]) longPoolEntry.getArray();
        //byte[] tmp = new byte[header.tablesize() * DWORDSIZE];
        PrimitiveArrayPool.PoolEntry bytePoolEntry = this.htoddc.byteArrayPool.allocate(header.tablesize() * DWORDSIZE);
        byte[] tmp = (byte[]) bytePoolEntry.getArray();

        filemgr.seek(currentTable);
        filemgr.read(tmp);
        DataInputStream das = new DataInputStream(new ByteArrayInputStream(tmp));
        for (int i = 0; i < header.tablesize(); i++) {
            htindex[i] = das.readLong();
        }
        das.close();

        //tmp = null;
        this.htoddc.byteArrayPool.returnToPool(bytePoolEntry);
    }

    /*************************************************************************
     * getInstance. Initializes a HashtableOnDisk instance over the specified
     * FileManager, from the specified instanceid. The instanceid was
     * used to originally create the instance in the createInstance method.
     * 
     * @param filemgr The FileManager for the HTOD.
     * @param auto_rehash If "true", the HTOD will automatically double in
     *            capacity when its occupancy exceeds its threshold. If "false"
     *            the HTOD will increase only if the startRehash() method is
     *            invoked.
     * @param instanceid The instance of the HTOD in the FileManager.
     * 
     * @return A HashtableOnDisk pointer
     *************************************************************************/
    static public HashtableOnDisk getInstance(FileManager filemgr,
                                              boolean auto_rehash,
                                              long instanceid,
                                              boolean hasCacheValue,
                                              HTODDynacache htoddc)
                    throws FileManagerException,
                    ClassNotFoundException,
                    IOException,
                    HashtableOnDiskException {
        return getStaticInstance(filemgr, auto_rehash, instanceid, null, hasCacheValue, htoddc);
    }

    /*************************************************************************
     * getInstance. Initializes a HashtableOnDisk instance over the specified
     * FileManager, from the specified instanceid. The instanceid was
     * used to originally create the instance in the createInstance method.
     * 
     * @param filemgr The FileManager for the HTOD.
     * @param auto_rehash If "true", the HTOD will automatically double in
     *            capacity when its occupancy exceeds its threshold. If "false"
     *            the HTOD will increase only if the startRehash() method is
     *            invoked.
     * @param instanceid The instance of the HTOD in the FileManager.
     * @param initfn An interface to which each object will be passed on
     *            initialztion *only* if it is determined that the HTOD was not
     *            previously properly closed.
     * 
     * @return A HashtableOnDisk pointer
     *************************************************************************/
    static public HashtableOnDisk getStaticInstance(FileManager filemgr,
                                                    boolean auto_rehash,
                                                    long instanceid,
                                                    HashtableInitInterface initfn,
                                                    boolean hasCacheValue,
                                                    HTODDynacache htoddc)
                    throws FileManagerException,
                    ClassNotFoundException,
                    IOException,
                    HashtableOnDiskException {
        if (instanceid == 0) {
            instanceid = filemgr.start();
        }

        HashtableOnDisk answer = null;
        try {
            answer = new HashtableOnDisk(filemgr, auto_rehash, instanceid, initfn, hasCacheValue, htoddc);
        } catch (EOFException e) {
            // eof means the file is empty and there is no such instance
        }

        return answer;
    }

    /*************************************************************************
     * createInstance Creates a new instance of a HTOD and stores a pointer
     * to its header on disk so it can be retrived later, when
     * reinitializing the HTOD.
     * 
     * @param filemgr The FileManager instance over which the HTOD is
     *            implemented.
     * @param instanceid The unique id for this new instance. If a persistent
     *            structure already exists with this instance the
     *            createInstance method will return false.
     * @param tablesize The initial number of entries in the hashtable.
     * @param load The hashtable load factor. This is what determines when
     *            the hashtable automatically increases its size. Let
     *            num_objects the number of objects in the hashtable, and
     *            ht_size be the number of buckets. When
     *            num_objects > (tablesize * load) the hashtable will automatically
     *            double the number of buckets and rehash each existing object.
     *            Note that is is used only if auto_rehash is "true" when
     *            instantiating the HTOD object via getInstance.
     * @return true if the instance was created
     * @return false if an instance with the specified id already exists.
     *************************************************************************/
    static public long createInstance(FileManager fm,
                                      int tablesize,
                                      int load)
                    throws IOException,
                    HashtableOnDiskException {
        HashHeader header = new HashHeader(fm, magic, load, tablesize);
        return header.disklocation;
    }

    static public void destroyInstance(FileManager fm, long instanceid)
                    throws IOException,
                    HashtableOnDiskException {
        if (instanceid == 0) {
            throw new HashtableOnDiskException("Attempt to destroy instance 0");
        }
        fm.deallocate(instanceid);
    }

    public FileManager getFileManager() {
        return filemgr;
    }

    /*************************************************************************
     * exists Determines if a FileManager managed file with the
     * specified physical disk manager exists.
     * 
     * NOTE: This is currently a hack and MUST be reworked. It is expected
     * that the interface will not change, however.
     * 
     * @param filename The name of the file to check
     * @param type The type of the physical disk manager. Currently
     *            FileManager.ORIGINAL or FileManager.MULTIVOLUME_PAGED
     * 
     * @return true If such a file exists.
     * @return false If no such file exists.
     *************************************************************************/
    static public boolean exists(String filename, int type) {
        File checkfile;
        if (type == FileManager.MULTIVOLUME_PAGED) {
            checkfile = new File(filename + "." + 0);
        } else {
            checkfile = new File(filename);
        }
        return checkfile.exists();
    }

    /*****************************************************************************
     * close Close the HTOD, write the header to disk, and blow out internal structures.
     * Note that if multiple HTODs are implemented over the same file manager they
     * will have to be individually closed. Note also that this method does NOT
     * automatically close the FileManager.
     * 
     * @exception IOException thrown if there are physical problems with the underlying file
     *                and the header cannot be written
     ****************************************************************************/
    public void close()
                    throws IOException {
        //
        // The disk is kept permanently consistent, and the application now
        // controls the filemgr, so we have nothing to do any more.
        //        
        iterationLock.p();
        try {
            synchronized (this) {
                header.write();
                filemgr = null;
            }
        } finally {
            releaseMemoryToPool();
            iterationLock.v();
        }
    }

    /**************************************************************************
     * size Number of key-value mappings in this map.
     *************************************************************************/
    public synchronized int size() {
        return header.num_objects();
    }

    /**************************************************************************
     * tablesize Physical size of the hashtable.
     *************************************************************************/
    public int tablesize() {
        return header.tablesize();
    }

    /**************************************************************************
     * getNextRangeIndex The range index for walkHash().
     *************************************************************************/
    public int getNextRangeIndex() {
        int length = rangeIndexList.size();
        if (length > 0) {
            Integer rindex = rangeIndexList.get(length - 1);
            return rindex.intValue();
        }
        return 0;
    }

    /**************************************************************************
     * getPreviousRangeIndex The range index for walkHash().
     *************************************************************************/
    public int getPreviousRangeIndex() {
        int length = rangeIndexList.size();
        if (length == 2) {
            rangeIndexList.remove(1);
            return 0;
        } else if (length == 1) {
            return 0;
        }
        rangeIndexList.remove(length - 1);
        rangeIndexList.remove(length - 2);
        Integer rindex = rangeIndexList.get(length - 3);
        return rindex.intValue();
    }

    /**************************************************************************
     * addRangeIndex The range index for walkHash().
     *************************************************************************/
    public void addRangeIndex(int index) {
        rangeIndexList.add(new Integer(index));
    }

    /**************************************************************************
     * initRangeIndex The range index for walkHash().
     *************************************************************************/
    public void initRangeIndex() {
        rangeIndexList.clear();
        rangeIndexList.add(new Integer(0));
    }

    /**************************************************************************
     * getFilename Name of my file.
     *************************************************************************/
    public String getFilename() {
        return filename;
    }

    /*****************************************************************************
     * Load factor. When the ratio of mappings to available hash buckets exceeds
     * this number the hashtable automatically doubles in size and all objects are
     * rehashed. The load factor must be an integer 1<= loadfactor <=100. Default
     * is 75.
     ****************************************************************************/
    public int load() {
        return header.loadFactor;
    }

    /*****************************************************************************
     * dump_filemgr_header Dumps the file manager stats header to the specified writer.
     * 
     * @param out A writer to which the statistics will be written
     ****************************************************************************/
    public void dump_filemgr_header(Writer out)
                    throws IOException {
        filemgr.dump_stats_header(out);
    }

    /*****************************************************************************
     * dump_filemgr_stats Dumps the file manager stats to the specified writer.
     * 
     * @param out A writer to which the statistics will be written
     * @param labels If true, the stats are printed in ascii, labeled for easy visual comprehension
     *            If false, the stats are dumped as a single, unlabeled ascii line,
     *            intended to be imported into spreadsheets for analysis.
     ****************************************************************************/
    public void dump_filemgr_stats(Writer out, boolean labels)
                    throws IOException {
        filemgr.dump_stats(out, labels);
    }

    /*****************************************************************************
     * dump_filemgr_memory Dumps the FileManager's memory structures in ascii to the
     * specified writer. Intended for debugging.
     * 
     * @param out A writer to which the structures will be written
     ****************************************************************************/
    public void dump_filemgr_memory(Writer out)
                    throws IOException {
        filemgr.dump_memory(out);
    }

    /*****************************************************************************
     * dump_filemgr_disk Dumps the FileManager's disk structures in ascii to the
     * specified writer. Intended for debugging.
     * 
     * @param out A writer to which the structures will be written
     ****************************************************************************/
    public void dump_filemgr_disk(Writer out)
                    throws IOException {
        filemgr.dump_disk_memory(out);
    }

    /**************************************************************************
     * isByteArray Find out if object is a byte array so we can avoid serialization
     * and subsequent deserialization.
     * 
     * @param x the Object to inspect
     * @return true if x is a byte array (byte[])
     * @return false if x is not a byte array
     *************************************************************************/
    static public boolean isByteArray(Object x) {
        if (x == null)
            return false;
        Class c = x.getClass();
        if (c.isArray()) {
            Class ct = c.getComponentType();
            if (ct == Byte.TYPE) {
                return true;
            }
        }
        return false;
    }

    /*************************************************************************
     * containsKey Returns true if this map contains a mapping for the specified key.
     * 
     * @param key The key for the mapping to look up.
     * 
     * @return true if the object exists, false otherwise.
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException Some object cannot be deserialized during
     *                the search.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     *************************************************************************/
    public synchronized boolean containsKey(Object key)
                    throws FileManagerException,
                    ClassNotFoundException,
                    IOException,
                    HashtableOnDiskException {
        if (filemgr == null) {
            throw new HashtableOnDiskException("No Filemanager");
        }

        HashtableEntry e = findEntry(key, RETRIEVE_KEY, !CHECK_EXPIRED);
        boolean found = (e != null);
        htoddc.returnToHashtableEntryPool(e);
        return found;
    }

    /**************************************************************************
     * Returns the value to which this map maps the specified key, or null.
     * 
     * @param key The key for the object to fetch.
     * 
     * @return The specified object, or null if it cannot be found.
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException The object or some object in the hash bucket
     *                being searched cannotbe deserialized.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception EOFxception We were asked to seek beyond the end of the file.
     *                The file is likely corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     *************************************************************************/
    public synchronized Object get(Object key)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (filemgr == null) {
            throw new HashtableOnDiskException("No Filemanager");
        }

        Object answer = null;

        if (answer == null) {
            HashtableEntry e = findEntry(key, RETRIEVE_ALL, !CHECK_EXPIRED);
            if (e != null) {
                answer = e.value;
                htoddc.returnToHashtableEntryPool(e);
            }
        }
        read_requests++;
        if (answer != null) {
            read_hits++;
        }

        return answer;
    }

    public synchronized HashtableEntry getHashTableEntry(Object key, boolean checkExpired)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (filemgr == null) {
            throw new HashtableOnDiskException("No Filemanager");
        }

        HashtableEntry e = findEntry(key, RETRIEVE_ALL, checkExpired);
        read_requests++;
        if (e != null) {
            read_hits++;
        }

        return e;
    }

    //This method is used by garbage collector to get the cache key for the corresponding EvictionTableEntry
    public synchronized Object getCacheKey(EvictionTableEntry evt) //3821 NK begin
    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        Object key = null;
        if (filemgr == null) {
            throw new HashtableOnDiskException("No Filemanager");
        }

        HashtableEntry e = findEntry(evt, RETRIEVE_KEY);
        //TODO: Do we need to increment read_hits and read_requests
        if (e != null)
            key = e.getKey();
        return key;
    } //3821 NK end

    /**************************************************************************
     * Returns the value to which this map maps the specified key, or null.
     * This is an experimental interface and has not been as thorougly tested
     * as the get() method. If an object is placed in the hashtable using one of
     * the put() methods that specifies an expirationTime, get_or_expire() will check
     * the expiration in the entries metadata and if the object is expired, will
     * internally remove it from disk and return null, saving the cost of
     * fetching and deserializing an expired object.
     * 
     * @param key The key for the object to fetch.
     * 
     * @return The specified object, or null if it cannot be found, or null
     *         if it is expired.
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException The object or some object in the hash bucket
     *                being searched cannotbe deserialized.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception EOFxception We were asked to seek beyond the end of the file.
     *                The file is likely corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     *************************************************************************/
    /*
     * public synchronized Object get_or_expire(Object key)
     * throws IOException,
     * EOFException,
     * FileManagerException,
     * ClassNotFoundException,
     * HashtableOnDiskException
     * {
     * if ( filemgr == null ) {
     * throw new HashtableOnDiskException("No Filemanager");
     * }
     * 
     * Object answer = null;
     * 
     * if ( answer == null ) {
     * HashtableEntry e = findEntry(key, true, true);
     * if ( e != null ) {
     * if ( e.expired() < 0 ) {
     * remove(key);
     * println("Expiring[" + (-e.expired()/1000) + "] " + key);
     * } else {
     * answer = e.value;
     * }
     * }
     * }
     * read_requests++;
     * if( answer != null ) {
     * read_hits++;
     * }
     * 
     * return answer;
     * }
     */
    /**************************************************************************
     * Associates the object with the key and stores it. If an object already
     * exists for this key it is replaced and the previous object discard. The
     * object is assumed to have been "manually" serialized by the caller and will
     * be written up to length "len", directly on disk with no further
     * serialization.
     * 
     * @param key The key for the object to store.
     * @param value The object to store.
     * @param len The maximum number of bytes from "value" to write to disk.
     * @param expirationTime The expiration time of this object
     * 
     * @return true if the object is placed into the hashtable
     *         false oherwise. False only occurs if the key is null.
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException Some key in the hash bucket cannot be
     *                deserialized while searching to see if the object already exists. The
     *                underlying file is likely corrupted.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception EOFxception We were asked to seek beyond the end of the file.
     *                The file is likely corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     *************************************************************************/
    public synchronized boolean put(Object key, byte[] value, int len, long expirationTime)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (key == null)
            return false;
        //if ( value == null ) return false;  // comment this line to allow the value=NULL

        mapPut(key, value, len, expirationTime, -1, null, null, 0, !ALIAS_ID);

        return true;
    }

    /**************************************************************************
     * Associates the object with the key and stores it. If an object already
     * exists for this key it is replaced and the previous object discard. The
     * object is assumed to have been "manually" serialized by the caller and will
     * be written up to length "len", directly on disk with no further
     * serialization.
     * 
     * @param key The key for the object to store.
     * @param value The object to store (might be serialized cache entry)
     * @param len The maximum number of bytes from "value" to write to disk.
     * @param expirationTime The expiration time of this object
     * @param serializedKey the serialized key
     * @param serializedCacheValue the serialized cache value (value in cache entry)
     * @param isAliasId boolean to indicate alias id
     * 
     * @return HashTableEntry of old entry if exists. Return null if the key is null or the entry does not exist
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException Some key in the hash bucket cannot be
     *                deserialized while searching to see if the object already exists. The
     *                underlying file is likely corrupted.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception EOFxception We were asked to seek beyond the end of the file.
     *                The file is likely corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     *************************************************************************/
    public synchronized HashtableEntry put(Object key, Object value, int len, long expirationTime, long validatorExpirationTime,
                                           byte[] serializedKey, byte[] serializedCacheValue, int valueHashcode, boolean isAliasId) // LI4337-17
    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (key == null)
            return null;
        //if ( value == null ) return 0;  // comment this line to allow the value=NULL

        return mapPut(key, value, len, expirationTime, validatorExpirationTime, serializedKey, serializedCacheValue, valueHashcode, isAliasId); // LI4337-17
    }

    /**************************************************************************
     * Associates the object with the key and stores it. If an object already
     * exists for this key it is replaced and the previous object discard.
     * 
     * @param key The key for the object to store.
     * @param value The object to store.
     * @param expirationTime The expiration time of this object
     * 
     * @return true if the object is placed into the hashtable
     *         false oherwise. False only occurs if the key is null.
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException Some key in the hash bucket cannot be
     *                deserialized while searching to see if the object already exists. The
     *                underlying file is likely corrupted.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception EOFxception We were asked to seek beyond the end of the file.
     *                The file is likely corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     *************************************************************************/
    public synchronized boolean put(Object key, Object value, long expirationTime)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (key == null)
            return false;
        //if ( value == null ) return false;  // comment this line to allow the value=NULL

        mapPut(key, value, -1, expirationTime, -1, null, null, 0, !ALIAS_ID);

        return true;
    }

    /**************************************************************************
     * Associates the object with the key and stores it. If an object already
     * exists for this key it is replaced and the previous object discard. The
     * object is assumed to have been "manually" serialized by the caller and will
     * be written up to a maximum of "len" bytes directly on disk with no further
     * serialization.
     * 
     * @param key The key for the object to store.
     * @param value The object to store.
     * @param len The maximum number of bytes from "value" to write to disk
     * 
     * @return true if the object is placed into the hashtable
     *         false oherwise. False only occurs if the key is null.
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException Some key in the hash bucket cannot be
     *                deserialized while searching to see if the object already exists. The
     *                underlying file is likely corrupted.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception EOFxception We were asked to seek beyond the end of the file.
     *                The file is likely corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     *************************************************************************/
    public synchronized boolean put(Object key, byte[] value, int len)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (key == null)
            return false;
        //if ( value == null ) return false;  // comment this line to allow the value=NULL
        if (value == null) {
            len = -1;
        }

        mapPut(key, value, len, -1, -1, null, null, 0, !ALIAS_ID);

        return true;
    }

    /**************************************************************************
     * Associates the object with the key and stores it. If an object already
     * exists for this key it is replaced and the previous object discard.
     * 
     * @param key The key for the object to store.
     * @param value The object to store.
     * 
     * @return true if the object is placed into the hashtable
     *         false oherwise. False only occurs if the key is null.
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException Some key in the hash bucket cannot be
     *                deserialized while searching to see if the object already exists. The
     *                underlying file is likely corrupted.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception EOFxception We were asked to seek beyond the end of the file.
     *                The file is likely corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     *************************************************************************/
    public synchronized boolean put(Object key, Object value)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (key == null)
            return false;
        //if ( value == null ) return false;   // comment this line to allow the value=NULL

        mapPut(key, value, -1, -1, -1, null, null, 0, !ALIAS_ID);

        return true;
    }

    /**************************************************************************
     * mapPut(key, value) Common internal method to write to the hashtable.
     * It sort of tries to mimic Java Map semantics, but because of the difficulty
     * of handling exceptions for persistent objects that implement the Map
     * interface we do not currently publicly advertise this interface.
     * 
     * @param key The key for the object to store.
     * @param value The object to store.
     * @param length The maximum number of bytes from "value" to write to disk,
     *            if value is a byte array. Ignored otherwise.
     * @param expirationTime The time of day after which this object is "stale"
     * @param serializedKey the serialized key
     * @param serializedCacheValue the serialized cache value
     * @param isAliasId boolean to indicate alias id
     * 
     * @return the HashtableEntry of the old entry. Return null if the key is null or the entry does not exist
     **************************************************************************/
    protected HashtableEntry mapPut(Object key,
                                    Object value,
                                    int length,
                                    long expirationTime,
                                    long validatorExpirationTime,
                                    byte[] serializedKey,
                                    byte[] serializedCacheValue,
                                    int valueHashcode,
                                    boolean isAliasId) // LI4337-17
    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (filemgr == null) {
            throw new HashtableOnDiskException("No Filemanager");
        }

        write_requests++;

        if (key == null)
            return null;

        //
        // Lets rehash if needed.  If the put is a simple replace this rehash
        // is technicaly not required, but detecting that case and handling
        // it specially not only complicates the code but requires an extra
        // fetch or two from disk which seems silly.  So we just rehash
        // immediately (we probably would be rehashing soon anyway, if we
        // are that close to the threshold).
        //
        if (auto_rehash) {
            if (header.num_objects() + 1 > threshold) {
                if (header.rehashInProgress == 0) {
                    rehash(); // time to grow the table
                }
            }
        }

        HashtableEntry e = findEntry(key, RETRIEVE_KEY, !CHECK_EXPIRED);

        if (e == null) { // cannot find the item
            int tableid = header.getTableidForNewEntry();
            HashtableEntry newEntry = htoddc.getFromHashtableEntryPool();
            newEntry.copy(key, value, header.tablesize(tableid), tableid, length, 0, expirationTime, validatorExpirationTime,
                          serializedKey, serializedCacheValue, valueHashcode, isAliasId); // LI4337-17

            writeEntry(newEntry); // write entry, update old entry
                                  // to chain in e, update hashtable
            header.incrementObjectCount(); // if necessary
            filemgr.flush();
            htoddc.returnToHashtableEntryPool(newEntry);
        } else {
            if (value == null) { // value is null when putting cache id for the dependency. return because the cache id already exists
                return null;
            }
            // Create HashtableEntry of old entry with key, size and expiration. These attributes will be used later in HTODDynacache 
            // to remove an old entry from eviction table.
            HashtableEntry oldEntry = htoddc.getFromHashtableEntryPool();
            oldEntry.key = e.key;
            oldEntry.size = e.size;
            oldEntry.expiration = e.expiration;

            //Object old_value = e.getValue();            // save for Map interface return
            long old_location = e.location; // save locaction of old space to free
            e.value = value; // update new data
            e.valuelen = length; // optional maxlen for byte[]
            e.expiration = expirationTime; // update expiration time 
            e.serializedKey = serializedKey; // update serialized key
            e.serializedCacheValue = serializedCacheValue; // update serialized cache value
            e.bAliasId = isAliasId; // update alias id indicator
            e.validatorExpiration = validatorExpirationTime; // update validator expiration time
            e.cacheValueHashcode = valueHashcode; // update cache value hashcode 
            if (getHtindex(e.index, e.tableid) == e.location) { // make sure the cached index
                updateHtindex(e.index, e.next, e.tableid); //    is "deallocated"
            }
            e.location = 0; // force reallocation
            writeEntry(e); // send to disk
            filemgr.deallocate(old_location); // free old space
            filemgr.flush();
            write_replacements++;
            htoddc.returnToHashtableEntryPool(e);
            //return old_value;                  //all done
            return oldEntry; // all done
        }

        return null;
    }

    /**
     * Removes the mapping for this key and deletes the object from disk.
     * 
     * @param key The key of the object being removed.
     * 
     * @return true if successful, false otherwise
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     * @exception ClassNotFoundException Some key in the hash bucket cannot be
     *                deserialized while searching to see if the object already exists. The
     *                underlying file is likely corrupted.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception EOFxception We were asked to seek beyond the end of the file.
     *                The file is likely corrupt.
     * @exception HashtableOnDiskException The hashtable header is readable but invalid.
     *                One or more of the following is true: the magic string is invalid, the header
     *                pointers are null, the header pointers do not point to a recognizable hashtable.
     * 
     */
    public synchronized boolean remove(Object key)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (filemgr == null) {
            throw new HashtableOnDiskException("No Filemanager");
        }

        if (key == null)
            return false; // no null keys allowed

        HashtableEntry e = findEntry(key, RETRIEVE_KEY, !CHECK_EXPIRED);
        if (e == null)
            return false; // not found

        boolean answer = remove(e);
        htoddc.returnToHashtableEntryPool(e);
        return answer;
    }

    public synchronized HashtableEntry getAndRemove(Object key, boolean bRetrieveCacheValue)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (filemgr == null) {
            throw new HashtableOnDiskException("No Filemanager");
        }

        if (key == null)
            return null; // no null keys allowed

        HashtableEntry hte = null;
        if (bRetrieveCacheValue) {
            if (key instanceof EvictionTableEntry) //3821 NK 
                hte = findEntry((EvictionTableEntry) key, RETRIEVE_ALL);
            else
                hte = findEntry(key, RETRIEVE_ALL, !CHECK_EXPIRED);
        } else {
            if (key instanceof EvictionTableEntry) {
                //System.out.println("About to invoke findEntry");
                hte = findEntry((EvictionTableEntry) key, RETRIEVE_KEY_VALUE);
                //System.out.println("Done findEntry, hte:"+ hte);
            } else
                hte = findEntry(key, RETRIEVE_KEY_VALUE, !CHECK_EXPIRED); //3821 NK
        }

        if (hte != null) {
            remove(hte);
        }
        return hte;
    }

    // This method is used to update expiration times in disk entry header
    public synchronized boolean updateExpirationInHeader(Object key, long expirationTime, long validatorExpirationTime)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (filemgr == null) {
            throw new HashtableOnDiskException("No Filemanager");
        }

        if (key == null)
            return false; // no null keys allowed

        HashtableEntry entry = findEntry(key, RETRIEVE_KEY, !CHECK_EXPIRED);
        if (entry == null)
            return false; // not found

        //
        // Seek to point to validator expiration time field in the header
        filemgr.seek(entry.location +
                     DWORDSIZE + // room for next
                     SWORDSIZE); // room for hash
        filemgr.writeLong(validatorExpirationTime); // update VET
        /*
         * comment out the code below because the expiration time does not change
         * filemgr.writeInt(0);
         * filemgr.writeInt(entry.cacheValueHashcode); // update cache value hashcode
         * filemgr.writeLong(entry.first_created); // update first created (not neccessary but move to pointer
         * filemgr.writeLong(expirationTime); // update RET
         */
        htoddc.returnToHashtableEntryPool(entry);
        return true;
    }

    /*************************************************************************
     * remove(key) with Map semantics, for HTODMap. See comment to mapPut. We
     * do not publicly advertise this interface.
     **************************************************************************/
    protected Object mapRemove(Object key)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (key == null)
            return null; // no null keys allowed

        HashtableEntry e = findEntry(key, RETRIEVE_KEY, !CHECK_EXPIRED);
        if (e == null)
            return null; // not found

        Object answer = e.getValue();
        if (!remove(e)) {
            answer = null;
        }
        htoddc.returnToHashtableEntryPool(e);
        return answer;
    }

    /**************************************************************************
     * Removes all objects from the map. Its probably faster to just close the
     * file, delete it, and reinitialize. But this is useful if there are multiple HTOD
     * instances in the file and we don't want to destroy some of them.
     * 
     * @exception FileManagerException The underlying file manager has a problem.
     *                deserialized while searching to see if the object already exists. The
     *                underlying file is likely corrupted.
     * @exception IOException The underlying file has a problem and is likely
     *                corrupt.
     * @exception EOFxception We were asked to seek beyond the end of the file.
     *                The file is likely corrupt.
     *************************************************************************/
    public void clear()
                    throws IOException,
                    EOFException,
                    FileManagerException {
        iterationLock.p();

        try {
            synchronized (this) {
                if (filemgr == null) {
                    throw new HashtableOnDiskException("No Filemanager");
                }

                int tableid = header.currentTableId();
                clears++;

                for (int i = 0; i < header.tablesize(); i++) {
                    filemgr.seek(header.calcOffset(i, tableid));
                    long location = filemgr.readLong();

                    while (location != 0) { // clear out buckets
                        filemgr.seek(location); // look for "next" pointer
                        long next = filemgr.readLong();
                        header.decrementObjectCount();
                        filemgr.deallocate(location);
                        location = next;
                    }

                    writeHashIndex(i, 0, tableid); // broken, jrc
                }
            }
        } finally {
            iterationLock.v();
        }
    }

    /**************************************************************************
     * This invokes the action's "execute" method once for every
     * object, passing both the key and the object to the interface.
     * 
     * The iteration is synchronized with concurrent get and put operations
     * to avoid locking the HTOD for long periods of time. Some objects which
     * are added during iteration may not be seen by the iteration.
     * 
     * @param action The object to be "invoked" for each object.
     * 
     *************************************************************************/
    public int iterateObjects(HashtableAction action, int index, int length)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        return walkHash(action, RETRIEVE_ALL, index, length);
    }

    /**************************************************************************
     * This invokes the action's "execute" method once for every
     * object passing only the key to the method, to avoid the
     * overhead of reading the object if it is not necessary.
     * 
     * The iteration is synchronized with concurrent get and put operations
     * to avoid locking the HTOD for long periods of time. Some objects which
     * are added during iteration may not be seen by the iteration.
     * 
     * @param action The object to be "invoked" for each object.
     * 
     *************************************************************************/
    public int iterateKeys(HashtableAction action, int index, int length)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        return walkHash(action, RETRIEVE_KEY, index, length);
    }

    /**************************************************************************
     * startRehash - API to force rehash. See rehash() method for details.
     * Note: this could take quite a while for a large hashtable. It is
     * synchronized with get, put, and remove operations, so the hashtable
     * is not locked out during resize.
     * 
     * @param newsize - the new size of the hashtable.
     *************************************************************************/
    public void startRehash(int newsize)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (header.rehashInProgress == 0) {
            doRehash(newsize); // time to grow the table
        }
    }

    // ---------------------------------------------------------------------------
    // non-public routines
    // ---------------------------------------------------------------------------

    // ---------------------------------------------------------------------------
    // Physical disk routines
    // ---------------------------------------------------------------------------

    //************************************************************************
    // Remove an entry from the table.
    //
    // In case of crash the structure will not be compromized but we
    // might permanently leak some space.  Scenarios:
    // 1)  no change - crashed before we could update disk
    // 2)  space leaked - crashed after updating chain but before we
    //     deallocated from disk.
    //************************************************************************
    private boolean remove(HashtableEntry entry)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (entry == null) {
            throw (new HashtableOnDiskException("remove: Internal error, null entry."));
        }

        removes++;
        //
        // First unchain from hashtable
        //
        if (entry.location == getHtindex(entry.index, entry.tableid)) { // first in chain
            writeHashIndex(entry.index, entry.next, entry.tableid);
        } else { // not first in chain
            updatePointer(entry.previous, entry.next);
        }

        //
        // Now delete data
        //
        header.decrementObjectCount();
        filemgr.deallocate(entry.location); // deallocate from disk
        filemgr.flush();

        return true;
    }

    /**************************************************************************
     * Given an index into the hash table and a value to store there, find it
     * on disk and write the value.
     *************************************************************************/
    private void writeHashIndex(int index, long value, int tableid)
                    throws IOException {

        long diskloc = header.calcOffset(index, tableid);
        filemgr.seek(diskloc);
        filemgr.writeLong(value);
        updateHtindex(index, value, tableid);
    }

    /**************************************************************************
     * Write the object pointer to the table. If we are in the process of
     * doubling, this method finds the correct table to update (since there are
     * two tables active during the doubling process).
     *************************************************************************/
    void updateHtindex(int index, long value, int tableid) {
        if (tableid == header.currentTableId()) {
            htindex[index] = value;
        } else {
            new_htindex[index] = value;
        }
    }

    /**************************************************************************
     * Retrieve the object pointer from the table. If we are in the process of
     * doubling, this method finds the correct table to update (since there are
     * two tables active during the doubling process).
     *************************************************************************/

    long getHtindex(int index, int tableid) {
        if (tableid == header.currentTableId()) {
            return htindex[index];
        } else {
            return new_htindex[index];
        }
    }

    /**************************************************************************
     * Seek to the "next" pointer for the HashtableEntry pointing to "me"
     * because I've been reallocated.
     *************************************************************************/
    private void updatePointer(long next_ptr, long next_value)
                    throws IOException,
                    EOFException {
        filemgr.seek(next_ptr);
        filemgr.writeLong(next_value);
    }

    /*************************************************************************
     * This preps the the reusable output buffer for writing object heders to
     * reduce the number of physical writes needed to write an object to disk.
     *************************************************************************/
    private void initWriteBuffer()
                    throws IOException {
        if (headeroutbuf == null) {
            int buflen =
                            DWORDSIZE + // room for next
                                            SWORDSIZE + // room for hash
                                            DWORDSIZE + // room for old format: last update
                                                        //          new format: validator expiration time (VET)
                                            DWORDSIZE + // room for old format: last reference
                                                        //          new format: H-WORD - unused; L-WORD - hashcode for cache value
                                            DWORDSIZE + // room for first creation
                                            DWORDSIZE + // room for expiration (RET)
                                            DWORDSIZE + // room for old format: grace 
                                                        //          new format: magic, version & data size
                                            SWORDSIZE; // room for key size

            headeroutbuf = new byte[buflen];
            headeroutbytestream = new ByteArrayPlusOutputStream(headeroutbuf);
            headerout = new DataOutputStream(headeroutbytestream);
        }
        headeroutbytestream.reset();
    }

    /*************************************************************************
     * This preps the reusable input buffer for reading object headers to reduce
     * the number of physical reads necessary to read an object from disk.
     *************************************************************************/
    private void initReadBuffer(long seek)
                    throws IOException {
        if (headerinbuf == null) {
            int buflen =
                            DWORDSIZE + // room for next
                                            SWORDSIZE + // room for hash
                                            DWORDSIZE + // room for old format: last update
                                                        //          new format: validator expiration time (VET)
                                            DWORDSIZE + // room for old format: last reference
                                                        //          new format: H-WORD - unused; L-WORD - hashcode for cache value
                                            DWORDSIZE + // room for first creation
                                            DWORDSIZE + // room for expiration
                                            DWORDSIZE + // room for old format:grace 
                                                        //          new format:magic, version & data size
                                            SWORDSIZE; // room for key size

            headerinbuf = new byte[buflen];
            headerinbytestream = new ByteArrayInputStream(headerinbuf);
            headerin = new DataInputStream(headerinbytestream);
        }
        filemgr.seek(seek);
        filemgr.read(headerinbuf);
        headerinbytestream.reset();
    }

    /**************************************************************************
     * Common code to physically write an entry to disk, new format march 29 2002
     *************************************************************************/
    private void updateEntry(HashtableEntry entry)
                    throws IOException, EOFException, FileManagerException, ClassNotFoundException {
        //
        // We'll write the header, key, and data into a byte arry first,
        // then allocate, seek, and write with a single write operation.
        // It is necessary to buffer the data first like this because we cannot
        // determine the size of storage to allocate without first examining the
        // size of the serialize object.
        //

        //
        // Serialize key and data.  If the data is already a byte[] array, we can
        // bypass serialization.
        //
        byte[] serializedKey = entry.serializedKey;
        int keySize = 0;
        byte[] serializedValue = null;
        int valueSize = -1;
        int bytes = 0;
        byte[] serializedCacheValue = entry.serializedCacheValue;
        int cacheValueSize = -1;
        int dataSize = HTENTRY_OVERHEAD_SIZE;

        if (serializedKey == null) {
            if (keyout == null) {
                keyout = new ByteArrayPlusOutputStream(500);
            } else {
                keyout.reset();
            }
            serializeTimer.reset();
            ObjectOutputStream out = new ObjectOutputStream(keyout);
            out.writeObject(entry.key);
            out.close();
            serializedKey = keyout.getTheBuffer();
            keySize = keyout.size();
            bytes_serialized += keySize;
        } else {
            keySize = entry.serializedKey.length;
        }
        dataSize += keySize;

        if (entry.value != null) {
            if (isByteArray(entry.value)) {
                bytes = 1;
                serializedValue = (byte[]) entry.value;
                if (entry.valuelen != -1) {
                    valueSize = entry.valuelen;
                } else {
                    valueSize = serializedValue.length;
                }
            } else {
                if (dataout == null) {
                    dataout = new ByteArrayPlusOutputStream(500);
                } else {
                    dataout.reset();
                }
                ObjectOutputStream out = new ObjectOutputStream(dataout);
                out.writeObject(entry.value);
                out.close();
                serializedValue = dataout.getTheBuffer();
                valueSize = dataout.size();
                bytes_serialized += valueSize;
                //
                // If an unusally large object is written it bloats the buffer, which can
                // hog resources.  If this happens dump the static buffer and start a new 
                // one on the next write.
                //
                if (valueSize > 100000) {
                    dataout = null;
                }
            }
        }

        serialize_time += serializeTimer.elapsed();

        int allocateSize = DWORDSIZE + // room for "next"
                           SWORDSIZE + // room for fastpath hash key
                           DWORDSIZE + // room for old format: last-modified
                                       //          new format: validator expiration time(VET)
                           DWORDSIZE + // room for old format: last-referenced;   // LI4337-17
                                       //          new format: H-Word = unused 
                                       //                      L-word = cache value hashcode 
                           DWORDSIZE + // room for first_created
                           DWORDSIZE + // room for expiration (RET)
                           DWORDSIZE + // room for flag + object size
                           SWORDSIZE + // room for size of the key
                           keySize + // room for serialized key
                           SWORDSIZE + // room for bytes flag
                           SWORDSIZE; // room for size of the value

        if (valueSize != -1) {
            allocateSize += valueSize; // room for serialized value
            dataSize += valueSize; // add value size to data size 
        }

        if (this.bHasCacheValue && !entry.bAliasId) {
            allocateSize += SWORDSIZE; // room for size of cacheValue
            if (serializedCacheValue != null) { // any cacheValue?
                cacheValueSize = serializedCacheValue.length; // size of the cacheValue
                allocateSize += cacheValueSize; // room for serialized cacheValue
                dataSize += cacheValueSize; // add cacheValue size to data size
            }
        }
        if (dataSize % 512 != 0) { // size adjustment for 512 blocks
            dataSize = (dataSize / 512 + 1) * 512;
        }
        //System.out.println("**** HashtableOnDisk: id=" + entry.key + " size=" + dataSize + " allocateSize=" + allocateSize);
        long tempData = 0;
        if (entry.bAliasId) {
            tempData = HTENTRY_MAGIC + HTENTRY_VERSION + HTENTRY_ALIAS_ID + dataSize;
        } else {
            tempData = HTENTRY_MAGIC + HTENTRY_VERSION + dataSize;
        }

        // end write key and value
        //
        // physical allocation
        //

        // For testing for cache id 
        //String testId = (String)entry.key;
        //if (testId.indexOf("928") > 0 || testId.indexOf("1028") > 0) {
        //    throw new IOException(com.ibm.ws.cache.HTODDynacache.DISK_CACHE_IN_GB_OVER_LIMIT_MSG);
        //}
        // For testing for depid/template
        //if (!this.bHasCacheValue) {
        //    String testId = (String)entry.key;
        //    if (testId.indexOf("926") > 0) {
        //        throw new IOException(com.ibm.ws.cache.HTODDynacache.DISK_CACHE_IN_GB_OVER_LIMIT_MSG);
        //    }
        //}
        entry.location = filemgr.allocate(allocateSize);

        initWriteBuffer();
        headerout.writeLong(entry.next);
        headerout.writeInt(entry.hash);
        //headerout.writeLong(System.currentTimeMillis());  // old format: last update
        headerout.writeLong(entry.validatorExpiration); // new format: VET
        //headerout.writeLong(System.currentTimeMillis());  // old format: last reference
        headerout.writeInt(0); // new format: unused    // LI4337-17
        headerout.writeInt(entry.cacheValueHashcode); //             cache value hashcode
        headerout.writeLong(entry.first_created); // when object is first created
        headerout.writeLong(entry.expiration); // expiration (RET) 
        headerout.writeLong(tempData); // write magic, version, flag & data size 
        headerout.writeInt(keySize);
        headerout.flush();

        //
        // Physical write the record like this:
        filemgr.seek(entry.location);
        filemgr.write(headeroutbuf);
        //
        // int keysize;            length of key
        // byte[] serialized key;  The key
        // int valueSize           length of value
        // int value_is_byte_array 0 if value is serialized obj 1 if a byte array
        // byte[] value            serialized value
        //
        filemgr.write(serializedKey, 0, keySize);
        filemgr.writeInt(bytes);
        filemgr.writeInt(valueSize);
        if (valueSize != -1) {
            filemgr.write(serializedValue, 0, valueSize);
        }

        //
        // if there is any cacheValue
        // int cacheValueSize      size of cacheValue - -1 if cacheValue is empty
        // byte[] cacheValue   serialized cacheValue
        //
        if (this.bHasCacheValue) {
            filemgr.writeInt(cacheValueSize);
            if (cacheValueSize != -1) {
                filemgr.write(serializedCacheValue, 0, cacheValueSize);
            }
        }

        //if (serializedCacheValue != null) {
        //   traceDebug("updateEntry()", "cacheName=" + this.htoddc.cacheName + " key=" + entry.key + " allocateSize=" + allocateSize + " dataSize=" + dataSize + " valueSize=" + valueSize + " cacheValueSize=" + cacheValueSize);
        //} else {
        //    traceDebug("updateEntry()", "cacheName=" + this.htoddc.cacheName + " key=" + entry.key + " allocateSize=" + allocateSize + " valueSize=" + valueSize);
        //}
    }

    /**************************************************************************
     * Common code to insert an entry into the hashtable.
     *************************************************************************/
    private void writeEntry(HashtableEntry entry)

    throws IOException, EOFException, FileManagerException, ClassNotFoundException {

        long index = getHtindex(entry.index, entry.tableid);
        if (index == 0) { // first one in this bucket
            updateEntry(entry); // write to disk
            writeHashIndex(entry.index, entry.location, entry.tableid); // update index
        } else if (index == entry.location) { // replacing first entry
            updateEntry(entry);
            writeHashIndex(entry.index, entry.location, entry.tableid); // update index
        } else { // 
            //
            // If the entry has a "previous" pointer, then it was read earlier
            // from disk.  Otherwise, it is a brand new entry.  If it is a brand
            // entry, we write it to disk and chain at the front of the bucket.
            // If "previous" is not zero, we check to see if reallocation is
            // needed (location == 0).  If not, we simply update on disk and we're
            // done.  If reallocation is needed we update on disk to do the allocation
            // and call updatePointer to chain into the bucket.
            //
            if (entry.previous == 0) {
                entry.next = index;
                updateEntry(entry);
                writeHashIndex(entry.index, entry.location, entry.tableid); // update index
            } else {
                if (entry.location == 0) { // allocation needed?
                    updateEntry(entry); // do allocation
                    updatePointer(entry.previous, entry.location); // chain in
                } else {
                    updateEntry(entry); // no allocation, just update fields
                }
            }
        }

    }

    /**************************************************************************
     * Common code to deallocate the space taken by header, key, and data.
     *************************************************************************/
    private void deallocate(HashtableEntry entry)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (entry.location == 0) {
            throw (new HashtableOnDiskException("deallocate:  space not allocated."));
        }

        filemgr.deallocate(entry.location);
        entry.location = 0;
    }

    /*************************************************************************
     * Read the data portion of the entry. This assumes that the caller as positioned
     * the seek point in the file.
     *************************************************************************/
    private Object readDataField(int bytes, int datalen)
                    throws IOException,
                    ClassNotFoundException {

        Object value = null;

        deserializeTimer.reset();
        if (bytes == 0) { // yes, need to deserialize
            if ((databuf == null) || (databuf.length < datalen)) {
                databuf = new byte[datalen];
            }

            deserialize_time += deserializeTimer.elapsed(); // careful not to add physical i/o into time
            filemgr.read(databuf, 0, datalen); // read data
            deserializeTimer.reset();

            value = SerializationUtility.deserialize(Arrays.copyOf(databuf, datalen), htoddc.cacheName);
            bytes_deserialized += datalen; // we don't count application deserialzation

        } else {
            value = new byte[datalen]; // space for data
            deserialize_time += deserializeTimer.elapsed(); // avoid counting physical i/o
            filemgr.read((byte[]) value); // read data
            deserializeTimer.reset();
        }
        deserialize_time += deserializeTimer.elapsed();

        //
        // If static deserializion buffer bloats things get expensive.  Force 
        // reallocation if a huge object is read in.
        //
        if (datalen > 100000) {
            databuf = null;
        }

        return value;
    }

    //************************************************************************
    // Restore an entry from disk and fill in as many fields as we can.
    // Understands new format march 29 2002
    // 
    // This method will optionally only deserialize the key.  This is useful for example,
    // when iterating the hashtable, if the only keys are needed, and when chasing
    // hashbucket chains.
    //************************************************************************
    protected HashtableEntry readEntry(long location,
                                       long previous,
                                       int retrieveMode,
                                       boolean checkExpired,
                                       int tableid)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {

        if (location == 0)
            return null;

        long next = 0;
        int hash = 0;

        initReadBuffer(location);
        next = headerin.readLong();
        hash = headerin.readInt();

        return readEntry2(location, next, hash, previous, retrieveMode, checkExpired, tableid, null, null);
    }

    //************************************************************************
    // Common code to read the object from disk.  The pointer to the next object in the
    // chain, and the key's hashcode have already been read in.  Those two fields
    // have been separated out to enable fast-lookup by checking the hashcode without
    // reading the entire object.
    //************************************************************************
    protected HashtableEntry readEntry2(long location,
                                        long next,
                                        int hash,
                                        long previous,
                                        int retrieveMode,
                                        boolean checkExpired,
                                        int tableid,
                                        Object matchKey,
                                        EvictionTableEntry matchEVT)
                    throws IOException,
                    ClassNotFoundException {
        long last_modified = 0;
        long last_referenced = 0;
        long first_created = 0;
        long expiration = -1;
        long validatorExpiration = -1;
        long tempData = -1;
        int dataSize = -1;

        int keySize = -1;
        Object key = null;

        int valueSize = -1;
        int bytes = 0;
        Object value = null;

        int cacheValueSize = 0;
        int cacheValueHashcode = 0; // LI4337-17
        byte[] serializedCacheValue = null;
        boolean isAliasId = false;
        boolean isValidHashcodeForValue = true;

        HashtableEntry htEntry = htoddc.getFromHashtableEntryPool();
        last_modified = headerin.readLong(); // old format: last modified
                                             // new format: VET 
        //
        // NOTE: last_referenced is no longer being maintained! 
        // we use this field for H-word: unused; L-word field for cache value hashcode
        //        
        last_referenced = headerin.readLong(); // old format: last referenced;
                                               // new format: unused (int)
                                               //             hashcode for cache value
        if (last_modified == last_referenced) { // LI4337-17
            isValidHashcodeForValue = false;
            cacheValueHashcode = 0;
            validatorExpiration = -1;
        } else {
            cacheValueHashcode = (int) (last_referenced & HTENTRY_HASHCODE_MASK);
            validatorExpiration = last_modified;
        }

        first_created = headerin.readLong(); // time obj first created
        expiration = headerin.readLong(); // expiration (RET)
        tempData = headerin.readLong(); // old format is grace (-1 means not used;  0 means alias)
                                        // new format: magic number, version, flag & data size
        if (tempData != -1) {
            if (tempData == 0) {
                isAliasId = true;
            } else {
                dataSize = (int) (tempData & HTENTRY_DATA_SIZE_MASK);
                isAliasId = (tempData & HTENTRY_ALIAS_ID) > 0 ? true : false;
                //long hteMagic = tempData & HTENTRY_MAGIC_MASK;
                //if (hteMagic != HTENTRY_MAGIC) {
                //    System.out.println("*** magic not compare received=" + hteMagic + " expected=" + HTENTRY_MAGIC);
                //}
                //long hteVersion = tempData & HTENTRY_VERSION_MASK;
                //if (hteVersion != HTENTRY_VERSION) {
                //    System.out.println("*** magic not compare received=" + hteVersion + " expected=" + HTENTRY_VERSION);
                //}
                //long hteFlags = tempData & HTENTRY_FLAGS_MASK;
                //if (hteFlags != 0) {
                //    System.out.println("*** magic not compare received=" + hteFlags + " expected=0");
                //}
            }
        }

        // if matchEVT is valid, compare the expiration time and size key. If not match, the code returns NULL.
        if (matchEVT != null) { //493877
            long expTime = expiration;
            if (expiration <= 0)
                expTime = Long.MAX_VALUE;
            if (expTime != matchEVT.expirationTime || dataSize != matchEVT.size) {
                return null;
            }
        }

        keySize = headerin.readInt(); // length of serialized key

        if ((keybuf == null) || (keybuf.length < keySize)) { // insure desrializion buffer is ok
            keybuf = new byte[keySize];
        }
        filemgr.read(keybuf, 0, keySize); // read key from disk

        deserializeTimer.reset();

        key = SerializationUtility.deserialize(Arrays.copyOf(keybuf, keySize), htoddc.cacheName); // deserialize the key
        deserialize_time += deserializeTimer.elapsed();
        bytes_deserialized += keySize;

        // if matchKey is valid, compare the key whether it matches or not. If not match, the code returns NULL.
        if (matchKey != null && key != null && !key.equals(matchKey)) { //493877
            return null;
        }

        if (checkExpired && expiration > 0) {
            if (System.currentTimeMillis() - expiration >= 0) {
                retrieveMode = RETRIEVE_KEY;
            }
        }

        if (retrieveMode == RETRIEVE_ALL || retrieveMode == RETRIEVE_KEY_VALUE) { // both key and data requested?
            bytes = filemgr.readInt(); // read byte flag
            valueSize = filemgr.readInt(); // read data length
            if (valueSize != -1) { // has data?
                value = readDataField(bytes, valueSize); // read data if byte flag 1 => don't deserialize on data; if byte flag = 0 => deserialize the data
            }
            if (this.bHasCacheValue && dataSize > 0) {
                cacheValueSize = dataSize - keySize - valueSize;
            }
        }

        if (this.bHasCacheValue && !isAliasId && dataSize > 0 && retrieveMode == RETRIEVE_ALL) {
            cacheValueSize = filemgr.readInt(); //read cacheValue length  
            if (cacheValueSize != -1) { // has cacheValue?
                serializedCacheValue = (byte[]) readDataField(1, cacheValueSize); // passing "1" ==> don't deserialize the cacheValue
            }
        }

        //if (this.bHasCacheValue) {
        //    traceDebug("readEntry2()", "cacheName=" + this.htoddc.cacheName + " key=" + key + " dataSize=" + dataSize + " valueSize=" + valueSize + " cacheValueSize=" + cacheValueSize + " expiration=" + expiration);
        //}

        //
        // Build the entry
        //
        htEntry.copy(location, first_created, key, value, next, previous,
                     header.tablesize(tableid), tableid, valueSize,
                     expiration, validatorExpiration, dataSize,
                     serializedCacheValue, cacheValueSize, cacheValueHashcode,
                     isAliasId, isValidHashcodeForValue); // LI4337-17
        return htEntry;
    }

    //************************************************************************
    // Given an object's key, read it in.
    //
    // @param key - the unserialized key
    // @param both - if true, read both key and data; otherwise read only key
    // @param checkExpired - apply checking expiration logic if true.
    //************************************************************************
    private HashtableEntry findEntry(Object key, int retrieveMode, boolean checkExpired)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (key == null) {
            return null;
        }

        int hashcode = key.hashCode();
        int index = header.getHtIndex(hashcode, header.currentTableId());
        long current = 0;
        HashtableEntry answer = null;

        if (header.rehashInProgress == 1) {

            if (debug)
                print("*");

            //
            // First search primary table.
            //
            if (htindex[index] != 0) {
                if (debug)
                    print("A");
                current = htindex[index];

                if (current == 0) {
                    throw new IllegalStateException("findEntry: ht pointer is null");
                }
                answer = findEntry(key, hashcode, retrieveMode, checkExpired, current, header.currentTableId());
                if (answer != null) {
                    return answer;
                }
            }

            //
            // Fell through, now search new table
            //
            index = header.getHtIndex(hashcode, header.alternateTableId());
            if (new_htindex[index] != 0) {
                if (debug)
                    print("B ");
                current = new_htindex[index];
                if (current == 0) {
                    throw new IllegalStateException("findEntry: ht pointer is null");
                }
                return findEntry(key, hashcode, retrieveMode, checkExpired, current, header.alternateTableId());
            }
            //
            // Fell through again, nothing here.
            //
            return null;
        } else {
            if (htindex[index] == 0) {
                return null;
            }
            int tableid = header.currentTableId();
            //current = header.calcOffset(index, tableid);
            current = htindex[index];
            if (current == 0) {
                throw new IllegalStateException("findEntry: ht pointer is null");
            }
            return findEntry(key, hashcode, retrieveMode, checkExpired, current, tableid);
        }
    }

    //************************************************************************
    // Given an hashcode, size and expirationTime, read it in.
    //
    // @param EvictionTableEntry  - 
    // @param both - if true, read both key and data; otherwise read only key
    // @param checkExpired - apply checking expiration logic if true.
    //************************************************************************
    private HashtableEntry findEntry(EvictionTableEntry evt, int retrieveMode)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        if (evt == null) {
            return null;
        }

        int hashcode = evt.hashcode;
        //System.out.println("evt.hashcode:"+ evt.hashcode);
        int index = header.getHtIndex(hashcode, header.currentTableId());
        long current = 0;
        HashtableEntry answer = null;

        if (header.rehashInProgress == 1) {

            if (debug)
                print("*");

            //
            // First search primary table.
            //
            if (htindex[index] != 0) {
                if (debug)
                    print("A");
                current = htindex[index];

                if (current == 0) {
                    throw new IllegalStateException("findEntry: ht pointer is null");
                }
                answer = findEntry(evt, retrieveMode, current, header.currentTableId());
                if (answer != null) {
                    return answer;
                }
            }

            //
            // Fell through, now search new table
            //
            index = header.getHtIndex(hashcode, header.alternateTableId());
            if (new_htindex[index] != 0) {
                if (debug)
                    print("B ");
                current = new_htindex[index];
                if (current == 0) {
                    throw new IllegalStateException("findEntry: ht pointer is null");
                }
                return findEntry(evt, retrieveMode, current, header.alternateTableId());
            }
            //
            // Fell through again, nothing here.
            //
            return null;
        } else {
            if (htindex[index] == 0) {
                return null;
            }
            int tableid = header.currentTableId();
            //current = header.calcOffset(index, tableid);
            current = htindex[index];
            if (current == 0) {
                throw new IllegalStateException("findEntry: ht pointer is null");
            }
            return findEntry(evt, retrieveMode, current, tableid);
        }
    }

    /**************************************************************************
     * Search disk for the indicated entry and return it and its
     * predecessor if any.
     *************************************************************************/
    private HashtableEntry findEntry(EvictionTableEntry evt, int retrieveMode, long current, int tableid)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {

        int hashcode = evt.hashcode;
        long previous = 0;

        long next = 0;
        int hash = 0;

        initReadBuffer(current);
        next = headerin.readLong();
        hash = headerin.readInt();

        while (current != 0) {
            //  System.out.println("current:"+current+" next:"+next);
            if (hash == hashcode) { // hash the same?
                // 		
                // Need to finish collision resolution and maybe return the object
                //   System.out.println("hashcode matched, hash:"+ hashcode); 
                HashtableEntry entry = readEntry2(current, next, hash, previous, retrieveMode, !CHECK_EXPIRED, tableid, null, evt); //493877
                if (entry != null) { // 493877 if entry is NOT null, the entry is found.
                    return entry;
                }
            }

            collisions++;
            //
            // Try for next object in bucket
            //
            previous = current;
            current = next;

            if (current != 0) { // optimize - don't read if we know we can't use it
                initReadBuffer(current);
                next = headerin.readLong();
                hash = headerin.readInt();
            }

        }

        //
        // If we fall through we did not find it
        //
        return null;
    }

    /**************************************************************************
     * Search disk for the indicated entry and return it and its
     * predecessor if any.
     *************************************************************************/
    private HashtableEntry findEntry(Object key, int hashcode, int retrieveMode, boolean checkExpired, long current, int tableid)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {

        long previous = 0;

        long next = 0;
        int hash = 0;

        initReadBuffer(current);
        next = headerin.readLong();
        hash = headerin.readInt();

        while (current != 0) {
            if (hash == hashcode) { // hash the same?
                // 
                // Need to finish collision resolution and maybe return the object
                //
                HashtableEntry entry = readEntry2(current, next, hash, previous, retrieveMode, checkExpired, tableid, key, null); //493877
                if (entry != null) { // 493877 if entry is NOT null, the entry is found.
                    return entry;
                }
            }

            collisions++;
            //
            // Try for next object in bucket
            //
            previous = current;
            current = next;

            if (current != 0) { // optimize - don't read if we know we can't use it
                initReadBuffer(current);
                next = headerin.readLong();
                hash = headerin.readInt();
            }

        }

        //
        // If we fall through we did not find it
        //
        return null;
    }

    /**
     * Generic routine to walk the hash table and pass each entry to
     * the "action" interface.
     */
    int walkHash(HashtableAction action, int retrieveMode, int index, int length)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        iterationLock.p();

        int tindex = -1;

        int tableSize = header.tablesize();
        //System.out.println("*** table size= " + tableSize + " index=" + index  + " length=" + length);

        try {
            for (int i = index, j = 0; i < tableSize; i++) {
                //
                // We want to drop the lock between each iteration so that other
                // threads can continue to work
                //
                boolean result = true;
                synchronized (this) {
                    long location = getHtindex(i, header.currentTableId());
                    long previous = 0;
                    long next = 0;
                    int hash = 0;
                    initReadBuffer(location);
                    next = headerin.readLong();
                    hash = headerin.readInt();

                    while (location != 0) {
                        HashtableEntry e = readEntry2(location, next, hash, previous, retrieveMode, !CHECK_EXPIRED, header.currentTableId(), null, null);
                        if (e != null) {
                            j++;
                            try {
                                Object id = e.getKey();
                                result = action.execute(e);
                                // if result is false, do not continue
                                if (result == false) { // LI4337-17
                                    traceDebug("walkHash()", "cacheName=" + this.htoddc.cacheName + " id=" + id + " action.execute() returns false.");
                                    break;
                                }
                            } catch (Exception xcp) {
                                throw new HashtableOnDiskException("HashtableAction: " + xcp.toString());
                            }
                            previous = location;
                            location = next;
                            if (location != 0) {
                                initReadBuffer(location);
                                next = headerin.readLong();
                                hash = headerin.readInt();
                            }
                        }
                    }
                    // if result is false, do not continue
                    if (result == false) { // LI4337-17
                        break;
                    }
                    if (j >= length) {
                        tindex = i + 1;
                        break;
                    }
                }
            }
        } finally {
            iterationLock.v();
        }
        if (tindex == -1) {
            tindex = tableSize;
        }
        //System.out.println("*** HTOD return index=" + tindex + " tableSize=" + tableSize);
        return tindex;
    }

    public void releaseMemoryToPool() {
        if (htindex != null) {
            this.htoddc.longArrayPool.returnToPool(new PrimitiveArrayPool.PoolEntry(htindex));
            htindex = null;
        }
        if (new_htindex != null) {
            this.htoddc.longArrayPool.returnToPool(new PrimitiveArrayPool.PoolEntry(new_htindex));
            new_htindex = null;
        }
    }

    class ListAction implements HashtableAction {
        Writer out;

        ListAction(Writer o) {
            out = o;
        }

        @Override
        public boolean execute(HashtableEntry e)
                        throws IOException {
            out.write(e.getKey().toString());
            out.write("\n");
            out.flush();
            return true;
        }

    }

    /**
     * Dump all keys to stdout.
     */
    public void listfiles(Writer o)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        ListAction act = new ListAction(o);
        walkHash(act, RETRIEVE_KEY, 0, -1);
    }

    public synchronized void reset_stats() {
        filemgr.reset_stats();
        collisions = 0;
        read_hits = 0;
        write_replacements = 0;
        serialize_time = 0;
        deserialize_time = 0;
        read_requests = 0;
        write_requests = 0;
        removes = 0;
        clears = 0;
        bytes_serialized = 0;
        bytes_deserialized = 0;
    }

    public void dump_stats_header(Writer out)
                    throws IOException {
        out.write("Header-Loc\t");
        out.write("Header-Size\t");
        out.write("Magic\t");
        out.write("Cur-Table\t");
        out.write("Num-Objects\t");
        out.write("Load-Factor\t");
        out.write("Auto_rehash\t");
        out.write("Rehash\t");

        out.write("Collisions\t");
        out.write("Read-Requests\t");
        out.write("Read-Hits\t");
        out.write("Write-Replacements\t");
        out.write("Write-Requests\t");
        out.write("Serialize-Time\t");
        out.write("Deserialize_time\t");
        out.write("Bytes-Serialized\t");
        out.write("Bytes-deSerialized\t");
        out.write("Removes\t");
        out.write("Clears\t");
    }

    public synchronized void dump_htod_stats(Writer out, boolean labels)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException

    {

        if (labels) {
            out.write("\n\n");
            out.write("--------------------------------------------------\n");
            out.write("HTOD Header:\n");
            out.write("--------------------------------------------------\n");
            out.write("Header location = " + (header.disklocation - SWORDSIZE) + "\n");
            out.write("Header size = " + filemgr.grain_size() + "\n");
            out.write("Magic string = " + header.magic + "\n");
            out.write("currentTablePtr = " + header.currentTablePtr + "\n");
            out.write("num_objects = " + header.num_objects() + "\n");
            out.write("loadFactor = " + header.loadFactor + "\n");
            out.write("auto_rehash = " + auto_rehash + "\n");
            out.write("rehashInProgress = " + header.rehashInProgress + "\n");

            if (header.tableLocation[0] == 0) {
                out.write("Hashtable[0] is empty\n");
            } else {
                out.write("Hashtable[0].tablesize = " + header.tablesize[0] + "\n");
                long true_location = header.tableLocation[0] - SWORDSIZE;
                int physical_size = header.tablesize[0];
                out.write("Hashtable[0] physical location, size = " +
                          true_location + " " +
                          physical_size + "\n");
            }

            if (header.tableLocation[1] == 0) {
                out.write("Hashtable[1] is empty\n");
            } else {
                out.write("Hashtable[1].tablesize = " + header.tablesize[1] + "\n");
                long true_location = header.tableLocation[1] - SWORDSIZE;
                int physical_size = header.tablesize[1];
                out.write("Hashtable[1] physical location, size = " +
                          true_location + " " +
                          physical_size + "\n");
            }

            out.write("collisions: " + collisions + "\n");
            out.write("read_requests " + read_requests + "\n");
            out.write("read_hits " + read_hits + "\n");
            out.write("write_replacements: " + write_replacements + "\n");
            out.write("write_requests: " + write_requests + "\n");
            out.write("serialize time: " + serialize_time + "\n");
            out.write("deserialize time: " + deserialize_time + "\n");
            out.write("bytes serialized: " + bytes_serialized + "\n");
            out.write("bytes deserialized: " + bytes_deserialized + "\n");
            out.write("removes: " + removes + "\n");
            out.write("clears: " + clears + "\n");

            out.write("--------------------------------------------------" + "\n");
        } else {
            out.write((header.disklocation - SWORDSIZE) + "\t");
            out.write(filemgr.grain_size() + "\t");
            out.write(header.magic + "\t");
            out.write(header.currentTablePtr + "\t");
            out.write(header.num_objects() + "\t");
            out.write(header.loadFactor + "\t");
            out.write(auto_rehash + "\t");
            out.write(header.rehashInProgress + "\t");

            out.write(collisions + "\t");
            out.write(read_requests + "\t");
            out.write(read_hits + "\t");
            out.write(write_replacements + "\t");
            out.write(write_requests + "\t");
            out.write(serialize_time + "\t");
            out.write(deserialize_time + "\t");
            out.write(bytes_serialized + "\t");
            out.write(bytes_deserialized + "\t");
            out.write(removes + "\t");
            out.write(clears + "\t");
        }
    }

    public void dumpFilemgr(Writer out)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        filemgr.dump_memory(out);
        filemgr.dump_disk_memory(out);
    }

    /**
     * We walk the physical disk hashtable printing information about it
     * to stdout for debugging and analysis purposes. Printed addresses are
     * true addresses, including the 4-byte length field.
     */
    public void analyzeHash(boolean full)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        //
        // Warning: don't call this on a "live" htod or you'll get strange results.
        // This method is intended for quiesced analysis.
        //
        int totalsize = 0;
        long true_location;

        true_location = header.disklocation - SWORDSIZE;
        filemgr.seek(true_location);
        int header_size = filemgr.readInt();

        //println("--------------------------------------------------");
        //println("FileManager information:");
        //println("--------------------------------------------------");
        //filemgr.dump_memory();
        // filemgr.dump_disk_memory(filename);
        //filemgr.show_stats();

        println("\n\n");
        println("--------------------------------------------------");
        println("Header information:\n");
        println("--------------------------------------------------");
        println("Header location = " + true_location);
        println("Header size = " + header_size);
        println("Magic string = " + header.magic);
        println("currentTablePtr = " + header.currentTablePtr);
        println("num_objects = " + header.num_objects());
        println("loadFactor = " + header.loadFactor);
        println("rehashInProgress = " + header.rehashInProgress);
        println("currentTablePtr = " + header.currentTablePtr);

        if (header.tableLocation[0] == 0) {
            println("Hashtable[0] is empty\n");
        } else {
            println("Hashtable[0].tablesize = " + header.tablesize[0]);
            true_location = header.tableLocation[0] - SWORDSIZE;
            filemgr.seek(true_location);
            int physical_size = filemgr.readInt();
            println("Hashtable[0] physical location, size = " +
                    true_location + " " +
                    physical_size);
        }

        if (header.tableLocation[1] == 0) {
            println("Hashtable[1] is empty\n");
        } else {
            println("Hashtable[1].tablesize = " + header.tablesize[1]);
            true_location = header.tableLocation[1] - SWORDSIZE;
            filemgr.seek(true_location);
            int physical_size = filemgr.readInt();
            println("Hashtable[1] physical location, size = " +
                    true_location + " " +
                    physical_size);
        }

        println("--------------------------------------------------");

        if (true) {
            return;
        }

        int tableid = header.currentTableId();
        for (int i = 0; i < header.tablesize(); i++) {

            filemgr.seek(header.calcOffset(i, tableid));
            long location = filemgr.readLong();
            true_location = location - SWORDSIZE;

            //
            // First get true object length
            //
            HashtableEntry e = readEntry(location, 0, RETRIEVE_ALL, !CHECK_EXPIRED, tableid);
            int bucketSize = 0;
            Vector bucketList = new Vector(10, 10);
            println("Bucket " + i + " starts:");
            while (e != null) {

                //
                // First lets peek at actual storage allocation.  The int just before
                // the entry is the buffered size, and the size of the actual object
                // is the size of the object itself.
                //
                filemgr.seek(true_location);
                int bufferedsize = filemgr.readInt();
                filemgr.seek(e.location);
                int objectsize = filemgr.readInt();

                totalsize++;
                bucketSize++;
                // bucketList.add(new AnalyzeStruct(e.key, true_location, bufferedsize, objectsize));

                println(".\t Entry = [loc(" +
                        true_location + ") bufsize(" +
                        bufferedsize + ") objsize(" +
                        objectsize + ") ] " +
                        e.key.toString());
                if (full) {
                    println(" \t Data is class " + e.value.getClass().getName());
                    if (e.value.getClass().getName().equals("[B")) {
                        println(" \t Data = " + new String((byte[]) e.value));
                    } else {
                        println(" \t Data = " + e.value.toString());
                    }
                }
                println(" \t ----------------------------------------------");

                true_location = e.next - SWORDSIZE;
                e = readEntry(e.next, e.location, RETRIEVE_ALL, !CHECK_EXPIRED, tableid);
            }

            println("Bucket " + i + " ends, size= " + bucketSize + "\n");
//             if ( bucketSize > 0 ) {
//                 for (int j = 0; j < bucketSize; j++ ) {
//                     AnalyzeStruct as = (AnalyzeStruct) bucketList.elementAt(j);
//                     println(".\t [loc(" + 
//                                        as.location + ") bufsize(" +
//                                        as.buffersize + ") objsize(" +
//                                        as.objectsize + ") ] " + 
//                                        as.object.toString());
//                 }
//             }

        }

        println("Total objects in the hashtable = " + totalsize);
    }

    class AnalyzeStruct {
        AnalyzeStruct(Object o, long loc, int bs, int os) {
            object = o;
            buffersize = bs;
            objectsize = os;
            location = loc;
        }

        Object object;
        int buffersize;
        int objectsize;
        long location;
    }

    /**
     * This walks the hash table and sets the internal count of objects.
     * Note that it also sort of works as a verifier of the content - if
     * we throw an exception we can be sure the file is corrupted.
     */
    private void countAndVerifyObjects()
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        println("countAndVerifyObjects(): Hashtable " + filename + " was not closed properly.  Validating ");
        header.set_num_objects(0);

        HashtableAction act = new HashtableAction() {
            public boolean execute(HashtableEntry e)
                            throws IOException
            {
                if ((header.num_objects() % 100) == 0) {
                    print(".");
                }
                header.incrementObjectCount();
                if (item_initialize != null) {
                    item_initialize.initialize(e.getKey(), e.getValue());
                }
                return true;
            }
        };

        walkHash(act, RETRIEVE_ALL, 0, -1);

        header.write();
        println("countAndVerifyObjects(): done");
    }

    /**
     * This walks the hash table and sets the internal count of objects. It is
     * streamlined to only get a count and not examine any objects.
     */
    private void countObjects()
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        println("countObjects(): Hashtable " + filename + " was not closed properly.  Validating ");

        iterationLock.p();

        int count = 0;
        try {
            for (int i = 0; i < header.tablesize(); i++) {

                //
                // We want to drop the lock between each iteration so that other
                // threads can continue to work
                //
                long next = getHtindex(i, header.currentTableId());// jrc 04/30/02 use cached entry

                while (next != 0) {
                    count++;
                    if ((count % 100) == 0) {
                        if (debug)
                            print(".");
                    }

                    filemgr.seek(next);
                    next = filemgr.readLong();
                }
            }

            header.set_num_objects(count);
            header.write();
        } finally {
            iterationLock.v();
        }
        println("countObjects(): done[" + count + "]");
    }

    /**
     * Internal method to do default rehash of doubling
     */
    private void rehash()
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        int size = (header.tablesize() * 2) + 1;
        if (this.tempTableSize > size) {
            doRehash(this.tempTableSize);
            this.tempTableSize = 0;
        } else {
            doRehash(size);
        }
    }

    /**
     * Double the size of the hash table -
     * 
     * The variable rehashInProgress is set to non-0 on disk to indicate
     * that rehashing is in progress. We use rehashInProgress to temporarily
     * hold the hash bucket in the new table while chaining the old one in.
     * So rehashInProgress is either 0 (rehash done), 1 (rehash in progress),
     * or something else (rehash in progress, points to hash bucket in new
     * table).
     * 
     * The algorithm is this:
     * 1. Allocate a new hashtable and set rehashInProgress to 1.
     * 2. For each entry in the old table (OT), calculate its new
     * hash in the new table. If the bucket is not empty, save
     * its value in the rehashInProgress pointer. Now place
     * the pointer to the entry in the new table.
     * 
     * If the entry has a "next" entry.
     * 3. Set the bucket pointer in OT to point to it.
     * 4. Set the "next" pointer in the entry to the saved
     * value from 2.
     * 5. Set rehashInProgress to 1.
     */
    private void doRehash(int new_table_size)
                    throws IOException,
                    EOFException,
                    FileManagerException,
                    ClassNotFoundException,
                    HashtableOnDiskException {
        //
        // Acquire the lock to prevent iteration while doubling
        //
        iterationLock.p();

        //
        // Step 0:
        // This should be our only exposure = for a short time we
        // will have allocated the new table but not stored its location
        // anywhere.  If we crash here we leak that storage permanently.
        //        

        long new_table = 0;

        header.setRehashFlag(1);
        new_table = filemgr.allocateAndClear(new_table_size * DWORDSIZE);

        header.initNewTable(new_table_size, new_table);
        PrimitiveArrayPool.PoolEntry longPoolEntry = this.htoddc.longArrayPool.allocate(new_table_size);
        new_htindex = (long[]) longPoolEntry.getArray();
        //new_htindex = new long[new_table_size];                                        

        Rehash rehash = new Rehash(this, new_table, new_table_size);
        Thread t = new Thread(rehash);
        t.start();
    }

    void rehashAllEntries(long new_table, int new_table_size)
                    throws IOException,
                    ClassNotFoundException,
                    FileManagerException,
                    HashtableOnDiskException {
        final String methodName = "rehashAllEntries()";
        //
        // Now walk the hash table and transfer the items
        //
        traceDebug(methodName, "cacheName=" + this.htoddc.cacheName + " new_table=" + new_table + " new_table_size=" + new_table_size);
        long start = System.nanoTime();

        try {
            int tableid = header.currentTableId();
            for (int i = 0; i < header.tablesize(); i++) {
                Thread.yield();
                synchronized (this) {
                    long old_bucket_pointer = header.calcOffset(i, tableid);
                    filemgr.seek(old_bucket_pointer); // get location of next item
                    long location = filemgr.readLong();

                    HashtableEntry e = readEntry(location, 0, RETRIEVE_KEY, !CHECK_EXPIRED, tableid); // read next item
                    long new_next = 0;
                    while (e != null) { // walk collision-chain for this bucket

                        //
                        // Step 1. generate new hash and insert pointer to object
                        //         in new table
                        //
                        int newindex = (e.hash & 0x7FFFFFFF) % new_table_size;
                        long diskloc = new_table + (newindex * DWORDSIZE);

                        filemgr.seek(diskloc);
                        new_next = filemgr.readLong(); // get existing chain

                        //
                        // If new_next is null we are starting a new bucket and life is
                        // good.  If new_next is not null we have to be very careful...
                        //
                        if (new_next != 0) {
                            //
                            // Step 1a - do not remove this comment - it is referenced in
                            //           comments to recover();
                            //
                            header.setRehashFlag(new_next); // remember where bucket is ...
                        }
                        filemgr.seek(diskloc);
                        filemgr.writeLong(e.location); // write new value
                        new_htindex[newindex] = e.location;

                        //
                        // Step 2: Update old hash table to point to next thing on
                        //         the hash chain
                        //
                        filemgr.seek(old_bucket_pointer);
                        filemgr.writeLong(e.next);
                        htindex[i] = e.next;

                        //
                        // Step 3: set "next" pointer in the object
                        //
                        long tempnext = e.next;
                        e.next = new_next;
                        updatePointer(e.location, e.next);
                        header.setRehashFlag(1); // clear swap space
                        htoddc.returnToHashtableEntryPool(e);

                        e = readEntry(tempnext, 0, RETRIEVE_KEY, !CHECK_EXPIRED, tableid);

                        if (debug)
                            print("."); // debugging
                    }
                    header.updateRehashIndex(i);
                }
            }

            //
            // Step 4.
            // all is transferred - swap the table pointers and clear the old ones.
            // Crashing here isn't fatal- in the worst case a little extra checking
            // is done on restart.
            //
            synchronized (this) {
                header.swapTables();

                threshold = (header.loadFactor * header.tablesize()) / 100; // when num_objects 
                // exceeds this we rehash again
                if (htindex != null) {
                    this.htoddc.longArrayPool.returnToPool(new PrimitiveArrayPool.PoolEntry(htindex));
                }

                htindex = new_htindex;
                new_htindex = null;
            }
        } finally {
            iterationLock.v();
        }
        traceDebug(methodName, "cacheName=" + this.htoddc.cacheName + " done - new_table=" + new_table + " elapsed=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    /*
     * This routine is called during initialization if the "rehash" flag is
     * discovered to have been set when the header is read from disk. During
     * rehash it is necessary to adjust (up to) three pointers for each object: the
     * pointer in the old hashtable, the pointer in the new hashtable, and the
     * "next" pointer of the object if there were multiple objects in the hash
     * bucket. Because it is impossible to guarantee simultaneous update of
     * all three pointers, a badly timed crash can leave the old and new
     * hashtables inconsistent.
     * 
     * This routine examines each entry the "new" hashtable to determine if
     * there are inconsistencies. Because only a single entry is updated at a
     * time, we can assume the two structures are consistent as soon as the
     * first "out of whack" entry is repaired.
     * 
     * Finally, after the two tables are known to be consistent, we simply
     * continue the rehash process.
     */
    void recover()
                    throws HashtableOnDiskException,
                    FileManagerException,
                    ClassNotFoundException,
                    IOException,
                    EOFException {

        println("recover(): Hashtable recover starts");

        //
        // Step 1 - check the two table pointers.  If only one is set it must
        //          also be the current pointer.  If only one is set we need only
        //          reset the rehashInProgress indicator and return.  Otherwise
        //          we continue to the next step.

        boolean done = false;
        if (header.tableLocation[0] == 0) { // Is there a table 0 pointer?
            if (header.currentTablePtr == 1) { // no .check - make sure current
                                               // is for table 1.
                if (header.tableLocation[1] != 0) { // And verify that table1 has a pointer
                    header.tableLocation[0] = 0;
                    header.tablesize[0] = 0;
                    done = true; // all ok
                } else {
                    throw new HashtableOnDiskException( // 
                                                       "Cannot recover hashtable, no valid table pointers.");
                }

            } else {
                throw new HashtableOnDiskException(
                                                   "Cannot recover hashtable, current table pointer is invalid.");
            }
        } else if (header.tableLocation[1] == 0) { // Is there a table 1 pointer?
            if (header.currentTablePtr == 0) { // no check - make sure current
                                               // is for table 0.
                if (header.tableLocation[0] != 0) { // And verify that table1 has a pointer
                    header.tableLocation[1] = 0;
                    header.tablesize[1] = 0;
                    done = true; // all ok
                } else {
                    throw new HashtableOnDiskException( // 
                                                       "Cannot recover hashtable, no valid table pointers.");
                }

            } else {
                throw new HashtableOnDiskException(
                                                   "Cannot recover hashtable, current table pointer is invalid.");
            }
        }

        if (done) {
            println("Recover(): ended - previous rehash did not enter critical section.");
            header.set_num_objects(0);
            header.rehashInProgress = 0;
            header.write();
            return;
        }

        //
        // Step 2. If we get here then both tables have pointers.  We must now
        //         look in every entry in the "new" table to see if it also occurs in
        //         the "old" table. If no entry occurs in both tables we can simply 
        //         restart the rehash.  Otherwise we must do some fixups first.
        //
        //         If we do a fixup, we must not find any other inconsistencies,
        //         since only a single entry is ever allowed to be inconsistent
        //         at a time.        
        //
        long new_table = header.alternateTable();
        int new_table_size = header.alternateSize();
        new_htindex = new long[new_table_size];

        println("Recover(): clearing inconsistencies");

        //
        clearInconsistencies(new_table, new_table_size);
        // Step 3. Resume the rehash process.
        //
        println("Recover(): resuming rehash.");

        iterationLock.p(); // released in rehashAllEntries, which is
                           // invoked in a new thread (see below)

        Rehash rehash = new Rehash(this, new_table, new_table_size);
        Thread t = new Thread(rehash);
        t.start();
    }

    /*************************************************************************
     * If we crash during a doubling operation there could be a small inconsistency
     * somewhere in the bowels of the hashtable. This routine looks all through
     * the ht until it finds a problem and repairs it.
     * 
     * This routine is a tad hairy - make absolutely SURE you know what you are
     * doing it you even think of touching it!
     *************************************************************************/
    void clearInconsistencies(long newtable, int newsize)
                    throws IOException,
                    EOFException,
                    ClassNotFoundException,
                    FileManagerException,
                    HashtableOnDiskException {
        //long oldtable = header.currentTable();
        //int oldsize = header.tablesize();

        for (int i = 0; i < newsize; i++) { // we iterate the NEW table
            long diskloc = newtable + (i * DWORDSIZE);
            filemgr.seek(diskloc);
            long location = filemgr.readLong(); // get existing chain head in NEW table

            if (header.rehashInProgress == location) {
                //
                // if this occurs we had just started a rehash but not
                // actually changed a table - i.e. step 1a in rehash(), and
                // ONLY step 1a has occurred. Step 1a saves the pointer in the
                // new hashtable index in rehashInProgress so that the
                // next physical  write can unchain the hash chain it points
                // to in prep for inserting the new item at the head of the
                // chain.  So if the above comparison is TRUE, that means we
                // have saved the chain pointer but not yet updated the pointer
                // in the new hashtable index (step 2).
                //
                // Therefore we can clear the rehashInProgress flag to "1" and
                // simply complete the rehash (because the structures are still
                // consistent).
                //
                header.setRehashFlag(1);
                return;
            }

            HashtableEntry e = readEntry(location, 0, RETRIEVE_KEY, !CHECK_EXPIRED,
                                         header.alternateTableId()); // read next item from NEW table

            while (e != null) { // walk down chain
                HashtableEntry oe = findEntry(e.key, e.hash, RETRIEVE_KEY, !CHECK_EXPIRED, 0,
                                              header.currentTableId()); // see if it is in oldtable also

                if (oe == null) { // not in old table
                    //
                    // We must check the "next" pointer on the new entry. If it
                    // is not 0, check to see if "next" points to an entry in
                    // the old table.  If so, the crash was between steps 2 and
                    // 3 in rehash(). 
                    //
                    if (e.next == 0) {
                        if (header.rehashInProgress != 1) {
                            e.next = header.rehashInProgress;
                        }
                        updatePointer(e.location, e.next);

                        header.setRehashFlag(1);
                        htoddc.returnToHashtableEntryPool(e);
                        return;

                    } else {
                        HashtableEntry ne = readEntry(e.next, 0, RETRIEVE_KEY, !CHECK_EXPIRED, header.alternateTableId());
                        oe = findEntry(ne.key, ne.hash, RETRIEVE_KEY, !CHECK_EXPIRED, 0,
                                       header.currentTableId());
                        htoddc.returnToHashtableEntryPool(ne);
                        if (oe != null) {
                            //
                            // The "next" entry is in the old table.  Fix up
                            // e.next and we're done.
                            //
                            if (header.rehashInProgress == 1) {
                                e.next = 0;
                            } else {
                                e.next = header.rehashInProgress;
                            }
                            updatePointer(e.location, e.next);

                            header.setRehashFlag(1);
                            htoddc.returnToHashtableEntryPool(oe);
                            htoddc.returnToHashtableEntryPool(e);
                            return;
                        }
                    }

                } else { // it is in old table
                    //
                    // If it is in both tables we crashed just before step 2 in
                    // rehash().  We will back out the partial update and trust
                    // the resume of rehash to redo things correctly.  The
                    // rehashInProgress value is 1 if there were no other items
                    // in the bucket, and it points to the other items which
                    // were temporarily unchained otherwise.
                    //
                    filemgr.seek(diskloc);
                    if (header.rehashInProgress == 1) {
                        //
                        // If here that means that step 1a in rehash was NOT run, which means
                        // there was no existing hash chain in the new table at this location,
                        // so we can zap the entry back to 0.
                        //
                        filemgr.writeLong(0); // clear "new" entry
                    } else {
                        //
                        // If here that means step 1a DID run, so there WAS alrady a chain in the
                        // new table at this location.  rehash() saved the head of that chain in
                        // the rehash flat so we can restore the new table like this.
                        //
                        filemgr.writeLong(header.rehashInProgress); // rechain things
                    }
                    header.setRehashFlag(1);
                    htoddc.returnToHashtableEntryPool(oe);
                    htoddc.returnToHashtableEntryPool(e);
                    return;
                }
                long nextLocation = e.next;
                htoddc.returnToHashtableEntryPool(e);
                e = readEntry(nextLocation, 0, RETRIEVE_KEY, !CHECK_EXPIRED, header.alternateTableId());
            }
        }
    }

    void println(String msg) {
        Tr.debug(tc, msg);
    }

    void print(String msg) {
        Tr.debug(tc, msg);
    }

    private void traceDebug(String methodName, String message) {
        if (IS_UNIT_TEST) {
            System.out.println(this.getClass().getName() + "." + methodName + " " + message);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " " + message);
            }
        }
    }

    /**************************************************************************
     * Debugging interface
     *************************************************************************/
    static void usage() {
        System.out.println("filemgr.HashtableOnDisk <fn> <instance> [-full]");
        System.exit(0);
    }

    public static void main(String args[]) {
        String filename = null;
        int instance = -1;
        boolean full = false;

        if (args.length < 2) {
            usage();
        }

        if (args.length == 2) {
            filename = args[0];
            instance = Integer.parseInt(args[1]);
            full = false;
        } else if (args.length == 3) {
            filename = args[0];
            instance = Integer.parseInt(args[1]);
            if (args[2].equals("-full")) {
                full = true;
            } else {
                usage();
            }
        } else {
            usage();
        }

        System.out.println("filename = " + filename);
        System.out.println("full = " + full);

        try {
            HTODDynacache hdc = new HTODDynacache();
            FileManager mgr = new FileManagerImpl(filename, false, "r", FileManager.MULTIVOLUME_PAGED, hdc);
            HashtableOnDisk ht = new HashtableOnDisk(mgr, true, instance, HAS_CACHE_VALUE, hdc);
            ht.analyzeHash(full);
            ht.close();
            mgr.close();
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
}

/*
 * We use two tables to insure that we can recover after a crash if the
 * crash occurs during a doubling operation. Current points to the currently
 * active table; the other table is null. When (current.length * loadFactor)
 * exceeds num_objects we allocate a new table of 2x current.length and assign
 * it to the "other table", then rehash each object and move it from current
 * to alternate. When all objects are moved we update to current to point to
 * the new table and deallocate the old one. At any time every object is
 * always in one of the two tables. If, during initializion, both tables
 * are non-null, that means we went down in the middle of a doubling. We
 * must finish moving objects from current to alternate and update the
 * pointer before returning in the constructor. We set a flag in the
 * rehash field so we can detect that crashed during rehash and can
 * initiate recovery.
 * 
 * We must store the number of current objects on disk; to
 * avoid the overhead of writing that for every insertion/deletion, we
 * maintain it in memory and write it to disk only in the close()
 * routine. During intialization, if that count is 0 we must walk the
 * table to get a correct count. Therefore, if the program
 * exits before calling close(), initialization will take a bit longer
 * than if a clean close() is completed.
 * 
 * Once the hastable has been initialized on disk the load factor cannot
 * be changed. The original values are used and the constructor
 * parameter is ignored.
 */

class HashHeader {
    /*
     * In C/C++ terms, the HashHeader looks (more or less) like this. Ints are 4 bytes,
     * longs are 8 bytes
     * 
     * struct hashtable { // 12 bytes total
     * int length; // size of this table
     * long table; // disk offset to first table element
     * }
     * 
     * struct root { // 44 bytes total
     * int magic; // magic# to identify this as a HashTableOnDIsk
     * // and also server as a version identifier.
     * int current; // pointer to table1 or table2 to indicate
     * // which is current - offset 4
     * int num_objects; // total objects in the table - offset 8
     * int loadFactor; // Load factor - offset 12
     * long rehash; // Set during rehash for recovery - offset 16
     * hashtable table1; // First table - offset 24
     * hashtable table2; // Second table - offset 36
     * } // end of table - offset 48
     */
    int magic;
    int currentTablePtr = 0; // current-table pointer
    private int num_objects = -1; // number of objs in htable
    int loadFactor = 75; // to determine when to rehash
    long rehashInProgress = 0; // remember we're in critical state
    long disklocation = -1;
    int currentRehashIndex = -1; // if rehashiInProgress, this records how
                                 // far down the original hashtable we've gone

    int tablesize[];
    long tableLocation[];

    private boolean dirty = false;
    private FileManager filemgr = null;

    //
    // These are the offsets of each field within the root structure
    //
    private final long headerLoc = 0; // disk offset to header
    private long currentOffset = 4; // location of current-table pointer
    private long countOffset = 8; // location of on-disk object count
    private long loadfOffset = 12; // location of load factor
    private long rehashOffset = 16; // location o rehash-in-progress flag
    private long table1Offset = 24; // location of hashtable 1 header
    private long table2Offset = 36; // location of  hashtable 2 header
    private final int headerSize = 48;

    HashHeader(FileManager fmgr, int mgc, int load, int tabsize)
        throws IOException,
        EOFException,
        FileManagerException {
        filemgr = fmgr;
        magic = mgc;
        loadFactor = load;

        disklocation = filemgr.allocate(headerSize);
        currentOffset += disklocation; // adjust offsets relative to disklocation
        countOffset += disklocation;
        loadfOffset += disklocation;
        rehashOffset += disklocation;
        table1Offset += disklocation;
        table2Offset += disklocation;
        currentTablePtr = 0;

        tablesize = new int[2];
        tableLocation = new long[2];
        tablesize[0] = tabsize;
        tablesize[1] = 0;
        tableLocation[0] = filemgr.allocateAndClear(tablesize[0] * HashtableOnDisk.DWORDSIZE);
        tableLocation[1] = 0;
        write();
    }

    HashHeader(FileManager fmgr, long instanceid)
        throws IOException {
        filemgr = fmgr;
        this.disklocation = instanceid;
        read();
    }

    public int num_objects() {
        return num_objects;
    }

    public void set_num_objects(int count) {
        num_objects = count;
    }

    public void updateRehashIndex(int ndx) {
        currentRehashIndex = ndx;
    }

    public int getHtIndex(int hcode, int tableid) {
        if (tablesize[tableid] == 0) {
            return 0;
        }
        return (hcode & 0x7FFFFFFF) % tablesize[tableid];
    }

    public long calcOffset(int hashindex, int tableid) {
        if (tableLocation[tableid] == 0) {
            return 0;
        }
        return tableLocation[tableid] + (hashindex * HashtableOnDisk.DWORDSIZE);
    }

    void read()
                    throws IOException {
        currentOffset += disklocation; // adjust offsets relative to disklocation
        countOffset += disklocation;
        loadfOffset += disklocation;
        rehashOffset += disklocation;
        table1Offset += disklocation;
        table2Offset += disklocation;

        filemgr.seek(disklocation);
        magic = filemgr.readInt();
        currentTablePtr = filemgr.readInt();
        num_objects = filemgr.readInt();
        loadFactor = filemgr.readInt(); // original load factor
        rehashInProgress = filemgr.readLong();

        tablesize = new int[2];
        tableLocation = new long[2];
        tablesize[0] = filemgr.readInt();
        tableLocation[0] = filemgr.readLong();
        tablesize[1] = filemgr.readInt();
        tableLocation[1] = filemgr.readLong();
    }

    void write()
                    throws IOException {
        if (!filemgr.isReadOnly()) {
            filemgr.seek(disklocation);
            filemgr.writeInt(magic);
            filemgr.writeInt(currentTablePtr);
            filemgr.writeInt(num_objects);
            filemgr.writeInt(loadFactor); // original load factor
            filemgr.writeLong(rehashInProgress);

            filemgr.writeInt(tablesize[0]);
            filemgr.writeLong(tableLocation[0]);
            filemgr.writeInt(tablesize[1]);
            filemgr.writeLong(tableLocation[1]);
            dirty = false;
        } else {
            System.out.println("Hashtable opened RO, skipping header update");
        }
    }

    int currentTableId() {
        return currentTablePtr;
    }

    int alternateTableId() {
        if (currentTablePtr == 0) {
            return 1;
        } else {
            return 0;
        }
    }

    int getTableidForNewEntry() {
        if (rehashInProgress == 1) {
            return alternateTableId();
        } else {
            return currentTableId();
        }
    }

    long currentTable() {
        return tableLocation[currentTablePtr];
    }

    int tablesize() {
        return tablesize[currentTablePtr];
    }

    int tablesize(int tableid) {
        return tablesize[tableid];
    }

    long alternateTable() {
        if (currentTablePtr == 0) {
            return tableLocation[1];
        } else {
            return tableLocation[0];
        }
    }

    int alternateSize() {
        if (currentTablePtr == 0) {
            return tablesize[1];
        } else {
            return tablesize[0];
        }
    }

    void writeCount()
                    throws IOException {
        //
        // This extra update seems not to make much of a performance difference, 
        // and vastly decreases the startup time after failure.
        //
        if (dirty) {
            return;
        } else {
            if (!filemgr.isReadOnly()) { // temporarily turned off ...
                filemgr.seek(countOffset);
                filemgr.writeInt(0);
                dirty = true;
            }
        }
//        
//   This code keeps the count current all the time, eliminating the need to count
//   objects on restart, and vastly decreasing startup time after failure.  We have
//   currently disabled it in favor of the "dirty" checking above, to speed up
//   run-time, under the assumption that the current use of HTOD will usually start
//   with a new one.  We should probably replace the above with the following if
//   restart becomes an issue, however.
//

//          if ( !filemgr.isReadOnly() ) {         // temporarily turned off ...
//              filemgr.seek(countOffset);
//              filemgr.writeInt(num_objects);
//          }
    }

    void incrementObjectCount()
                    throws IOException {
        if (num_objects < 0) {
            num_objects = 0;
        }

        num_objects++;
        writeCount();
    }

    void decrementObjectCount()
                    throws IOException {
        num_objects--;
        writeCount();
    }

    /**
     * Set the rehash indicator. If set during startup we must
     * initiate recovery and complete the rehash.
     */
    void setRehashFlag(long location)
                    throws IOException,
                    EOFException {
        rehashInProgress = location;
        filemgr.seek(rehashOffset);
        filemgr.writeLong(location);
    }

    void initNewTable(int new_table_size, long new_table)
                    throws IOException {
        if (currentTablePtr == 0) {
            filemgr.seek(table2Offset);
            filemgr.writeInt(new_table_size);
            filemgr.writeLong(new_table);
            tablesize[1] = new_table_size;
            tableLocation[1] = new_table;
        } else {
            filemgr.seek(table1Offset);
            filemgr.writeInt(new_table_size);
            filemgr.writeLong(new_table);
            tablesize[0] = new_table_size;
            tableLocation[0] = new_table;
        }
    }

    void swapTables()
                    throws IOException,
                    ClassNotFoundException,
                    FileManagerException {
        //
        // We update current info asap since as soon as this is complete
        // we cannot have integrity problems.  After we're safe we deallocate
        // the old table, so the only possible problem is a leak of that space.
        //
        long save = 0;
        rehashInProgress = 0;
        currentRehashIndex = -1;
        if (currentTablePtr == 0) {
            save = tableLocation[0];
            currentTablePtr = 1;
            tableLocation[0] = 0; // zap table 1 in memory
            tablesize[0] = 0;
            write();
        } else {
            save = tableLocation[1];
            currentTablePtr = 0;
            tableLocation[1] = 0;
            tablesize[1] = 0;
            write();
        }

        filemgr.deallocate(save); // deallocate the space
    }
}

class Rehash
                implements Runnable {
    HashtableOnDisk htod;
    long new_table;
    int new_table_size;

    Rehash(HashtableOnDisk htod, long new_table, int new_table_size) {
        this.htod = htod;
        this.new_table = new_table;
        this.new_table_size = new_table_size;
    }

    public void run() {
        try {
            htod.rehashAllEntries(new_table, new_table_size);
        } catch (Exception e) {
            FFDCFilter.processException(e, this.getClass().getName() + ".rehash()", "3260", this);
        }
    }
}
