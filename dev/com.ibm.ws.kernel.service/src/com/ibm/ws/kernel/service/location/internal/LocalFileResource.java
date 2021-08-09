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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.location.ExistingResourceException;
import com.ibm.wsspi.kernel.service.location.ResourceMismatchException;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

/**
 *
 */
class LocalFileResource implements InternalWsResource {
    private static final TraceComponent tc = Tr.register(LocalFileResource.class);

    /** Root containing this local resource */
    private final SymbolicRootResource resourceRoot;

    /**
     * Type of this resource, set when the resource is created based on the
     * wrapped file path: {@link Type#DIRECTORY} paths end in slashes, {@link Type#FILE} paths do not.
     */
    private final WsResource.Type type;

    /** File wrapped by the resource. Immutable. */
    private final File wrappedFile;

    /** Normalized path */
    private final String normalizedPath;

    /** Name of resource (including terminating slash for files) */
    private final String name;

    /** Calculated hashCode */
    private final int hashCode;

    /** Cached string value: lazily initialized. */
    private String stringValue = null;

    /** Cached externalURI: lazily initialized */
    private URI externalURI = null;

    /**
     * Repository URI which abstracts the location of the resource from the
     * underlying file system. Initialized lazily.
     */
    private String repositoryPath = null;

    /**
     * Create a new local resource
     * 
     * @param normalizedPath
     *            Normalized file path for this resource; can not be null.
     * @param repositoryPath
     *            An abstraction of the file location; may be null.
     */
    public static LocalFileResource newResource(String normalizedPath, String repositoryPath) {
        return new LocalFileResource(normalizedPath, repositoryPath, null);
    }

    /**
     * Create a new local resource
     * 
     * @param normalizedPath
     *            Normalized file path for this resource; can not be null.
     * @param repositoryPath
     *            An abstraction of the file location; may be null.
     * @param root
     *            Symbolic root for resource
     */
    public static LocalFileResource newResource(String normalizedPath, String repositoryPath, SymbolicRootResource root) {
        return new LocalFileResource(normalizedPath, repositoryPath, root);
    }

    /**
     * Create a new local resource
     * 
     * @param normalizedPath
     *            Normalized file path for this resource; can not be null.
     * @param repositoryPath
     *            An abstraction of the file location; may be null.
     * @param related
     *            LocalFileResource with shared attributes (like the SymbolicRoot)
     */
    public static LocalFileResource newResourceFromResource(String normalizedPath, String repositoryPath, InternalWsResource related) {
        SymbolicRootResource root = null;
        if (related != null)
            root = related.getSymbolicRoot();

        return new LocalFileResource(normalizedPath, repositoryPath, root);
    }

    /**
     * Create a new local resource
     * 
     * @param wrappedFile
     *            The file wrapped by this resource; can not be null
     * @param repoPath
     *            An abstraction of the file location; may be null.
     * @param root
     *            Repository root used for locating relative resources and
     *            constructing
     *            repository paths; may be null
     * 
     * @throws NullPointerException
     *             if normalized path is null
     * @throws IllegalArgumentException
     *             if resource to be created already exists as
     *             a different type: i.e. if the normalizedPath indicates this
     *             resource
     *             should be a directory and the resource exists as a file, or vice
     *             versa.
     */
    private LocalFileResource(String normalizedPath, String repositoryPath, SymbolicRootResource root) {
        if (normalizedPath == null)
            throw new NullPointerException("File for a LocalFileResource can not be null");

        File f = new File(normalizedPath);
        String tmpName = f.getName();

        if (normalizedPath.charAt(normalizedPath.length() - 1) == '/') {
            this.type = Type.DIRECTORY;
            if (repositoryPath != null && repositoryPath.charAt(repositoryPath.length() - 1) != '/')
                repositoryPath += '/';
            tmpName += '/';
        } else {
            this.type = Type.FILE;
            if (repositoryPath != null && repositoryPath.charAt(repositoryPath.length() - 1) == '/')
                repositoryPath = repositoryPath.substring(0, repositoryPath.length() - 1);
        }

        if (f.exists()) {
            if (f.isFile() && type == Type.DIRECTORY)
                throw new IllegalArgumentException("Path specified a directory, but resource exists as a file (path=" + normalizedPath + ")");
            else if (f.isDirectory() && type == Type.FILE)
                throw new IllegalArgumentException("Path specified a file, but resource exists as a directory (path=" + normalizedPath + ")");
        }

        this.normalizedPath = normalizedPath;
        this.wrappedFile = f;
        this.name = tmpName;
        this.repositoryPath = repositoryPath;
        this.resourceRoot = root;

        hashCode = normalizedPath.hashCode();
    }

