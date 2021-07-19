/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing.internal;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.core.utils.DirectBufferHelper;
import com.ibm.ws.zos.core.utils.DoubleGutter;

/**
 * Helper class for instrospecting the native control blocks associated with
 * a localcomm connection.
 */
public class IntrospectHelper {
    
    /**
     * Reference to the local channel provider service.
     */
    private LocalChannelProviderImpl localChannelProvider;
    
    /**
     * The LSCL/LOCL is shared across multiple conn handles. 
     * These sets are populated as the LSCL/LOCLs get dumped to ensure
     * we dump each of them only once.
     */ 
    Set<Long> formattedLSCLs = new HashSet<Long>();
    Set<Long> formattedLOCLs = new HashSet<Long>();
    
    /**
     * CTOR.
     * 
     * Takes a LocalChannelProviderImpl, for access to other service components
     * like DirectBufferHelper and DoubleGutter.
     */
    public IntrospectHelper(LocalChannelProviderImpl localChannelProvider) {
        this.localChannelProvider = localChannelProvider;
    }
    
    /**
     * @return DoubleGutter via LocalChannelProviderImpl.
     */
    private DoubleGutter getDoubleGutter() {
        return localChannelProvider.getDoubleGutter();
    }
    
    /**
     * @return DirectBufferHelper via LocalChannelProviderImpl.
     */
    private DirectBufferHelper getDirectBufferHelper() {
        return localChannelProvider.getDirectBufferHelper();
    }
    
    /**
     * 
     * NOTE: This method should be called only after safely obtaining access to the
     * client's shared memory area via the SharedMemoryAttachmentManager.
     * 
     * @return A list of Stringified native control block data (double guttered).
     */
    protected List<String> dumpNativeControlBlocks(LocalCommClientConnHandle clientConnHandle) {
        List<String> retMe = new ArrayList<String>();
        
        retMe.add("LocalCommClientConnHandle: " + clientConnHandle.toString());
              
        LocalCommConnectionHandle lhdl = new LocalCommConnectionHandle(clientConnHandle.getLhdlPtr(), 
                                                                       getDirectBufferHelper());
        
        retMe.add("");  // Formatting.
        retMe.addAll(dumpLhdl(lhdl));
        retMe.add("");  // Formatting.
        retMe.addAll(dumpLscl(lhdl.getLsclPtr()));
        
        return retMe;
    }
    
    /**
     *  @return A list of Stringified native control block data (double guttered).
     */
    private List<String> dumpLhdl(LocalCommConnectionHandle lhdl) {
        List<String> retMe = new ArrayList<String>();
        
        retMe.add("LocalCommConnectionHandle (BBGZLHDL):");
        retMe.add(lhdl.dumpDoubleGutter(getDoubleGutter()));
        retMe.add("");  // Formatting.

        // Dump handle's Local Comm Footprint Table
        FootprintTable footprintTable = FootprintTable.getFootprintTable( lhdl.getFootprintTablePtr(), getDirectBufferHelper() );
        retMe.add("LocalCommConnectionHandle Footprint Table (LHDL--BBGZLFTB):");
        retMe.add(footprintTable.dumpDoubleGutter(getDoubleGutter()));
        
        return retMe;
    }
    
