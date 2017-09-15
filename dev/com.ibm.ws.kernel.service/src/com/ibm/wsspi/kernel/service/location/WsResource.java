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
package com.ibm.wsspi.kernel.service.location;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

/**
 * This interface provides a wrapper/proxy for resources located by the
 * WsLocationAdmin service. The nature of the underlying resource (local vs.
 * remote, retrieved w/ http or some other service, etc.) should remain opaque
 * to the caller.
 * <p>
 * A resource can be a {@link Type#FILE} (e.g. a file), a {@link Type#DIRECTORY} (e.g. a directory or folder), or a {@link Type#URL} (e.g., an HTTP resource.)
 * A file can contain data, but not other files or files; a directory can contain other files or files, but no data.
 * <p>
 * Note that toExternalURI returns a URI, which is subject to URI encoding rules.
 * <p>
 * An WsResource instance is not synchronized against concurrent access, and offers no
 * guarantees about potential conflicts when modifying the underlying resource.
 * 
 * @see com.ibm.wsspi.kernel.service.location.WsLocationAdmin
 */
public interface WsResource {
    public enum Type {
        FILE, DIRECTORY, REMOTE
    };

    /**
     * Creates a resource if it does not exist. The type of resource created
     * depends on how the resource was resolved: a resource with a URI or
     * repository path ending in a '/' will be created as a directory; a resource
     * with a URI/path not ending in '/' will be created as a file.
     * <p>
     * If the resource is a file, ({@link #put(InputStream)}, {@link #put(ReadableByteChannel)}, {@link #putChannel()}, or {@link #putStream()} can
     * then be used to write to the new resource.
     * <p>
     * If the resource is a directory, child files or files can be created by resolving the child location and calling create.
     * 
     * @return <code>true</code> if and only if the wrapped/proxied resource is
     *         successfully created; <code>false</code> otherwise.
     * 
     * @throws SecurityException
     *             If a security manager exists and its {@link java.lang.SecurityManager#checkWrite(java.lang.String)} method
     *             denies access to create the file (in the case of a file/directory-backed resource)
     */
    boolean create();

    /**
     * Deletes the wrapped/proxied resource. If this resource is a directory (e.g. a
     * directory), then it must be empty (no sub-files or files) in order to be
     * deleted.
     * 
     * @return <code>true</code> if and only if the wrapped/proxied resource is
     *         successfully deleted; <code>false</code> otherwise
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkDelete}</code> method denies
     *             delete access to the file (in the case of a
     *             file/directory-backed resource)
     */
    boolean delete();

    /**
     * Tests whether the wrapped/proxied resource exists.
     * 
     * @return <code>true</code> if and only if the wrapped resource exists; <code>false</code> otherwise
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file or
     *             directory (in the
     *             case of a file/directory-backed resource)
     */
    boolean exists();

    /**
     * Creates and returns an InputStream for reading the contents of the
     * wrapped/proxied resource.
     * <p>
     * The caller is responsible for closing the returned input stream.
     * 
     * @return An input stream for reading from the wrapped/proxied resource.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code> {@link java.lang.SecurityManager#checkRead(java.lang.String)} </code> method denies read access to the file (in the
     *             case
     *             of
     *             a file/directory-backed resource)
     * 
     * @throws IOException
     *             If an I/O error occurs: if the resource has not been
     *             resolved, does not exist, is a directory rather than a file, or
     *             for some other reason cannot be opened for reading.
     */
    InputStream get() throws IOException;

    /**
     * Creates and returns a ReadableByteChannel for reading the contents of the
     * wrapped/proxied resource.
     * <p>
     * The caller is responsible for closing the returned nio channel.
     * 
     * @return An input stream for reading from the wrapped/proxied resource.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code> {@link java.lang.SecurityManager#checkRead(java.lang.String)} </code> method denies read access to the file (in the
     *             case
     *             of
     *             a file/directory-backed resource).
     * 
     * @throws IOException
     *             If an I/O error occurs: if the resource has not been
     *             resolved, does not exist, is a directory rather than a file, or
     *             for some other reason cannot be opened for reading.
     */
    ReadableByteChannel getChannel() throws IOException;

