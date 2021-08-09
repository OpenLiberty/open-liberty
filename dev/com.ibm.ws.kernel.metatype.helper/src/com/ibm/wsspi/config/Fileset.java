/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.config;

import java.io.File;
import java.util.Collection;

/**
 * Provides access to get the directory path or files represented by a &ltfileset/&gt configuration.
 */
public interface Fileset {

    /**
     * Returns the {@link String} path representing the directory of the fileset.
     * 
     * @return the directory path
     */
    String getDir();

    /**
     * This method returns a {@link java.util.Collection} of {@link File} objects determined by the fileset configuration. The returned {@link java.util.Collection} is not updated
     * if the configuration changes
     * or the contents of a monitored directory are udpated. To be notified of
     * these changes use a {@link FilesetChangeListener}.
     * 
     * @return the collection of matching files
     */
    Collection<File> getFileset();
}