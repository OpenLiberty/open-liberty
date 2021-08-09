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
package com.ibm.ws.adaptable.module.internal;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.MaskedPathEntry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.EntryAdapter;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */
public class MaskedPathEntryAdapter implements EntryAdapter<MaskedPathEntry> {

    @Override
    public MaskedPathEntry adapt(Container root, OverlayContainer rootOverlay,
                                 ArtifactEntry artifactEntry, Entry entryToAdapt) throws UnableToAdaptException {
        return new MaskedPathEntryImpl(rootOverlay, artifactEntry.getPath());
    }

    private static final class MaskedPathEntryImpl implements MaskedPathEntry {
        private final OverlayContainer rootOverlay;
        private final String path;

        public MaskedPathEntryImpl(OverlayContainer rootOverlay, String path) {
            this.rootOverlay = rootOverlay;
            this.path = path;
        }

        @Override
        public void mask() {
            rootOverlay.mask(path);
        }

        @Override
        public void unMask() {
            rootOverlay.unMask(path);
        }

        @Override
        public boolean isMasked() {
            return rootOverlay.isMasked(path);
        }
    }
}
