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

public class SingletonSetId implements Comparable<SingletonSetId> {
    private final String setId;
    private final boolean setIsPrivate;

    public SingletonSetId(FeatureInfo setMember) {
        if ("private".equals(setMember.getVisibility())) {
            setIsPrivate = true;
        } else {
            setIsPrivate = false;
        }
        String name = setMember.getName();
        if (name.lastIndexOf('-') == -1) {
            throw new IllegalStateException("Bad feature name " + name + " expected -X.X version suffix.");
        }
        setId = name.substring(0, name.lastIndexOf('-'));
    }

    public SingletonSetId(String name, boolean isPrivate) {
        if (name.lastIndexOf('-') == -1) {
            throw new IllegalStateException("Bad feature name " + name + " expected -X.X version suffix.");
        }
        setId = name.substring(0, name.lastIndexOf('-'));
        setIsPrivate = isPrivate;
    }

    public String getSetFeatureNamePrefix() {
        return setId;
    }

    public boolean isPrivate() {
        return setIsPrivate;
    }

    @Override
    public String toString() {
        return (setIsPrivate ? "##" : "") + setId;
    }

    //hashCode & equals.. ignores the SetIsPrivate flag. 

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((setId == null) ? 0 : setId.hashCode());
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
        SingletonSetId other = (SingletonSetId) obj;
        if (setId == null) {
            if (other.setId != null)
                return false;
        } else if (!setId.equals(other.setId))
            return false;
        return true;
    }

    @Override
    public int compareTo(SingletonSetId o) {
        //a little simpler, because setId can never be null.
        if (o != null)
            return this.setId.compareTo(o.setId);
        else
            return -1;
    }
}
