/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.relationship.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

@Entity
@Table(name = "AnnRootEmRL")
public class JPAEmbeddableRelationshipEntity {

    @Id
    private int id;

    @Embedded
    private UniO2OOwnerFieldAccessEmbed uniO2OOwnerFieldAccessEmbed;
    @Embedded
    private UniO2OOwnerPropertyAccessEmbed uniO2OOwnerPropertyAccessEmbed;
    @Embedded
    private BiO2OOwnerEmbed biO2OOwnerEmbed;
    @Embedded
    private BiO2OInverseEmbed biO2OInverseEmbed;
    @Embedded
    @AssociationOverrides({ @AssociationOverride(name = "biO2OInverseAssociationOverridesEntity", joinColumns = @JoinColumn(name = "BIO2OINVERSEASSOCOVERRIDE")) })
    private BiO2OOwnerAssociationOverridesEmbed biO2OOwnerAssociationOverridesEmbed;
    // AssociationOverrides only works for owning-side relationships per 11.1.2.

    @Embedded
    private UniM2OOwnerEmbed uniM2OOwnerEmbed;
    @Embedded
    private BiM2OOwnerEmbed biM2OOwnerEmbed;
    // BiM2OInverseEmbed is not common per Pro EJB 3 pg 99.

    // UniO2MOwnerEmbed is not common per Pro EJB 3 pg 99.
    @Embedded
    private BiO2MInverseEmbed biO2MInverseEmbed;
    // BiO2MOwnerEmbed is not common per Pro EJB 3 pg 99.

    @Embedded
    private BiM2MOwnerEmbed biM2MOwnerEmbed;

    // Embeds of Collection<AnyRelationship> is prohibited by the spec.
    // Embeds of List<AnyRelationship> is prohibited by the spec.
    // Embeds of Set<AnyRelationship> is prohibited by the spec.
    // Embeds of Map<AnyRelationship> is prohibited by the spec.

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "colUniO2OOwnPAEmOC", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    private Collection<UniO2OOwnerPropertyAccessEmbed> collectionUniO2OOwnerPropertyAccessEmbedOrderColumn;
    // Collection<EmbedBiToOne> is prohibited by OpenJPA (spec should say this).
    // Collection<EmbedToMany> is prohibited by the spec per section 2.6.

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listUniO2OOwnFAEmOC", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    private List<UniO2OOwnerFieldAccessEmbed> listUniO2OOwnerFieldAccessEmbedOrderColumn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listUniO2OOwnFAEmAOOC", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    @AssociationOverrides({ @AssociationOverride(name = "uniO2ODummyEntity_FA", joinColumns = @JoinColumn(name = "UNIO2OINVERSEASSOCOVERRIDE")) })
    private List<UniO2OOwnerFieldAccessEmbed> listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn;
    // List<EmbedBiToOne> is prohibited by OpenJPA (the spec should say this).
    // List<EmbedToMany> is prohibited by the spec per section 2.6.

