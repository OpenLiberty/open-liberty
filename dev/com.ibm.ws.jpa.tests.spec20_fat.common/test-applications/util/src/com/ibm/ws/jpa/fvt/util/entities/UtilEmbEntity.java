/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.util.entities;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class UtilEmbEntity {

    private int id;

    private int version;

    private String name;

    private UtilEmbeddable emb;

    private UtilEmbeddable emb1;

    private UtilEmbeddable2 initNullEmb;

    @Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Version
    public int getVersion() {
        return version;
    }

    // Setter method needed by EclipseLink
    public void setVersion(int version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Embedded
    public UtilEmbeddable getEmb() {
        return emb;
    }

    public void setEmb(UtilEmbeddable emb) {
        this.emb = emb;
    }

    @Embedded
    @AttributeOverrides({
                          @AttributeOverride(name = "embName", column = @Column(name = "emb1Name")),
                          @AttributeOverride(name = "embNotLoaded", column = @Column(name = "emb1NotLoaded"))
    })
    public UtilEmbeddable getEmb1() {
        return emb1;
    }

    public void setEmb1(UtilEmbeddable emb1) {
        this.emb1 = emb1;
    }

    @Embedded
    public UtilEmbeddable2 getInitNullEmb() {
        return initNullEmb;
    }

    public void setInitNullEmb(UtilEmbeddable2 initNullEmb) {
        this.initNullEmb = initNullEmb;
    }

    @Override
    public String toString() {
        return "UtilEmbEntity[id=" + id + ",ver=" + version + "]";
    }
}
