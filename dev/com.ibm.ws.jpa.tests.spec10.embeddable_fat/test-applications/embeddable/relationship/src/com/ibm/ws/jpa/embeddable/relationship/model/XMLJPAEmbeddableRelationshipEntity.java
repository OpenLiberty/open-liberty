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

public class XMLJPAEmbeddableRelationshipEntity {

    private int id;

    private XMLUniO2OOwnerFieldAccessEmbed uniO2OOwnerFieldAccessEmbed;
    private XMLUniO2OOwnerPropertyAccessEmbed uniO2OOwnerPropertyAccessEmbed;
    private XMLBiO2OOwnerEmbed biO2OOwnerEmbed;
    private XMLBiO2OInverseEmbed biO2OInverseEmbed;
    private XMLBiO2OOwnerAssociationOverridesEmbed biO2OOwnerAssociationOverridesEmbed;
    // AssociationOverrides only works for owning-side relationships per 11.1.2.

    private XMLUniM2OOwnerEmbed uniM2OOwnerEmbed;
    private XMLBiM2OOwnerEmbed biM2OOwnerEmbed;
    // XMLBiM2OInverseEmbed is not common per Pro EJB 3 pg 99.

    // XMLUniO2MOwnerEmbed is not common per Pro EJB 3 pg 99.
    private XMLBiO2MInverseEmbed biO2MInverseEmbed;
    // XMLBiO2MOwnerEmbed is not common per Pro EJB 3 pg 99.

    private XMLBiM2MOwnerEmbed biM2MOwnerEmbed;

    // Embeds of Collection<AnyRelationship> is prohibited by the spec.
    // Embeds of List<AnyRelationship> is prohibited by the spec.
    // Embeds of Set<AnyRelationship> is prohibited by the spec.
    // Embeds of Map<AnyRelationship> is prohibited by the spec.

    private Collection<XMLUniO2OOwnerPropertyAccessEmbed> collectionUniO2OOwnerPropertyAccessEmbedOrderColumn;
    // Collection<EmbedBiToOne> is prohibited by OpenJPA (spec should say this).
    // Collection<EmbedToMany> is prohibited by the spec per section 2.6.

    private List<XMLUniO2OOwnerFieldAccessEmbed> listUniO2OOwnerFieldAccessEmbedOrderColumn;
    private List<XMLUniO2OOwnerFieldAccessEmbed> listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn;
    // List<EmbedBiToOne> is prohibited by OpenJPA (the spec should say this).
    // List<EmbedToMany> is prohibited by the spec per section 2.6.

    private Set<XMLUniO2OOwnerFieldAccessEmbed> setUniO2OOwnerFieldAccessEmbedOrderColumn;
    // Set<EmbedBiToOne> is prohibited by OpenJPA (the spec should say this).
    // Set<EmbedToMany> is prohibited by the spec per section 2.6.

    private Map<Integer, XMLUniO2OOwnerFieldAccessEmbed> mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn;
    private Map<XMLUniO2OOwnerFieldAccessEmbed, XMLUniO2OOwnerFieldAccessEmbed> mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn;

    // Map<EmbedBiToOne> is prohibited by OpenJPA (the spec should say this).
    // Map<EmbedToMany> is prohibited by the spec per section 2.6.

    public XMLJPAEmbeddableRelationshipEntity() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public XMLUniO2OOwnerFieldAccessEmbed getUniO2OOwnerFieldAccessEmbed() {
        return this.uniO2OOwnerFieldAccessEmbed;
    }

    public void setUniO2OOwnerFieldAccessEmbed(
                                               XMLUniO2OOwnerFieldAccessEmbed uniO2OOwnerFieldAccessEmbed) {
        this.uniO2OOwnerFieldAccessEmbed = uniO2OOwnerFieldAccessEmbed;
    }

    public XMLUniO2OOwnerPropertyAccessEmbed getUniO2OOwnerPropertyAccessEmbed() {
        return this.uniO2OOwnerPropertyAccessEmbed;
    }

    public void setUniO2OOwnerPropertyAccessEmbed(
                                                  XMLUniO2OOwnerPropertyAccessEmbed uniO2OOwnerPropertyAccessEmbed) {
        this.uniO2OOwnerPropertyAccessEmbed = uniO2OOwnerPropertyAccessEmbed;
    }

    public XMLBiO2OOwnerEmbed getBiO2OOwnerEmbed() {
        return this.biO2OOwnerEmbed;
    }

    public void setBiO2OOwnerEmbed(XMLBiO2OOwnerEmbed biO2OOwnerEmbed) {
        this.biO2OOwnerEmbed = biO2OOwnerEmbed;
    }

    public XMLBiO2OInverseEmbed getBiO2OInverseEmbed() {
        return biO2OInverseEmbed;
    }

