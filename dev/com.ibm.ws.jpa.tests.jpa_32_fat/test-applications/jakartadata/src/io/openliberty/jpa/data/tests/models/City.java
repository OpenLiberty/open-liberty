/**
 *
 */
package io.openliberty.jpa.data.tests.models;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Version;

/**
 *
 */
@Entity
@IdClass(CityId.class)
public class City {
    // TODO uncomment to reproduce EclipseLink bug with selecting an attribute that is a collection type.
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

    @Override
    public String toString() {
        return "City of " + name + ", " + stateName + " pop " + population + " in " + areaCodes + " v" + changeCount;
    }
}
