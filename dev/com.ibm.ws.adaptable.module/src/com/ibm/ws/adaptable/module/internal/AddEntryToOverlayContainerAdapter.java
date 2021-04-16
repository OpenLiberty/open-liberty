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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.ibm.wsspi.adaptable.module.AddEntryToOverlay;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

public class AddEntryToOverlayContainerAdapter implements ContainerAdapter<AddEntryToOverlay> {

    @Override
    public AddEntryToOverlay adapt(Container root, OverlayContainer rootOverlay,
                                   ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        return new AddEntryToOverlayImpl(rootOverlay, artifactContainer.getPath());
    }

    private static final class AddEntryToOverlayImpl implements AddEntryToOverlay {
        private final OverlayContainer rootOverlayContainer;
        private final String containerPath;

        public AddEntryToOverlayImpl(OverlayContainer rootOverlayContainer, String containerPath) {
            this.rootOverlayContainer = rootOverlayContainer;
            this.containerPath = containerPath;
        }

        /** {@inheritDoc} */
        @Override
        public boolean add(String entryRelativePath, String entryData) {
            boolean done = false;
            entryRelativePath = PathUtils.normalize(entryRelativePath);
            if (!PathUtils.pathIsAbsolute(entryRelativePath)) {
                String absolutePath = this.containerPath + "/" + entryRelativePath;
                String name = PathUtils.getName(absolutePath);
                ArtifactEntry artifactEntry = new AddedArtifactEntry(name, entryData);
                done = this.rootOverlayContainer.addToOverlay(artifactEntry, absolutePath, false);
            }
            return done;
        }
    }

    public static class AddedArtifactEntry implements ArtifactEntry {
        private final String name;
        private final byte[] bytes;
        private final Long lastModified;

        public AddedArtifactEntry(String name, String data) {
            this.name = name;
            this.bytes = data.getBytes(StandardCharsets.UTF_8);
            this.lastModified = System.currentTimeMillis();
        }

        @Override
        // never called, navigation is handled by overlay that wraps us.
        public ArtifactContainer getEnclosingContainer() {
            throw new IllegalStateException();
        }

        @Override
        // never called, navigation is handled by overlay that wraps us.
        public String getPath() {
            throw new IllegalStateException();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        // AddEntryToOverlay supports adding entries with data (so file-like),
        // conversion not possible so return null as per interface definition.
        public ArtifactContainer convertToContainer() {
            return null;
        }

        @Override
        // AddEntryToOverlay supports adding entries with data (so file-like),
        // conversion not possible so return null as per interface definition.
        public ArtifactContainer convertToContainer(boolean localOnly) {
            return null;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        // never called, navigation is handled by overlay that wraps us.
        public ArtifactContainer getRoot() {
            throw new IllegalStateException();
        }

        /** {@inheritDoc} */
        @Override
        public long getLastModified() {
            return lastModified;
        }

        /** {@inheritDoc} */
        @Override
        // no real location on disk so return null as per interface definition.
        public URL getResource() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        // no real location on disk so return null as per interface definition.
        public String getPhysicalPath() {
            return null;
        }
    }

}
