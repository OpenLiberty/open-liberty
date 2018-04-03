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
