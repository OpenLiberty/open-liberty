/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.ordercolumns.model;

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

@Entity
@Table(name = "AttrOColE")
public class AttributeOrderColumnEntity implements java.io.Serializable {

    private static final long serialVersionUID = 7665602775075426355L;

    @Id
    private int id;
    private String name;

    @ElementCollection //(fetch=FetchType.EAGER)
    @OrderColumn
    @CollectionTable(name = "AttrOColE_oNameTypeElem")
    private List<String> orderNameTypeElements;

    @ElementCollection //(fetch=FetchType.EAGER)
    @OrderColumn(name = "NONNULLABLE_ORDER", nullable = false, insertable = false)
    @CollectionTable(name = "AttrOColE_nonNullAnnoElem")
    private List<String> nonNullableAnnotatedElements;

    @ElementCollection //(fetch=FetchType.EAGER)
    @OrderColumn(name = "NONINSERTABLE_ORDER", insertable = false)
    @CollectionTable(name = "AttrOColE_nonInsertAnnoElem")
    private List<String> nonInsertableAnnotatedElements;

    @ElementCollection //(fetch=FetchType.EAGER)
    @OrderColumn(name = "NONUPDATABLE_ORDER", updatable = false)
    @CollectionTable(name = "AttrOColE_nonUpdateAnnoElem")
    private List<String> nonUpdableAnnotatedElements;

    private List<String> nonNullableXmlElements;

    private List<String> nonInsertableXmlElements;

    private List<String> nonUpdableXmlElements;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getOrderNameTypeElements() {
        return orderNameTypeElements;
    }

    public void setOrderNameTypeElements(List<String> elements) {
        this.orderNameTypeElements = elements;
    }

    public List<String> getNonNullableAnnotatedElements() {
        return nonNullableAnnotatedElements;
    }

    public void setNonNullableAnnotatedElements(List<String> elements) {
        this.nonNullableAnnotatedElements = elements;
    }

    public List<String> getNonInsertableAnnotatedElements() {
        return nonInsertableAnnotatedElements;
    }

    public void setNonInsertableAnnotatedElements(List<String> elements) {
        this.nonInsertableAnnotatedElements = elements;
    }

    public List<String> getNonUpdableAnnotatedElements() {
        return nonUpdableAnnotatedElements;
    }

    public void setNonUpdableAnnotatedElements(List<String> elements) {
        this.nonUpdableAnnotatedElements = elements;
    }

    public List<String> getNonNullableXmlElements() {
        return nonNullableXmlElements;
    }

    public void setNonNullableXmlElements(List<String> elements) {
        this.nonNullableXmlElements = elements;
    }

    public List<String> getNonInsertableXmlElements() {
        return nonInsertableXmlElements;
    }

    public void setNonInsertableXmlElements(List<String> elements) {
        this.nonInsertableXmlElements = elements;
    }

    public List<String> getNonUpdableXmlElements() {
        return nonUpdableXmlElements;
    }

    public void setNonUpdableXmlElements(List<String> elements) {
        this.nonUpdableXmlElements = elements;
    }

    @Override
    public String toString() {
        return "OrderNameAttributeEntity[" + id + "]=" + name;
    }
}
