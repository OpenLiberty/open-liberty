/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature;

import java.util.EnumSet;

import org.osgi.framework.Version;

public interface FeatureDefinition {

    // NOTE: This is in an exported package for other bundles to use.
    // Do not expose bulk or provisioning methods like getHeaders or getHeaderElements:
    // it reduces our ability to clean up between provisioning operations.

    /**
     * Get the Symbolic Name for this feature, as defined by its header.
     * 
     * @return
     */
    public String getSymbolicName();

    /**
     * Get the Feature Name for this feature.
     * 
     * @return
     */
    public String getFeatureName();

    /**
     * Get the Version for this feature, as defined by its header.
     * 
     * @return
     */
    public Version getVersion();

    /**
     * Get the Visibility for this feature, as defined by its header.
     * 
     * @return
     */
    public Visibility getVisibility();

    /**
     * Get the process types for this feature, as defined by its header.
     * 
     * @return the process type
     */
    public EnumSet<ProcessType> getProcessTypes();

    /**
     * Get the IBM-App-ForceRestart setting for this feature, as defined by its header.
     * 
     * @return
     */
    public AppForceRestart getAppForceRestart();

    /**
     * @return true if this is the kernel feature definition
     */
    public boolean isKernel();

    /**
     * @return
     */
    public String getApiServices();

}
