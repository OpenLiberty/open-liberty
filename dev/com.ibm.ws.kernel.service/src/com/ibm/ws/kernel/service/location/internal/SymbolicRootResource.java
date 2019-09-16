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
import java.net.URI;
import java.util.Iterator;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.location.SymbolException;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * A SymbolicRoot acts as the root of a mini file-system. When traversing
 * or resolving relative resources, LocalFileResources will not traverse away
 * from/outside of the root they were resolved with.
 */
public class SymbolicRootResource extends LocalDirectoryResource implements InternalWsResource {
    /** The root file resource that represents the base of a hierarchy. */
    private final File root;

    /** The symbolic representation of this root resource */
    private final String symbolicName;

    /**
     * The normalized path to this resource (with extra .. and . and whatnots
     * collapsed).
     */
    private final String normalizedRoot;

    /** Cache the string value */
    private String stringValue = null;

    /** Cache the external URI */
    private URI externalURI = null;

    /** Common virtual parent */
    private final VirtualRootResource parent;

    /**
     * Create a new resource root using the provided symbolic name
     * to represent the root file.
     * 
     * @param fileName
     *            The name of the file that is the root resource
     * @param symName
     *            The symbolic name of this resource without the symbol decorations
     * @param commonRoot
     * 
     * @throws NullPointerException
     *             if either the fileName or the symbolicName are null
     * @throws IllegalArgumentException
     *             if the named location exits and is a file
     * @see File#File(String)
     */
    public SymbolicRootResource(String fileName, String symName, VirtualRootResource commonRoot) {
        if (fileName == null || symName == null)
            throw new NullPointerException("Both a file name and a symbolic name are required to create a resource root (f=" + fileName + ",s=" + symName + ")");

        File f = new File(fileName);

        // The resource doesn't have to exist yet, but if it does, it can't be a
        // file
        if (f.exists() && f.isFile())
            throw new IllegalArgumentException("Resource root already exists as a file (fn=" + fileName + ")");

        String nroot = PathUtils.normalize(f.getAbsolutePath());

        if (nroot.charAt(nroot.length() - 1) != '/')
            nroot += "/";

        parent = commonRoot;
        normalizedRoot = nroot;

        // Make the symbolic root undeleteable if it's one of ours (was.whatever)
        // otherwise, we don't know whether or not it should be removable..
        root = symName.startsWith("was") ? new UndeletableFile(nroot) : new File(nroot);

        symbolicName = WsLocationConstants.SYMBOL_PREFIX + symName + WsLocationConstants.SYMBOL_SUFFIX;
        boolean added = SymbolRegistry.getRegistry().addRootSymbol(symName, this);

        // Throw if the root symbol was not added: can not assume that the other
        // registration was for the same path, or for another root..
        if (!added)
            throw new SymbolException("Symbolic root could not be registered, variable already exists (variable=" + symbolicName + ",root=" + normalizedRoot + ")");

        // parent is not null, add this to parent
        if (parent != null)
            parent.addChild(this);
    }

    @Override
    public boolean create() {
        if (!root.exists()) {
            return root.mkdirs();
        }
        return false;
    }

    /**
     * @param relativePath
     * @return
     */
    public InternalWsResource createDescendantResource(String relativePath) {
        String path = PathUtils.normalizeDescendentPath(relativePath);
        if (path.length() == 0)
            return this;

        return LocalFileResource.newResource(normalizedRoot + path, symbolicName + '/' + path, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WsResource getChild(String name) {
        return ResourceUtils.getChildResource(this, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getChildren() {
        // Return empty list if the wrapped file is null, if it isn't an existing
        // directory, or if we don't have a root. We will not resolve resources (or traverse
        // parent/child) if we aren't associated with a root
        if (!root.isDirectory())
            return ResourceUtils.EMPTY_STRING_LIST.iterator();

        return ResourceUtils.getChildren(this, root);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getChildren(String regex) {
        // Return empty list if the wrapped file is null, if it is not a directory,
        // or if we don't have a root. We will not resolve resources (or traverse parent/child) if we 
        // aren't associated with a root
        if (!root.isDirectory())
            return ResourceUtils.EMPTY_STRING_LIST.iterator();

        return ResourceUtils.getChildren(this, root, regex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WsResource resolveRelative(String relativeResourceURI) {
        if (relativeResourceURI == null)
            return null;

        // If it's an empty string, return this
        if (relativeResourceURI.length() == 0)
            return this;

        // If it's ${/}, return the virtual root
        if (relativeResourceURI.equals(WsLocationConstants.SYMBOL_ROOT_NODE))
            return parent;

        return ResourceUtils.getRelativeResource(this, relativeResourceURI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WsResource getParent() {
        return parent;
    }

    /**
     * Check if the provided path is contained within this root's hierarchy.
     * 
     * @param normalizedPath
     *            normalized path for a potential descendant ('..' and '.' segments
     *            removed)
     * @return
     *         true if this root contains the resource indicated by the
     *         normalized path, false otherwise (or if the path is null).
     */
    boolean contains(String normalizedPath) {
        if (normalizedPath == null)
            return false;

        if (normalizedPath.length() < normalizedRoot.length())
            return false;

        return normalizedPath.regionMatches(0, normalizedRoot, 0, normalizedRoot.length());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() {
        return root.exists();
    }

    /**
     * {@inheritDoc

     */
    @Override
    public SymbolicRootResource getSymbolicRoot() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNormalizedPath() {
        return normalizedRoot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return root.getName() + '/';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRawRepositoryPath() {
        return symbolicName + '/';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toRepositoryPath() {
        return symbolicName + '/';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI toExternalURI() {
        URI local = externalURI;

        if (local == null)
            externalURI = local = root.toURI();

        return local;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public File asFile() {
        return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String string = stringValue;
        if (string == null)
            stringValue = string = this.getClass().getSimpleName() + "[" + symbolicName + "/=" + normalizedRoot + "]";

        return string;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;

        int result = 17;
        result = prime * result + symbolicName.hashCode();
        result = prime * result + normalizedRoot.hashCode();

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof SymbolicRootResource))
            return false;

        SymbolicRootResource other = (SymbolicRootResource) obj;

        if (!symbolicName.equals(other.symbolicName))
            return false;

        if (!normalizedRoot.equals(other.normalizedRoot))
            return false;

        return true;
    }

    /**
     * @return
     */
    public String getSymbolicName() {
        return symbolicName;
    }
}
