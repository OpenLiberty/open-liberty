/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cache.persistent.filemgr;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.ibm.ws.cache.HTODDynacache;
import com.ibm.ws.cache.PrimitiveArrayPool;
import com.ibm.ws.cache.persistent.util.ByteArrayPlusOutputStream;

/**************************************************************************
 * Class to implement dynamic storage allocator on disk using a quick fit
 * system.
 * This system satisfies requests in a fault-tolerant way.  Free list
 * structures are maintained on disk.  The first header word of each block on
 * disk contains a block size.  No free list structures are maintained on disk.
 * Each allocation, deallocation only requires one write, except for tail
 * allocations which require 2 writes.
 * By convention, offsets representing addresses are long integers, sizes
 * are regular integers.
 *************************************************************************/
public class FileManagerImpl implements FileManager, Constants, Instrumentation {

    private long allocs = 0;
    private long deallocs = 0;
    private long coalesces = 0;

    private long allocated_blocks = 0;
    private long free_blocks = 0;
    private long allocated_words = 0;
    private long free_words = 0;

    private long seeks = 0;
    private long small_requests = 0;
    private long large_requests = 0;
    private long ql_hits = 0;
    private long ml_hits = 0;
    private long ml_splits = 0;
    private long ml_blocks_searched = 0;
    private int fast_startups = 0;
    private int nonempty_lists = 0;


    //private int cache_hits = 0;
    //private int cache_misses = 0;
    //private int page_allocations = 0;
    private long write_time = 0;
    private long read_time = 0;
    private int read_count = 0;
    private int write_count = 0;
    private long bytes_read = 0;
    private long bytes_written = 0;

    // The following are default settings for parameters.  If the storage
    // manager is being run on an already existing file, values are taken from
    // the file.
    private int first_quick_size = 1;
    private int last_quick_size = 75;
    private int grain_size = 512;
    private int acceptable_waste = 2000;

    // should be > first_quick_size * grain_size
    private int first_quick_size_block;
    private int last_quick_size_block;
    private int last_ql_index;

    private boolean readOnly;
    private long tail_ptr;

    private String filename;
    //private long cachesize;
    private int type;
    private int truetype;
    //private int pagesize;

    private Listhead[] ql_heads;    // The last element of ql_heads is a pointer to the misc list
    private long[][] userData = new long[NUM_USER_WORDS][2];

    private PhysicalFileInterface physical = null;

    private HTODDynacache htoddc = null;

    /**************************************************************************
     * Constructor.  If file exists, read in free lists from disk.  Otherwise,
     * initialize file, in-memory data structures.  The "coalesce_blocks"
     * parameter indicates whether the system should try to coalesce adjacent
     * free blocks upon start-up.   Note that this code knows nothing of the
     * application using it such as the HTOD.
     *
     * @param fname            The name of the file.
     * @param coalesce_blocks  If true, coalesce_blocks on startup.
     * @param mode             "r" for read-only, "rw" for read-write
     * @param type             Type of physical I/O manager.  The only option
     *                         currently supported is MULTIVOLUME.
     *
     * @exception FileManagerException
     * @exception IOException
     *************************************************************************/
    public FileManagerImpl(String fname, 
                           boolean coalesce_blocks, 
                           String mode, 
                           int type,
                           HTODDynacache htoddc)
    throws FileManagerException, IOException
    {
        this.type = type;
        this.htoddc = htoddc;
        if ( type == MULTIVOLUME ) {
            physical = new MultivolumeRAFWrapper(fname, mode, this, htoddc.getDiskCacheSizeInfo());
        } else {
            throw new FileManagerException("Unknown type: " + type);           
        }

        filename = fname;

        long cache_end;
        long magic_number;
        int qlist_num;

        if (mode.equals("r")) {
            readOnly = true;
        } else {
            readOnly = false;
        };

        //
        // Bootstrap code.  This relies on the first "page" being identical for all physical
        // implementations, and starting at location 0, and residing within the first file of a
        // multi-volume set.
        //
        if (length() == 0) {
            if (readOnly) {
                throw (new FileManagerException("Attempt to open an empty file in read only mode"));
            }
            tail_ptr = FIRST_TAIL;
            seek(STARTOFDATA_LOCATION);
            writeLong(MAGIC);
            writeLong(FIRST_TAIL);
            writeLong(DISK_NULL);
            writeInt(first_quick_size);
            writeInt(last_quick_size);
            writeInt(grain_size);
            writeInt(acceptable_waste);
            byte[] clear = new byte[RESERVED_WORDS_SIZE]; // clear reserved words
            write(clear);
            clear = new byte[USER_AREA_SIZE];           // clear user area
            write(clear);

            //
            // if changes to init require i/o remember to skip past the reserved
            // space before doing anything.
            //
            init_freelists(true);
        } else {
            seek(STARTOFDATA_LOCATION);
            magic_number = readLong();
            if ( magic_number != MAGIC) {
                throw (new FileManagerException("File not valid (invalid magic string). Expected "
                                                + MAGIC + " received " + magic_number));
            }

            tail_ptr = readLong();
            if (tail_ptr < FIRST_TAIL) {
                throw (new FileManagerException("File not valid (illegal tail pointer)"));
            }
            cache_end = readLong();
            if (cache_end < 0) {
                throw (new FileManagerException("File not valid (illegal end of cache pointer)"));
            }
            first_quick_size = readInt();
            if (first_quick_size < 1) {
                throw (new FileManagerException("File not valid (illegal first quick size)"));
            }
            last_quick_size = readInt();
            if (last_quick_size < first_quick_size) {
                throw (new FileManagerException("File not valid (illegal last quick size)"));
            }
            grain_size = readInt();
            if (grain_size < 1) {
                throw (new FileManagerException("File not valid (illegal grain size)"));
            }
            acceptable_waste = readInt();

            read_user_data();

            init_freelists(true);
            if (acceptable_waste < 1) {
                throw (new FileManagerException("File not valid (illegal acceptable waste)"));
            }

            if (coalesce_blocks && (!readOnly)) {
                read_block_sizes_from_disk(true);
                seek(CACHE_END);
                writeLong(DISK_NULL);
            } else {
                if (cache_end == DISK_NULL) {
                    read_block_sizes_from_disk(false);
                } else {
                    read_block_sizes_from_cache(cache_end);
                }
            }
        }
    }

