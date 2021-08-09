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
package com.ibm.ws.kernel.service.location.internal;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
public class RemoteResource implements InternalWsResource {

    private static final TraceComponent tc = Tr.register(RemoteResource.class);
    private static final String CACHED_RESOURCE_FILE_NAME_PREFIX = "resCache_";
    private final Type type;
    private final URL url;
    private long lastModified;
    private WsResource cachedResource;

    /** Cached string value: lazily initialized. */
    private String stringValue = null;

    public RemoteResource(URL url) {
        this.url = url;
        this.type = Type.REMOTE;
        cacheResource();
    }

    /** {@inheritDoc} */
    @Override
    public boolean create() {
        throw new UnsupportedOperationException("Not valid for a remote resource");
    }

    /** {@inheritDoc} */
    @Override
    public boolean delete() {
        throw new UnsupportedOperationException("Not valid for a remote resource");
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists() {
        if (this.cachedResource != null)
            return true;

        try {
            InputStream is = get();
            if (is != null) {
                is.close();
                return true;
            }
            return false;
        } catch (IOException ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "IOException while checking existence of resource", ex);
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public InputStream get() throws IOException {
        return url.openStream();
    }

    /** {@inheritDoc} */
    @Override
    public ReadableByteChannel getChannel() throws IOException {
        return Channels.newChannel(get());
    }

    /** {@inheritDoc} */
    @Override
    public WsResource getChild(String name) {
        throw new UnsupportedOperationException("Not valid for a remote resource");
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<String> getChildren() {
        throw new UnsupportedOperationException("Not valid for a remote resource");
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<String> getChildren(String resourceRegex) {
        throw new UnsupportedOperationException("Not valid for a remote resource");
    }

    /** {@inheritDoc} */
    @Override
    public long getLastModified() {
        long value = 0;
        try {
            value = this.url.openConnection().getLastModified();
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getLastModified - caught IOException", e);
            }
            value = -1;
        }

        if (value == -1) { // exception caught
            value = 0; // 0 indicates unknown time
        } else if (value == 0) { // the connection succeeded, but the last modified date/time is unknown
            if (isUpdated()) { // do we need to update the lastModified?
                value = System.currentTimeMillis();
                setLastModified(value);
            }
        } else { // got last modified date/time from remote resource
            setLastModified(value);
        }

        return value;
    }

    /**
     * @return
     */
    private boolean isUpdated() {
        if (cachedResource == null) {
            return true;
        }
        ReadableByteChannel currentChannel = null;
        ReadableByteChannel cachedChannel = null;
        try {
            currentChannel = getChannel();
            cachedChannel = cachedResource.getChannel();

            ByteBuffer currentBuffer = ByteBuffer.allocateDirect(1024);
            ByteBuffer cachedBuffer = ByteBuffer.allocateDirect(1024);

            currentBuffer.flip();
            cachedBuffer.flip();

            while (true) {
                int currentByte = readFromChannel(currentChannel, currentBuffer);
                int cachedByte = readFromChannel(cachedChannel, cachedBuffer);

                if (currentByte == -1) {
                    if (cachedByte == -1) {
                        return false;
                    } else {
                        return true;
                    }
                } else if (currentByte != cachedByte) {
                    return true;
                }
            }
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "IOException while comparing resources", e);
            return true;
        } finally {
            tryToClose(currentChannel);
            tryToClose(cachedChannel);
        }
    }

    private static int readFromChannel(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        while (!buffer.hasRemaining()) {
            buffer.clear();
            int read = channel.read(buffer);
            if (read == -1) {
                return -1;
            } else if (read == 0) {
                // try reading again
            } else {
                buffer.flip();
                break;
            }
        }
        return buffer.get() & 0xff;
    }

    @FFDCIgnore(IOException.class)
    private static void tryToClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // At least attempt to close stream
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.url.getPath();
    }

    /** {@inheritDoc} */
    @Override
    public WsResource getParent() {
        throw new UnsupportedOperationException("Not valid for a remote resource");
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isType(Type resourceType) {
        return this.type == resourceType;
    }

    /** {@inheritDoc} */
    @Override
    public long length() {
        if (this.cachedResource == null) {
            return 0;
        } else {
            return this.cachedResource.length();
        }
    }

    /**
     * @throws IOException
     * 
     */
    private void cacheResource() {

        WsLocationAdmin locSvc = WsLocationAdminImpl.getInstance();
        File tempFile = locSvc.getBundleFile(this, CACHED_RESOURCE_FILE_NAME_PREFIX + url.toString().hashCode());

        WsResource resource = locSvc.asResource(tempFile, true);

        try {
            moveTo(resource);
            this.cachedResource = resource;
            setLastModified(System.currentTimeMillis());
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "IOException while attempting to cache resource", e);
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean moveTo(WsResource target) throws IOException {
        if (target == null)
            throw new NullPointerException("Can not move to a null resource");
        else if (target.equals(this))
            throw new IllegalArgumentException("Can not move a target to itself");

        if (target.getType() == Type.FILE) {
            InputStream is = get();
            if (is == null)
                return false;

            try {
                target.put(is);
            } finally {
                is.close();
            }

            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void put(InputStream source) throws IOException {
        throw new UnsupportedOperationException("Can not replace remote resource");
    }

    /** {@inheritDoc} */
    @Override
    public void put(ReadableByteChannel source) throws IOException {
        throw new UnsupportedOperationException("Can not replace remote resource");
    }

    /** {@inheritDoc} */
    @Override
    public OutputStream putStream() throws IOException {
        throw new UnsupportedOperationException("Can not replace remote resource");
    }

    /** {@inheritDoc} */
    @Override
    public WritableByteChannel putChannel() throws IOException {
        throw new UnsupportedOperationException("Can not replace remote resource");
    }

    /** {@inheritDoc} */
    @Override
    public WsResource resolveRelative(String relativeResourceURI) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean setLastModified(long lastModified) {
        if (this.lastModified == lastModified)
            return false;
        this.lastModified = lastModified;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public URI toExternalURI() {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public File asFile() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public String toRepositoryPath() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getNormalizedPath() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getRawRepositoryPath() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SymbolicRootResource getSymbolicRoot() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final String simpleName = this.getClass().getSimpleName();

        String result = stringValue;
        if (result == null) {
            StringBuilder str = new StringBuilder();
            str.append(simpleName).append("[").append(type).append(";").append("@")
                            .append(System.identityHashCode(this)).append(";").append(url).append("]");

            stringValue = result = str.toString();
        }

        return result;
    }

    @Override
    public String[] introspectSelf() {
        return new String[] { toString() };
    }
}
