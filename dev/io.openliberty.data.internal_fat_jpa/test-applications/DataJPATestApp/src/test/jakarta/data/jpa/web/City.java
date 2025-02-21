/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Version;

/**
 * Entity with a composite id (using IdClass) and version.
 */
@Entity
@IdClass(CityId.class)
public class City {
    // TODO uncomment to reproduce EclipseLink bugs #28589, #29475
    // that select an attribute that is a collection type.
    //@ElementCollection(fetch = FetchType.EAGER)
    public Set<Integer> areaCodes;

    @Version
    long changeCount;

    @Id
    public String name;

    public int population;

    @Id
    public String stateName;

    public City() {
    }

    City(String name, String state, int population, Set<Integer> areaCodes) {
        this.name = name;
        this.stateName = state;
        this.population = population;
        this.areaCodes = areaCodes;
    }

    static City of(CityId id, int population, Set<Integer> areaCodes, long version) {
        City city = new City(id.name, id.getStateName(), population, areaCodes);
        city.changeCount = version;
        return city;
    }

    @Override
    public String toString() {
        return "City of " + name + ", " + stateName + " pop " + population + " in " + areaCodes + " v" + changeCount;
    }
}