    /**
     *  @return A list of Stringified native control block data (double guttered).
     */
    private List<String> dumpLscl(long lsclPointer) {

        List<String> retMe = new ArrayList<String>();
        
        if (formattedLSCLs.add(lsclPointer)) {
            // Haven't formatted this LSCL yet
            LocalCommClientServerPair lscl = new LocalCommClientServerPair( lsclPointer, getDirectBufferHelper() );
            retMe.add("LocalCommClientServerPair (BBGZLSCL):");
            retMe.add(lscl.dumpDoubleGutter(getDoubleGutter()));
            retMe.add("");  // Formatting.
            
            LocalCommClientDataStore ldat = new LocalCommClientDataStore( lscl.getLdatPtr(), getDirectBufferHelper() );
            retMe.add("LocalCommClientDataStore (BBGZLDAT):");
            retMe.add(ldat.dumpDoubleGutter(getDoubleGutter()));
            retMe.add("");  // Formatting.
            
            // Get BBGZLOCL (server) from pointer in LSCL
            Long serverLoclPointer = lscl.getServerLoclPtr(); 
            if (formattedLOCLs.add(serverLoclPointer)) {
                // Haven't formatted this LOCL yet.
                LocalCommClientAnchor locl = new LocalCommClientAnchor(serverLoclPointer, getDirectBufferHelper());
                retMe.add("Server's LocalCommClientAnchor (BBGZLOCL):");
                retMe.add(locl.dumpDoubleGutter(getDoubleGutter())); 
                retMe.add("");  // Formatting.

                // Format Server LOCL local comm footprint table
                FootprintTable footprintTable = FootprintTable.getFootprintTable( locl.getFootprintTablePtr(), getDirectBufferHelper() );
                retMe.add("Server's LocalCommClientAnchor Footprint Table (LOCL--BBGZLFTB):");
                retMe.add(footprintTable.dumpDoubleGutter(getDoubleGutter()));
                retMe.add("");  // Formatting.
            }

            // Get BBGZLOCL (client) from pointer in LSCL
            Long clientLoclPointer = lscl.getClientLoclPtr(); 
            if (formattedLOCLs.add(clientLoclPointer)) {
                // Haven't formatted this LOCL yet.
                LocalCommClientAnchor locl = new LocalCommClientAnchor(clientLoclPointer, getDirectBufferHelper());
                retMe.add("Client's LocalCommClientAnchor (BBGZLOCL):");
                retMe.add(locl.dumpDoubleGutter(getDoubleGutter())); 
                retMe.add("");  // Formatting.
                
                // Format Client LOCL local comm footprint table
                FootprintTable footprintTable = FootprintTable.getFootprintTable( locl.getFootprintTablePtr(), getDirectBufferHelper() );
                retMe.add("Client's LocalCommClientAnchor Footprint Table (LOCL--BBGZLFTB):");
                retMe.add(footprintTable.dumpDoubleGutter(getDoubleGutter()));
            }
        }
        
        return retMe;
    }

}


/**
 * Parent class to a bunch of Java classes that map native control blocks.
 * 
 * A native control block consists of a nativeAddress and a DirectByteBuffer
 * that maps over that address.
 * 
 * Subclasses define the size of the control block.
 */
class NativeControlBlock {
    
    /**
     * The native address of this control block.
     */
    protected long nativeAddress;
    
    /**
     * The raw data for this control block.
     * This is typically a read-only DirectByteBuffer wrapped around the native storage.
     */
    protected ByteBuffer rawData;
    
    /**
     * CTOR.
     * 
     * @param nativeAddress - the native address of the control block.
     * @param size - the size of the control block
     * @param directBufferHelper - for creating a DirectByteBuffer on the native storage
     */
    public NativeControlBlock(long nativeAddress, int size, DirectBufferHelper directBufferHelper) {
        this.nativeAddress = nativeAddress;
        this.rawData = directBufferHelper.getSlice(nativeAddress, size); 
    }
    
    /**
     * @return The control block's rawData dumped in double-gutter format.
     */
    public String dumpDoubleGutter(DoubleGutter doubleGutter) {
        return doubleGutter.asDoubleGutter(nativeAddress, rawData);
    }
}


/**
 * Java mapping of the LocalCommConnectionHandle native control block (LHDL).
 * 
 * The LocalCommClientConnHandle points to the LocalCommConnectionHandle.
 * There's 1 LocalCommConnectionHandle per connection (note that a single 
 * client may have multiple connections).
 * 
 * The LocalCommConnectionHandle points to the LSCL (client-server pair).
 * 
 * !!! NOTE: must be kept in sync with com.ibm.zos.native/include/server_local_comm_client.h
 */
class LocalCommConnectionHandle extends NativeControlBlock {
    