    private void read_user_data()
    throws IOException
    {
        // Read userdata from disk during init.
        seek(USER_AREA_START);
        for ( int i = 0; i < NUM_USER_WORDS; i++ ) {
            userData[i][0] = readLong();
            userData[i][1] = readLong();
        }
    }

    private void writeUserData(int ndx)
    throws IOException
    {
        // sync disk version of userdata with memory version
        long location = USER_AREA_START + (ndx * 2 * LONG_SIZE);
        seek(location);
        writeLong(userData[ndx][0]);
        writeLong(userData[ndx][1]);
    }

    /**************************************************************************
     * Fetch userdata associated with instanceid
     *
     * @param instanceid This is the unique identifier for this userdata
     *
     * @return the data if it can be found.  Throw exception otherwise.
     *
     * @throws IOException if disk write fails.
     *************************************************************************/
    public long fetchUserData(long instanceid)
    throws IOException
    {
        for ( int i = 0; i < NUM_USER_WORDS; i++ ) {
            if ( userData[i][0] == instanceid ) {
                return userData[i][1];
            }
        }
        return 0;
    }

    /**************************************************************************
     * Save userdata in parm 1 under unique serial number from parm2.
     *
     * @param instanceid This is the unique identifier for this userdata
     * @param data This is the data associated with the instanceid
     *
     * @return true if the data is stored, false if the serial number is 0
     *
     * @throws IOException if disk write fails.
     * @throws FileManagerException if there are no slots available for the
     *         userdata.
     *************************************************************************/
    public boolean storeUserData(long instanceid, long data)
    throws IOException,
    FileManagerException
    {
        if ( instanceid == 0 ) {
            return false;
        }
        for ( int i = 0; i < NUM_USER_WORDS; i++ ) {
            if ( userData[i][0] == instanceid ) {
                userData[i][1] = data;
                writeUserData(i);
                return true;
            }
        }

        //
        // If we fall through then this is the first time.  Now search for an
        // opening.
        //
        for ( int i = 0; i < NUM_USER_WORDS; i++ ) {
            if ( userData[i][0] == 0 ) {
                userData[i][0] = instanceid;
                userData[i][1] = data;
                writeUserData(i);
                return true;
            }
        }

        //
        // If we fall through then all userdata slots are filled. Throw an
        // exception so caller knows why he's hosed.
        //
        throw new FileManagerException("storeUserdata: No remaining slots");
    }

    //************************************************************************
    // Forward all physical calls to the physical implementation.
    //************************************************************************
    public String filename() 
    { 
        return physical.filename(); 
    }

    public long length() 
    throws IOException
    { 
        return physical.length(); 
    }