    public void setBiO2OInverseEmbed(XMLBiO2OInverseEmbed biO2OInverseEmbed) {
        this.biO2OInverseEmbed = biO2OInverseEmbed;
    }

    public XMLBiO2OOwnerAssociationOverridesEmbed getBiO2OOwnerAssociationOverridesEmbed() {
        return this.biO2OOwnerAssociationOverridesEmbed;
    }

    public void setBiO2OOwnerAssociationOverridesEmbed(
                                                       XMLBiO2OOwnerAssociationOverridesEmbed biO2OOwnerAssociationOverridesEmbed) {
        this.biO2OOwnerAssociationOverridesEmbed = biO2OOwnerAssociationOverridesEmbed;
    }

    public XMLUniM2OOwnerEmbed getUniM2OOwnerEmbed() {
        return this.uniM2OOwnerEmbed;
    }

    public void setUniM2OOwnerEmbed(XMLUniM2OOwnerEmbed uniM2OOwnerEmbed) {
        this.uniM2OOwnerEmbed = uniM2OOwnerEmbed;
    }

    public XMLBiM2OOwnerEmbed getBiM2OOwnerEmbed() {
        return this.biM2OOwnerEmbed;
    }

    public void setBiM2OOwnerEmbed(XMLBiM2OOwnerEmbed biM2OOwnerEmbed) {
        this.biM2OOwnerEmbed = biM2OOwnerEmbed;
    }

    public XMLBiO2MInverseEmbed getBiO2MInverseEmbed() {
        return this.biO2MInverseEmbed;
    }

    public void setBiO2MInverseEmbed(XMLBiO2MInverseEmbed biO2MInverseEmbed) {
        this.biO2MInverseEmbed = biO2MInverseEmbed;
    }

    public XMLBiM2MOwnerEmbed getBiM2MOwnerEmbed() {
        return this.biM2MOwnerEmbed;
    }

    public void setBiM2MOwnerEmbed(XMLBiM2MOwnerEmbed biM2MOwnerEmbed) {
        this.biM2MOwnerEmbed = biM2MOwnerEmbed;
    }

    public Collection<XMLUniO2OOwnerPropertyAccessEmbed> getCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn() {
        return this.collectionUniO2OOwnerPropertyAccessEmbedOrderColumn;
    }

    public void setCollectionUniO2OOwnerPropertyAccessEmbedOrderColumn(
                                                                       Collection<XMLUniO2OOwnerPropertyAccessEmbed> collectionUniO2OOwnerPropertyAccessEmbedOrderColumn) {
        this.collectionUniO2OOwnerPropertyAccessEmbedOrderColumn = collectionUniO2OOwnerPropertyAccessEmbedOrderColumn;
    }

    public List<XMLUniO2OOwnerFieldAccessEmbed> getListUniO2OOwnerFieldAccessEmbedOrderColumn() {
        return this.listUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public void setListUniO2OOwnerFieldAccessEmbedOrderColumn(
                                                              List<XMLUniO2OOwnerFieldAccessEmbed> listUniO2OOwnerFieldAccessEmbedOrderColumn) {
        this.listUniO2OOwnerFieldAccessEmbedOrderColumn = listUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public List<XMLUniO2OOwnerFieldAccessEmbed> getListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn() {
        return this.listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn;
    }

    public void setListUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn(
                                                                                  List<XMLUniO2OOwnerFieldAccessEmbed> listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn) {
        this.listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn = listUniO2OOwnerFieldAccessEmbedAssociationOverridesOrderColumn;
    }

    public Set<XMLUniO2OOwnerFieldAccessEmbed> getSetUniO2OOwnerFieldAccessEmbedOrderColumn() {
        return this.setUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public void setSetUniO2OOwnerFieldAccessEmbedOrderColumn(
                                                             Set<XMLUniO2OOwnerFieldAccessEmbed> setUniO2OOwnerFieldAccessEmbedOrderColumn) {
        this.setUniO2OOwnerFieldAccessEmbedOrderColumn = setUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public Map<Integer, XMLUniO2OOwnerFieldAccessEmbed> getMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn() {
        return this.mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public void setMapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn(
                                                                            Map<Integer, XMLUniO2OOwnerFieldAccessEmbed> mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn) {
        this.mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn = mapKeyIntegerValueUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public Map<XMLUniO2OOwnerFieldAccessEmbed, XMLUniO2OOwnerFieldAccessEmbed> getMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn() {
        return this.mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn;
    }

    public void setMapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn(
                                                                                                Map<XMLUniO2OOwnerFieldAccessEmbed, XMLUniO2OOwnerFieldAccessEmbed> mapKeyUniO2OOwnerFieldAccessEmbedValueUniO2OOwnerFieldAccessEmbedOrderColumn) {
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
