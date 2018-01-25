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

import java.util.Locale;

import com.ibm.ws.install.InstallLicense;

/**
 *
 */
public interface InstallAsset {

    /**
     * @return the asset display name in specified locale
     */
    public String getDisplayName(Locale locale);

    /**
     * @return the asset display name in English
     */
    public String getDisplayName();

    /**
     * @return the asset reference id. <br/>
     *         e.g.
     *         <ul>
     *         <li>feature: same a the symbolic name</li>
     *         <li>featureCollection: same a the symbolic name</li>
     *         </ul>
     */
    public String getId();

    /**
     * @return the asset description in specified locale
     */
    public String getDescription(Locale locale);

    /**
     * @return the asset description in English
     */
    public String getDescription();

    /**
     * @return the license in specified locale
     */
    public InstallLicense getLicense(Locale locale);

    /**
     * @return the license in English
     */
    public InstallLicense getLicense();

    /**
     * @return size of the asset in bytes
     */
    public long size();
}
