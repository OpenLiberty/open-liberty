/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.transport.model;

import java.util.Collection;
import java.util.HashSet;

public class RequireFeatureWithTolerates {

    private String feature;
    private Collection<String> tolerates;

    public String getFeature() {
        return this.feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public Collection<String> getTolerates() {
        return this.tolerates;
    }

    public void setTolerates(Collection<String> tolerates) {
        this.tolerates = tolerates;
    }

    public void addTolerates(String tolerate) {
        if (this.tolerates == null) {
            this.tolerates = new HashSet<String>();
        }
        tolerates.add(tolerate);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());
        result = prime * result + ((tolerates == null) ? 0 : tolerates.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RequireFeatureWithTolerates other = (RequireFeatureWithTolerates) obj;
        if (feature == null) {
            if (other.feature != null)
                return false;
        } else if (!feature.equals(other.feature))
            return false;

        if (tolerates == null) {
            if (other.tolerates != null)
                return false;
        } else if (!tolerates.equals(other.tolerates))
            return false;
        return true;
    }

}
