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
package com.ibm.ws.artifact.file.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.ibm.ws.artifact.file.ContainerFactoryHolder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

public class FileEntry implements com.ibm.wsspi.artifact.ArtifactEntry {

    private final ContainerFactoryHolder containerFactoryHolder;
    private final ArtifactContainer enclosingContainer;
    private final File file;
    private final FileContainer root;

    /**
     * Builds an Entry for a given File.
     * 
     * @param e {@link ArtifactContainer} that wraps this {@link Entry}.
     * @param f {@link File} representing this {@link ArtifactEntry} on disk.
     * @param r {@link FileContainer} representing the root of the heirarchy this Entry is part of.
     * @param c Instance of {@link ContainerFactoryHolder} to obtain {@link ArtifactContainerFactory} from
     */
    FileEntry(ArtifactContainer e, File f, FileContainer r, ContainerFactoryHolder c) {
        file = f;
        enclosingContainer = e;
        if (enclosingContainer == null) {
            throw new IllegalArgumentException();
        }
        containerFactoryHolder = c;
        root = r;
    }

    @Override
    public ArtifactContainer getEnclosingContainer() {
        return enclosingContainer;
    }

    @Override
    public String getPath() {
        //determine this Entries path by using path for the enclosing container & adding our name            
        String path = enclosingContainer.getPath();
        if (!path.equals("/")) {
            path += "/" + file.getName();
        } else {
            path += file.getName();
        }
        return path;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public ArtifactContainer convertToContainer() {
        return convertToContainer(false);
    }

    @Override
    public ArtifactContainer convertToContainer(boolean localOnly) {
        ArtifactContainer rv = null;

        //if its a dir.. go convert it immediately to a FileContainer.
        if (FileUtils.fileIsDirectory(file)) {
            File newCacheDir = null;
            String relativeLocation = enclosingContainer.getPath();
            if (relativeLocation.equals("/")) {
                newCacheDir = root.getCacheDir();
            } else {
                //use of substring 1 is ok here, because thisentry MUST be within a container, and the smallest path
                //as container can have is "/", which is dealt with above, therefore, in this branch the relativeLocation MUST 
                //be longer than "/"
                newCacheDir = new File(root.getCacheDir(), relativeLocation.substring(1));
            }
            //newCacheDir = new File(newCacheDir, this.getName());
            rv = new FileContainer(newCacheDir, enclosingContainer, this, file, containerFactoryHolder, false, root);
        } else if (!localOnly) {
            //Let other people have a crack at the conversion..
            rv = containerFactoryHolder.getContainerFactory().getContainer(new File(root.getCacheDir(), enclosingContainer.getPath()), enclosingContainer, this, file);
        }

        return rv;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (FileUtils.fileIsFile(file))
            return FileUtils.getInputStream(file);
        else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public long getSize() {
        if (FileUtils.fileIsFile(file) && FileUtils.fileExists(file))
            return FileUtils.fileLength(file);
        else
            return 0L;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer getRoot() {
        return root;
    }

    /** {@inheritDoc} */
    @Override
    public long getLastModified() {
        return AccessController.doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return file.lastModified();
            }

        });
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(PrivilegedActionException.class)
    public URL getResource() {
        try {
            URL url = (URL) AccessController.doPrivileged(
                            new PrivilegedExceptionAction() {
                                @Override
                                public URL run() throws MalformedURLException {
                                    return file.toURI().toURL();
                                }
                            }
                            );
            return url;
        } catch (PrivilegedActionException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getPhysicalPath() {
        return file.getAbsolutePath();
    }

}
