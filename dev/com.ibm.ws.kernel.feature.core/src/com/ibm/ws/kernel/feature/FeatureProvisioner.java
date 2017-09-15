/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature;

import java.util.Set;

import org.osgi.framework.Filter;

/**
 * <p>The FeatureProvisioner is a service for querying installed features in the server.</p>
 */
public interface FeatureProvisioner {

    /**
     * <p>Requests the set of features that are installed on the server. The result reported
     * to the caller is never null, but may be an empty set if no features are installed.</p>
     * 
     * @return the set of feature names known to the feature provisioner.
     */
    public Set<String> getInstalledFeatures();

    /**
     * <p>Requests the feature definition for the given feature. If no matching feature is found then
     * null may be returned.
     * </p>
     * 
     * @param featureName The name of the feature.
     * @return the feature definition, or null.
     */
    public FeatureDefinition getFeatureDefinition(String featureName);

    /**
     * TODO: FIXME -- this is for performance
     * 
     * @return
     */
    public String getKernelApiServices();

    /**
     * This method rescans the feature directories and provisions any new auto features
     * whose requirements are satisfied, and de-provisions any whose requirements are no longer satisfied,
     * since the server started.
     * <p>
     * TODO: defer exposing this as SPI until it has been vetted.
     */
    public void refreshFeatures(Filter filter);

    /**
     * This method rescans the feature directories and provisions any new auto features
     * whose requirements are satisfied, and de-provisions any whose requirements are no longer satisfied,
     * since the server started.
     * <p>
     * This has been deprecated in favor of refreshFeatures with a filter
     */
    @Deprecated
    public void refreshFeatures();
}
