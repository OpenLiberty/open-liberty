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

package com.ibm.ws.jpa.embeddable.basic.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.jpa.embeddable.basic.model.XMLEnumeratedPropertyAccessEmbed.XMLEnumeratedPropertyAccessEnum;

public class XMLJPAEmbeddableBasicEntity {

    private int id;

    private XMLIntegerEmbed integerEmbed;
    private XMLIntegerAttributeOverridesEmbed integerAttributeOverridesEmbed;

    private XMLIntegerFieldAccessEmbed integerFieldAccessEmbed;
    private XMLEnumeratedFieldAccessEmbed enumeratedFieldAccessEmbed;
    private XMLTemporalFieldAccessEmbed temporalFieldAccessEmbed;
    private XMLLobFieldAccessEmbed lobFieldAccessEmbed;

    private XMLIntegerTransientEmbed integerTransientEmbed;

    private XMLIntegerPropertyAccessEmbed integerPropertyAccessEmbed;
    private XMLEnumeratedPropertyAccessEmbed enumeratedPropertyAccessEmbed;
    private XMLTemporalPropertyAccessEmbed temporalPropertyAccessEmbed;
    private XMLLobPropertyAccessEmbed lobPropertyAccessEmbed;

    private XMLCollectionIntegerEmbed collectionIntegerEmbed;
    // XMLCollectionIntegerAttributeOverridesEmbed is disallowed by the spec per
    // 10.1.4.
    private XMLCollectionEnumeratedEmbed collectionEnumeratedEmbed;
    private XMLCollectionTemporalEmbed collectionTemporalEmbed;
    private XMLCollectionLobEmbed collectionLobEmbed;

    private XMLListIntegerOrderColumnEmbed listIntegerOrderColumnEmbed;
    private XMLListIntegerOrderByEmbed listIntegerOrderByEmbed;
    // XMLListIntegerAttributeOverridesEmbed is disallowed by the spec per
    // 10.1.4.
    private XMLListEnumeratedEmbed listEnumeratedEmbed;
    private XMLListTemporalEmbed listTemporalEmbed;
    private XMLListLobEmbed listLobEmbed;

    private XMLSetIntegerEmbed setIntegerEmbed;
    // XMLSetIntegerAttributeOverridesEmbed is disallowed by the spec per
    // 10.1.4.
    private XMLSetEnumeratedEmbed setEnumeratedEmbed;
    private XMLSetTemporalEmbed setTemporalEmbed;
    private XMLSetLobEmbed setLobEmbed;

    private XMLMapKeyIntegerEmbed mapKeyIntegerEmbed;
    // XMLMapKeyIntegerValueIntegerAttributeOverridesEmbed is disallowed by the
    // spec per 10.1.4.
    private XMLMapKeyIntegerValueTemporalEmbed mapKeyIntegerValueTemporalEmbed;
    private XMLMapKeyEnumeratedValueEnumeratedEmbed mapKeyEnumeratedValueEnumeratedEmbed;
    private XMLMapKeyTemporalValueTemporalEmbed mapKeyTemporalValueTemporalEmbed;
    private XMLMapKeyEnumeratedValueLobEmbed mapKeyEnumeratedValueLobEmbed;

    private Collection<XMLLobPropertyAccessEmbed> collectionLobPropertyAccessEmbed;

    private List<XMLIntegerEmbed> listIntegerEmbedOrderColumn;
    private List<XMLIntegerAttributeOverridesEmbed> listIntegerAttributeOverridesEmbedOrderColumn;
    private List<XMLIntegerPropertyAccessEmbed> listIntegerPropertyAccessEmbedOrderColumn;
    private List<XMLEnumeratedPropertyAccessEmbed> listEnumeratedPropertyAccessEmbedOrderColumn;
    private List<XMLEnumeratedPropertyAccessEmbed> listEnumeratedPropertyAccessEmbedOrderBy;
    private List<XMLTemporalPropertyAccessEmbed> listTemporalPropertyAccessEmbedOrderColumn;

    private Set<XMLIntegerPropertyAccessEmbed> setIntegerPropertyAccessEmbed;

