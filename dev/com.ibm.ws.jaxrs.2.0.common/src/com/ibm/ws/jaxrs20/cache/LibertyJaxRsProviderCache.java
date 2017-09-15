/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cache;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.model.ProviderInfo;

/**
 *
 */
public class LibertyJaxRsProviderCache {

    private final ConcurrentHashMap<Class<?>, SoftReference<ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>>> readerProviderCache = new ConcurrentHashMap<Class<?>, SoftReference<ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>>>();

    private final ConcurrentHashMap<Class<?>, SoftReference<ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>>> writerProviderCache = new ConcurrentHashMap<Class<?>, SoftReference<ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>>>();

    public List<List<ProviderInfo<?>>> getReader(Class<?> type, MediaType mt) {

        if (type == null || mt == null) {
            return Collections.emptyList();
        }

        SoftReference<ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>> mediaTypeToProviderCacheRef = this.readerProviderCache.get(type);

        if (mediaTypeToProviderCacheRef == null || mediaTypeToProviderCacheRef.get() == null) {
            return Collections.emptyList();
        }

        ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>> mediaTypeToProviderCache = mediaTypeToProviderCacheRef.get();

        if (mediaTypeToProviderCache == null) {
            return Collections.emptyList();
        }

        if (mediaTypeToProviderCache.containsKey(mt)) {
            return mediaTypeToProviderCache.get(mt);
        }
        else
            return Collections.emptyList();
    }

    public List<List<ProviderInfo<?>>> getWriter(Class<?> type, MediaType mt) {

        if (type == null || mt == null) {
            return Collections.emptyList();
        }

        SoftReference<ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>> mediaTypeToProviderCacheRef = this.writerProviderCache.get(type);

        if (mediaTypeToProviderCacheRef == null || mediaTypeToProviderCacheRef.get() == null) {
            return Collections.emptyList();
        }

        ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>> mediaTypeToProviderCache = mediaTypeToProviderCacheRef.get();

        if (mediaTypeToProviderCache == null) {
            return Collections.emptyList();
        }

        if (mediaTypeToProviderCache.containsKey(mt)) {
            return mediaTypeToProviderCache.get(mt);
        }
        else
            return Collections.emptyList();
    }

    public void putReader(Class<?> type, MediaType mt, List<List<ProviderInfo<?>>> candidates) {

        if (type == null || mt == null || candidates == null || candidates.size() == 0) {
            return;
        }

        SoftReference<ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>> mediaTypeToProviderCacheRef = this.readerProviderCache.get(type);

        if (mediaTypeToProviderCacheRef == null || mediaTypeToProviderCacheRef.get() == null) {

            ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>> mediaTypeToProviderCache = new ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>();

            mediaTypeToProviderCacheRef = new SoftReference<ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>>(mediaTypeToProviderCache);

            readerProviderCache.put(type, mediaTypeToProviderCacheRef);
        }

        if (mediaTypeToProviderCacheRef != null) {
            ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>> mediaTypeToProviderCache = mediaTypeToProviderCacheRef.get();

            if (mediaTypeToProviderCache != null) {
                mediaTypeToProviderCache.put(mt, candidates);
            }
        }
    }

    public void putWriter(Class<?> type, MediaType mt, List<List<ProviderInfo<?>>> candidates) {

        if (type == null || mt == null || candidates == null || candidates.size() == 0) {
            return;
        }

        SoftReference<ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>> mediaTypeToProviderCacheRef = this.writerProviderCache.get(type);

        if (mediaTypeToProviderCacheRef == null || mediaTypeToProviderCacheRef.get() == null) {

            ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>> mediaTypeToProviderCache = new ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>();

            mediaTypeToProviderCacheRef = new SoftReference<ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>>>(mediaTypeToProviderCache);

            writerProviderCache.put(type, mediaTypeToProviderCacheRef);
        }

        if (mediaTypeToProviderCacheRef != null) {
            ConcurrentHashMap<MediaType, List<List<ProviderInfo<?>>>> mediaTypeToProviderCache = mediaTypeToProviderCacheRef.get();

            if (mediaTypeToProviderCache != null) {
                mediaTypeToProviderCache.put(mt, candidates);
            }
        }
    }

    public void destroy() {
        this.readerProviderCache.clear();
        this.writerProviderCache.clear();
    }
}
