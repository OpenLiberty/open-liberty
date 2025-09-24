/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.hibernate.web;

import java.io.Serializable;
import java.util.Objects;

/**
 * Id class for City entity.
 */
public class CityId implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;

    public String stateName;

    public CityId() {
        //Hibernate requires default constructor
    }

    public CityId(String name, String state) {
        this.name = name;
        this.stateName = state;
    }

    public static CityId of(String name, String state) {
        return new CityId(name, state);
    }

    @Override
    public String toString() {
        return name + ", " + stateName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, stateName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CityId other = (CityId) obj;
        return Objects.equals(name, other.name) && Objects.equals(stateName, other.stateName);
    }

}