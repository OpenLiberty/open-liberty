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

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyTemporal;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.TemporalType;

import com.ibm.ws.jpa.embeddable.basic.model.EnumeratedPropertyAccessEmbed.EnumeratedPropertyAccessEnum;

@Entity
@Table(name = "AnnRootEmBT")
public class JPAEmbeddableBasicEntity {

    @Id
    private int id;

    @Embedded
    private IntegerEmbed integerEmbed;
    @Embedded
    @AttributeOverrides({ @AttributeOverride(name = "notIntegerValue", column = @Column(name = "integerValueAttributeOverride")) })
    private IntegerAttributeOverridesEmbed integerAttributeOverridesEmbed;

    @Embedded
    private IntegerFieldAccessEmbed integerFieldAccessEmbed;
    @Embedded
    private EnumeratedFieldAccessEmbed enumeratedFieldAccessEmbed;
    @Embedded
    private TemporalFieldAccessEmbed temporalFieldAccessEmbed;
    @Embedded
    private LobFieldAccessEmbed lobFieldAccessEmbed;

    @Embedded
    private IntegerTransientEmbed integerTransientEmbed;

    @Embedded
    private IntegerPropertyAccessEmbed integerPropertyAccessEmbed;
    @Embedded
    private EnumeratedPropertyAccessEmbed enumeratedPropertyAccessEmbed;
    @Embedded
    private TemporalPropertyAccessEmbed temporalPropertyAccessEmbed;
    @Embedded
    private LobPropertyAccessEmbed lobPropertyAccessEmbed;

    @Embedded
    private CollectionIntegerEmbed collectionIntegerEmbed;
    // CollectionIntegerAttributeOverridesEmbed is disallowed by the spec per
    // 10.1.4.
    @Embedded
    private CollectionEnumeratedEmbed collectionEnumeratedEmbed;
    @Embedded
    private CollectionTemporalEmbed collectionTemporalEmbed;
    @Embedded
    private CollectionLobEmbed collectionLobEmbed;

    @Embedded
    private ListIntegerOrderColumnEmbed listIntegerOrderColumnEmbed;
    @Embedded
    private ListIntegerOrderByEmbed listIntegerOrderByEmbed;
    // ListIntegerAttributeOverridesEmbed is disallowed by the spec per 10.1.4.
    @Embedded
    private ListEnumeratedEmbed listEnumeratedEmbed;
    @Embedded
    private ListTemporalEmbed listTemporalEmbed;
    @Embedded
    private ListLobEmbed listLobEmbed;

    @Embedded
    private SetIntegerEmbed setIntegerEmbed;
    // SetIntegerAttributeOverridesEmbed is disallowed by the spec per 10.1.4.
    @Embedded
    private SetEnumeratedEmbed setEnumeratedEmbed;
    @Embedded
    private SetTemporalEmbed setTemporalEmbed;
    @Embedded
    private SetLobEmbed setLobEmbed;

