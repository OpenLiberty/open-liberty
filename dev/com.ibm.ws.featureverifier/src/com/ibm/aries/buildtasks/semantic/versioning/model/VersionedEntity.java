/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.semantic.versioning.model;

import org.osgi.framework.Version;

public class VersionedEntity implements Comparable<VersionedEntity> {
    private final String name;
    private final String version;

    public VersionedEntity(String name, String version) {
        this.name = name;
        if (version != null && version.trim().length() == 0) {
            version = null;
        }
        //if version isn't null, let osgi parse it and give us back a canonical one
        //this should map 1.1 into 1.1.0 etc so we can match them at runtime.
        this.version = (version == null ? null : Version.parseVersion(version).toString());
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    /**
     * FINAL equals method.
     * 
     * This equals method uses instanceof to allow child class comparisons to be made
     * against superclass instances.. this means we need to make equals final else
     * we may not uphold a=b b=c therefore c=a
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof VersionedEntity))
            return false;
        VersionedEntity other = (VersionedEntity) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public int compareTo(VersionedEntity o) {
        if (name.equals(o.name)) {
            if (version == null && o.version != null) {
                return -1;
            }
            if (version != null && o.version == null) {
                return 1;
            }
            if (version == null && o.version == null) {
                return 0;
            }
            Version thisV = Version.parseVersion(version);
            Version otherV = Version.parseVersion(o.version);
            return thisV.compareTo(otherV);
        } else {
            return name.compareTo(o.name);
        }
    }

    @Override
    public String toString() {
        return name + "@" + version;
    }

}
