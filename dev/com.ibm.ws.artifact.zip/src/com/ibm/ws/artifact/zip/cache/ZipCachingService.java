/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.cache;

import java.io.IOException;

/**
 * <p>Interface for the zip file handle and zip file caching service.</p>
 */
public interface ZipCachingService {
    /**
     * <p>Obtain a zip file handle for a specified path.</p>
     * 
     * <p>Callers are encouraged to generate paths in canonical form.  The
     * path as given is used as the key for cached handles and for opening
     * the zip file.</p>
     * 
     * <p>Callers for the same path values are not guaranteed to obtain
     * identical zip file handles.  Zip file handles are discarded and
     * recreated as needed within a configured cache size.</p>
     * 
     * @param path The path of the zip file handle.
     * 
     * @return A zip file handle for the path.
     * 
     * @throws IOException Thrown if a handle could not be obtained.
     */
    ZipFileHandle openZipFile(String path) throws IOException;
}
