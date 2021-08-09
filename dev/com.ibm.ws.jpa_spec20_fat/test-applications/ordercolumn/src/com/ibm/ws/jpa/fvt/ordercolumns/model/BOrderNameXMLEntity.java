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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

@Entity
@Table(name = "BONameXE")
public class BOrderNameXMLEntity implements java.io.Serializable {

    private static final long serialVersionUID = -1059986449941927485L;

    @Id
    private int id;
    private String name;

    @ManyToOne
    private XMLOrderColumnEntity xmlColumn;

    @ManyToMany(mappedBy = "bm2mNames")
    @OrderColumn
    private List<XMLOrderColumnEntity> xmlColumns;

    public BOrderNameXMLEntity() {
    }

    public BOrderNameXMLEntity(String name) {
        this.id = name.charAt(0) - 'A' + 1;
        this.name = name;
    }

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

    public XMLOrderColumnEntity getXmlColumn() {
        return xmlColumn;
    }

    public void setXmlColumn(XMLOrderColumnEntity column) {
        this.xmlColumn = column;
    }

    public List<XMLOrderColumnEntity> getXmlColumns() {
        return xmlColumns;
    }

    public void setXmlColumns(List<XMLOrderColumnEntity> columns) {
        this.xmlColumns = columns;
    }

    public void addXmlColumns(XMLOrderColumnEntity column) {
        if (xmlColumns == null) {
            xmlColumns = new ArrayList<XMLOrderColumnEntity>();
        }
        xmlColumns.add(column);
    }

    public XMLOrderColumnEntity removeXmlColumns(XMLOrderColumnEntity entity) {
        XMLOrderColumnEntity rtnVal = null;
        if (xmlColumns != null) {
            if (xmlColumns.remove(entity))
                rtnVal = entity;
        }
        return rtnVal;
    }

    @Override
    public String toString() {
        return name;
    }
}
