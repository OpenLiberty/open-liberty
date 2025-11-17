/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.datastore.web.lib;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * An entity that is defined in a library of a web module.
 */
@Entity
public class WebLibEntity {

    @Id
    public int id;

    public String value;

    public static WebLibEntity of(int id, String value) {
        WebLibEntity entity = new WebLibEntity();
        entity.id = id;
        entity.value = value;
        return entity;
    }

    @Override
    public String toString() {
        return "WebLibEntity id=" + id + "; value=" + value;
    }
}
