/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.internal.classloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

/**
 */
public class DirectoryResourceHandler implements ResourceHandler {

    private final File directory;
    private final String directoryPath;
    private URL url;
    private boolean manifestLoaded;
    private Manifest manifest;

    public DirectoryResourceHandler(File directory) {
        this.directory = directory;
        this.directoryPath = getCanonicalPath(directory);
    }

    @Override
    public void close() throws IOException {}

    @Override
    public ResourceEntry getEntry(String name) {
        File file = new File(directory, name);
        if (file.exists() && getCanonicalPath(file).startsWith(directoryPath)) {
            return new DirectoryResourceEntry(this, file);
        } else {
            return null;
        }
    }

    @Override
    public URL toURL() {
        if (url == null) {
            url = JarFileClassLoader.toURL(directory);
        }
        return url;
    }

    @Override
    public Manifest getManifest() throws IOException {
        if (!manifestLoaded) {
            File file = new File(directory, "META-INF/MANIFEST.MF");
            if (file.exists()) {
                InputStream in = null;
                try {
                    in = new FileInputStream(file);
                    manifest = new Manifest(in);
                } finally {
                    JarFileClassLoader.close(in);
                }
            }
            manifestLoaded = true;
        }
        return manifest;
    }

    private static String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            // really?
            throw new Error(e);
        }
    }
}