    public void close() 
    throws IOException 
    { 
        cache_free_storage_info();
        physical.close(); 
    }

    public void flush() 
    throws IOException 
    { 
        physical.flush(); 
    }

    public int read() 
    throws IOException 
    { 
        bytes_read++;
        return physical.read(); 
    }

    public int read(byte[] v) 
    throws IOException 
    { 
        int answer = physical.read(v); 
        bytes_read += answer;
        return answer;
    }

    public int read(byte[] v, int off, int len) 
    throws IOException 
    { 
        int answer = physical.read(v, off, len); 
        bytes_read += answer;
        return answer;
    }

    public int readInt()
    throws IOException 
    { 
        bytes_read += 4;
        return physical.readInt();
    }

    public long readLong()
    throws IOException 
    { 
        bytes_read += 8;
        return physical.readLong();
    }

    public short readShort()
    throws IOException 
    { 
        bytes_read += 2;
        return physical.readShort();
    }

    public void seek(long loc)
    throws IOException 
    { 
        physical.seek(loc);
    }

    public void write(byte[] v)
    throws IOException 
    { 
        physical.write(v);
        bytes_written += v.length;
    }

    public void write(byte[] v, int off, int len)
    throws IOException 
    { 
        physical.write(v, off, len);
        bytes_written += len;
    }

    public void write(int v)
    throws IOException 
    { 
        physical.write(v);
        bytes_written++;
    }

    public void writeInt(int v)
    throws IOException 
    { 
        physical.writeInt(v);
        bytes_written += 4;
    }

    public void writeLong(long v)
    throws IOException 
    { 
        physical.writeLong(v);
        bytes_written += 8;
    }

    public void writeShort(short v)
    throws IOException 
    { 
        physical.writeShort(v);
        bytes_written += 2;
    }


    /**************************************************************************
     * @return   true if the file is opened read-only
     *************************************************************************/
    public boolean isReadOnly() {
        return readOnly;
    }
    /* Initialize storage parameters */
    private void init_storage_parameters()
    {
        first_quick_size_block = first_quick_size * grain_size;
        last_quick_size_block = last_quick_size * grain_size;
        last_ql_index = last_quick_size + 1 - first_quick_size;
        ql_heads = new Listhead[last_ql_index + 1];
    }

    /* Initialize in-memory free lists */
    private void init_freelists(boolean create)
    {
        int qlist_num;

        if (create == true) {
            init_storage_parameters();
        };
        for (qlist_num = 0; qlist_num <= last_ql_index;
            qlist_num++) {
            if (create == true) {
                ql_heads[qlist_num] =  new Listhead();
            }
            ql_heads[qlist_num].length = 0;
            ql_heads[qlist_num].first_block = null;
        };
        nonempty_lists = 0;
    }

    /* Read tail from disk */
    private void read_tail_from_disk()
    throws IOException
    {
        seek(TAIL_DPTR);
        tail_ptr = readLong();
    }

    /* Coalesce adjacent free blocks on disk */
    private int combine_blocks(long addr)
    throws IOException
    {
        long cum_length;
        int length;
        long next_addr = addr;

        length = readInt();
        if (length > 0) {
            while ((length > 0) && (next_addr + length < tail_ptr)) {
                next_addr += length;
                seek(next_addr);
                length = readInt();
            }
            if (length < 0) {
                cum_length = next_addr - addr;
                seek(addr);
                writeInt((int) cum_length);
            } else {
                cum_length = tail_ptr - addr;
            }
            return(int) cum_length;
        } else {
            return length;
        }
    }


    /* Read block sizes from disk */
    private void read_block_sizes_from_disk(boolean coalesce_blocks)
    throws IOException
    {
        long addr = FIRST_TAIL;
        int length;
        long new_tail = tail_ptr;

        while (addr < tail_ptr) {
            seek(addr);
            if (coalesce_blocks) {
                length = combine_blocks(addr);
            } else {
                length = readInt();
            }
            if (length > 0) {
                if ((length + addr >= tail_ptr) && (!readOnly)) {
                    // add last block to tail
                    new_tail = addr;
                } else {
                    add_to_freelist(addr, length);
                }
            } else {
                length = - length;
                allocated_blocks++;
                allocated_words += length;
            }
            addr = addr + length;
        }
        if ((new_tail != tail_ptr) && (!readOnly)) {
            tail_ptr = new_tail;
            seek(TAIL_DPTR);
            writeLong(tail_ptr);
        }

    }

