/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.library.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.library.internal.SharedLibraryImpl.ClasspathType;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.config.Fileset;
import org.osgi.framework.ServiceRegistration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

final class LibraryGeneration {
    private static final TraceComponent tc = Tr.register(LibraryGeneration.class);
    private static final AtomicInteger nextInt = new AtomicInteger();
    private final String libraryId;
    private final int genId = nextInt.getAndIncrement();

    // NOTE: the <? extends ...> pattern enforces read-only semantics at compile time.
    // It has no bearing on the runtime behaviour at all.
    private final Dictionary<? extends String, ? extends Object> libraryProps;
    private final List<? extends String> filesetRefs;
    private final EnumSet<? extends ApiType> apiTypeVisibility;
    private final Collection<? extends File> files;
    private final Collection<? extends File> folders;
    private final Collection<? extends ArtifactContainer> fileAndFolderContainers;

    private final Collection<Fileset> filesets;
    private final ConcurrentLinkedQueue<ServiceRegistration<?>> filesetListenerRegs = new ConcurrentLinkedQueue<ServiceRegistration<?>>();

    private volatile boolean cancelled;
    private final SharedLibraryImpl library;

    LibraryGeneration(SharedLibraryImpl library, String libraryId, Dictionary<String, Object> props) {
        this.library = library;
        this.libraryId = libraryId;
        Dictionary<String, Object> libraryProps = Util.copy(props);
        this.libraryProps = libraryProps;

        String displayId = (String) props.get("config.displayId");
        if (displayId == null)
            displayId = this.libraryId;

        String[] fsRefs = null;
        EnumSet<ApiType> apiTypeVisibility = EnumSet.noneOf(ApiType.class);
        String[] fileRef = null;
        String[] folderRef = null;
        String[] pathRef = null;
        for (SharedLibraryConstants.SharedLibraryAttribute attr : SharedLibraryConstants.SharedLibraryAttribute.values()) {
            Object o = props.get(attr.toString());
            switch (attr) {
                case filesetRef:
                    fsRefs = (String[]) o;
                    continue;
                case apiTypeVisibility:
                    apiTypeVisibility = ApiType.createApiTypeSet((String) o);
                    continue;
                case fileRef:
                    fileRef = (String[]) o;
                    continue;
                case folderRef:
                    folderRef = (String[]) o;
                    continue;
                case pathRef:
                    pathRef = (String[]) o;
                    continue;
                default:
                    continue;
            }
        }
        Collection<File> files = library.retrieveClasspaths(ClasspathType.FILE, libraryId, fileRef, displayId);
        Collection<File> folders = library.retrieveClasspaths(ClasspathType.FOLDER, libraryId, folderRef, displayId);
        Collection<File> paths = library.retrieveClasspaths(ClasspathType.PATH, libraryId, pathRef, displayId);
        for (File p : paths) {
            if (p.isDirectory()) {
                folders.add(p);
            } else {
                files.add(p);
            }
        }
        if (fsRefs == null) {
            this.filesetRefs = Collections.emptyList();
        } else {
            this.filesetRefs = Collections.unmodifiableList(Arrays.asList(fsRefs));
        }
        this.fileAndFolderContainers = initContainers(files, folders);
        this.files = files;
        this.folders = folders;
        this.apiTypeVisibility = apiTypeVisibility;
        if (this.filesetRefs.isEmpty()) {
            filesets = Collections.emptyList();
        } else {
            filesets = new ArrayBlockingQueue<Fileset>(filesetRefs.size());
        }
    }

    private List<ArtifactContainer> initContainers(Collection<File> files, Collection<File> folders) {
        if (files.isEmpty() && folders.isEmpty())
            return Collections.emptyList();
        LinkedList<ArtifactContainer> result = new LinkedList<ArtifactContainer>();
        for (File f : files) {
            if (Util.isArchive(f)) {
                addContainerFromFile(f, result);
            }
        }
        for (File f : folders) {
            addContainerFromFile(f, result);
        }
        return result;
    }

    void addContainerFromFile(File f, Collection<ArtifactContainer> containers) {
        String filename = String.format("%s/%s_%s_%s", SharedLibraryFactory.CONT_CACHE, libraryId, genId, f.getName());
        File wc = library.ctx.getBundle().getDataFile(filename);
        boolean ok = wc.mkdir();
        if (!ok) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "cannot create cache dir for container:", f.getName());
            }
        }
        ArtifactContainer ac = library.artifactContainerFactory.getContainer(wc, f);
        if (ac != null) {
            containers.add(ac);
        }
    }

    void cancel() {
        cancelled = true;
        ServiceRegistration<?> listener;
        while ((listener = filesetListenerRegs.poll()) != null) {
            listener.unregister();
        }
    }

    void fetchFilesets() {
        if (isCancelled() || library.isDeleted()) {
            return;
        }

        if (filesetRefs.isEmpty()) {
            library.publishGeneration(this);
        } else {
            new FilesetListener(library, this, filesetRefs, filesets, filesetListenerRegs);
        }
    }

    @SuppressWarnings("unchecked")
    EnumSet<ApiType> getApiTypeVisibility() {
        return EnumSet.copyOf((EnumSet<ApiType>) apiTypeVisibility);
    }

    Collection<File> getFiles() {
        return Util.freeze(files);
    }

    Collection<File> getFolders() {
        return Util.freeze(folders);
    }

    Collection<Fileset> getFilesets() {
        return Util.freeze(filesets);
    }

    Collection<ArtifactContainer> getContainers() {
        // Easy case: no filesets to traverse
        if (filesets.isEmpty())
            return Util.freeze(fileAndFolderContainers);
        // First make a copy of the (constant) files and folders for this LibraryGeneration.
        Collection<ArtifactContainer> result = new ArrayList<ArtifactContainer>(fileAndFolderContainers);
        // Then add the containers for the (dynamic) filesets.
        for (Fileset fset : filesets) {
            for (File f : fset.getFileset()) {
                addContainerFromFile(f, result);
            }
        }
        // Finally, ensure no-one messes with the result, lest they assume it is meaningful to do so.
        return Util.freeze(result);
    }

    Dictionary<String, Object> getProperties() {
        return Util.copy(libraryProps);
    }

    boolean isCancelled() {
        return cancelled;
    }

    String getLibraryId() {
        return libraryId;
    }
}
