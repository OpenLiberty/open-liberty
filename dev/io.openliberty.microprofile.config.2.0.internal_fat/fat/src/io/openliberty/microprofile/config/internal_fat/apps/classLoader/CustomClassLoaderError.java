/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.classLoader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

public class CustomClassLoaderError extends URLClassLoader {

    /**
     * @param urls
     */
    public CustomClassLoaderError(URL[] urls) {
        super(urls);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        throw new IOException();
    }

}