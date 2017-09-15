/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.ear.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.adaptable.module.structure.StructureHelper;
import com.ibm.wsspi.artifact.ArtifactContainer;

final class EARStructureHelper implements StructureHelper {
    private static final EARStructureHelper unknownRootInstance = new EARStructureHelper(null);

    public static StructureHelper getUnknownRootInstance() {
        return unknownRootInstance;
    }

    public static EARStructureHelper create(Collection<String> rootPaths) {
        Map<String, Object> rootPathTree = new HashMap<String, Object>();
        for (String path : rootPaths) {
            addRootPath(rootPathTree, path);
        }

        return new EARStructureHelper(rootPathTree);
    }

    private static void addRootPath(Map<String, Object> rootPathTree, String path) {
        Map<String, Object> node = rootPathTree;

        String[] parts = path.split("/+");
        if (parts.length > 0) {
            int begin = parts[0].isEmpty() ? 1 : 0;
            if (begin < parts.length) {
                for (int i = begin; i < parts.length - 1; i++) {
                    Object nextNode = node.get(parts[i]);
                    if (nextNode == RootStatus.Root) {
                        return;
                    }

                    if (nextNode != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> uncheckedNode = (Map<String, Object>) nextNode;
                        node = uncheckedNode;
                    } else {
                        Map<String, Object> newNode = new HashMap<String, Object>();
                        node.put(parts[i], newNode);
                        node = newNode;
                    }
                }

                node.put(parts[parts.length - 1], RootStatus.Root);
            }
        }
    }

    private static boolean isContainerWithinTopPathSpace(ArtifactContainer artifactContainer) {
        return artifactContainer.getRoot().getEnclosingContainer() == null;
    }

    /**
     * A map of root paths. The key is the entry basename, and the value is
     * either RootStatus.Root if the entry is a root or another map of roots.
     */
    private final Map<String, Object> rootPathTree;

    private EARStructureHelper(Map<String, Object> rootPathTree) {
        this.rootPathTree = rootPathTree;
    }

    private static boolean isRootName(String name) {
        return name.endsWith(".war") ||
               name.endsWith(".jar") ||
               name.endsWith(".rar");
    }

    enum RootStatus {
        None,
        Root,
        ParentRoot
    }

    private RootStatus pathContainsRoot(String path) {
        // Ensure no parent path contains a root.  That is, allow "/dir/x.jar"
        // but disallow "/x.war/WEB-INF/y.jar".
        String[] parts = path.split("/");
        Map<String, Object> node = rootPathTree;
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                if (rootPathTree == null) {
                    // We don't know which paths are actually roots, so guess
                    // based on the entry name.
                    if (isRootName(parts[i])) {
                        return i == parts.length - 1 ? RootStatus.Root : RootStatus.ParentRoot;
                    }
                } else {
                    Object nextNode = node.get(parts[i]);
                    if (nextNode == null) {
                        return RootStatus.None;
                    }

                    if (nextNode == RootStatus.Root) {
                        return i == parts.length - 1 ? RootStatus.Root : RootStatus.ParentRoot;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> uncheckedNode = (Map<String, Object>) nextNode;
                    node = uncheckedNode;
                }
            }
        }

        return RootStatus.None;
    }

    @Override
    public boolean isRoot(ArtifactContainer artifactContainer) {
        if (!isContainerWithinTopPathSpace(artifactContainer)) {
            // We only interact with the top path space.
            return false;
        }

        String path = artifactContainer.getPath();
        return pathContainsRoot(path) == RootStatus.Root;
    }

    @Override
    public boolean isValid(ArtifactContainer artifactContainer, String path) {
        if (!isContainerWithinTopPathSpace(artifactContainer)) {
            // We only interact with the top path space.
            return true;
        }

        // If the path is absolute, then we're evaluating within the context of
        // a container that might be a virtual root.
        if (!path.startsWith("/")) {
            String acPath = artifactContainer.getPath();
            if (pathContainsRoot(acPath) != RootStatus.None) {
                // The caller already did a getEntry(acPath).adapt(Container.class)
                // sequence followed by isRoot/isValid, so allow all subdirectories.
                return true;
            }
        }

        // Don't allow subdirectories of virtual roots.
        return pathContainsRoot(path) != RootStatus.ParentRoot;
    }
}