    /* Read block sizes from cached area on disk */
    private void read_block_sizes_from_cache(long cache_end)
    throws IOException
    {
        int block_size;
        long block_address;
        ByteArrayInputStream bis;
        byte[] buf;
        long cache_cursor;
        DataInputStream dis;
        int list_index;

        buf = new byte[(int) (cache_end - tail_ptr)];
        seek(tail_ptr);
        read(buf);
        bis = new ByteArrayInputStream(buf);
        dis = new DataInputStream(bis);
        allocated_blocks = dis.readLong();
        allocated_words = dis.readLong();
        free_blocks = 0;
        free_words = 0;
        cache_cursor = tail_ptr + (2*LONG_SIZE);
        while (cache_cursor < cache_end) {
            list_index = dis.readInt();
            block_address = dis.readLong();
            cache_cursor += INT_SIZE + PTR_SIZE;
            while (block_address != DISK_NULL) {
                block_size = dis.readInt();
                add_new_block_to_freelist(block_address, block_size,
                                          list_index);
                block_address = dis.readLong();
                cache_cursor += INT_SIZE + PTR_SIZE;
            }
        }
        dis.close();
        bis.close();
        if (!readOnly) {
            seek(CACHE_END);
            writeLong(DISK_NULL);
        }
        fast_startups++;
    }

    /*************************************************************************
    * Cache free storage information for fast restart 
    *
    * @exception IOException 
    *************************************************************************/
    public void cache_free_storage_info()
    throws IOException
    {
        Block block;
        byte[] buf;
        ByteArrayPlusOutputStream bos;
        long cache_size;
        DataOutputStream dos;
        int qlist_num;

        if (readOnly) {
            throw (new FileManagerException("Attempt to cache free storage information in read only mode"));
        }
        cache_size = (2*LONG_SIZE) + ((free_blocks + nonempty_lists) *
                                      (INT_SIZE + PTR_SIZE));
        bos = new ByteArrayPlusOutputStream((int) cache_size);
        dos = new DataOutputStream(bos);
        dos.writeLong(allocated_blocks);
        dos.writeLong(allocated_words);
        for (qlist_num = 0; qlist_num <= last_ql_index;
            qlist_num++) {
            if (ql_heads[qlist_num].first_block != null) {
                dos.writeInt(qlist_num);
                block = ql_heads[qlist_num].first_block;
                while (block != null) {
                    dos.writeLong(block.address); 
                    dos.writeInt(block.size); 
                    block = block.next;
                }
                dos.writeLong(DISK_NULL);
            }
        }
        dos.close();
        bos.close();
        buf = bos.getTheBuffer();

        seek(tail_ptr);
        write(buf);
        seek(CACHE_END);
        writeLong(tail_ptr + cache_size);
    }

    /**************************************************************************
     * Outputs storage information stored on disk.  Debugging interface.
     * 
     * @param fname  the name of the file to dump the info into.
     *************************************************************************/
    public void dump_disk_memory(Writer out)
    throws IOException
    {
        long block_ptr;
        long cache_end;

        long magic;
        int size;
        long tail;

        out.write("Information stored on disk\n");
        seek(STARTOFDATA_LOCATION);
        magic = readLong();
        out.write("File magic number: " + magic + "\n");
        seek(TAIL_DPTR);
        tail = readLong();
        out.write("Tail on disk: " + tail + "\n");
        cache_end = readLong();
        out.write("End of cached free list info: " + cache_end + "\n");
        size = readInt();
        out.write("First quick size: " + size + "\n");
        size = readInt();
        out.write("Last quick size: " + size + "\n");
        size = readInt();
        out.write("Grain size: " + size + "\n");
        size = readInt();
        out.write("Acceptable waste: " + size + "\n");
        block_ptr = FIRST_TAIL;
        while (block_ptr < tail) {
            seek(block_ptr);
            size = readInt();
            out.write(block_ptr + ", " + size + "\n");
            block_ptr += abs(size);
        };
        out.write("\n\n");
    }


    /* Do a seek and count it */
    private void seek_and_count (long offset)
    throws IOException
    {
        seek(offset);
        seeks++;
    }


    /* print a free list stored in memory */
    private void print_memory_freelist(Writer out, Block block)
    throws IOException
    {
        while (block != null) {
            out.write(block.address + " " + block.size + ", ");
            block = block.next;
        }
        out.write("\n");
    }

