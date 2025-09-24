/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Version;

/**
 * Recreate from io.openliberty.data.internal_fat_jpa
 */
@Entity
@IdClass(CityId.class)
public class City {
    @ElementCollection(fetch = FetchType.EAGER)
    public Set<Integer> areaCodes;

    @Version
    long changeCount;

    @Id
    public String name;

    public int population;
    @Version
    public long version;

    @Id
    public String stateName;

    public static City of(String name, String state, int population, Set<Integer> areaCodes) {
        City inst = new City();
        inst.name = name;
        inst.stateName = state;
        inst.population = population;
        inst.areaCodes = areaCodes;
        return inst;
    }

    @Override
    public String toString() {
        return "City of " + name + ", " + stateName + " pop " + population + " in " + areaCodes + " v" + changeCount;
    }
}
