/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.io.Serializable;
import java.util.Objects;

/**
 * Id class for City entity.
 */
public class CityId implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;

    private String stateName;

    public CityId() {
    }

    public CityId(String name, String state) {
        this.name = name;
        this.stateName = state;
    }

    @Override
    public boolean equals(Object o) {
        CityId c;
        return CityId.class.equals(o.getClass()) &&
               Objects.equals(name, (c = (CityId) o).name) &&
               Objects.equals(stateName, c.stateName);
    }

    public String getStateName() {
        return stateName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, stateName);
    }

    public static CityId of(String name, String state) {
        return new CityId(name, state);
    }

    public void setStateName(String v) {
        stateName = v;
    }

    @Override
    public String toString() {
        return name + ", " + stateName;
    }
}