    public void reset_stats()
    {
        seeks = 0;
        small_requests = 0;
        large_requests = 0;
        ql_hits = 0;
        ml_hits = 0;
        ml_splits = 0;
        ml_blocks_searched = 0;
        fast_startups = 0;
        deallocs = 0;
        allocs = 0;

        read_count = 0;
        write_count = 0;
        read_time = 0;
        write_time = 0;
        bytes_read = 0;
        bytes_written = 0;
    }

    public void dump_stats_header(Writer out)
    throws IOException
    {
        //
        // You MUST update this method if you update the dump_stats method.  The labels
        // must not have whitespace chars in them.
        //
        out.write("Filename\t");
        out.write("Allocations\t");
        out.write("Deallocations\t");
        out.write("Coalesces\t");
        out.write("Alloc-Blocks\t");
        out.write("Allocated-Words\t");     
        out.write("Free-Blocks\t");
        out.write("Free-Words\t");

        out.write("Seeks\t");
        out.write("Reads\t");
        out.write("Writes\t");
        out.write("Read-Time\t");
        out.write("Write-Time\t");

        out.write("Bytes-Read\t");
        out.write("Bytes-Written\t");

        out.write("Small-Requests\t");
        out.write("Large-Requests\t");
        out.write("QL-Hits\t");
        out.write("ML-Hits\t");
        out.write("ML-Splits\t");
        out.write("ML-Blocks_searched\t");
        out.write("Fast-Startups\t");

        //out.write("Cache-Pagesize\t");
        //out.write("Cachesize\t");
        //out.write("Cache-Hits\t");
        //out.write("Cache-Misses\t");
        //out.write("Page-Allocations\t");
    }

    /**
     * Outputs statistics on memory management.  Debugging interface, writes
     * interesting stuff to stdout.
     */
    public void dump_stats(Writer out, boolean labels)
    throws IOException
    {
        if ( labels ) {
            out.write("--------------------------------------------------\n");
            out.write("FileManager Header:\n");
            out.write("--------------------------------------------------\n");
            out.write("Filename = " + filename + "\n");
            out.write("Allocations: " + allocs + "\n");
            out.write("Deallocations: " + deallocs +"\n");
            out.write("Coalesces: " + coalesces + "\n");
            out.write("Allocated blocks: " + allocated_blocks + "\n");
            out.write("Allocated bytes: " + allocated_words +"\n");           
            out.write("Free blocks: " + free_blocks+ "\n");
            out.write("Free bytes: " + free_words+ "\n");
            out.write("Seeks: " + seeks+ "\n");
            out.write("Requests for small blocks: " + small_requests+ "\n");
            out.write("Requests for large blocks: " + large_requests+ "\n");
            out.write("Quick list hits: " + ql_hits+ "\n");
            out.write("Misc list hits: " + ml_hits+ "\n");
            out.write("Block splits (from misc list): " + ml_splits+ "\n");
            out.write("Misc list blocks searched: " + ml_blocks_searched+ "\n");
            out.write("Fast startups: " + fast_startups+ "\n");

            //out.write("Internal blocksize = " + CACHE_PAGESIZE + "\n");
            //out.write("Cache size = " + cachesize+ "\n");
            //out.write("Cache hits: " + cache_hits+ "\n");
            //out.write("Cache misses: " + cache_misses+ "\n");
            //out.write("Page allocations: " + page_allocations+ "\n");
            out.write("Read requests: " + read_count + "\n");
            out.write("Write requests: " + write_count+ "\n");
            out.write("Accumulated physical read time:" + read_time+ "\n");
            out.write("Accumulated physical write time:" + write_time+ "\n");
            out.write("Bytes read: " + bytes_read + "\n");
            out.write("Bytes written: " + bytes_written + "\n");
        } else {
            out.write(filename + "\t");
            out.write(allocs + "\t");
            out.write(deallocs +"\t");
            out.write(coalesces + "\t");
            out.write(allocated_blocks + "\t");
            out.write(allocated_words +"\t");           
            out.write(free_blocks+ "\t");
            out.write(free_words+ "\t");

            out.write(seeks+ "\t");
            out.write(read_count + "\t");
            out.write(write_count+ "\t");
            out.write(read_time+ "\t");
            out.write(write_time+ "\t");

            out.write(bytes_read + "\t");
            out.write(bytes_written + "\t");

            out.write(small_requests+ "\t");
            out.write(large_requests+ "\t");
            out.write(ql_hits+ "\t");
            out.write(ml_hits+ "\t");
            out.write(ml_splits+ "\t");
            out.write(ml_blocks_searched+ "\t");
            out.write(fast_startups+ "\t");

            //out.write(CACHE_PAGESIZE + "\t");
            //out.write(cachesize+ "\t");
            //out.write(cache_hits+ "\t");
            //out.write(cache_misses+ "\t");
            //out.write(page_allocations+ "\t");

        }
    }

