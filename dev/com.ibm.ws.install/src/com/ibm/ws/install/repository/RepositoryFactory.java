/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