    private Map<Integer, XMLTemporalPropertyAccessEmbed> mapKeyIntegerValueTemporalPropertyAccessEmbed;
    private Map<Date, XMLTemporalPropertyAccessEmbed> mapKeyTemporalValueTemporalPropertyAccessEmbed;

    private Map<Integer, XMLIntegerPropertyAccessEmbed> mapKeyIntegerValueIntegerPropertyAccessEmbed;
    private Map<XMLEnumeratedPropertyAccessEnum, XMLEnumeratedPropertyAccessEmbed> mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed;
    private Map<XMLIntegerEmbed, XMLIntegerEmbed> mapKeyIntegerEmbedValueIntegerEmbed;
    private Map<XMLLobPropertyAccessEmbed, XMLLobPropertyAccessEmbed> mapKeyLobEmbedValueLobEmbed;

    public XMLJPAEmbeddableBasicEntity() {
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public XMLIntegerEmbed getIntegerEmbed() {
        return this.integerEmbed;
    }

    public void setIntegerEmbed(XMLIntegerEmbed integerEmbed) {
        this.integerEmbed = integerEmbed;
    }

    public XMLIntegerAttributeOverridesEmbed getIntegerAttributeOverridesEmbed() {
        return this.integerAttributeOverridesEmbed;
    }

    public void setIntegerAttributeOverridesEmbed(XMLIntegerAttributeOverridesEmbed integerAttributeOverridesEmbed) {
        this.integerAttributeOverridesEmbed = integerAttributeOverridesEmbed;
    }

    public XMLIntegerFieldAccessEmbed getIntegerFieldAccessEmbed() {
        return this.integerFieldAccessEmbed;
    }

    public void setIntegerFieldAccessEmbed(XMLIntegerFieldAccessEmbed integerFieldAccessEmbed) {
        this.integerFieldAccessEmbed = integerFieldAccessEmbed;
    }

    public XMLEnumeratedFieldAccessEmbed getEnumeratedFieldAccessEmbed() {
        return this.enumeratedFieldAccessEmbed;
    }

    public void setEnumeratedFieldAccessEmbed(XMLEnumeratedFieldAccessEmbed enumeratedFieldAccessEmbed) {
        this.enumeratedFieldAccessEmbed = enumeratedFieldAccessEmbed;
    }

    public XMLTemporalFieldAccessEmbed getTemporalFieldAccessEmbed() {
        return this.temporalFieldAccessEmbed;
    }

    public void setTemporalFieldAccessEmbed(XMLTemporalFieldAccessEmbed temporalFieldAccessEmbed) {
        this.temporalFieldAccessEmbed = temporalFieldAccessEmbed;
    }

    public XMLLobFieldAccessEmbed getLobFieldAccessEmbed() {
        return this.lobFieldAccessEmbed;
    }

    public void setLobFieldAccessEmbed(XMLLobFieldAccessEmbed lobFieldAccessEmbed) {
        this.lobFieldAccessEmbed = lobFieldAccessEmbed;
    }

    public XMLIntegerTransientEmbed getIntegerTransientEmbed() {
        return this.integerTransientEmbed;
    }

    public void setIntegerTransientEmbed(XMLIntegerTransientEmbed integerTransientEmbed) {
        this.integerTransientEmbed = integerTransientEmbed;
    }

    public XMLIntegerPropertyAccessEmbed getIntegerPropertyAccessEmbed() {
        return this.integerPropertyAccessEmbed;
    }

    public void setIntegerPropertyAccessEmbed(XMLIntegerPropertyAccessEmbed integerPropertyAccessEmbed) {
        this.integerPropertyAccessEmbed = integerPropertyAccessEmbed;
    }

    public XMLEnumeratedPropertyAccessEmbed getEnumeratedPropertyAccessEmbed() {
        return this.enumeratedPropertyAccessEmbed;
    }

    public void setEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEmbed enumeratedPropertyAccessEmbed) {
        this.enumeratedPropertyAccessEmbed = enumeratedPropertyAccessEmbed;
    }

    public XMLTemporalPropertyAccessEmbed getTemporalPropertyAccessEmbed() {
        return this.temporalPropertyAccessEmbed;
    }

