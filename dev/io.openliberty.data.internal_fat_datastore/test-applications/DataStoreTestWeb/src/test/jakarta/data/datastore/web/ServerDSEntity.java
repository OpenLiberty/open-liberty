/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.datastore.web;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ServerDSEntity {

    @Id
    public String id;

    public int value;

    static ServerDSEntity of(String id, int value) {
        ServerDSEntity entity = new ServerDSEntity();
        entity.id = id;
        entity.value = value;
        return entity;
    }

    @Override
    public String toString() {
        return "ServerDSEntity id=" + id + "; value=" + value;
    }
}
