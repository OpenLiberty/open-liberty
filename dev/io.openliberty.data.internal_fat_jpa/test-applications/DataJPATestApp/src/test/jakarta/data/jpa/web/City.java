/*******************************************************************************
 * Copyright (c) 2023,2026 IBM Corporation and others.
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
    Set<Integer> areaCodes;

    @Version
    long changeCount;

    @Id
    String name;

    int population;

    @Id
    String stateName;

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

    public Set<Integer> getAreaCodes() {
        return areaCodes;
    }

    public long getChangeCount() {
        return changeCount;
    }

    public String getName() {
        return name;
    }

    public int getPopulation() {
        return population;
    }

    public String getStateName() {
        return stateName;
    }

    public void setAreaCodes(Set<Integer> areaCodes) {
        this.areaCodes = areaCodes;
    }

    public void setChangeCount(long changeCount) {
        this.changeCount = changeCount;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String toString() {
        return "City of " + name + ", " + stateName + " pop " + population + " in " + areaCodes + " v" + changeCount;
    }
}