    public void setTemporalPropertyAccessEmbed(XMLTemporalPropertyAccessEmbed temporalPropertyAccessEmbed) {
        this.temporalPropertyAccessEmbed = temporalPropertyAccessEmbed;
    }

    public XMLLobPropertyAccessEmbed getLobPropertyAccessEmbed() {
        return this.lobPropertyAccessEmbed;
    }

    public void setLobPropertyAccessEmbed(XMLLobPropertyAccessEmbed lobPropertyAccessEmbed) {
        this.lobPropertyAccessEmbed = lobPropertyAccessEmbed;
    }

    public XMLCollectionIntegerEmbed getCollectionIntegerEmbed() {
        return this.collectionIntegerEmbed;
    }

    public void setCollectionIntegerEmbed(XMLCollectionIntegerEmbed collectionIntegerEmbed) {
        this.collectionIntegerEmbed = collectionIntegerEmbed;
    }

    public XMLCollectionEnumeratedEmbed getCollectionEnumeratedEmbed() {
        return this.collectionEnumeratedEmbed;
    }

    public void setCollectionEnumeratedEmbed(XMLCollectionEnumeratedEmbed collectionEnumeratedEmbed) {
        this.collectionEnumeratedEmbed = collectionEnumeratedEmbed;
    }

    public XMLCollectionTemporalEmbed getCollectionTemporalEmbed() {
        return this.collectionTemporalEmbed;
    }

    public void setCollectionTemporalEmbed(XMLCollectionTemporalEmbed collectionTemporalEmbed) {
        this.collectionTemporalEmbed = collectionTemporalEmbed;
    }

    public XMLCollectionLobEmbed getCollectionLobEmbed() {
        return this.collectionLobEmbed;
    }

    public void setCollectionLobEmbed(XMLCollectionLobEmbed collectionLobEmbed) {
        this.collectionLobEmbed = collectionLobEmbed;
    }

    public XMLListIntegerOrderColumnEmbed getListIntegerOrderColumnEmbed() {
        return this.listIntegerOrderColumnEmbed;
    }

    public void setListIntegerOrderColumnEmbed(XMLListIntegerOrderColumnEmbed listIntegerOrderColumnEmbed) {
        this.listIntegerOrderColumnEmbed = listIntegerOrderColumnEmbed;
    }

    public XMLListIntegerOrderByEmbed getListIntegerOrderByEmbed() {
        return this.listIntegerOrderByEmbed;
    }

    public void setListIntegerOrderByEmbed(XMLListIntegerOrderByEmbed listIntegerOrderByEmbed) {
        this.listIntegerOrderByEmbed = listIntegerOrderByEmbed;
    }

    public XMLListEnumeratedEmbed getListEnumeratedEmbed() {
        return this.listEnumeratedEmbed;
    }

    public void setListEnumeratedEmbed(XMLListEnumeratedEmbed listEnumeratedEmbed) {
        this.listEnumeratedEmbed = listEnumeratedEmbed;
    }

    public XMLListTemporalEmbed getListTemporalEmbed() {
        return this.listTemporalEmbed;
    }

    public void setListTemporalEmbed(XMLListTemporalEmbed listTemporalEmbed) {
        this.listTemporalEmbed = listTemporalEmbed;
    }

    public XMLListLobEmbed getListLobEmbed() {
        return this.listLobEmbed;
    }

    public void setListLobEmbed(XMLListLobEmbed listLobEmbed) {
        this.listLobEmbed = listLobEmbed;
    }

    public XMLSetIntegerEmbed getSetIntegerEmbed() {
        return this.setIntegerEmbed;
    }

    public void setSetIntegerEmbed(XMLSetIntegerEmbed setIntegerEmbed) {
        this.setIntegerEmbed = setIntegerEmbed;
    }

    public XMLSetEnumeratedEmbed getSetEnumeratedEmbed() {
        return this.setEnumeratedEmbed;
    }

    public void setSetEnumeratedEmbed(XMLSetEnumeratedEmbed setEnumeratedEmbed) {
        this.setEnumeratedEmbed = setEnumeratedEmbed;
    }

    public XMLSetTemporalEmbed getSetTemporalEmbed() {
        return this.setTemporalEmbed;
    }

