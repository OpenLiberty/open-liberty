/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.nosql.integration.web;

import java.util.Arrays;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

@Entity
public class County {
    @Id
    public String name;

    @Column
    public Integer population;

    @Column
    public Integer[] zipcodes;

    @Column
    public String countySeat;

    public static County of(String name, String stateName, Integer population, Integer[] zipcodes, String countySeat) {
        County inst = new County();
        inst.name = name;
        inst.population = population;
        inst.zipcodes = zipcodes;
        inst.countySeat = countySeat;
        return inst;
    }

    @Override
    public String toString() {
        return "County of " + name + " population " + population + " zipcodes " + Arrays.toString(zipcodes) + " countySeat " + countySeat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof County))
            return false;

        County inst = (County) o;

        return this.name.equals(inst.name) &&
               this.population.equals(inst.population) &&
               Arrays.equals(this.zipcodes, inst.zipcodes) &&
               this.countySeat.equals(inst.countySeat);
    }
}