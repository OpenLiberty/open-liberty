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
package io.openliberty.restfulWS30.fat.xml;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Entity {

    private String entityName;
    private int entityNumber;

    public Entity() {}
    public Entity(String entityName, int entityNumber) {
        this.entityName = entityName;
        this.entityNumber = entityNumber;
    }
    
    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public int getEntityNumber() {
        return entityNumber;
    }

    public void setEntityNumber(int entityNumber) {
        this.entityNumber = entityNumber;
    }

    @Override
    public String toString() {
        return "Entity(" + entityNumber + ", " + entityName + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || (!(o instanceof Entity))) {
            return false;
        }
        Entity other = (Entity) o;
        if (entityName == null && other.entityName != null ||
            entityName != null && other.entityName == null) {
            return false;
        }
        return entityNumber == other.entityNumber && (entityName == null || entityName.equals(other.entityName));
    }
}
