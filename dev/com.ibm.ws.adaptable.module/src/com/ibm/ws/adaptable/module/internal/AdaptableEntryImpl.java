/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.adaptable.module.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class AdaptableEntryImpl implements Entry {
    protected final Container root;
    protected final OverlayContainer rootOverlay;
    protected final FactoryHolder factoryHolder;
    protected final ArtifactEntry delegate;

    public AdaptableEntryImpl(ArtifactEntry delegate, Container root, OverlayContainer rootOverlay, FactoryHolder factoryHolder) {
        this.delegate = delegate;
        this.root = root;
        this.rootOverlay = rootOverlay;
        this.factoryHolder = factoryHolder;
    }

    /** {@inheritDoc} */
    @Override
    public Container getEnclosingContainer() {
        ArtifactContainer parent = delegate.getEnclosingContainer();
        if (parent != null)
            //because we are an Entry, we don't need to worry about our container being a different root to us.
            return new AdaptableContainerImpl(delegate.getEnclosingContainer(), root, rootOverlay, factoryHolder);
        else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public Container getRoot() {
        return root;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return delegate.getName();
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        return delegate.getPath();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public long getSize() {
        return delegate.getSize();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public long getLastModified() {
        return delegate.getLastModified();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public URL getResource() {
        return delegate.getResource();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public String getPhysicalPath() {
        return delegate.getPhysicalPath();
    }

    @SuppressWarnings("unchecked")
    /** {@inheritDoc} */
    @Override
    public <T> T adapt(Class<T> adaptTarget) throws UnableToAdaptException {

        //built in conversion for inputstream special case.
        if (adaptTarget.equals(InputStream.class)) {
            try {
                return (T) delegate.getInputStream();
            } catch (IOException e) {
                throw new UnableToAdaptException(e);
            }
        }

        if (adaptTarget.equals(Container.class)) {
            ArtifactContainer c = delegate.convertToContainer();
            if (c != null) {
                if (c.isRoot()) {
                    //c is a new root, need to switch the overlay passed.
                    OverlayContainer newOverlay = rootOverlay.getOverlayForEntryPath(delegate.getPath());
                    return (T) new AdaptableContainerImpl(c, newOverlay, factoryHolder);
                } else {
                    return (T) new AdaptableContainerImpl(c, root, rootOverlay, factoryHolder);
                }
            }
        }

        T adapted = factoryHolder.getAdapterFactoryService().adapt(root, rootOverlay, delegate, this, adaptTarget);
        return adapted;
    }
}
