/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.parsers;

import java.io.File;

import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;

/**
 * Instances of this interface are responsible for uploading artifacts into the Liberty Repository. An instance will be asked if it can handle a particular file before being asked
 * to upload it. The file passed to methods on this interface will either be an asset binary with a sibling metadata zip file OR just the metadata zip itself.
 *
 * @param <T>
 */
public interface Parser<T extends RepositoryResourceWritable> {

    /**
     * Parses the <code>assetFile</code> to resource ready to be uploaded.
     *
     * @param assetFile The file that we want to parse
     * @param metadataFile The metadata.zip file location for the asset or null if it is co-located with the assetFile
     * @param contentUrl The URL pointing to the content. If it is <code>null</code> the value supplied in "downloadURL" the repository metadata ZIP will be used and if that is
     *            also <code>null</code> then the content file will be uploading into the repository.
     * @throws RepositoryException if something goes wrong writing the file to the repository
     */
    public T parseFileToResource(File assetFile, File metadataFile, String contentUrl) throws RepositoryException;

    /**
     * Clean up any temporary files created while parsing the resource
     */
    public void tidyUp();

}
