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

import java.io.File;

/**
 * A quick interface to represent native libraries for the native library adapter.
 */
public interface NativeLibrary {
    /**
     * Obtain the File on disk representing this library
     * 
     * @return File of library.
     */
    public File getLibraryFile();
}
