/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.interfaces;

import java.util.Collection;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;

/**
 *
 */
public interface Application {
    /**
     * Get the J2EEName of the application
     *
     * @return the J2EEName of the application
     */
    J2EEName getJ2EEName();

    /**
     * Return true if this application has at least one module
     *
     * @return true if this application has at least one module
     * @throws CDIException
     */
    boolean hasModules() throws CDIException;

    /**
     * Get the Application ClassLoader
     *
     * @return the Application ClassLoader
     * @throws CDIException
     */
    ClassLoader getClassLoader() throws CDIException;

    /**
     * Get the type of the application (currently either EAR or WAR)
     *
     * @return the type of the application
     */
    ApplicationType getType();

    /**
     * Get a Collection of all Library Archives within this application (i.e. not modules)
     *
     * @return a Collection of all Library Archives
     * @throws CDIException
     */
    Collection<CDIArchive> getLibraryArchives() throws CDIException;

    /**
     * Get a Collection of all Module Archives within this application
     *
     * @return a Collection of all Module Archives
     * @throws CDIException
     */
    Collection<CDIArchive> getModuleArchives() throws CDIException;

    /**
     * Get the name of the application
     *
     * @return the name of the Application
     */
    String getName();

    /**
     * Get the ApplicationMetaData for the application
     *
     * @return the ApplicationMetaData
     */
    ApplicationMetaData getApplicationMetaData();

}
