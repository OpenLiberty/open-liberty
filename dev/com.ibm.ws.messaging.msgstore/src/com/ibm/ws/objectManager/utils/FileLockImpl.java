package com.ibm.ws.objectManager.utils;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * <p>Encapsulate the locking of the log file.
 * 
 * @author IBM Corporation
 */
public class FileLockImpl
                extends FileLock {
    private static final Class cclass = FileLockImpl.class;
    private static Trace trace = Utils.traceFactory.getTrace(cclass,
                                                             UtilsConstants.MSG_GROUP_UTILS);

    private java.nio.channels.FileChannel fileChannel;
    private java.nio.channels.FileLock fileLock = null;

    /**
     * Make a file lock using NIO.
     * 
     * @param file the open file.
     * @param fileName of the file.
     * @throws java.io.IOException
     */
    public FileLockImpl(java.io.RandomAccessFile file,
                        String fileName)
        throws java.io.IOException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { file,
                                      fileName });

        // Make sure no one else can write to the log.
        // Obtain an exclusive lock on the fileChannel.
        fileChannel = file.getChannel();
        fileLock = fileChannel.tryLock();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // FileLockImpl();

    /**
     * Test the lock.
     * 
     * @return true if the lock is held, otherwise false.
     */
    public boolean tryLock()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "tryLock"
                            );

        boolean isLocked = true;
        if (fileLock == null) // Did we get the lock?
            isLocked = false;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "tryLock"
                       , "returns isLocked=" + isLocked + "(bloolean)"
                            );
        return isLocked;
    } // tryLock().

    /**
     * Unlock the file.
     * 
     * @throws java.io.IOException
     */
    public final void release()
                    throws java.io.IOException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "release");

        // Give up the lock.
        if (fileLock != null)
            fileLock.release();
        fileLock = null;

        if (fileChannel != null)
            fileChannel.close();
        fileChannel = null;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "release");
    } // release().
} // FileLock class.
