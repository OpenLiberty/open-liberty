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
package test.jakarta.data.datastore.ejb;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "DefDSEntity") // share with DefaultDSEntity from web module
public class EJBModuleDefaultDSEntity {

    @Id
    public long id;

    public String value;

    static EJBModuleDefaultDSEntity of(long id, String value) {
        EJBModuleDefaultDSEntity entity = new EJBModuleDefaultDSEntity();
        entity.id = id;
        entity.value = value;
        return entity;
    }

    @Override
    public String toString() {
        return "EJBModuleDefaultDSEntity id=" + id + "; value=" + value;
    }
}
