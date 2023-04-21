/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * An entity with a collection attribute that is not annotated as ElementCollection.
 */
@Entity
public class County {
    public Set<CityId> cities;

    @Id
    public String name;

    public int population;

    public County() {
    }

    County(String name, String stateName, int population, String... cities) {
        this.name = name;
        this.population = population;
        this.cities = new LinkedHashSet<>();
        for (String c : cities)
            this.cities.add(CityId.of(c, stateName));
    }

    @Override
    public String toString() {
        return "County of " + name + " population " + population;
    }
}