    /**
     * Obtain an WsResource for the named child (direct descendant of this
     * resource).
     * <p>
     * Note that this method returns null when the child does not exist. This differs from resolve(String) which never returns null.
     * 
     * @param name
     *            Name of child; can not contain path separator characters
     * 
     * @return An WsResource for the child if this is a directory and the child
     *         exists. Returns <code>null</code> if the child name is null, if
     *         this resource is not a directory, if the child does not exist, or if
     *         an I/O error occurs.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code> {@link java.lang.SecurityManager#checkRead(java.lang.String)} </code> method denies read access to the directory (in
     *             the
     *             case of a file/directory-backed resource).
     * 
     * @throws MalformedLocationException
     *             if name contains path separators.
     * @throws IllegalArgumentException
     *             if the child resource exists as a different type:
     *             i.e. if child name indicates the resource should be a
     *             directory and the resource exists as a file, or vice versa.
     */
    WsResource getChild(String name);

    /**
     * Returns an Iterator that will iterate over the names of each of the
     * children of this directory.
     * <p>
     * There is no guarantee that the names returned by the iterator will be in any specific order.
     * <p>
     * Not thread safe.
     * 
     * @return An <code>Iterator</code> that will iterate over a collection of <code>String</code>s representing the children of this <code>WsResource</code>; the Iterator will
     *         have no elements
     *         if the directory is empty, if this resource is not a directory, or if an
     *         I/O error occurs.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code> {@link java.lang.SecurityManager#checkRead(java.lang.String)} </code> method denies read access to the directory (in
     *             the
     *             case of a directory-backed resource)
     */
    Iterator<String> getChildren();

    /**
     * Find the children whose names match the provided regular expression.
     * <p>
     * The iterator will present the matching names in the order they were matched. If no resources were found, the iterator will have no elements.
     * 
     * @param regex
     *            Regular expression used to filter the names of children.
     * 
     * @return Iterator for the set/list of children whose names match the
     *         provided regular expression; the Iterator will have no elements
     *         if the directory is empty, if this resource is not a directory, or if an
     *         I/O error occurs.
     * 
     * @see String#matches(String)
     */
    Iterator<String> getChildren(String resourceRegex);

    /**
     * Returns the time that the wrapped/proxied resource was last modified.
     * 
     * @return A <code>long</code> value representing the time the file was last
     *         modified, measured in milliseconds since the epoch (00:00:00 GMT,
     *         January 1, 1970), or <code>0L</code> if the file does not exist
     *         or if an I/O error occurs
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file (in the
     *             case of a
     *             file/directory-backed resource)
     */
    long getLastModified();

    /**
     * Returns the name of this resource.
     * 
     * @return The name of the file or directory denoted by this abstract
     *         pathname. Node names will be terminated with a '/'.
     */
    String getName();

    /**
     * Obtain an WsResource for the parent of this resource.
     * 
     * @return An WsResource for the parent, which will be a directory; returns
     *         null if this resource has not been resolved, or if the parent of
     *         the resource is not reachable.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the parent (in the
     *             case of a
     *             file/directory-backed resource)
     * 
     * @throws IllegalArgumentException
     *             if parent resource already exists as a file.
     */
    WsResource getParent();

    /**
     * Return the resource type
     * 
     * @return type of WsResource.
     * @see WsResource.Type
     */
    Type getType();

    /**
     * Tests whether the wrapped/proxied resource is of the specified type: {@link Type}. The resource must exist for its type to be determined.
     * 
     * @return <code>true</code> if and only if the wrapped/proxied resource
     *         exists <em>and</em> matches the specified type; <code>false</code> otherwise.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file
     */
    boolean isType(Type resourceType);

