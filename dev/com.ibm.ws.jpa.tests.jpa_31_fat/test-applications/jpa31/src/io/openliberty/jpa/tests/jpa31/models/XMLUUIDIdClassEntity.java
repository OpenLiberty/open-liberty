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

package io.openliberty.jpa.tests.jpa31.models;

import java.util.UUID;

public class XMLUUIDIdClassEntity {
    private UUID uuid_id;

    private long l_id;

    private String strData;

    public XMLUUIDIdClassEntity() {

    }

    /**
     * @return the uuid_id
     */
    public UUID getUuid_id() {
        return uuid_id;
    }

    /**
     * @param uuid_id the uuid_id to set
     */
    public void setUuid_id(UUID uuid_id) {
        this.uuid_id = uuid_id;
    }

    /**
     * @return the l_id
     */
    public long getL_id() {
        return l_id;
    }

    /**
     * @param l_id the l_id to set
     */
    public void setL_id(long l_id) {
        this.l_id = l_id;
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
        return "XMLUUIDIdClassEntity [uuid_id=" + uuid_id + ", l_id=" + l_id + ", strData=" + strData + "]";
    }

}
