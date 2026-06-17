/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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
package test.jakarta.data.jpa.web.hibernate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SimpleEntity {

    @Id
    long id;

    String val;

    public long getId() {
        return id;
    }

    public String getValue() {
        return val;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setValue(String value) {
        this.val = value;
    }

    @Override
    public String toString() {
        return "SimpleEntity id=" + id + " " + " value=" + val;
    }
}
