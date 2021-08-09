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

import java.util.Iterator;

import com.ibm.ws.adaptable.module.structure.StructureHelper;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.InterpretedContainer;
import com.ibm.wsspi.adaptable.module.Notifier;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.EnclosedEntity;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */
public class InterpretedContainerImpl extends AdaptableContainerImpl implements InterpretedContainer {

    private StructureHelper structureHelper = null;

    private Boolean structureHelperSaysWeAreRoot = null;
    private ArtifactContainer structureHelperLocalRootDelegate = null;

    /** {@inheritDoc} */
    @Override
    public void setStructureHelper(StructureHelper sh) {
        if (sh != null && this.structureHelper == null) {
            this.structureHelper = sh;
            structureHelperSaysWeAreRoot = structureHelper.isRoot(delegate);
            if (structureHelperSaysWeAreRoot && !delegate.isRoot()) {
                structureHelperLocalRootDelegate = delegate;
            }
        } else {
            throw new IllegalStateException();
        }
    }

    private Entry getInterpretedEntryInEnclosingContainer() {
        ArtifactEntry e = delegate.getEntryInEnclosingContainer();
        if (e != null) {
            if (this.isRoot()) {
                //we're going up to a new root so find out the information about this new root
                RootInformation rootInformation = getRootInformation(e, e.getEnclosingContainer());
                return new InterpretedEntryImpl(e, rootInformation.root, rootInformation.overlay, factoryHolder, structureHelper, rootInformation.structureHelperRoot);
            } else {
                //still in the same root so just return a new entry impl using the same root information as this object
                return new InterpretedEntryImpl(e, this.root, this.rootOverlay, factoryHolder, structureHelper, structureHelperLocalRootDelegate);
            }
        } else {
            //we must have been the ultimate top container.. which has no corresponding entry.
            return null;
        }
    }

    /**
     * This data structure stores information about a root container
     */
    private class RootInformation {
        /** The root container */
        private final Container root;
        /** The overlay for the artifact container root */
        private final OverlayContainer overlay;
        /** The root Artifact Container as supplied by the structure helper, may be <code>null</code> */
        private final ArtifactContainer structureHelperRoot;

        /**
         * @param root The root container
         * @param overlay The overlay for the artifact container root
         * @param structureHelperRoot The root Artifact Container as supplied by the structure helper
         */
        public RootInformation(Container root, OverlayContainer overlay, ArtifactContainer structureHelperRoot) {
            super();
            this.root = root;
            this.overlay = overlay;
            this.structureHelperRoot = structureHelperRoot;
        }
    }

