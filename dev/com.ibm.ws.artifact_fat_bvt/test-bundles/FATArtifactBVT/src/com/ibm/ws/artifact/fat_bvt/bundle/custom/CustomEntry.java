/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.bundle.custom;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;

/**
 * Simple property based entry.
 */
public class CustomEntry implements ArtifactEntry {

    protected CustomEntry(CustomContainer parent, String name, String content) {
        this.parent = parent;

        this.name = name;
        this.path = "/" + name;

        this.content = content;
    }

    //

    private final CustomContainer parent;

    @Override
    public CustomContainer getRoot() {
        return parent;
    }

    @Override
    public CustomContainer getEnclosingContainer() {
        return parent;
    }

    //

    private final String name;
    private final String path;
    private final String content;

    //

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    //

    @Deprecated
    @Override
    public String getPhysicalPath() {
        return null;
    }

    @Override
    public URL getResource() {
        return null;
    }

    //

    @Override
    public ArtifactContainer convertToContainer() {
        return null;
    }

    @Override
    public ArtifactContainer convertToContainer(boolean localOnly) {
        return null;
    }

    //

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content.getBytes());
    }

    @Override
    public long getSize() {
        return content.getBytes().length;
    }

    @Override
    public long getLastModified() {
        return 1337;
    }
}
