/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "PKGAO_GAOE")
public class GeneralAnnotationOverrideEntity {
    @Id
    private int id;

    @Basic(optional = false)
    private String name;

    @Basic(fetch = FetchType.EAGER)
    private String annotatedEagerName;

    @Basic(fetch = FetchType.LAZY)
    private String annotatedLazyName;

    @Column(unique = true)
    private String annotatedUniqueName;

    @Column(unique = false)
    private String annotatedNonUniqueName;

    @Column(length = 10)
    private String lengthBoundString;

    public String getAnnotatedEagerName() {
        return annotatedEagerName;
    }

    public void setAnnotatedEagerName(String annotatedEagerName) {
        this.annotatedEagerName = annotatedEagerName;
    }

    public String getAnnotatedLazyName() {
        return annotatedLazyName;
    }

    public void setAnnotatedLazyName(String annotatedLazyName) {
        this.annotatedLazyName = annotatedLazyName;
    }

    public String getAnnotatedNonUniqueName() {
        return annotatedNonUniqueName;
    }

    public void setAnnotatedNonUniqueName(String annotatedNonUniqueName) {
        this.annotatedNonUniqueName = annotatedNonUniqueName;
    }

    public String getAnnotatedUniqueName() {
        return annotatedUniqueName;
    }

    public void setAnnotatedUniqueName(String annotatedUniqueName) {
        this.annotatedUniqueName = annotatedUniqueName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLengthBoundString() {
        return lengthBoundString;
    }

    public void setLengthBoundString(String lengthBoundString) {
        this.lengthBoundString = lengthBoundString;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "GeneralAnnotationOverrideEntity [id=" + id + "]";
    }

}
