/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015,2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.repository.internal;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.repository.FeatureAsset;
import com.ibm.ws.install.repository.FeatureCollectionAsset;
import com.ibm.ws.install.repository.Repository;
import com.ibm.ws.install.repository.RepositoryException;
import com.ibm.ws.kernel.feature.Visibility;

/**
 * This API holds possible command options for a Base Repository.
 */
public abstract class BaseRepository implements Repository {

    protected final Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
    protected String classname = "BaseRepository";

    /**
     * Returns the features of the Base Repository as a collection
     * with the visibility set to PUBLIC
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<FeatureAsset> getFeatures(String productId, String productVersion, String productInstallType, String productLicenseType, String productEdition) {
        return (Collection<FeatureAsset>) getEsaAsset(productId, productVersion, productInstallType, productLicenseType, productEdition,
                                                      Visibility.PUBLIC);
    }

    protected Collection<?> getEsaAsset(String productId, String productVersion, String productInstallType, String productLicenseType, String productEdition,
                                        Visibility visibility) {
        //TODO: override this method for each repository class for unique implementation;
        return null;
    }

    /**
     * Returns the features of the Base Repository as a collection
     * with the visibility set to INSTALL
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<FeatureCollectionAsset> getFeatureCollections(String productId, String productVersion, String productInstallType, String productLicenseType,
                                                                    String productEdition) {
        return (Collection<FeatureCollectionAsset>) getEsaAsset(productId, productVersion, productInstallType, productLicenseType, productEdition,
                                                                Visibility.INSTALL);
    }

    protected void log(String method, String msg) {
        logger.log(Level.FINEST, classname + "." + method + ": " + msg);
    }

    protected void log(String method, String msg, Exception e) {
        if (e == null)
            logger.log(Level.FINEST, classname + "." + method + ": " + msg);
        else
            logger.log(Level.FINEST, classname + "." + method + ": " + msg, e);
    }

    protected RepositoryException createException(String msg) {
        logger.log(Level.SEVERE, msg);
        return new RepositoryException(msg);
    }

}