    public void setSetTemporalEmbed(XMLSetTemporalEmbed setTemporalEmbed) {
        this.setTemporalEmbed = setTemporalEmbed;
    }

    public XMLSetLobEmbed getSetLobEmbed() {
        return this.setLobEmbed;
    }

    public void setSetLobEmbed(XMLSetLobEmbed setLobEmbed) {
        this.setLobEmbed = setLobEmbed;
    }

    public XMLMapKeyIntegerEmbed getMapKeyIntegerEmbed() {
        return this.mapKeyIntegerEmbed;
    }

    public void setMapKeyIntegerEmbed(XMLMapKeyIntegerEmbed mapKeyIntegerEmbed) {
        this.mapKeyIntegerEmbed = mapKeyIntegerEmbed;
    }

    public XMLMapKeyIntegerValueTemporalEmbed getMapKeyIntegerValueTemporalEmbed() {
        return this.mapKeyIntegerValueTemporalEmbed;
    }

    public void setMapKeyIntegerValueTemporalEmbed(XMLMapKeyIntegerValueTemporalEmbed mapKeyIntegerValueTemporalEmbed) {
        this.mapKeyIntegerValueTemporalEmbed = mapKeyIntegerValueTemporalEmbed;
    }

    public XMLMapKeyEnumeratedValueEnumeratedEmbed getMapKeyEnumeratedValueEnumeratedEmbed() {
        return this.mapKeyEnumeratedValueEnumeratedEmbed;
    }

    public void setMapKeyEnumeratedValueEnumeratedEmbed(XMLMapKeyEnumeratedValueEnumeratedEmbed mapKeyEnumeratedValueEnumeratedEmbed) {
        this.mapKeyEnumeratedValueEnumeratedEmbed = mapKeyEnumeratedValueEnumeratedEmbed;
    }

    public XMLMapKeyTemporalValueTemporalEmbed getMapKeyTemporalValueTemporalEmbed() {
        return this.mapKeyTemporalValueTemporalEmbed;
    }

    public void setMapKeyTemporalValueTemporalEmbed(XMLMapKeyTemporalValueTemporalEmbed mapKeyTemporalValueTemporalEmbed) {
        this.mapKeyTemporalValueTemporalEmbed = mapKeyTemporalValueTemporalEmbed;
    }

    public XMLMapKeyEnumeratedValueLobEmbed getMapKeyEnumeratedValueLobEmbed() {
        return this.mapKeyEnumeratedValueLobEmbed;
    }

    public void setMapKeyEnumeratedValueLobEmbed(XMLMapKeyEnumeratedValueLobEmbed mapKeyEnumeratedValueLobEmbed) {
        this.mapKeyEnumeratedValueLobEmbed = mapKeyEnumeratedValueLobEmbed;
    }

    public Collection<XMLLobPropertyAccessEmbed> getCollectionLobPropertyAccessEmbed() {
        return this.collectionLobPropertyAccessEmbed;
    }

    public void setCollectionLobPropertyAccessEmbed(Collection<XMLLobPropertyAccessEmbed> collectionLobPropertyAccessEmbed) {
        this.collectionLobPropertyAccessEmbed = collectionLobPropertyAccessEmbed;
    }

    public List<XMLIntegerEmbed> getListIntegerEmbedOrderColumn() {
        return this.listIntegerEmbedOrderColumn;
    }

    public void setListIntegerEmbedOrderColumn(List<XMLIntegerEmbed> listIntegerEmbedOrderColumn) {
        this.listIntegerEmbedOrderColumn = listIntegerEmbedOrderColumn;
    }

    public List<XMLIntegerAttributeOverridesEmbed> getListIntegerAttributeOverridesEmbedOrderColumn() {
        return this.listIntegerAttributeOverridesEmbedOrderColumn;
    }

    public void setListIntegerAttributeOverridesEmbedOrderColumn(List<XMLIntegerAttributeOverridesEmbed> listIntegerAttributeOverridesEmbedOrderColumn) {
        this.listIntegerAttributeOverridesEmbedOrderColumn = listIntegerAttributeOverridesEmbedOrderColumn;
    }

