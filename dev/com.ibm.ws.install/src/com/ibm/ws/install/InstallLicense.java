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
package com.ibm.ws.install;

import java.util.Collection;

/**
 * This interface provides APIs for Installation Licenses.
 */
public interface InstallLicense {

    /**
     * Return the license id
     *
     * @return The license id which is the Subsystem-License attribute
     *         of the subsystem.mf file
     */
    public String getId();

    /**
     * Return the license type
     *
     * @return The license type which is from the assetInfo.properties in the zip to upload
     */
    public String getType();

    /**
     * Return the license name
     *
     * @return The license name which is from the first line of the agreement
     */
    public String getName();

    /**
     * Return the program name
     *
     * @return The program name which is from the line containing "Program Name:"
     *         of the license information
     */
    public String getProgramName();

    /**
     * Return the license information
     *
     * @return The text of license information
     */
    public String getInformation();

    /**
     * Return the license agreement
     *
     * @return The text of license agreement
     */
    public String getAgreement();

    /**
     * Return the features use this license
     *
     * @return an Collection of feature names
     */
    Collection<String> getFeatures();

}