    /**
     * Returns the length of the wrapped/proxied resource. The return value is
     * unspecified if this is not a file.
     * 
     * @return the length, in bytes, of the file denoted by this wrapped/proxied
     *         resource, or 0L if not a file.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file
     */
    public long length();

    /**
     * Moves this resource to the target resource. The (non-null) target resource
     * must
     * not exist before calling this method, it (and any missing parent files)
     * will be created when the resource is moved.
     * <p>
     * This should be viewed as a rename function: this resource (the source) is immutable. After calling this method
     * (assuming it completes without an exception), exists() should return false.
     * 
     * @param target
     *            The resolved WsResource that this resource should be moved
     *            to.
     * 
     * @return true if and only if the renaming succeeded; false otherwise
     * 
     * @throws java.io.IOException
     *             If the source resource (this) does not exist, if the source
     *             and target resources are not the same type (directory vs. file),
     *             if the target resource already exists or can not be created,
     *             or if there is any other error associated with moving the
     *             resource.
     * 
     * @throws java.lang.SecurityException
     *             If a security manager exists and its
     *             SecurityManager.checkWrite(java.lang.String) method denies
     *             write access to either the old or new pathnames
     * 
     * @throws java.lang.NullPointerException
     *             If parameter target is null
     */
    boolean moveTo(WsResource target) throws IOException;

    /**
     * Create or replace the content of this resource with the contents of the
     * provided InputStream.
     * <p>
     * If this resource is a file, this will also create any required parent files (e.g. calling <code>getParent().mkdirs()</code> on a File-backed resource).
     * <p>
     * An output stream will be created, filled with the contents of the input stream, and closed. The caller should close the input stream.
     * 
     * @param source
     *            InputStream containing the new contents of the resource.
     *            Method will return with no action if the source is null or
     *            empty.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code> {@link java.lang.SecurityManager#checkWrite(java.lang.String)} </code> method denies write access to the file (in the
     *             case
     *             of a file-backed resource)
     * 
     * @throws IOException
     *             If an I/O error occurs: if the resource does not exist and
     *             cannot be created, exists but is a directory rather than a file,
     *             or for some other reason cannot be opened for writing.
     */
    void put(InputStream source) throws IOException;

    /**
     * Create or replace the content of this resource with the contents of the
     * provided ReadableByteChannel.
     * <p>
     * If this resource is a file, this will also create any required parent files (e.g. calling <code>getParent().mkdirs()</code> on a File-backed resource).
     * <p>
     * An output stream/writable channel will be created, filled with the contents from the source channel, and closed. The caller should close the provided source.
     * 
     * @param source
     *            NIO ReadableByteChannel containing the new contents of the
     *            resource. Method will return with no action if the source is
     *            null or empty.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code> {@link java.lang.SecurityManager#checkWrite(java.lang.String)} </code> method denies write access to the file (in the
     *             case
     *             of a file-backed resource)
     * 
     * @throws IOException
     *             If an I/O error occurs: if the resource does not exist and
     *             cannot be created, exists but is a directory rather than a file,
     *             or for some other reason cannot be opened for writing.
     * 
     * @see java.nio.channels.ReadableByteChannel
     */
    void put(ReadableByteChannel source) throws IOException;

    /**
     * Creates and returns an OutputStream which can be used to replace the
     * contents of the named resource. The returned stream will be for writing
     * only, and will be a strict overwrite (not append).
     * <p>
     * If this resource is a file, this will also create any required parent files (e.g. calling <code>getParent().mkdirs()</code> on a File-backed resource).
     * <p>
     * The caller is responsible for closing the OutputStream.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code> {@link java.lang.SecurityManager#checkWrite(java.lang.String)} </code> method denies write access to the file (in the
     *             case
     *             of a file-backed resource)
     * 
     * @throws IOException
     *             If an I/O error occurs: if the resource does not exist and
     *             cannot be created, exists but is a directory rather than a file,
     *             or for some other reason cannot be opened for writing.
     */
    OutputStream putStream() throws IOException;