    public List<XMLIntegerPropertyAccessEmbed> getListIntegerPropertyAccessEmbedOrderColumn() {
        return this.listIntegerPropertyAccessEmbedOrderColumn;
    }

    public void setListIntegerPropertyAccessEmbedOrderColumn(List<XMLIntegerPropertyAccessEmbed> listIntegerPropertyAccessEmbedOrderColumn) {
        this.listIntegerPropertyAccessEmbedOrderColumn = listIntegerPropertyAccessEmbedOrderColumn;
    }

    public List<XMLEnumeratedPropertyAccessEmbed> getListEnumeratedPropertyAccessEmbedOrderColumn() {
        return this.listEnumeratedPropertyAccessEmbedOrderColumn;
    }

    public void setListEnumeratedPropertyAccessEmbedOrderColumn(List<XMLEnumeratedPropertyAccessEmbed> listEnumeratedPropertyAccessEmbedOrderColumn) {
        this.listEnumeratedPropertyAccessEmbedOrderColumn = listEnumeratedPropertyAccessEmbedOrderColumn;
    }

    public List<XMLEnumeratedPropertyAccessEmbed> getListEnumeratedPropertyAccessEmbedOrderBy() {
        return this.listEnumeratedPropertyAccessEmbedOrderBy;
    }

    public void setListEnumeratedPropertyAccessEmbedOrderBy(List<XMLEnumeratedPropertyAccessEmbed> listEnumeratedPropertyAccessEmbedOrderBy) {
        this.listEnumeratedPropertyAccessEmbedOrderBy = listEnumeratedPropertyAccessEmbedOrderBy;
    }

    public List<XMLTemporalPropertyAccessEmbed> getListTemporalPropertyAccessEmbedOrderColumn() {
        return this.listTemporalPropertyAccessEmbedOrderColumn;
    }

    public void setListTemporalPropertyAccessEmbedOrderColumn(List<XMLTemporalPropertyAccessEmbed> listTemporalPropertyAccessEmbedOrderColumn) {
        this.listTemporalPropertyAccessEmbedOrderColumn = listTemporalPropertyAccessEmbedOrderColumn;
    }

    public Set<XMLIntegerPropertyAccessEmbed> getSetIntegerPropertyAccessEmbed() {
        return this.setIntegerPropertyAccessEmbed;
    }

    public void setSetIntegerPropertyAccessEmbed(Set<XMLIntegerPropertyAccessEmbed> setIntegerPropertyAccessEmbed) {
        this.setIntegerPropertyAccessEmbed = setIntegerPropertyAccessEmbed;
    }

    public Map<Integer, XMLIntegerPropertyAccessEmbed> getMapKeyIntegerValueIntegerPropertyAccessEmbed() {
        return this.mapKeyIntegerValueIntegerPropertyAccessEmbed;
    }

    public void setMapKeyIntegerValueIntegerPropertyAccessEmbed(Map<Integer, XMLIntegerPropertyAccessEmbed> mapKeyIntegerValueIntegerPropertyAccessEmbed) {
        this.mapKeyIntegerValueIntegerPropertyAccessEmbed = mapKeyIntegerValueIntegerPropertyAccessEmbed;
    }

    public Map<Integer, XMLTemporalPropertyAccessEmbed> getMapKeyIntegerValueTemporalPropertyAccessEmbed() {
        return this.mapKeyIntegerValueTemporalPropertyAccessEmbed;
    }

    public void setMapKeyIntegerValueTemporalPropertyAccessEmbed(Map<Integer, XMLTemporalPropertyAccessEmbed> mapKeyIntegerValueTemporalPropertyAccessEmbed) {
        this.mapKeyIntegerValueTemporalPropertyAccessEmbed = mapKeyIntegerValueTemporalPropertyAccessEmbed;
    }

