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
package test.jakarta.data.experimental.web;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Version;

/**
 * An entity that uses IdClass to have a composite id.
 */
@Entity
@IdClass(TownId.class)
public class Town {
    public Set<Integer> areaCodes;

    @Version
    long changeCount;

    @Id
    public String name;

    public int population;

    @Id
    public String stateName;

    public Town() {
    }

    Town(String name, String state, int population, Set<Integer> areaCodes) {
        this.name = name;
        this.stateName = state;
        this.population = population;
        this.areaCodes = areaCodes;
    }

    @Override
    public String toString() {
        return "Town of " + name + ", " + stateName + " pop " + population + " in " + areaCodes + " v" + changeCount;
    }
}