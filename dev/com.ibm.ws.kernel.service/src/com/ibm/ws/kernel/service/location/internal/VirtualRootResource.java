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
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
class VirtualRootResource extends LocalDirectoryResource implements WsResource {
    private final TreeMap<String, SymbolicRootResource> children = new TreeMap<String, SymbolicRootResource>();

    VirtualRootResource() {
        SymbolRegistry.getRegistry().addRootSymbol(WsLocationConstants.LOC_VIRTUAL_ROOT, this);
    }

    /**
     * @param child
     */
    void addChild(SymbolicRootResource child) {
        children.put(child.getSymbolicName(), child);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WsResource getChild(String name) {
        return children.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getChildren() {
        return Collections.unmodifiableSet(children.keySet()).iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getChildren(String resourceRegex) {
        return new ResourceIterators.MatchingIterator(getChildren(), resourceRegex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WsResource resolveRelative(String relativeResourceURI) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Virtual root has a simple virtual name
     */
    @Override
    public String getName() {
        return WsLocationConstants.SYMBOL_ROOT_NODE;
    }

    /**
     * Virtual root has no parent
     */
    @Override
    public WsResource getParent() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public File asFile() {
        return null;
    }

    /**
     * Virtual root does not have a URI
     */
    @Override
    public URI toExternalURI() {
        return null;
    }

    /**
     * Virtual root repository ID is magic string (same as name)
     */
    @Override
    public String toRepositoryPath() {
        return WsLocationConstants.SYMBOL_ROOT_NODE;
    }

    @Override
    public String getNormalizedPath() {
        // this does not ahve a normalized path: it does not exist in the real world
        return null;
    }

    /**
     * Repository path is always the same
     */
    @Override
    public String getRawRepositoryPath() {
        return toRepositoryPath();
    }

    /**
     * This element has no parents
     */
    @Override
    public SymbolicRootResource getSymbolicRoot() {
        return null;
    }
}
