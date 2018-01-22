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
package com.ibm.ws.repository.resources.internal;

import java.util.Collection;

import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;

/**
 * This is an extension to the {@link RepositoryResourceMatchingData} to hold a provide feature, version and applies to filter info fields.
 */
public class ExtendedMatchingData extends RepositoryResourceMatchingData {
    String provideFeature;
    String version;
    Collection<AppliesToFilterInfo> atfi;
    String platformInfo;

    /**
     * @return the provideFeature
     */
    public String getProvideFeature() {
        return provideFeature;
    }

    /**
     * @param provideFeature the provideFeature to set
     */
    public void setProvideFeature(String provideFeature) {
        this.provideFeature = provideFeature;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the atfi
     */
    public Collection<AppliesToFilterInfo> getAtfi() {
        return atfi;
    }

    /**
     * @param atfi the atfi to set
     */
    public void setAtfi(Collection<AppliesToFilterInfo> atfi) {
        this.atfi = atfi;
    }

    /**
     * @return the platformInfo
     */
    public String getPlatformInfo() {
        return platformInfo;
    }

    /**
     * @param platformInfo the platformInfo to set
     */
    public void setPlatformInfo(String platformInfo) {
        this.platformInfo = platformInfo;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((provideFeature == null) ? 0 : provideFeature.hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((platformInfo == null) ? 0 : platformInfo.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExtendedMatchingData other = (ExtendedMatchingData) obj;
        if (provideFeature == null) {
            if (other.provideFeature != null)
                return false;
        } else if (!provideFeature.equals(other.provideFeature))
            return false;
        if (getType() != other.getType())
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;

        // atfi is a list, equals will say they are different if they are in a different order but same contents. We
        // want to treat them as being the same so do logic here instead of delegating to list.
        // Check appliesToFilterInfo
        Collection<AppliesToFilterInfo> appliesTo = other.getAtfi();
        if (appliesTo == null) {
            if (atfi != null)
                return false;
        } else {
            if (atfi == null)
                return false;
        }
        if (atfi != null) {
            if (appliesTo.size() != atfi.size())
                return false;
            for (AppliesToFilterInfo a : appliesTo) {
                if (!atfi.contains(a))
                    return false;
            }
        }

        if (platformInfo == null) {
            if (other.platformInfo != null)
                return false;
        } else if (!platformInfo.equals(other.platformInfo))
            return false;

        return true;
    }
}
