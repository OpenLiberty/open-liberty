/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh8014.model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * The purpose of this class is for querying an Entity table with no results.
 */
@Entity
public class NoResultEntityOLGH8014 {
    @Id
    private int id;

    private String content;

    @Basic
    private int primitive;

    private Integer wrapper;

    NoResultEntityOLGH8014() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getPrimitive() {
        return primitive;
    }

    public void setPrimitive(int primitive) {
        this.primitive = primitive;
    }

    public Integer getWrapper() {
        return wrapper;
    }

    public void setWrapper(Integer wrapper) {
        this.wrapper = wrapper;
    }
}
