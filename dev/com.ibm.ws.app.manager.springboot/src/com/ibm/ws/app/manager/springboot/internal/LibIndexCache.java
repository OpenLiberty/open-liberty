/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_SHARED_LIB_CACHE_DIR;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_WORKAREA_LIB_CACHE_DIR;

import java.io.File;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.location.WsResource.Type;

/**
 *
 */
@Component(service = { LibIndexCache.class })
public final class LibIndexCache {
    private static final String CACHE_DIR = ".cache.dirs";
    private static final String CACHE_ARTIFACT_DIR = "artifact.cache";
    private static final String CACHE_ADAPT_DIR = "adapt.cache";
    private static final String CACHE_OVERLAY_DIR = "overlay.cache";

    private WsResource libraryIndexParent;
    private WsResource libraryIndexWorkArea;
    private ArtifactContainerFactory containerFactory;
    private AdaptableModuleFactory adaptableFactory;

    @Reference
    protected void setLocationAdmin(WsLocationAdmin locAdmin) {
        WsResource indexResParent = locAdmin.resolveResource(SPRING_SHARED_LIB_CACHE_DIR);
        if (indexResParent.isType(Type.DIRECTORY)) {
            libraryIndexParent = indexResParent;
        } else {
            libraryIndexParent = null;
        }
        WsResource indexResWorkArea = locAdmin.resolveResource(SPRING_WORKAREA_LIB_CACHE_DIR);
        indexResWorkArea.create();
        libraryIndexWorkArea = indexResWorkArea;
    }

    @Reference(target = "(&(category=DIR)(category=JAR)(category=BUNDLE))")
    protected void setArtifactContainerFactory(ArtifactContainerFactory containerFactory) {
        this.containerFactory = containerFactory;
    }

    @Reference
    protected void setAdaptableModuleFactory(AdaptableModuleFactory adaptableFactory) {
        this.adaptableFactory = adaptableFactory;
    }

    private File getLibrary(Map.Entry<String, String> LibIndexEntry) {
        if (libraryIndexParent != null) {
            // look in the parent (shared area) first
            WsResource parentRes = getStoreLocation(LibIndexEntry, libraryIndexParent);
            if (parentRes.exists()) {
                // found in parent; return it
                return parentRes.asFile();
            }
        }
        // look in the server work area now
        WsResource workareaRes = getStoreLocation(LibIndexEntry, libraryIndexWorkArea);
        if (workareaRes.exists()) {
            return workareaRes.asFile();
        }
        // nothing found
        return null;
    }

    private static WsResource getStoreLocation(Map.Entry<String, String> LibIndexEntry, WsResource storeRoot) {
        String hash = LibIndexEntry.getValue();
        String key = LibIndexEntry.getKey();
        //strip off /BOOT-INF/lib or /WEB-INF/lib
        String jarName = key.substring(key.lastIndexOf('/'));
        CharSequence prefix = hash.subSequence(0, 2);
        CharSequence postFix = hash.subSequence(2, hash.length());
        WsResource prefixDir = storeRoot.resolveRelative(prefix.toString() + '/');
        return prefixDir.resolveRelative(postFix.toString() + '/' + jarName);
    }

    public Container getLibraryContainer(Map.Entry<String, String> libIndexEntry) throws UnableToAdaptException {
        File libFile = getLibrary(libIndexEntry);
        if (libFile == null) {
            return null;
        }
        ArtifactContainer libArtifactContainer = containerFactory.getContainer(getCache(libFile, CACHE_ARTIFACT_DIR), libFile);
        if (libArtifactContainer == null) {
            throw new UnableToAdaptException("Unable to get the container for the entry " + libIndexEntry.getKey());
        }
        return adaptableFactory.getContainer(getCache(libFile, CACHE_ADAPT_DIR), getCache(libFile, CACHE_OVERLAY_DIR), libArtifactContainer);
    }

    private File getCache(File libFile, String cacheName) {
        WsResource cacheDir = libraryIndexWorkArea.resolveRelative(CACHE_DIR + '/' + libFile.getParentFile().getName() + '/' + libFile.getName() + '/' + cacheName + '/');
        cacheDir.create();
        return cacheDir.asFile();
    }

    public File getLibIndexParent() {
        return libraryIndexParent.asFile();
    }

    public File getLibIndexWorkarea() {
        return libraryIndexWorkArea.asFile();
    }

}
