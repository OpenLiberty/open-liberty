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
@Table(name = "DefOColE")
public class DefinitionOrderColumnEntity implements java.io.Serializable {

    private static final long serialVersionUID = -7796256815144917778L;

    @Id
    private int id;
    private String name;

    @ElementCollection
    @OrderColumn(name = "OCDefElements_ODR")
    @CollectionTable(name = "DefOColE_oColDefElem")
    private List<String> orderColumnDefinitionElements;

    @ElementCollection
    @OrderColumn(name = "OVROCDefElements_ODR", columnDefinition = "SMALLINT")
    @CollectionTable(name = "DefOColE_ovrOColDefElem")
    private List<String> overrideOrderColumnDefinitionElements;

    private List<String> xmlOrderColumnDefinitionElements;
    private List<String> xmlOverrideOrderColumnDefinitionElements;

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

    public List<String> getOrderColumnDefinitionElements() {
        return orderColumnDefinitionElements;
    }

    public void setOrderColumnDefinitionElements(List<String> elements) {
        this.orderColumnDefinitionElements = elements;
    }

    public List<String> getXmlOrderColumnDefinitionElements() {
        return xmlOrderColumnDefinitionElements;
    }

    public void setXmlOrderColumnDefinitionElements(List<String> elements) {
        this.xmlOrderColumnDefinitionElements = elements;
    }

    public List<String> getOverrideOrderColumnDefinitionElements() {
        return overrideOrderColumnDefinitionElements;
    }

    public void setOverrideOrderColumnDefinitionElements(List<String> elements) {
        this.overrideOrderColumnDefinitionElements = elements;
    }

    public List<String> getXmlOverrideOrderColumnDefinitionElements() {
        return xmlOverrideOrderColumnDefinitionElements;
    }

    public void setXmlOverrideOrderColumnDefinitionElements(List<String> elements) {
        this.xmlOverrideOrderColumnDefinitionElements = elements;
    }

    @Override
    public String toString() {
        return "DefinitionOrderColumnEntity[" + id + "]=" + name;
    }
}
