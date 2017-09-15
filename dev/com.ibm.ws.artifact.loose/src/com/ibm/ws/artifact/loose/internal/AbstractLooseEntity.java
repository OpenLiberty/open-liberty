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
package com.ibm.ws.artifact.loose.internal;

import com.ibm.ws.artifact.loose.internal.LooseArchive.EntryInfo;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.EnclosedEntity;

public abstract class AbstractLooseEntity implements EnclosedEntity {
    private final LooseArchive root;
    private final String path;
    private String name;
    private final EntryInfo entry;

    public AbstractLooseEntity(LooseArchive looseArchive, EntryInfo ei, String pathAndName) {
        path = pathAndName;
        root = looseArchive;
        entry = ei;
    }

    @Override
    public ArtifactContainer getEnclosingContainer() {
        final String parentPath = PathUtil.getParent(path);
        if ("/".equals(parentPath)) {
            return root;
        } else {
            return root.getEntry(parentPath).convertToContainer();
        }
    }

    public ArtifactEntry getEntryInEnclosingContainer() {
        if ("/".equals(path)) {
            return root.getEntryInEnclosingContainer();
        } else {
            return root.getEntry(path);
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getName() {
        if (name == null) {
            name = PathUtil.getName(path);
        }

        return name;
    }

    protected LooseArchive getParent() {
        return root;
    }

    protected EntryInfo getEntryInfo() {
        return entry;
    }
}