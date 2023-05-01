/*******************************************************************************
 * Copyright (c) 2019,2023 IBM Corporation and others.
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
package com.ibm.ws.artifact.overlay.internal;

import java.io.File;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

//@formatter:off
/**
 * Implementation of a WeakHashMap used as a Set to store all registered DirectoryBasedOverlayContainersImpl
 * created by OverlayContainerFactoryImpl
 */
public class DirectoryBasedOverlayContainerRegistry {
    private static final TraceComponent tc = Tr.register(DirectoryBasedOverlayContainerRegistry.class);

    /**
     * Signature of the base file of a directory overlay container.
     *
     * <code>uniquePath</code> is a locally unique string value which is used
     * as a unique identifier for the base file.  The unique path value
     * has a special relationship to the storage provided by
     * <code>signatures</code>.
     */
    public static class Signature {
        public final String uniquePath;
        public final long size;
        public final long lastModified;

        public Signature(String uniquePath, long size, long lastModified) {
            this.uniquePath = uniquePath;
            this.size = size;
            this.lastModified = lastModified;
        }
    }

    public Signature newSignature(File file) {
        return new Signature( getUniquePath(file), file.length(), file.lastModified() );
    }

    /**
     * Table of active overlay containers.
     *
     * Keys are path values as unique strings.  These keys must never leak outside of their
     * use by the registry.
     *
     * Values are tables of containers and their signatures.
     *
     * Both layers use weak maps.  Removal of all references to the bottom layer container keys
     * removes container and the mapped signature from the bottom layer.
     *
     * Removal of all signatures of a given unique key means no strong references will remain to
     * the unique key, and the mapping from the unique key will be removed along with the now
     * empty signatures bucket.
     */
    private final Map<String, Map<DirectoryBasedOverlayContainerImpl, Signature>> signatures
        = new WeakHashMap<>();

    /**
     * Answer the unique path for a specified file.  That is the
     * unique path based on the absolute path of the file.
     *
     * Answer either the unique path from the signatures collection, or
     * a new unique path.
     *
     * @param file The for which to obtain a unique path.
     *
     * @return A unique path for the file.
     */
    public String getUniquePath(File file) {
        return getUniquePath( file.getAbsolutePath() );
    }

    public String getUniquePath(String path) {
        Map<DirectoryBasedOverlayContainerImpl, Signature> bucket = signatures.get(path);
        if ( bucket != null ) {
            Signature signature = anyValueOf(bucket);
            if ( signature != null ) {
                return signature.uniquePath;
            }
        }
        return new String(path);
    }

    private <K, V> Map.Entry<K, V> createEntry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<K, V>(key, value);
    }

    private <K, V> Map.Entry<K, V> anyOf(Map<K, V> map) {
        for ( Map.Entry<K, V> entry : map.entrySet() ) {
            return createEntry( entry.getKey(), entry.getValue() );
        }
        return null;
    }

    private <K, V> K anyKeyOf(Map<K, V> map) {
        for ( Map.Entry<K, V> entry : map.entrySet() ) {
            return entry.getKey();
        }
        return null;
    }

    private <K, V> V anyValueOf(Map<K, V> map) {
        for ( Map.Entry<K, V> entry : map.entrySet() ) {
            return entry.getValue();
        }
        return null;
    }

    private Map.Entry<DirectoryBasedOverlayContainerImpl, Signature> lookup(Signature gSig) { // 'goal'
        Map<DirectoryBasedOverlayContainerImpl, Signature> bucket = signatures.get(gSig.uniquePath);
        if ( bucket == null ) {
            return null;
        } else if ( bucket.size() == 1 ) {
            return anyOf(bucket);
        }

        DirectoryBasedOverlayContainerImpl n = null; // 'nearest'
        Signature nSig = null;

        boolean multipleError = false;

        for ( Map.Entry<DirectoryBasedOverlayContainerImpl, Signature> entry : bucket.entrySet() ) {
            DirectoryBasedOverlayContainerImpl c = entry.getKey();
            Signature cSig = entry.getValue();

            if ( cSig.lastModified == gSig.lastModified ) {
                // Simplified checking when the times match.
                if ( cSig.size == gSig.size ) {
                    return createEntry(c, cSig); // Exact match: Immediately return.
                } else if ( (n != null) && (nSig.lastModified == gSig.lastModified) ) {
                    continue; // First same time has precedence.
                } else {
                    // Either, the first encountered, or the first
                    // with matching times.  Fall through to assignment.
                }

                // From here on, the candidate time must be different than the goal time.

            } else if ( n == null ) {
                // First encountered is always assigned.
                // Fall through to assignment

            } else if ( nSig.lastModified == gSig.lastModified ) {
                continue; // Nearest is at the exact time and still has precedence.

                // From here on, the nearest time must be different than the goal time.

            } else if ( cSig.lastModified < nSig.lastModified ) {
                // c < n
                if ( gSig.lastModified < cSig.lastModified ) {
                    // g < c < n.  Fall through to assignment
                } else {
                    // c < n < g.  Current nearest has precedence
                    // c < g < n.  Current nearest has precedence.
                    continue;
                }

            } else if ( cSig.lastModified == nSig.lastModified ) {
                // Same time; matching size has precedence.
                if ( nSig.size == gSig.size ) {
                    continue; // Current nearest matched size.
                } else if ( cSig.size != gSig.size ) {
                    continue; // Both don't match size.
                } else {
                    // Candidate matches sizes; current nearest doesn't.
                    // Fall through.
                }

            } else if ( cSig.lastModified > nSig.lastModified ) {
                // n < c
                if ( gSig.lastModified < nSig.lastModified ) {
                    // g < n < c; Current nearest has precedence.
                    continue;
                } else {
                    // n < g < c.  Candidate has precedence.
                    // n < c < g.  Candidate has precedence.
                    // Fall through to assignment.
                }
            }

            n = c;
            nSig = gSig;
        }

        return createEntry(n, nSig);
    }

    //

    public void setSignature(DirectoryBasedOverlayContainerImpl container, File file) {
        String uniquePath = getUniquePath(file);
        Signature signature = newSignature(uniquePath, file);
        synchronized (containers) {
            containers.put(container, signature);
        }
    }

    public Set<DirectoryBasedOverlayContainerImpl> getSnapshotSet() {
        synchronized(containers) {
            return new HashSet<DirectoryBasedOverlayContainerImpl>( containers.keySet() );
        }
    }
}
//@formatter:on