    /** 
     * Field offsets...
     */
    public static final int HANDLE_length_OFFSET         = 0x01A;  // handle OFFSET to localCommConnectionHandle.length 
    public static final int HANDLE_bbgzlscl_p_OFFSET     = 0x038;  // handle OFFSET to localCommConnectionHandle.bbgzlscl_p
    public static final int HANDLE_footprintTable_OFFSET = 0x140;  // handle OFFSET to localCommConnectionHandle.footprintTable
    
    /**
     * Total size of the handle.
     */
    public static final int Size = 0x160;
    
    /**
     * CTOR.
     */
    public LocalCommConnectionHandle(long lhdlPointer, DirectBufferHelper directBufferHelper) {
        super(lhdlPointer, Size, directBufferHelper); 
    }

    /**
     * @return the footprintTable field value.
     */
    public long getFootprintTablePtr() {
        return rawData.getLong(HANDLE_footprintTable_OFFSET);
    }

    /**
     * @return the bbgzlscl_p field value.
     */
    public long getLsclPtr() {
        return rawData.getLong(HANDLE_bbgzlscl_p_OFFSET);
    }
}

/**
 * Java mapping for the LocalCommClientServerPair native control block (LSCL).
 * 
 * !!! NOTE: must be kept in sync with com.ibm.zos.native/include/server_local_comm_client.h
 */
class LocalCommClientServerPair extends NativeControlBlock {
    
    /**
     * Field offsets...
     */
    public static final int LSCL_length_OFFSET           = 0x00A;  // LocalCommClientServerPair_t.length offset
    public static final int LSCL_firstDataStore_p_OFFSET = 0x018;  // LocalCommClientServerPair_t.firstDataStore_p offset (LDAT pointer)
    public static final int LSCL_localCommClientControlBlock_p_OFFSET = 0x040; // LocalCommClientServerPair_t.localCommClientControlBlock_p offset
    public static final int LSCL_serverLOCL_p_OFFSET     = 0x050;  // LocalCommClientServerPair_t.serverLOCL_p offset
    
    /**
     * Total size of the LSCL control block.
     */
    public static final int Size =  0x200; 
    
    /**
     * CTOR.
     */
    public LocalCommClientServerPair(Long lsclPointer, DirectBufferHelper directBufferHelper) {
        super(lsclPointer, Size, directBufferHelper);
    }

    /**
     * @return the localCommClientControlBlock_p field value.
     */
    public long getClientLoclPtr() {
        return rawData.getLong(LSCL_localCommClientControlBlock_p_OFFSET);
    }

    /**
     * @return the serverLocl_p field value.
     */
    public long getServerLoclPtr() {
        return rawData.getLong(LSCL_serverLOCL_p_OFFSET);
    }

    /**
     * @return the firstDataSTore_p field value.
     */
    public long getLdatPtr() {
        return rawData.getLong(LSCL_firstDataStore_p_OFFSET);
    }
}

/**
 * Java mapping for the LocalCommCientAnchor (LOCL).  
 * 
 * There's 1 LOCL for each localcomm address space; the server has a LOCL,
 * as does every client that connects to the server.
 * 
 * The LSCL (a client-server pair) points to both the client's LOCL and
 * the server's LOCL.
 * 
 * !!! NOTE: this must be kept in sync with com.ibm.zos.native/include/server_local_comm_client.h
 */
class LocalCommClientAnchor extends NativeControlBlock {
    
    /**
     * Field offsets...
     */
    public static final int LOCL_length_OFFSET           = 0x00A;  // localCommClientAnchor.length offset
    public static final int LOCL_footprintTable_OFFSET   = 0x140;  // localCommClientAnchor.footprintTable offset

    /**
     * Size of the LOCL control block.
     */
    public static final int Size = 0x200;
    
    /**
     * CTOR.
     */
    public LocalCommClientAnchor(long loclPointer, DirectBufferHelper directBufferHelper) {
        super(loclPointer, Size, directBufferHelper);
    }

