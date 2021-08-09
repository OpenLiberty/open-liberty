/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.persistent.filemgr;

//
// Common location for static constants.
//
public interface Constants 
{

    final static int INT_SIZE = 4;
    final static int LONG_SIZE = 8;
    final static int PTR_SIZE = 8;

    final static long MAGIC = 44534132;                 // ws magic 44534132 set 6/4/04 jrc-

    final static long STARTOFDATA_LOCATION = 0;
    final static long TAIL_DPTR = STARTOFDATA_LOCATION + LONG_SIZE;
    final static long CACHE_END = TAIL_DPTR + PTR_SIZE;
    final static long DISK_NULL = 0;
    final static long FIRST_QSIZE_PTR = CACHE_END + PTR_SIZE;
    final static long LAST_QSIZE_PTR = FIRST_QSIZE_PTR + INT_SIZE;
    final static long GRAIN_SIZE_PTR = LAST_QSIZE_PTR + INT_SIZE;
    final static long ACC_WASTE_PTR = GRAIN_SIZE_PTR + INT_SIZE;

    //
    // We reserve 16 DWs for future use between ACC_WASTE and FIRST_TAIL.
    // Please use these and the magic string appropriately to avoid
    // the need for future conversion of files.
    //
    final static int NUM_RESERVED_WORDS = 16;
    final static int RESERVED_WORDS_SIZE = NUM_RESERVED_WORDS * LONG_SIZE;
    final static long RESERVED_WORDS_START = ACC_WASTE_PTR + INT_SIZE;
    final static long RESERVED_WORDS_END = RESERVED_WORDS_START + RESERVED_WORDS_SIZE;

    //
    // Now we reserve space for user data.  There are NUM_USER_WORDS areas
    // reserved.  Each area is two DW.  The first DW is a serial number
    // chosen by the user, the second is any data chosen by the user. See
    // the comments for storeUserData for more information.
    //
    final static int NUM_USER_WORDS = 128;
    final static int USER_AREA_SIZE = NUM_USER_WORDS * 2 * LONG_SIZE;
    final static long USER_AREA_START = RESERVED_WORDS_END;
    final static long USER_AREA_END = USER_AREA_START + USER_AREA_SIZE;

    //
    // Volume group constants
    //
    static int CACHE_PAGESIZE = 16*1024;                // note no caching in this version jrc
    static int MAX_FILESIZE = 1024*1024*1024;

    //
    // FIRST_TAIL is the first allocatable byte in the file.
    // Force alignment on a cache line.
    //
    static long FIRST_TAIL =         
        ((USER_AREA_END + CACHE_PAGESIZE - 1) / CACHE_PAGESIZE) * CACHE_PAGESIZE;

    final static int HDR_SIZE = INT_SIZE;
    final static long FIRST_ALLOCATABLE_BYTE = FIRST_TAIL + HDR_SIZE;

    //
    // Cache Policies - not used in this version. jrc 6/4/02
    //
    static final int LAZY = 0;
    static final int IMMEDIATE = 1;

    //
    // Physical storage manager type
    //
    static final int ORIGINAL = 0;
    static final int MULTIVOLUME_PAGED = 1;
    static final int MULTIVOLUME = 2;                  // this is only supported version jrc 6/4/02
    
}










