/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
//Class: RLSAccessFile
//------------------------------------------------------------------------------
/**
* RLSAccessFile class provides an extension to RandomAccessFile to share the same file
* descriptor between "multiple" usages.   The CoordinationLock and LogFileHandle usage
* of RandomAccessFile is independent and used to use two separate file desciptors.
* However on Windows and i-Series this causes problems with file access and locks
* which is alleviated by using the same file descriptor.  A further problem with two
* file desciptors is that some unix specifications state that the lock will get
* released on the first close issued, but we need the lock to be freed on the final
* close.  This will be the CoordinationLock.  This is ok for peer recovery as the
* CoordinationLock is released, but for local recovery/shutdown it is not, so the
* locks and file closes will not happen until the JVM exits.   At some point, the
* CoordinationLock class should be integrated into the shutdown logic - but it is
* handled by the RLS and the tran service still accesses the logs after the RLS has
* terminated.
*
*/
public class RLSAccessFile extends RandomAccessFile 
{

    /**
     * WebSphere RAS TraceComponent registration
     */
    private static final TraceComponent tc = Tr.register(RLSAccessFile.class, TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * Map of open RandomAccessFiles
     */
    private static HashMap _accessFiles = new HashMap();

    /**
     * Open use count of the file
     */
    private int _useCount;

    /**
     * The file
     */
    private File _file;

    //
    // Usage of this class:
    //
    // To share a RandomAccessFile multiple times, use the getRLSAccessFile call.  This will check
    // in the HashMap to see if the file is already open.   Note: it is assumed the caller has already
    // ensured that the File passed in will match appropriate Files in the table.  A simple open
    // use count is kept and this is decremented on each close.  Once the use count is at zero,
    // the File is actually closed.
    //

    RLSAccessFile(File file, String mode) throws FileNotFoundException
    {
        super(file, mode);
        if (tc.isEntryEnabled()) Tr.entry(tc, "RLSAccessFile", new Object[]{file, mode});
        _useCount = 1;
        _file = file;
        if (tc.isEntryEnabled()) Tr.exit(tc, "RLSAccessFile", this);
    }


    // The logic in close needs to account for recursive calls. See notes below.

    public void close() throws IOException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "close", new Object[]{this, _file});
       
    // By locking on the class rather than the object, and removing
    // the inner lock on the class, this seems to resolve the problems
    // reported in d347231 that file handles were not being released properly
    //    synchronized(this) 
		synchronized(RLSAccessFile.class)
        { 
            _useCount--;
            if (tc.isDebugEnabled()) Tr.debug(tc, "remaining file use count", new Integer(_useCount));

            // Check for 0 usage and close the actual file.  
            // One needs to be aware that close() can be called both directly by the user on
            // file.close(), and also indirectly by the JVM.  The JVM will call close() on a
            // filechannel.close() and also recursively call filechanel.close() and hence close()
            // on a file.close().   For this reason, filechannel.close() has been removed from
            // LogFileHandle calls - but it is still in CoordinationLock just in case the behaviour
            // of the JVM changes wrt lock releases.   This behaviour can make interesting trace
            // reading because one can get the use count dropping to -1 either because of two
            // serial calls (filechannel.close() + file.close()) or two recursive calls (ie
            // file.close() calling filechannel.close() calling file.close()).

            // Trace an innocious exception to help debug any recursion problems.
            if (tc.isDebugEnabled() && (_useCount <= 0))
            {
                Tr.debug(tc, "call stack", new Exception("Dummy traceback"));
            }
            if (_useCount == 0)
            {
                 super.close();
       // Outer lock is now on class, so no need to lock here (d347231)
       //          synchronized(RLSAccessFile.class)  
       //          { 
                     _accessFiles.remove(_file);
       //          }
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "close");
    }

    static synchronized RLSAccessFile getRLSAccessFile(File file) throws FileNotFoundException
    {

        if (tc.isEntryEnabled()) Tr.entry(tc, "getRLSAccessFile", file);

        // Note we only want to return a single fd per file name to enable locking to work with
        // a non-mapped file.  We always open each file with the same mode, ie "rw".  Assume the
        // caller passes in a suitable File reference which will match for equality as required.
        // An alternative is to use canonical names if this needs to be more generalized.
        RLSAccessFile raf = (RLSAccessFile)_accessFiles.get(file);
        if (raf == null)
        {
            raf = new RLSAccessFile(file, "rw");
            _accessFiles.put(file, raf);
        }
        else
        {
            raf._useCount++;
            if (tc.isDebugEnabled()) Tr.debug(tc, "total file use count", new Integer(raf._useCount));
        }
        if (tc.isEntryEnabled()) Tr.exit(tc, "getRLSAccessFile", raf);
        return raf;
    }
}
