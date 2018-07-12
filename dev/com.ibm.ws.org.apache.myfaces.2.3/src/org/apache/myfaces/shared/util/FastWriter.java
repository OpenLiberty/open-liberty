/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.shared.util;

import java.io.IOException;
import java.io.Writer;

/**
 * StringWriter cannot be reused without create a new object over and over.
 * This class provide a simple implementation that allows reuse the same buffer
 * in a efficient way.
 *
 * @author Jacob Hookom
 */
public final class FastWriter extends Writer
{

    private char[] buff;
    private int size;

    public FastWriter()
    {
        this(1024);
    }

    public FastWriter(int initialSize)
    {
        if (initialSize < 0)
        {
            throw new IllegalArgumentException("Initial Size cannot be less than 0");
        }
        this.buff = new char[initialSize];
    }

    public void close() throws IOException
    {
        // do nothing
    }

    public void flush() throws IOException
    {
        // do nothing
    }

    private final void overflow(int len)
    {
        if (this.size + len > this.buff.length)
        {
            char[] next = new char[(this.size + len) * 2];
            System.arraycopy(this.buff, 0, next, 0, this.size);
            this.buff = next;
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException
    {
        overflow(len);
        System.arraycopy(cbuf, off, this.buff, this.size, len);
        this.size += len;
    }

    public void write(char[] cbuf) throws IOException
    {
        this.write(cbuf, 0, cbuf.length);
    }

    public void write(int c) throws IOException
    {
        this.overflow(1);
        this.buff[this.size] = (char) c;
        this.size++;
    }

    public void write(String str, int off, int len) throws IOException
    {
        overflow(len);
        str.getChars(off, off+len, this.buff, size);
        this.size += len;
    }

    public void write(String str) throws IOException
    {
        this.write(str, 0, str.length());
    }

    public void reset()
    {
        this.size = 0;
    }

    public String toString()
    {
        return new String(this.buff, 0, this.size);
    }
}
