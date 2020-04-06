/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

import com.ibm.ws.classloading.LibertyClassLoader;

public abstract class LibertyLoader extends LibertyClassLoader implements DeclaredApiAccess {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    public LibertyLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected abstract Class<?> findClass(String className) throws ClassNotFoundException;

    @Override
    protected URL findResource(String resName) {
        return super.findResource(resName);
    }

    @Override
    protected Enumeration<URL> findResources(String resName) throws IOException {
        return super.findResources(resName);
    }

    public abstract Bundle getBundle();
}