    @Embedded
    private MapKeyIntegerEmbed mapKeyIntegerEmbed;
    // MapKeyIntegerValueIntegerAttributeOverridesEmbed is disallowed by the
    // spec per 10.1.4.
    @Embedded
    private MapKeyIntegerValueTemporalEmbed mapKeyIntegerValueTemporalEmbed;
    @Embedded
    private MapKeyEnumeratedValueEnumeratedEmbed mapKeyEnumeratedValueEnumeratedEmbed;
    @Embedded
    private MapKeyTemporalValueTemporalEmbed mapKeyTemporalValueTemporalEmbed;
    @Embedded
    private MapKeyEnumeratedValueLobEmbed mapKeyEnumeratedValueLobEmbed;

//  JPA 1.0 spec: Support for collections of embedded objects and for the polymorphism and inheritance of embeddable classes will be required in  a future release of this specification.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntColLobPAE", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    private Collection<LobPropertyAccessEmbed> collectionLobPropertyAccessEmbed;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntListIntegerE", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    private List<IntegerEmbed> listIntegerEmbedOrderColumn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntListIntegerAOE", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    @AttributeOverrides({ @AttributeOverride(name = "notIntegerValue", column = @Column(name = "integerValueAttributeOverride")) })
    private List<IntegerAttributeOverridesEmbed> listIntegerAttributeOverridesEmbedOrderColumn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntListIntegerPAE", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    private List<IntegerPropertyAccessEmbed> listIntegerPropertyAccessEmbedOrderColumn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntListEnumPAEOrderColumn", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    private List<EnumeratedPropertyAccessEmbed> listEnumeratedPropertyAccessEmbedOrderColumn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntListEnumPAEOrderBy", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderBy("enumeratedOrdinalValuePA")
    private List<EnumeratedPropertyAccessEmbed> listEnumeratedPropertyAccessEmbedOrderBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntListTemporalPAE", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    private List<TemporalPropertyAccessEmbed> listTemporalPropertyAccessEmbedOrderColumn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntSetIntegerPAE", joinColumns = @JoinColumn(name = "parent_id"))
    private Set<IntegerPropertyAccessEmbed> setIntegerPropertyAccessEmbed;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntMapIntegerIntegerPAE", joinColumns = @JoinColumn(name = "parent_id"))
    @MapKeyColumn(name = "mykey")
    @AttributeOverrides({ @AttributeOverride(name = "value.integerValuePropertyAccessColumn", column = @Column(name = "value")) })
    private Map<Integer, IntegerPropertyAccessEmbed> mapKeyIntegerValueIntegerPropertyAccessEmbed;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntMapIntegerTemporalPAE", joinColumns = @JoinColumn(name = "parent_id"))
    @MapKeyColumn(name = "mykey")
//    @OrderColumn(name = "valueOrderColumn")
    private Map<Integer, TemporalPropertyAccessEmbed> mapKeyIntegerValueTemporalPropertyAccessEmbed;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntMapDateTemporalPAE", joinColumns = @JoinColumn(name = "parent_id"))
    @MapKeyColumn(name = "mykey")
    @MapKeyTemporal(TemporalType.DATE)
//    @OrderColumn(name = "valueOrderColumn")
    private Map<Date, TemporalPropertyAccessEmbed> mapKeyTemporalValueTemporalPropertyAccessEmbed;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntMapEnumPAEEnumPAE", joinColumns = @JoinColumn(name = "parent_id"))
    @MapKeyColumn(name = "mykey")
    @MapKeyEnumerated(EnumType.STRING)
