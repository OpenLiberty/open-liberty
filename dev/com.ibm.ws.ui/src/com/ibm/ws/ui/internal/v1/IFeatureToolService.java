/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.ui.internal.v1.pojo.FeatureTool;

/**
 *
 */
public interface IFeatureToolService {

    /**
     * Returns the list of feature tools available in the installation
     * that should be loaded, expressed in the JVM's default Locale.
     *
     * @return The set of tools. Will not return {@code null}.
     */
    Set<FeatureTool> getTools();

    /**
     * Returns the value of the Subsystem-Icon header for the required feature.
     *
     * @return A Map whose key is the feature name, and whose value is the subsystem icon header from the relevant manifest.
     */

    Map<String, String> getIconMap();

    /**
     * Returns the icon install location for the feature.
     *
     * @param feature - A String representing the Feature symbolicname
     * @return A String containing the directory that the feature icons are installed into, or null if the feature doesn't exist.
     */
    String getFeatureIconInstallDir(String featureSymbolicName);

    /**
     * Obtains a List of all FeatureTools expressed in the current request Locale.
     *
     * @return The list of tools. Will not return {@code null}.
     */
    List<FeatureTool> getToolsForRequestLocale();

    /**
     * Obtains a single FeatureTool expressed in the current request Locale.
     *
     * @param id
     * @return The FeatureTool
     */
    FeatureTool getToolForRequestLocale(String id);

    /**
     * Returns {@code true} if the feature is provisioned in the server runtime.
     *
     * @param featureToFind
     * @return {@code true} if the feature is provisioned in the server runtime, {@code false} otherwise
     */
    boolean isFeatureProvisioned(String featureToFind);
}