    /**
     * Outputs storage information stored in main memory.  Debugging interface,
     * writes interesting stuff to stdout.
     */
    public void dump_memory(Writer out)
    throws IOException
    {
        int qlist_num;

        out.write("First quick size: " + first_quick_size + "\n");
        out.write("Last quick size: " + last_quick_size + "\n");
        out.write("Grain size: " + grain_size + "\n");
        out.write("Acceptable waste: " + acceptable_waste + "\n");
        out.write("Tail pointer in memory: " + tail_ptr + "\n");
        out.write("First allocatable byte: " + start() + "\n\n");
        out.write("Free lists from memory structures\n");
        for (qlist_num = 0; qlist_num <= last_ql_index;
            qlist_num++) {
            out.write(qlist_num + ": ");
            out.write("Length = " +
                      ql_heads[qlist_num].length + "; ");
            print_memory_freelist(out, ql_heads[qlist_num].first_block);
        };
        out.write("Nonempty free lists: " + nonempty_lists + "\n");
        out.write("Tail pointer in memory: " + tail_ptr + "\n");
        out.write("First allocatable byte: " + start() + "\n\n");
    }

    /* Calculate the list index of free list which can satisfy a request for a
     * block size  */
    private int calculate_list_index_for_alloc(int size)
    {
        //	return ceiling((size -  first_quick_size_block)/grain_size);
        int junk = 0;
        if ((size -  first_quick_size_block) % grain_size > 0)
            junk = 1;
        return junk + (int) ((size -  first_quick_size_block)/grain_size);
    }

    /* Calculate the list index of free list for storing a block of a
     * particular size. */
    private int calculate_list_index_for_dealloc(int size)
    {
        //	return floor((size -  first_quick_size_block)/grain_size);
        return(int) ((size -  first_quick_size_block)/grain_size);
    }

    private int abs(int size)
    {
        if (size > 0)
            return size;
        else
            return(- size);
    }

    /* Calculate block size stored on a particular quick list */
    private int calculate_block_size(int ql_index)
    {
        return first_quick_size_block + (ql_index * grain_size);
    }

    /* Allocate a block from the tail. */
    private long allocate_from_tail(int block_size)
    throws IOException
    {
        long block_to_alloc;

        block_to_alloc = tail_ptr;
        tail_ptr = tail_ptr + block_size;

        allocated_blocks++;
        allocated_words += block_size;

        seek_and_count(block_to_alloc);
        writeInt(- block_size);
        seek_and_count(TAIL_DPTR);
        writeLong(tail_ptr);

        return(block_to_alloc + HDR_SIZE);
    }

    /* Try to allocate a block from a quick list.  If the quick list contains
       insufficient information, allocate space from the tail. */
    private long allocate_from_ql(int request_size)
    throws IOException
    {
        Block block1;
        int block_size;
        int list_index;

        small_requests++;
        list_index = calculate_list_index_for_alloc(request_size);
        if (ql_heads[list_index].length > 0 ) {
            // quick list is nonempty
            block1 = ql_heads[list_index].first_block;
            ql_heads[list_index].length--;
            if (ql_heads[list_index].length == 0) {
                nonempty_lists--;
            }
            ql_heads[list_index].first_block = block1.next;

            allocated_blocks++;
            free_blocks--;
            ql_hits++;
            allocated_words += block1.size;
            free_words -= block1.size;

            // do the following as late as possible
            seek_and_count(block1.address);
            writeInt(- block1.size);

            return(block1.address + HDR_SIZE);
        } else {
            // allocate from tail
            block_size = calculate_block_size(list_index);
            return allocate_from_tail(block_size);

        }
    }

    /* Split a block on disk and return the appropriate address.  We assume
     * that this is only called to split a block on the misc list. */
    private long split_block(long block_addr, int request_size, int rem)
    throws IOException
    {
        allocated_words += request_size;
        allocated_blocks++;
        free_words -= request_size;
        ml_hits++;
        ml_splits++;
        seek_and_count(block_addr + request_size);
        writeInt(rem);
        seek_and_count(block_addr);
        writeInt(- request_size);
        return(block_addr + HDR_SIZE);
    }

