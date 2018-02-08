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
public interface Repository {

    /**
     * @param productId
     * @param productVersion
     * @param productInstallType
     * @param productLcenseType
     * @param productEdition
     * @return the collection of features which are applicable to the provided information
     */
    public Collection<FeatureAsset> getFeatures(String productId, String productVersion, String productInstallType, String productLicenseType, String productEdition);

    /**
     * @param productId
     * @param productVersion
     * @param productInstallType
     * @param productLcenseType
     * @param productEdition
     * @return the collection of feature collection which are applicable to the provided information
     */
    public Collection<FeatureCollectionAsset> getFeatureCollections(String productId, String productVersion, String productInstallType, String productLcenseType,
                                                                    String productEdition);

}
