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
package com.ibm.ws.jpa.olgh8950.model;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SimpleMappingEntityOLGH8950 {
    @Id
    private long id;

    private int mappingField1;

    @Embedded
    private SimpleMappingEmbeddableOLGH8950 aggregateObjectMapping;

    public SimpleMappingEntityOLGH8950() {}

    public SimpleMappingEntityOLGH8950(long id, int mappingField1, SimpleMappingEmbeddableOLGH8950 aggregateObjectMapping) {
        this.id = id;
        this.mappingField1 = mappingField1;
        this.aggregateObjectMapping = aggregateObjectMapping;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getMappingField1() {
        return mappingField1;
    }

    public void setMappingField1(int mappingField1) {
        this.mappingField1 = mappingField1;
    }

    public SimpleMappingEmbeddableOLGH8950 getAggregateObjectMapping() {
        return aggregateObjectMapping;
    }

    public void setAggregateObjectMapping(SimpleMappingEmbeddableOLGH8950 aggregateObjectMapping) {
        this.aggregateObjectMapping = aggregateObjectMapping;
    }
}