    public Map<Date, XMLTemporalPropertyAccessEmbed> getMapKeyTemporalValueTemporalPropertyAccessEmbed() {

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        Map<Date, XMLTemporalPropertyAccessEmbed> dateOnly = new HashMap<Date, XMLTemporalPropertyAccessEmbed>();
        Set<Date> keys = mapKeyTemporalValueTemporalPropertyAccessEmbed.keySet();
        String sDate = null;
        Date dDate = null;
        for (Date key : keys) {
            sDate = sdf.format(key);
            try {
                dDate = sdf.parse(sDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            //There will be a java.lang.IllegalArgumentException while executing new Date(sdf.format(key) in Chinese locale, fixed this in Chinese locale defect 220028
            // dateOnly.put(new Date(sdf.format(key)), mapKeyTemporalValueTemporalPropertyAccessEmbedOrderColumn.get(key));
            dateOnly.put(dDate, mapKeyTemporalValueTemporalPropertyAccessEmbed.get(key));
        }
        return dateOnly;
    }

    public void setMapKeyTemporalValueTemporalPropertyAccessEmbed(Map<Date, XMLTemporalPropertyAccessEmbed> mapKeyTemporalValueTemporalPropertyAccessEmbed) {
        this.mapKeyTemporalValueTemporalPropertyAccessEmbed = mapKeyTemporalValueTemporalPropertyAccessEmbed;
    }

    public Map<XMLEnumeratedPropertyAccessEnum, XMLEnumeratedPropertyAccessEmbed> getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed() {
        return this.mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed;
    }

    public void setMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(Map<XMLEnumeratedPropertyAccessEnum, XMLEnumeratedPropertyAccessEmbed> mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed) {
        this.mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed = mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed;
    }

    public Map<XMLIntegerEmbed, XMLIntegerEmbed> getMapKeyIntegerEmbedValueIntegerEmbed() {
        return this.mapKeyIntegerEmbedValueIntegerEmbed;
    }

    public void setMapKeyIntegerEmbedValueIntegerEmbed(Map<XMLIntegerEmbed, XMLIntegerEmbed> mapKeyIntegerEmbedValueIntegerEmbed) {
        this.mapKeyIntegerEmbedValueIntegerEmbed = mapKeyIntegerEmbedValueIntegerEmbed;
    }

    public Map<XMLLobPropertyAccessEmbed, XMLLobPropertyAccessEmbed> getMapKeyLobEmbedValueLobEmbed() {
        return this.mapKeyLobEmbedValueLobEmbed;
    }

    public void setMapKeyLobEmbedValueLobEmbed(Map<XMLLobPropertyAccessEmbed, XMLLobPropertyAccessEmbed> mapKeyLobEmbedValueLobEmbed) {
        this.mapKeyLobEmbedValueLobEmbed = mapKeyLobEmbedValueLobEmbed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id=" + id);
        sb.append(", " + integerEmbed);
        sb.append(", " + integerAttributeOverridesEmbed);
        sb.append(", " + integerFieldAccessEmbed);
        sb.append(", " + enumeratedFieldAccessEmbed);
        sb.append(", " + temporalFieldAccessEmbed);
        sb.append(", " + lobFieldAccessEmbed);
        sb.append(", " + integerTransientEmbed);
        sb.append(", " + integerPropertyAccessEmbed);
        sb.append(", " + enumeratedPropertyAccessEmbed);
        sb.append(", " + temporalPropertyAccessEmbed);
        sb.append(", " + lobPropertyAccessEmbed);
        sb.append(", " + collectionIntegerEmbed);
        sb.append(", " + collectionEnumeratedEmbed);
        sb.append(", " + collectionTemporalEmbed);
        sb.append(", " + collectionLobEmbed);
        sb.append(", " + listIntegerOrderColumnEmbed);
        sb.append(", " + listIntegerOrderByEmbed);
        sb.append(", " + listEnumeratedEmbed);
        sb.append(", " + listTemporalEmbed);
        sb.append(", " + listLobEmbed);
        sb.append(", " + setIntegerEmbed);
        sb.append(", " + setEnumeratedEmbed);
        sb.append(", " + setTemporalEmbed);
        sb.append(", " + setLobEmbed);
        sb.append(", " + mapKeyIntegerEmbed);
        sb.append(", " + mapKeyIntegerValueTemporalEmbed);
        sb.append(", " + mapKeyEnumeratedValueEnumeratedEmbed);
        sb.append(", " + mapKeyTemporalValueTemporalEmbed);
        sb.append(", " + mapKeyEnumeratedValueLobEmbed);
        if (collectionLobPropertyAccessEmbed != null)
            sb.append(", collectionLobPropertyAccessEmbed=" + collectionLobPropertyAccessEmbed.toString());
        else
            sb.append(", collectionLobPropertyAccessEmbed=null");
        if (listIntegerEmbedOrderColumn != null)
            sb.append(", listIntegerEmbedOrderColumn=" + listIntegerEmbedOrderColumn.toString());
        else
            sb.append(", listIntegerEmbedOrderColumn=null");
        if (listIntegerAttributeOverridesEmbedOrderColumn != null)
            sb.append(", listIntegerAttributeOverridesEmbedOrderColumn=" + listIntegerAttributeOverridesEmbedOrderColumn.toString());
        else
            sb.append(", listIntegerAttributeOverridesEmbedOrderColumn=null");
        if (listIntegerPropertyAccessEmbedOrderColumn != null)
            sb.append(", listIntegerPropertyAccessEmbedOrderColumn=" + listIntegerPropertyAccessEmbedOrderColumn.toString());
        else
            sb.append(", listIntegerPropertyAccessEmbedOrderColumn=null");
        if (listEnumeratedPropertyAccessEmbedOrderColumn != null)
            sb.append(", listEnumeratedPropertyAccessEmbedOrderColumn=" + listEnumeratedPropertyAccessEmbedOrderColumn.toString());
        else
            sb.append(", listEnumeratedPropertyAccessEmbedOrderColumn=null");
        if (listEnumeratedPropertyAccessEmbedOrderBy != null)
            sb.append(", listEnumeratedPropertyAccessEmbedOrderBy=" + listEnumeratedPropertyAccessEmbedOrderBy.toString());
        else
            sb.append(", listEnumeratedPropertyAccessEmbedOrderBy=null");
        if (listTemporalPropertyAccessEmbedOrderColumn != null)
            sb.append(", listTemporalPropertyAccessEmbedOrderColumn=" + listTemporalPropertyAccessEmbedOrderColumn.toString());
        else
            sb.append(", listTemporalPropertyAccessEmbedOrderColumn=null");
        if (setIntegerPropertyAccessEmbed != null)
            sb.append(", setIntegerPropertyAccessEmbed=" + setIntegerPropertyAccessEmbed.toString());
        else
            sb.append(", setIntegerPropertyAccessEmbed=null");
        if (mapKeyIntegerValueIntegerPropertyAccessEmbed != null)
            sb.append(", mapKeyIntegerValueIntegerPropertyAccessEmbed=" + mapKeyIntegerValueIntegerPropertyAccessEmbed.toString());
        else
            sb.append(", mapKeyIntegerValueIntegerPropertyAccessEmbed=null");
        if (mapKeyIntegerValueTemporalPropertyAccessEmbed != null)
            sb.append(", mapKeyIntegerValueTemporalPropertyAccessEmbed=" + mapKeyIntegerValueTemporalPropertyAccessEmbed.toString());
        else
            sb.append(", mapKeyIntegerValueTemporalPropertyAccessEmbed=null");
        if (mapKeyTemporalValueTemporalPropertyAccessEmbed != null)
            sb.append(", mapKeyTemporalValueTemporalPropertyAccessEmbed=" + getMapKeyTemporalValueTemporalPropertyAccessEmbed().toString());
        else
            sb.append(", mapKeyTemporalValueTemporalPropertyAccessEmbed=null");
        if (mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed != null)
            sb.append(", mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed=" + getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed().toString());
        else
            sb.append(", mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed=null");
        if (mapKeyIntegerEmbedValueIntegerEmbed != null)
            sb.append(", mapKeyIntegerEmbedValueIntegerEmbed=" + mapKeyIntegerEmbedValueIntegerEmbed.toString());
        else
            sb.append(", mapKeyIntegerEmbedValueIntegerEmbed=null");
        if (mapKeyLobEmbedValueLobEmbed != null)
            sb.append(", mapKeyLobEmbedValueLobEmbed=" + mapKeyLobEmbedValueLobEmbed.toString());
        else
            sb.append(", mapKeyLobEmbedValueLobEmbed=null");
        return sb.toString();
    }
}