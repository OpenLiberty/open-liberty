/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.jpa.tests.jpa31.models;

import java.util.UUID;

public class XMLUUIDAutoGenEntity {
    private UUID id;

    private String strData;

    public XMLUUIDAutoGenEntity() {

    }

    /**
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * @return the strData
     */
    public String getStrData() {
        return strData;
    }

    /**
     * @param strData the strData to set
     */
    public void setStrData(String strData) {
        this.strData = strData;
    }

    @Override
    public String toString() {
        return "XMLUUIDAutoGenEntity [id=" + id + ", strData=" + strData + "]";
    }
}
