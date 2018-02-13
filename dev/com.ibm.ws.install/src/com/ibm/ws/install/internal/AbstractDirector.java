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
package com.ibm.ws.install.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.CancelException;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * This abstract class contains APIs for obtaining installation information.
 */
public abstract class AbstractDirector {

    static final String DEFAULT_TO_EXTENSION = "default";

    Product product;
    EventManager eventManager;
    Logger logger = null;

    boolean enableEvent = true;

    /**
     *
     * @param product
     * @param eventManager
     * @param logger
     */
    AbstractDirector(Product product, EventManager eventManager, Logger logger) {
        this.product = product;
        this.eventManager = eventManager;
        this.logger = logger;
    }

    /**
     * Creates a Progress event message.
     *
     * @param state the state integer
     * @param progress the progress integer
     * @param message the message to be displayed
     */
    void fireProgressEvent(int state, int progress, String message) {
        try {
            fireProgressEvent(state, progress, message, false);
        } catch (InstallException e) {
        }
    }

    /**
     * Creates a Progress event message that can handle cancel exceptions.
     *
     * @param state the state integer
     * @param progress the progress integer
     * @param message the message to be displayed
     * @param allowCancel if cancel exceptions should be handled
     * @throws InstallException
     */
    void fireProgressEvent(int state, int progress, String message, boolean allowCancel) throws InstallException {
        log(Level.FINEST, message);
        if (enableEvent) {
            try {
                eventManager.fireProgressEvent(state, progress, message);
            } catch (CancelException ce) {
                if (allowCancel)
                    throw ce;
                else
                    log(Level.FINEST, "fireProgressEvent caught cancel exception: " + ce.getMessage());
            } catch (Exception e) {
                log(Level.FINEST, "fireProgressEvent caught exception: " + e.getMessage());
            }
        }
    }

    /**
     * Logs a message.
     *
     * @param level the level of the message
     * @param msg the message
     */
    void log(Level level, String msg) {
        if (msg != null && !msg.isEmpty())
            logger.log(level, msg);
    }

    /**
     * Logs a message with an exception.
     *
     * @param level the level of the message
     * @param msg the message
     * @param e the exception causing the message
     */
    void log(Level level, String msg, Exception e) {
        if (e != null)
            logger.log(level, msg, e);
    }

    /**
     * Checks if installResources map contains any resources
     *
     * @param installResources the map of installResources
     * @return true if all lists in the map are empty
     */
    boolean isEmpty(Map<String, List<List<RepositoryResource>>> installResources) {
        if (installResources == null)
            return true;
        for (List<List<RepositoryResource>> targetList : installResources.values()) {
            for (List<RepositoryResource> mrList : targetList) {
                if (!mrList.isEmpty())
                    return false;
            }
        }
        return true;
    }

    /**
     * Checks if installResources contains any resources
     *
     * @param installResources the list of lists containing Install Resources
     * @return true if all lists are empty
     */
    boolean isEmpty(List<List<RepositoryResource>> installResources) {
        if (installResources == null)
            return true;
        for (List<RepositoryResource> mrList : installResources) {
            if (!mrList.isEmpty())
                return false;
        }
        return true;
    }

    /**
     * Checks if the feature is in installedFeatures
     *
     * @param installedFeatures the map of installed features
     * @param feature the feature to look for
     * @return true if feature is in installedFeatures
     */
    boolean containFeature(Map<String, ProvisioningFeatureDefinition> installedFeatures, String feature) {
        if (installedFeatures.containsKey(feature))
            return true;
        for (ProvisioningFeatureDefinition pfd : installedFeatures.values()) {
            String shortName = InstallUtils.getShortName(pfd);
            if (shortName != null && shortName.equalsIgnoreCase(feature))
                return true;
        }
        return false;
    }

    /**
     * Creates a collection of features that still need to be installed
     *
     * @param requiredFeatures Collection of all features that should be installed
     * @param download if all features (including already installed) should be downloaded
     * @return The subset of requiredFeatures containing features that still need to be installed
     */
    Collection<String> getFeaturesToInstall(Collection<String> requiredFeatures, boolean download) {
        Collection<String> featuresToInstall = new ArrayList<String>(requiredFeatures.size());
        if (!requiredFeatures.isEmpty()) {
            Map<String, ProvisioningFeatureDefinition> installedFeatures = product.getFeatureDefinitions();
            for (String feature : requiredFeatures) {
                if (download || !containFeature(installedFeatures, feature))
                    featuresToInstall.add(feature);
            }
        }
        return featuresToInstall;
    }

    /**
     * Checks if filesInstalled includes scripts in a bin folder.
     *
     * @param filesInstalled the list of files that are installed
     * @return true if at least one file is in the bin path
     */
    boolean containScript(List<File> filesInstalled) {
        for (File f : filesInstalled) {
            String path = f.getAbsolutePath().toLowerCase();
            if (path.contains("/bin/") || path.contains("\\bin\\")) {
                return true;
            }
        }
        return false;
    }
}