    /* Search the misc list to satisfy a request.  In order
       to prevent too much fragmentation, we use a combination of first fit
       and best fit.  We locate the first block which can satisfy the request
       without splitting.  If this fails, we do a best fit search. */
    private long search_ml(int request_size)
    throws IOException
    {
        Block best_candidate = null;
        Block best_candidate_pred = null;
        Block block = ql_heads[last_ql_index].first_block;
        long block_addr;
        boolean found = false;
        Block prev_block = null;
        int ql_index;
        int ql_size;
        int rem;
        int rem2;

        large_requests++;
        while (block != null) {
            ml_blocks_searched++;
            if (block.size >= request_size) {
                rem = block.size - request_size;
                if (rem <= acceptable_waste) {
                    // allocate the block without splitting
                    if (prev_block != null) {
                        prev_block.next = block.next;
                    } else {
                        ql_heads[last_ql_index].first_block = block.next;
                    }
                    ql_heads[last_ql_index].length--;
                    if (ql_heads[last_ql_index].length == 0) {
                        nonempty_lists--;
                    }

                    allocated_blocks++;
                    free_blocks--;
                    ml_hits++;
                    allocated_words += block.size;
                    free_words -= block.size;

                    seek_and_count(block.address);
                    writeInt(- block.size);

                    return(block.address + HDR_SIZE);
                } else {
                    if (best_candidate == null) {
                        best_candidate = block;
                        best_candidate_pred = prev_block;
                    } else {
                        if (best_candidate.size >= block.size) {
                            best_candidate = block;
                            best_candidate_pred = prev_block;
                        }
                    }
                }
            }
            prev_block = block;
            block = block.next;
        }
        if (best_candidate == null) {
            // have to allocate from tail
            return allocate_from_tail(request_size);
        } else {
            // we have to split a block
            rem = best_candidate.size - request_size;
            block_addr = best_candidate.address;
            if (rem <= last_quick_size_block) {
                rem2 = rem % grain_size;
                rem -= rem2;
                ql_index = calculate_list_index_for_dealloc(rem);
                request_size += rem2;
                if (best_candidate_pred != null) {
                    best_candidate_pred.next = best_candidate.next;
                } else {
                    ql_heads[last_ql_index].first_block = best_candidate.next;
                }
                add_block_to_freelist(best_candidate, ql_index);
            }
            best_candidate.size = rem;
            best_candidate.address += request_size;
            return split_block(block_addr, request_size, rem);
        }

    }

    /************************************************************************** 
     * start - Return the first allocatable address 
     * @return The first allocatable address in the file.
     **************************************************************************/
    public long start()
    {
        return FIRST_ALLOCATABLE_BYTE;
    };

    public int grain_size()
    {
        return grain_size;
    }

    /**************************************************************************
     * Allocate a block of storage and insure it is cleared to 0s
     *
     * @param  size   Number of bytes to allocate.
     * 
     * @return address in file that is allocated.
     *
     * @exception FileManagerException
     * @exception IOException
     * @exception EOFException
     *************************************************************************/     
    public long allocateAndClear(int size)
    throws FileManagerException, 
    IOException, 
    EOFException
    {
        //System.out.println("*** allocateAndClear filename=" + filename + " size=" + size);
        long ret = allocate(size);
        PrimitiveArrayPool.PoolEntry bytePoolEntry = this.htoddc.byteArrayPool.allocate(size);
        byte[] clear = (byte[])bytePoolEntry.getArray();
        //byte[] clear = new byte[size];
        seek(ret);
        write(clear);
        this.htoddc.byteArrayPool.returnToPool(bytePoolEntry);
        //clear = null;
        return ret;
    }

    /**************************************************************************
     * Allocate a block of storage.  It may or may not be cleared to 0.
     *
     * @param  size   Number of bytes to allocate.
     * 
     * @return address in file that is allocated.
     *
     * @exception FileManagerException
     * @exception IOException
     * @exception EOFException
     *************************************************************************/     
    public long allocate(int request_size)
    throws IOException
    {
        //System.out.println("*** allocate filename=" + filename + " size=" + request_size);
        if (readOnly) {
            throw (new FileManagerException("Attempt to allocate in read only mode"));
        }
        allocs++;
        request_size = request_size + HDR_SIZE;
        if (request_size <= last_quick_size_block) {
            return allocate_from_ql(request_size);
        } else {
            // round up to nearest multiple of a grain_size
            request_size = (( request_size + grain_size - 1 ) / grain_size) * grain_size;
            return search_ml(request_size);
        }
    }

