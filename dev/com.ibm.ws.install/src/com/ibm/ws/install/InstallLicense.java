/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
