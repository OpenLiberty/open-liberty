/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature;

import java.util.EnumSet;

import org.osgi.framework.Version;

/**
 * Base feature definition.
 *
 * This interface defines a lightweight API which is exported across
 * bundles.
 *
 * Provisioning APIs must not be added to this interface. That reduces
 * the ability to cleanup between provisioning operations.
 */
public interface FeatureDefinition {
    /**
     * Answer the symbolic name of this feature.
     *
     * The feature symbolic name is stored as a header attribute.
     *
     * @return The symbolic name of this feature.
     */
    String getSymbolicName();

    /**
     * Answer the name of this feature.
     *
     * @return The name of this feature.
     */
    String getFeatureName();

    /**
     * Answer the version of this feature.
     *
     * The feature version is stored as a header attribute.
     *
     * @return The version of this feature.
     */
    Version getVersion();

    /**
     * Answer the visibility of this feature.
     *
     * The feature visibility is stored as a header attribute.
     *
     * @return The visibility of this feature.
     */
    Visibility getVisibility();

    /**
     * Answer the types of processes on which this feature may
     * be provisioned.
     *
     * The process types are stored in a header attribute.
     *
     * @return The types of processes on which this feature may
     *         be provisioned.
     */
    EnumSet<ProcessType> getProcessTypes();

    /**
     * Answer the setting of whether updates to this feature
     * require applications to be restarted.
     *
     * There are several defined behaviors; see {@link AppForceRestart}
     * for more details.
     *
     * @return The setting of whether updates to this feature
     *         require applications to be restarted.
     */
    AppForceRestart getAppForceRestart();

    /**
     * Tell if this is a kernel feature.
     *
     * @return True or false telling if this is a kernel feature.
     */
    boolean isKernel();

    String getApiServices();
}
