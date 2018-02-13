/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.transport.model;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class FilterVersion {
    String value;
    boolean inclusive;
    String label;
    String compatibilityLabel;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean getInclusive() {
        return inclusive;
    }

    public void setInclusive(boolean inclusive) {
        this.inclusive = inclusive;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (inclusive ? 1231 : 1237);
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FilterVersion other = (FilterVersion) obj;
        if (inclusive != other.inclusive)
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (compatibilityLabel == null) {
            if (other.compatibilityLabel != null)
                return false;
        } else if (!compatibilityLabel.equals(other.compatibilityLabel))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the compatibilityLabel
     */
    public String getCompatibilityLabel() {
        return compatibilityLabel;
    }

    /**
     * @param compatibilityLabel the compatibilityLabel to set
     */
    public void setCompatibilityLabel(String compatibilityLabel) {
        this.compatibilityLabel = compatibilityLabel;
    }

    /**
     * This method creates a version range from the supplied min and max FilterVersion
     *
     * @param minVersion The min version in the range. Can be null, if so treated as {@link Version#emptyVersion}
     * @param maxVersion The max version. Can be null, null in a version range is treated as infinite
     * @return A version range object representing the min and max values supplied
     */
    public static VersionRange getFilterRange(FilterVersion minVersion, FilterVersion maxVersion) {
        VersionRange vr = null;
        Version vmin = minVersion == null ? Version.emptyVersion : new Version(minVersion.getValue());
        Version vmax = maxVersion == null ? null : new Version(maxVersion.getValue());
        char leftType = (minVersion == null || minVersion.getInclusive()) ? VersionRange.LEFT_CLOSED : VersionRange.LEFT_OPEN;
        char rightType = (maxVersion == null || maxVersion.getInclusive()) ? VersionRange.RIGHT_CLOSED : VersionRange.RIGHT_OPEN;
        vr = new VersionRange(leftType, vmin, vmax, rightType);
        return vr;
    }
}
