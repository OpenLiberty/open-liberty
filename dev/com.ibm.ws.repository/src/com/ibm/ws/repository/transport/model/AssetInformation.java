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

import java.util.List;

public class AssetInformation extends AbstractJSON {

    private List<Language> languages;
    private List<PlatformRequirement> platformrequirements;
    private List<Platform> platforms;
    private String size;

    public enum Language {
        JS
    }

    public enum PlatformRequirement {
        WINDOWS, ECLIPSE
    }

    public enum Platform {
        ANDROID, IOS
    }

    public List<Language> getLanguages() {
        return languages;
    }

    public void setLanguages(List<Language> languages) {
        this.languages = languages;
    }

    public List<PlatformRequirement> getPlatformrequirements() {
        return platformrequirements;
    }

    public void setPlatformrequirements(
                                        List<PlatformRequirement> platformrequirements) {
        this.platformrequirements = platformrequirements;
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + ((languages == null) ? 0 : languages.hashCode());
        result = prime
                 * result
                 + ((platformrequirements == null) ? 0 : platformrequirements.hashCode());
        result = prime * result
                 + ((platforms == null) ? 0 : platforms.hashCode());
        result = prime * result + ((size == null) ? 0 : size.hashCode());
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
        AssetInformation other = (AssetInformation) obj;
        if (languages == null) {
            if (other.languages != null)
                return false;
        } else if (!languages.equals(other.languages))
            return false;
        if (platformrequirements == null) {
            if (other.platformrequirements != null)
                return false;
        } else if (!platformrequirements.equals(other.platformrequirements))
            return false;
        if (platforms == null) {
            if (other.platforms != null)
                return false;
        } else if (!platforms.equals(other.platforms))
            return false;
        if (size == null) {
            if (other.size != null)
                return false;
        } else if (!size.equals(other.size))
            return false;
        return true;
    }

}
