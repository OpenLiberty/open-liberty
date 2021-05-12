/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.utils;

import org.osgi.framework.Version;

/**
 * This class store the bundle SymbolicName, version and Web-ContextPath, and allows it to be
 * sorted in a list. We sort by SymbolicName and version.
 */
public class SortedBundle implements Comparable<SortedBundle> {

    private final String symbolicName;
    private final Version version;
    private final String webContextPath;
    private final String primaryEndpointFeatureName;

    public SortedBundle(String symbolicName, Version version, String webContextPath, String primaryEndpointFeatureName) {
        this.symbolicName = symbolicName;
        this.version = version;
        this.webContextPath = webContextPath;
        this.primaryEndpointFeatureName = primaryEndpointFeatureName;
    }

    public String getSymbolicName() {
        return this.symbolicName;
    }

    public Version getVersion() {
        return this.version;
    }

    public String getWebContextPath() {
        return this.webContextPath;
    }

    public boolean isUIEndpoint() {
        return this.primaryEndpointFeatureName != null;
    }

    /**
     * @return the primaryEndpointFeatureName
     */
    public String getPrimaryEndpointFeatureName() {
        return primaryEndpointFeatureName;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(SortedBundle bundleToCompare) {
        int result = this.symbolicName.compareToIgnoreCase(bundleToCompare.getSymbolicName());

        if (result == 0)
            result = this.version.compareTo(bundleToCompare.version);

        return result;
    }

    @Override
    public String toString() {
        return ("SymbolicName: " + this.symbolicName + " Version: " + this.version + " WebContextPath: " + this.webContextPath +
                " PrimaryEndpointFeatureName: " + primaryEndpointFeatureName);
    }

    @Override
    public boolean equals(final Object obj) {
        boolean result;
        if (obj == null) {
            result = false;
        } else if (obj == this) {
            result = true;
        } else if (obj instanceof SortedBundle) {
            result = this.compareTo((SortedBundle) obj) == 0;
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public int hashCode() {
        int result = 19;
        result = 31 * result + new Integer(symbolicName).intValue();
        result = 31 * result + new Integer(version.toString()).intValue();
        result = 31 * result + new Integer(webContextPath).intValue();
        result = 31 * (primaryEndpointFeatureName == null ? 3 : new Integer(primaryEndpointFeatureName).intValue());
        return result;
    }
}