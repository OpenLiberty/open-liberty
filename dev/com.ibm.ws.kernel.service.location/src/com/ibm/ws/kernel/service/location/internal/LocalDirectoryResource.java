/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
abstract class LocalDirectoryResource implements InternalWsResource {

    @Override
    public boolean create() {
        return false;
    }

    /**
     * Virtual root resources can not be deleted.
     */
    @Override
    public boolean delete() {
        return false;
    }

    /**
     * Virtual root resources "exist"
     */
    @Override
    public boolean exists() {
        return true;
    }

    /**
     * Can not read from virtual root
     * 
     * @throws IOException
     *             when called
     */
    @Override
    public InputStream get() throws IOException {
        throw new IOException("Can not read from virtual root");
    }

    /**
     * Can not read from virtual root
     * 
     * @throws IOException
     *             when called
     */
    @Override
    public ReadableByteChannel getChannel() throws IOException {
        throw new IOException("Can not read from virtual root");
    }

    /**
     * Virtual root does not have a last modified date
     */
    @Override
    public long getLastModified() {
        return 0;
    }

    /**
     * Virtual root does not have a length
     */
    @Override
    public long length() {
        return 0;
    }

    /**
     * Virtual root is a directory
     */
    @Override
    public Type getType() {
        return Type.DIRECTORY;
    }

    /**
     * Virtual root is a directory
     */
    @Override
    public boolean isType(Type resourceType) {
        if (resourceType == Type.DIRECTORY)
            return true;

        return false;
    }

    /**
     * internal directory resource can not be moved.
     */
    @Override
    public boolean moveTo(WsResource target) {
        return false;
    }

    /**
     *
     */
    @Override
    public void put(InputStream source) throws IOException {
        throw new IOException("Can not write to directory " + toRepositoryPath());
    }

    /**
     * Can not write to virtual root
     * 
     * @throws IOException
     *             when called
     */
    @Override
    public void put(ReadableByteChannel source) throws IOException {
        throw new IOException("Can not write to directory " + toRepositoryPath());
    }

    /**
     * Can not write to virtual root
     * 
     * @throws IOException
     *             when called
     */
    @Override
    public WritableByteChannel putChannel() throws IOException {
        throw new IOException("Can not write to directory " + toRepositoryPath());
    }

    /**
     * Can not write to virtual root
     * 
     * @throws IOException
     *             when called
     */
    @Override
    public OutputStream putStream() throws IOException {
        throw new IOException("Can not write to directory " + toRepositoryPath());
    }

    /**
     * Can not set last modified time for directory
     */
    @Override
    public boolean setLastModified(long lastModified) {
        return false;
    }

    @Override
    public String[] introspectSelf() {
        return new String[] { toString() };
    }
}