    /**
     * <p>
     * This method will create a new root Container for the supplied <code>entity</code> when moving up from this container. As we are moving up from this container the
     * <code>entity</code> being passed in should either be the one returned from <code>delegate.getEntryInEnclosingContainer</code> or
     * <code>delegate.getEnclosingContainer()</code>.
     * </p><p>
     * When creating the new root this will either use the root {@link ArtifactContainer} for the <code>entity</code> as its delegate or if the {@link StructureHelper} for this
     * object says there is a fake root in the tree for the entity then this will be used.
     * </p>
     * 
     * @param entity The entity to get the root for
     * @param firstPotentialRoot This is the first {@link ArtifactContainer} that could potentially be a fake root from the structure helper. When <code>entity</code> is an
     *            {@link ArtifactEntry} then this will be the container returned from <code>entity.getEnclosingContainer()</code> as this is the first container for the entity. If
     *            <code>entity</code> is a {@link ArtifactContainer} then it could be a root itself so the <code>firstPotentialRoot</code> will be the same as <code>entity</code>.
     * @return The information used to create the new root container
     */
    private RootInformation getRootInformation(EnclosedEntity entity, ArtifactContainer firstPotentialRoot) {
        //first we need to see if we need a new overlay container, you only get new overlays for new artifact roots, not fake roots
        OverlayContainer newOverlay = rootOverlay;
        if (delegate.isRoot()) {
            //the delegate was root, so we're moving up to a new artifact root.
            //we have to correct the overlay

            //the overlay is simple, it always has a 1:1 relationship with artifact roots.
            //as we have gone up a root, we make it do so also.
            newOverlay = rootOverlay.getParentOverlay();
        }

        //now we need to work out what the root is for this entity.  There are two options, either a fake root or a real root.
        //to see if we need to use a fake root we ask the structure helper if the path to this entity is valid
        //we only ask the structure helper this question if the path contains containers..
        // eg. /filename  does not. 
        //     /container/filename does.
        //because if it is just /filename then there can't be a fake root above it.

        //note that if filename is a container then it might be a fake root itself,
        //that is ok though as when we create a new InterpretedContainerImpl for it the constructor will do a test to see if it should be a fake root
        String ePath = entity.getPath();
        //remove leading / from path so we can test if this has containers above it
        ePath = ePath.substring(1);

        ArtifactContainer structureHelperSetRootDelegate = null;
        final ArtifactContainer newRootDelegate;
        if (ePath.indexOf("/") == -1 || (structureHelper != null && structureHelper.isValid(entity.getRoot(), entity.getPath()))) {
            //easy case, the entry in the enclosing container is supposed to be there..
            //we don't need to pass a fake root node, because the node did exist under the
            //artifact api root.

            //leave structureHelperSetRootDelegate as null as the new root won't have a structure helper root delegate...
            //...it will have a normal root delegate though so set this
            newRootDelegate = entity.getRoot();
        } else {
            //walk up the enclosing container chain, asking the structure helper
            //if each should be considered as the root.
            ArtifactContainer enclosing = firstPotentialRoot;
            while (!enclosing.isRoot()) {
                if (structureHelper != null && structureHelper.isRoot(enclosing)) {
                    structureHelperSetRootDelegate = enclosing;
                    break;
                }
                enclosing = enclosing.getEnclosingContainer();
            }
            if (structureHelper == null) {
                newRootDelegate = enclosing;
            } else {
                if (structureHelperSetRootDelegate == null) {
                    //this means the structure helper told us a path was not valid,
                    //but did not identify to us which container as part of that path
                    //should have been the new root.
                    throw new IllegalStateException();
                }

                //we've found the root delegate from the structure helper so use this as the root deleage for the new root
                newRootDelegate = structureHelperSetRootDelegate;
            }
        }

        //the adaptable root for the new adaptable entry, is derivable, because
        //we only need to tell containers the correct overlay, 
        //can initialise it with the correct overlay, and correct structure helper delegate.
        Container newRoot = new InterpretedContainerImpl(newRootDelegate, newOverlay, factoryHolder, structureHelper, structureHelperSetRootDelegate);
        return new RootInformation(newRoot, newOverlay, structureHelperSetRootDelegate);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(Class<T> adaptTarget) throws UnableToAdaptException {
        //intercept the adapt to parent entry, to return interpreted entry
        if (adaptTarget == Entry.class) {
            return (T) getInterpretedEntryInEnclosingContainer();
        } else if (adaptTarget == Notifier.class && structureHelper != null) {
            // We only adapt roots so if we aren't root then grab the root
            if (isRoot()) {
                // Lazily create the notifier
                if (this.notifier == null) {
                    final ArtifactContainer rootArtifactContainer;
                    if (structureHelperLocalRootDelegate == null) {
                        // We are a real root so we know that the delegate is the root
                        rootArtifactContainer = delegate;
                    } else {
                        //we are creating a notifier and are a fake root so use the fake delegate root
                        rootArtifactContainer = structureHelperLocalRootDelegate;
                    }
                    this.notifier = new InterpretedNotifier(this, rootArtifactContainer, structureHelper);
                }
                return (T) this.notifier;
            } else {
                return (T) getRoot().adapt(Notifier.class);
            }
        } else if (adaptTarget != InterpretedContainer.class)
            //safety feature.. the above if prevents adapting interpreted into another interpreted.
            return super.adapt(adaptTarget);
        else {
            //adaptTarget was Interpreted, so we prevent the adapt.
            return null;
        }
    }

    public InterpretedContainerImpl(ArtifactContainer delegate, OverlayContainer rootOverlay, FactoryHolder factoryHolder) {
        super(delegate, null, rootOverlay, factoryHolder);

        //we're the very 1st converted interpreted container, we patch up the root
        //in our super class, to be 'us' rather than the original Container passed to us.
        this.root = this;

        //the call to setStructureHelper will set if we become root
    }

    public InterpretedContainerImpl(ArtifactContainer delegate, OverlayContainer rootOverlay, FactoryHolder factoryHolder, StructureHelper sh,
                                    ArtifactContainer structureHelperLocalRootDelegate) {
        this(delegate, null, rootOverlay, factoryHolder, sh, structureHelperLocalRootDelegate);
        this.root = this;
    }

    public InterpretedContainerImpl(ArtifactContainer delegate, Container root, OverlayContainer rootOverlay, FactoryHolder factoryHolder, StructureHelper sh,
                                    ArtifactContainer structureHelperLocalRootDelegate) {
        super(delegate, root, rootOverlay, factoryHolder);
        this.structureHelper = sh;
        this.structureHelperLocalRootDelegate = structureHelperLocalRootDelegate;
        if (structureHelper != null) {
            //we're a nested interpreted container, if we're under an overridden root, it got passed
            //in as structureHelperLocalRootDelegate.
            //but! we may also be overridden to be a root too!
            //so, we have to ask the structure helper if we should be, and if so, update our stored 
            //structureHelperLocalRootDelegate.
            structureHelperSaysWeAreRoot = structureHelper.isRoot(delegate);
            if (structureHelperSaysWeAreRoot) {
                this.root = this;
                if (!delegate.isRoot()) {
                    this.structureHelperLocalRootDelegate = delegate;
                } else {
                    //the structure helper asked us to override to root, something
                    //that already was a root, so we disable our override logic by
                    //setting our Boolean back to null.
                    structureHelperSaysWeAreRoot = null;
                    this.structureHelperLocalRootDelegate = null;
                }
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public Container getEnclosingContainer() {
        com.ibm.wsspi.artifact.ArtifactContainer parent = delegate.getEnclosingContainer();
        if (parent != null) {
            if (this.isRoot()) {
                //the parent is in a different root so find out all the information we need about this root
                RootInformation rootInformation = this.getRootInformation(parent, parent);
                return new InterpretedContainerImpl(parent, rootInformation.root, rootInformation.overlay, factoryHolder, structureHelper, rootInformation.structureHelperRoot);
            } else {
                //easy option, the delegate was not a root, so going upwards would not be a different root. 
                return new InterpretedContainerImpl(delegate.getEnclosingContainer(), root, rootOverlay, factoryHolder, structureHelper, structureHelperLocalRootDelegate);
            }
        } else
            return null;
    }

    @Override
    public Entry getEntry(String pathAndName) {

        if (structureHelperSaysWeAreRoot != null && structureHelperLocalRootDelegate != null) {
            //we need to 'fix' any requests for entries asking from the root, 
            //as the root has moved.. 
            if (pathAndName.startsWith("/")) {
                //path name requested is absolute.. and we have a fake root, so it needs fixing.
                //we just add the node we are using as root's path to the request. 
                pathAndName = structureHelperLocalRootDelegate.getPath() + pathAndName;
            } else {
                //path name requested was relative to this node.
                //so we can just ask the delegate directly =)
            }
        }

        ArtifactEntry entry = null;
        //ask the structure helper to make sure the path doesn't go into a new root.. 
        if (structureHelper != null && structureHelperSaysWeAreRoot != null && structureHelperLocalRootDelegate != null) {
            entry = delegate.getEntry(pathAndName);
            if (entry != null) {
                String path = entry.getPath();
                String delegatePath = structureHelperLocalRootDelegate.getPath();
                //we have to check the user's path and name didnt escape the fake root to another valid path.
                //starts with is a good check, but we need to know if they didn't slip to a directory parallel to the fake root
                //that starts with the same entry name.. eg, fake root of   /a/FakeRoot   and parallel dir of /a/FakeRootToo
                //so we test starts with, length, and then charAt to look for the /     
                //this blocks i) stuff completely outside the hierarchy of of the fakeroot (!startswith)
                //           ii) stuff parallel that would pass startswith in error..
                //          iii) the fake root itself to be returned. (as you can't getEntry root).
                if (!(path.startsWith(delegatePath) && path.length() > delegatePath.length() && path.charAt(delegatePath.length()) == '/')) {
                    //we are inside a fake root, else we would have taken the other branch back at 'ask the structure helper'
                    //so any paths returned should start with delegatePath.. otherwise the pathAndName path escaped our 
                    //fake root jail, that would mean the user requested a path above this root, (eg using ..) which we 
                    //don't support.
                    entry = null;
                } else {
                    //convert path of entry back to relative to this 'root'
                    //we do this because pathAndName was a user originated path, that may have had .. etc in it.
                    //path is currently path to entry, as found from delegate, so we strip it back to a relative 
                    //one by trimming off the sh root delegates path.. which will either be /xxx(/xxx)* or / in the first
                    //case that leaves a path of /yyy in the 2nd just yyy, we make it relative to / by trimming the /
                    path = path.substring(delegatePath.length());
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    //ask the structure helper, and blank entry if bad.
                    if (!structureHelper.isValid(structureHelperLocalRootDelegate, path))
                        entry = null;
                }
            }
        } else {
            //no delegate root? just ask base..
            entry = delegate.getEntry(pathAndName);
            //if we found something, and have a structure helper we need to check it's "allowed"
            //note: use entry.getPath rather than pathAndName so the path is cleaned up for the StructureHelper.
            if (entry != null && structureHelper != null && !structureHelper.isValid(delegate.getRoot(), entry.getPath())) {
                entry = null;
            }
        }

        if (entry != null)
            return new InterpretedEntryImpl(entry, root, rootOverlay, factoryHolder, structureHelper, structureHelperLocalRootDelegate);
        else
            return null;
    }

    @Override
    public String getName() {
        if (structureHelperSaysWeAreRoot != null && structureHelperLocalRootDelegate != null && structureHelperSaysWeAreRoot) {
            return "/";
        } else {
            return super.getName();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        if (structureHelperSaysWeAreRoot != null && structureHelperLocalRootDelegate != null) {
            if (structureHelperSaysWeAreRoot) {
                return "/";
            } else {
                String originalNodePath = super.getPath();
                return originalNodePath.substring(structureHelperLocalRootDelegate.getPath().length());
            }
        } else {
            return super.getPath();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRoot() {
        //the 2nd part gates to make sure structureHelper can only make things become root,
        //rather than let it demote things from being root..
        if (structureHelperSaysWeAreRoot != null && structureHelperSaysWeAreRoot) {
            return structureHelperSaysWeAreRoot;
        } else {
            return super.isRoot();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<Entry> iterator() {
        Iterator<ArtifactEntry> iter = delegate.iterator();
        if (structureHelperSaysWeAreRoot != null) {
            return new WrapperedIterator(iter, root, rootOverlay, factoryHolder, structureHelper, structureHelperLocalRootDelegate);
        } else {
            return new WrapperedIterator(iter, root, rootOverlay, factoryHolder, structureHelper, null);
        }
    }

    private static class WrapperedIterator implements Iterator<Entry> {
        private final Iterator<com.ibm.wsspi.artifact.ArtifactEntry> delegateIter;
        private final Container root;
        private final OverlayContainer rootOverlay;
        private final FactoryHolder factoryHolder;
        private final StructureHelper sh;
        private final ArtifactContainer structureHelperLocalRootDelegate;

        public WrapperedIterator(Iterator<ArtifactEntry> delegate, Container root, OverlayContainer rootOverlay, FactoryHolder factoryHolder,
                                 StructureHelper sh, ArtifactContainer shRoot) {
            this.delegateIter = delegate;
            this.root = root;
            this.rootOverlay = rootOverlay;
            this.factoryHolder = factoryHolder;
            this.sh = sh;
            this.structureHelperLocalRootDelegate = shRoot;
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
                return new InterpretedEntryImpl(entry, root, rootOverlay, factoryHolder, sh, structureHelperLocalRootDelegate);
            else
                return null;
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            delegateIter.remove();
        }

    }
}
