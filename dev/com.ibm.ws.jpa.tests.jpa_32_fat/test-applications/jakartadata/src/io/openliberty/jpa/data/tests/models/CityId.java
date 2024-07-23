/**
 *
 */
package io.openliberty.jpa.data.tests.models;

import java.io.Serializable;
import java.util.Objects;

/**
 *
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
