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
package test.jakarta.data.jpa.hibernate.integration.web;

import java.util.Arrays;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class County {
    @Id
    public String name;

    public int population;

    public int[] zipcodes;

    public String countySeat;

    public static County of(String name, String stateName, int population, int[] zipcodes, String countySeat) {
        County inst = new County();
        inst.name = name;
        inst.population = population;
        inst.zipcodes = zipcodes;
        inst.countySeat = countySeat;
        return inst;
    }

    @Override
    public String toString() {
        return "County of " + name + " population " + population;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof County))
            return false;

        County inst = (County) o;

        return this.name.equals(inst.name) &&
               this.population == inst.population &&
               Arrays.equals(this.zipcodes, inst.zipcodes) &&
               this.countySeat.equals(inst.countySeat);
    }
}