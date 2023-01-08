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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities;

public class EntListTestEntity {
    private int id;
    private String name;

    private transient boolean entityListenerFQ = false;
    private transient boolean entityListenerNFQ = false;
    private transient boolean entityDefaultListenerFQ = false;
    private transient boolean entityDefaultListenerNFQ = false;

    public void callbackEntityListenerFQ() {
        entityListenerFQ = true;
    }

    public void callbackEntityListenerNFQ() {
        entityListenerNFQ = true;
    }

    public void callbackEntityDefaultListenerFQ() {
        entityDefaultListenerFQ = true;
    }

    public void callbackEntityDefaultListenerNFQ() {
        entityDefaultListenerNFQ = true;
    }

    public boolean isEntityDefaultListenerFQ() {
        return entityDefaultListenerFQ;
    }

    public void setEntityDefaultListenerFQ(boolean entityDefaultListenerFQ) {
        this.entityDefaultListenerFQ = entityDefaultListenerFQ;
    }

    public boolean isEntityDefaultListenerNFQ() {
        return entityDefaultListenerNFQ;
    }

    public void setEntityDefaultListenerNFQ(boolean entityDefaultListenerNFQ) {
        this.entityDefaultListenerNFQ = entityDefaultListenerNFQ;
    }

    public boolean isEntityListenerFQ() {
        return entityListenerFQ;
    }

    public void setEntityListenerFQ(boolean entityListenerFQ) {
        this.entityListenerFQ = entityListenerFQ;
    }

    public boolean isEntityListenerNFQ() {
        return entityListenerNFQ;
    }

    public void setEntityListenerNFQ(boolean entityListenerNFQ) {
        this.entityListenerNFQ = entityListenerNFQ;
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

    @Override
    public String toString() {
        return "EntListTestEntity [id=" + id + ", name=" + name + "]";
    }

}
