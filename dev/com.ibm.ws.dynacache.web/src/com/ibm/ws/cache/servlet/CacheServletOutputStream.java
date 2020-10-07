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
package com.ibm.ws.cache.servlet;

import java.io.*;
import javax.servlet.ServletOutputStream;


/**
 * This class is a proxy to the WebSphere output stream object.
 * It has features added to enable caching.
 */
public class CacheServletOutputStream extends ServletOutputStream
{
    /**
     * The WebSphere output stream that this one proxies.
     */
    protected OutputStream outputStream = null;


    /**
     * Constructor with parameters.
     *
     * @param outputStream The output stream to be proxied.
     */
    public CacheServletOutputStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    /**
     * This overrides the method in the WebSphere output stream.
     * It is forwarded to the output stream that is proxied.
     *
     * @param output The int to be written.
     */
    public void write(int output) throws IOException
    {
        outputStream.write(output);
    }

    /**
     * This overrides the method in the WebSphere output stream.
     * It is forwarded to the output stream that is proxied.
     *
     * @param output The byte[] to be written.
     */
    public void write(byte output[],int offset,int length) throws IOException
    {
        outputStream.write(output,offset,length);
    }

    /**
     * This overrides the method in the WebSphere output stream.
     * It is forwarded to the output stream that is proxied.
     *
     * @param output The byte[] to be written.
     */
    public void write(byte output[]) throws IOException
    {
        outputStream.write(output);
    }
}
