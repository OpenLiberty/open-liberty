/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package io.openliberty.microprofile.config.internal_fat.apps.classLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class CustomClassLoaderError extends ClassLoader {

    public CustomClassLoaderError() {

    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        throw new IOException();
    }

}