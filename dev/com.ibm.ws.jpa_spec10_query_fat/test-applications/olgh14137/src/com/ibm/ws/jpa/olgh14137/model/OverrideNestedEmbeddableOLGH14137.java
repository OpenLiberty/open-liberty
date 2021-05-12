package com.ibm.ws.jpa.olgh14137.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class OverrideNestedEmbeddableOLGH14137 {

    @Column(name = "nested_value")
    private Integer nestedValue;

    @Column(name = "nested_value2")
    private Integer nestedValue2;

    public OverrideNestedEmbeddableOLGH14137() {
    }

    public OverrideNestedEmbeddableOLGH14137(Integer nestedValue, Integer nestedValue2) {
        this.nestedValue = nestedValue;
        this.nestedValue2 = nestedValue2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nestedValue == null) ? 0 : nestedValue.hashCode());
        result = prime * result + ((nestedValue2 == null) ? 0 : nestedValue2.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OverrideNestedEmbeddableOLGH14137 other = (OverrideNestedEmbeddableOLGH14137) obj;
        if (nestedValue == null) {
            if (other.nestedValue != null)
                return false;
        } else if (!nestedValue.equals(other.nestedValue))
            return false;
        if (nestedValue2 == null) {
            if (other.nestedValue2 != null)
                return false;
        } else if (!nestedValue2.equals(other.nestedValue2))
            return false;
        return true;
    }
}
