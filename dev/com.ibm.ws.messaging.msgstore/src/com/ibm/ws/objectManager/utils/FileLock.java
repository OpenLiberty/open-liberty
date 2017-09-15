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
 * <p>Encapsulate the locking of a file.
 * 
 * @author IBM Corporation
 */
public abstract class FileLock
{
    static final Class cclass = FileLock.class;
    static Trace trace = Utils.traceFactory.getTrace(cclass,
                                                     UtilsConstants.MSG_GROUP_UTILS);

    /**
     * Create a platform specific FileLock instance.
     * 
     * @param file to be locked. The file must be already open.
     * @param fileName of the file.
     * @return FileLock for the file.
     * @throws java.io.IOException
     */
    public static FileLock getFileLock(java.io.RandomAccessFile file, String fileName) throws java.io.IOException
    {
        return (FileLock) Utils.getImpl("com.ibm.ws.objectManager.utils.FileLockImpl",
                                        new Class[] { java.io.RandomAccessFile.class, String.class },
                                        new Object[] { file, fileName });
    } // getFileLock().

    /**
     * Test the lock.
     * 
     * @return true if the lock is held, otherwise false.
     */
    public abstract boolean tryLock();

    /**
     * Unlock the file.
     * 
     * @throws java.io.IOException
     */
    public abstract void release() throws java.io.IOException;
}
