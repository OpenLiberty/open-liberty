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
package com.ibm.ws.adaptable.module.internal;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.InterpretedContainer;
import com.ibm.wsspi.adaptable.module.Notifier;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */
public class AdaptableContainerImpl implements Container {
    protected Container root; //root can be updated by interpreted container.
    protected final OverlayContainer rootOverlay;
    protected final FactoryHolder factoryHolder;
    protected final com.ibm.wsspi.artifact.ArtifactContainer delegate;
    /** The notifier for this container, only set when {@link #isRoot()} returns <code>true</code> */
    protected Notifier notifier;

    public AdaptableContainerImpl(OverlayContainer rootOverlay, FactoryHolder factoryHolder) {
        this.delegate = rootOverlay;
        this.root = this;
        this.rootOverlay = rootOverlay;
        this.factoryHolder = factoryHolder;
    }

    public AdaptableContainerImpl(com.ibm.wsspi.artifact.ArtifactContainer delegate, OverlayContainer rootOverlay, FactoryHolder factoryHolder) {
        this.delegate = delegate;
        this.root = this;
        this.rootOverlay = rootOverlay;
        this.factoryHolder = factoryHolder;
    }

    public AdaptableContainerImpl(com.ibm.wsspi.artifact.ArtifactContainer delegate, Container root, OverlayContainer rootOverlay, FactoryHolder factoryHolder) {
        this.delegate = delegate;
        this.root = root;
        this.rootOverlay = rootOverlay;
        this.factoryHolder = factoryHolder;
    }

    /** {@inheritDoc} */
    @Override
    public Container getEnclosingContainer() {
        com.ibm.wsspi.artifact.ArtifactContainer parent = delegate.getEnclosingContainer();
        if (parent != null) {
            if (delegate.isRoot()) {
                //if the delegate has isRoot true, then the enclosingContainer is going to 
                //be from the next root up.. which means a change of overlay.. 
                OverlayContainer newOverlay = rootOverlay.getParentOverlay();
                //the container will be the container the artifact api gives us.
                ArtifactContainer newDelegateContainer = delegate.getEnclosingContainer();
                //the adaptable root for the container has to be built, using the new artifactContainer.getRoot, and the parent Overlay.
                Container newRoot = new AdaptableContainerImpl(newDelegateContainer.getRoot(), newOverlay, factoryHolder);
                //factoryHolder can stay the same
                return new AdaptableContainerImpl(newDelegateContainer, newRoot, newOverlay, factoryHolder);
            } else {
                return new AdaptableContainerImpl(delegate.getEnclosingContainer(), root, rootOverlay, factoryHolder);
            }
        } else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public Container getRoot() {
        return root;
    }

    public Entry getEntryInEnclosingContainer() {
        //rather than use isRoot, we check if there is an enclosing container
        //the top most root will not have one.
        //all other roots will have one.
        com.ibm.wsspi.artifact.ArtifactContainer parent = delegate.getEnclosingContainer();
        if (parent != null) {
            if (delegate.isRoot()) {
                //if the delegate has isRoot true, then the entryInEnclosingContainer is going to 
                //be from the next root up.. which means a change of overlay.. 
                OverlayContainer newOverlay = rootOverlay.getParentOverlay();
                //the entry will be the entry the artifact api gives us.
                ArtifactEntry newDelegateEntry = delegate.getEntryInEnclosingContainer();
                //the adaptable root for the entry has to be built, using the new artifactEntry.getRoot, and the parent Overlay.
                Container newRoot = new AdaptableContainerImpl(newDelegateEntry.getRoot(), newOverlay, factoryHolder);
                //factoryHolder can stay the same
                return new AdaptableEntryImpl(newDelegateEntry, newRoot, newOverlay, factoryHolder);
            } else {
                return new AdaptableEntryImpl(delegate.getEntryInEnclosingContainer(), root, rootOverlay, factoryHolder);
            }
        } else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public Entry getEntry(String pathAndName) {
        com.ibm.wsspi.artifact.ArtifactEntry entry = delegate.getEntry(pathAndName);
        if (entry != null)
            return new AdaptableEntryImpl(entry, root, rootOverlay, factoryHolder);
        else
            return null;
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
    public Collection<URL> getURLs() {
        return delegate.getURLs();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public String getPhysicalPath() {
        return delegate.getPhysicalPath();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(Class<T> adaptTarget) throws UnableToAdaptException {
        //if this is the top level adaptable container.. allow conversion to an interpreted one..
        if (adaptTarget.equals(InterpretedContainer.class) && isRoot() && getEnclosingContainer() == null) {
            return (T) new InterpretedContainerImpl(delegate, rootOverlay, factoryHolder);
        }
        //built in to adapt to entry..
        if (adaptTarget.equals(Entry.class)) {
            return (T) getEntryInEnclosingContainer();
        }
        // built in to adapt to Notifier
        if (adaptTarget.equals(Notifier.class)) {
            // Only return a new notifier if we're root
            if (this.isRoot()) {
                // Lazily create the notifier
                if (this.notifier == null) {
                    this.notifier = new NotifierImpl(delegate, this);
                }
                return (T) this.notifier;
            } else {
                return (T) this.getRoot().adapt(Notifier.class);
            }
        }
        //all other adapts go to the factory.
        T adapted = factoryHolder.getAdapterFactoryService().adapt(root, rootOverlay, delegate, this, adaptTarget);
        return adapted;
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<Entry> iterator() {
        return new WrapperedIterator(delegate.iterator(), root, rootOverlay, factoryHolder);
    }

    private static class WrapperedIterator implements Iterator<Entry> {
        private final Iterator<com.ibm.wsspi.artifact.ArtifactEntry> delegateIter;
        private final Container root;
        private final OverlayContainer rootOverlay;
        private final FactoryHolder factoryHolder;

        public WrapperedIterator(Iterator<com.ibm.wsspi.artifact.ArtifactEntry> delegate, Container root, OverlayContainer rootOverlay, FactoryHolder factoryHolder) {
            this.delegateIter = delegate;
            this.root = root;
            this.rootOverlay = rootOverlay;
            this.factoryHolder = factoryHolder;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return delegateIter.hasNext();
        }

        /** {@inheritDoc} */
        @Override
        public Entry next() {
            com.ibm.wsspi.artifact.ArtifactEntry entry = delegateIter.next();
            if (entry != null)
                return new AdaptableEntryImpl(entry, root, rootOverlay, factoryHolder);
            else
                return null;
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            delegateIter.remove();
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean isRoot() {
        return delegate.isRoot();
    }
}
