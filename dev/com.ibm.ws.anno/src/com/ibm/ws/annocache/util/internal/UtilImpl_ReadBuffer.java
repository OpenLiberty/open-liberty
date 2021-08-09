/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.util.internal;

import java.io.IOException;

/**
 * Simple buffered reader, with augmented APIs for
 * binary cache reading.
 */
public interface UtilImpl_ReadBuffer {
    // Identity ...

    /**
     * Answer the path of the base resource.
     *
     * @return The path of the base resource.
     */
    String getPath();

    // Base resource ...
    
    void close() throws IOException;

    // Integer reads ...

    /**
     * Read a small (two byte) integer from the buffer.
     *
     * @return The small integer which was read from the buffer.
     *
     * @throws IOException Thrown if the read failed.
     */
    int readSmallInt() throws IOException;

    /**
     * Read a large (four byte) integer from the buffer.
     *
     * @return The large integer which was read from the buffer.
     *
     * @throws IOException Thrown if the read failed.
     */
    int readLargeInt() throws IOException;

    // Basic read API ...

    int read() throws IOException;
    void read(byte[] bytes) throws IOException;
    public void read(byte[] bytes, int offset, int len) throws IOException;

    // Seek API ...

    int getFileLength();

    public void seekEnd(int offset) throws IOException;
    public void seek(int offset) throws IOException;

    // String read API ...

    String getEncoding();

    public int validSmallInt() throws IOException;

    public String readString() throws IOException;
    public String readString(int width) throws IOException;

    // Field helper API ...

    public void requireByte(byte fieldByte) throws IOException;
    public String requireField(byte fieldByte) throws IOException;
    public String requireField(byte fieldByte, int width) throws IOException;
}
