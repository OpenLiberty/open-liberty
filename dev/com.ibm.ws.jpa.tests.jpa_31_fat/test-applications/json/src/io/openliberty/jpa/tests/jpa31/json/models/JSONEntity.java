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

package io.openliberty.jpa.tests.jpa31.json.models;

import jakarta.json.JsonValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class JSONEntity {
    @Id
    private long id;

    private JsonValue json;

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the json
     */
    public JsonValue getJson() {
        return json;
    }

    /**
     * @param json the json to set
     */
    public void setJson(JsonValue json) {
        this.json = json;
    }

    @Override
    public String toString() {
        return "JSONEntity [id=" + id + ", json=" + json + "]";
    }

}
