/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import jakarta.persistence.Table;

@Entity
@Table(name = "DefDSEntity2")
public class DefaultDSEntity2 {

    @Id
    public long id;

    public String value;

    static DefaultDSEntity2 of(long id, String value) {
        DefaultDSEntity2 entity = new DefaultDSEntity2();
        entity.id = id;
        entity.value = value;
        return entity;
    }

    @Override
    public String toString() {
        return "DefaultDSEntity2 id=" + id + "; value=" + value;
    }
}
