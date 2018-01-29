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
package com.ibm.ws.install;

import java.util.Locale;

/**
 * This class provides the APIs to get an instance of installed asset.
 */
public interface InstalledAsset {

    /**
     * @return the display name of the installed asset
     */
    public String getDisplayName();

    /**
     * @return the display name of the installed asset in the specified Locale
     */
    public String getDisplayName(Locale locale);

    /**
     * @return the short description of the installed asset
     */
    public String getShortDescription();

    /**
     * @return the short description of the installed asset in the specified Locale
     */
    public String getShortDescription(Locale locale);

    /**
     * @return true if the visibility of the installed asset is public or install
     */
    public boolean isPublic();

    /**
     * @return the product id of the installed asset
     */
    public String getProductId();
}
