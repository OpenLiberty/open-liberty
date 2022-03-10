/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;

public class DDEntry<T> implements ArtifactEntry {

    private final Entry adaptableEntry;
    private final T cachedDD;

    public DDEntry(Entry adaptableEntry, T cachedDD) {
        this.adaptableEntry = adaptableEntry;
        this.cachedDD = cachedDD;
    }

    public T getCachedDD() {
        return cachedDD;
    }

    @Override
    public ArtifactContainer getEnclosingContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPath() {
        return adaptableEntry.getPath();
    }

    @Override
    public ArtifactContainer getRoot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPhysicalPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return adaptableEntry.getName();
    }

    @Override
    public ArtifactContainer convertToContainer() {
        return null;
    }

    @Override
    public ArtifactContainer convertToContainer(boolean localOnly) {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return adaptableEntry.adapt(InputStream.class);
        } catch (UnableToAdaptException e) {
            throw new IOException(e);
        }
    }

    @Override
    public long getSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getResource() {
        throw new UnsupportedOperationException();
    }
}
