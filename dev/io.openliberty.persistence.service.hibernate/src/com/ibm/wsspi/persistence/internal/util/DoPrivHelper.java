/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
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
package com.ibm.wsspi.persistence.internal.util;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.persistence.InMemoryMappingFile;

@Trivial
public class DoPrivHelper {

    public static PersistenceClassLoader newPersistenceClassLoader(ClassLoader root, Class<?>... del) {
        return AccessController.doPrivileged((PrivilegedAction<PersistenceClassLoader>) () -> {
            ClassLoader[] loaders = Arrays.stream(del).map(Class::getClassLoader).toArray(ClassLoader[]::new);
            return new PersistenceClassLoader(root, loaders);
        });
    }

    public static PersistenceClassLoader newPersistenceClassLoader(ClassLoader root, ClassLoader... delegates) {
        return AccessController.doPrivileged((PrivilegedAction<PersistenceClassLoader>) () -> new PersistenceClassLoader(root, delegates));
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public static FileInputStream newFileInputStream(File f) {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<FileInputStream>) () -> new FileInputStream(f));
        } catch (PrivilegedActionException e) {
            return null;
        }
    }

    public static URL newInMemoryMappingFileURL(InMemoryMappingFile immf) {
        return AccessController.doPrivileged((PrivilegedAction<URL>) () -> {
            try {
                return new URL("persistence-service", "", 777, immf.getName());
            } catch (MalformedURLException e) {
                // unexpected
                throw new RuntimeException(e);
            }
        });
    }

}
