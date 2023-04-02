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

/**
 *
 */
public class XMLEmbeddableUUID_ID {
    private UUID eid;

    public XMLEmbeddableUUID_ID() {

    }

    public XMLEmbeddableUUID_ID(UUID id) {
        this.eid = id;
    }

    /**
     * @return the id
     */
    public UUID getEId() {
        return eid;
    }

    /**
     * @param id the id to set
     */
    public void setEId(UUID id) {
        this.eid = id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((eid == null) ? 0 : eid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        XMLEmbeddableUUID_ID other = (XMLEmbeddableUUID_ID) obj;
        if (eid == null) {
            if (other.eid != null)
                return false;
        } else if (!eid.equals(other.eid))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "XMLEmbeddableUUID_ID [eid=" + eid + "]";
    }

}