    /* Get the block size stored on disk for a block */
    private int get_block_size(long block)
    throws IOException
    {
        seek_and_count(block);
        return readInt();
    }

    /* Add a new block to the beginning of a free list data structure */
    private void add_block_to_freelist(Block block, int ql_index)
    {
        block.next = ql_heads[ql_index].first_block;
        ql_heads[ql_index].first_block = block;
        ql_heads[ql_index].length++;
        if (ql_heads[ql_index].length == 1) {
            nonempty_lists++;
        }
    }

    /* Add a new block to the beginning of a free list data structure, 
       increment counters */
    private void add_new_block_to_freelist(long addr, int size, int ql_index)
    {
        Block block = new Block();

        block.address = addr;
        block.size = size;
        add_block_to_freelist(block, ql_index);
        free_blocks++;
        free_words += size;
    }

    /* Add a new block to a free list. */
    private void add_to_freelist(long addr, int size)
    {
        int list_index;

        if (size <= last_quick_size_block) {
            list_index = calculate_list_index_for_dealloc(size);
        } else {
            list_index = last_ql_index;
        }
        add_new_block_to_freelist(addr, size, list_index);
    }


    /****************************************************************
     * Main deallocation routine exposed in public interface.  This routine
     *  performs a disk read to read the size and then a disk write to mark
     *  the size tag allocated.  An optimization to avoid doing the read would
     *  be to store the size of all allocated blocks in a hash table. 
     * 
     * @param  block  The address of the block to deallocate.
     *
     * @exception IOException
     * @exception FileManagerException
     *************************************************************************/
    public void deallocate(long block)
    throws IOException
    {
        int size;

        if (readOnly) {
            throw (new FileManagerException("Attempt to deallocate in read only mode"));
        }
        block = block - HDR_SIZE;
        if (block < FIRST_TAIL) {
            throw (new FileManagerException("Illegal block address passed to deallocate"));
        }
        size = get_block_size(block);
        if (size >= 0) {
            throw (new FileManagerException("Attempt to deallocate block with illegal size"));
        }
        size = abs(size);
        deallocs++;
        seek_and_count(block);
        writeInt(size);
        add_to_freelist(block, size);
        allocated_blocks--;
        allocated_words -= size;
    }


    /**************************************************************************
     * Main coalescing routine exposed in public interface.  This forces a
     * coalesce of adjacent free blocks.
     *
     * @exception IOException
     * @exception FileManagerException
     *************************************************************************/
    public void coalesce()
    throws IOException
    {
        if (readOnly) {
            throw (new FileManagerException("Attempt to coalesce in read only mode"));
        }
        free_blocks = 0;
        free_words = 0;
        allocated_blocks = 0;
        allocated_words = 0;
        init_freelists(false);
        read_block_sizes_from_disk(true);
        coalesces++;
    }

    public void update_read_time(long time)
    {
        read_time += time;
    }

    public void update_write_time(long time)
    {
        write_time += time;
    }

    public void increment_read_count()
    {
        read_count++;
    }

    public void increment_write_count()
    {
        write_count++;
    }


    private void run_tests()
    throws IOException
    {
        long block1 = allocate(900);
        long block2 = allocate(900);
        long block3 = allocate(900);
        long block4 = allocate(1900);
        long block5 = allocate(2900);
        long block6 = allocate(900);
        long block7 = allocate(7900);
        deallocate(block1);
        deallocate(block2);
        deallocate(block3);
        deallocate(block4);
        deallocate(block5);
        deallocate(block6);
        deallocate(block7);

        cache_free_storage_info();
    }

    /**************************************************************************
     * Debugging and testing interface
     *************************************************************************/
    public static void main(String args[])
    throws FileManagerException, IOException
    {
        HTODDynacache hdc = new HTODDynacache();
        FileManagerImpl mem_manager = new FileManagerImpl("foo1",   // filename
                                                          false,    // coalsece
                                                          "rw",     // mode
                                                          MULTIVOLUME,  // type
                                                          hdc);
        mem_manager.run_tests();

        OutputStreamWriter out = new OutputStreamWriter(System.out);
        mem_manager.dump_disk_memory(out);
        mem_manager.dump_memory(out);
        mem_manager.dump_stats(out, false);
        mem_manager.cache_free_storage_info();
        mem_manager.dump_disk_memory(out);
        out.flush();
        out.close();
    }

    class Block {
        public long address;
        public int size;
        public Block next;
    };

    class Listhead {
        public int length;
        public Block first_block;
    };

}



