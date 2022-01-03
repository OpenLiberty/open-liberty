package com.ibm.ws.jpa.olgh14137.model;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
/*
 * Hibernate defaults to PROPERTY access type for Embedded objects.
 * EclipseLink/OpenJPA defaults to FIELD access type.
 *
 * Access type needs to explicitly be defined here so that Hibernate can obtain the defined annotations
 */
@Access(AccessType.FIELD)
public class OverrideEmbeddableOLGH14137 implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "value")
    private Integer value;

    @Column(name = "value2")
    private Integer value2;

    @Embedded
    private OverrideNestedEmbeddableOLGH14137 nestedValue;

    public OverrideEmbeddableOLGH14137() {
    }

    public OverrideEmbeddableOLGH14137(Integer value, Integer value2, OverrideNestedEmbeddableOLGH14137 nestedValue) {
        this.value = value;
        this.value2 = value2;
        this.nestedValue = nestedValue;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getValue2() {
        return value2;
    }

    public void setValue2(Integer value2) {
        this.value2 = value2;
    }

    public OverrideNestedEmbeddableOLGH14137 getNestedValue() {
        return nestedValue;
    }

    public void setNestedValue(OverrideNestedEmbeddableOLGH14137 nestedValue) {
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
