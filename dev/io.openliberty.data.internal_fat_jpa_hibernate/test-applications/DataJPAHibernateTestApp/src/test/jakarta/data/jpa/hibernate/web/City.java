/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.hibernate.web;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class City {

//  TODO re-enable version for this entity once bug https://hibernate.atlassian.net/browse/HHH-18248 is resolved
//  @Version
//  long changeCount;

//  TODO use @IdClass when Hibernate supports the usage in it's annotation processor. Bug: https://hibernate.atlassian.net/browse/HHH-18252
    @Id
    public CityId id;

    public int population;

    public Set<Integer> areaCodes;

    public City() {
    }

    City(String name, String state, int population, Set<Integer> areaCodes) {
        this.id = new CityId(name, state);
        this.population = population;
        this.areaCodes = areaCodes;
    }

    @Override
    public String toString() {
        return "City of " + id.name + ", " + id.stateName + " pop " + population + " in " + areaCodes
//                        + " v" + changeCount
        ;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof City)) {
            return false;
        }

        City c = (City) o;
        return this.id.name.equals(c.id.name)
               && this.id.stateName.equals(c.id.stateName)
               && this.population == c.population
               && this.areaCodes.equals(c.areaCodes);
    }
}