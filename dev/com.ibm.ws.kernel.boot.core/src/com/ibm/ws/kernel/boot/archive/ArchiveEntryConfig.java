/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

/**
 * Represent an entry adding to Archive
 */
public interface ArchiveEntryConfig {

    /**
     * Get the entry path.
     * 
     * @return
     */
    public String getEntryPath();

    /**
     * Get the source file of the entry.
     * 
     * @return
     */
    public File getSource();

    /**
     * Configure the archive
     * 
     * @param archive
     * @throws IOException
     */
    public void configure(Archive archive) throws IOException;

}