    /**
     * Creates and returns a WritableByteChannel which can be used to replace
     * the contents of the named resource. The returned channel will be for
     * writing only, and will be a strict overwrite (not append).
     * <p>
     * If this resource is a file, this will also create any required parent files (e.g. calling <code>getParent().mkdirs()</code> on a File-backed resource).
     * <p>
     * The caller is responsible for closing the channel.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code> {@link java.lang.SecurityManager#checkWrite(java.lang.String)} </code> method denies write access to the file (in the
     *             case
     *             of a file-backed resource)
     * 
     * @throws IOException
     *             If an I/O error occurs: if the resource does not exist and
     *             cannot be created, exists but is a directory rather than a file,
     *             or for some other reason cannot be opened for writing.
     */
    WritableByteChannel putChannel() throws IOException;

    /**
     * Resolve an WsResource relative to the current resource/location.
     * <ul>
     * <li>If the current resource is a directory, an unqualified resource name will be resolved as a child
     * of the directory, e.g. if this resource is "a/b/c/", and you resolve "d", the
     * returned resource will be "a/b/c/d"</li>
     * <li>If the current resource is a file, an unqualified resource name will be resolved as a peer,
     * e.g. if this resource is "a/b/c", and you resolve "d", the returned resource
     * will be "a/b/d"</li>
     * <li>The special location "${/}" {@link WsLocationConstants.SYMBOL_ROOT_NODE} can be used to resolve
     * this resource's root directory.</li>
     * </ul>
     * 
     * @param relativeResourceURI
     *            String describing the artifact to resolve. Be sure to include
     *            the terminator when resolving a directory.
     * 
     * @return new WsResource for the relative resource, or null if the
     *         provided string is null or not a valid relative URI, or if the
     *         relative location can not be determined (i.e. no associated
     *         root).
     * 
     * @throws MalformedLocationException
     *             if relativeResourceURI is badly formed (contains illegal
     *             characters, etc.)
     * 
     * @throws IllegalArgumentException
     *             if resource to be created already exists as a different type:
     *             i.e. if the resource uri indicates the resource should be a
     *             directory and the resource exists as a file, or vice versa;
     *             or if the relative path refers to a resource that is outside
     *             of the given file hierarchy (above the resource root).
     * @see URI#URI(String)
     * @see URI#URI(String, String, String, String)
     */
    WsResource resolveRelative(String relativeResourceURI);

    /**
     * Set the time that the wrapped/proxied resource was last modified.
     * 
     * @param lastModified
     *            A <code>long</code> value representing the time the file was
     *            last modified, measured in milliseconds since the epoch
     *            (00:00:00 GMT, January 1, 1970), or <code>0L</code> if the
     *            file does not exist or if an I/O error occurs
     * 
     * @return true if modification time for the resource was updated.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies read access to the file (in the
     *             case of a
     *             file/directory-backed resource)
     */
    boolean setLastModified(long lastModified);

    /**
     * Constructs a URI that represents this wrapped resource. The exact form of
     * the URI may be system-dependent, and may or may not use a well-known
     * scheme (like file).
     * <p>
     * If it can be determined that this is resource is a directory, the resulting URI will end with a slash.
     * 
     * @return a URI representing this resource with a defined scheme, but
     *         possibly undefined authority, query, and fragment components.
     */
    URI toExternalURI();

    /**
     * Returns the file backing this wrapped resource.
     * 
     * @return the File backing this resource, or null.
     */
    File asFile();

    /**
     * Constructs a String that represents this wrapped resource using symbolic
     * roots. The returned String will be suitable for use with {@link WsLocationAdmin#resolveResource(String)} or {@link WsResource#resolveRelative(String)}.
     * <p>
     * If it can be determined that this is resource is a directory, the resulting URI will end with a slash.
     * 
     * @return a relative URI (no scheme, etc.) representing this resource using
     *         symbolic elements.
     */
    String toRepositoryPath();
}