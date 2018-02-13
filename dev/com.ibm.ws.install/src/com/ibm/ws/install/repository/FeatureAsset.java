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
