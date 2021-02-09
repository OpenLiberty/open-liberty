package com.ibm.ws.jpa.olgh14137.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class OverrideEmbeddableOLGH14137 implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "value")
    private Integer value;

    @Column(name = "value2")
    private Integer value2;

    private OverrideNestedEmbeddableOLGH14137 nestedValue;

    public OverrideEmbeddableOLGH14137() {
    }

    public OverrideEmbeddableOLGH14137(Integer value, Integer value2, OverrideNestedEmbeddableOLGH14137 nestedValue) {
        this.value = value;
        this.value2 = value2;
        this.nestedValue = nestedValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nestedValue == null) ? 0 : nestedValue.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
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
        OverrideEmbeddableOLGH14137 other = (OverrideEmbeddableOLGH14137) obj;
        if (nestedValue == null) {
            if (other.nestedValue != null)
                return false;
        } else if (!nestedValue.equals(other.nestedValue))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        if (value2 == null) {
            if (other.value2 != null)
                return false;
        } else if (!value2.equals(other.value2))
            return false;
        return true;
    }
}