    /**
     * Package private method to retrieve wrapped file
     */
    @Override
    public String getNormalizedPath() {
        return normalizedPath;
    }

    @Override
    public String getRawRepositoryPath() {
        return repositoryPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean create() {
        if (wrappedFile.exists())
            return false;

        boolean success = true;

        switch (type) {
            default:
            case FILE:
                if (!wrappedFile.getParentFile().exists())
                    success = FileUtils.ensureDirExists(wrappedFile.getParentFile());
                else if (!wrappedFile.getParentFile().isDirectory())
                    return false;

                if (success) {
                    try {
                        success = wrappedFile.createNewFile();
                    } catch (IOException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "IOException creating file", wrappedFile, e);
                        return false;
                    }
                }
                break;

            case DIRECTORY:
                if (wrappedFile.getParentFile().exists() && !wrappedFile.getParentFile().isDirectory())
                    return false;

                success = wrappedFile.mkdirs();
                break;
        }

        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() {
        if (!wrappedFile.exists())
            return false;

        return wrappedFile.delete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() {
        return wrappedFile.exists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream get() throws IOException {
        if (type == Type.DIRECTORY)
            throw new IOException("Can not read contents of a directory");

        return new FileInputStream(wrappedFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadableByteChannel getChannel() throws IOException {
        if (type == Type.DIRECTORY)
            throw new IOException("Can not read contents of a directory");

        return new FileInputStream(wrappedFile).getChannel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WsResource getChild(String name) {
        // Return null if the wrapped file is null, if it isn't an existing
        // directory,
        // or if we don't have a root. We will not resolve resources (or traverse
        // parent/child)
        // if we aren't associated with a root
        if (type == Type.FILE || !wrappedFile.isDirectory() || resourceRoot == null)
            return null;

        return ResourceUtils.getChildResource(this, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getChildren() {
        // Return empty list if the wrapped file is null, if it isn't an existing
        // directory,
        // or if we don't have a root. We will not resolve resources (or traverse
        // parent/child)
        // if we aren't associated with a root
        if (type == Type.FILE || !wrappedFile.isDirectory() || resourceRoot == null)
            return ResourceUtils.EMPTY_STRING_LIST.iterator();

        return ResourceUtils.getChildren(this, wrappedFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getChildren(String regex) {
        // Return empty list if the wrapped file is null, if it isn't an existing
        // directory,
        // or if we don't have a root. We will not resolve resources (or traverse
        // parent/child)
        // if we aren't associated with a root
        if (type == Type.FILE || !wrappedFile.isDirectory() || resourceRoot == null)
            return ResourceUtils.EMPTY_STRING_LIST.iterator();

        return ResourceUtils.getChildren(this, wrappedFile, regex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WsResource getParent() {
        // We will not resolve resources (or traverse parent/child) if we aren't
        // associated with a root
        if (resourceRoot == null)
            return null;

        return ResourceUtils.getParentResource(this, resourceRoot);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isType(Type type) {
        return this.type == type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastModified() {
        return wrappedFile.lastModified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setLastModified(long lastModified) {
        return wrappedFile.setLastModified(lastModified);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean moveTo(WsResource target) throws IOException {
        if (target == null)
            throw new NullPointerException("Can not move to a null resource");

        if (this.equals(target))
            return false;

        if (target.exists())
            throw new ExistingResourceException(this.toString(), target.toString());

        // If target resource was created as the wrong type, throw an exception
        if (!target.isType(type))
            throw new ResourceMismatchException(target.toRepositoryPath(), type, target.getType());

        // Only support moving file to file now.
        if (target instanceof LocalFileResource) {
            LocalFileResource targetFile = (LocalFileResource) target;
            return wrappedFile.renameTo(targetFile.wrappedFile);
        }

        throw new UnsupportedOperationException("Can only move files");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(InputStream source) throws IOException {
        if (source == null)
            return;

        // Error condition checking in putStream (directory, unknown file, etc.)
        OutputStream os = putStream();
        byte[] buffer = new byte[1024];
        try {
            int read;
            while ((read = source.read(buffer)) >= 0) {
                os.write(buffer, 0, read);
            }
        } finally {
            os.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(ReadableByteChannel source) throws IOException {
        if (source == null || !source.isOpen())
            return;

        WritableByteChannel os = putChannel();
        FileChannel fos = null;

        try {
            if (source instanceof FileChannel) {
                fos = (FileChannel) os;
                FileChannel fis = (FileChannel) source;

                fis.transferTo(0, fis.size(), fos);
            } else {
                ByteBuffer buf = ByteBuffer.allocate(1024);
                int rc;
                while ((rc = source.read(buf)) >= 0) {
                    if (rc == 0)
                        continue;

                    buf.flip(); // flip (position back to 0, limit to amount read)
                    os.write(buf); // copy data into writable channel
                    buf.clear(); // prepare for new read
                }
            }
        } finally {
            if (fos != null)
                fos.close();
            else if (os != null)
                os.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WritableByteChannel putChannel() throws IOException {
        // Error condition checking in putStream (directory, unknown file, etc.)
        FileOutputStream fos = (FileOutputStream) putStream();

        return fos.getChannel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream putStream() throws IOException {
        if (type == Type.DIRECTORY)
            throw new IOException("Can not write to a directory");

        if (!FileUtils.ensureDirExists(wrappedFile.getParentFile())) {
            throw new IOException("Unable to create parent directory");
        }

        return new FileOutputStream(wrappedFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WsResource resolveRelative(String relativeResourceURI) {
        if (relativeResourceURI == null)
            return null;

        // If it's an empty string, just duplicate ourselves
        if (relativeResourceURI.length() == 0)
            return newResourceFromResource(normalizedPath, repositoryPath, this);

        // We will not resolve relative resources (or traverse parent/child)
        // if we aren't associated with a root
        if (resourceRoot == null)
            return null;

        // If it's ${/}, return our root
        if (relativeResourceURI.equals(WsLocationConstants.SYMBOL_ROOT_NODE))
            return resourceRoot;

        return ResourceUtils.getRelativeResource(this, relativeResourceURI);
    }

    /**
     * {@inheritDoc
     */
    @Override
    public SymbolicRootResource getSymbolicRoot() {
        return resourceRoot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public File asFile() {
        return wrappedFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public URI toExternalURI() {
        URI local = externalURI;
        if (local == null) {
            local = wrappedFile.toURI();
            String sURI = local.toString();
            if (type == Type.DIRECTORY && sURI.charAt(sURI.length() - 1) != '/')
                local = URI.create(sURI + '/');

            externalURI = local;
        }

        return local;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public String toRepositoryPath() {
        String path = repositoryPath;

        if (path == null) {
            if (resourceRoot != null)
                repositoryPath = path = ResourceUtils.createRepositoryURI(this, resourceRoot);
            else
                repositoryPath = path = new File(toExternalURI()).getAbsolutePath(); //.getRawPath();
        }

        return path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final String simpleName = this.getClass().getSimpleName();
        final String undefined = "<undefined>";

        String result = stringValue;
        if (result == null) {
            String rPath = repositoryPath == null ? undefined : repositoryPath;

            StringBuilder str = new StringBuilder(simpleName.length() + rPath.length() + normalizedPath.length() + 5);

            str.append(simpleName).append("[").append(type).append(";").append("@").append(hashCode).append(";").append(rPath).append(";").append(normalizedPath).append("]");

            stringValue = result = str.toString();
        }

        return result;
    }

    @Override
    public String[] introspectSelf() {
        return new String[] { toString() };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public int hashCode() {
        return hashCode;
    }

    /**
     * Compares the argument to the receiver, and answers true if they represent
     * the same object using a class specific comparison:
     * <ul>
     * <li>if the argument is the exact same object as the receiver (==).
     * <li>if the argument is an instance of a LocalFileResource and the wrapped files are either both null, or equal (File.equals).</li>
     * 
     * @param o
     *            the object to compare with this object.
     * @return boolean <code>true</code> if the object is the same as this
     *         object <code>false</code> if it is different from this object.
     * @see #hashCode
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (!(obj instanceof LocalFileResource))
            return false;

        LocalFileResource other = (LocalFileResource) obj;
        if (!normalizedPath.equals(other.normalizedPath))
            return false;

        return true;
    }

    @Override
    public long length() {
        return wrappedFile.length();
    }
}