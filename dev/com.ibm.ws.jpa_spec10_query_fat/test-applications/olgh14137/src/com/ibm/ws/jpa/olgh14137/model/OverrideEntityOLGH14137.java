package com.ibm.ws.jpa.olgh14137.model;

import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "override_entity_b")
public class OverrideEntityOLGH14137 {

    @Id
    @Column(name = "b_id")
    private Integer id;

    @ElementCollection
    @CollectionTable(name = "ct_override_entity_b", joinColumns = @JoinColumn(name = "entity_b_ct_entity_b")) // use default join column name
    @AttributeOverrides({
                          @AttributeOverride(name = "value", column = @Column(name = "ct_b_override_value")),
                          @AttributeOverride(name = "nestedValue.nestedValue", column = @Column(name = "ct_b_override_nested_value"))
    })
    private Set<OverrideEmbeddableOLGH14137> simpleMappingEmbeddable;

    public OverrideEntityOLGH14137() {
    }

    public OverrideEntityOLGH14137(Integer id, Set<OverrideEmbeddableOLGH14137> simpleMappingEmbeddable) {
        this.id = id;
        this.simpleMappingEmbeddable = simpleMappingEmbeddable;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Set<OverrideEmbeddableOLGH14137> getSimpleMappingEmbeddable() {
        return simpleMappingEmbeddable;
    }

    public void setSimpleMappingEmbeddable(Set<OverrideEmbeddableOLGH14137> simpleMappingEmbeddable) {
        this.simpleMappingEmbeddable = simpleMappingEmbeddable;
    }
}
