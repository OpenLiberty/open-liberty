/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package com.ibm.ws.kernel.internal.classloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.Certificate;

/**
 */
public class DirectoryResourceEntry implements ResourceEntry {

    private final DirectoryResourceHandler handler;
    private final File file;

    public DirectoryResourceEntry(DirectoryResourceHandler handler, File file) {
        this.handler = handler;
        this.file = file;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return handler;
    }

    @Override
    public Certificate[] getCertificates() {
        return null;
    }

    @Override
    public byte[] getBytes() throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            return JarFileClassLoader.getBytes(in, file.length());
        } finally {
            JarFileClassLoader.close(in);
        }
    }

    @Override
    public URL toURL() {
        return JarFileClassLoader.toURL(file);
    }
}
