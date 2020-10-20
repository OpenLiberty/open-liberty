/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.archive;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Policy;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.security.WLPDynamicPolicy;

/**
 * Create the archive.
 */
public class ArchiveFactory {
    /*
     * This class uses a URLClassLoader and reflection to provide the implementations of the Archive interface.
     * This is done so that archive subclasses (such as ZipArchive) that depend on third party libraries (such
     * as Apache Commons Compress) do not inadvertently make those libraries available to Liberty server and/or
     * application code by being in the JVM's application classpath.
     *
     * Archive implementations should reside in the com.ibm.ws.kernel.boot.archive bundle, and their class names
     * should be declared here.
     */
    private final static String PAX_ARCHIVE_CLASS_NAME = "com.ibm.ws.kernel.boot.archive.internal.PaxArchive";
    private final static String ZIP_ARCHIVE_CLASS_NAME = "com.ibm.ws.kernel.boot.archive.internal.ZipArchive";

    private final static URL ARCHIVE_IMPL_BUNDLE_URL;
    static {
        URL u = ArchiveFactory.class.getProtectionDomain().getCodeSource().getLocation();
        String path = u.toExternalForm();
        path = path.replaceAll("com.ibm.ws.kernel.boot_", "com.ibm.ws.kernel.boot.archive_");
        path = path.replaceAll("/com.ibm.ws.kernel.boot.core/build/classes/", "/com.ibm.ws.kernel.boot.archive/build/classes/");
        try {
            u = new URL(path);
        } catch (MalformedURLException e) {
            Debug.printStackTrace(e);
            u = null;
        }
        ARCHIVE_IMPL_BUNDLE_URL = u;
    }

    public static Archive create(final String archivePath) throws IOException {
        File archiveFile = new File(archivePath);
        return create(archiveFile);
    }

    public static Archive create(final File archiveFile) throws IOException {
        if (!archiveFile.isAbsolute())
            throw new IllegalArgumentException(archiveFile.getPath());

        String className;
        if (archiveFile.getName().endsWith(".pax")) {
            className = PAX_ARCHIVE_CLASS_NAME;
        } else {
            className = ZIP_ARCHIVE_CLASS_NAME;
        }
        // could add other archive type in future

        try {
            @SuppressWarnings("resource")
            URLClassLoader loader = new URLClassLoader(new URL[] { ARCHIVE_IMPL_BUNDLE_URL });
            @SuppressWarnings("unchecked")
            Class<? extends Archive> archiveImplClass = (Class<? extends Archive>) loader.loadClass(className);
            Constructor<? extends Archive> ctor = archiveImplClass.getConstructor(File.class);
            return ctor.newInstance(archiveFile);
        } catch (Exception ex) {
            throw new IOException(ex);
        }

    }

    /**
     * Perform the create by applying the archive impl to the security policy - package minify
     *
     * @param archiveFile
     * @param j2security
     * @return
     * @throws IOException
     */
    public static Archive create(final File archiveFile, boolean j2security) throws IOException {
        Archive arch = null;
        if (j2security == true) {
            List<URL> urlList = new ArrayList<URL>();
            urlList.add(ARCHIVE_IMPL_BUNDLE_URL);
            Policy wlpPolicy = new WLPDynamicPolicy(Policy.getPolicy(), urlList);
            Policy.setPolicy(wlpPolicy);
            wlpPolicy.refresh();
            arch = create(archiveFile);
        } else {
            arch = create(archiveFile);
        }
        return arch;
    }
}
