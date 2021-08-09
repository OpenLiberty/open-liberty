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

import com.ibm.ws.adaptable.module.structure.StructureHelper;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */
public class InterpretedEntryImpl extends AdaptableEntryImpl {
    final StructureHelper structureHelper;
    final ArtifactContainer structureHelperLocalRootDelegate;

    public InterpretedEntryImpl(ArtifactEntry delegate, Container root, OverlayContainer rootOverlay, FactoryHolder factoryHolder, StructureHelper sh,
                                ArtifactContainer shRoot) {
        super(delegate, root, rootOverlay, factoryHolder);
        this.structureHelper = sh;
        this.structureHelperLocalRootDelegate = shRoot;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(Class<T> adaptTarget) throws UnableToAdaptException {
        //intercept the adapt to container, to return interpreted.
        if (adaptTarget.equals(Container.class)) {
            ArtifactContainer c = delegate.convertToContainer();
            if (c != null) {
                if (c.isRoot()) {
                    //c is a new root, need to switch the overlay passed.
                    OverlayContainer newOverlay = rootOverlay.getOverlayForEntryPath(delegate.getPath());
                    //Structure helpers can only make non-roots into roots
                    //they cannot make roots into non-roots.
                    //
                    //we have just encountered an artifact root, so this WILL
                    //be a new adaptable root. 
                    //
                    //we swap to using..
                    // - the new overlay for the new root
                    // - this InterpretedContainer becomes root (omit passing the 2nd arg 'root')
                    // - pass null as the structureHelperLocalRootDelegate, because the roots align again.
                    //
                    return (T) new InterpretedContainerImpl(c, newOverlay, factoryHolder, structureHelper, null);
                } else {
                    //the new artifact container may or may not be a structure helper declared root
                    //this doesnt matter here.
                    //we don't need to reconfigure the structureHelperLocalRootDelegate, as the interpreted container
                    //does that in its constructor.
                    return (T) new InterpretedContainerImpl(c, root, rootOverlay, factoryHolder, structureHelper, structureHelperLocalRootDelegate);
                }
            }
        }
        return super.adapt(adaptTarget);
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        if (structureHelperLocalRootDelegate != null) {
            String originalNodePath = super.getPath();
            return originalNodePath.substring(structureHelperLocalRootDelegate.getPath().length());
        } else {
            return super.getPath();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Container getEnclosingContainer() {
        com.ibm.wsspi.artifact.ArtifactContainer parent = delegate.getEnclosingContainer();
        if (parent != null) {
            //we are an entry, and will always have an owning container.. 
            //we dont need to worry about fake root transitions here.
            return new InterpretedContainerImpl(delegate.getEnclosingContainer(), root, rootOverlay, factoryHolder, structureHelper, structureHelperLocalRootDelegate);
        } else
            return null;
    }

}