    @ElementCollection(fetch = FetchType.EAGER, targetClass = UniO2OOwnerFieldAccessEmbed.class)
    @CollectionTable(name = "setUniO2OOwnFAEmOC", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    private Set<?> setUniO2OOwnerFieldAccessEmbedOrderColumn;
    // Set<EmbedBiToOne> is prohibited by OpenJPA (the spec should say this).
    // Set<EmbedToMany> is prohibited by the spec per section 2.6.

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mapKeyIntValUniO2OOwnFAEmOC", joinColumns = @JoinColumn(name = "parent_id"))
    @MapKeyColumn(name = "mykey")
    @AssociationOverrides({ @AssociationOverride(name = "value.uniO2ODummyEntity_FA", joinColumns = @JoinColumn(name = "value")) })
    private Map<Integer, UniO2OOwnerFieldAccessEmbed> mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mapKeyUniO2OEmValUniO2OEmOC", joinColumns = @JoinColumn(name = "parent_id"))
    @AssociationOverrides({
                            @AssociationOverride(name = "key.uniO2ODummyEntity_FA", joinColumns = @JoinColumn(name = "mykey")),
                            @AssociationOverride(name = "value.uniO2ODummyEntity_FA", joinColumns = @JoinColumn(name = "value")) })
    private Map<UniO2OOwnerFieldAccessEmbed, UniO2OOwnerFieldAccessEmbed> mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn;

    // Map<EmbedBiToOne> is prohibited by OpenJPA (the spec should say this).
    // Map<EmbedToMany> is prohibited by the spec per section 2.6.

    public JPAEmbeddableRelationshipEntity() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UniO2OOwnerFieldAccessEmbed getUniO2OOwnerFieldAccessEmbed() {
        return this.uniO2OOwnerFieldAccessEmbed;
    }

    public void setUniO2OOwnerFieldAccessEmbed(
                                               UniO2OOwnerFieldAccessEmbed uniO2OOwnerFieldAccessEmbed) {
        this.uniO2OOwnerFieldAccessEmbed = uniO2OOwnerFieldAccessEmbed;
    }

    public UniO2OOwnerPropertyAccessEmbed getUniO2OOwnerPropertyAccessEmbed() {
        return this.uniO2OOwnerPropertyAccessEmbed;
    }

    public void setUniO2OOwnerPropertyAccessEmbed(
                                                  UniO2OOwnerPropertyAccessEmbed uniO2OOwnerPropertyAccessEmbed) {
        this.uniO2OOwnerPropertyAccessEmbed = uniO2OOwnerPropertyAccessEmbed;
    }

    public BiO2OOwnerEmbed getBiO2OOwnerEmbed() {
        return this.biO2OOwnerEmbed;
    }

    public void setBiO2OOwnerEmbed(BiO2OOwnerEmbed biO2OOwnerEmbed) {
        this.biO2OOwnerEmbed = biO2OOwnerEmbed;
    }

    public BiO2OInverseEmbed getBiO2OInverseEmbed() {
        return biO2OInverseEmbed;
    }

    public void setBiO2OInverseEmbed(BiO2OInverseEmbed biO2OInverseEmbed) {
        this.biO2OInverseEmbed = biO2OInverseEmbed;
    }

    public BiO2OOwnerAssociationOverridesEmbed getBiO2OOwnerAssociationOverridesEmbed() {
        return this.biO2OOwnerAssociationOverridesEmbed;
    }

    public void setBiO2OOwnerAssociationOverridesEmbed(
                                                       BiO2OOwnerAssociationOverridesEmbed biO2OOwnerAssociationOverridesEmbed) {
        this.biO2OOwnerAssociationOverridesEmbed = biO2OOwnerAssociationOverridesEmbed;
    }

    public UniM2OOwnerEmbed getUniM2OOwnerEmbed() {
        return this.uniM2OOwnerEmbed;
    }

    public void setUniM2OOwnerEmbed(UniM2OOwnerEmbed uniM2OOwnerEmbed) {
        this.uniM2OOwnerEmbed = uniM2OOwnerEmbed;
    }

    public BiM2OOwnerEmbed getBiM2OOwnerEmbed() {
        return this.biM2OOwnerEmbed;
    }

    public void setBiM2OOwnerEmbed(BiM2OOwnerEmbed biM2OOwnerEmbed) {
        this.biM2OOwnerEmbed = biM2OOwnerEmbed;
    }

    public BiO2MInverseEmbed getBiO2MInverseEmbed() {
        return this.biO2MInverseEmbed;
    }

    public void setBiO2MInverseEmbed(BiO2MInverseEmbed biO2MInverseEmbed) {
        this.biO2MInverseEmbed = biO2MInverseEmbed;
    }

    public BiM2MOwnerEmbed getBiM2MOwnerEmbed() {
        return this.biM2MOwnerEmbed;
    }

    public void setBiM2MOwnerEmbed(BiM2MOwnerEmbed biM2MOwnerEmbed) {
        this.biM2MOwnerEmbed = biM2MOwnerEmbed;
    }

    public Collection<UniO2OOwnerPropertyAccessEmbed> getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn() {
        return this.collectionUniO2OOwnerPropertyAccessEmbedOrderColumn;
    }

    public void setCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn(
                                                                       Collection<UniO2OOwnerPropertyAccessEmbed> collectionUniO2OOwnerPropertyAccessEmbedOrderColumn) {
        this.collectionUniO2OOwnerPropertyAccessEmbedOrderColumn = collectionUniO2OOwnerPropertyAccessEmbedOrderColumn;
    }

    public List<UniO2OOwnerFieldAccessEmbed> getListUniO2OOwnerFieldAccessEmbedOrderColumn() {
        return this.listUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public void setListUniO2OOwnerFieldAccessEmbedOrderColumn(
                                                              List<UniO2OOwnerFieldAccessEmbed> listUniO2OOwnerFieldAccessEmbedOrderColumn) {
        this.listUniO2OOwnerFieldAccessEmbedOrderColumn = listUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public List<UniO2OOwnerFieldAccessEmbed> getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn() {
        return this.listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn;
    }

    public void setListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(
                                                                                  List<UniO2OOwnerFieldAccessEmbed> listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn) {
        this.listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn = listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn;
    }

    public Set getSetUniO2OOwnerFieldAccessEmbedOrderColumn() {
        return this.setUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public void setSetUniO2OOwnerFieldAccessEmbedOrderColumn(
                                                             Set setUniO2OOwnerFieldAccessEmbedOrderColumn) {
        this.setUniO2OOwnerFieldAccessEmbedOrderColumn = setUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public Map<Integer, UniO2OOwnerFieldAccessEmbed> getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn() {
        return this.mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public void setMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(
                                                                            Map<Integer, UniO2OOwnerFieldAccessEmbed> mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn) {
        this.mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn = mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public Map<UniO2OOwnerFieldAccessEmbed, UniO2OOwnerFieldAccessEmbed> getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn() {
        return this.mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public void setMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(
                                                                                                Map<UniO2OOwnerFieldAccessEmbed, UniO2OOwnerFieldAccessEmbed> mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn) {
        this.mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn = mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id=" + id);
        sb.append(", " + uniO2OOwnerFieldAccessEmbed);
        sb.append(", " + uniO2OOwnerPropertyAccessEmbed);
        sb.append(", " + biO2OOwnerEmbed);
        sb.append(", " + biO2OInverseEmbed);
        sb.append(", " + biO2OOwnerAssociationOverridesEmbed);
        sb.append(", " + uniM2OOwnerEmbed);
        sb.append(", " + biM2OOwnerEmbed);
        sb.append(", " + biO2MInverseEmbed);
        sb.append(", " + biM2MOwnerEmbed);
        if (collectionUniO2OOwnerPropertyAccessEmbedOrderColumn != null)
            sb.append(", collectionUniO2OOwnerPropertyAccessEmbedOrderColumn="
                      + collectionUniO2OOwnerPropertyAccessEmbedOrderColumn
                                      .toString());
        else
            sb
                            .append(", collectionUniO2OOwnerPropertyAccessEmbedOrderColumn=null");
        if (listUniO2OOwnerFieldAccessEmbedOrderColumn != null)
            sb.append(", listUniO2OOwnerFieldAccessEmbedOrderColumn="
                      + listUniO2OOwnerFieldAccessEmbedOrderColumn.toString());
        else
            sb.append(", listUniO2OOwnerFieldAccessEmbedOrderColumn=null");
        if (listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn != null)
            sb
                            .append(", listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn="
                                    + listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn
                                                    .toString());
        else
            sb
                            .append(", listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn=null");
        if (setUniO2OOwnerFieldAccessEmbedOrderColumn != null)
            sb.append(", setUniO2OOwnerFieldAccessEmbedOrderColumn="
                      + setUniO2OOwnerFieldAccessEmbedOrderColumn.toString());
        else
            sb.append(", setUniO2OOwnerFieldAccessEmbedOrderColumn=null");
        if (mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn != null)
            sb
                            .append(", mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn="
                                    + mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn
                                                    .toString());
        else
            sb
                            .append(", mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn=null");
        if (mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn != null)
            sb
                            .append(", mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn="
                                    + mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn
                                                    .toString());
        else
            sb
                            .append(", mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn=null");
        return sb.toString();
    }

}
