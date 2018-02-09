/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.repository;

import java.io.File;
import java.util.zip.ZipFile;

import com.ibm.ws.install.repository.internal.DirectoryRepository;
import com.ibm.ws.install.repository.internal.ZipRepository;

/**
 * This class provides the APIs to get an instance of Repository.
 */
public class RepositoryFactory {

    /**
     * Return the instance of local directory repository.
     *
     * @param repositoryDir a local directory which contains assets.
     * @return an instance of Repository
     */
    public static Repository getInstance(File repositoryDir) throws RepositoryException {
        return new DirectoryRepository(repositoryDir);
    }

    public static Repository getInstance(ZipFile repositoryZip) throws RepositoryException {
        return new ZipRepository(repositoryZip);
    }
}
