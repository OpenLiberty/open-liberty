/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.url;

import java.io.File;

public interface WSJarURLConnection {
    /**
     * Returns a File object referencing the archive addressed by the wsjar URL.
     * 
     * @return
     */
    public File getFile();

    /**
     * Returns a String containing the archive path component of the wsjar URL. This is the path component
     * following the !/ separator.
     * 
     * @return
     */
    public String getEntry();
}
