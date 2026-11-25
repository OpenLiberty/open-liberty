/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.utils;

import java.nio.ByteBuffer;

/**
 * Provides native related services.
 */
public interface NativeUtils {

    /**
     * Retrieve the current MVS STCK value.
     *
     * @return The current native MVS STCK.
     */
    public long getSTCK();

    /**
     * Create a new {@code DirectByteBuffer} that maps the specified
     * address for the specified size.
     */
    public ByteBuffer mapDirectByteBuffer(final long address, final int size);

    /**
     * Pad/truncate a string to the required length and convert to EBCDIC
     *
     * @param s              A Java String
     * @param requiredLength The right length
     * @return an EBCDIC byte array of the required length, padded with blanks
     *         or truncated as necessary. A null if something went wrong.
     */
    public byte[] convertAsciiStringToFixedLengthEBCDIC(final String s, final int requiredLength);

    /**
     * Retrieve the current task ID using pthread_self()
     *
     * @return The current task ID.
     */
    public long getTaskId();

    /**
     * Retrieve the servers process ID using getpid()
     *
     * @return The servers process ID.
     */
    public int getPid();

    /**
     * Retrieve SMF data.
     * psatold 4 ttoken 16 thread id 8 and cvtldto 8
     *
     * @return SMF data.
     */
    public byte[] getSmfData();

    /**
     * Retrieve TIMEUSED data.
     *
     * @return TIMEUSED data.
     */
    public byte[] getTimeusedData();

    /**
     * Retrieve the server process UMASK.
     *
     * @return An decimal representation of the UMASK.
     */
    public int getUmask();

}