//    @OrderColumn(name = "valueOrderColumn")
    @AttributeOverrides({ @AttributeOverride(name = "value.enumeratedStringValuePA", column = @Column(name = "valueString")),
                          @AttributeOverride(name = "value.enumeratedOrdinalValuePA", column = @Column(name = "valueOrdinal")) })
    private Map<EnumeratedPropertyAccessEnum, EnumeratedPropertyAccessEmbed> mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntMapIntegerEIntegerE", joinColumns = @JoinColumn(name = "parent_id"))
    @AttributeOverrides({ @AttributeOverride(name = "key.integerValue", column = @Column(name = "mykey")),
                          @AttributeOverride(name = "value.integerValue", column = @Column(name = "value")) })
    private Map<IntegerEmbed, IntegerEmbed> mapKeyIntegerEmbedValueIntegerEmbed;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntMapLobPAELobPAE", joinColumns = @JoinColumn(name = "parent_id"))
    @AttributeOverrides({ @AttributeOverride(name = "key.clobValuePA", column = @Column(name = "mykey")),
                          @AttributeOverride(name = "value.clobValuePA", column = @Column(name = "value")) })
    private Map<LobPropertyAccessEmbed, LobPropertyAccessEmbed> mapKeyLobEmbedValueLobEmbed;

    public JPAEmbeddableBasicEntity() {
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public IntegerEmbed getIntegerEmbed() {
        return this.integerEmbed;
    }

    public void setIntegerEmbed(IntegerEmbed integerEmbed) {
        this.integerEmbed = integerEmbed;
    }

    public IntegerAttributeOverridesEmbed getIntegerAttributeOverridesEmbed() {
        return this.integerAttributeOverridesEmbed;
    }

    public void setIntegerAttributeOverridesEmbed(IntegerAttributeOverridesEmbed integerAttributeOverridesEmbed) {
        this.integerAttributeOverridesEmbed = integerAttributeOverridesEmbed;
    }

    public IntegerFieldAccessEmbed getIntegerFieldAccessEmbed() {
        return this.integerFieldAccessEmbed;
    }

    public void setIntegerFieldAccessEmbed(IntegerFieldAccessEmbed integerFieldAccessEmbed) {
        this.integerFieldAccessEmbed = integerFieldAccessEmbed;
    }

    public EnumeratedFieldAccessEmbed getEnumeratedFieldAccessEmbed() {
        return this.enumeratedFieldAccessEmbed;
    }

    public void setEnumeratedFieldAccessEmbed(EnumeratedFieldAccessEmbed enumeratedFieldAccessEmbed) {
        this.enumeratedFieldAccessEmbed = enumeratedFieldAccessEmbed;
    }

    public TemporalFieldAccessEmbed getTemporalFieldAccessEmbed() {
        return this.temporalFieldAccessEmbed;
    }

    public void setTemporalFieldAccessEmbed(TemporalFieldAccessEmbed temporalFieldAccessEmbed) {
        this.temporalFieldAccessEmbed = temporalFieldAccessEmbed;
    }

    public LobFieldAccessEmbed getLobFieldAccessEmbed() {
        return this.lobFieldAccessEmbed;
    }

    public void setLobFieldAccessEmbed(LobFieldAccessEmbed lobFieldAccessEmbed) {
        this.lobFieldAccessEmbed = lobFieldAccessEmbed;
    }

    public IntegerTransientEmbed getIntegerTransientEmbed() {
        return this.integerTransientEmbed;
    }

    public void setIntegerTransientEmbed(IntegerTransientEmbed integerTransientEmbed) {
        this.integerTransientEmbed = integerTransientEmbed;
    }

    public IntegerPropertyAccessEmbed getIntegerPropertyAccessEmbed() {
        return this.integerPropertyAccessEmbed;
    }

    public void setIntegerPropertyAccessEmbed(IntegerPropertyAccessEmbed integerPropertyAccessEmbed) {
        this.integerPropertyAccessEmbed = integerPropertyAccessEmbed;
    }

    public EnumeratedPropertyAccessEmbed getEnumeratedPropertyAccessEmbed() {
        return this.enumeratedPropertyAccessEmbed;
    }

    public void setEnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEmbed enumeratedPropertyAccessEmbed) {
        this.enumeratedPropertyAccessEmbed = enumeratedPropertyAccessEmbed;
    }

    public TemporalPropertyAccessEmbed getTemporalPropertyAccessEmbed() {
        return this.temporalPropertyAccessEmbed;
    }

    public void setTemporalPropertyAccessEmbed(TemporalPropertyAccessEmbed temporalPropertyAccessEmbed) {
        this.temporalPropertyAccessEmbed = temporalPropertyAccessEmbed;
    }

    public LobPropertyAccessEmbed getLobPropertyAccessEmbed() {
        return this.lobPropertyAccessEmbed;
    }

    public void setLobPropertyAccessEmbed(LobPropertyAccessEmbed lobPropertyAccessEmbed) {
        this.lobPropertyAccessEmbed = lobPropertyAccessEmbed;
    }

    public CollectionIntegerEmbed getCollectionIntegerEmbed() {
        return this.collectionIntegerEmbed;
    }

    public void setCollectionIntegerEmbed(CollectionIntegerEmbed collectionIntegerEmbed) {
        this.collectionIntegerEmbed = collectionIntegerEmbed;
    }

    public CollectionEnumeratedEmbed getCollectionEnumeratedEmbed() {
        return this.collectionEnumeratedEmbed;
    }

    public void setCollectionEnumeratedEmbed(CollectionEnumeratedEmbed collectionEnumeratedEmbed) {
        this.collectionEnumeratedEmbed = collectionEnumeratedEmbed;
    }

    public CollectionTemporalEmbed getCollectionTemporalEmbed() {
        return this.collectionTemporalEmbed;
    }

    public void setCollectionTemporalEmbed(CollectionTemporalEmbed collectionTemporalEmbed) {
        this.collectionTemporalEmbed = collectionTemporalEmbed;
    }

    public CollectionLobEmbed getCollectionLobEmbed() {
        return this.collectionLobEmbed;
    }

    public void setCollectionLobEmbed(CollectionLobEmbed collectionLobEmbed) {
        this.collectionLobEmbed = collectionLobEmbed;
    }

    public ListIntegerOrderColumnEmbed getListIntegerOrderColumnEmbed() {
        return this.listIntegerOrderColumnEmbed;
    }

    public void setListIntegerOrderColumnEmbed(ListIntegerOrderColumnEmbed listIntegerOrderColumnEmbed) {
        this.listIntegerOrderColumnEmbed = listIntegerOrderColumnEmbed;
    }

    public ListIntegerOrderByEmbed getListIntegerOrderByEmbed() {
        return this.listIntegerOrderByEmbed;
    }

    public void setListIntegerOrderByEmbed(ListIntegerOrderByEmbed listIntegerOrderByEmbed) {
        this.listIntegerOrderByEmbed = listIntegerOrderByEmbed;
    }

    public ListEnumeratedEmbed getListEnumeratedEmbed() {
        return this.listEnumeratedEmbed;
    }

    public void setListEnumeratedEmbed(ListEnumeratedEmbed listEnumeratedEmbed) {
        this.listEnumeratedEmbed = listEnumeratedEmbed;
    }

    public ListTemporalEmbed getListTemporalEmbed() {
        return this.listTemporalEmbed;
    }

    public void setListTemporalEmbed(ListTemporalEmbed listTemporalEmbed) {
        this.listTemporalEmbed = listTemporalEmbed;
    }

    public ListLobEmbed getListLobEmbed() {
        return this.listLobEmbed;
    }

    public void setListLobEmbed(ListLobEmbed listLobEmbed) {
        this.listLobEmbed = listLobEmbed;
    }

    public SetIntegerEmbed getSetIntegerEmbed() {
        return this.setIntegerEmbed;
    }

    public void setSetIntegerEmbed(SetIntegerEmbed setIntegerEmbed) {
        this.setIntegerEmbed = setIntegerEmbed;
    }

    public SetEnumeratedEmbed getSetEnumeratedEmbed() {
        return this.setEnumeratedEmbed;
    }

    public void setSetEnumeratedEmbed(SetEnumeratedEmbed setEnumeratedEmbed) {
        this.setEnumeratedEmbed = setEnumeratedEmbed;
    }

    public SetTemporalEmbed getSetTemporalEmbed() {
        return this.setTemporalEmbed;
    }

    public void setSetTemporalEmbed(SetTemporalEmbed setTemporalEmbed) {
        this.setTemporalEmbed = setTemporalEmbed;
    }

    public SetLobEmbed getSetLobEmbed() {
        return this.setLobEmbed;
    }

    public void setSetLobEmbed(SetLobEmbed setLobEmbed) {
        this.setLobEmbed = setLobEmbed;
    }

    public MapKeyIntegerEmbed getMapKeyIntegerEmbed() {
        return this.mapKeyIntegerEmbed;
    }

    public void setMapKeyIntegerEmbed(MapKeyIntegerEmbed mapKeyIntegerEmbed) {
        this.mapKeyIntegerEmbed = mapKeyIntegerEmbed;
    }

    public MapKeyIntegerValueTemporalEmbed getMapKeyIntegerValueTemporalEmbed() {
        return this.mapKeyIntegerValueTemporalEmbed;
    }

    public void setMapKeyIntegerValueTemporalEmbed(MapKeyIntegerValueTemporalEmbed mapKeyIntegerValueTemporalEmbed) {
        this.mapKeyIntegerValueTemporalEmbed = mapKeyIntegerValueTemporalEmbed;
    }

    public MapKeyEnumeratedValueEnumeratedEmbed getMapKeyEnumeratedValueEnumeratedEmbed() {
        return this.mapKeyEnumeratedValueEnumeratedEmbed;
    }

    public void setMapKeyEnumeratedValueEnumeratedEmbed(MapKeyEnumeratedValueEnumeratedEmbed mapKeyEnumeratedValueEnumeratedEmbed) {
        this.mapKeyEnumeratedValueEnumeratedEmbed = mapKeyEnumeratedValueEnumeratedEmbed;
    }

    public MapKeyTemporalValueTemporalEmbed getMapKeyTemporalValueTemporalEmbed() {
        return this.mapKeyTemporalValueTemporalEmbed;
    }

    public void setMapKeyTemporalValueTemporalEmbed(MapKeyTemporalValueTemporalEmbed mapKeyTemporalValueTemporalEmbed) {
        this.mapKeyTemporalValueTemporalEmbed = mapKeyTemporalValueTemporalEmbed;
    }

    public MapKeyEnumeratedValueLobEmbed getMapKeyEnumeratedValueLobEmbed() {
        return this.mapKeyEnumeratedValueLobEmbed;
    }

    public void setMapKeyEnumeratedValueLobEmbed(MapKeyEnumeratedValueLobEmbed mapKeyEnumeratedValueLobEmbed) {
        this.mapKeyEnumeratedValueLobEmbed = mapKeyEnumeratedValueLobEmbed;
    }

    public Collection<LobPropertyAccessEmbed> getCollectionLobPropertyAccessEmbed() {
        return this.collectionLobPropertyAccessEmbed;
    }

    public void setCollectionLobPropertyAccessEmbed(Collection<LobPropertyAccessEmbed> collectionLobPropertyAccessEmbed) {
        this.collectionLobPropertyAccessEmbed = collectionLobPropertyAccessEmbed;
    }

    public List<IntegerEmbed> getListIntegerEmbedOrderColumn() {
        return this.listIntegerEmbedOrderColumn;
    }

    public void setListIntegerEmbedOrderColumn(List<IntegerEmbed> listIntegerEmbedOrderColumn) {
        this.listIntegerEmbedOrderColumn = listIntegerEmbedOrderColumn;
    }

    public List<IntegerAttributeOverridesEmbed> getListIntegerAttributeOverridesEmbedOrderColumn() {
        return this.listIntegerAttributeOverridesEmbedOrderColumn;
    }

    public void setListIntegerAttributeOverridesEmbedOrderColumn(List<IntegerAttributeOverridesEmbed> listIntegerAttributeOverridesEmbedOrderColumn) {
        this.listIntegerAttributeOverridesEmbedOrderColumn = listIntegerAttributeOverridesEmbedOrderColumn;
    }

    public List<IntegerPropertyAccessEmbed> getListIntegerPropertyAccessEmbedOrderColumn() {
        return this.listIntegerPropertyAccessEmbedOrderColumn;
    }

    public void setListIntegerPropertyAccessEmbedOrderColumn(List<IntegerPropertyAccessEmbed> listIntegerPropertyAccessEmbedOrderColumn) {
        this.listIntegerPropertyAccessEmbedOrderColumn = listIntegerPropertyAccessEmbedOrderColumn;
    }

    public List<EnumeratedPropertyAccessEmbed> getListEnumeratedPropertyAccessEmbedOrderColumn() {
        return this.listEnumeratedPropertyAccessEmbedOrderColumn;
    }

    public void setListEnumeratedPropertyAccessEmbedOrderColumn(List<EnumeratedPropertyAccessEmbed> listEnumeratedPropertyAccessEmbedOrderColumn) {
        this.listEnumeratedPropertyAccessEmbedOrderColumn = listEnumeratedPropertyAccessEmbedOrderColumn;
    }

    public List<EnumeratedPropertyAccessEmbed> getListEnumeratedPropertyAccessEmbedOrderBy() {
        return this.listEnumeratedPropertyAccessEmbedOrderBy;
    }

    public void setListEnumeratedPropertyAccessEmbedOrderBy(List<EnumeratedPropertyAccessEmbed> listEnumeratedPropertyAccessEmbedOrderBy) {
        this.listEnumeratedPropertyAccessEmbedOrderBy = listEnumeratedPropertyAccessEmbedOrderBy;
    }

    public List<TemporalPropertyAccessEmbed> getListTemporalPropertyAccessEmbedOrderColumn() {
        return this.listTemporalPropertyAccessEmbedOrderColumn;
    }

    public void setListTemporalPropertyAccessEmbedOrderColumn(List<TemporalPropertyAccessEmbed> listTemporalPropertyAccessEmbedOrderColumn) {
        this.listTemporalPropertyAccessEmbedOrderColumn = listTemporalPropertyAccessEmbedOrderColumn;
    }

    public Set<IntegerPropertyAccessEmbed> getSetIntegerPropertyAccessEmbed() {
        return this.setIntegerPropertyAccessEmbed;
    }

    public void setSetIntegerPropertyAccessEmbed(Set<IntegerPropertyAccessEmbed> setIntegerPropertyAccessEmbed) {
        this.setIntegerPropertyAccessEmbed = setIntegerPropertyAccessEmbed;
    }

    public Map<Integer, IntegerPropertyAccessEmbed> getMapKeyIntegerValueIntegerPropertyAccessEmbed() {
        return this.mapKeyIntegerValueIntegerPropertyAccessEmbed;
    }

    public void setMapKeyIntegerValueIntegerPropertyAccessEmbed(Map<Integer, IntegerPropertyAccessEmbed> mapKeyIntegerValueIntegerPropertyAccessEmbed) {
        this.mapKeyIntegerValueIntegerPropertyAccessEmbed = mapKeyIntegerValueIntegerPropertyAccessEmbed;
    }

    public Map<Integer, TemporalPropertyAccessEmbed> getMapKeyIntegerValueTemporalPropertyAccessEmbed() {
        return this.mapKeyIntegerValueTemporalPropertyAccessEmbed;
    }

    public void setMapKeyIntegerValueTemporalPropertyAccessEmbed(Map<Integer, TemporalPropertyAccessEmbed> mapKeyIntegerValueTemporalPropertyAccessEmbed) {
        this.mapKeyIntegerValueTemporalPropertyAccessEmbed = mapKeyIntegerValueTemporalPropertyAccessEmbed;
    }

    public Map<Date, TemporalPropertyAccessEmbed> getMapKeyTemporalValueTemporalPropertyAccessEmbed() {

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        Map<Date, TemporalPropertyAccessEmbed> dateOnly = new HashMap<Date, TemporalPropertyAccessEmbed>();
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

    public void setMapKeyTemporalValueTemporalPropertyAccessEmbed(Map<Date, TemporalPropertyAccessEmbed> mapKeyTemporalValueTemporalPropertyAccessEmbed) {
        this.mapKeyTemporalValueTemporalPropertyAccessEmbed = mapKeyTemporalValueTemporalPropertyAccessEmbed;
    }

    public Map<EnumeratedPropertyAccessEnum, EnumeratedPropertyAccessEmbed> getMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed() {
        return this.mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed;
    }

    public void setMapKeyEnumeratedValueEnumeratedPropertyAccessEmbed(Map<EnumeratedPropertyAccessEnum, EnumeratedPropertyAccessEmbed> mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed) {
        this.mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed = mapKeyEnumeratedValueEnumeratedPropertyAccessEmbed;
    }

    public Map<IntegerEmbed, IntegerEmbed> getMapKeyIntegerEmbedValueIntegerEmbed() {
        return this.mapKeyIntegerEmbedValueIntegerEmbed;
    }

    public void setMapKeyIntegerEmbedValueIntegerEmbed(Map<IntegerEmbed, IntegerEmbed> mapKeyIntegerEmbedValueIntegerEmbed) {
        this.mapKeyIntegerEmbedValueIntegerEmbed = mapKeyIntegerEmbedValueIntegerEmbed;
    }

    public Map<LobPropertyAccessEmbed, LobPropertyAccessEmbed> getMapKeyLobEmbedValueLobEmbed() {
        return this.mapKeyLobEmbedValueLobEmbed;
    }

    public void setMapKeyLobEmbedValueLobEmbed(Map<LobPropertyAccessEmbed, LobPropertyAccessEmbed> mapKeyLobEmbedValueLobEmbed) {
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
            sb.append(", collectionLobPropertyAccessEmbedOrderColumn=" + collectionLobPropertyAccessEmbed.toString());
        else
            sb.append(", collectionLobPropertyAccessEmbedOrderColumn=null");
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
            sb.append(", listEnumeratedPropertyAccessColumnEmbedOrderColumn=" + listEnumeratedPropertyAccessEmbedOrderColumn.toString());
        else
            sb.append(", listEnumeratedPropertyAccessColumnEmbedOrderColumn=null");
        if (listEnumeratedPropertyAccessEmbedOrderBy != null)
            sb.append(", listEnumeratedPropertyAccessColumnEmbedOrderBy=" + listEnumeratedPropertyAccessEmbedOrderBy.toString());
        else
            sb.append(", listEnumeratedPropertyAccessColumnEmbedOrderBy=null");
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
