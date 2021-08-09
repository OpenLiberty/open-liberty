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

import java.io.IOException;

interface PhysicalFileInterface
{

    public String filename();
    
    public long length() throws IOException;

    public void close() throws IOException;

    public void flush() throws IOException;

    public int read() throws IOException;

    public int read(byte[] v) throws IOException;

    public int read(byte[] v, int off, int len) throws IOException;

    public int readInt() throws IOException;

    public long readLong() throws IOException;

    public short readShort() throws IOException;

    public void seek(long loc) throws IOException;

    public void write(byte[] v) throws IOException;

    public void write(byte[] v, int off, int len) throws IOException;

    public void write(int v) throws IOException;
    
    public void writeInt(int v) throws IOException;

    public void writeLong(long v) throws IOException;

    public void writeShort(short v) throws IOException;

}





