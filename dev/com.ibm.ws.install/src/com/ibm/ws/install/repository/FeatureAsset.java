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

import java.util.Collection;

/**
 *
 */
public interface FeatureAsset extends InstallAsset {

    /**
     * @return the short name of the feature if available, otherwise null.
     */
    public String getShortName();

    /**
     * Gets the name of the feature
     * 
     * @return The name of the feature
     */
    public String getProvideFeature();

    /**
     * Gets the list of required features for this feature
     * 
     * @return The list of required features defined on this feature
     */
    public Collection<String> getRequireFeature();
}