    /**
     * @return the footprintTable field value
     */
    public long getFootprintTablePtr() {
        return rawData.getLong(LOCL_footprintTable_OFFSET);
    }
}

/**
 * Java mapping of the LocalCommClientDataStore native control block (LDAT).
 * 
 * The LSCL (client-server pair) points to the LDAT.
 * 
 * !!! NOTE: this must be kept in sync with com.ibm.zos.native/include/server_local_comm_data_store.h
 *
 */
class LocalCommClientDataStore extends NativeControlBlock {
    
    /**
     * Field offsets...
     */
    public static final int LDAT_length_OFFSET           = 0x00A;  // localCommClientDataStore.length offset
    
    /**
     * Size of the LDAT control block.
     */
    public static final int Size = 0x200;
    
    /**
     * CTOR.
     */
    public LocalCommClientDataStore(long ldatPointer, DirectBufferHelper directBufferHelper) {
        super(ldatPointer, Size, directBufferHelper);
    }
}

/**
 * Java mapping of the CF_FootprintTable.
 * 
 * !!! NOTE: this must be kept in sync with com.ibm.zos.native/include/server_local_comm_footprint.h
 */
class FootprintTable extends NativeControlBlock {
    
    /**
     * Field offsets...
     */
    final int LFTB_m_availableEntry_OFFSET = 0x008;  // CF_FootprintTable.m_availableEntry offset
    final int LFTB_m_totalEntries_OFFSET   = 0x00C;  // CF_FootprintTable.m_totalEntries
    final int LFTB_m_flags_OFFSET          = 0x010;  // CF_FootprintTable.m_flags
    final int LFTB_m_tableWrapped          = 0x80000000; // m_flags.m_tableWrapped
    final int LFTP_m_footprintEntries_OFFSET = 0x020;
    
    /**
     * The size of the footprint table header.
     * Note: does not include entries in the table.  Entries immediately
     * follow the header in storage.
     */
    public static final int HeaderSize = 0x20;
    
    /**
     * The size of a footprint entry.
     */
    public static final int CF_FootprintEntry_size = 0x030; 
    
    /**
     * @return a FootprintTable object mapping the given footprintTablePtr storage.
     */
    public static FootprintTable getFootprintTable( long footprintTablePtr, DirectBufferHelper directBufferHelper ) {
        
        // Map the header first to determine the actual size of the footprint table.
        FootprintTable header = new FootprintTable(footprintTablePtr, directBufferHelper );
        
        return new FootprintTable( footprintTablePtr, header.getFullTableSize(), directBufferHelper );
    }
    
    /**
     * CTOR.
     * 
     * Will only format the footprint table header (no entries).
     */
    public FootprintTable(long footprintTablePtr, DirectBufferHelper directBufferHelper) {
        super(footprintTablePtr, HeaderSize, directBufferHelper);
    }
    
    /**
     * CTOR.
     * 
     * @param size - The total size of the footprint table, including entries.
     */
    public FootprintTable(long footprintTablePtr, int size, DirectBufferHelper directBufferHelper) {
        super(footprintTablePtr, size, directBufferHelper);
    }        
    
    /**
     * @return true if the table has wrapped (i.e. has used all available entries).
     */
    public boolean hasWrapped() {
        return (rawData.getInt(LFTB_m_flags_OFFSET) & LFTB_m_tableWrapped) != 0 ;
    }
    
    /**
     * @return the storage size of all used footprint entries.
     */
    public int getSizeOfAllUsedEntries() {
        if (hasWrapped()) {
            return CF_FootprintEntry_size * rawData.getInt(LFTB_m_totalEntries_OFFSET);
        } else {
            return CF_FootprintEntry_size * rawData.getInt(LFTB_m_availableEntry_OFFSET);
        }
    }
    
    /**
     * @return the storage size of the entire footprint table, including used entries
     *         but not including unused entries.
     */
    public int getFullTableSize() {
        return HeaderSize + getSizeOfAllUsedEntries();
    }
}
