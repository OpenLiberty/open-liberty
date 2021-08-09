/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <p>A handle to a zip file.</p>
 */
public interface ZipFileHandle {
    /**
     * <p>Attempt to obtain a zip file for the zip file handle.</p>
     * 
     * <p>Exactly one call to {@link #close} is required for each open call which
     * is made.  Zip file are held in cache as long as any open is active.</p>
     * 
     * <p>Calls which are made while the zip file handle has any opens will
     * answer identical zip files.</p>
     * 
     * <p>A call to open which is made after all active opens are closed may
     * generate the same zip file as was obtained by an early call to open.
     * Calls to {@link #close} do not immediately cause the zip file to be
     * closed.</p>
     * 
     * @return A zip file for the zip file handle.
     *  
     * @throws IOException If the zip file could not be obtained for the zip file
     *     handle.
     */
    ZipFile open() throws IOException;

    /**
     * <p>Close the zip file handle.</p>
     * 
     * <p>Zip file handles keep an open count.  A call to close will have no
     * effect, other than to reduce the open count, unless the call reduces
     * the open count to zero (0).</p>
     * 
     * <p>The zip file is not immediately closed when the open count reaches
     * zero.  The zip file is held open for a configured period of time.  Any
     * calls to open the zip file handle will obtain the same zip file if the
     * prior close has not reached the configured time.  (The close may be
     * performed earlier if the zip file cache grows beyond a configured size.)</p>
     * 
     * <p>Exactly one call to {@link #close} must be performed for each call
     * to {@link #open}.  If too few calls are made to {@link #close}, the zip file
     * will never be released from cache.  If too many calls are made to
     * {@link #close}, a prior open will have an unusable zip file.</p>
     */
    void close();

    /**
     * <p>Get an input stream for a zip file and an entry of that zip file.</p> 
     *
     * <p>The zip file must have been obtained from the zip file handle.  The
     * zip entry must have been obtained from the zip file.  The zip file handle
     * must have at least one active open.</p>
     * 
     * <p>An attempt to obtain an input stream from a directory type entry
     * will result in a null return value.</p>
     * 
     * @param zipFile The zip file from which to obtain the input stream.
     * @param zipEntry The entry for which to obtain the input stream.
     * 
     * @return An input stream for the entry in the zip file.  Null if the
     *     zip entry is a directory type entry.
     *
     * @throws IOException Thrown if the input stream cannot be obtained.
     */
    InputStream getInputStream(ZipFile zipFile, ZipEntry zipEntry) throws IOException;
    
    /**
     * <p>Get an input stream for a zip file and an entry of that zip file.</p> 
     *
     * <p>The zip file must have been obtained from the zip file handle.  The
     * zip entry must have been obtained from the zip file.  The zip file handle
     * must have at least one active open.</p>
     * 
     * <p>An attempt to obtain an input stream from a directory type entry
     * will result in a null return value.</p>
     * 
     * @param zipFile The zip file from which to obtain the input stream.
     * @param entryName The name of the entry for which to obtain the input stream.
     * 
     * @return An input stream for the entry in the zip file.  Null if the
     *     zip entry is a directory type entry.
     *
     * @throws IOException Thrown if the input stream cannot be obtained.
     */
    InputStream getInputStream(ZipFile zipFile, String entryName) throws IOException;
    
}
